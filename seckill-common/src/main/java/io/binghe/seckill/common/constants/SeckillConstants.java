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
package io.binghe.seckill.common.constants;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 秒杀常量类
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public class SeckillConstants {

    /**
     * LUA脚本商品库存不存在
     */
    public static final long LUA_RESULT_GOODS_STOCK_NOT_EXISTS = -1;

    /**
     * LUA脚本要扣减的商品数量小于等于0
     */
    public static final long LUA_RESULT_GOODS_STOCK_PARAMS_LT_ZERO = -2;

    /**
     * LUA脚本库存不足
     */
    public static final long LUA_RESULT_GOODS_STOCK_LT_ZERO = -3;

    /**
     * 未执行LUA脚本具体逻辑
     */
    public static final long LUA_RESULT_NOT_EXECUTE = -100;

    /**
     * 分桶库存不存在
     */
    public static final long LUA_BUCKET_STOCK_NOT_EXISTS = -1;

    /**
     * 暂停服务
     */
    public static final long LUA_BUCKET_STOCK_SUSPEND = -2;

    /**
     * 校准中
     */
    public static final long LUA_BUCKET_STOCK_CALIBRATION = -3;

    /**
     * 库存不足
     */
    public static final long LUA_BUCKET_STOCK_NOT_ENOUGH = -4;

    /**
     * 执行成功
     */
    public static final long LUA_BUCKET_STOCK_EXECUTE_SUCCESS = 1;

    /**
     * LUA脚本执行下单许可成功
     */
    public static final long LUA_RESULT_EXECUTE_TOKEN_SUCCESS = 1;

    /**
     * 已经执行过恢复缓存库存的操作
     */
    public static final Long CHECK_RECOVER_STOCK_HAS_EXECUTE = 0L;

    /**
     * 未执行过恢复缓存库存的操作
     */
    public static final Long CHECK_RECOVER_STOCK_NOT_EXECUTE = 1L;

    /**
     * 事务日志7天过期
     */
    public static final long TX_LOG_EXPIRE_DAY = 7;

    /**
     * 事务日志过期秒数
     */
    public static final long TX_LOG_EXPIRE_SECONDS = 7 * 24 * 3600;

    /**
     * 重复提交时间间隔
     */
    public static final long SUBMIT_DATA_EXECUTE_EXPIRE_SECONDS = 60;

    /**
     * 订单任务过期时间秒数
     */
    public static final long ORDER_TASK_EXPIRE_SECONDS = 24 * 3600;

    /**
     * 商品key前缀
     */
    public static final String GOODS_ITEM_KEY_PREFIX = "item:";

    /**
     * 预约记录前缀
     */
    public static final String RESERVATION_USER = "reservation_user";

    /**
     * 预约配置前缀
     */
    public static final String RESERVATION_CONFIG = "reservation_config";

    /**
     * 订单
     */
    public static final String TYPE_ORDER = "type_order";

    /**
     * 下单许可
     */
    public static final String TYPE_TASK = "type_task";

    /**
     * 商品事务列表
     */
    public static final String GOODS_TX_KEY = "item_tx:";

    /**
     * 库存事务列表
     */
    public static final String STOCK_TX_KEY = "stock_tx:";

    /**
     * 订单事务列表
     */
    public static final String ORDER_TX_KEY = "order_tx:";

    /**
     * 事务消息主题
     */
    public static final String TOPIC_TX_MSG = "topic_tx_msg";

    /**
     * 分桶事务消息主题
     */
    public static final String TOPIC_BUCKET_TX_MSG = "topic_bucket_tx_msg";

    /**
     * 异常消息主题
     */
    public static final String TOPIC_ERROR_MSG = "topic_error_msg";

    /**
     * 提交订单任务的消息主题
     */
    public static final String TOPIC_ORDER_MSG = "topic_order_msg";

    /**
     * 数据库方式
     */
    public static final String PLACE_ORDER_TYPE_DB = "db";

    /**
     * 分布式锁方法
     */
    public static final String PLACE_ORDER_TYPE_LOCK = "lock";

    /**
     * lua脚本方式
     */
    public static final String PLACE_ORDER_TYPE_LUA = "lua";

    /**
     * 分桶模式
     */
    public static final String PLACE_ORDER_TYPE_BUCKET = "bucket";

    /**
     * 非分桶模式
     */
    public static final String PLACE_ORDER_TYPE_NORMAL = "normal";

    /**
     * 消息的key
     */
    public static final String MSG_KEY = "message";

    /**
     * 活动事件消息topic
     */
    public static final String TOPIC_EVENT_ROCKETMQ_ACTIVITY = "topic_event_rocketmq_activity";

    /**
     * 商品事件消息topic
     */
    public static final String TOPIC_EVENT_ROCKETMQ_GOODS = "topic_event_rocketmq_goods";

    /**
     * 预约配置事件消息topic
     */
    public static final String TOPIC_EVENT_ROCKETMQ_RESERVATION_CONFIG = "topic_event_rocketmq_reservation_config";

    /**
     * 预约记录事件消息Topic
     */
    public static final String TOPIC_EVENT_ROCKETMQ_RESERVATION_USER = "topic_event_rocketmq_reservation_user";

    /**
     * 订单事件消息topic
     */
    public static final String TOPIC_EVENT_ROCKETMQ_ORDER = "topic_event_rocketmq_order";

    /**
     * 库存事件消息topic
     */
    public static final String TOPIC_EVENT_ROCKETMQ_STOCK = "topic_event_rocketmq_stock";

    /**
     * 订单消费分组
     */
    public static final String EVENT_ORDER_CONSUMER_GROUP = "event_order_consumer_group";

    /**
     * 商品消费分组
     */
    public static final String EVENT_GOODS_CONSUMER_GROUP = "event_goods_consumer_group";

    /**
     * 商品预约配置消费分组
     */
    public static final String EVENT_RESERVATION_CONFIG_CONSUMER_GROUP = "event_reservation_config_consumer_group";

    /**
     * 预约商品消费分组
     */
    public static final String EVENT_RESERVATION_USER_CONSUMER_GROUP = "event_reservation_user_consumer_group";

    /**
     * 库存消费分组
     */
    public static final String EVENT_STOCK_CONSUMER_GROUP = "event_stock_consumer_group";

    /**
     * 活动消费分组
     */
    public static final String EVENT_ACTIVITY_CONSUMER_GROUP = "event_activity_consumer_group";


    /**
     * Cola订阅事件
     */
    public static final String TOPIC_EVENT_COLA = "topic_event_cola";

    /**
     * cola事件类型
     */
    public static final String EVENT_PUBLISH_TYPE_COLA = "cola";

    /**
     * RocketMQ事件类型
     */
    public static final String EVENT_PUBLISH_TYPE_ROCKETMQ = "rocketmq";

    /**
     * 订单事务分组
     */
    public static final String TX_ORDER_PRODUCER_GROUP = "tx_order_producer_group";

    /**
     * 订单消费分组
     */
    public static final String TX_ORDER_CONSUMER_GROUP = "tx_order_consumer_group";

    /**
     * 提交订单分组
     */
    public static final String SUBMIT_ORDER_CONSUMER_GROUP = "submit_order_consumer_group";

    /**
     * 商品事务分组
     */
    public static final String TX_GOODS_PRODUCER_GROUP = "tx_goods_producer_group";

    /**
     * 商品消费分组
     */
    public static final String TX_GOODS_CONSUMER_GROUP = "tx_goods_condumer_group";

    /**
     * 库存消费分组
     */
    public static final String TX_STOCK_CONSUMER_GROUP = "tx_stock_condumer_group";


    /**
     * 订单Key前缀
     */
    public static final String ORDER_KEY_PREFIX = "order:";

    /**
     * Lua脚本后缀
     */
    public static final String LUA_SUFFIX = "_lua";

    /**
     * 订单锁
     */
    public static final String ORDER_LOCK_KEY_PREFIX = "order:lock:";

    /**
     * 订单任务id的key
     */
    public static final String ORDER_TASK_ID_KEY = "order:task:";

    /**
     * 通过任务id存储的订单id的key
     */
    public static final String ORDER_TASK_ORDER_ID_KEY = "order:task:id:";

    /**
     * 订单任务的Token Key
     */
    public static final String ORDER_TASK_AVAILABLE_TOKENS_KEY = "order:token:";

    /**
     * 加锁获取最新的下单许可
     */
    public static final String LOCK_REFRESH_LATEST_AVAILABLE_TOKENS_KEY = "order:refresh:";

    /**
     * 商品库存的Key
     */
    public static final String GOODS_ITEM_STOCK_KEY_PREFIX = "item:stock:";

    /**
     * 商品库存编排
     */
    public static final String GOODS_STOCK_BUCKETS_SUSPEND_KEY = "goods:buckets:suspend:";

    /**
     * 库存校对
     */
    public static final String GOODS_STOCK_BUCKETS_ALIGN_KEY = "goods:buckets:align:";

    /**
     * 分桶库存
     */
    public static final String GOODS_BUCKET_AVAILABLE_STOCKS_KEY = "goods:buckets:available:";

    /**
     * 分桶编排
     */
    public static final String GOODS_BUCKET_ARRANGEMENT_KEY = "goods:buckets:arrangement:";

    /**
     * 商品库存分桶数量
     */
    public static final String GOODS_BUCKETS_QUANTITY_KEY = "goods:buckets:quantity:";

    /**
     * 商品限购数量Key
     */
    public static final String GOODS_ITEM_LIMIT_KEY_PREFIX = "item:limit:";

    /**
     * 商品上架标识
     */
    public static final String GOODS_ITEM_ONLINE_KEY_PREFIX = "item:onffline:";

    /**
     * 用户缓存前缀
     */
    public static final String USER_KEY_PREFIX = "user:";

    /**
     * 获取Key
     */
    public static String getKey(String prefix, String key){
        return prefix.concat(key);
    }

    public static String getType(String type){
        return PLACE_ORDER_TYPE_BUCKET.equals(type) ? PLACE_ORDER_TYPE_BUCKET.concat(":") : PLACE_ORDER_TYPE_NORMAL.concat(":");
    }

    /**
     * token的载荷中盛放的信息 只盛放一个userName 其余什么也不再盛放
     */
    public static final String TOKEN_CLAIM = "userId";

    /**
     * jwtToken过期时间 默认为7天
     */
    public static final Long TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L;

    /**
     * token请求头名称
     */
    public static final String TOKEN_HEADER_NAME = "access-token";

    /**
     * JWT的密钥
     */
    public static final String JWT_SECRET = "a814edb0e7c1ba4c";


    /*****************缓存相关的配置****************/
    public static final Long FIVE_MINUTES = 5 * 60L;
    public static final Long FIVE_SECONDS = 5L;
    public static final Long HOURS_24 = 3600 * 24L;

    public static final String SECKILL_ACTIVITY_CACHE_KEY = "SECKILL_ACTIVITY_CACHE_KEY";
    public static final String SECKILL_ACTIVITIES_CACHE_KEY = "SECKILL_ACTIVITIES_CACHE_KEY";

    public static final String SECKILL_GOODS_CACHE_KEY = "SECKILL_GOODS_CACHE_KEY";
    public static final String SECKILL_STOCK_CACHE_KEY = "SECKILL_STOCK_CACHE_KEY";
    public static final String SECKILL_GOODSES_CACHE_KEY = "SECKILL_GOODSES_CACHE_KEY";
    public static final String SECKILL_RESERVATION_CONFIG_CACHE_KEY = "SECKILL_RESERVATION_CONFIG_CACHE_KEY";
    public static final String SECKILL_RESERVATION_CONFIG_CURRENT_COUNT_CACHE_KEY = "SECKILL_RESERVATION_CONFIG_CURRENT_COUNT_CACHE_KEY";
    public static final String SECKILL_RESERVATION_CONFIG_LIST_CACHE_KEY = "SECKILL_RESERVATION_CONFIG_LIST_CACHE_KEY";
    public static final String SECKILL_RESERVATION_USER_CACHE_KEY = "SECKILL_RESERVATION_USER_CACHE_KEY";
    public static final String SECKILL_RESERVATION_USER_LIST_CACHE_KEY = "SECKILL_RESERVATION_USER_LIST_CACHE_KEY";
}
