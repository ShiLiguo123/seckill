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
package io.binghe.seckill.order.application.message;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import io.binghe.seckill.common.constants.SeckillConstants;
import io.binghe.seckill.order.application.model.task.SeckillOrderTask;
import io.binghe.seckill.order.application.service.SeckillSubmitOrderService;
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
 * @description 订单任务监听类
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */

@Component
@ConditionalOnProperty(name = "submit.order.type", havingValue = "async")
@RocketMQMessageListener(consumerGroup = SeckillConstants.SUBMIT_ORDER_CONSUMER_GROUP, topic = SeckillConstants.TOPIC_ORDER_MSG)
public class OrderTaskConsumerListener implements RocketMQListener<String> {

    private final Logger logger = LoggerFactory.getLogger(OrderTaskConsumerListener.class);

    @Autowired
    private SeckillSubmitOrderService seckillSubmitOrderService;

    @Override
    public void onMessage(String message) {
        logger.info("onMessage|秒杀订单微服务接收异步订单任务消息:{}", message);
        if (StrUtil.isEmpty(message)){
            logger.info("onMessage|秒杀订单微服务接收异步订单任务消息为空:{}", message);
            return;
        }
        SeckillOrderTask seckillOrderTask = this.getTaskMessage(message);
        if (seckillOrderTask.isEmpty()){
            logger.info("onMessage|秒杀订单微服务接收异步订单任务消息转换成任务对象为空{}", message);
            return;
        }
        logger.info("onMessage|处理下单任务:{}", seckillOrderTask.getOrderTaskId());
        seckillSubmitOrderService.handlePlaceOrderTask(seckillOrderTask);
    }

    private SeckillOrderTask getTaskMessage(String msg){
        JSONObject jsonObject = JSONObject.parseObject(msg);
        String txStr = jsonObject.getString(SeckillConstants.MSG_KEY);
        return JSONObject.parseObject(txStr, SeckillOrderTask.class);
    }
}
