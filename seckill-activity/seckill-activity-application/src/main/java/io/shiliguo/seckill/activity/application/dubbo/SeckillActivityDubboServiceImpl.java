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
package io.shiliguo.seckill.activity.application.dubbo;

import io.shiliguo.seckill.activity.application.service.SeckillActivityService;
import io.shiliguo.seckill.common.model.dto.activity.SeckillActivityDTO;
import io.shiliguo.seckill.dubbo.interfaces.activity.SeckillActivityDubboService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @version 1.0.0
 * @description Dubbo服务
 */
@Component
@DubboService(version = "1.0.0")
public class SeckillActivityDubboServiceImpl implements SeckillActivityDubboService {
    @Autowired
    private SeckillActivityService seckillActivityService;

    @Override
    public SeckillActivityDTO getSeckillActivity(Long id, Long version) {
        return seckillActivityService.getSeckillActivity(id, version);
    }
}
