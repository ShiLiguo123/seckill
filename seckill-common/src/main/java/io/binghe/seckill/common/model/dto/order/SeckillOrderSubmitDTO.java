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
package io.binghe.seckill.common.model.dto.order;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 订单提交后返回的数据
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public class SeckillOrderSubmitDTO {

    /**
     * 同步下单时，为订单id
     * 异步下单时，为许可id
     */
    private String id;

    /**
     * 商品id
     */
    private Long goodsId;

    /**
     * 类型
     * type_order：id为订单号
     * type_task：id为下单许可号
     */
    private String type;

    public SeckillOrderSubmitDTO() {
    }

    public SeckillOrderSubmitDTO(String id, Long goodsId, String type) {
        this.id = id;
        this.type = type;
        this.goodsId = goodsId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Long goodsId) {
        this.goodsId = goodsId;
    }
}
