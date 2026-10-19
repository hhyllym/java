package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.common.ShopDefs;
import com.fenglin.springboottest.entity.ChatMessage;
import com.fenglin.springboottest.entity.Friend;
import com.fenglin.springboottest.mapper.ChatMessageMapper;
import com.fenglin.springboottest.mapper.FriendMapper;
import com.fenglin.springboottest.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * 好友接口，前端 src/friend.js 调的就是这些：
 *   POST /api/friend/search  { meId, keyword }   按名字/ID 搜索用户
 *   POST /api/friend/apply   { fromId, toId, msg }  发申请
 *   POST /api/friend/applies { meId }            收到的申请
 *   POST /api/friend/handle  { applyId, meId, accept }  同意/拒绝
 *   POST /api/friend/list    { meId }            好友列表
 *   POST /api/friend/delete  { meId, friendId }  删除好友（软删，再加回保留数据）
 *   POST /api/friend/remark  { meId, friendId, remark }  设置备注（空串=清除）
 */
@RestController
@RequestMapping("/api/friend")
public class FriendController {

    @Autowired
    private FriendService friendService;

    @Autowired
    private FriendMapper friendMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    /** 互动标识：按聊天条数、好友天数、绑定关系算徽章 */
    @GetMapping("/badges")
    public Result<?> badges(@RequestParam Long meId, @RequestParam Long friendId) {
        Friend f = friendMapper.selectOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getStatus, 1)
                .and(w -> w.eq(Friend::getUserId, meId).eq(Friend::getFriendId, friendId)
                        .or().eq(Friend::getUserId, friendId).eq(Friend::getFriendId, meId))
                .last("limit 1"));
        if (f == null) {
            return Result.fail("你们还不是好友");
        }
        int msgCount = Math.toIntExact(chatMessageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                .and(w -> w.eq(ChatMessage::getFromId, meId).eq(ChatMessage::getToId, friendId)
                        .or().eq(ChatMessage::getFromId, friendId).eq(ChatMessage::getToId, meId))));
        int days = 0;
        if (f.getCreateTime() != null) {
            days = (int) ChronoUnit.DAYS.between(f.getCreateTime().toLocalDate(), java.time.LocalDate.now());
        }
        String relationType = f.getRelationType() == null ? "" : f.getRelationType();
        return Result.ok(ShopDefs.interactBadges(msgCount, days, relationType));
    }

    @PostMapping("/search")
    public Result<?> search(@RequestBody Map<String, Object> body) {
        return friendService.search(toLong(body.get("meId")), (String) body.get("keyword"));
    }

    @PostMapping("/apply")
    public Result<?> apply(@RequestBody Map<String, Object> body) {
        return friendService.apply(toLong(body.get("fromId")), toLong(body.get("toId")),
                (String) body.get("msg"));
    }

    @PostMapping("/applies")
    public Result<?> applies(@RequestBody Map<String, Object> body) {
        return friendService.applies(toLong(body.get("meId")));
    }

    @PostMapping("/handle")
    public Result<?> handle(@RequestBody Map<String, Object> body) {
        Object accept = body.get("accept");
        boolean ok = Boolean.TRUE.equals(accept) || "true".equals(String.valueOf(accept)) || Integer.valueOf(1).equals(accept);
        return friendService.handle(toLong(body.get("applyId")), toLong(body.get("meId")), ok);
    }

    @PostMapping("/list")
    public Result<?> list(@RequestBody Map<String, Object> body) {
        return friendService.list(toLong(body.get("meId")));
    }

    @PostMapping("/delete")
    public Result<?> delete(@RequestBody Map<String, Object> body) {
        return friendService.delete(toLong(body.get("meId")), toLong(body.get("friendId")));
    }

    @PostMapping("/remark")
    public Result<?> remark(@RequestBody Map<String, Object> body) {
        return friendService.setRemark(toLong(body.get("meId")), toLong(body.get("friendId")),
                (String) body.get("remark"));
    }

    private Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            return Long.valueOf(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }
}
