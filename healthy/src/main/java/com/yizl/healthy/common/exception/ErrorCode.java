package com.yizl.healthy.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    SUCCESS(0, "success", HttpStatus.OK),
    BAD_REQUEST(40001, "请求参数错误", HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR(40001, "请求参数校验失败", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(40101, "未登录、JWT 无效或已过期", HttpStatus.UNAUTHORIZED),
    AUTH_ERROR(40301, "无权限访问该资源", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND(40401, "资源不存在", HttpStatus.NOT_FOUND),
    PHONE_ALREADY_EXISTS(40901, "手机号已被使用", HttpStatus.CONFLICT),
    DATA_CONFLICT(40900, "数据冲突", HttpStatus.CONFLICT),
    INTERNAL_ERROR(500, "服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
