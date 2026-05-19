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
package io.binghe.seckill.stock.infrastructure.repository;

import cn.hutool.core.collection.CollectionUtil;
import io.binghe.seckill.common.model.enums.SeckillStockBucketStatus;
import io.binghe.seckill.stock.domain.model.entity.SeckillStockBucket;
import io.binghe.seckill.stock.domain.repository.SeckillStockBucketRepository;
import io.binghe.seckill.stock.infrastructure.mapper.SeckillStockBucketMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 分桶库存Repository实现类
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Component
public class SeckillStockBucketRepositoryImpl implements SeckillStockBucketRepository {

    @Autowired
    private SeckillStockBucketMapper seckillStockBucketMapper;

    @Override
    public boolean submitBuckets(Long goodsId, List<SeckillStockBucket> buckets) {
        if (goodsId == null || CollectionUtil.isEmpty(buckets)){
            return false;
        }
        seckillStockBucketMapper.deleteByGoodsId(goodsId);
        seckillStockBucketMapper.insertBatch(buckets);
        return true;
    }

    @Override
    public boolean increaseStock(Integer quantity, Integer serialNo, Long goodsId) {
        if (quantity == null || serialNo == null || goodsId == null){
            return false;
        }
        seckillStockBucketMapper.increaseStock(quantity, serialNo, goodsId);
        return true;
    }

    @Override
    public boolean decreaseStock(Integer quantity, Integer serialNo, Long goodsId) {
        if (quantity == null || serialNo == null || goodsId == null){
            return false;
        }
        seckillStockBucketMapper.decreaseStock(quantity, serialNo, goodsId);
        return true;
    }

    @Override
    public List<SeckillStockBucket> getBucketsByGoodsId(Long goodsId) {
        if (goodsId == null){
            return Collections.emptyList();
        }
        return seckillStockBucketMapper.getBucketsByGoodsId(goodsId);
    }

    @Override
    public boolean suspendBuckets(Long goodsId) {
        if (goodsId == null){
            return false;
        }
        seckillStockBucketMapper.updateStatusByGoodsId(SeckillStockBucketStatus.DISABLED.getCode(), goodsId);
        return true;
    }

    @Override
    public boolean resumeBuckets(Long goodsId) {
        if (goodsId == null){
            return false;
        }
        seckillStockBucketMapper.updateStatusByGoodsId(SeckillStockBucketStatus.ENABLED.getCode(), goodsId);
        return true;
    }
}
