/**
 * Copyright 2022-9999 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.binghe.seckill.stock.application.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import io.binghe.seckill.common.cache.distribute.DistributedCacheService;
import io.binghe.seckill.common.cache.model.SeckillBusinessCache;
import io.binghe.seckill.common.constants.SeckillConstants;
import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.exception.SeckillException;
import io.binghe.seckill.common.lock.DistributedLock;
import io.binghe.seckill.common.lock.factoty.DistributedLockFactory;
import io.binghe.seckill.common.model.enums.SeckillStockBucketStatus;
import io.binghe.seckill.stock.application.cache.SeckillStockBucketCacheService;
import io.binghe.seckill.stock.application.model.dto.SeckillStockBucketDTO;
import io.binghe.seckill.stock.application.service.SeckillStockBucketArrangementService;
import io.binghe.seckill.stock.domain.model.entity.SeckillStockBucket;
import io.binghe.seckill.stock.domain.model.enums.SeckillStockBucketArrangementMode;
import io.binghe.seckill.stock.domain.service.SeckillStockBucketDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 分桶库存编排服务
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Service
public class SeckillStockBucketArrangementServiceImpl implements SeckillStockBucketArrangementService {
    private static final Logger logger = LoggerFactory.getLogger(SeckillStockBucketArrangementServiceImpl.class);
    @Autowired
    private SeckillStockBucketDomainService seckillStockBucketDomainService;
    @Autowired
    private SeckillStockBucketCacheService seckillStockBucketCacheService;
    @Autowired
    private DistributedCacheService distributedCacheService;
    @Autowired
    private DistributedLockFactory distributedLockFactory;
    @Autowired
    private DataSourceTransactionManager dataSourceTransactionManager;
    @Autowired
    private TransactionDefinition transactionDefinition;
    @Value("${place.order.type:bucket}")
    private String placeOrderType;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void arrangeStockBuckets(Long goodsId, Integer stock, Integer bucketsQuantity, Integer assignmentMode) {
        logger.info("arrangeBuckets|准备库存分桶|{},{},{}", goodsId, stock, bucketsQuantity);
        if (goodsId == null || stock == null || stock <= 0 || bucketsQuantity == null || bucketsQuantity <= 0){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        DistributedLock lock = distributedLockFactory.getDistributedLock(SeckillConstants.getKey(SeckillConstants.GOODS_STOCK_BUCKETS_SUSPEND_KEY, String.valueOf(goodsId)));
        try{
            boolean isLock = lock.tryLock();
            if (!isLock){
                logger.info("arrangeStockBuckets|库存分桶时获取锁失败|{}", goodsId);
                return;
            }
            TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
            try{
                boolean suspendSuccess = seckillStockBucketDomainService.suspendBuckets(goodsId);
                if (!suspendSuccess){
                    logger.info("arrangeStockBuckets|关闭库存分桶失败|{}", goodsId);
                    throw new SeckillException(ErrorCode.BUCKET_CLOSED_FAILED);
                }
                dataSourceTransactionManager.commit(transactionStatus);
            }catch (Exception e){
                logger.info("arrangeStockBuckets|关闭分桶失败回滚中|{}", goodsId, e);
                dataSourceTransactionManager.rollback(transactionStatus);
            }
            List<SeckillStockBucket> buckets = seckillStockBucketDomainService.getBucketsByGoodsId(goodsId);
            if (CollectionUtil.isEmpty(buckets)){
                //初始化库存分桶
                this.initStockBuckets(goodsId, stock, bucketsQuantity);
                return;
            }
            if (SeckillStockBucketArrangementMode.isTotalArrangementMode(assignmentMode)){  //总量模式
                this.arrangeStockBucketsBasedTotalMode(goodsId, stock, bucketsQuantity, buckets);
            }else if (SeckillStockBucketArrangementMode.isIncrementalArrangementMode(assignmentMode)){  //增量模式
                this.rearrangeStockBucketsBasedIncrementalMode(goodsId, stock, bucketsQuantity, buckets);
            }
        }catch (Exception e){
            logger.error("arrangeStockBuckets|库存分桶错误|", e);
            throw new SeckillException(ErrorCode.BUCKET_CREATE_FAILED);
        }finally {
            lock.unlock();
            //打开分桶
            boolean success = seckillStockBucketDomainService.resumeBuckets(goodsId);
            if (!success){
                logger.error("arrangeStockBuckets|打开库存分桶失败|{}", goodsId);
            }
        }
    }

    /**
     * 按照库存增量模式编排库存
     */
    private void rearrangeStockBucketsBasedIncrementalMode(Long goodsId, Integer stock, Integer bucketsQuantity, List<SeckillStockBucket> buckets) {
        //获取所有所有可用库存
        int availableStock = buckets.stream().mapToInt(SeckillStockBucket :: getAvailableStock).sum();
        int initStock = buckets.stream().mapToInt(SeckillStockBucket :: getInitialStock).sum();
        //计算增加后的可用库存
        int totalAvailableStock = stock + availableStock;
        //计算增加后的总库存
        int totalInitStock = stock + initStock;
        //可用库存不足
        if (totalAvailableStock <= 0){
            throw new SeckillException(ErrorCode.STOCK_LT_ZERO);
        }
        //计算销量
        int soldStock = initStock - availableStock;
        //销量大于现有总库存
        if (soldStock > totalInitStock){
            throw new SeckillException(ErrorCode.BUCKET_SOLD_BEYOND_TOTAL);
        }
        //提交分桶
        this.submitBuckets(goodsId, totalInitStock, totalAvailableStock, bucketsQuantity);
    }

    /**
     * 按照库存总量模式编排库存
     */
    private void arrangeStockBucketsBasedTotalMode(Long goodsId, Integer stock, Integer bucketsQuantity, List<SeckillStockBucket> buckets) {
        //获取当前所有的可用库存
        int availableStock = buckets.stream().mapToInt(SeckillStockBucket :: getAvailableStock).sum();
        //获取当前所有的库存
        int totalStock = buckets.stream().mapToInt(SeckillStockBucket :: getInitialStock).sum();
        //计算已售库存量
        int soldStock = totalStock - availableStock;
        //已售商品储量大于传入的总商品数量
        if (soldStock > stock){
            throw new SeckillException(ErrorCode.BUCKET_SOLD_BEYOND_TOTAL);
        }
        //计算当前可用库存量
        availableStock = availableStock + (stock - totalStock);
        if (availableStock <= 0){
            throw new SeckillException(ErrorCode.STOCK_LT_ZERO);
        }
        //提交分桶
        this.submitBuckets(goodsId, stock, availableStock, bucketsQuantity);
    }

    /**
     * 初始化分桶信息
     */
    private void initStockBuckets(Long goodsId, Integer stock, Integer bucketsQuantity) {
        this.submitBuckets(goodsId, stock, stock, bucketsQuantity);
    }

    /**
     * 提交分桶库存
     */
    private void submitBuckets(Long goodsId, Integer initStock, Integer availableStock, Integer bucketsQuantity){
        List<SeckillStockBucket> buckets = this.buildBuckets(goodsId, initStock, availableStock, bucketsQuantity);
        boolean success = seckillStockBucketDomainService.arrangeBuckets(goodsId, buckets);
        if (!success){
            throw new SeckillException(ErrorCode.BUCKET_CREATE_FAILED);
        }
        //清理缓存分桶库存
        distributedCacheService.deleteKeyPrefix(SeckillConstants.GOODS_BUCKET_AVAILABLE_STOCKS_KEY);
        //保存分桶库存
        String stockBucketKey = SeckillConstants.getKey(SeckillConstants.GOODS_BUCKET_AVAILABLE_STOCKS_KEY, String.valueOf(goodsId));
        //保存每一项分桶库存到缓存
        buckets.forEach(bucket -> distributedCacheService.put(SeckillConstants.getKey(stockBucketKey,String.valueOf(bucket.getSerialNo())), bucket.getAvailableStock()));
        //保存分桶数量到缓存
        distributedCacheService.put(SeckillConstants.getKey(SeckillConstants.GOODS_BUCKETS_QUANTITY_KEY, String.valueOf(goodsId)), buckets.size());
    }

    /**
     * 构建分桶库存
     */
    private List<SeckillStockBucket> buildBuckets(Long goodsId, Integer initStock, Integer availableStock,  Integer bucketsQuantity){
        if (goodsId == null || initStock == null || initStock <= 0 || availableStock == null || availableStock <= 0 || bucketsQuantity == null || bucketsQuantity <= 0){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        List<SeckillStockBucket> buckets = new ArrayList<>();
        int initAverageStocks = initStock / bucketsQuantity;
        int initPieceStocks = initStock % bucketsQuantity;

        int avaiableAverageStocks = availableStock / bucketsQuantity;
        int avaiablePieceStocks = availableStock % bucketsQuantity;

        for (int i = 0; i < bucketsQuantity - 1; i++){
            SeckillStockBucket bucket = new SeckillStockBucket(goodsId, initAverageStocks, avaiableAverageStocks, SeckillStockBucketStatus.ENABLED.getCode(), i);
            buckets.add(bucket);
        }
        int initRestStock = initAverageStocks + initPieceStocks;
        int availableRestStock = avaiableAverageStocks + avaiablePieceStocks;

        //计算差值
        int subRestStock = availableRestStock - initRestStock;
        SeckillStockBucket bucket = new SeckillStockBucket(goodsId, initRestStock, subRestStock > 0 ? initRestStock : availableRestStock, SeckillStockBucketStatus.ENABLED.getCode(), bucketsQuantity - 1);
        buckets.add(bucket);
        return subRestStock > 0 ? this.buildBuckets(buckets, subRestStock) : buckets;
    }

    private List<SeckillStockBucket> buildBuckets(List<SeckillStockBucket> buckets, int subRestStock){
        if (CollectionUtil.isEmpty(buckets)){
            return buckets;
        }
        IntStream.range(0, subRestStock).forEach((i) -> {
            SeckillStockBucket bucket = buckets.get(i);
            bucket.setAvailableStock(bucket.getAvailableStock() + 1);
            buckets.set(i, bucket);
        });
        return buckets;
    }

    @Override
    public SeckillStockBucketDTO getSeckillStockBucketDTO(Long goodsId, Long version) {
        if (goodsId == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        SeckillBusinessCache<SeckillStockBucketDTO> seckillStockBucketCache = seckillStockBucketCacheService.getTotalStockBuckets(goodsId, version);
        //稍后再试，前端需要对这个状态做特殊处理，即不去刷新数据，静默稍后再试
        if (seckillStockBucketCache.isRetryLater()){
            throw new SeckillException(ErrorCode.RETRY_LATER);
        }
        //缓存中不存在商品库存数据
        if (!seckillStockBucketCache.isExist()){
            throw new SeckillException(ErrorCode.STOCK_IS_NULL);
        }
        return seckillStockBucketCache.getData();
    }
}
