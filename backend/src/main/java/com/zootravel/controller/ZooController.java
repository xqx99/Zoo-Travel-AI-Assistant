package com.zootravel.controller;

import com.zootravel.dto.ApiResponse;
import com.zootravel.dto.PageResult;
import com.zootravel.dto.ZooDetailDto;
import com.zootravel.dto.ZooSaveRequest;
import com.zootravel.entity.Zoo;
import com.zootravel.service.ZooService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 动物园数据接口
 */
@RestController
@RequestMapping("/api/zoo")
public class ZooController {

    private final ZooService zooService;

    public ZooController(ZooService zooService) {
        this.zooService = zooService;
    }

    @GetMapping("/list")
    public ApiResponse<PageResult<Zoo>> list(@RequestParam(required = false) String city,
                                             @RequestParam(required = false) String animal,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(zooService.list(city, animal, page, size));
    }

    @GetMapping("/cities")
    public ApiResponse<List<String>> cities() {
        return ApiResponse.ok(zooService.listCities());
    }

    @GetMapping("/{id}")
    public ApiResponse<ZooDetailDto> detail(@PathVariable Long id) {
        return zooService.detail(id)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("动物园不存在: " + id));
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody ZooSaveRequest req) {
        return ApiResponse.ok(zooService.save(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody ZooSaveRequest req) {
        if (zooService.update(id, req)) {
            return ApiResponse.ok();
        }
        return ApiResponse.fail("动物园不存在: " + id);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        if (zooService.delete(id)) {
            return ApiResponse.ok();
        }
        return ApiResponse.fail("动物园不存在: " + id);
    }
}
