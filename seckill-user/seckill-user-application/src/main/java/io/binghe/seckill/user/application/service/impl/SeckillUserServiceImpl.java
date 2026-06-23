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
package io.binghe.seckill.user.application.service.impl;

import io.binghe.seckill.common.cache.distribute.DistributedCacheService;
import io.binghe.seckill.common.constants.SeckillConstants;
import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.exception.SeckillException;
import io.binghe.seckill.common.shiro.utils.CommonsUtils;
import io.binghe.seckill.common.shiro.utils.JwtUtils;
import io.binghe.seckill.user.application.service.SeckillUserService;
import io.binghe.seckill.user.domain.model.entity.SeckillUser;
import io.binghe.seckill.user.domain.repository.SeckillUserRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 用户Service
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Service
public class SeckillUserServiceImpl implements SeckillUserService {
    @Autowired
    private SeckillUserRepository seckillUserRepository;
    @Autowired
    private DistributedCacheService distributedCacheService;

    @Override
    public String login(String userName, String password) {
        if (StringUtils.isEmpty(userName)){
            throw new SeckillException(ErrorCode.USERNAME_IS_NULL);
        }
        if (StringUtils.isEmpty(password)){
            throw new SeckillException(ErrorCode.PASSWORD_IS_NULL);
        }
        SeckillUser seckillUser = seckillUserRepository.getSeckillUserByUserName(userName);
        if (seckillUser == null){
            throw new SeckillException(ErrorCode.USERNAME_IS_ERROR);
        }
        String paramsPassword = CommonsUtils.encryptPassword(password, userName);
        if (!paramsPassword.equals(seckillUser.getPassword())){
            throw new SeckillException(ErrorCode.PASSWORD_IS_ERROR);
        }
        String token = JwtUtils.sign(seckillUser.getId());
        String key = SeckillConstants.getKey(SeckillConstants.USER_KEY_PREFIX, String.valueOf(seckillUser.getId()));
        //缓存到Redis
        distributedCacheService.put(key, seckillUser);
        //返回Token
        return token;
    }

    @Override
    public SeckillUser getSeckillUserByUserName(String userName) {
        SeckillUser seckillUser = seckillUserRepository.getSeckillUserByUserName(userName);
        if (seckillUser != null){
            seckillUser.setPassword("");
        }
        return seckillUser;
    }

}
