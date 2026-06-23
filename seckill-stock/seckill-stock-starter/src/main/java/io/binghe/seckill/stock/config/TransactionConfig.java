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
package io.binghe.seckill.stock.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import io.binghe.seckill.common.config.RedisConfig;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description Spring事务编程配置类
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Configuration
@MapperScan(value = {"io.binghe.seckill.stock.infrastructure.mapper"})
@ComponentScan(value = {"io.binghe.seckill", "com.alibaba.cola"})
@Import({RedisConfig.class,})
@EnableTransactionManagement(proxyTargetClass = true)
@ServletComponentScan(basePackages = {"io.binghe.seckill"})
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class TransactionConfig {

}
