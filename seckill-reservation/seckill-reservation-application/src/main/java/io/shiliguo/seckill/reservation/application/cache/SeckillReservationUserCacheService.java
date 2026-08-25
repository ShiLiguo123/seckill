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
import io.shiliguo.seckill.reservation.domain.event.SeckillReservationUserEvent;
import io.shiliguo.seckill.reservation.domain.model.entity.SeckillReservationUser;

import java.util.List;

/**
 * @version 1.0.0
 * @description SeckillReservationUserCacheService
 */
public interface SeckillReservationUserCacheService extends SeckillCacheService {

    /**
     * 根据用户id和商品id获取用户预约信息
     */
    SeckillBusinessCache<SeckillReservationUser> getSeckillReservationUserCacheByUserIdAndGoodsId(Long userId, Long goodsId, Long version);

    /**
     * 根据用户id和商品id更新用户预约信息
     */
    SeckillBusinessCache<SeckillReservationUser> tryUpdateSeckillReservationUserCacheByUserIdAndGoodsId(Long userId, Long goodsId, boolean doubleCheck);

    /**
     * 根据商品id查看预约用户列表
     */
    SeckillBusinessCache<List<SeckillReservationUser>> getUserListCacheByGoodsId(Long goodsId, Long version);

    /**
     * 根据商品id更新预约用户列表
     */
    SeckillBusinessCache<List<SeckillReservationUser>> tryUpdatetUserListCacheByGoodsId(Long goodsId, boolean doubleCheck);

    /**
     * 根据用户id查看预约的商品列表
     */
    SeckillBusinessCache<List<SeckillReservationUser>> getGoodsListCacheByUserId(Long userId, Long version);

    /**
     * 根据用户id更新预约的商品列表
     */
    SeckillBusinessCache<List<SeckillReservationUser>> tryUpdateGoodsListCacheByUserId(Long userId, boolean doubleCheck);

    /**
     * 删除缓存中的数据
     */
    void deleteSeckillReservationUserFromCache(SeckillReservationUserEvent seckillReservationUserEvent);
}
