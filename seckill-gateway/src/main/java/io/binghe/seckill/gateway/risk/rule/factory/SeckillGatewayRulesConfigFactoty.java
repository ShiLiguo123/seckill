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
package io.binghe.seckill.gateway.risk.rule.factory;

import io.binghe.seckill.gateway.risk.rule.model.Rule;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 风控规则配置Factoty
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public interface SeckillGatewayRulesConfigFactoty {

    /**
     * IP校验规则
     */
    Rule getIpRule();

    /**
     * 账户校验规则
     */
    Rule getAccountRule();

    /**
     * 路径校验规则
     */
    Rule getPathRule(String path);
}
