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
package io.binghe.seckill.activity.application.service.impl;

import io.binghe.seckill.activity.application.builder.SeckillActivityBuilder;
import io.binghe.seckill.activity.application.cache.service.SeckillActivityCacheService;
import io.binghe.seckill.activity.application.cache.service.SeckillActivityListCacheService;
import io.binghe.seckill.activity.application.command.SeckillActivityCommand;
import io.binghe.seckill.activity.application.service.SeckillActivityService;
import io.binghe.seckill.activity.domain.model.entity.SeckillActivity;
import io.binghe.seckill.activity.domain.service.SeckillActivityDomainService;
import io.binghe.seckill.common.cache.model.SeckillBusinessCache;
import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.exception.SeckillException;
import io.binghe.seckill.common.model.dto.activity.SeckillActivityDTO;
import io.binghe.seckill.common.model.enums.SeckillActivityStatus;
import io.binghe.seckill.common.utils.beans.BeanUtil;
import io.binghe.seckill.common.utils.id.SnowFlakeFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 秒杀活动
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Service
public class SeckillActivityServiceImpl implements SeckillActivityService {
    @Autowired
    private SeckillActivityDomainService seckillActivityDomainService;

    @Autowired
    private SeckillActivityCacheService seckillActivityCacheService;
    @Autowired
    private SeckillActivityListCacheService seckillActivityListCacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSeckillActivity(SeckillActivityCommand seckillActivityCommand) {
        if (seckillActivityCommand == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        SeckillActivity seckillActivity = SeckillActivityBuilder.toSeckillActivity(seckillActivityCommand);
        seckillActivity.setId(SnowFlakeFactory.getSnowFlakeFromCache().nextId());
        seckillActivity.setStatus(SeckillActivityStatus.PUBLISHED.getCode());
        seckillActivityDomainService.saveSeckillActivity(seckillActivity);
    }

    @Override
    public List<SeckillActivity> getSeckillActivityList(Integer status) {
        return seckillActivityDomainService.getSeckillActivityList(status);
    }

    @Override
    public List<SeckillActivity> getSeckillActivityListBetweenStartTimeAndEndTime(Date currentTime, Integer status) {
        return seckillActivityDomainService.getSeckillActivityListBetweenStartTimeAndEndTime(currentTime, status);
    }

    @Override
    public List<SeckillActivityDTO> getSeckillActivityList(Integer status, Long version) {
        SeckillBusinessCache<List<SeckillActivity>> seckillActivitiyListCache = seckillActivityListCacheService.getCachedActivities(status, version);
        //稍后再试，前端需要对这个状态做特殊处理，即不去刷新数据，静默稍后再试
        if (seckillActivitiyListCache.isRetryLater()){
            throw new SeckillException(ErrorCode.RETRY_LATER);
        }

        if (!seckillActivitiyListCache.isExist()){
            throw new SeckillException(ErrorCode.ACTIVITY_NOT_EXISTS);
        }
        List<SeckillActivityDTO> seckillActivityDTOList = seckillActivitiyListCache.getData().stream().map((seckillActivity) -> {
            SeckillActivityDTO seckillActivityDTO = new SeckillActivityDTO();
            BeanUtil.copyProperties(seckillActivity, seckillActivityDTO);
            seckillActivityDTO.setVersion(seckillActivitiyListCache.getVersion());
            return seckillActivityDTO;
        }).collect(Collectors.toList());
        return seckillActivityDTOList;
    }

    @Override
    public List<SeckillActivityDTO> getSeckillActivityListBetweenStartTimeAndEndTime(Date currentTime, Integer status, Long version) {
        SeckillBusinessCache<List<SeckillActivity>> seckillActivitiyListCache = seckillActivityListCacheService.getCachedActivities(currentTime, status, version);
        //稍后再试，前端需要对这个状态做特殊处理，即不去刷新数据，静默稍后再试
        if (seckillActivitiyListCache.isRetryLater()){
            throw new SeckillException(ErrorCode.RETRY_LATER);
        }
        if (!seckillActivitiyListCache.isExist()){
            throw new SeckillException(ErrorCode.ACTIVITY_NOT_EXISTS);
        }
        List<SeckillActivityDTO> seckillActivityDTOList = seckillActivitiyListCache.getData().stream().map((seckillActivity) -> {
            SeckillActivityDTO seckillActivityDTO = new SeckillActivityDTO();
            BeanUtil.copyProperties(seckillActivity, seckillActivityDTO);
            seckillActivityDTO.setVersion(seckillActivitiyListCache.getVersion());
            return seckillActivityDTO;
        }).collect(Collectors.toList());
        return seckillActivityDTOList;
    }

    @Override
    public SeckillActivity getSeckillActivityById(Long id) {
        return seckillActivityDomainService.getSeckillActivityById(id);
    }

    @Override
    public SeckillActivityDTO getSeckillActivity(Long id, Long version) {
        if (id == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        SeckillBusinessCache<SeckillActivity> seckillActivityCache = seckillActivityCacheService.getCachedSeckillActivity(id, version);
        //稍后再试，前端需要对这个状态做特殊处理，即不去刷新数据，静默稍后再试
        if (seckillActivityCache.isRetryLater()){
            throw new SeckillException(ErrorCode.RETRY_LATER);
        }
        //缓存中的活动数据不存在
        if (!seckillActivityCache.isExist()){
            throw new SeckillException(ErrorCode.ACTIVITY_NOT_EXISTS);
        }
        SeckillActivityDTO seckillActivityDTO = SeckillActivityBuilder.toSeckillActivityDTO(seckillActivityCache.getData());
        seckillActivityDTO.setVersion(seckillActivityCache.getVersion());
        return seckillActivityDTO;
    }

    @Override
    public void updateStatus(Integer status, Long id) {
        seckillActivityDomainService.updateStatus(status, id);
    }
}
