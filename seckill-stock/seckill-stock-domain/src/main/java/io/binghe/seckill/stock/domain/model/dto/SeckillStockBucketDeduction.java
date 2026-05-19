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
package io.binghe.seckill.stock.domain.model.dto;

import java.io.Serializable;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 商品库存扣减与恢复数据
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public class SeckillStockBucketDeduction implements Serializable {
    private static final long serialVersionUID = -6298907463471862983L;
    //商品id
    private Long goodsId;
    //商品数量
    private Integer quantity;
    //用户id
    private Long userId;
    //分桶编号
    private Integer serialNo;

    public SeckillStockBucketDeduction() {
    }

    public SeckillStockBucketDeduction(Long goodsId, Integer quantity, Long userId, Integer serialNo) {
        this.goodsId = goodsId;
        this.quantity = quantity;
        this.userId = userId;
        this.serialNo = serialNo;
    }

    public Long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Long goodsId) {
        this.goodsId = goodsId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(Integer serialNo) {
        this.serialNo = serialNo;
    }

    public boolean isEmpty(){
        return this.goodsId == null
                || this.quantity == null
                || this.userId == null
                || this.serialNo == null;
    }
}
