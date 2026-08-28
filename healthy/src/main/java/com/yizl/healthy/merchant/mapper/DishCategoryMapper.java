package com.yizl.healthy.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yizl.healthy.merchant.entity.DishCategoryEntity;
import com.yizl.healthy.merchant.mapper.row.DishCategoryRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DishCategoryMapper extends BaseMapper<DishCategoryEntity> {

    List<DishCategoryRow> selectAllWithNutritionTags();

    List<String> selectNutritionTags(@Param("categoryId") Long categoryId);
}
