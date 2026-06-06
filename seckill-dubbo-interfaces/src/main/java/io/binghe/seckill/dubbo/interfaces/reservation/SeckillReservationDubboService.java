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
package io.binghe.seckill.dubbo.interfaces.reservation;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 预约服务Dubbo接口
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public interface SeckillReservationDubboService {

    /**
     * 下单时校验预约信息
     * 如果当前商品不存在预约配置，则说明当前商品无需提前预约即可下单，返回true
     * 如果当前商品存在预约配置，则校验当前用户是否预约过商品，如果预约过当前商品，则返回true，否则返回false。
     */
    boolean checkReservation(Long userId, Long goodsId);
}
