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
package io.binghe.seckill.reservation.application.service;

import io.binghe.seckill.reservation.application.command.SeckillReservationConfigCommand;
import io.binghe.seckill.reservation.application.command.SeckillReservationUserCommand;
import io.binghe.seckill.reservation.domain.model.entity.SeckillReservationConfig;
import io.binghe.seckill.reservation.domain.model.entity.SeckillReservationUser;

import java.util.List;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description SeckillReservationService
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public interface SeckillReservationService {

    /**
     * 保存预约配置
     */
    boolean saveSeckillReservationConfig(SeckillReservationConfigCommand seckillReservationConfigCommand);

    /**
     * 更新预约配置
     */
    boolean updateSeckillReservationConfig(SeckillReservationConfigCommand seckillReservationConfigCommand);

    /**
     * 更新配置状态
     */
    boolean updateConfigStatus(Integer status, Long goodsId);

    /**
     * 获取配置列表
     */
    List<SeckillReservationConfig> getConfigList(Long version);

    /**
     * 获取配置详情
     */
    SeckillReservationConfig getConfigDetail(Long goodsId, Long version);

    /**
     * 根据商品id查看预约用户列表
     */
    List<SeckillReservationUser> getUserListByGoodsId(Long goodsId, Long version);

    /**
     * 根据用户id查看预约的商品列表
     */
    List<SeckillReservationUser> getGoodsListByUserId(Long userId, Long version);

    /**
     * 预约秒杀商品
     */
    boolean reserveGoods(SeckillReservationUserCommand seckillReservationUserCommand);

    /**
     * 取消预约秒杀商品
     */
    boolean cancelReserveGoods(SeckillReservationUserCommand seckillReservationUserCommand);

    /**
     * 获取用户预约的某个商品信息
     */
    SeckillReservationUser getSeckillReservationUser(SeckillReservationUserCommand seckillReservationUserCommand);
}
