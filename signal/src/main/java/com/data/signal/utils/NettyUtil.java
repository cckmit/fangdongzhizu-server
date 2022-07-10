package com.data.signal.utils;

import com.alibaba.fastjson.JSON;
import com.data.signal.entity.Transmit;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import static com.data.signal.constants.Constant.MSG_PING_TYPE;

@Slf4j
public class NettyUtil {

    private static AtomicInteger userCount = new AtomicInteger(0);
    private static ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
    public static ConcurrentMap<Channel, Transmit> channels = new ConcurrentHashMap<>();


    public static boolean saveUser(Channel channel, String from) {
        Transmit transmit = channels.get(channel);
        if (transmit == null) {
            return false;
        }
        if (!channel.isActive()) {
            log.error("当前服务通道未激活，请联系管理员, 名称是: {}", from);
            return false;
        }
        // 增加一个认证用户
        userCount.incrementAndGet();
        transmit.setFrom(from);
        transmit.setAuth(true);
        transmit.setDatetime(String.valueOf(System.currentTimeMillis()));
        return true;
    }

    /**
     * 获取Channel的远程IP地址
     */
    public static String getChannelRemoteIP(final Channel channel) {
        if (null == channel) {
            return "";
        }
        SocketAddress remote = channel.remoteAddress();
        final String addr = remote != null ? remote.toString() : "";
        if (addr.length() > 0) {
            int index = addr.lastIndexOf("/");
            if (index >= 0) {
                return addr.substring(index + 1);
            }
            return addr;
        }
        return "";
    }

    /**
     * 在缓存中移除channel
     */
    public static void removeChannel(Channel channel) {
        try {
            log.warn("-------channel将要被移除, 地址是:{}-------", NettyUtil.getChannelRemoteIP(channel));
            rwLock.writeLock().lock();
            channel.close();
            Transmit transmit = channels.get(channel);
            if (transmit != null) {
                Transmit remove = channels.remove(channel);
                if (remove != null && remove.getAuth()) {
                    // 减去一个认证用户
                    userCount.decrementAndGet();
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 在缓存中添加channel
     */
    public static void addChannel(Channel channel) {
        String remoteAddr = getChannelRemoteIP(channel);
        if (!channel.isActive()) {
            log.error("-------channel处于未激活的状态, 地址是: {}-------", remoteAddr);
        }
        // 这里需要放入全量数据
        Transmit transmit = new Transmit();
        transmit.setDatetime(String.valueOf(System.currentTimeMillis()));
        channels.put(channel, transmit);
    }

    /**
     * 发送系统消息
     */
    public static void sendMsg(Channel channel, String msg) {
        channel.writeAndFlush(new TextWebSocketFrame(msg));
    }

    /**
     * 广播系统消息
     */
    public static void broadCastSystemMsg(String msg) {
        try {
            rwLock.readLock().lock();
            Set<Channel> keySet = channels.keySet();
            for (Channel ch : keySet) {
                Transmit transmit = channels.get(ch);
                if (transmit == null || !transmit.getAuth()) continue;
                ch.writeAndFlush(new TextWebSocketFrame(msg));
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * 广播ping消息
     */
    public static void broadCastPingMsg() {
        try {
            rwLock.readLock().lock();
            log.info("-------广播Ping 当前在线用户数共计: {}-------", userCount.intValue());
            Set<Channel> keySet = channels.keySet();
            for (Channel ch : keySet) {
                Transmit transmit = channels.get(ch);
                if (transmit == null || !transmit.getAuth()) continue;
                ch.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(Map.of("type", MSG_PING_TYPE, MSG_PING_TYPE, "ping"))));
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * 广播普通消息
     */
    public static void broadcastGeneralMsg(String data) {
        if (!isBlank(data)) {
            try {
                rwLock.readLock().lock();
                Set<Channel> keySet = channels.keySet();
                for (Channel ch : keySet) {
                    Transmit transmit = channels.get(ch);
                    if (transmit == null || !transmit.getAuth()) continue;
                    ch.writeAndFlush(new TextWebSocketFrame(data));
                }
            } finally {
                rwLock.readLock().unlock();
            }
        }
    }

    /**
     * 扫描并关闭失效的Channel
     */
    public static void scanNotActiveChannel() {
        Set<Channel> keySet = channels.keySet();
        for (Channel ch : keySet) {
            Transmit transmit = channels.get(ch);
            if (transmit == null) continue;
            if (!ch.isOpen() || !ch.isActive() || (!transmit.getAuth() && (System.currentTimeMillis() - Long.parseLong(transmit.getDatetime())) > 10000)) {
                removeChannel(ch);
            }
        }
    }

    /**
     * 设置ping/pong事件时间
     */
    public static void updateUserTime(Channel channel) {
        Transmit transmit = getUserInfo(channel);
        if (transmit != null) {
            transmit.setDatetime(String.valueOf(System.currentTimeMillis()));
        }
    }

    /**
     * 获取认证用户信息
     */
    public static Transmit getUserInfo(Channel channel) {
        return channels.get(channel);
    }

    /**
     * 获取认证用户数量
     */
    public static int getAuthUserCount() {
        return userCount.get();
    }

    /**
     * 判断字符串是否为空
     */
    public static boolean isBlank(final String str) {
        return (str == null) || (str.trim().length() <= 0);
    }

}
