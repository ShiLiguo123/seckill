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

import cn.hutool.core.util.BooleanUtil;
import com.alibaba.fastjson.JSONObject;
import io.binghe.seckill.common.cache.distribute.DistributedCacheService;
import io.binghe.seckill.common.constants.SeckillConstants;
import io.binghe.seckill.common.model.message.TxMessage;
import io.binghe.seckill.order.application.place.SeckillPlaceOrderService;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 监听事务消息
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Component
@RocketMQTransactionListener(/*txProducerGroup = SeckillConstants.TX_ORDER_PRODUCER_GROUP,*/ rocketMQTemplateBeanName = "rocketMQTemplate")
public class OrderTxMessageListener implements RocketMQLocalTransactionListener{
    private final Logger logger = LoggerFactory.getLogger(OrderTxMessageListener.class);
    @Autowired
    private SeckillPlaceOrderService seckillPlaceOrderService;
    @Autowired
    private DistributedCacheService distributedCacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RocketMQLocalTransactionState executeLocalTransaction(Message message, Object o) {
        TxMessage txMessage = this.getTxMessage(message);
        try{
            //已经抛出了异常，则直接回滚
            if (BooleanUtil.isTrue(txMessage.getException())){
                return RocketMQLocalTransactionState.ROLLBACK;
            }
            seckillPlaceOrderService.saveOrderInTransaction(txMessage);
            logger.info("executeLocalTransaction|秒杀订单微服务成功提交本地事务|{}", txMessage.getTxNo());
            return RocketMQLocalTransactionState.COMMIT;
        }catch (Exception e){
            logger.error("executeLocalTransaction|秒杀订单微服务异常回滚事务|{}",txMessage.getTxNo());
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message message) {
        TxMessage txMessage = this.getTxMessage(message);
        logger.info("checkLocalTransaction|秒杀订单微服务查询本地事务|{}", txMessage.getTxNo());
        Boolean submitTransaction = distributedCacheService.hasKey(SeckillConstants.getKey(SeckillConstants.ORDER_TX_KEY, String.valueOf(txMessage.getTxNo())));
        return BooleanUtil.isTrue(submitTransaction) ? RocketMQLocalTransactionState.COMMIT : RocketMQLocalTransactionState.UNKNOWN ;
    }

    private TxMessage getTxMessage(Message msg){
        String messageString = new String((byte[]) msg.getPayload());
        JSONObject jsonObject = JSONObject.parseObject(messageString);
        String txStr = jsonObject.getString(SeckillConstants.MSG_KEY);
        return JSONObject.parseObject(txStr, TxMessage.class);
    }
}
