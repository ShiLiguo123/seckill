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
package io.binghe.seckill.gateway.risk.rule.service.base;

import io.binghe.seckill.common.constants.SeckillConstants;
import io.binghe.seckill.common.shiro.utils.JwtUtils;
import io.binghe.seckill.gateway.risk.rule.factory.SeckillGatewayRulesConfigFactoty;
import io.binghe.seckill.gateway.risk.window.SlidingWindowLimitService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 基础服务
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public abstract class SeckillGatewayBaseRuleChainService {
    private static final Logger logger = LoggerFactory.getLogger(SeckillGatewayBaseRuleChainService.class);
    @Autowired
    protected SlidingWindowLimitService slidingWindowLimitService;
    @Autowired
    protected SeckillGatewayRulesConfigFactoty seckillGatewayRulesConfigFactoty;

    public SeckillGatewayBaseRuleChainService(){
        logger.info("SeckillGatewayBaseRuleChainService|当前风控服务|{}", this.getServiceName());
    }

    /**
     * 获取ip地址
     */
    protected String getIp(ServerWebExchange serverWebExchange){
        ServerHttpRequest request = serverWebExchange.getRequest();
        HttpHeaders headers = request.getHeaders();
        String ip = headers.getFirst("x-forwarded-for");
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个ip值，第一个ip才是真实ip
            if (ip.indexOf(",") != -1) {
                ip = ip.split(",")[0];
            }
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.getFirst("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.getFirst("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.getFirst("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.getFirst("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.getFirst("X-Real-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddress().getAddress().getHostAddress();
        }

        return ip.replaceAll(":", ".");
    }

    /**
     * 获取用户id
     */
    protected Long getUserId(ServerWebExchange exchange){
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();
        String token = headers.getFirst(SeckillConstants.TOKEN_HEADER_NAME);
        if (StringUtils.isEmpty(token)){
            return null;
        }
        return JwtUtils.getUserId(token);
    }

    /**
     * 当前服务的服务名称
     */
    public abstract String getServiceName();
}
