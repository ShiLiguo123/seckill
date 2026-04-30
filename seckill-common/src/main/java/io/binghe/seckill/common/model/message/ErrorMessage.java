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

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 错误消息，扣减库存失败，由商品微服务发送给订单微服务
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public class ErrorMessage {
    //全局事务编号
    private Long txNo;
    //商品id
    private Long goodsId;
    //购买数量
    private Integer quantity;
    //下单的类型
    private String placeOrderType;
    //是否扣减了缓存库存
    private Boolean exception;

    public ErrorMessage() {
    }

    public ErrorMessage(Long txNo, Long goodsId, Integer quantity, String placeOrderType, Boolean exception) {
        this.txNo = txNo;
        this.goodsId = goodsId;
        this.quantity = quantity;
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
