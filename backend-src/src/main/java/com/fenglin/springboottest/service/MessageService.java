package com.fenglin.springboottest.service;

import com.fenglin.springboottest.common.Result;

import java.util.Map;

/**
 * 消息中心业务：留言板 / 站内私信 / 系统通知。
 * 列表类方法返回 { list, total, page, size, pages, hasMore }（和 PonyServiceImpl.search 同构）；
 * 写操作返回 Result（code=200 算成功，前端判断 code===200）。
 */
public interface MessageService {

    /** 发留言板（公开墙） */
    Result<?> postBoard(Long fromId, String fromName, String content);

    /** 留言板列表（最新在前） */
    Map<String, Object> boardList(int page, int size);

    /** 发私信：toName 自动解析成 toId */
    Result<?> sendPm(Long fromId, String fromName, String toName, String content);

    /** 私信列表：box=inbox 收件箱 / sent 发件箱 */
    Map<String, Object> pmList(String box, Long userId, int page, int size);

    /** 标记私信已读（只有收件人能标） */
    Result<?> readPm(Long id, Long userId);

    /** 系统通知列表（含 to_id=-1 的全员广播） */
    Map<String, Object> notifyList(Long userId, int page, int size);

    /** 标记系统通知已读（广播公告为全员共享，标记为 no-op） */
    Result<?> readNotify(Long id, Long userId);

    /** 未读计数：{ pm, notify, total }（广播公告不计入未读） */
    Map<String, Object> unread(Long userId);
}
