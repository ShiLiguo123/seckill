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
package io.binghe.seckill.gateway.filter;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.response.ResponseMessage;
import io.binghe.seckill.common.response.ResponseMessageBuilder;
import io.binghe.seckill.gateway.props.NotAuthUrlProperties;
import io.binghe.seckill.gateway.risk.rule.service.SeckillGatewayRuleChainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 基础过滤器
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public abstract class SeckillGatewayGlobalFilter implements GlobalFilter, Ordered {

    @Autowired
    private NotAuthUrlProperties notAuthUrlProperties;
    @Autowired
    private List<SeckillGatewayRuleChainService> seckillGatewayRuleChainServices;
    /**
     * 获取排序好的规则链
     */
    protected List<SeckillGatewayRuleChainService> getSeckillGatewayBaseRuleChainServices(){
        if (CollectionUtil.isEmpty(seckillGatewayRuleChainServices)){
            return Collections.emptyList();
        }
        return seckillGatewayRuleChainServices.stream().sorted(Comparator.comparing(SeckillGatewayRuleChainService::getOrder)).collect(Collectors.toList());
    }

    public boolean shouldSkip(String currentUrl) {
        PathMatcher pathMatcher = new AntPathMatcher();
        for (String skipPath : notAuthUrlProperties.getShouldSkipUrls()) {
            if (pathMatcher.match(skipPath, currentUrl)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 自定义Header
     */
    protected Mono<Void> chainFilterAndSetHeaders(GatewayFilterChain chain, ServerWebExchange exchange, Map<String, String> headerMap) {
        // 添加header
        Consumer<HttpHeaders> httpHeaders = httpHeader -> {
            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                // 遍历Map设置header，向后传递
                httpHeader.set(entry.getKey(), entry.getValue());
            }
        };

        ServerHttpRequest newRequest = exchange.getRequest().mutate().headers(httpHeaders).build();
        ServerWebExchange build = exchange.mutate().request(newRequest).build();
        //将现在的request 变成 exchange对象
        return chain.filter(build);
    }

    /**
     * 不调用过滤器链，快速返回
     */
    protected Mono<Void> fastFinish(ServerWebExchange exchange){
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        ResponseMessage<String> responseMessage = ResponseMessageBuilder.build(ErrorCode.RISK_CONTROL_INVALID.getCode(), ErrorCode.RISK_CONTROL_INVALID.getMesaage());
        DataBuffer dataBuffer = response.bufferFactory().allocateBuffer().write(JSON.toJSONString(responseMessage).getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(dataBuffer));
    }
    /**
     * 不调用过滤器链，快速返回
     */
    protected Mono<Void> fastFinish(ServerWebExchange exchange, ErrorCode errorCode){
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        ResponseMessage<String> responseMessage = ResponseMessageBuilder.build(errorCode.getCode(), errorCode.getMesaage());
        DataBuffer dataBuffer = response.bufferFactory().allocateBuffer().write(JSON.toJSONString(responseMessage).getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(dataBuffer));
    }
}
