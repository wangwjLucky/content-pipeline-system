package com.pipeline.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pipeline.admin.entity.Topic;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TopicMapper extends BaseMapper<Topic> {
}