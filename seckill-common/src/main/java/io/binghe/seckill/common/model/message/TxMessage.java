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
public class TxMessage extends ErrorMessage {
    //活动id
    private Long activityId;
    //商品版本号
    private Long version;
    //商品名称
    private String goodsName;
    //秒杀活动价格
    private BigDecimal activityPrice;

    public TxMessage() {
    }

    public TxMessage(String destination, Long txNo, Long goodsId, Integer quantity, Long activityId, Long version, Long userId, String goodsName,
                     BigDecimal activityPrice, String placeOrderType, Boolean exception, Integer bucketSerialNo, String orderTaskId) {

        super(destination, txNo, goodsId, quantity, placeOrderType, exception, bucketSerialNo, userId, orderTaskId);
        this.activityId = activityId;
        this.version = version;
        this.goodsName = goodsName;
        this.activityPrice = activityPrice;
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
}
