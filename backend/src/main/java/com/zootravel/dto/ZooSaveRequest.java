package com.zootravel.dto;

import java.math.BigDecimal;

/**
 * 动物园新增/修改请求体
 */
public record ZooSaveRequest(
        String name,
        String city,
        String province,
        String address,
        BigDecimal longitude,
        BigDecimal latitude,
        BigDecimal ticketAdult,
        BigDecimal ticketChild,
        String ticketNote,
        String openTime,
        String openHoursNote,
        String website,
        String description,
        String starAnimals) {
}
