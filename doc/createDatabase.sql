-- 健康外卖系统初始化脚本（MySQL 8.0+）
-- 注意：本脚本会删除 healthy 数据库中当前项目的所有业务表，仅用于首次初始化或可清空数据的开发环境。

CREATE DATABASE IF NOT EXISTS `healthy`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `healthy`;

SET NAMES utf8mb4;

-- 按关联关系逆序删除表。当前项目使用逻辑外键，未创建数据库级 FOREIGN KEY 约束。
DROP TABLE IF EXISTS `diet_recommendation`;
DROP TABLE IF EXISTS `health_analysis`;
DROP TABLE IF EXISTS `nutrition_record`;
DROP TABLE IF EXISTS `diet_record`;
DROP TABLE IF EXISTS `order_detail`;
DROP TABLE IF EXISTS `orders`;
DROP TABLE IF EXISTS `shopping_cart`;
DROP TABLE IF EXISTS `user_address`;
DROP TABLE IF EXISTS `setmeal_dish`;
DROP TABLE IF EXISTS `setmeal`;
DROP TABLE IF EXISTS `dish`;
DROP TABLE IF EXISTS `category_nutrition`;
DROP TABLE IF EXISTS `dish_category`;
DROP TABLE IF EXISTS `merchant`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `password_hash` varchar(255) NOT NULL COMMENT '密码哈希，不保存明文',
  `name` varchar(64) NOT NULL COMMENT '用户姓名',
  `gender` varchar(16) DEFAULT NULL COMMENT 'MALE、FEMALE、UNKNOWN',
  `phone` varchar(32) NOT NULL COMMENT '手机号，同时作为登录账号',
  `avatar_path` varchar(255) DEFAULT NULL COMMENT '头像路径',
  `role` varchar(16) NOT NULL COMMENT 'USER 或 MERCHANT',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1正常，0禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_phone` (`phone`),
  KEY `idx_user_role` (`role`),
  CONSTRAINT `ck_user_role` CHECK (`role` IN ('USER', 'MERCHANT')),
  CONSTRAINT `ck_user_status` CHECK (`status` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户与商家登录账号';

CREATE TABLE `merchant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '商家管理员账号ID，逻辑关联user.id',
  `name` varchar(128) NOT NULL COMMENT '商家名称',
  `avatar_path` varchar(255) DEFAULT NULL COMMENT '商家头像路径',
  `description` varchar(500) DEFAULT NULL COMMENT '商家介绍',
  `address` varchar(255) NOT NULL COMMENT '商家地址',
  `phone` varchar(32) NOT NULL COMMENT '联系电话',
  `business_hours` varchar(128) DEFAULT NULL COMMENT '营业时间，例如09:00-21:00',
  `business_status` tinyint NOT NULL DEFAULT 1 COMMENT '1营业，0停业',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_user_id` (`user_id`),
  KEY `idx_merchant_status` (`business_status`),
  CONSTRAINT `ck_merchant_status` CHECK (`business_status` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家表';

CREATE TABLE `dish_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) NOT NULL COMMENT '平台统一菜品分类名称',
  `initial_sort` int NOT NULL DEFAULT 0 COMMENT '初始排序，数值越小越靠前',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dish_category_name` (`name`),
  KEY `idx_dish_category_sort` (`initial_sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台统一菜品分类表';

CREATE TABLE `category_nutrition` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_id` bigint NOT NULL COMMENT '菜品分类ID，逻辑关联dish_category.id',
  `nutrition_tag` varchar(64) NOT NULL COMMENT '定性营养标签，例如高蛋白、高脂肪',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_nutrition` (`category_id`, `nutrition_tag`),
  KEY `idx_category_nutrition_tag` (`nutrition_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类营养标签表';

CREATE TABLE `dish` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_id` bigint NOT NULL COMMENT '商家ID，逻辑关联merchant.id',
  `category_id` bigint NOT NULL COMMENT '菜品分类ID，逻辑关联dish_category.id',
  `name` varchar(128) NOT NULL COMMENT '菜品名称',
  `price` decimal(10,2) NOT NULL COMMENT '菜品价格',
  `description` varchar(1000) DEFAULT NULL COMMENT '菜品描述',
  `image_path` varchar(255) DEFAULT NULL COMMENT '图片路径',
  `sales_count` int NOT NULL DEFAULT 0 COMMENT '销售量',
  `status` varchar(16) NOT NULL DEFAULT 'OFF_SALE' COMMENT 'ON_SALE、OFF_SALE、SOLD_OUT、DELETED',
  `nutrition_detail` json DEFAULT NULL COMMENT '可选具体营养成分JSON',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dish_merchant_name` (`merchant_id`, `name`),
  KEY `idx_dish_merchant_status` (`merchant_id`, `status`),
  KEY `idx_dish_category` (`category_id`),
  CONSTRAINT `ck_dish_price` CHECK (`price` >= 0),
  CONSTRAINT `ck_dish_sales_count` CHECK (`sales_count` >= 0),
  CONSTRAINT `ck_dish_status` CHECK (`status` IN ('ON_SALE', 'OFF_SALE', 'SOLD_OUT', 'DELETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品表';

CREATE TABLE `setmeal` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_id` bigint NOT NULL COMMENT '商家ID，逻辑关联merchant.id',
  `name` varchar(128) NOT NULL COMMENT '套餐名称',
  `price` decimal(10,2) NOT NULL COMMENT '套餐价格',
  `description` varchar(1000) DEFAULT NULL COMMENT '套餐描述',
  `image_path` varchar(255) DEFAULT NULL COMMENT '套餐图片路径',
  `status` varchar(16) NOT NULL DEFAULT 'OFF_SALE' COMMENT 'ON_SALE、OFF_SALE、SOLD_OUT、DELETED',
  `sales_count` int NOT NULL DEFAULT 0 COMMENT '销售量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_setmeal_merchant_name` (`merchant_id`, `name`),
  KEY `idx_setmeal_merchant_status` (`merchant_id`, `status`),
  CONSTRAINT `ck_setmeal_price` CHECK (`price` >= 0),
  CONSTRAINT `ck_setmeal_sales_count` CHECK (`sales_count` >= 0),
  CONSTRAINT `ck_setmeal_status` CHECK (`status` IN ('ON_SALE', 'OFF_SALE', 'SOLD_OUT', 'DELETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套餐表';

CREATE TABLE `setmeal_dish` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `setmeal_id` bigint NOT NULL COMMENT '套餐ID，逻辑关联setmeal.id',
  `dish_id` bigint NOT NULL COMMENT '菜品ID，逻辑关联dish.id',
  `copies` decimal(10,2) NOT NULL COMMENT '菜品份数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_setmeal_dish` (`setmeal_id`, `dish_id`),
  KEY `idx_setmeal_dish_dish` (`dish_id`),
  CONSTRAINT `ck_setmeal_dish_copies` CHECK (`copies` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套餐菜品组成表';

CREATE TABLE `user_address` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID，逻辑关联user.id',
  `address` varchar(255) NOT NULL COMMENT '送餐地址',
  `contact_phone` varchar(32) NOT NULL COMMENT '联系电话',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '1可用，0停用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_address_user_enabled` (`user_id`, `enabled`),
  CONSTRAINT `ck_user_address_enabled` CHECK (`enabled` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户送餐地址表';

CREATE TABLE `shopping_cart` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID，逻辑关联user.id',
  `merchant_id` bigint NOT NULL COMMENT '商家ID，逻辑关联merchant.id',
  `product_type` varchar(16) NOT NULL COMMENT 'DISH或SETMEAL',
  `product_id` bigint NOT NULL COMMENT '商品ID，按product_type逻辑关联dish或setmeal',
  `quantity` int NOT NULL COMMENT '商品数量',
  `selected` tinyint NOT NULL DEFAULT 1 COMMENT '1已勾选，0未勾选',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cart_item` (`user_id`, `merchant_id`, `product_type`, `product_id`),
  KEY `idx_cart_user_merchant` (`user_id`, `merchant_id`),
  CONSTRAINT `ck_cart_product_type` CHECK (`product_type` IN ('DISH', 'SETMEAL')),
  CONSTRAINT `ck_cart_quantity` CHECK (`quantity` > 0),
  CONSTRAINT `ck_cart_selected` CHECK (`selected` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表';

CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '用户ID，逻辑关联user.id',
  `merchant_id` bigint NOT NULL COMMENT '商家ID，逻辑关联merchant.id',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING_PAYMENT' COMMENT '订单状态',
  `order_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `payment_time` datetime DEFAULT NULL COMMENT '模拟支付时间',
  `total_amount` decimal(10,2) NOT NULL COMMENT '商品合计金额',
  `paid_amount` decimal(10,2) NOT NULL COMMENT '实付金额，本期等于商品合计',
  `delivery_address_snapshot` varchar(255) NOT NULL COMMENT '下单时送餐地址快照',
  `recipient_phone_snapshot` varchar(32) NOT NULL COMMENT '下单时联系电话快照',
  `reminder_count` int NOT NULL DEFAULT 0 COMMENT '催单次数',
  `last_reminder_time` datetime DEFAULT NULL COMMENT '最后催单时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_orders_order_no` (`order_no`),
  KEY `idx_orders_user_time` (`user_id`, `order_time`),
  KEY `idx_orders_merchant_status_time` (`merchant_id`, `status`, `order_time`),
  CONSTRAINT `ck_orders_status` CHECK (`status` IN ('PENDING_PAYMENT', 'PENDING_ACCEPT', 'PREPARING', 'DELIVERING', 'COMPLETED', 'CANCELLED')),
  CONSTRAINT `ck_orders_total_amount` CHECK (`total_amount` >= 0),
  CONSTRAINT `ck_orders_paid_amount` CHECK (`paid_amount` >= 0),
  CONSTRAINT `ck_orders_reminder_count` CHECK (`reminder_count` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

CREATE TABLE `order_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '订单ID，逻辑关联orders.id',
  `product_id` bigint NOT NULL COMMENT '原商品ID',
  `product_type` varchar(16) NOT NULL COMMENT 'DISH或SETMEAL',
  `product_name_snapshot` varchar(128) NOT NULL COMMENT '下单时商品名称快照',
  `product_price_snapshot` decimal(10,2) NOT NULL COMMENT '下单时商品单价快照',
  `quantity` int NOT NULL COMMENT '商品数量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_detail_order` (`order_id`),
  CONSTRAINT `ck_order_detail_product_type` CHECK (`product_type` IN ('DISH', 'SETMEAL')),
  CONSTRAINT `ck_order_detail_price` CHECK (`product_price_snapshot` >= 0),
  CONSTRAINT `ck_order_detail_quantity` CHECK (`quantity` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单商品明细快照表';

CREATE TABLE `diet_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID，逻辑关联user.id',
  `dish_id` bigint DEFAULT NULL COMMENT '平台菜品ID，逻辑关联dish.id',
  `food_name` varchar(128) DEFAULT NULL COMMENT '手工填写食品名称',
  `food_nutrition_tags` json DEFAULT NULL COMMENT '手工填写食品营养标签JSON数组',
  `meal_time` datetime NOT NULL COMMENT '用餐时间',
  `quantity` decimal(10,2) NOT NULL COMMENT '用餐数量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_diet_record_user_meal_time` (`user_id`, `meal_time`),
  KEY `idx_diet_record_dish` (`dish_id`),
  CONSTRAINT `ck_diet_record_quantity` CHECK (`quantity` > 0),
  CONSTRAINT `ck_diet_record_source` CHECK (
    (`dish_id` IS NOT NULL AND `food_name` IS NULL) OR
    (`dish_id` IS NULL AND `food_name` IS NOT NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户饮食记录表';

CREATE TABLE `nutrition_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID，逻辑关联user.id',
  `category_id` bigint DEFAULT NULL COMMENT '菜品分类ID，手工食品可为空',
  `nutrition_tag` varchar(64) NOT NULL COMMENT '营养标签',
  `record_date` date NOT NULL COMMENT '记录日期',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_nutrition_record_user_date` (`user_id`, `record_date`),
  KEY `idx_nutrition_record_user_date_tag` (`user_id`, `record_date`, `nutrition_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户按日营养标签记录表';

CREATE TABLE `health_analysis` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID，逻辑关联user.id',
  `analysis_date` date NOT NULL COMMENT '分析覆盖日期',
  `input_revision` bigint NOT NULL COMMENT '本次分析使用的用户当日营养输入版本',
  `health_score` decimal(5,2) NOT NULL COMMENT '健康评分，范围0至100',
  `risk_summary` varchar(1000) DEFAULT NULL COMMENT '风险摘要',
  `optimization_suggestion` text DEFAULT NULL COMMENT '优化建议',
  `analysis_model` varchar(128) NOT NULL COMMENT 'Agent或规则版本标识',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_health_analysis_user_date` (`user_id`, `analysis_date`),
  KEY `idx_health_analysis_user_date_revision` (`user_id`, `analysis_date`, `input_revision`),
  CONSTRAINT `ck_health_analysis_score` CHECK (`health_score` >= 0 AND `health_score` <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='饮食健康分析结果表';

CREATE TABLE `diet_recommendation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID，逻辑关联user.id',
  `health_analysis_id` bigint NOT NULL COMMENT '健康分析ID，逻辑关联health_analysis.id',
  `recommended_category_id` bigint NOT NULL COMMENT '推荐菜品分类ID，逻辑关联dish_category.id',
  `recommendation_score` decimal(5,2) NOT NULL COMMENT '推荐分数',
  `reason` varchar(1000) DEFAULT NULL COMMENT '推荐原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_diet_recommendation_analysis_score` (`health_analysis_id`, `recommendation_score`),
  KEY `idx_diet_recommendation_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='饮食推荐分类表';
