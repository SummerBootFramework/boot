/*
 * Copyright 2005-2022 Du Law Office - The Summer Boot Framework Project
 *
 * The Summer Boot Project licenses this file to you under the Apache License, version 2.0 (the
 * "License"); you may not use this file except in compliance with the License and you have no
 * policy prohibiting employee contributions back to this file (unless the contributor to this
 * file is your current or retired employee). You may obtain a copy of the License at:
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.summerboot.jexpress.nio.server;

import com.google.inject.Singleton;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.summerboot.jexpress.boot.instrumentation.HealthMonitor;

/**
 *
 * @author Changski Tie Zheng Zhang 张铁铮, 魏泽北, 杜旺财, 杜富贵
 */
@ChannelHandler.Sharable
@Singleton
public class BootHttpPingHandler extends SimpleChannelInboundHandler<HttpObject> {

    protected static Logger log = LogManager.getLogger(BootHttpPingHandler.class.getName());

    private final String pingURL;

    public BootHttpPingHandler(/*String pingURL*/) {
        super(FullHttpRequest.class, false);
        String endpointCfg = NioServerContext.getLoadBalancingEndpoint();
        if (StringUtils.isNotBlank(endpointCfg)) {
            pingURL = endpointCfg;
        } else {
            pingURL = null;
        }
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final HttpObject httpObject) throws Exception {
        boolean isPingRequest = false;
        if (pingURL != null && (httpObject instanceof HttpRequest)) {
            HttpRequest req = (HttpRequest) httpObject;
            if (HttpMethod.GET.equals(req.method()) && pingURL.equals(req.uri())) {
                isPingRequest = true;
                NioServerContext.COUNTER_PING_HIT.incrementAndGet();
                try {
                    HttpResponseStatus status = HealthMonitor.isServiceAvaliable() ? HttpResponseStatus.OK : HttpResponseStatus.SERVICE_UNAVAILABLE;
                    NioHttpUtil.sendText(ctx, HttpUtil.isKeepAlive((HttpRequest) req), null, status, null, null, null, true);
                } finally {
                    ReferenceCountUtil.release(req);
                }
            }
        }
        if (!isPingRequest) {
            //pass to next Handler
            ctx.fireChannelRead(httpObject);
        }
    }
}
