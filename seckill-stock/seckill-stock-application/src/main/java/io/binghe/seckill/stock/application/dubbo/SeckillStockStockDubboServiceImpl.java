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
package io.binghe.seckill.stock.application.dubbo;

import io.binghe.seckill.common.cache.model.SeckillBusinessCache;
import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.exception.SeckillException;
import io.binghe.seckill.common.model.dto.stock.SeckillStockDTO;
import io.binghe.seckill.dubbo.interfaces.stock.SeckillStockDubboService;
import io.binghe.seckill.stock.application.service.SeckillStockBucketService;
import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 库存Dubbo服务实现类
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Component
@DubboService(version = "1.0.0")
public class SeckillStockStockDubboServiceImpl implements SeckillStockDubboService {
    private final Logger logger = LoggerFactory.getLogger(SeckillStockStockDubboServiceImpl.class);
    @Autowired
    private SeckillStockBucketService seckillStockBucketService;

    @Override
    public SeckillBusinessCache<Integer> getAvailableStock(Long goodsId, Long version) {
        if (goodsId == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        return seckillStockBucketService.getAvailableStock(goodsId, version);
    }

    @Override
    public SeckillBusinessCache<SeckillStockDTO> getSeckillStock(Long goodsId, Long version) {
        if (goodsId == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        return seckillStockBucketService.getSeckillStock(goodsId, version);
    }
}
