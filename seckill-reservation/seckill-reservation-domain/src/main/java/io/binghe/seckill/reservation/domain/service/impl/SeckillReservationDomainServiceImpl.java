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
package io.binghe.seckill.reservation.domain.service.impl;

import com.alibaba.fastjson.JSON;
import io.binghe.seckill.common.constants.SeckillConstants;
import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.exception.SeckillException;
import io.binghe.seckill.common.model.enums.SeckillReservationUserStatus;
import io.binghe.seckill.mq.MessageSenderService;
import io.binghe.seckill.reservation.domain.event.SeckillReservationConfigEvent;
import io.binghe.seckill.reservation.domain.event.SeckillReservationUserEvent;
import io.binghe.seckill.reservation.domain.model.entity.SeckillReservationConfig;
import io.binghe.seckill.reservation.domain.model.entity.SeckillReservationUser;
import io.binghe.seckill.reservation.domain.repository.SeckillReservationRepository;
import io.binghe.seckill.reservation.domain.service.SeckillReservationDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description SeckillReservationDomainServiceImpl
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Service
public class SeckillReservationDomainServiceImpl implements SeckillReservationDomainService {
    private static final Logger logger = LoggerFactory.getLogger(SeckillReservationDomainServiceImpl.class);
    @Autowired
    private SeckillReservationRepository seckillReservationRepository;
    @Autowired
    private MessageSenderService messageSenderService;
    @Value("${message.mq.type}")
    private String eventType;

    @Override
    public boolean saveSeckillReservationConfig(SeckillReservationConfig seckillReservationConfig) {
        if (seckillReservationConfig == null || seckillReservationConfig.isEmpty()){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        logger.info("saveSeckillReservationConfig|添加商品预约配置|{}", JSON.toJSONString(seckillReservationConfig));
        boolean success = seckillReservationRepository.saveSeckillReservationConfig(seckillReservationConfig);
        if (success){
            logger.info("saveSeckillReservationConfig|添加商品预约配置|{}", JSON.toJSONString(seckillReservationConfig));
            SeckillReservationConfigEvent seckillReservationConfigEvent = new SeckillReservationConfigEvent(seckillReservationConfig.getGoodsId(), seckillReservationConfig.getStatus(), this.getConfigTopicEvent());
            messageSenderService.send(seckillReservationConfigEvent);
        }
        return success;
    }

    @Override
    public boolean updateSeckillReservationConfig(SeckillReservationConfig seckillReservationConfig) {
        if (seckillReservationConfig == null || seckillReservationConfig.isEmpty()){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        logger.info("updateSeckillReservationConfig|更新商品预约配置|{}", JSON.toJSONString(seckillReservationConfig));
        boolean success = seckillReservationRepository.updateSeckillReservationConfig(seckillReservationConfig);
        if (success){
            logger.info("updateSeckillReservationConfig|更新商品预约配置成功|{}", JSON.toJSONString(seckillReservationConfig));
            SeckillReservationConfigEvent seckillReservationConfigEvent = new SeckillReservationConfigEvent(seckillReservationConfig.getGoodsId(), seckillReservationConfig.getStatus(), this.getConfigTopicEvent());
            messageSenderService.send(seckillReservationConfigEvent);
        }
        return success;
    }

    @Override
    public boolean updateConfigStatus(Integer status, Long goodsId) {
        if (status == null || goodsId == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        logger.info("updateConfigStatus|更新商品预约配置状态|{}|{}", status, goodsId);
        boolean success = seckillReservationRepository.updateConfigStatus(status, goodsId);
        if (success){
            logger.info("updateConfigStatus|更新商品预约配置状态成功|{}|{}", status, goodsId);
            SeckillReservationConfigEvent seckillReservationConfigEvent = new SeckillReservationConfigEvent(goodsId, status, this.getConfigTopicEvent());
            messageSenderService.send(seckillReservationConfigEvent);
        }
        return success;
    }

    @Override
    public int updateReserveCurrentUserCount(Integer reserveCurrentUserCount, Long goodsId) {
        if (reserveCurrentUserCount == null || goodsId == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        return seckillReservationRepository.updateReserveCurrentUserCount(reserveCurrentUserCount, goodsId);
    }

    @Override
    public List<SeckillReservationConfig> getConfigList() {
        return seckillReservationRepository.getConfigList();
    }

    @Override
    public SeckillReservationConfig getConfigDetail(Long goodsId) {
        if (goodsId == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        return seckillReservationRepository.getConfigDetail(goodsId);
    }

    @Override
    public List<SeckillReservationUser> getUserListByGoodsId(Long goodsId, Integer status) {
        if (goodsId == null || status == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        return seckillReservationRepository.getUserListByGoodsId(goodsId, status);
    }

    @Override
    public List<SeckillReservationUser> getGoodsListByUserId(Long userId, Integer status) {
        if (userId == null || status == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        return seckillReservationRepository.getGoodsListByUserId(userId, status);
    }

    @Override
    public boolean reserveGoods(SeckillReservationUser seckillReservationUser) {
        if (seckillReservationUser == null || seckillReservationUser.isEmpty()){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        logger.info("reserveGoods|预约秒杀商品|{}", JSON.toJSONString(seckillReservationUser));
        boolean success = seckillReservationRepository.reserveGoods(seckillReservationUser);
        if (success){
            logger.info("reserveGoods|预约秒杀商品成功|{}", JSON.toJSONString(seckillReservationUser));
            SeckillReservationUserEvent seckillReservationUserEvent = new SeckillReservationUserEvent(seckillReservationUser.getUserId(), seckillReservationUser.getGoodsId(), SeckillReservationUserStatus.NORMAL.getCode(), this.getUserTopicEvent());
            messageSenderService.send(seckillReservationUserEvent);
        }
        return success;
    }

    @Override
    public boolean cancelReserveGoods(Long goodsId, Long userId) {
        if (goodsId == null || userId == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        logger.info("cancelReserveGoods|取消预约秒杀商品|{}|{}", goodsId, userId);
        boolean success = seckillReservationRepository.cancelReserveGoods(goodsId, userId);
        if (success){
            logger.info("cancelReserveGoods|取消预约秒杀商品成功|{}|{}", goodsId, userId);
            SeckillReservationUserEvent seckillReservationUserEvent = new SeckillReservationUserEvent(userId, goodsId, SeckillReservationUserStatus.DELETE.getCode(), this.getUserTopicEvent());
            messageSenderService.send(seckillReservationUserEvent);
        }
        return success;
    }

    @Override
    public SeckillReservationUser getSeckillReservationUser(Long userId, Long goodsId, Integer status) {
        if (goodsId == null || userId == null || status == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        return seckillReservationRepository.getSeckillReservationUser(userId, goodsId, status);
    }

    /**
     * 获取预约配置主题事件
     */
    private String getConfigTopicEvent(){
        return SeckillConstants.EVENT_PUBLISH_TYPE_ROCKETMQ.equals(eventType) ? SeckillConstants.TOPIC_EVENT_ROCKETMQ_RESERVATION_CONFIG : SeckillConstants.TOPIC_EVENT_COLA;
    }
    /**
     * 获取预约记录主题事件
     */
    private String getUserTopicEvent(){
        return SeckillConstants.EVENT_PUBLISH_TYPE_ROCKETMQ.equals(eventType) ? SeckillConstants.TOPIC_EVENT_ROCKETMQ_RESERVATION_USER : SeckillConstants.TOPIC_EVENT_COLA;
    }
}
