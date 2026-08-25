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
package io.shiliguo.seckill.gateway.filter.impl;

import io.shiliguo.seckill.common.exception.ErrorCode;
import io.shiliguo.seckill.gateway.enums.SeckillGatewayFilterEnum;
import io.shiliguo.seckill.gateway.filter.SeckillGatewayGlobalFilter;
import io.shiliguo.seckill.gateway.risk.rule.service.SeckillGatewayRuleChainService;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @version 1.0.0
 * @description 风控
 */
@Component
public class SeckillGatewayRiskControlFilter extends SeckillGatewayGlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        List<SeckillGatewayRuleChainService> seckillGatewayBaseRuleChainServices = this.getSeckillGatewayBaseRuleChainServices();
        for (SeckillGatewayRuleChainService seckillGatewayRuleChainService : seckillGatewayBaseRuleChainServices){
            ErrorCode errorCode = seckillGatewayRuleChainService.execute(exchange);
            if (!ErrorCode.SUCCESS.getCode().equals(errorCode.getCode())){
                return fastFinish(exchange, errorCode);
            }
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return SeckillGatewayFilterEnum.RISKCONTROL.getCode();
    }
}
