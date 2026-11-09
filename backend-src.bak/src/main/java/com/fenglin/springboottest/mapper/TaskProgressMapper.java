package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.TaskProgress;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskProgressMapper extends BaseMapper<TaskProgress> {
}
