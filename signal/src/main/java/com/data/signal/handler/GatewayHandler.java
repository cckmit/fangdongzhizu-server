package com.data.signal.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.data.signal.constants.Constant;
import com.data.signal.entity.Transmit;
import com.data.signal.utils.NettyUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import static com.data.signal.constants.Constant.*;

@Slf4j
public class GatewayHandler extends SimpleChannelInboundHandler<Object> {


    private WebSocketServerHandshaker handshaker;
    private static String WEBSOCKET_URL = "ws://localhost:29379/websocket";


    /**
     * 网关处理认证消息
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 判断是http消息还是websocket消息
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocket(ctx, (WebSocketFrame) msg);
        }
    }

    /**
     * 处理http请求
     */
    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (!request.decoderResult().isSuccess() || !"websocket".equals(request.headers().get("Upgrade"))) {
            log.warn("-------protobuf不支持websocket！！！-------");
            ctx.channel().close();
            return;
        }
        // 处理http握手请求
        WebSocketServerHandshakerFactory handshakerFactory = new WebSocketServerHandshakerFactory(WEBSOCKET_URL, null, true);
        handshaker = handshakerFactory.newHandshaker(request);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            // 动态加入websocket的编解码处理
            handshaker.handshake(ctx.channel(), request);
            // 存储已经连接的Channel
            NettyUtil.addChannel(ctx.channel());
        }
    }

    /**
     * 处理websocket请求
     */
    private void handleWebSocket(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // 判断是否关闭链路命令
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            NettyUtil.removeChannel(ctx.channel());
            return;
        }
        // 判断是否Ping消息
        if (frame instanceof PingWebSocketFrame) {
            log.info("-------ping 消息:{}-------", frame.content().retain());
            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        // 判断是否Pong消息
        if (frame instanceof PongWebSocketFrame) {
            log.info("-------pong 消息:{}-------", frame.content().retain());
            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        // 当前仅支持文本信息传递
        if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(frame.getClass().getName() + " 不被支持的frame类型");
        }
        // 判断是否文本消息
        if (frame instanceof TextWebSocketFrame) {
            String message = ((TextWebSocketFrame) frame).text();
            JSONObject json = JSONObject.parseObject(message);
            String type = json.getString("type");
            Channel channel = ctx.channel();
            switch (type) {
                case Constant.MSG_PONG_TYPE:
                    NettyUtil.updateUserTime(channel);
                    log.info("-------收到pong消息, address: {}-------",NettyUtil.getChannelRemoteIP(channel));
                    return;
                case Constant.MSG_AUTH_TYPE:
                    boolean isSuccess = NettyUtil.saveUser(channel, json.getString("from"));
                    NettyUtil.sendMsg(channel,JSON.toJSONString(Map.of("type", USER_AUTH_STATUS, USER_AUTH_STATUS, isSuccess)));
                    if (isSuccess) {
                        NettyUtil.broadCastSystemMsg(JSON.toJSONString(Map.of("type", USER_ONLINE_COUNT, USER_ONLINE_COUNT, NettyUtil.getAuthUserCount())));
                    }
                    return;
                case Constant.MSG_IMG_TYPE:
                    Transmit image = NettyUtil.channels.get(channel);
                    image.setType(type);
                    // 将图片消息留给ImageHandler处理
                    break;
                case MSG_TEXT_TYPE:
                    Transmit text = NettyUtil.channels.get(channel);
                    text.setType(type);
                    // 将文字消息留给MessageHandler处理
                    break;
                default:
                    log.warn("-------当前标识符：{} 未被识别，请联系管理员!!!-------", type);
                    return;
            }
        }
        // break消息交给后续流程处理
        ctx.fireChannelRead(frame.retain());
    }

    /**
     * 用户事件回调
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent evnet = (IdleStateEvent) evt;
            // 判断Channel是否读空闲, 读空闲时移除Channel
            if (evnet.state().equals(IdleState.READER_IDLE)) {
                final String remoteAddress = NettyUtil.getChannelRemoteIP(ctx.channel());
                log.warn("网络服务管道通知：有异常空闲通道 [{}]", remoteAddress);
                NettyUtil.removeChannel(ctx.channel());
                NettyUtil.broadCastSystemMsg(JSON.toJSONString(Map.of("type", USER_ONLINE_COUNT, USER_ONLINE_COUNT, NettyUtil.getAuthUserCount())));
            }
        }
        ctx.fireUserEventTriggered(evt);
    }

}
