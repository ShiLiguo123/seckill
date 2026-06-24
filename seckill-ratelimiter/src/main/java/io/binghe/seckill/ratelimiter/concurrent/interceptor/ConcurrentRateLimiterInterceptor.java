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
package io.binghe.seckill.ratelimiter.concurrent.interceptor;

import cn.hutool.core.util.StrUtil;
import com.google.common.util.concurrent.RateLimiter;
import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.exception.SeckillException;
import io.binghe.seckill.ratelimiter.concurrent.annotation.ConcurrentRateLimiter;
import io.binghe.seckill.ratelimiter.concurrent.bean.BHConcurrentRateLimiter;
import io.binghe.seckill.ratelimiter.concurrent.policy.ConcurrentRateLimiterPolicy;
import io.binghe.seckill.ratelimiter.qps.annotation.SeckillRateLimiter;
import io.binghe.seckill.ratelimiter.qps.bean.BHRateLimiter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 并发数限流拦截器
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Aspect
@Component
@ConditionalOnProperty(prefix = "rate.limit.local.concurrent", name = "enabled", havingValue = "true")
public class ConcurrentRateLimiterInterceptor implements EnvironmentAware, DisposableBean {
    private final Logger logger = LoggerFactory.getLogger(ConcurrentRateLimiterInterceptor.class);

    private static final Map<String, BHConcurrentRateLimiter> BH_CONCURRENT_RATE_LIMITER_MAP = new ConcurrentHashMap<>();
    private Environment environment;

    @Value("${rate.limit.local.concurrent.default.corePoolSize:3}")
    private int defaultCorePoolSize;

    @Value("${rate.limit.local.concurrent.default.maximumPoolSize:5}")
    private int defaultMaximumPoolSize;

    @Value("${rate.limit.local.concurrent.default.queueCapacity:10}")
    private int defaultQueueCapacity;

    @Value("${rate.limit.local.concurrent.default.keepAliveTime:30}")
    private long defaultKeepAliveTime;

    @Value("${rate.limit.local.concurrent.default.timeout:1}")
    private long defaultTimeOut;

    @Pointcut("@annotation(concurrentRateLimiter)")
    public void pointCut(ConcurrentRateLimiter concurrentRateLimiter){

    }

    @Around(value = "pointCut(concurrentRateLimiter)")
    public Object around(ProceedingJoinPoint pjp, ConcurrentRateLimiter concurrentRateLimiter) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String className = pjp.getTarget().getClass().getSimpleName();
        String methodName = signature.getName();
        String rateLimitName = environment.resolvePlaceholders(concurrentRateLimiter.name());
        if (StrUtil.isEmpty(rateLimitName) || rateLimitName.contains("${")) {
            rateLimitName = className + "-" + methodName;
        }
        BHConcurrentRateLimiter rateLimiter = this.getRateLimiter(rateLimitName, concurrentRateLimiter);
        Object[] args = pjp.getArgs();
        //基于自定义线程池实现并发数限流
        return rateLimiter.submit(() -> {
            try {
                return pjp.proceed(args);
            } catch (Throwable e) {
                if (e instanceof SeckillException){
                    throw (SeckillException) e;
                }
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 获取BHConcurrentRateLimiter对象
     */
    private BHConcurrentRateLimiter getRateLimiter(String rateLimitName, ConcurrentRateLimiter concurrentRateLimiter) {
        //先从Map缓存中获取
        BHConcurrentRateLimiter bhRateLimiter = BH_CONCURRENT_RATE_LIMITER_MAP.get(rateLimitName);
        //如果获取的bhRateLimiter为空，则创建bhRateLimiter，注意并发，创建的时候需要加锁
        if (bhRateLimiter == null){
            final String finalRateLimitName = rateLimitName.intern();
            synchronized (finalRateLimitName){
                //double check
                bhRateLimiter = BH_CONCURRENT_RATE_LIMITER_MAP.get(rateLimitName);
                //获取的bhRateLimiter再次为空
                if (bhRateLimiter == null){
                    int corePoolSize = concurrentRateLimiter.corePoolSize() <= 0 ? defaultCorePoolSize : concurrentRateLimiter.corePoolSize();
                    int maximumPoolSize = concurrentRateLimiter.maximumPoolSize() <= 0 ? defaultMaximumPoolSize : concurrentRateLimiter.maximumPoolSize();
                    int queueCapacity = concurrentRateLimiter.queueCapacity() <= 0 ? defaultQueueCapacity : concurrentRateLimiter.queueCapacity();

                    long keepAliveTime = concurrentRateLimiter.keepAliveTime() <= 0 ? defaultKeepAliveTime : concurrentRateLimiter.keepAliveTime();
                    TimeUnit keepAliveTimeUnit = concurrentRateLimiter.keepAliveTimeUnit();

                    long timeout = concurrentRateLimiter.timeout() <= 0 ? defaultTimeOut : concurrentRateLimiter.timeout();
                    TimeUnit timeoutUnit = concurrentRateLimiter.timeoutUnit();

                    ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize,
                            maximumPoolSize,
                            keepAliveTime,
                            keepAliveTimeUnit,
                            new ArrayBlockingQueue<>(queueCapacity),
                            (r) -> new Thread(r, "rate-limiter-threadPool-".concat(rateLimitName).concat("-"))
                            , new ConcurrentRateLimiterPolicy());

                    bhRateLimiter = new BHConcurrentRateLimiter(executor, timeout, timeoutUnit);
                    BH_CONCURRENT_RATE_LIMITER_MAP.putIfAbsent(rateLimitName, bhRateLimiter);
                }
            }
        }
        return bhRateLimiter;
    }


    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void destroy() throws Exception {
        //关闭线程池
        logger.info("destroy|关闭线程池");
        Map<String, BHConcurrentRateLimiter> map = BH_CONCURRENT_RATE_LIMITER_MAP;
        if (map.size() > 0){
            for (Map.Entry<String, BHConcurrentRateLimiter> entry : map.entrySet()){
                BHConcurrentRateLimiter rateLimiter = entry.getValue();
                rateLimiter.shutdown();
            }
            map.clear();
            BH_CONCURRENT_RATE_LIMITER_MAP.clear();
        }
    }
}
