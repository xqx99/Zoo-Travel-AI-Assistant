package com.zootravel.dto;

import java.util.List;

/**
 * 分页结果
 */
public record PageResult<T>(long total, List<T> records) {
}
