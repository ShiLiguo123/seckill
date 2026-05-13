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
package io.binghe.seckill.order.application.model.task;

import cn.hutool.core.util.StrUtil;
import io.binghe.seckill.common.model.message.TopicMessage;
import io.binghe.seckill.order.application.model.command.SeckillOrderCommand;
import io.binghe.seckill.order.domain.model.entity.SeckillOrder;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 异步下单提交的订单任务
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public class SeckillOrderTask extends TopicMessage {
    //订单任务id
    private String orderTaskId;
    //用户id
    private Long userId;
    //提交的订单数据
    private SeckillOrderCommand seckillOrderCommand;

    public SeckillOrderTask() {
    }

    public SeckillOrderTask(String destination, String orderTaskId, Long userId, SeckillOrderCommand seckillOrderCommand) {
        super(destination);
        this.orderTaskId = orderTaskId;
        this.userId = userId;
        this.seckillOrderCommand = seckillOrderCommand;
    }

    public String getOrderTaskId() {
        return orderTaskId;
    }

    public void setOrderTaskId(String orderTaskId) {
        this.orderTaskId = orderTaskId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public SeckillOrderCommand getSeckillOrderCommand() {
        return seckillOrderCommand;
    }

    public void setSeckillOrderCommand(SeckillOrderCommand seckillOrderCommand) {
        this.seckillOrderCommand = seckillOrderCommand;
    }

    public boolean isEmpty(){
        return StrUtil.isEmpty(this.getDestination())
                || StrUtil.isEmpty(orderTaskId)
                || userId == null
                || seckillOrderCommand == null;
    }
}
