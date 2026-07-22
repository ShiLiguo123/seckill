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
package io.binghe.seckill.activity.application.service.fallback;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import io.binghe.seckill.common.model.dto.activity.SeckillActivityDTO;
import io.binghe.seckill.common.model.enums.SeckillActivityStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Component
public class SeckillActivityFallbackService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeckillActivityFallbackService.class);

    public static SeckillActivityDTO getSeckillActivityBlockHandler(Long id, Long version, BlockException e){
        LOGGER.info("getSeckillActivityBlockHandler|处理lockHandler|{},{},{}",id, version, e.getMessage());
        Date date = new Date();
        return new SeckillActivityDTO(-1001L, "BlockHandler活动", date, date, SeckillActivityStatus.OFFLINE.getCode(), "BlockHandler活动", 0L);
    }

    public static SeckillActivityDTO getSeckillActivityFallback(Long id, Long version, Throwable t){
        LOGGER.info("getSeckillActivityBlockHandler|处理Fallback|{},{},{}", id, version, t.getMessage());
        Date date = new Date();
        return new SeckillActivityDTO(-1001L, "Fallback活动", date, date, SeckillActivityStatus.OFFLINE.getCode(), "Fallback活动", 0L);
    }
}
