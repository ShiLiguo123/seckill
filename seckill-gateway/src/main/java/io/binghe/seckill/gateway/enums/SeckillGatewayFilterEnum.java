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
package io.binghe.seckill.gateway.enums;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 排序
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public enum SeckillGatewayFilterEnum {

    TRANCE(0, "链路追踪过滤器"),
    RISKCONTROL(1, "风控过滤器"),
    AUTH(2, "登录认证过滤器");

    private final Integer code;
    private final String mesaage;

    SeckillGatewayFilterEnum(Integer code, String mesaage) {
        this.code = code;
        this.mesaage = mesaage;
    }

    public Integer getCode() {
        return code;
    }

    public String getMesaage() {
        return mesaage;
    }
}
