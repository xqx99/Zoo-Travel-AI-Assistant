package com.zootravel.entity;

/**
 * 动物实体
 */
public record Animal(
        Long id,
        String name,
        String scientificName,
        String category,
        String habitat,
        String diet,
        String description,
        String funFacts) {
}
