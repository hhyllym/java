package com.fenglin.springboottest.service;

import com.fenglin.springboottest.common.Result;

/**
 * 好友一对一聊天：会话列表、历史消息、发消息、标记已读、未读总数。
 * 前端 src/chat.js 调 /api/chat/* 这些接口。
 */
public interface ChatService {

    /** 会话列表：每个好友的最后一条消息 + 未读数 */
    Result<?> conversations(Long meId);

    /** 与某个好友的聊天记录（正序，最多 200 条） */
    Result<?> history(Long meId, Long friendId);

    /** 发消息（必须是好友；msgType: text/image） */
    Result<?> send(Long fromId, Long toId, String content, String msgType);

    /** 撤回自己的消息（5 分钟内） */
    Result<?> recall(Long meId, Long msgId);

    /** 通话记录：answered=true 存时长秒数，false 为未接来电 */
    Result<?> callLog(Long fromId, Long toId, int duration, boolean answered);

    /** 把对方发给我的消息标记为已读 */
    Result<?> markRead(Long meId, Long friendId);

    /** 未读消息总数（导航栏红点用） */
    int unreadTotal(Long meId);
}
