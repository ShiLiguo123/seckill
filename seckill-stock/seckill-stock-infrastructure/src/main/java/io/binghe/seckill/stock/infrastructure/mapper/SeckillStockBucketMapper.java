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
package io.binghe.seckill.stock.infrastructure.mapper;

import io.binghe.seckill.stock.domain.model.entity.SeckillStockBucket;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 商品分桶库存Mapper
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public interface SeckillStockBucketMapper {

    /**
     * 增加库存
     */
    int increaseStock(@Param("quantity") Integer quantity, @Param("serialNo") Integer serialNo, @Param("goodsId") Long goodsId);

    /**
     * 扣减库存
     */
    int decreaseStock(@Param("quantity") Integer quantity, @Param("serialNo") Integer serialNo, @Param("goodsId") Long goodsId);

    /**
     * 根据商品id获取库存分桶列表
     */
    List<SeckillStockBucket> getBucketsByGoodsId(@Param("goodsId") Long goodsId);

    /**
     * 根据商品id修改状态
     */
    int updateStatusByGoodsId(@Param("status") Integer status, @Param("goodsId") Long goodsId);

    /**
     * 根据商品id删除数据
     */
    int deleteByGoodsId(@Param("goodsId") Long goodsId);

    /**
     * 保存分桶数据
     */
    void insertBatch(@Param("buckets") List<SeckillStockBucket> buckets);
}
