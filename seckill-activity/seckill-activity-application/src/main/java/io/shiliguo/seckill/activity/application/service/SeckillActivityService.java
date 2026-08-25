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
package io.shiliguo.seckill.activity.application.service;

import io.shiliguo.seckill.activity.application.command.SeckillActivityCommand;
import io.shiliguo.seckill.activity.domain.model.entity.SeckillActivity;
import io.shiliguo.seckill.common.model.dto.activity.SeckillActivityDTO;

import java.util.Date;
import java.util.List;

/**
 * @version 1.0.0
 * @description 活动
 */
public interface SeckillActivityService {

    /**
     * 保存活动信息
     */
    void saveSeckillActivity(SeckillActivityCommand seckillActivityCommand);

    /**
     * 活动列表
     */
    List<SeckillActivity> getSeckillActivityList(Integer status);

    /**
     * 获取正在进行中的活动列表
     */
    List<SeckillActivity> getSeckillActivityListBetweenStartTimeAndEndTime(Date currentTime, Integer status);
    /**
     * 活动列表
     */
    List<SeckillActivityDTO> getSeckillActivityList(Integer status, Long version);

    /**
     * 获取正在进行中的活动列表
     */
    List<SeckillActivityDTO> getSeckillActivityListBetweenStartTimeAndEndTime(Date currentTime, Integer status, Long version);

    /**
     * 根据id获取活动信息
     */
    @Deprecated
    SeckillActivity getSeckillActivityById(Long id);

    /**
     * 获取活动信息，带有缓存
     */
    SeckillActivityDTO getSeckillActivity(Long id, Long version);

    /**
     * 修改状态
     */
    void updateStatus(Integer status, Long id);
}
