package com.yizl.healthy.cart.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("`shopping_cart`")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartEntity {
    @TableId(type = IdType.AUTO)
    Long id;

    @TableField("user_id")
    Long userId;

    @TableField("merchant_id")
    Long merchantId;

    @TableField("product_type")
    String productType;

    @TableField("product_id")
    Long productId;

    Integer quantity;

    Integer selected;

    @TableField("create_time")
    LocalDateTime createTime;

    @TableField("update_time")
    LocalDateTime updateTime;

}
