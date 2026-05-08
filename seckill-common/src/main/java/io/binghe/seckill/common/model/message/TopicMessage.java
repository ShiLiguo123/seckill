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
package io.binghe.seckill.common.model.message;

import com.alibaba.cola.event.DomainEventI;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 基础消息
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public class TopicMessage implements DomainEventI {
    /**
     * 消息目的地，可以是消息主题
     */
    private String destination;

    public TopicMessage(){
    }

    public TopicMessage(String destination) {
        this.destination = destination;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
}
