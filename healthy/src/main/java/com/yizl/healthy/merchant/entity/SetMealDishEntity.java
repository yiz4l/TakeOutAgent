package com.yizl.healthy.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("setmeal_dish")
public class SetMealDishEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("setmeal_id")
    private Long setMealId;

    @TableField("dish_id")
    private Long dishId;

    private BigDecimal copies;

    @TableField("create_time")
    private LocalDateTime createTime;
}
