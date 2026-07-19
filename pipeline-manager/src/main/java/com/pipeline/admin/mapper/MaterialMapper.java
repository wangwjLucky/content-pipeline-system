package com.pipeline.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pipeline.admin.entity.Material;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MaterialMapper extends BaseMapper<Material> {
}