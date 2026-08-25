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
package io.shiliguo.seckill.reservation.infrastructure.mapper;

import io.shiliguo.seckill.reservation.domain.model.entity.SeckillReservationUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @version 1.0.0
 * @description SeckillReservationUserMapper接口
 */
public interface SeckillReservationUserMapper {

    /**
     * 根据用户id查看预约的商品列表
     */
    List<SeckillReservationUser> getGoodsListByUserId(@Param("userId") Long userId, @Param("status") Integer status);

    /**
     * 预约秒杀商品
     */
    int reserveGoods(SeckillReservationUser seckillReservationUser);

    /**
     * 取消预约秒杀商品
     */
    int cancelReserveGoods(@Param("goodsId") Long goodsId, @Param("userId") Long userId);

    /**
     * 获取用户预约的某个商品信息
     */
    SeckillReservationUser getSeckillReservationUser(@Param("userId") Long userId, @Param("goodsId") Long goodsId, @Param("status") Integer status);
}
