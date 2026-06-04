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
package io.binghe.seckill.reservation.application.service.impl;

import cn.hutool.core.util.NumberUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.binghe.seckill.common.cache.distribute.DistributedCacheService;
import io.binghe.seckill.common.cache.model.SeckillBusinessCache;
import io.binghe.seckill.common.constants.SeckillConstants;
import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.exception.SeckillException;
import io.binghe.seckill.common.model.dto.goods.SeckillGoodsDTO;
import io.binghe.seckill.common.model.enums.SeckillReservationConfigStatus;
import io.binghe.seckill.common.utils.beans.BeanUtil;
import io.binghe.seckill.common.utils.id.SnowFlakeFactory;
import io.binghe.seckill.common.utils.string.StringUtil;
import io.binghe.seckill.dubbo.interfaces.goods.SeckillGoodsDubboService;
import io.binghe.seckill.reservation.application.builder.SeckillReservationConfigBuilder;
import io.binghe.seckill.reservation.application.cache.SeckillReservationConfigCacheService;
import io.binghe.seckill.reservation.application.command.SeckillReservationConfigCommand;
import io.binghe.seckill.reservation.application.service.SeckillReservationService;
import io.binghe.seckill.reservation.domain.model.entity.SeckillReservationConfig;
import io.binghe.seckill.reservation.domain.service.SeckillReservationDomainService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description SeckillReservationService实现类
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Service
public class SeckillReservationServiceImpl implements SeckillReservationService {
    private final Logger logger = LoggerFactory.getLogger(SeckillReservationServiceImpl.class);
    @Autowired
    private SeckillReservationConfigCacheService seckillReservationConfigCacheService;
    @Autowired
    private SeckillReservationDomainService seckillReservationDomainService;
    @Autowired
    private DistributedCacheService distributedCacheService;
    @DubboReference(version = "1.0.0", check = false)
    private SeckillGoodsDubboService seckillGoodsDubboService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveSeckillReservationConfig(SeckillReservationConfigCommand seckillReservationConfigCommand) {
        if (seckillReservationConfigCommand == null || seckillReservationConfigCommand.isEmpty()){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        SeckillGoodsDTO seckillGoods = seckillGoodsDubboService.getSeckillGoods(seckillReservationConfigCommand.getGoodsId(), 0L);
        if (seckillGoods == null){
            throw new SeckillException(ErrorCode.GOODS_NOT_EXISTS);
        }
        SeckillReservationConfig seckillReservationConfig = null;
        SeckillBusinessCache<SeckillReservationConfig> seckillReservationConfigCache = seckillReservationConfigCacheService.getSeckillReservationConfig(seckillReservationConfigCommand.getGoodsId(), 0L);
        if (seckillReservationConfigCache.isExist() && seckillReservationConfigCache.getData() != null){
            seckillReservationConfig = seckillReservationConfigCache.getData();
        }
        if (seckillReservationConfig != null){
            throw new SeckillException(ErrorCode.GOODS_RESERVATION_CONFIG_EXISTS);
        }
        if (new Date().after(seckillReservationConfigCommand.getReserveEndTime()) || seckillReservationConfigCommand.getReserveEndTime().after(seckillGoods.getStartTime())){
            throw new SeckillException(ErrorCode.GOODS_RESERVATION_TIME_NOT_INVALIDATE);
        }
        String luaKey = StringUtil.append(SeckillConstants.RESERVATION_CONFIG, seckillReservationConfigCommand.getGoodsId());
        Long result = distributedCacheService.checkExecute(luaKey, SeckillConstants.SUBMIT_DATA_EXECUTE_EXPIRE_SECONDS);
        //已经预约过，操作过于频繁，处理幂等性问题
        if (NumberUtil.equals(result, SeckillConstants.CHECK_RECOVER_STOCK_HAS_EXECUTE)){
            logger.info("reserveGoods|已经执行过预约方法|{}", JSONObject.toJSONString(seckillReservationConfigCommand));
            throw new SeckillException(ErrorCode.RETRY_LATER);
        }
        seckillReservationConfig = SeckillReservationConfigBuilder.toSeckillReservationConfig(seckillReservationConfigCommand);
        seckillReservationConfig.setId(SnowFlakeFactory.getSnowFlakeFromCache().nextId());
        seckillReservationConfig.setGoodsName(seckillGoods.getGoodsName());
        seckillReservationConfig.setSeckillStartTime(seckillGoods.getStartTime());
        seckillReservationConfig.setSeckillEndTime(seckillGoods.getEndTime());
        seckillReservationConfig.setReserveCurrentUserCount(0);
        seckillReservationConfig.setStatus(SeckillReservationConfigStatus.PUBLISHED.getCode());
        boolean success = seckillReservationDomainService.saveSeckillReservationConfig(seckillReservationConfig);
        if (!success){
            logger.info("saveSeckillReservationConfig|添加商品预约配置失败|{}", JSON.toJSONString(seckillReservationConfigCommand));
        }
        return success;
    }

    @Override
    public boolean updateSeckillReservationConfig(SeckillReservationConfigCommand seckillReservationConfigCommand) {
        if (seckillReservationConfigCommand == null || seckillReservationConfigCommand.isEmpty()){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        SeckillReservationConfig seckillReservationConfig = null;
        SeckillBusinessCache<SeckillReservationConfig> seckillReservationConfigCache = seckillReservationConfigCacheService.getSeckillReservationConfig(seckillReservationConfigCommand.getGoodsId(), 0L);
        if (seckillReservationConfigCache.isExist() && seckillReservationConfigCache.getData() != null){
            seckillReservationConfig = seckillReservationConfigCache.getData();
        }
        if (seckillReservationConfig == null){
            throw new SeckillException(ErrorCode.GOODS_RESERVATION_CONFIG_NOT_EXISTS);
        }
        if (seckillReservationConfigCommand.getReserveMaxUserCount() < seckillReservationConfig.getReserveCurrentUserCount()){
            throw new SeckillException(ErrorCode.GOODS_RESERVATION_COUNT_INVALIDATE);
        }
        if (new Date().after(seckillReservationConfigCommand.getReserveEndTime()) || seckillReservationConfigCommand.getReserveEndTime().after(seckillReservationConfig.getSeckillStartTime())){
            throw new SeckillException(ErrorCode.GOODS_RESERVATION_TIME_NOT_INVALIDATE);
        }
        BeanUtil.copyProperties(seckillReservationConfigCommand, seckillReservationConfig);
        return seckillReservationDomainService.updateSeckillReservationConfig(seckillReservationConfig);
    }

    @Override
    public boolean updateConfigStatus(Integer status, Long goodsId) {
        if (status == null || goodsId == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        return seckillReservationDomainService.updateConfigStatus(status, goodsId);
    }

    @Override
    public List<SeckillReservationConfig> getConfigList(Long version) {
        SeckillBusinessCache<List<SeckillReservationConfig>> seckillReservationConfigListCache = seckillReservationConfigCacheService.getSeckillReservationConfigList(version);
        //稍后再试，前端需要对这个状态做特殊处理，即不去刷新数据，静默稍后再试
        if (seckillReservationConfigListCache.isRetryLater()){
            throw new SeckillException(ErrorCode.RETRY_LATER);
        }
        //缓存中不存在配置数据
        if (!seckillReservationConfigListCache.isExist()){
            throw new SeckillException(ErrorCode.GOODS_RESERVATION_CONFIG_NOT_EXISTS);
        }
        return seckillReservationConfigListCache.getData();
    }

    @Override
    public SeckillReservationConfig getConfigDetail(Long goodsId, Long version) {
        if (goodsId == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        SeckillBusinessCache<SeckillReservationConfig> seckillReservationConfigCache = seckillReservationConfigCacheService.getSeckillReservationConfig(goodsId, version);
        //稍后再试，前端需要对这个状态做特殊处理，即不去刷新数据，静默稍后再试
        if (seckillReservationConfigCache.isRetryLater()){
            throw new SeckillException(ErrorCode.RETRY_LATER);
        }
        //缓存中不存在配置数据
        if (!seckillReservationConfigCache.isExist()){
            throw new SeckillException(ErrorCode.GOODS_RESERVATION_CONFIG_NOT_EXISTS);
        }
        return seckillReservationConfigCache.getData();
    }

}
