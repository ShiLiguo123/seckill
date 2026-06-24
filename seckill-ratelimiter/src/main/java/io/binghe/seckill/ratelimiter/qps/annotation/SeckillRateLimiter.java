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
package io.binghe.seckill.ratelimiter.qps.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 自定义限流注解
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Inherited
@Documented
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SeckillRateLimiter {

    /**
     * 限流器名称，如果不设置，默认是类名加方法名。如果多个接口设置了同一个名称，则使用同一个限流器
     */
    String name() default "";

    /**
     * 一秒内允许通过的请求数QPS
     */
    int permitsPerSecond() default 0;

    /**
     * 获取令牌超时时间
     */
    long timeout() default 0;

    /**
     * 获取令牌超时时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
