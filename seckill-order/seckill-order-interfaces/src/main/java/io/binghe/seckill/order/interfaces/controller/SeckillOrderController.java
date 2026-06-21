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
package io.binghe.seckill.order.interfaces.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.model.dto.order.SeckillOrderSubmitDTO;
import io.binghe.seckill.common.response.ResponseMessage;
import io.binghe.seckill.common.response.ResponseMessageBuilder;
import io.binghe.seckill.order.application.model.command.SeckillOrderCommand;
import io.binghe.seckill.order.application.model.command.SeckillOrderTaskCommand;
import io.binghe.seckill.order.application.service.SeckillOrderService;
import io.binghe.seckill.order.application.service.SeckillSubmitOrderService;
import io.binghe.seckill.order.domain.model.entity.SeckillOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 订单
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */

@RestController
@RequestMapping(value = "/order")
public class SeckillOrderController {
    @Autowired
    private SeckillOrderService seckillOrderService;
    @Autowired
    private SeckillSubmitOrderService seckillSubmitOrderService;

    /**
     * 保存秒杀订单
     */
    @RequestMapping(value = "/saveSeckillOrder", method = {RequestMethod.GET,RequestMethod.POST})
    @SentinelResource(value = "SAVE-DATA-FLOW")
    public ResponseMessage<SeckillOrderSubmitDTO> saveSeckillOrder(@RequestAttribute Long userId, SeckillOrderCommand seckillOrderCommand){
        SeckillOrderSubmitDTO seckillOrderSubmitDTO = seckillSubmitOrderService.saveSeckillOrder(userId, seckillOrderCommand);
        return ResponseMessageBuilder.build(ErrorCode.SUCCESS.getCode(), seckillOrderSubmitDTO);
    }
    /**
     * 获取用户维度的订单列表
     */
    @RequestMapping(value = "/getSeckillOrderByUserId", method = {RequestMethod.GET,RequestMethod.POST})
    public ResponseMessage<List<SeckillOrder>> getSeckillOrderByUserId(Long userId){
        List<SeckillOrder> seckillOrderList = seckillOrderService.getSeckillOrderByUserId(userId);
        return ResponseMessageBuilder.build(ErrorCode.SUCCESS.getCode(), seckillOrderList);
    }

    /**
     * 获取商品维度的订单列表
     */
    @RequestMapping(value = "/getSeckillOrderByGoodsId", method = {RequestMethod.GET,RequestMethod.POST})
    public ResponseMessage<List<SeckillOrder>> getSeckillOrderByGoodsId(Long goodsId){
        List<SeckillOrder> seckillOrderList = seckillOrderService.getSeckillOrderByGoodsId(goodsId);
        return ResponseMessageBuilder.build(ErrorCode.SUCCESS.getCode(), seckillOrderList);
    }

    /**
     * 异步下单时，会返回任务id，如果下单返回的是任务id，则调用此接口获取订单id，如果前端多次调用此接口返回的数据仍为为任务id，则表示下单失败，不再调用
     * 如果调用此接口（含重试），返回了订单id，则异步下单获取到了与同步下单相同的数据，就可以通过订单id来查询订单信息了。
     */
    @RequestMapping(value = "/getSeckillOrderSubmitDTO", method = {RequestMethod.GET,RequestMethod.POST})
    public ResponseMessage<SeckillOrderSubmitDTO> get(@RequestAttribute Long userId, SeckillOrderTaskCommand seckillOrderTaskCommand){
        SeckillOrderSubmitDTO seckillOrderSubmitDTO = seckillOrderService.getSeckillOrderSubmitDTOByTaskId(seckillOrderTaskCommand.getOrderTaskId(), userId, seckillOrderTaskCommand.getGoodsId());
        return ResponseMessageBuilder.build(ErrorCode.SUCCESS.getCode(), seckillOrderSubmitDTO);
    }
}
