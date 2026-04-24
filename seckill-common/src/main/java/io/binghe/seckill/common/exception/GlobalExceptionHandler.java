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
package io.binghe.seckill.common.exception;

import io.binghe.seckill.common.response.ResponseMessage;
import io.binghe.seckill.common.response.ResponseMessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 全局统一异常处理器
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    /**
     * 全局异常处理，统一返回状态码
     */
    @ExceptionHandler(SeckillException.class)
    public ResponseMessage<String> handleSeckillException(SeckillException e) {
        logger.error("服务器抛出了异常：{}", e);
        return ResponseMessageBuilder.build(e.getCode(), e.getMessage());
    }
    /**
     * 全局异常处理，统一返回状态码
     */
    @ExceptionHandler(Exception.class)
    public ResponseMessage<String> handleException(Exception e) {
        logger.error("服务器抛出了异常：{}", e);
        return ResponseMessageBuilder.build(ErrorCode.SERVER_EXCEPTION.getCode(), e.getMessage());
    }


}
