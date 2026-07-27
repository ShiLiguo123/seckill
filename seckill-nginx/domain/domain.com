server {
    listen       11111;
    server_name  localhost;

    error_log logs/domain-error.log error;
    access_log logs/domain-access.log access;
    charset utf-8;

    #模拟用户id
    set_by_lua_block $user_id{
        return "binghe";
    }

    #用户token,可作为标识用户的唯一id
    set_by_lua_file $user_access_token D:/Workspaces/myself/seckill/myself/seckill/seckill/seckill-nginx/lua/set_common_var.lua;

    location / {
        root   html;
        index  index.html index.htm;
    }

    #location ^~ /seckill {
    #    limit_req zone=limit_by_user burst=1 nodelay;
    #    proxy_pass http://real_server;
    #    proxy_set_header Host $host;
    #    proxy_set_header X-Real-IP $remote_addr;
    #    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    #}

    location /seckill-user/user/get {
        limit_req zone=limit_by_user_access_token burst=1 nodelay;
        proxy_pass http://real_server;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /seckill-activity/activity/seckillActivity {
        limit_req zone=limit_by_user_access_token burst=1 nodelay;
        proxy_pass http://real_server;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }


    location /seckill-goods/goods/getSeckillGoods {
        limit_req zone=limit_by_user_access_token burst=1 nodelay;
        proxy_pass http://real_server;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }



    error_page   500 502 503 504  /503_fail.html;
    location = /50x.html {
        root   html;
    }
}

server {
    listen       11112;
    server_name  localhost;

    location /hello {
        default_type text/plain;
        content_by_lua_block {
            ngx.say("{\"response\" : \"hello world!!!\"}")
        }
    }
}