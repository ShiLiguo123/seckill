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
package io.binghe.seckill.mq;

import io.binghe.seckill.common.model.message.TopicMessage;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.springframework.messaging.MessagingException;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 消息队列服务
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public interface MessageSenderService {

    /**
     * 发送消息
     * @param message 发送的消息
     */
    void send(TopicMessage message);

    /**
     * 发送事务消息，主要是RocketMQ
     * @param message 事务消息
     * @param arg 其他参数
     * @return 返回事务发送结果
     */
    default TransactionSendResult sendMessageInTransaction(TopicMessage message, Object arg){
        return null;
    }
}
