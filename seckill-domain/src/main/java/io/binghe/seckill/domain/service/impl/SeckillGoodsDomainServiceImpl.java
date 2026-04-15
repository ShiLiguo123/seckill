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
package io.binghe.seckill.domain.service.impl;

import com.alibaba.fastjson.JSON;
import io.binghe.seckill.domain.code.HttpCode;
import io.binghe.seckill.domain.event.SeckillGoodsEvent;
import io.binghe.seckill.domain.event.publisher.EventPublisher;
import io.binghe.seckill.domain.exception.SeckillException;
import io.binghe.seckill.domain.model.entity.SeckillGoods;
import io.binghe.seckill.domain.model.enums.SeckillGoodsStatus;
import io.binghe.seckill.domain.repository.SeckillGoodsRepository;
import io.binghe.seckill.domain.service.SeckillGoodsDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 领域层服务实现类
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Service
public class SeckillGoodsDomainServiceImpl implements SeckillGoodsDomainService {
    private static final Logger logger = LoggerFactory.getLogger(SeckillGoodsDomainServiceImpl.class);

    @Autowired
    private SeckillGoodsRepository seckillGoodsRepository;

    @Autowired
    private EventPublisher eventPublisher;

    @Override
    public void saveSeckillGoods(SeckillGoods seckillGoods) {
        logger.info("goodsPublish|发布秒杀商品|{}", JSON.toJSON(seckillGoods));
        if (seckillGoods == null || !seckillGoods.validateParams()){
            throw new SeckillException(HttpCode.PARAMS_INVALID);
        }
        seckillGoods.setStatus(SeckillGoodsStatus.PUBLISHED.getCode());
        seckillGoodsRepository.saveSeckillGoods(seckillGoods);
        logger.info("goodsPublish|秒杀商品已经发布|{}", seckillGoods.getId());

        SeckillGoodsEvent seckillGoodsEvent = new SeckillGoodsEvent(seckillGoods.getId(), seckillGoods.getActivityId(), SeckillGoodsStatus.PUBLISHED.getCode());
        eventPublisher.publish(seckillGoodsEvent);
        logger.info("goodsPublish|秒杀商品事件已经发布|{}", seckillGoods.getId());
    }

    @Override
    public SeckillGoods getSeckillGoodsId(Long id) {
        return seckillGoodsRepository.getSeckillGoodsId(id);
    }

    @Override
    public List<SeckillGoods> getSeckillGoodsByActivityId(Long activityId) {
        return seckillGoodsRepository.getSeckillGoodsByActivityId(activityId);
    }

    @Override
    public void updateStatus(Integer status, Long id) {
        logger.info("goodsPublish|更新秒杀商品状态|{}", id);
        if (id == null){
            throw new SeckillException(HttpCode.PARAMS_INVALID);
        }
        SeckillGoods seckillGoods = seckillGoodsRepository.getSeckillGoodsId(id);
        if (seckillGoods == null){
            throw new SeckillException(HttpCode.GOODS_NOT_EXISTS);
        }
        //更新状态状态
        seckillGoodsRepository.updateStatus(status, id);
        logger.info("goodsPublish|秒杀商品状态已经更新|{},{}", id, status);

        SeckillGoodsEvent seckillGoodsEvent = new SeckillGoodsEvent(seckillGoods.getId(), seckillGoods.getActivityId(), status);
        eventPublisher.publish(seckillGoodsEvent);
        logger.info("goodsPublish|秒杀商品事件已经发布|{}", seckillGoodsEvent.getId());
    }

    @Override
    public void updateAvailableStock(Integer count, Long id) {
        logger.info("goodsPublish|更新秒杀商品库存|{}", id);
        if (count == null || count <= 0 || id == null){
            throw new SeckillException(HttpCode.PARAMS_INVALID);
        }
        SeckillGoods seckillGoods = seckillGoodsRepository.getSeckillGoodsId(id);
        if (seckillGoods == null){
            throw new SeckillException(HttpCode.GOODS_NOT_EXISTS);
        }
        seckillGoodsRepository.updateAvailableStock(count, id);
        logger.info("goodsPublish|秒杀商品库存已经更新|{}", id);

        SeckillGoodsEvent seckillGoodsEvent = new SeckillGoodsEvent(seckillGoods.getId(), seckillGoods.getActivityId(), seckillGoods.getStatus());
        eventPublisher.publish(seckillGoodsEvent);
        logger.info("goodsPublish|秒杀商品库存事件已经发布|{}", id);
    }

    @Override
    public Integer getAvailableStockById(Long id) {
        return seckillGoodsRepository.getAvailableStockById(id);
    }
}
