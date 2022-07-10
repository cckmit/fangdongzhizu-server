package com.data.signal.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.data.signal.entity.Transmit;
import com.data.signal.utils.NettyUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.Random;
import static com.data.signal.constants.Constant.USER_ONLINE_COUNT;

@Slf4j
public class ImageHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    /**
     * 处理客户端发送的消息
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) throws Exception {
        Transmit transmit = NettyUtil.getUserInfo(ctx.channel());
        if (transmit != null && transmit.getAuth() && "img".equals(transmit.getType())) {
            JSONObject json = JSONObject.parseObject(frame.text());
            // 广播返回用户发送的消息文本
            transmit.setId(new Random().nextLong());
            transmit.setMsg(json.getString("msg"));
            transmit.setType(json.getString("type"));
            transmit.setTarget(json.getString("target"));
            transmit.setStatus("true"); // 消息状态默认已读
            System.out.println("--------------> 这是我想看的 [图片] 传输数据:" + transmit.toString());
            NettyUtil.broadcastGeneralMsg(JSON.toJSONString(transmit));
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        NettyUtil.removeChannel(ctx.channel());
        NettyUtil.broadCastSystemMsg(JSON.toJSONString(Map.of("type", USER_ONLINE_COUNT, USER_ONLINE_COUNT, NettyUtil.getAuthUserCount())));
        super.channelUnregistered(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("-------服务连接错误并关闭通道-------", cause);
        NettyUtil.removeChannel(ctx.channel());
        NettyUtil.broadCastSystemMsg(JSON.toJSONString(Map.of("type", USER_ONLINE_COUNT, USER_ONLINE_COUNT, NettyUtil.getAuthUserCount())));
    }


}
