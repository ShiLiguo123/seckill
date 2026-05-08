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
package io.binghe.seckill.activity.application.event.handler;

import com.alibaba.cola.dto.Response;
import com.alibaba.cola.event.EventHandler;
import com.alibaba.cola.event.EventHandlerI;
import com.alibaba.fastjson.JSON;
import io.binghe.seckill.activity.application.cache.service.SeckillActivityCacheService;
import io.binghe.seckill.activity.application.cache.service.SeckillActivityListCacheService;
import io.binghe.seckill.activity.domain.event.SeckillActivityEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 接收活动事件
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@EventHandler
@ConditionalOnProperty(name = "message.mq.type", havingValue = "cola")
public class SeckillActivityColaEventHandler implements EventHandlerI<Response, SeckillActivityEvent> {
    private final Logger logger = LoggerFactory.getLogger(SeckillActivityColaEventHandler.class);
    @Autowired
    private SeckillActivityCacheService seckillActivityCacheService;
    @Autowired
    private SeckillActivityListCacheService seckillActivityListCacheService;

    @Override
    public Response execute(SeckillActivityEvent seckillActivityEvent) {
        logger.info("cola|activityEvent|接收活动事件|{}", JSON.toJSON(seckillActivityEvent));
        if (seckillActivityEvent == null){
            logger.info("cola|activityEvent|事件参数错误" );
            return Response.buildSuccess();
        }
        seckillActivityCacheService.tryUpdateSeckillActivityCacheByLock(seckillActivityEvent.getId(), false);
        seckillActivityListCacheService.tryUpdateSeckillActivityCacheByLock(seckillActivityEvent.getStatus(), false);
        return Response.buildSuccess();
    }
}
