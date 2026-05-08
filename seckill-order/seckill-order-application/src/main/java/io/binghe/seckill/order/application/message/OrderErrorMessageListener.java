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
import io.binghe.seckill.common.model.message.ErrorMessage;
import io.binghe.seckill.order.application.service.SeckillOrderService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 监听异常消息
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Component
@RocketMQMessageListener(consumerGroup = SeckillConstants.TX_ORDER_CONSUMER_GROUP, topic = SeckillConstants.TOPIC_ERROR_MSG)
public class OrderErrorMessageListener implements RocketMQListener<String> {
    private final Logger logger = LoggerFactory.getLogger(OrderErrorMessageListener.class);
    @Autowired
    private SeckillOrderService seckillOrderService;
    @Override
    public void onMessage(String message) {
        logger.info("onMessage|秒杀订单微服务开始消费消息:{}", message);
        if (StrUtil.isEmpty(message)){
            return;
        }
        //删除数据库中对应的订单
        seckillOrderService.deleteOrder(this.getErrorMessage(message));
    }

    private ErrorMessage getErrorMessage(String msg){
        JSONObject jsonObject = JSONObject.parseObject(msg);
        String txStr = jsonObject.getString(SeckillConstants.MSG_KEY);
        return JSONObject.parseObject(txStr, ErrorMessage.class);
    }
}
