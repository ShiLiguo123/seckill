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
package io.binghe.seckill.reservation.application.event;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.binghe.seckill.common.constants.SeckillConstants;
import io.binghe.seckill.common.model.enums.SeckillReservationUserStatus;
import io.binghe.seckill.reservation.application.cache.SeckillReservationConfigCacheService;
import io.binghe.seckill.reservation.application.cache.SeckillReservationUserCacheService;
import io.binghe.seckill.reservation.domain.event.SeckillReservationUserEvent;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 基于RocketMQ的商品事件处理器
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Component
@ConditionalOnProperty(name = "message.mq.type", havingValue = "rocketmq")
@RocketMQMessageListener(consumerGroup = SeckillConstants.EVENT_RESERVATION_USER_CONSUMER_GROUP, topic = SeckillConstants.TOPIC_EVENT_ROCKETMQ_RESERVATION_USER)
public class SeckillReservationUserRocketMQEventHandler implements RocketMQListener<String> {
    private final Logger logger = LoggerFactory.getLogger(SeckillReservationUserRocketMQEventHandler.class);
    @Autowired
    private SeckillReservationUserCacheService seckillReservationUserCacheService;
    @Autowired
    private SeckillReservationConfigCacheService seckillReservationConfigCacheService;
    @Override
    public void onMessage(String message) {
        logger.info("rocketmq|reservationUserEvent|接收秒杀品预约事件|{}", message);
        if (StrUtil.isEmpty(message)){
            logger.info("rocketmq|reservationUserEvent|接收秒杀品预约事件参数错误");
            return;
        }
        SeckillReservationUserEvent seckillReservationUserEvent = this.getEventMessage(message);
        //获取的数据为空
        if (seckillReservationUserEvent == null || seckillReservationUserEvent.getId() == null || seckillReservationUserEvent.getGoodsId() == null){
            logger.info("rocketmq|reservationUserEvent|接收秒杀品预约事件参数错误");
            return;
        }
        logger.info("rocketmq|reservationUserEvent|接收秒杀品预约事件解析后的数据|{}", JSON.toJSONString(seckillReservationUserEvent));
        if (seckillReservationUserEvent.getStatus() != null && SeckillReservationUserStatus.isDeleted(seckillReservationUserEvent.getStatus())){
            logger.info("rocketmq|reservationUserEvent|删除缓存中的数据|{}", JSON.toJSONString(seckillReservationUserEvent));
            seckillReservationUserCacheService.deleteSeckillReservationUserFromCache(seckillReservationUserEvent);
        }else{
            logger.info("rocketmq|reservationUserEvent|更新缓存中的数据|{}", JSON.toJSONString(seckillReservationUserEvent));
            seckillReservationUserCacheService.tryUpdateSeckillReservationUserCacheByUserIdAndGoodsId(seckillReservationUserEvent.getId(), seckillReservationUserEvent.getGoodsId(), false);
            seckillReservationUserCacheService.tryUpdateGoodsListCacheByUserId(seckillReservationUserEvent.getId(), false);
            seckillReservationUserCacheService.tryUpdatetUserListCacheByGoodsId(seckillReservationUserEvent.getGoodsId(), false);
        }
        seckillReservationConfigCacheService.updateSeckillReservationConfigCurrentUserCount(seckillReservationUserEvent.getGoodsId(), seckillReservationUserEvent.getStatus(), 0L);
    }

    private SeckillReservationUserEvent getEventMessage(String msg){
        JSONObject jsonObject = JSONObject.parseObject(msg);
        String eventStr = jsonObject.getString(SeckillConstants.MSG_KEY);
        return JSONObject.parseObject(eventStr, SeckillReservationUserEvent.class);
    }
}
