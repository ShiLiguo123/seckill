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
package io.binghe.seckill.reservation.application.cache;

import io.binghe.seckill.common.cache.model.SeckillBusinessCache;
import io.binghe.seckill.common.cache.service.SeckillCacheService;
import io.binghe.seckill.reservation.domain.model.entity.SeckillReservationConfig;

import java.util.List;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description SeckillReservationConfigCacheService
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public interface SeckillReservationConfigCacheService extends SeckillCacheService {

    /**
     * 根据商品id和版本号获取商品预约配置信息
     */
    SeckillBusinessCache<SeckillReservationConfig> getSeckillReservationConfig(Long goodsId, Long version);

    /**
     * 更新预约人数
     */
    SeckillBusinessCache<SeckillReservationConfig> updateSeckillReservationConfigCurrentUserCount(Long goodsId, Integer status, Long version);

    /**
     * 更新商品预约配置缓存
     */
    SeckillBusinessCache<SeckillReservationConfig> tryUpdateSeckillReservationConfigCacheByLock(Long goodsId, boolean doubleCheck);

    /**
     * 获取预约配置列表
     */
    SeckillBusinessCache<List<SeckillReservationConfig>> getSeckillReservationConfigList(Long version);

    /**
     * 更新预约配置列表
     */
    SeckillBusinessCache<List<SeckillReservationConfig>> tryUpdateSeckillReservationConfigListCacheByLock(boolean doubleCheck);

}
