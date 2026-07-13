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
import io.binghe.seckill.common.response.ResponseMessage;
import io.binghe.seckill.common.response.ResponseMessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description NacosController
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@RestController
@RefreshScope
@RequestMapping(value = "/nacos")
public class NacosController {

    private final Logger logger = LoggerFactory.getLogger(NacosController.class);

    @Autowired
    private ConfigurableApplicationContext context;

    @Value("${seckill.author.name}")
    private String nacosAuthorName;

    @GetMapping("/test")
    public ResponseMessage<String> nacosTest(){
        String authorName = context.getEnvironment().getProperty("seckill.author.name");
        logger.info("获取到的作者姓名为：{}", authorName);
        return ResponseMessageBuilder.build(ErrorCode.SUCCESS.getCode(), authorName);
    }

    @GetMapping("/name")
    public ResponseMessage<String> nacosName(){
        logger.info("从Nacos中获取到的作者的姓名为：{}", nacosAuthorName);
        return ResponseMessageBuilder.build(ErrorCode.SUCCESS.getCode(), nacosAuthorName);
    }
}
