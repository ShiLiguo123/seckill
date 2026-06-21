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
package io.binghe.seckill.interfaces.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.response.ResponseMessage;
import io.binghe.seckill.common.response.ResponseMessageBuilder;
import io.binghe.seckill.reservation.application.command.SeckillReservationConfigCommand;
import io.binghe.seckill.reservation.application.command.SeckillReservationUserCommand;
import io.binghe.seckill.reservation.application.service.SeckillReservationService;
import io.binghe.seckill.reservation.domain.model.entity.SeckillReservationConfig;
import io.binghe.seckill.reservation.domain.model.entity.SeckillReservationUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description SeckillReservationController
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */

@RestController
@RequestMapping(value = "/reservation")
public class SeckillReservationController {

    @Autowired
    private SeckillReservationService seckillReservationService;

    /**
     * 保存预约配置信息
     */
    @RequestMapping(value = "/config/saveSeckillReservationConfig", method = {RequestMethod.GET,RequestMethod.POST})
    public ResponseMessage<String> saveSeckillReservationConfig(@RequestBody SeckillReservationConfigCommand seckillReservationConfigCommand){
        seckillReservationService.saveSeckillReservationConfig(seckillReservationConfigCommand);
        return ResponseMessageBuilder.build(ErrorCode.SUCCESS.getCode());
    }

    /**
     * 更新预约配置信息
     */
    @RequestMapping(value = "/config/updateSeckillReservationConfig", method = {RequestMethod.GET,RequestMethod.POST})
    public ResponseMessage<String> updateSeckillReservationConfig(@RequestBody SeckillReservationConfigCommand seckillReservationConfigCommand){
        seckillReservationService.updateSeckillReservationConfig(seckillReservationConfigCommand);
        return ResponseMessageBuilder.build(ErrorCode.SUCCESS.getCode());
    }

    /**
     * 更新预约配置状态
     */
    @RequestMapping(value = "/config/updateConfigStatus", method = {RequestMethod.GET,RequestMethod.POST})
    public ResponseMessage<String> updateConfigStatus(Integer status, Long goodsId){
        seckillReservationService.updateConfigStatus(status, goodsId);
        return ResponseMessageBuilder.build(ErrorCode.SUCCESS.getCode());
    }

    /**
     * 获取预约配置列表
     */
    @RequestMapping(value = "/config/getConfigList", method = {RequestMethod.GET,RequestMethod.POST})
    public ResponseMessage<List<SeckillReservationConfig>> getConfigList(Long version){
        List<SeckillReservationConfig> serviceConfigList = seckillReservationService.getConfigList(version);
        return ResponseMessageBuilder.build(ErrorCode.SUCCESS.getCode(), serviceConfigList);
    }

    /**
     * 获取预约配置详情，此接口可在商详页也不调用，如果正常展示数据，则说明商品开通了预约通道，需要提前预约再进行秒杀抢购下单，如果数据为空，则说明商品未开通预约通道，无需提前预约即可下单
     */
    @RequestMapping(value = "/config/getConfigDetail", method = {RequestMethod.GET,RequestMethod.POST})
    public ResponseMessage<SeckillReservationConfig> getConfigDetail(Long goodsId, Long version){
        SeckillReservationConfig serviceConfigDetail = seckillReservationService.getConfigDetail(goodsId, version);
        return ResponseMessageBuilder.build(ErrorCode.SUCCESS.getCode(), serviceConfigDetail);
    }

    /**
     * 根据商品id查看预约用户列表
     */
    @RequestMapping(value = "/user/getUserListByGoodsId", method = {RequestMethod.GET,RequestMethod.POST})
    public ResponseMessage<List<SeckillReservationUser>> getUserListByGoodsId(Long goodsId, Long version){
        List<SeckillReservationUser> serviceUserList = seckillReservationService.getUserListByGoodsId(goodsId, version);
        return ResponseMessageBuilder.build(ErrorCode.SUCCESS.getCode(), serviceUserList);
    }

    /**
     * 根据用户id查看预约的商品列表
     */
    @RequestMapping(value = "/user/getGoodsListByUserId", method = {RequestMethod.GET,RequestMethod.POST})
    public ResponseMessage<List<SeckillReservationUser>> getGoodsListByUserId(Long userId, Long version){
        List<SeckillReservationUser> serviceUserList = seckillReservationService.getGoodsListByUserId(userId, version);
        return ResponseMessageBuilder.build(ErrorCode.SUCCESS.getCode(), serviceUserList);
    }

    /**
     * 预约秒杀商品
     */
    @RequestMapping(value = "/user/reserveGoods", method = {RequestMethod.GET,RequestMethod.POST})
    @SentinelResource(value = "SAVE-DATA-FLOW")
    public ResponseMessage<String> reserveGoods(@RequestBody SeckillReservationUserCommand seckillReservationUserCommand){
        seckillReservationService.reserveGoods(seckillReservationUserCommand);
        return ResponseMessageBuilder.build(ErrorCode.SUCCESS.getCode());
    }
    /**
     * 取消预约秒杀商品
     */
    @RequestMapping(value = "/user/cancelReserveGoods", method = {RequestMethod.GET,RequestMethod.POST})
    @SentinelResource(value = "SAVE-DATA-FLOW")
    public ResponseMessage<String> cancelReserveGoods(@RequestBody SeckillReservationUserCommand seckillReservationUserCommand){
        seckillReservationService.cancelReserveGoods(seckillReservationUserCommand);
        return ResponseMessageBuilder.build(ErrorCode.SUCCESS.getCode());
    }

    /**
     * 获取用户预约的某个商品信息
     */
    @RequestMapping(value = "/user/getSeckillReservationUser", method = {RequestMethod.GET,RequestMethod.POST})
    public ResponseMessage<SeckillReservationUser> getSeckillReservationUser(@RequestBody SeckillReservationUserCommand seckillReservationUserCommand){
        SeckillReservationUser seckillReservationUser = seckillReservationService.getSeckillReservationUser(seckillReservationUserCommand);
        return ResponseMessageBuilder.build(ErrorCode.SUCCESS.getCode(), seckillReservationUser);
    }
}
