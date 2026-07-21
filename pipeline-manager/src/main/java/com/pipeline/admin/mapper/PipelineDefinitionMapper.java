package com.pipeline.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pipeline.admin.entity.PipelineDefinition;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PipelineDefinitionMapper extends BaseMapper<PipelineDefinition> {
}