package com.yizl.healthy.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yizl.healthy.merchant.entity.SetMealDishEntity;
import com.yizl.healthy.merchant.mapper.row.SetMealDishRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SetMealDishMapper extends BaseMapper<SetMealDishEntity> {

    List<SetMealDishRow> selectDetailsBySetMealId(@Param("setMealId") Long setMealId);
}
