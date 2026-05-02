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
package io.binghe.seckill.common.event.publisher.rocketmq;

import com.alibaba.fastjson.JSONObject;
import io.binghe.seckill.common.constants.SeckillConstants;
import io.binghe.seckill.common.event.SeckillBaseEvent;
import io.binghe.seckill.common.event.publisher.EventPublisher;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 基于RocketMQ发布事件
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Component
@ConditionalOnProperty(name = "event.publish.type", havingValue = "rocketmq")
public class RocketMQDomainEventPublisher implements EventPublisher {
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Override
    public void publish(SeckillBaseEvent domainEvent) {
        //发送失败消息给订单微服务
        rocketMQTemplate.send(domainEvent.getTopicEvent(), getEventMessage(domainEvent));
    }

    private Message<String> getEventMessage(SeckillBaseEvent domainEvent){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(SeckillConstants.EVENT_MSG_KEY, domainEvent);
        return MessageBuilder.withPayload(jsonObject.toJSONString()).build();
    }
}
