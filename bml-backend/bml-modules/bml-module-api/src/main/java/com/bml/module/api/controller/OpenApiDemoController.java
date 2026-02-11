package com.bml.module.api.controller;

import com.bml.core.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开放平台演示接口
 * <p>
 * 用于验证签名认证机制
 * </p>
 *
 * @author BML Team
 */
@Tag(name = "OpenAPI演示")
@RestController
@RequestMapping("/open/api/demo")
public class OpenApiDemoController {

    @Operation(summary = "Hello测试(需签名)")
    @GetMapping("/hello")
    public Result<String> hello(@RequestParam String name) {
        return Result.ok("Hello, " + name + "! 您的签名认证通过了。");
    }
}
