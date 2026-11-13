package com.fenglin.springboottest.service;

import com.fenglin.springboottest.common.Result;

import java.util.List;
import java.util.Map;

/**
 * 星糖大厅服务：
 *   - 公共聊天：所有用户可见，消息落库（t_hall_message），可以翻历史
 *   - 游戏邀请：内存房间，谁都能发起邀请、谁来加入，实时可见
 *
 * 前端 src/hall.js 调的就是这些：
 *   GET  /api/hall/messages?limit=            大厅聊天记录（倒序取、正序返回）
 *   POST /api/hall/messages  { userId, content }
 *   GET  /api/hall/online                      当前在线的小马
 *   GET  /api/hall/rooms                       正在进行的游戏邀请
 *   POST /api/hall/rooms     { userId, game, title, maxMembers }
 *   POST /api/hall/rooms/join   { userId, roomId }
 *   POST /api/hall/rooms/leave  { userId, roomId }
 *   POST /api/hall/rooms/close  { userId, roomId }
 */
public interface HallService {

    /** 大厅聊天记录（最新的在最后） */
    Result<?> messages(int limit);

    /** 发送一条大厅消息 */
    Result<?> send(Long userId, String content);

    /** 在线的小马列表 */
    Result<?> online();

    /** 游戏邀请房间列表 */
    Result<?> rooms();

    /** 发起一个游戏邀请 */
    Result<?> createRoom(Long userId, String game, String title, Integer maxMembers);

    /** 加入邀请房间 */
    Result<?> joinRoom(Long userId, String roomId);

    /** 离开邀请房间 */
    Result<?> leaveRoom(Long userId, String roomId);

    /** 房主关闭自己的邀请 */
    Result<?> closeRoom(Long userId, String roomId);

    /** 供内部调用：往大厅发一条系统提示（例如有人发起了游戏邀请） */
    void systemNotice(String content);

    /** 供内部调用：某用户当前在哪些房间里 */
    List<Map<String, Object>> roomsOf(Long userId);
}
