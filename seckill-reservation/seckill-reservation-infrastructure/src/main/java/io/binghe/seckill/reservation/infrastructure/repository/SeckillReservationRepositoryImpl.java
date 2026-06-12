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
package io.binghe.seckill.reservation.infrastructure.repository;

import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.exception.SeckillException;
import io.binghe.seckill.reservation.domain.model.entity.SeckillReservationConfig;
import io.binghe.seckill.reservation.domain.model.entity.SeckillReservationUser;
import io.binghe.seckill.reservation.domain.repository.SeckillReservationRepository;
import io.binghe.seckill.reservation.infrastructure.mapper.SeckillReservationConfigMapper;
import io.binghe.seckill.reservation.infrastructure.mapper.SeckillReservationGoodsMapper;
import io.binghe.seckill.reservation.infrastructure.mapper.SeckillReservationUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 预约服务Repository实现类
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Component
public class SeckillReservationRepositoryImpl implements SeckillReservationRepository {

    @Autowired
    private SeckillReservationConfigMapper seckillReservationConfigMapper;
    @Autowired
    private SeckillReservationUserMapper seckillReservationUserMapper;
    @Autowired
    private SeckillReservationGoodsMapper seckillReservationGoodsMapper;

    @Override
    public boolean saveSeckillReservationConfig(SeckillReservationConfig seckillReservationConfig) {
        if (seckillReservationConfig == null || seckillReservationConfig.isEmpty()){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        return seckillReservationConfigMapper.saveSeckillReservationConfig(seckillReservationConfig) == 1;
    }

    @Override
    public boolean updateSeckillReservationConfig(SeckillReservationConfig seckillReservationConfig) {
        if (seckillReservationConfig == null || seckillReservationConfig.isEmpty()){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        return seckillReservationConfigMapper.updateSeckillReservationConfig(seckillReservationConfig) == 1;
    }

    @Override
    public boolean updateConfigStatus(Integer status, Long goodsId) {
        if (status == null || goodsId == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        return seckillReservationConfigMapper.updateStatus(status, goodsId) == 1;
    }

    @Override
    public int updateReserveCurrentUserCount(Integer reserveCurrentUserCount, Long goodsId) {
        if (reserveCurrentUserCount == null || goodsId == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        return seckillReservationConfigMapper.updateReserveCurrentUserCount(reserveCurrentUserCount, goodsId);
    }

    @Override
    public List<SeckillReservationConfig> getConfigList() {
        return seckillReservationConfigMapper.getConfigList();
    }

    @Override
    public SeckillReservationConfig getConfigDetail(Long goodsId) {
        if (goodsId == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        return seckillReservationConfigMapper.getConfigDetail(goodsId);
    }

    @Override
    public List<SeckillReservationUser> getUserListByGoodsId(Long goodsId, Integer status) {
        if (goodsId == null || status == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        return seckillReservationGoodsMapper.getUserListByGoodsId(goodsId, status);
    }

    @Override
    public List<SeckillReservationUser> getGoodsListByUserId(Long userId, Integer status) {
        if (userId == null || status == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        return seckillReservationUserMapper.getGoodsListByUserId(userId, status);
    }

    @Override
    public boolean reserveGoods(SeckillReservationUser seckillReservationUser) {
        if (seckillReservationUser == null || seckillReservationUser.isEmpty()){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        boolean userResult = seckillReservationUserMapper.reserveGoods(seckillReservationUser) == 1;
        boolean goodsResult = seckillReservationGoodsMapper.reserveGoods(seckillReservationUser) == 1;
        return userResult && goodsResult;
    }

    @Override
    public boolean cancelReserveGoods(Long goodsId, Long userId) {
        if (goodsId == null || userId == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        boolean userResult = seckillReservationUserMapper.cancelReserveGoods(goodsId, userId) == 1;
        boolean goodsResult = seckillReservationGoodsMapper.cancelReserveGoods(goodsId, userId) == 1;
        return userResult && goodsResult;
    }

    @Override
    public SeckillReservationUser getSeckillReservationUser(Long userId, Long goodsId, Integer status) {
        if (goodsId == null || userId == null || status == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        return seckillReservationUserMapper.getSeckillReservationUser(userId, goodsId, status);
    }
}
