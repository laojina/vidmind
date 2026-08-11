package com.vidmind.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vidmind.server.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    //MyBatis-Plus自动搞定增删改查
}