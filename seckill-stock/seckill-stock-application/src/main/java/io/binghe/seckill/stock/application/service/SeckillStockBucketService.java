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
package io.binghe.seckill.stock.application.service;

import io.binghe.seckill.common.cache.model.SeckillBusinessCache;
import io.binghe.seckill.common.model.message.TxMessage;
import io.binghe.seckill.stock.application.model.command.SeckillStockBucketWrapperCommand;
import io.binghe.seckill.stock.application.model.dto.SeckillStockBucketDTO;
import io.binghe.seckill.common.model.dto.stock.SeckillStockDTO;
import io.binghe.seckill.stock.domain.model.dto.SeckillStockBucketDeduction;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 商品库存服务
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public interface SeckillStockBucketService {

    /**
     * 编排库存
     */
    void arrangeStockBuckets(Long userId, SeckillStockBucketWrapperCommand stockBucketWrapperCommand);

    /**
     * 获取库存分桶
     */
    SeckillStockBucketDTO getTotalStockBuckets(Long goodsId, Long version);

    /**
     * 获取商品可用库存
     */
    SeckillBusinessCache<Integer> getAvailableStock(Long goodsId, Long version);

    /**
     * 获取商品的库存信息
     */
    SeckillBusinessCache<SeckillStockDTO> getSeckillStock(Long goodsId, Long version);

    /**
     * 扣减商品库存
     */
    boolean decreaseStock(TxMessage txMessage);
}
