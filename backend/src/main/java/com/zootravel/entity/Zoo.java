package com.zootravel.entity;

import java.math.BigDecimal;

/**
 * 动物园实体
 */
public record Zoo(
        Long id,
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
