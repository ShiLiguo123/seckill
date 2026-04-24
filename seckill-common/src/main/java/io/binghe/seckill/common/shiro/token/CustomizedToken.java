package io.binghe.seckill.common.shiro.token;

import org.apache.shiro.authc.UsernamePasswordToken;

/**
 * @author binghe
 * @version 1.0.0
 * @description 自定义的Token类
 */
public class CustomizedToken extends UsernamePasswordToken {

	private static final long serialVersionUID = -4242621146969761340L;

    public CustomizedToken(final String username, final String password) {
        super(username, password);
    }

    @Override
    public String toString(){
        return "username=" + super.getUsername()+",password="+ String.valueOf(super.getPassword());
    }

}
