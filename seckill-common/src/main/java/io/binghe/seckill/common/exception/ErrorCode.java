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
package io.binghe.seckill.common.exception;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public enum ErrorCode {

    SUCCESS(1001, "成功"),
    FAILURE(2001, "失败"),
    PARAMS_INVALID(2002, "参数错误"),
    USERNAME_IS_NULL(2003, "用户名不能为空"),
    PASSWORD_IS_NULL(2004, "密码不能为空"),
    USERNAME_IS_ERROR(2005, "用户名错误"),
    PASSWORD_IS_ERROR(2006, "密码错误"),
    SERVER_EXCEPTION(2007, "服务器异常"),
    STOCK_LT_ZERO(2008, "库存不足"),
    GOODS_NOT_EXISTS(2009, "当前商品不存在"),
    ACTIVITY_NOT_EXISTS(2010, "当前活动不存在"),
    BEYOND_LIMIT_NUM(2011, "下单数量不能超过限购数量"),
    USER_NOT_LOGIN(2012, "用户未登录"),
    TOKEN_EXPIRE(2013, "Token失效"),
    GOODS_OFFLINE(2014, "商品已下线"),
    DATA_PARSE_FAILED(2015, "数据解析失败"),
    RETRY_LATER(2016, "操作过于频繁，稍后再试"),
    USER_INVALID(2017, "当前账户异常，不能参与秒杀"),
    GOODS_PUBLISH(2018, "商品未上线"),
    ORDER_FAILED(2019, "下单失败"),
    BEYOND_TIME(2020, "超出活动时间"),
    GOODS_FINISH(2021, "商品已售罄"),
    REDUNDANT_SUBMIT(2022, "请勿重复下单"),
    ORDER_TOKENS_NOT_AVAILABLE(2023, "暂无可用库存"),
    ORDER_TASK_ID_INVALID(2024, "下单任务编号错误"),
    BUCKET_INIT_STOCK_ERROR(2025, "分桶总库存错误"),
    BUCKET_AVAILABLE_STOCK_ERROR(2026, "分桶可用库存错误"),
    BUCKET_STOCK_ERROR(2027, "分桶库存错误"),
    BUCKET_GOODSID_ERROR(2028, "秒杀商品id错误"),
    BUCKET_CREATE_FAILED(2029, "库存分桶失败"),
    BUCKET_CLOSED_FAILED(2030, "关闭分桶失败"),
    BUCKET_SOLD_BEYOND_TOTAL(2031, "已售商品数量大于要设置的总库存"),
    FREQUENTLY_ERROR(2032, "操作频繁，稍后再试"),
    BUCKET_STOCK_NOT_EXISTS(2033, "分桶库存不存在"),
    BUCKET_STOCK_SUSPEND(2034, "库存维护中"),
    BUCKET_STOCK_ALIGN(2035, "库存校准中"),
    GOODS_RESERVATION_CONFIG_NOT_EXISTS(2036, "商品预约配置不存在"),
    GOODS_RESERVATION_CONFIG_EXISTS(2037, "商品预约配置已存在"),
    GOODS_RESERVATION_USER_NOT_EXISTS(2038, "商品预约信息不存在"),
    GOODS_RESERVATION_USER_EXISTS(2039, "已经预约过，不可重复预约"),
    GOODS_RESERVATION_COUNT_INVALIDATE(2040, "重置预约配置时最大人数上限不能小于当前已预约人数"),
    GOODS_RESERVATION_CONFIG_NOT_ONLINE(2041, "当前商品未开通预约通道"),
    GOODS_RESERVATION_NOT_TIME(2042, "不在预约时间范围内，不能预约商品"),
    GOODS_RESERVATION_TIME_NOT_INVALIDATE(2043, "商品预约配置时间错误"),
    GOODS_RESERVATION_NOT_RESERVE(2044, "未预约商品不能直接下单"),
    GOODS_RESERVATION_USER(2045, "预约人数已满"),
    SENTINEL_FLOW(2046, "您访问过快-限流中"),
    SENTINEL_DEGRADE(2047, "您访问过快-降级中"),
    SENTINEL_PARAMAS(2048, "热点参数限流"),
    SENTINEL_SYSTEM(2049, "系统规则（负载/...不满足要求）"),
    SENTINEL_AUTHORITY(2050, "授权规则不通过"),
    STOCK_IS_NULL(2051, "商品库存不存在");

    private final Integer code;
    private final String mesaage;

    ErrorCode(Integer code, String mesaage) {
        this.code = code;
        this.mesaage = mesaage;
    }

    public Integer getCode() {
        return code;
    }

    public String getMesaage() {
        return mesaage;
    }

}
