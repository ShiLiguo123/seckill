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
package io.binghe.seckill.gateway.filter.impl;

import cn.hutool.core.util.StrUtil;
import io.binghe.seckill.common.constants.SeckillConstants;
import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.exception.SeckillException;
import io.binghe.seckill.common.shiro.utils.JwtUtils;
import io.binghe.seckill.gateway.enums.SeckillGatewayFilterEnum;
import io.binghe.seckill.gateway.filter.SeckillGatewayGlobalFilter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description SeckillGatewayFilter
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Component
public class SeckillGatewayAuthFilter extends SeckillGatewayGlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String currentUrl = request.getURI().getPath();
        if (shouldSkip(currentUrl)){
            return chain.filter(exchange);
        }
        HttpHeaders headers = request.getHeaders();
        String userIdStr = headers.getFirst(SeckillConstants.USER_ID);
        if (!StrUtil.isEmpty(userIdStr)){
            return chain.filter(exchange);
        }

        String token = headers.getFirst(SeckillConstants.TOKEN_HEADER_NAME);
        if (StringUtils.isEmpty(token)){
            throw new SeckillException(ErrorCode.USER_NOT_LOGIN);
        }
        Long userId = JwtUtils.getUserId(token);
        if (userId == null){
            throw new SeckillException(ErrorCode.USER_NOT_LOGIN);
        }
        request = request.mutate().header(SeckillConstants.USER_ID, String.valueOf(userId)).build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return SeckillGatewayFilterEnum.AUTH.getCode();
    }
}
