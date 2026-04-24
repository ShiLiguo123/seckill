package io.binghe.seckill.common.shiro.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.binghe.seckill.common.constants.SeckillConstants;

import java.util.Date;

/**
 * @author binghe
 * @version 1.0.0
 * @description 需要注意的是 这边校验的方式并非非对称加密
 * 可以改造为非对称加密: 公钥加密，私钥解密 私钥生成签名，公钥验证签名
 */
public class JwtUtils {
    /**
     * 校验token是否正确
     * @param token  密钥
     * @return 是否正确
     */
    public static boolean verify(String token, String secret) {
        try {
            //根据密码生成JWT效验器
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withClaim(SeckillConstants.TOKEN_CLAIM, getUserId(token))
                    .build();
            // 效验TOKEN
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException exception) {
            return false;
        }
    }


    /**
     * 获得token中的信息无需secret解密也能获得
     * @return token中包含的用户名
     */
    public static Long getUserId(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaim(SeckillConstants.TOKEN_CLAIM).asLong();
        } catch (JWTDecodeException e) {
            return null;
        }
    }

    /**
     *
     * @param userId   用户名
     * @param secret   制作此token的签名依据
     * @return 加密的token
     */
    public static String sign(Long userId, String secret) {
        Date date = new Date(System.currentTimeMillis() + SeckillConstants.TOKEN_EXPIRE_TIME);
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.create()
                .withClaim(SeckillConstants.TOKEN_CLAIM, userId)
                .withExpiresAt(date)
                .sign(algorithm);
    }
    /**
     *
     * @param userId   用户名
     * @return 加密的token
     */
    public static String sign(Long userId) {
       return sign(userId, SeckillConstants.JWT_SECRET);
    }

}
