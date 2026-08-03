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
package io.binghe.seckill.gateway.risk.rule.model;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 规则
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public class Rule {
    /**
     * 是否启用，默认为false
     */
    private boolean enabled = false;

    /**
     * 路径
     */
    private String path;

    /**
     * 窗口限流的周期，单位是毫秒
     */
    private long windowPeriod;

    /**
     * 滑动窗口大小
     */
    private int windowSize;

    public boolean isEnabled() {
        return enabled;
    }

    public Rule setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String getPath() {
        return path;
    }

    public Rule setPath(String path) {
        this.path = path;
        return this;
    }

    public long getWindowPeriod() {
        return windowPeriod;
    }

    public Rule setWindowPeriod(long windowPeriod) {
        this.windowPeriod = windowPeriod;
        return this;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public Rule setWindowSize(int windowSize) {
        this.windowSize = windowSize;
        return this;
    }
}
