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
 * @description 账户规则链服务
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Component
public class SeckillGatewayAccountRuleChainService extends SeckillGatewayBaseRuleChainService implements SeckillGatewayRuleChainService {
    private final Logger logger = LoggerFactory.getLogger(SeckillGatewayAccountRuleChainService.class);
    @Override
    public ErrorCode execute(ServerWebExchange exchange) {
        Rule rule = seckillGatewayRulesConfigFactoty.getAccountRule();
        if (!rule.isEnabled()){
            return ErrorCode.SUCCESS;
        }
        try{
            //TODO 对接其他服务验证账号
            return ErrorCode.SUCCESS;
        }catch (Exception e){
            logger.error("SeckillGatewayAccountRuleChainService|账户限制异常|{}", e.getMessage());
            return ErrorCode.RISK_CONTROL_ACCOUNT_INVALID;
        }
    }


    @Override
    public String getServiceName() {
        return SeckillGatewayRuleEnum.ACCOUNT.getMesaage();
    }


    @Override
    public int getOrder() {
        return SeckillGatewayRuleEnum.ACCOUNT.getCode();
    }
}
