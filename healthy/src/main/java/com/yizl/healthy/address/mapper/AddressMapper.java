package com.yizl.healthy.address.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yizl.healthy.address.entity.UserAddressEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AddressMapper extends BaseMapper<UserAddressEntity> {

    default List<UserAddressEntity> selectByUserId(Long userId) {
        return selectList(
                Wrappers.<UserAddressEntity>lambdaQuery()
                        .eq(UserAddressEntity::getUserId, userId)
                        .orderByDesc(UserAddressEntity::getId)
        );
    }

    default UserAddressEntity selectOwnedById(Long userId, Long addressId) {
        return selectOne(
                Wrappers.<UserAddressEntity>lambdaQuery()
                        .eq(UserAddressEntity::getId, addressId)
                        .eq(UserAddressEntity::getUserId, userId)
        );
    }

    default int deleteOwnedById(Long userId, Long addressId) {
        return delete(
                Wrappers.<UserAddressEntity>lambdaQuery()
                        .eq(UserAddressEntity::getId, addressId)
                        .eq(UserAddressEntity::getUserId, userId)
        );
    }
}
