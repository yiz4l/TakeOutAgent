package com.yizl.healthy.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("`user`")
public class UserEntity {
	@TableId(type = IdType.AUTO)
	private Long id;

	@TableField("password_hash")
	private String passwordHash;

	private String name;

	private String gender;

	private String phone;

	@TableField("avatar_path")
	private String avatarPath;

	private String role;

	private int status;

	@TableField("create_time")
	private LocalDateTime createTime;

	@TableField("update_time")
	private LocalDateTime updateTime;
}
