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
package io.binghe.seckill.reservation.application.command;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 商品预约配置
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public class SeckillReservationConfigCommand implements Serializable {
    private static final long serialVersionUID = 6994147588632776413L;
    //商品id
    private Long goodsId;
    //预约人数上限
    private Integer reserveMaxUserCount;
    //预约开始时间
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+08:00")
    private Date reserveStartTime;
    //预约结束时间
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+08:00")
    private Date reserveEndTime;

    public boolean isEmpty() {
        return this.goodsId == null
                || this.reserveMaxUserCount == null
                || this.reserveMaxUserCount <= 0
                || this.reserveStartTime == null
                || this.reserveEndTime == null
                || this.reserveStartTime.after(this.reserveEndTime);
    }

    public Long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Long goodsId) {
        this.goodsId = goodsId;
    }

    public Integer getReserveMaxUserCount() {
        return reserveMaxUserCount;
    }

    public void setReserveMaxUserCount(Integer reserveMaxUserCount) {
        this.reserveMaxUserCount = reserveMaxUserCount;
    }

    public Date getReserveStartTime() {
        return reserveStartTime;
    }

    public void setReserveStartTime(Date reserveStartTime) {
        this.reserveStartTime = reserveStartTime;
    }

    public Date getReserveEndTime() {
        return reserveEndTime;
    }

    public void setReserveEndTime(Date reserveEndTime) {
        this.reserveEndTime = reserveEndTime;
    }
}
