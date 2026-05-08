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
package io.binghe.seckill.application.message;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import io.binghe.seckill.application.service.SeckillGoodsService;
import io.binghe.seckill.common.constants.SeckillConstants;
import io.binghe.seckill.common.model.message.TxMessage;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 商品微服务事务消息
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Component
@RocketMQMessageListener(consumerGroup = SeckillConstants.TX_GOODS_CONSUMER_GROUP, topic = SeckillConstants.TOPIC_TX_MSG)
public class GoodsTxMessageListener implements RocketMQListener<String> {
    private final Logger logger = LoggerFactory.getLogger(GoodsTxMessageListener.class);
    @Autowired
    private SeckillGoodsService seckillGoodsService;
    @Override
    public void onMessage(String message) {
        if (StrUtil.isEmpty(message)){
            return;
        }
        logger.info("秒杀商品微服务开始消费事务消息:{}", message);
        TxMessage txMessage = this.getTxMessage(message);
        //如果协调的异常信息字段为false，订单微服务没有抛出异常，则处理库存信息
        if (BooleanUtil.isFalse(txMessage.getException())){
            seckillGoodsService.updateAvailableStock(txMessage);
        }
    }

    private TxMessage getTxMessage(String msg){
        JSONObject jsonObject = JSONObject.parseObject(msg);
        String txStr = jsonObject.getString(SeckillConstants.MSG_KEY);
        return JSONObject.parseObject(txStr, TxMessage.class);
    }
}
