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
package io.binghe.seckill.user.controller;

import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.model.dto.user.SeckillUserDTO;
import io.binghe.seckill.common.response.ResponseMessage;
import io.binghe.seckill.common.response.ResponseMessageBuilder;
import io.binghe.seckill.ratelimiter.concurrent.annotation.ConcurrentRateLimiter;
import io.binghe.seckill.user.application.service.SeckillUserService;
import io.binghe.seckill.user.domain.model.entity.SeckillUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 用户登录
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@RestController
@RequestMapping(value = "/user")
public class SeckillUserController {

    @Autowired
    private SeckillUserService seckillUserService;
    /**
     * 登录系统
     */
    @RequestMapping(value = "/login", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseMessage<String> login(@RequestBody SeckillUserDTO seckillUserDTO){
       return ResponseMessageBuilder.build(ErrorCode.SUCCESS.getCode(), seckillUserService.login(seckillUserDTO.getUserName(), seckillUserDTO.getPassword()));
    }
    /**
     * 获取用户信息
     */
    @RequestMapping(value = "/get", method = {RequestMethod.GET, RequestMethod.POST})
//    @SeckillRateLimiter(permitsPerSecond = 1, timeout = 0)
    @ConcurrentRateLimiter(name = "bhRateLimiter", queueCapacity = 0)
    public ResponseMessage<SeckillUser> get(@RequestParam String username){
       return ResponseMessageBuilder.build(ErrorCode.SUCCESS.getCode(), seckillUserService.getSeckillUserByUserName(username));
    }
}
