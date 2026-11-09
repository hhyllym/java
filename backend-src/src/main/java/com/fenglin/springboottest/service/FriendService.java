package com.fenglin.springboottest.service;

import com.fenglin.springboottest.common.Result;

/**
 * 好友功能：搜索用户、发申请、处理申请、好友列表、删除好友、备注、申请管理、资料卡、推荐。
 * 前端 src/friend.js 调 /api/friend/* 这些接口。
 */
public interface FriendService {

    /** 按小马名字或 ID 搜索用户（排除自己），并标注与我的关系 */
    Result<?> search(Long meId, String keyword);

    /** 发送好友申请 */
    Result<?> apply(Long fromId, Long toId, String msg);

    /** 我收到的好友申请列表 */
    Result<?> applies(Long meId);

    /** 同意 / 拒绝申请 */
    Result<?> handle(Long applyId, Long meId, boolean accept);

    /** 我的好友列表 */
    Result<?> list(Long meId);

    /** 删除好友（双向解除） */
    Result<?> delete(Long meId, Long friendId);

    /** 设置好友备注（remark 为空表示清除备注） */
    Result<?> setRemark(Long meId, Long friendId, String remark);

    /** 我发出的申请：待处理 / 对方未通过，供撤回与查看进度 */
    Result<?> sent(Long meId);

    /** 撤回自己发出的、尚未处理的申请 */
    Result<?> cancel(Long applyId, Long meId);

    /** 好友资料卡：基本信息、认识天数、互动徽章、共同好友、游戏战绩对比 */
    Result<?> profile(Long meId, Long friendId);

    /** 可能认识的人：按共同好友推荐，不足则用新伙伴补齐 */
    Result<?> suggest(Long meId);
}
