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
package io.binghe.seckill.dubbo.interfaces.goods;

import io.binghe.seckill.common.cache.model.SeckillBusinessCache;
import io.binghe.seckill.common.model.dto.goods.SeckillGoodsDTO;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 商品Dubbo服务接口
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public interface SeckillGoodsDubboService {

    /**
     * 根据id和版本号获取商品详情
     */
    SeckillGoodsDTO getSeckillGoods(Long id, Long version);

    /**
     * 扣减数据库库存
     */
    boolean updateDbAvailableStock(Integer count, Long id);

    /**
     * 扣减商品库存
     */
    boolean updateAvailableStock(Integer count, Long id);

    /**
     * 根据商品id获取可用库存
     */
    Integer getAvailableStockById(Long goodsId);

    /**
     * 获取商品的可用库存
     */
    SeckillBusinessCache<Integer> getAvailableStock(Long goodsId, Long version);
}
