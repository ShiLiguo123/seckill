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

import io.binghe.seckill.stock.application.model.dto.SeckillStockBucketDTO;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 库存编排服务
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public interface SeckillStockBucketArrangementService {

    /**
     * 编码分桶库存
     * @param goodsId 商品id
     * @param stock 库存数量
     * @param bucketsQuantity 分桶数量
     * @param assignmentMode 编排模式, 1:总量模式; 2:增量模式
     */
    void arrangeStockBuckets(Long goodsId, Integer stock, Integer bucketsQuantity, Integer assignmentMode);

    /**
     * 通过商品id获取库存分桶信息
     */
    SeckillStockBucketDTO getSeckillStockBucketDTO(Long goodsId, Long version);
}
