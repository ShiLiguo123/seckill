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
package io.binghe.seckill.activity.application.builder;

import io.binghe.seckill.activity.application.command.SeckillActivityCommand;
import io.binghe.seckill.activity.domain.model.entity.SeckillActivity;
import io.binghe.seckill.common.builder.SeckillCommonBuilder;
import io.binghe.seckill.common.model.dto.activity.SeckillActivityDTO;
import io.binghe.seckill.common.utils.beans.BeanUtil;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 秒杀活动构建类
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public class SeckillActivityBuilder extends SeckillCommonBuilder {

    public static SeckillActivity toSeckillActivity(SeckillActivityCommand seckillActivityCommand){
        if (seckillActivityCommand == null){
            return null;
        }
        SeckillActivity seckillActivity = new SeckillActivity();
        BeanUtil.copyProperties(seckillActivityCommand, seckillActivity);
        return seckillActivity;
    }

    public static SeckillActivityDTO toSeckillActivityDTO(SeckillActivity seckillActivity){
        if (seckillActivity == null){
            return null;
        }
        SeckillActivityDTO seckillActivityDTO = new SeckillActivityDTO();
        BeanUtil.copyProperties(seckillActivity, seckillActivityDTO);
        return seckillActivityDTO;
    }
}
