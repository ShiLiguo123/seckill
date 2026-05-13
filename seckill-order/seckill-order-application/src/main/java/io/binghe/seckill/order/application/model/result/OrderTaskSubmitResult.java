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
package io.binghe.seckill.order.application.model.result;

import io.binghe.seckill.common.exception.ErrorCode;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 订单任务提交结果
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public class OrderTaskSubmitResult {

    private boolean success;
    private Integer code;
    private String message;

    public static OrderTaskSubmitResult ok() {
        return new OrderTaskSubmitResult()
                .setSuccess(true);
    }

    public static OrderTaskSubmitResult failed(ErrorCode errorCode) {
        return new OrderTaskSubmitResult()
                .setSuccess(false)
                .setCode(errorCode.getCode())
                .setMessage(errorCode.getMesaage());
    }

    public OrderTaskSubmitResult() {
    }

    public OrderTaskSubmitResult(boolean success, Integer code, String message) {
        this.success = success;
        this.code = code;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public OrderTaskSubmitResult setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public Integer getCode() {
        return code;
    }

    public OrderTaskSubmitResult setCode(Integer code) {
        this.code = code;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public OrderTaskSubmitResult setMessage(String message) {
        this.message = message;
        return this;
    }
}
