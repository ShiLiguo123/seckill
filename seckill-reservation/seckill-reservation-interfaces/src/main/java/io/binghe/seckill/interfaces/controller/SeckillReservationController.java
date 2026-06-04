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

import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.response.ResponseMessage;
import io.binghe.seckill.common.response.ResponseMessageBuilder;
import io.binghe.seckill.reservation.application.command.SeckillReservationConfigCommand;
import io.binghe.seckill.reservation.application.service.SeckillReservationService;
import io.binghe.seckill.reservation.domain.model.entity.SeckillReservationConfig;
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
     * 获取预约配置详情
     */
    @RequestMapping(value = "/config/getConfigDetail", method = {RequestMethod.GET,RequestMethod.POST})
    public ResponseMessage<SeckillReservationConfig> getConfigDetail(Long goodsId, Long version){
        SeckillReservationConfig serviceConfigDetail = seckillReservationService.getConfigDetail(goodsId, version);
        return ResponseMessageBuilder.build(ErrorCode.SUCCESS.getCode(), serviceConfigDetail);
    }
}
