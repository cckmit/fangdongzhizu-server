package com.data.signal.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class Transmit {

    // 消息源 (谁发的消息)
    private String from;

    // 是否认证
    private Boolean auth;

    // 目标地 (发给谁的问题)
    private String target;

    // 消息
    private String msg;

    // 消息类型 (普通消息，通信消息，系统消息，视频，音频，图片等)
    private String type;

    // 消息状态 (已读，未读)
    private String status;

    // 事件发生时间 (发送消息的时间)
    private String datetime;

}
