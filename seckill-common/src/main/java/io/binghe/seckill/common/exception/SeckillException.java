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
package io.binghe.seckill.common.exception;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 自定义异常
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public class SeckillException extends RuntimeException{

    private Integer code;

    public SeckillException(String message) {
        super(message);
    }

    public SeckillException(ErrorCode errorCode){
        this(errorCode.getCode(), errorCode.getMesaage());
    }

    public SeckillException(Integer code, String messgae){
        super(messgae);
        this.code = code;
    }


    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}
