package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.HallMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HallMessageMapper extends BaseMapper<HallMessage> {
}
