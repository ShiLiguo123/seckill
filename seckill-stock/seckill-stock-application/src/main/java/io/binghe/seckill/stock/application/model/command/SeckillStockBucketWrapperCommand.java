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
package io.binghe.seckill.stock.application.model.command;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 库存分桶
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public class SeckillStockBucketWrapperCommand extends SeckillStockBucketGoodsCommand {
    private static final long serialVersionUID = 2920951547657301665L;

    //库存分桶信息
    private SeckillStockBucketCommand stockBucketCommand;

    public SeckillStockBucketWrapperCommand() {
    }

    public SeckillStockBucketWrapperCommand(Long userId, Long goodsId, SeckillStockBucketCommand stockBucketCommand) {
        super(userId, goodsId);
        this.stockBucketCommand = stockBucketCommand;
    }

    public SeckillStockBucketCommand getStockBucketCommand() {
        return stockBucketCommand;
    }

    public void setStockBucketCommand(SeckillStockBucketCommand stockBucketCommand) {
        this.stockBucketCommand = stockBucketCommand;
    }

    public boolean isEmpty(){
        return this.stockBucketCommand == null
                || super.isEmpty()
                || stockBucketCommand.isEmpty();
    }
}
