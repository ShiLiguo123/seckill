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
package io.binghe.seckill.reservation.infrastructure.mapper;

import io.binghe.seckill.reservation.domain.model.entity.SeckillReservationConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description SeckillReservationConfigMapper接口
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public interface SeckillReservationConfigMapper {

    /**
     * 保存预约配置
     */
    int saveSeckillReservationConfig(SeckillReservationConfig seckillReservationConfig);

    /**
     * 更新预约配置
     */
    int updateSeckillReservationConfig(SeckillReservationConfig seckillReservationConfig);

    /**
     * 更新状态
     */
    int updateStatus(@Param("status") Integer status, @Param("goodsId") Long goodsId);

    /**
     * 更新当前预约人数
     */
    int updateReserveCurrentUserCount(@Param("reserveCurrentUserCount") Integer reserveCurrentUserCount, @Param("goodsId") Long goodsId);

    /**
     * 获取配置列表
     */
    List<SeckillReservationConfig> getConfigList();

    /**
     * 获取配置详情
     */
    SeckillReservationConfig getConfigDetail(@Param("goodsId") Long goodsId);
}
