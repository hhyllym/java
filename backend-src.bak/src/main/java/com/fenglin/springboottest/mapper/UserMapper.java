package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus 直接给好了单表增删改查，不用写 XML。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
