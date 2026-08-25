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
package io.shiliguo.seckill.stock.interfaces.controller;

import io.shiliguo.seckill.common.exception.ErrorCode;
import io.shiliguo.seckill.stock.application.model.command.SeckillStockBucketWrapperCommand;
import io.shiliguo.seckill.common.response.ResponseMessage;
import io.shiliguo.seckill.common.response.ResponseMessageBuilder;
import io.shiliguo.seckill.stock.application.model.dto.SeckillStockBucketDTO;
import io.shiliguo.seckill.stock.application.service.SeckillStockBucketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @version 1.0.0
 * @description 分桶库存
 */
@RestController
@RequestMapping(value = "/stock/bucket")
public class SeckillStockBucketController {
    @Autowired
    private SeckillStockBucketService seckillStockBucketService;

    /**
     * 库存分桶
     */
    @RequestMapping(value = "/arrangeStockBuckets", method = {RequestMethod.GET,RequestMethod.POST})
    public ResponseMessage<String> arrangeStockBuckets(@RequestAttribute Long userId, @RequestBody SeckillStockBucketWrapperCommand seckillStockCommond){
        seckillStockBucketService.arrangeStockBuckets(userId, seckillStockCommond);
        return ResponseMessageBuilder.build(ErrorCode.SUCCESS.getCode());
    }

    /**
     * 获取库存分桶数据
     */
    @RequestMapping(value = "/getTotalStockBuckets", method = {RequestMethod.GET,RequestMethod.POST})
    public ResponseMessage<SeckillStockBucketDTO> getTotalStockBuckets(Long goodsId, Long version){
        return ResponseMessageBuilder.build(ErrorCode.SUCCESS.getCode(), seckillStockBucketService.getTotalStockBuckets(goodsId, version));
    }

}
