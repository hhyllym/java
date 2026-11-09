package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息中心：留言板 / 私信 / 系统通知，全部落在 t_message 一张表上。
 * 简单查询用 MyBatis-Plus 的 LambdaQueryWrapper，不需要写 XML。
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
