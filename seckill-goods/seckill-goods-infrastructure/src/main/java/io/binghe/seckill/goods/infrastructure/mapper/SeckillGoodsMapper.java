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
package io.binghe.seckill.goods.infrastructure.mapper;

import io.binghe.seckill.goods.domain.model.entity.SeckillGoods;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 商品
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public interface SeckillGoodsMapper {

    /**
     * 保存商品信息
     */
    int saveSeckillGoods(SeckillGoods seckillGoods);

    /**
     * 根据id获取商品详细信息
     */
    SeckillGoods getSeckillGoodsId(@Param("id") Long id);

    /**
     * 根据活动id获取商品列表
     */
    List<SeckillGoods> getSeckillGoodsByActivityId(@Param("activityId") Long activityId);

    /**
     * 修改商品状态
     */
    int updateStatus(@Param("status") Integer status, @Param("id") Long id);

    /**
     * 扣减库存
     */
    int updateAvailableStock(@Param("count") Integer count, @Param("id") Long id);

    /**
     * 获取当前可用库存
     */
    Integer getAvailableStockById(@Param("id") Long id);

}
