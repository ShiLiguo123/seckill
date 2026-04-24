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
package io.binghe.seckill.goods.infrastructure.repository;

import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.exception.SeckillException;
import io.binghe.seckill.goods.domain.model.entity.SeckillGoods;
import io.binghe.seckill.goods.domain.repository.SeckillGoodsRepository;
import io.binghe.seckill.goods.infrastructure.mapper.SeckillGoodsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 商品
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Component
public class SeckillGoodsRepositoryImpl implements SeckillGoodsRepository {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Override
    public int saveSeckillGoods(SeckillGoods seckillGoods) {
        if (seckillGoods == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        return seckillGoodsMapper.saveSeckillGoods(seckillGoods);
    }

    @Override
    public SeckillGoods getSeckillGoodsId(Long id) {
        return seckillGoodsMapper.getSeckillGoodsId(id);
    }

    @Override
    public List<SeckillGoods> getSeckillGoodsByActivityId(Long activityId) {
        return seckillGoodsMapper.getSeckillGoodsByActivityId(activityId);
    }

    @Override
    public int updateStatus(Integer status, Long id) {
        return seckillGoodsMapper.updateStatus(status, id);
    }

    @Override
    public int updateAvailableStock(Integer count, Long id) {
        return seckillGoodsMapper.updateAvailableStock(count, id);
    }

    @Override
    public Integer getAvailableStockById(Long id) {
        return seckillGoodsMapper.getAvailableStockById(id);
    }
}
