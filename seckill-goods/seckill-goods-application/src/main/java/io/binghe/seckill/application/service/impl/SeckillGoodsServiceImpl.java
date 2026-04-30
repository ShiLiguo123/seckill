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
package io.binghe.seckill.application.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.alibaba.fastjson.JSONObject;
import io.binghe.seckill.application.builder.SeckillGoodsBuilder;
import io.binghe.seckill.application.cache.service.SeckillGoodsCacheService;
import io.binghe.seckill.application.cache.service.SeckillGoodsListCacheService;
import io.binghe.seckill.application.command.SeckillGoodsCommond;
import io.binghe.seckill.application.service.SeckillGoodsService;
import io.binghe.seckill.common.cache.distribute.DistributedCacheService;
import io.binghe.seckill.common.cache.local.LocalCacheService;
import io.binghe.seckill.common.cache.model.SeckillBusinessCache;
import io.binghe.seckill.common.constants.SeckillConstants;
import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.exception.SeckillException;
import io.binghe.seckill.common.model.dto.SeckillActivityDTO;
import io.binghe.seckill.common.model.dto.SeckillGoodsDTO;
import io.binghe.seckill.common.model.enums.SeckillGoodsStatus;
import io.binghe.seckill.common.model.message.ErrorMessage;
import io.binghe.seckill.common.model.message.TxMessage;
import io.binghe.seckill.common.utils.beans.BeanUtil;
import io.binghe.seckill.common.utils.id.SnowFlakeFactory;
import io.binghe.seckill.dubbo.interfaces.activity.SeckillActivityDubboService;
import io.binghe.seckill.goods.domain.model.entity.SeckillGoods;
import io.binghe.seckill.goods.domain.service.SeckillGoodsDomainService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 商品服务
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {
    private final Logger logger = LoggerFactory.getLogger(SeckillGoodsServiceImpl.class);
    @Autowired
    private SeckillGoodsDomainService seckillGoodsDomainService;
    @DubboReference(version = "1.0.0")
    private SeckillActivityDubboService seckillActivityDubboService;
    @Autowired
    private LocalCacheService<String, SeckillGoods> localCacheService;
    @Autowired
    private DistributedCacheService distributedCacheService;
    @Autowired
    private SeckillGoodsCacheService seckillGoodsCacheService;
    @Autowired
    private SeckillGoodsListCacheService seckillGoodsListCacheService;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSeckillGoods(SeckillGoodsCommond seckillGoodsCommond) {
        if (seckillGoodsCommond == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        SeckillActivityDTO seckillActivity = seckillActivityDubboService.getSeckillActivity(seckillGoodsCommond.getActivityId(), seckillGoodsCommond.getVersion());
        if (seckillActivity == null){
            throw new SeckillException(ErrorCode.ACTIVITY_NOT_EXISTS);
        }
        SeckillGoods seckillGoods = SeckillGoodsBuilder.toSeckillGoods(seckillGoodsCommond);
        seckillGoods.setStartTime(seckillActivity.getStartTime());
        seckillGoods.setEndTime(seckillActivity.getEndTime());
        seckillGoods.setAvailableStock(seckillGoodsCommond.getInitialStock());
        seckillGoods.setId(SnowFlakeFactory.getSnowFlakeFromCache().nextId());
        seckillGoods.setStatus(SeckillGoodsStatus.PUBLISHED.getCode());
        //将商品的库存同步到Redis
        distributedCacheService.put(SeckillConstants.getKey(SeckillConstants.GOODS_ITEM_STOCK_KEY_PREFIX, String.valueOf(seckillGoods.getId())), seckillGoods.getAvailableStock());
        //商品限购同步到Redis
        distributedCacheService.put(SeckillConstants.getKey(SeckillConstants.GOODS_ITEM_LIMIT_KEY_PREFIX, String.valueOf(seckillGoods.getId())), seckillGoods.getLimitNum());
        seckillGoodsDomainService.saveSeckillGoods(seckillGoods);
    }

    @Override
    public SeckillGoods getSeckillGoodsId(Long id) {
        //实现多级缓存，先从本地缓存获取
        String key = SeckillConstants.getKey(SeckillConstants.GOODS_ITEM_KEY_PREFIX, String.valueOf(id));
        //从本地缓存获取数据
        SeckillGoods seckillGoods =  localCacheService.getIfPresent(key);
        //如果本地缓存为空，从Redis获取数据
        if (seckillGoods == null){
            //从Redis获取数据
            seckillGoods = distributedCacheService.getObject(key, SeckillGoods.class);
            //从Redis获取的数据为空
            if (seckillGoods == null){
                //数据库中获取数据
                seckillGoods = seckillGoodsDomainService.getSeckillGoodsId(id);
                if (seckillGoods != null){
                    //将数据缓存到Redis
                    distributedCacheService.put(key, seckillGoods, 10, TimeUnit.MINUTES);
                }
            }
            if (seckillGoods != null){
                localCacheService.put(key, seckillGoods);
            }
        }
        return seckillGoods;
    }

    @Override
    public SeckillGoodsDTO getSeckillGoods(Long id, Long version) {
        if (id == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        SeckillBusinessCache<SeckillGoods> seckillGoodsCache = seckillGoodsCacheService.getSeckillGoods(id, version);
        //稍后再试，前端需要对这个状态做特殊处理，即不去刷新数据，静默稍后再试
        if (seckillGoodsCache.isRetryLater()){
            throw new SeckillException(ErrorCode.RETRY_LATER);
        }
        //缓存中不存在商品数据
        if (!seckillGoodsCache.isExist()){
            throw new SeckillException(ErrorCode.GOODS_NOT_EXISTS);
        }
        SeckillGoodsDTO seckillGoodsDTO = SeckillGoodsBuilder.toSeckillGoodsDTO(seckillGoodsCache.getData());
        seckillGoodsDTO.setVersion(seckillGoodsCache.getVersion());
        return seckillGoodsDTO;
    }


    @Override
    public List<SeckillGoods> getSeckillGoodsByActivityId(Long activityId) {
        return seckillGoodsDomainService.getSeckillGoodsByActivityId(activityId);
    }

    @Override
    public List<SeckillGoodsDTO> getSeckillGoodsList(Long activityId, Long version) {
        if (activityId == null){
            throw new SeckillException(ErrorCode.ACTIVITY_NOT_EXISTS);
        }
        SeckillBusinessCache<List<SeckillGoods>> seckillGoodsListCache = seckillGoodsListCacheService.getCachedGoodsList(activityId, version);
        //稍后再试，前端需要对这个状态做特殊处理，即不去刷新数据，静默稍后再试
        if (seckillGoodsListCache.isRetryLater()){
            throw new SeckillException(ErrorCode.RETRY_LATER);
        }
        if (!seckillGoodsListCache.isExist()){
            throw new SeckillException(ErrorCode.ACTIVITY_NOT_EXISTS);
        }
        List<SeckillGoodsDTO> seckillActivityDTOList = seckillGoodsListCache.getData().stream().map((seckillGoods) -> {
            SeckillGoodsDTO seckillGoodsDTO = new SeckillGoodsDTO();
            BeanUtil.copyProperties(seckillGoods, seckillGoodsDTO);
            seckillGoodsDTO.setVersion(seckillGoodsListCache.getVersion());
            return seckillGoodsDTO;
        }).collect(Collectors.toList());
        return seckillActivityDTOList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer status, Long id) {
        if (status == SeckillGoodsStatus.OFFLINE.getCode()){
            //清空缓存
            this.clearCache(String.valueOf(id));
        }
        seckillGoodsDomainService.updateStatus(status, id);
    }

    /**
     * 清空缓存的商品数据
     */
    private void clearCache(String id){
        //清除缓存中的商品库存
        distributedCacheService.delete(SeckillConstants.getKey(SeckillConstants.GOODS_ITEM_STOCK_KEY_PREFIX, id));
        //清除本地缓存中的商品
        localCacheService.delete(SeckillConstants.getKey(SeckillConstants.GOODS_ITEM_KEY_PREFIX, id));
        //清除Redis缓存中的商品
        distributedCacheService.delete(SeckillConstants.getKey(SeckillConstants.GOODS_ITEM_KEY_PREFIX, id));
        //清除商品的限购信息
        distributedCacheService.delete(SeckillConstants.getKey(SeckillConstants.GOODS_ITEM_LIMIT_KEY_PREFIX, id));
    }

    @Override
    public boolean updateAvailableStock(Integer count, Long id) {
        return seckillGoodsDomainService.updateAvailableStock(count, id);
    }
    @Override
    public boolean updateDbAvailableStock(Integer count, Long id) {
        return seckillGoodsDomainService.updateDbAvailableStock(count, id);
    }

    @Override
    public Integer getAvailableStockById(Long id) {
        return seckillGoodsDomainService.getAvailableStockById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAvailableStock(TxMessage txMessage) {
        Boolean decrementStock = distributedCacheService.hasKey(SeckillConstants.getKey(SeckillConstants.GOODS_TX_KEY, String.valueOf(txMessage.getTxNo())));
        if (BooleanUtil.isTrue(decrementStock)){
            logger.info("updateAvailableStock|秒杀商品微服务已经扣减过库存|{}", txMessage.getTxNo());
            return true;
        }
        boolean isUpdate = false;
        try{
            isUpdate = seckillGoodsDomainService.updateAvailableStock(txMessage.getQuantity(), txMessage.getGoodsId());
            //成功扣减库存成功
            if (isUpdate){
                distributedCacheService.put(SeckillConstants.getKey(SeckillConstants.GOODS_TX_KEY, String.valueOf(txMessage.getTxNo())), txMessage.getTxNo(), SeckillConstants.TX_LOG_EXPIRE_DAY, TimeUnit.DAYS);
            }else{
                //发送失败消息给订单微服务
                rocketMQTemplate.send(SeckillConstants.TOPIC_ERROR_MSG, getErrorMessage(txMessage));
            }
        }catch (Exception e){
            isUpdate = false;
            logger.error("updateAvailableStock|抛出异常|{}|{}",txMessage.getTxNo(), e.getMessage());
            //发送失败消息给订单微服务
            rocketMQTemplate.send(SeckillConstants.TOPIC_ERROR_MSG, getErrorMessage(txMessage));
        }
        return isUpdate;
    }

    /**
     * 发送给订单微服务的错误消息
     */
    private Message<String> getErrorMessage(TxMessage txMessage){
        ErrorMessage errorMessage = new ErrorMessage(txMessage.getTxNo(), txMessage.getGoodsId(), txMessage.getQuantity(), txMessage.getPlaceOrderType(), txMessage.getException());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(SeckillConstants.ERROR_MSG_KEY, errorMessage);
        return MessageBuilder.withPayload(jsonObject.toJSONString()).build();
    }

}
