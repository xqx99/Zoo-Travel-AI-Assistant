package com.zootravel.entity;

/**
 * 动物园-动物关联（含展区/投喂表演时间/明星标记，联查动物名）
 */
public record ZooAnimal(
        Long id,
        Long zooId,
        Long animalId,
        String animalName,
        String category,
        String exhibitArea,
        String showTime,
        boolean star,
        String note,
        String funFacts) {
}
