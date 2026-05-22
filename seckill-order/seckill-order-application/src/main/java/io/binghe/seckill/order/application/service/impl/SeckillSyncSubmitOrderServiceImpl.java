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
package io.binghe.seckill.order.application.service.impl;

import io.binghe.seckill.common.constants.SeckillConstants;
import io.binghe.seckill.common.model.dto.order.SeckillOrderSubmitDTO;
import io.binghe.seckill.order.application.model.command.SeckillOrderCommand;
import io.binghe.seckill.order.application.service.SeckillSubmitOrderService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 同步提交订单
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Service
@ConditionalOnProperty(name = "submit.order.type", havingValue = "sync")
public class SeckillSyncSubmitOrderServiceImpl extends SeckillBaseSubmitOrderServiceImpl implements SeckillSubmitOrderService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillOrderSubmitDTO saveSeckillOrder(Long userId, SeckillOrderCommand seckillOrderCommand) {
        //进行基本的检查
        this.checkSeckillOrder(userId, seckillOrderCommand);
        return new SeckillOrderSubmitDTO(String.valueOf(seckillPlaceOrderService.placeOrder(userId, seckillOrderCommand)), seckillOrderCommand.getGoodsId(), SeckillConstants.TYPE_ORDER);
    }
}
