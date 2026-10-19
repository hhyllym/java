package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.GameScore;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.GameScoreMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 游戏成绩：提交、排行榜、好友对战对比（互动小游戏的基础）。
 */
@RestController
@RequestMapping("/api/game")
public class GameScoreController {

    @Autowired
    private GameScoreMapper gameScoreMapper;

    @Autowired
    private UserMapper userMapper;

    /** 提交一局成绩 */
    @PostMapping("/score")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> score(@RequestBody Map<String, Object> body) {
        String gameId = body.get("gameId") == null ? "" : String.valueOf(body.get("gameId")).trim();
        Long userId = toLong(body.get("userId"));
        Integer score = body.get("score") == null ? null : ((Number) body.get("score")).intValue();
        if (userId == null || gameId.isEmpty() || score == null) {
            return Result.fail("参数不完整");
        }
        GameScore g = new GameScore();
        g.setGameId(gameId);
        g.setUserId(userId);
        g.setScore(score);
        g.setCreateTime(LocalDateTime.now());
        gameScoreMapper.insert(g);
        return Result.ok("成绩已记录");
    }

    /** 某游戏排行榜 top 20 */
    @GetMapping("/rank")
    public Result<?> rank(@RequestParam String gameId) {
        List<GameScore> rows = gameScoreMapper.selectList(new LambdaQueryWrapper<GameScore>()
                .eq(GameScore::getGameId, gameId)
                .orderByDesc(GameScore::getScore)
                .last("limit 20"));
        List<Map<String, Object>> out = new ArrayList<>();
        int i = 1;
        for (GameScore g : rows) {
            User u = userMapper.selectById(g.getUserId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rank", i++);
            m.put("userId", g.getUserId());
            m.put("name", u == null ? "已退场的小马" : u.getName());
            m.put("avatar", u == null || u.getAvatar() == null ? "" : u.getAvatar());
            m.put("score", g.getScore());
            m.put("time", String.valueOf(g.getCreateTime()));
            out.add(m);
        }
        return Result.ok(out);
    }

    /** 我和好友在某游戏上的最高分对比 */
    @GetMapping("/vs")
    public Result<?> vs(@RequestParam Long meId, @RequestParam Long friendId, @RequestParam String gameId) {
        Map<String, Object> mine = bestOf(meId, gameId);
        Map<String, Object> theirs = bestOf(friendId, gameId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("me", mine);
        m.put("friend", theirs);
        int ms = ((Number) mine.get("best")).intValue();
        int fs = ((Number) theirs.get("best")).intValue();
        if (ms > fs) {
            m.put("winner", "me");
        } else if (fs > ms) {
            m.put("winner", "friend");
        } else {
            m.put("winner", ms > 0 ? "tie" : "none");
        }
        return Result.ok(m);
    }

    private Map<String, Object> bestOf(Long userId, String gameId) {
        GameScore g = gameScoreMapper.selectOne(new LambdaQueryWrapper<GameScore>()
                .eq(GameScore::getGameId, gameId)
                .eq(GameScore::getUserId, userId)
                .orderByDesc(GameScore::getScore)
                .last("limit 1"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", userId);
        if (g == null) {
            m.put("best", 0);
            m.put("plays", 0);
        } else {
            m.put("best", g.getScore());
            m.put("plays", gameScoreMapper.selectCount(new LambdaQueryWrapper<GameScore>()
                    .eq(GameScore::getGameId, gameId)
                    .eq(GameScore::getUserId, userId)));
        }
        return m;
    }

    private Long toLong(Object o) {
        try {
            return o == null ? null : Long.valueOf(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }
}
