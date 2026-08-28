package com.yizl.healthy.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yizl.healthy.user.entity.UserEntity;
import com.yizl.healthy.user.mapper.param.UserQueryCondition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

    long countByCondition(UserQueryCondition condition);

    List<UserEntity> selectPageByCondition(UserQueryCondition condition);

    long countByPhoneExcludingId(
            @Param("phone") String phone,
            @Param("excludedUserId") Long excludedUserId
    );
}
