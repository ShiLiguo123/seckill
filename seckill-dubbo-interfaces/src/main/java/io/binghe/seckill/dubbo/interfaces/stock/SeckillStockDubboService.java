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
package io.binghe.seckill.dubbo.interfaces.stock;

import io.binghe.seckill.common.cache.model.SeckillBusinessCache;
import io.binghe.seckill.common.model.dto.stock.SeckillStockDTO;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 库存和商品都要实现的Dubbo接口
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public interface SeckillStockDubboService {
    /**
     * 获取商品的可用库存
     */
    SeckillBusinessCache<Integer> getAvailableStock(Long goodsId, Long version);

    /**
     * 获取商品的库存信息
     */
    SeckillBusinessCache<SeckillStockDTO> getSeckillStock(Long goodsId, Long version);
}
