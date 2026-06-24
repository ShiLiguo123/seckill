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
package io.binghe.seckill.ratelimiter.qps.bean;

import com.google.common.util.concurrent.RateLimiter;

import java.util.concurrent.TimeUnit;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 限流器
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public class BHRateLimiter {
    //Google RateLimiter限流器
    private RateLimiter rateLimiter;
    //获取令牌超时时间
    private long timeout;
    //获取令牌超时时间单位
    private TimeUnit timeUnit;

    public BHRateLimiter() {
    }

    public BHRateLimiter(RateLimiter rateLimiter, long timeout, TimeUnit timeUnit) {
        this.rateLimiter = rateLimiter;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    public void setRateLimiter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public boolean tryAcquire(){
        return rateLimiter.tryAcquire();
    }

    public boolean tryAcquireWithTimeout(){
        return rateLimiter.tryAcquire(timeout, timeUnit);
    }

    public boolean tryAcquire(int permits){
        return rateLimiter.tryAcquire(permits);
    }

    public boolean tryAcquireWithTimeout(int permits){
        return rateLimiter.tryAcquire(permits, timeout, timeUnit);
    }
}
