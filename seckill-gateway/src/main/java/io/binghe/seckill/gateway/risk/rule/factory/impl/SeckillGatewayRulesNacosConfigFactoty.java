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
package io.binghe.seckill.gateway.risk.rule.factory.impl;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.config.listener.Listener;
import io.binghe.seckill.gateway.risk.rule.config.SeckillGatewayRulesConfig;
import io.binghe.seckill.gateway.risk.rule.factory.SeckillGatewayRulesConfigFactoty;
import io.binghe.seckill.gateway.risk.rule.model.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.Executor;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description SeckillGatewayRulesNacosConfigFactoty
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Component
@RefreshScope
public class SeckillGatewayRulesNacosConfigFactoty implements SeckillGatewayRulesConfigFactoty, InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(SeckillGatewayRulesNacosConfigFactoty.class);

    private static final String NACOS_DATA_ID = "seckill-gateway-rules";

    @Value("${spring.cloud.nacos.config.group}")
    private String group;

    @Resource
    private NacosConfigManager nacosConfigManager;

    private SeckillGatewayRulesConfig seckillGatewayRulesConfig;

    @Override
    public Rule getIpRule() {
        if (seckillGatewayRulesConfig == null){
            return new Rule().setEnabled(false);
        }
        return seckillGatewayRulesConfig.getIpRule();
    }

    @Override
    public Rule getAccountRule(){
        if (seckillGatewayRulesConfig == null){
            return new Rule().setEnabled(false);
        }
        return seckillGatewayRulesConfig.getAccountRule();
    }

    @Override
    public Rule getPathRule(String path) {
        if (seckillGatewayRulesConfig == null){
            return new Rule().setEnabled(false);
        }
        return seckillGatewayRulesConfig.getPathRule(path);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        nacosConfigManager.getConfigService().addListener(NACOS_DATA_ID, group, new Listener() {
            @Override
            public Executor getExecutor() {
                return null;
            }
            @Override
            public void receiveConfigInfo(String configInfo) {
                logger.info("SeckillGatewayRulesConfiguration|动态感知风控配置|{}", configInfo);
                seckillGatewayRulesConfig = JSON.parseObject(configInfo, SeckillGatewayRulesConfig.class);
            }
        });
    }
}
