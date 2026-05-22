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
package io.binghe.seckill.application.dubbo;

import io.binghe.seckill.application.service.SeckillGoodsService;
import io.binghe.seckill.common.cache.model.SeckillBusinessCache;
import io.binghe.seckill.common.model.dto.goods.SeckillGoodsDTO;
import io.binghe.seckill.dubbo.interfaces.goods.SeckillGoodsDubboService;
import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 商品Dubbo服务实现类
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Component
@DubboService(version = "1.0.0")
public class SeckillGoodsDubboServiceImpl implements SeckillGoodsDubboService {
    private final Logger logger = LoggerFactory.getLogger(SeckillGoodsDubboServiceImpl.class);
    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @Override
    public SeckillGoodsDTO getSeckillGoods(Long id, Long version) {
        return seckillGoodsService.getSeckillGoods(id, version);
    }

    @Override
    public boolean updateDbAvailableStock(Integer count, Long id) {
        return seckillGoodsService.updateDbAvailableStock(count, id);
    }

    @Override
    public boolean updateAvailableStock(Integer count, Long id) {
        return seckillGoodsService.updateAvailableStock(count, id);
    }

    @Override
    public Integer getAvailableStockById(Long goodsId) {
        return seckillGoodsService.getAvailableStockById(goodsId);
    }

    @Override
    public SeckillBusinessCache<Integer> getAvailableStock(Long goodsId, Long version) {
        logger.info("调用商品Dubbo服务");
        return seckillGoodsService.getAvailableStock(goodsId, version);
    }
}
