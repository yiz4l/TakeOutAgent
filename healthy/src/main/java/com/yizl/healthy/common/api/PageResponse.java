package com.yizl.healthy.common.api;

import java.util.List;

public record PageResponse<T>(long total, int page, int size, List<T> records) {

    public PageResponse {
        records = List.copyOf(records);
    }
}
