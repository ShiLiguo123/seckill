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
package io.binghe.seckill.dubbo.interfaces.filter;

import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.slf4j.MDC;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description Dubbo链路追踪
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Activate(group = {CommonConstants.PROVIDER, CommonConstants.CONSUMER}, value = "tracing")
public class DubboTraceFilter implements Filter {

    /**
     * TraceId key
     */
    private static final String TRACE_ID = "traceId";
    /**
     * SpanId key
     */
    private static final String SPAN_ID = "spanId";

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 获取dubbo上下文中的traceId
        String traceId = RpcContext.getContext().getAttachment(TRACE_ID);
        String spanId = RpcContext.getContext().getAttachment(SPAN_ID);
        if (StringUtils.isBlank(traceId) ) {
            // customer 设置traceId到dubbo的上下文
            RpcContext.getContext().setAttachment(TRACE_ID, MDC.get(TRACE_ID));
        } else {
            // provider 设置traceId到日志的上下文
            MDC.put(TRACE_ID, traceId);
        }
        if (StringUtils.isBlank(spanId)){
            // customer 设置spanId到dubbo的上下文
            RpcContext.getContext().setAttachment(SPAN_ID, MDC.get(SPAN_ID));
        }else{
            // provider 设置traceId到日志的上下文
            MDC.put(SPAN_ID, spanId);
        }

        return invoker.invoke(invocation);
    }
}
