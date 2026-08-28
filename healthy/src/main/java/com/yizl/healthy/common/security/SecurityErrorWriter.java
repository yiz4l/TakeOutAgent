package com.yizl.healthy.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yizl.healthy.common.api.ApiResponse;
import com.yizl.healthy.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.failure(errorCode, errorCode.getMessage())
        );
    }
}
