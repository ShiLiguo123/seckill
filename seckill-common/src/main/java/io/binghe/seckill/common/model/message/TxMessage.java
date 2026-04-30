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
package io.binghe.seckill.common.model.message;

import java.math.BigDecimal;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 事务消息
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public class TxMessage {
    //全局事务编号
    private Long txNo;
    //商品id
    private Long goodsId;
    //购买数量
    private Integer quantity;
    //活动id
    private Long activityId;
    //商品版本号
    private Long version;
    //用户id
    private Long userId;
    //商品名称
    private String goodsName;
    //秒杀活动价格
    private BigDecimal activityPrice;
    //下单的类型
    private String placeOrderType;
    //是否抛出了异常
    private Boolean exception;


    public TxMessage() {
    }

    public TxMessage(Long txNo, Long goodsId, Integer quantity, Long activityId, Long version, Long userId, String goodsName,
                     BigDecimal activityPrice, String placeOrderType, Boolean exception) {
        this.txNo = txNo;
        this.goodsId = goodsId;
        this.quantity = quantity;
        this.activityId = activityId;
        this.version = version;
        this.userId = userId;
        this.goodsName = goodsName;
        this.activityPrice = activityPrice;
        this.placeOrderType = placeOrderType;
        this.exception = exception;
    }

    public Long getTxNo() {
        return txNo;
    }

    public void setTxNo(Long txNo) {
        this.txNo = txNo;
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

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getGoodsName() {
        return goodsName;
    }

    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
    }

    public BigDecimal getActivityPrice() {
        return activityPrice;
    }

    public void setActivityPrice(BigDecimal activityPrice) {
        this.activityPrice = activityPrice;
    }

    public String getPlaceOrderType() {
        return placeOrderType;
    }

    public void setPlaceOrderType(String placeOrderType) {
        this.placeOrderType = placeOrderType;
    }

    public Boolean getException() {
        return exception;
    }

    public void setException(Boolean exception) {
        this.exception = exception;
    }
}
