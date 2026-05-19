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
package io.binghe.seckill.stock.application.event.handler;

import com.alibaba.cola.dto.Response;
import com.alibaba.cola.event.EventHandler;
import com.alibaba.cola.event.EventHandlerI;
import com.alibaba.fastjson.JSON;
import io.binghe.seckill.stock.application.cache.SeckillStockBucketCacheService;
import io.binghe.seckill.stock.domain.event.SeckillStockBucketEvent;
import io.binghe.seckill.stock.domain.model.enums.SeckillStockBucketEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 基于Cola的库存事件处理器
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@EventHandler
@ConditionalOnProperty(name = "message.mq.type", havingValue = "cola")
public class SeckillStockColaEventHandler implements EventHandlerI<Response, SeckillStockBucketEvent> {
    private final Logger logger = LoggerFactory.getLogger(SeckillStockColaEventHandler.class);
    @Autowired
    private SeckillStockBucketCacheService seckillStockBucketCacheService;
    @Override
    public Response execute(SeckillStockBucketEvent seckillStockBucketEvent) {
        logger.info("cola|stockEvent|接收库存事件|{}", JSON.toJSON(seckillStockBucketEvent));
        if (seckillStockBucketEvent == null || seckillStockBucketEvent.getId() == null){
            logger.info("cola|stockEvent|订单参数错误");
            return Response.buildSuccess();
        }
        //开启了库存分桶，就更新缓存数据
        if (SeckillStockBucketEventType.ENABLED.getCode().equals(seckillStockBucketEvent.getStatus())){
            seckillStockBucketCacheService.tryUpdateSeckillStockBucketCacheByLock(seckillStockBucketEvent.getId(), false);
        }
        return Response.buildSuccess();
    }
}
