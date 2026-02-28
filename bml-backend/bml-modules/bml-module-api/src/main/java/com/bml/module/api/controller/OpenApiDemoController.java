package com.bml.module.api.controller;

import com.bml.core.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OpenAPI 示例控制器。
 * <p>
 * 提供最小可用签名调用样例，便于联调 appKey + sign 链路。
 * </p>
 */
@Tag(name = "OpenAPI 示例接口")
@RestController
@RequestMapping("/open/api")
public class OpenApiDemoController {

    @Operation(summary = "连通性检测", description = "用于验证 OpenAPI 签名链路")
    @GetMapping("/ping")
    public Result<Map<String, Object>> ping() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "ok");
        payload.put("service", "bml-openapi");
        payload.put("time", LocalDateTime.now());
        return Result.ok(payload);
    }
}
