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
package io.binghe.seckill.goods.domain.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.binghe.seckill.common.model.enums.SeckillGoodsStatus;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 秒杀商品
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public class SeckillGoods implements Serializable {
    private static final long serialVersionUID = -8447592991812016065L;
    //数据id
    private Long id;
    //商品名称
    private String goodsName;
    //秒杀活动id
    private Long activityId;
    //活动开始时间
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+08:00")
    private Date startTime;
    //活动结束时间
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+08:00")
    private Date endTime;
    //商品原价
    private BigDecimal originalPrice;
    //秒杀活动价格
    private BigDecimal activityPrice;
    //初始库存，如果设置了分桶库存，则此字段无效，库存由库存服务进行处理
    private Integer initialStock;
    //限购个数
    private Integer limitNum;
    //当前可用库存，如果设置了分桶库存，则此字段无效，库存由库存服务进行处理
    private Integer availableStock;
    //描述
    private String description;
    //图片
    private String imgUrl;
    //秒杀状态 0：已发布； 1：上线； -1：下线
    private Integer status;
    //分桶数量
    private Integer bucketsQuantity;

    public boolean validateParams(){
        if (StringUtils.isEmpty(goodsName)
            || activityId == null
            || startTime == null
            || endTime == null
            || startTime.after(endTime)
            || endTime.before(new Date())
            || activityPrice == null
            || activityPrice.compareTo(BigDecimal.ZERO) < 0
            || originalPrice == null
            || originalPrice.compareTo(BigDecimal.ZERO) < 0
            || initialStock == null
            || initialStock <= 0
            || limitNum == null
            || limitNum <= 0){
            return false;
        }
        return true;
    }

    public boolean isOnline(){
        return SeckillGoodsStatus.isOnline(status);
    }

    public boolean isInSeckilling(){
        Date currentDate = new Date();
        return startTime.before(currentDate) && endTime.after(currentDate);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getLimitNum() {
        return limitNum;
    }

    public void setLimitNum(Integer limitNum) {
        this.limitNum = limitNum;
    }

    public String getGoodsName() {
        return goodsName;
    }

    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public BigDecimal getActivityPrice() {
        return activityPrice;
    }

    public void setActivityPrice(BigDecimal activityPrice) {
        this.activityPrice = activityPrice;
    }

    public Integer getInitialStock() {
        return initialStock;
    }

    public void setInitialStock(Integer initialStock) {
        this.initialStock = initialStock;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getBucketsQuantity() {
        return bucketsQuantity;
    }

    public void setBucketsQuantity(Integer bucketsQuantity) {
        this.bucketsQuantity = bucketsQuantity;
    }
}
