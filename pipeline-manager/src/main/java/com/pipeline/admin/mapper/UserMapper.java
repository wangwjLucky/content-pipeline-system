package com.pipeline.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pipeline.admin.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<SysUser> {
}