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
package io.binghe.seckill.application.service;

import io.binghe.seckill.application.command.SeckillGoodsCommond;
import io.binghe.seckill.common.cache.model.SeckillBusinessCache;
import io.binghe.seckill.common.model.dto.goods.SeckillGoodsDTO;
import io.binghe.seckill.common.model.message.TxMessage;
import io.binghe.seckill.goods.domain.model.entity.SeckillGoods;

import java.util.List;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 商品
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public interface SeckillGoodsService {

    /**
     * 保存商品信息
     */
    void saveSeckillGoods(SeckillGoodsCommond seckillGoodsCommond);

    /**
     * 根据id获取商品详细信息
     */
    SeckillGoods getSeckillGoodsId(Long id);

    /**
     * 根据id获取商品详细信息（带缓存）
     */
    SeckillGoodsDTO getSeckillGoods(Long id, Long version);

    /**
     * 根据活动id获取商品列表
     */
    List<SeckillGoods> getSeckillGoodsByActivityId(Long activityId);

    /**
     * 根据活动id从缓存中获取数据
     */
    List<SeckillGoodsDTO> getSeckillGoodsList(Long activityId, Long version);

    /**
     * 修改商品状态
     */
    void updateStatus(Integer status, Long id);

    /**
     * 扣减库存
     */
    boolean updateAvailableStock(Integer count, Long id);

    /**
     * 扣减数据库库存
     */
    boolean updateDbAvailableStock(Integer count, Long id);

    /**
     * 获取当前可用库存
     */
    Integer getAvailableStockById(Long id);

    /**
     * 扣减库存
     */
    boolean updateAvailableStock(TxMessage txMessage);

    /**
     * 获取商品可用库存
     */
    SeckillBusinessCache<Integer> getAvailableStock(Long goodsId, Long version);
}
