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

import com.alibaba.cola.dto.Response;
import com.alibaba.cola.event.EventHandler;
import com.alibaba.cola.event.EventHandlerI;
import com.alibaba.fastjson.JSON;
import io.binghe.seckill.common.model.enums.SeckillReservationUserStatus;
import io.binghe.seckill.reservation.application.cache.SeckillReservationConfigCacheService;
import io.binghe.seckill.reservation.application.cache.SeckillReservationUserCacheService;
import io.binghe.seckill.reservation.domain.event.SeckillReservationUserEvent;
import io.binghe.seckill.reservation.domain.model.entity.SeckillReservationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 商品事件处理器
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@EventHandler
@ConditionalOnProperty(name = "message.mq.type", havingValue = "cola")
public class SeckillReservationUserColaEventHandler implements EventHandlerI<Response, SeckillReservationUserEvent> {
    private final Logger logger = LoggerFactory.getLogger(SeckillReservationUserColaEventHandler.class);
    @Autowired
    private SeckillReservationUserCacheService seckillReservationUserCacheService;
    @Autowired
    private SeckillReservationConfigCacheService seckillReservationConfigCacheService;
    @Override
    public Response execute(SeckillReservationUserEvent seckillReservationUserEvent) {
        if (seckillReservationUserEvent == null || seckillReservationUserEvent.getId() == null || seckillReservationUserEvent.getGoodsId() == null){
            logger.info("cola|reservationUserEvent|接收秒杀品预约事件参数错误");
            return Response.buildSuccess();
        }
        logger.info("cola|reservationUserEvent|接收秒杀品预约事件|{}", JSON.toJSON(seckillReservationUserEvent));

        if (seckillReservationUserEvent.getStatus() != null && SeckillReservationUserStatus.isDeleted(seckillReservationUserEvent.getStatus())){
            logger.info("cola|reservationUserEvent|删除缓存中的数据|{}", JSON.toJSONString(seckillReservationUserEvent));
            seckillReservationUserCacheService.deleteSeckillReservationUserFromCache(seckillReservationUserEvent);
        }else{
            logger.info("cola|reservationUserEvent|更新缓存中的数据|{}", JSON.toJSONString(seckillReservationUserEvent));
            seckillReservationUserCacheService.tryUpdateSeckillReservationUserCacheByUserIdAndGoodsId(seckillReservationUserEvent.getId(), seckillReservationUserEvent.getGoodsId(), false);
            seckillReservationUserCacheService.tryUpdateGoodsListCacheByUserId(seckillReservationUserEvent.getId(), false);
            seckillReservationUserCacheService.tryUpdatetUserListCacheByGoodsId(seckillReservationUserEvent.getGoodsId(), false);
        }
        seckillReservationConfigCacheService.updateSeckillReservationConfigCurrentUserCount(seckillReservationUserEvent.getGoodsId(), seckillReservationUserEvent.getStatus(), 0L);
        return Response.buildSuccess();
    }
}
