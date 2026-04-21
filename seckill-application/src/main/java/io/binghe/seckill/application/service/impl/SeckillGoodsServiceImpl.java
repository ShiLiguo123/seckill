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
package io.binghe.seckill.application.service.impl;

import io.binghe.seckill.application.builder.SeckillGoodsBuilder;
import io.binghe.seckill.application.cache.model.SeckillBusinessCache;
import io.binghe.seckill.application.cache.service.goods.SeckillGoodsCacheService;
import io.binghe.seckill.application.cache.service.goods.SeckillGoodsListCacheService;
import io.binghe.seckill.application.command.SeckillGoodsCommond;
import io.binghe.seckill.application.service.SeckillGoodsService;
import io.binghe.seckill.domain.code.HttpCode;
import io.binghe.seckill.domain.constants.SeckillConstants;
import io.binghe.seckill.domain.exception.SeckillException;
import io.binghe.seckill.domain.model.dto.SeckillGoodsDTO;
import io.binghe.seckill.domain.model.entity.SeckillActivity;
import io.binghe.seckill.domain.model.entity.SeckillGoods;
import io.binghe.seckill.domain.model.enums.SeckillGoodsStatus;
import io.binghe.seckill.domain.service.SeckillActivityDomainService;
import io.binghe.seckill.domain.service.SeckillGoodsDomainService;
import io.binghe.seckill.infrastructure.cache.distribute.DistributedCacheService;
import io.binghe.seckill.infrastructure.utils.beans.BeanUtil;
import io.binghe.seckill.infrastructure.utils.id.SnowFlakeFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 商品服务
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {
    @Autowired
    private SeckillGoodsDomainService seckillGoodsDomainService;
    @Autowired
    private SeckillActivityDomainService seckillActivityDomainService;
    @Autowired
    private SeckillGoodsListCacheService seckillGoodsListCacheService;
    @Autowired
    private SeckillGoodsCacheService seckillGoodsCacheService;
    @Autowired
    private DistributedCacheService distributedCacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSeckillGoods(SeckillGoodsCommond seckillGoodsCommond) {
        if (seckillGoodsCommond == null){
            throw new SeckillException(HttpCode.PARAMS_INVALID);
        }
        SeckillActivity seckillActivity = seckillActivityDomainService.getSeckillActivityById(seckillGoodsCommond.getActivityId());
        if (seckillActivity == null){
            throw new SeckillException(HttpCode.ACTIVITY_NOT_EXISTS);
        }
        SeckillGoods seckillGoods = SeckillGoodsBuilder.toSeckillGoods(seckillGoodsCommond);
        seckillGoods.setStartTime(seckillActivity.getStartTime());
        seckillGoods.setEndTime(seckillActivity.getEndTime());
        seckillGoods.setAvailableStock(seckillGoodsCommond.getInitialStock());
        seckillGoods.setId(SnowFlakeFactory.getSnowFlakeFromCache().nextId());
        seckillGoods.setStatus(SeckillGoodsStatus.PUBLISHED.getCode());
        seckillGoodsDomainService.saveSeckillGoods(seckillGoods);
    }

    @Override
    public SeckillGoods getSeckillGoodsId(Long id) {
       return seckillGoodsDomainService.getSeckillGoodsId(id);
    }


    @Override
    public List<SeckillGoods> getSeckillGoodsByActivityId(Long activityId) {
        return seckillGoodsDomainService.getSeckillGoodsByActivityId(activityId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer status, Long id) {
        if (status == SeckillGoodsStatus.OFFLINE.getCode()){
            //清空缓存
            this.clearCache(String.valueOf(id));
        }
        seckillGoodsDomainService.updateStatus(status, id);
    }

    /**
     * 清空缓存的商品数据
     */
    private void clearCache(String id){
        //清除缓存中的商品库存
        distributedCacheService.delete(SeckillConstants.getKey(SeckillConstants.GOODS_ITEM_STOCK_KEY_PREFIX, id));
        //清除本地缓存中的商品
        distributedCacheService.delete(SeckillConstants.getKey(SeckillConstants.GOODS_ITEM_KEY_PREFIX, id));
        //清除商品的限购信息
        distributedCacheService.delete(SeckillConstants.getKey(SeckillConstants.GOODS_ITEM_LIMIT_KEY_PREFIX, id));
    }

    @Override
    public boolean updateAvailableStock(Integer count, Long id) {
        return seckillGoodsDomainService.updateAvailableStock(count, id);
    }
    @Override
    public boolean updateDbAvailableStock(Integer count, Long id) {
        return seckillGoodsDomainService.updateDbAvailableStock(count, id);
    }

    @Override
    public Integer getAvailableStockById(Long id) {
        return seckillGoodsDomainService.getAvailableStockById(id);
    }

    @Override
    public List<SeckillGoodsDTO> getSeckillGoodsList(Long activityId, Long version) {
        if (activityId == null){
            throw new SeckillException(HttpCode.ACTIVITY_NOT_EXISTS);
        }
        SeckillBusinessCache<List<SeckillGoods>> seckillGoodsListCache = seckillGoodsListCacheService.getCachedGoodsList(activityId, version);
        //稍后再试，前端需要对这个状态做特殊处理，即不去刷新数据，静默稍后再试
        if (seckillGoodsListCache.isRetryLater()){
            throw new SeckillException(HttpCode.RETRY_LATER);
        }
        if (!seckillGoodsListCache.isExist()){
            throw new SeckillException(HttpCode.ACTIVITY_NOT_EXISTS);
        }
        List<SeckillGoodsDTO> seckillActivityDTOList = seckillGoodsListCache.getData().stream().map((seckillGoods) -> {
            SeckillGoodsDTO seckillGoodsDTO = new SeckillGoodsDTO();
            BeanUtil.copyProperties(seckillGoods, seckillGoodsDTO);
            seckillGoodsDTO.setVersion(seckillGoodsListCache.getVersion());
            return seckillGoodsDTO;
        }).collect(Collectors.toList());
        return seckillActivityDTOList;
    }

    @Override
    public SeckillGoodsDTO getSeckillGoods(Long id, Long version) {
        if (id == null){
            throw new SeckillException(HttpCode.PARAMS_INVALID);
        }
        SeckillBusinessCache<SeckillGoods> seckillGoodsCache = seckillGoodsCacheService.getSeckillGoods(id, version);
        //稍后再试，前端需要对这个状态做特殊处理，即不去刷新数据，静默稍后再试
        if (seckillGoodsCache.isRetryLater()){
            throw new SeckillException(HttpCode.RETRY_LATER);
        }
        //缓存中不存在商品数据
        if (!seckillGoodsCache.isExist()){
            throw new SeckillException(HttpCode.ACTIVITY_NOT_EXISTS);
        }
        SeckillGoodsDTO seckillGoodsDTO = SeckillGoodsBuilder.toSeckillGoodsDTO(seckillGoodsCache.getData());
        seckillGoodsDTO.setVersion(seckillGoodsCache.getVersion());
        return seckillGoodsDTO;
    }
}
