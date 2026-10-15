package com.zootravel.service;

import com.zootravel.dto.PageResult;
import com.zootravel.dto.Quote;
import com.zootravel.dto.ZooDetailDto;
import com.zootravel.dto.ZooSaveRequest;
import com.zootravel.entity.Zoo;
import com.zootravel.entity.ZooAnimal;
import com.zootravel.repository.ZooRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 动物园业务：页面数据 + 聊天用的结构化检索
 */
@Service
public class ZooService {

    private final ZooRepository zooRepository;

    public ZooService(ZooRepository zooRepository) {
        this.zooRepository = zooRepository;
    }

    public PageResult<Zoo> list(String city, String animal, int page, int size) {
        int offset = Math.max(0, page - 1) * size;
        long total = zooRepository.countSearch(city, animal);
        List<Zoo> records = zooRepository.findPage(city, animal, offset, size);
        return new PageResult<>(total, records);
    }

    public List<Zoo> findAll() {
        return zooRepository.findAll();
    }

    public List<String> listCities() {
        return zooRepository.listCities();
    }

    public Optional<ZooDetailDto> detail(Long id) {
        return zooRepository.findById(id).map(zoo -> {
            List<ZooDetailDto.AnimalItem> items = zooRepository.findAnimalsByZooId(id).stream()
                    .map(a -> new ZooDetailDto.AnimalItem(
                            a.animalName(), a.category(), a.exhibitArea(), a.showTime(), a.star(), a.note(),
                            a.funFacts()))
                    .toList();
            return new ZooDetailDto(
                    zoo.id(), zoo.name(), zoo.city(), zoo.province(), zoo.address(),
                    fmt(zoo.ticketAdult()), fmt(zoo.ticketChild()), zoo.ticketNote(),
                    zoo.openTime(), zoo.openHoursNote(), zoo.website(), zoo.description(),
                    zoo.starAnimals(), items);
        });
    }

    public long save(ZooSaveRequest req) {
        return zooRepository.save(req);
    }

    public boolean update(Long id, ZooSaveRequest req) {
        return zooRepository.update(id, req) > 0;
    }

    public boolean delete(Long id) {
        return zooRepository.delete(id) > 0;
    }

    /**
     * 聊天结构化检索结果
     */
    public record StructuredResult(String text, List<Quote> quotes) {
    }

    /**
     * 根据意图检索 MySQL：目的地命中动物园名/城市，动物命中关联动物
     */
    public StructuredResult chatSearch(String destination, String animal) {
        List<Zoo> zoos = new ArrayList<>();
        if (destination != null) {
            zoos.addAll(zooRepository.searchForChat(destination));
        }
        if (animal != null) {
            for (Zoo z : zooRepository.searchForChat(animal)) {
                if (!zoos.contains(z)) {
                    zoos.add(z);
                }
            }
        }
        // 无任何命中时取前3个兜底，保证回答有据可依
        if (zoos.isEmpty()) {
            zoos.addAll(zooRepository.findAll().stream().limit(3).toList());
        }
        List<Zoo> top = zoos.stream().limit(3).toList();

        StringBuilder sb = new StringBuilder();
        List<Quote> quotes = new ArrayList<>();
        for (Zoo z : top) {
            sb.append("【动物园】").append(z.name()).append("（").append(z.city()).append("，")
                    .append(z.province()).append("）\n");
            sb.append("地址：").append(z.address()).append("；门票：成人")
                    .append(fmt(z.ticketAdult())).append("元/儿童").append(fmt(z.ticketChild()))
                    .append("元").append(z.ticketNote() == null ? "" : "（" + z.ticketNote() + "）")
                    .append("；开放时间：").append(z.openTime())
                    .append(z.openHoursNote() == null ? "" : "（" + z.openHoursNote() + "）")
                    .append("\n");
            sb.append("简介：").append(z.description()).append("\n");
            sb.append("明星动物：").append(z.starAnimals()).append("\n");
            List<ZooAnimal> animals = zooRepository.findAnimalsByZooId(z.id());
            for (ZooAnimal a : animals) {
                if (animal == null || a.animalName().contains(animal) || animal.contains(a.animalName())) {
                    sb.append("· ").append(a.animalName())
                            .append(a.exhibitArea() == null ? "" : "（展区：" + a.exhibitArea() + "）")
                            .append(a.showTime() == null ? "" : "，时间：" + a.showTime())
                            .append(a.star() ? "，明星动物" : "")
                            .append("\n");
                }
            }
            quotes.add(new Quote("zoo", z.id(), z.name(),
                    z.name() + "·" + z.city() + "·成人" + fmt(z.ticketAdult()) + "元·" + z.openTime()));
        }
        return new StructuredResult(sb.toString(), quotes);
    }

    private String fmt(BigDecimal v) {
        if (v == null) return "待定";
        return v.stripTrailingZeros().toPlainString();
    }
}
