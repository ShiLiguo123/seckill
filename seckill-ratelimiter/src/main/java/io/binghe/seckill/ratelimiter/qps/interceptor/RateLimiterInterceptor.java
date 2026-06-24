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
package io.binghe.seckill.ratelimiter.qps.interceptor;

import cn.hutool.core.util.StrUtil;
import com.google.common.util.concurrent.RateLimiter;
import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.exception.SeckillException;
import io.binghe.seckill.ratelimiter.qps.annotation.SeckillRateLimiter;
import io.binghe.seckill.ratelimiter.qps.bean.BHRateLimiter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 限流切面
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Aspect
@Component
@ConditionalOnProperty(prefix = "rate.limit.local.qps", name = "enabled", havingValue = "true")
public class RateLimiterInterceptor implements EnvironmentAware {
    private final Logger logger = LoggerFactory.getLogger(RateLimiterInterceptor.class);
    private static final Map<String, BHRateLimiter> BH_RATE_LIMITER_MAP = new ConcurrentHashMap<>();
    private Environment environment;

    @Value("${rate.limit.local.qps.default.permitsPerSecond:1000}")
    private double defaultPermitsPerSecond;

    @Value("${rate.limit.local.qps.default.timeout:1}")
    private long defaultTimeout;

    @Pointcut("@annotation(seckillRateLimiter)")
    public void pointCut(SeckillRateLimiter seckillRateLimiter){

    }

    @Around(value = "pointCut(seckillRateLimiter)")
    public Object around(ProceedingJoinPoint pjp, SeckillRateLimiter seckillRateLimiter) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String className = pjp.getTarget().getClass().getSimpleName();
        String methodName = signature.getName();
        String rateLimitName = environment.resolvePlaceholders(seckillRateLimiter.name());
        if (StrUtil.isEmpty(rateLimitName) || rateLimitName.contains("${")) {
            rateLimitName = className + "-" + methodName;
        }
        BHRateLimiter rateLimiter = this.getRateLimiter(rateLimitName, seckillRateLimiter);
        boolean success = rateLimiter.tryAcquire();
        Object[] args = pjp.getArgs();
        if (success){
            return pjp.proceed(args);
        }
        logger.error("around|访问接口过于频繁|{}|{}", className, methodName);
        throw new SeckillException(ErrorCode.RETRY_LATER);
    }

    /**
     * 获取BHRateLimiter对象
     */
    private BHRateLimiter getRateLimiter(String rateLimitName, SeckillRateLimiter seckillRateLimiter) {
        //先从Map缓存中获取
        BHRateLimiter bhRateLimiter = BH_RATE_LIMITER_MAP.get(rateLimitName);
        //如果获取的bhRateLimiter为空，则创建bhRateLimiter，注意并发，创建的时候需要加锁
        if (bhRateLimiter == null){
            final String finalRateLimitName = rateLimitName.intern();
            synchronized (finalRateLimitName){
                //double check
                bhRateLimiter = BH_RATE_LIMITER_MAP.get(rateLimitName);
                //获取的bhRateLimiter再次为空
                if (bhRateLimiter == null){
                    double permitsPerSecond = seckillRateLimiter.permitsPerSecond() <= 0 ? defaultPermitsPerSecond : seckillRateLimiter.permitsPerSecond();
                    long timeout = seckillRateLimiter.timeout() <= 0 ? defaultTimeout : seckillRateLimiter.timeout();
                    TimeUnit timeUnit = seckillRateLimiter.timeUnit();
                    bhRateLimiter = new BHRateLimiter(RateLimiter.create(permitsPerSecond), timeout, timeUnit);
                    BH_RATE_LIMITER_MAP.putIfAbsent(rateLimitName, bhRateLimiter);
                }
            }
        }
        return bhRateLimiter;
    }


    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
