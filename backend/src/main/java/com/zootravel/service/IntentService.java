package com.zootravel.service;

import com.zootravel.entity.Zoo;
import com.zootravel.repository.ZooRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 意图识别：从用户消息中提取目的地（城市/动物园名）、想看的动物、问题类型
 * 规则词表优先，简单确定，不消耗 token
 */
@Service
public class IntentService {

    private final ZooRepository zooRepository;

    public IntentService(ZooRepository zooRepository) {
        this.zooRepository = zooRepository;
    }

    /** 识别结果 */
    public record Intent(String destination, String animal, String type) {
    }

    public Intent analyze(String message) {
        String dest = null;
        List<Zoo> zoos = zooRepository.findAll();
        // 先匹配完整动物园名（更精确），再匹配城市
        for (Zoo z : zoos) {
            if (message.contains(z.name())) {
                dest = z.name();
                break;
            }
        }
        if (dest == null) {
            for (String c : zooRepository.listCities()) {
                if (message.contains(c)) {
                    dest = c;
                    break;
                }
            }
        }
        String animal = null;
        for (String n : zooRepository.listAnimalNames()) {
            if (message.contains(n)) {
                animal = n;
                break;
            }
        }
        return new Intent(dest, animal, classify(message));
    }

    private String classify(String m) {
        if (containsAny(m, "门票", "价格", "多少钱", "票价", "收费")) return "ticket";
        if (containsAny(m, "开放", "几点", "时间", "闭园", "开园", "营业")) return "hours";
        if (containsAny(m, "亲子", "小孩", "带娃", "孩子", "小朋友", "遛娃")) return "family";
        if (containsAny(m, "攻略", "路线", "怎么玩", "建议", "一日游", "怎么走", "安排")) return "guide";
        return "general";
    }

    private boolean containsAny(String text, String... keys) {
        for (String k : keys) {
            if (text.contains(k)) return true;
        }
        return false;
    }
}
