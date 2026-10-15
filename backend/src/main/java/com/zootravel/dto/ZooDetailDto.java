package com.zootravel.dto;

import java.util.List;

/**
 * 动物园详情（含关联动物列表）
 */
public record ZooDetailDto(
        Long id,
        String name,
        String city,
        String province,
        String address,
        String ticketAdult,
        String ticketChild,
        String ticketNote,
        String openTime,
        String openHoursNote,
        String website,
        String description,
        String starAnimals,
        List<AnimalItem> animals) {

    public record AnimalItem(String animalName, String category, String exhibitArea,
                             String showTime, boolean star, String note, String funFacts) {
    }
}
