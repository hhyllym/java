package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.PetCosmetic;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PetCosmeticMapper extends BaseMapper<PetCosmetic> {
}
