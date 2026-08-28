package com.yizl.healthy.user.mapper.param;

/**
 * UserMapper 自定义 SQL 的查询参数，不直接复用 Controller 的请求 DTO。
 */
public record UserQueryCondition(
        String name,
        String phone,
        String gender,
        long offset,
        int size
) {
}
