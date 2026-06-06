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

import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.exception.SeckillException;
import io.binghe.seckill.common.model.dto.goods.SeckillGoodsDTO;
import io.binghe.seckill.dubbo.interfaces.goods.SeckillGoodsDubboService;
import io.binghe.seckill.dubbo.interfaces.reservation.SeckillReservationDubboService;
import io.binghe.seckill.order.application.model.command.SeckillOrderCommand;
import io.binghe.seckill.order.application.place.SeckillPlaceOrderService;
import io.binghe.seckill.order.application.security.SecurityService;
import io.binghe.seckill.order.application.service.SeckillSubmitOrderService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 提交订单基础实现类
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public abstract class SeckillBaseSubmitOrderServiceImpl implements SeckillSubmitOrderService {
    @Autowired
    private SecurityService securityService;

    @DubboReference(version = "1.0.0", check = false)
    private SeckillGoodsDubboService seckillGoodsDubboService;

    @Autowired
    protected SeckillPlaceOrderService seckillPlaceOrderService;

    @DubboReference(version = "1.0.0", check = false)
    private SeckillReservationDubboService seckillReservationDubboService;

    @Override
    public void checkSeckillOrder(Long userId, SeckillOrderCommand seckillOrderCommand) {
        if (userId == null || seckillOrderCommand == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        //模拟风控
        if (!securityService.securityPolicy(userId)){
            throw new SeckillException(ErrorCode.USER_INVALID);
        }
        //获取商品信息
        SeckillGoodsDTO seckillGoods = seckillGoodsDubboService.getSeckillGoods(seckillOrderCommand.getGoodsId(), seckillOrderCommand.getVersion());
        //检测商品信息
        seckillPlaceOrderService.checkSeckillGoods(seckillOrderCommand, seckillGoods);
        //通过预约服务检测是否可以正常下单
        if (!seckillReservationDubboService.checkReservation(userId, seckillOrderCommand.getGoodsId())){
            throw new SeckillException(ErrorCode.GOODS_RESERVATION_NOT_RESERVE);
        }
    }
}
