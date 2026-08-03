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
package io.binghe.seckill.gateway.risk.rule.service;

import io.binghe.seckill.common.exception.ErrorCode;
import org.springframework.web.server.ServerWebExchange;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 网关调用链服务
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public interface SeckillGatewayRuleChainService {

    /**
     * 执行调用链服务
     */
    ErrorCode execute(ServerWebExchange exchange);

    /**
     * 调用链的顺序
     */
    int getOrder();
}
