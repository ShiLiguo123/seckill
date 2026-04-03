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
package io.binghe.seckill.domain.constants;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 秒杀常量类
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public class SeckillConstants {
    /**
     * 用户缓存前缀
     */
    public static final String USER_KEY_PREFIX = "user_";

    /**
     * 获取Key
     */
    public static String getKey(String prefix, String key){
        return prefix.concat(key);
    }

    /**
     * token的载荷中存放的信息 只存放一个userId
     */
    public static final String TOKEN_CLAIM = "userId";

    /**
     * jwtToken过期时间 默认为7天
     */
    public static final Long TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L;

    /**
     * token请求头名称
     */
    public static final String TOKEN_HEADER_NAME = "access-token";

    /**
     * JWT的密钥
     */
    public static final String JWT_SECRET = "a814edb0e7c1ba4c";

    /*****************缓存相关的配置****************/
    public static final Long FIVE_MINUTES = 5 * 60L;
    public static final Long FIVE_SECONDS = 5L;
    public static final Long HOURS_24 = 3600 * 24L;

    public static final String SECKILL_ACTIVITY_CACHE_KEY = "SECKILL_ACTIVITY_CACHE_KEY";
    public static final String SECKILL_ACTIVITIES_CACHE_KEY = "SECKILL_ACTIVITIES_CACHE_KEY";

    public static final String SECKILL_GOODS_CACHE_KEY = "SECKILL_GOODS_CACHE_KEY";
    public static final String SECKILL_GOODSES_CACHE_KEY = "SECKILL_GOODSES_CACHE_KEY";


}
