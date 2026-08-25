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
package io.shiliguo.seckill.reservation.application.cache;

import io.shiliguo.seckill.common.cache.model.SeckillBusinessCache;
import io.shiliguo.seckill.common.cache.service.SeckillCacheService;
import io.shiliguo.seckill.reservation.domain.model.entity.SeckillReservationConfig;

import java.util.List;

/**
 * @version 1.0.0
 * @description SeckillReservationConfigCacheService
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
