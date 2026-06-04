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
import io.binghe.seckill.reservation.application.cache.SeckillReservationConfigCacheService;
import io.binghe.seckill.reservation.domain.event.SeckillReservationConfigEvent;
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
public class SeckillReservationConfigColaEventHandler implements EventHandlerI<Response, SeckillReservationConfigEvent> {
    private final Logger logger = LoggerFactory.getLogger(SeckillReservationConfigColaEventHandler.class);

    @Autowired
    private SeckillReservationConfigCacheService seckillReservationConfigCacheService;

    @Override
    public Response execute(SeckillReservationConfigEvent seckillReservationConfigEvent) {
        if (seckillReservationConfigEvent == null || seckillReservationConfigEvent.getId() == null){
            logger.info("cola|reservationConfigEvent|接收秒杀品预约配置事件参数错误");
            return Response.buildSuccess();
        }
        logger.info("cola|reservationConfigEvent|接收秒杀品预约配置事件|{}", JSON.toJSON(seckillReservationConfigEvent));
        seckillReservationConfigCacheService.tryUpdateSeckillReservationConfigCacheByLock(seckillReservationConfigEvent.getId(), false);
        seckillReservationConfigCacheService.tryUpdateSeckillReservationConfigListCacheByLock(false);
        return Response.buildSuccess();
    }
}
