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
package io.binghe.seckill.gateway.risk.rule.config;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import io.binghe.seckill.gateway.risk.rule.model.Rule;
import io.binghe.seckill.gateway.risk.rule.model.PathRule;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 网关规则配置
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public class SeckillGatewayRulesConfig {
    /**
     * 是否开启
     */
    private boolean enabled;

    /**
     * 基于ip的规则
     */
    private Rule ipRule;

    /**
     * 基于路径的规则
     */
    private PathRule pathRule;

    /**
     * 基于账户的规则
     */
    private Rule accountRule;

    public Rule getIpRule() {
        if (ipRule == null || !ipRule.isEnabled() || !this.isEnabled()){
            return new Rule().setEnabled(false);
        }
        return new Rule()
                .setEnabled(ipRule.isEnabled())
                .setWindowPeriod(ipRule.getWindowPeriod())
                .setWindowSize(ipRule.getWindowSize());
    }

    public Rule getAccountRule(){
        if (accountRule == null || !accountRule.isEnabled() || !this.isEnabled()){
            new Rule().setEnabled(false);
        }
        return new Rule().setEnabled(accountRule.isEnabled());
    }

    public Rule getPathRule(String path) {
        if (StrUtil.isEmpty(path) || pathRule == null || CollectionUtil.isEmpty(pathRule.getUrlPaths())){
            return new Rule().setEnabled(false);
        }
        if (!pathRule.isEnabled() || !this.isEnabled()){
            return new Rule().setEnabled(false);
        }
        for (Rule rule : pathRule.getUrlPaths()){
            if (StrUtil.isEmpty(rule.getPath())){
                continue;
            }
            if (this.isPathMatch(rule.getPath(), path)){
                return new Rule()
                        .setEnabled(rule.isEnabled())
                        .setPath(rule.getPath())
                        .setWindowPeriod(rule.getWindowPeriod() <= 0 ? pathRule.getWindowPeriod() : rule.getWindowPeriod())
                        .setWindowSize(rule.getWindowSize() <= 0 ? pathRule.getWindowSize() : rule.getWindowSize());
            }
        }
        return new Rule().setEnabled(false);
    }


    private boolean isPathMatch(String uriTemplate, String path) {
        PathPatternParser parser = new PathPatternParser();
        parser.setMatchOptionalTrailingSeparator(true);
        PathPattern pathPattern = parser.parse(uriTemplate);
        PathContainer pathContainer = toPathContainer(path);
        return pathPattern.matches(pathContainer);
    }

    private PathContainer toPathContainer(String path) {
        if (path == null) {
            return null;
        }
        return PathContainer.parsePath(path);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setAccountRule(Rule accountRule) {
        this.accountRule = accountRule;
    }

    public void setIpRule(Rule ipRule) {
        this.ipRule = ipRule;
    }

    public void setPathRule(PathRule pathRule) {
        this.pathRule = pathRule;
    }
}
