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
package io.binghe.seckill.gateway.risk.rule.service.impl;

import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.utils.string.StringUtil;
import io.binghe.seckill.gateway.enums.SeckillGatewayRuleEnum;
import io.binghe.seckill.gateway.risk.rule.model.Rule;
import io.binghe.seckill.gateway.risk.rule.service.SeckillGatewayRuleChainService;
import io.binghe.seckill.gateway.risk.rule.service.base.SeckillGatewayBaseRuleChainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 基于IP的过滤规则
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Component
public class SeckillGatewayPathRuleChainService extends SeckillGatewayBaseRuleChainService implements SeckillGatewayRuleChainService {
    private final Logger logger = LoggerFactory.getLogger(SeckillGatewayPathRuleChainService.class);

    @Override
    public ErrorCode execute(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        Rule rule = seckillGatewayRulesConfigFactoty.getPathRule(path);
        if (!rule.isEnabled()){
            return ErrorCode.SUCCESS;
        }
        try{
            //获取用户id
            Long userId = this.getUserId(exchange);
            //用户id不为空
            if (userId != null){
                String userPath = StringUtil.append(userId, path);
                boolean result = slidingWindowLimitService.passThough(userPath, rule.getWindowPeriod(), rule.getWindowSize());
                return result ? ErrorCode.SUCCESS : ErrorCode.RISK_CONTROL_PATH_INVALID;
            }
        }catch (Exception e){
            logger.error("SeckillGatewayPathRuleChainService|资源限制异常|{}", e.getMessage());
            return ErrorCode.RISK_CONTROL_PATH_INVALID;
        }
        return ErrorCode.SUCCESS;
    }

    @Override
    public int getOrder() {
        return SeckillGatewayRuleEnum.PATH.getCode();
    }

    @Override
    public String getServiceName() {
        return SeckillGatewayRuleEnum.PATH.getMesaage();
    }
}
