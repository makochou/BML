package com.bml.module.api.controller;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.Result;
import com.bml.module.api.entity.ApiApp;
import com.bml.module.api.mapper.ApiAppMapper;
import com.bml.module.api.service.ApiSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API应用管理控制器
 *
 * @author BML Team
 */
@Tag(name = "API应用管理")
@RestController
@RequestMapping("/api/app")
public class ApiAppController extends BaseController {

    @Resource
    private ApiAppMapper apiAppMapper; // 简单CRUD直接用Mapper，复杂用Service

    @Resource
    private ApiSyncService apiSyncService;

    @Operation(summary = "同步所有API接口")
    @PostMapping("/sync")
    public Result<String> sync() {
        return Result.ok(apiSyncService.syncAll());
    }

    @Operation(summary = "获取应用列表")
    @GetMapping("/list")
    public Result<List<ApiApp>> list(ApiApp query) {
        return Result.ok(apiAppMapper.selectList(new LambdaQueryWrapper<ApiApp>()
                .like(query.getName() != null, ApiApp::getName, query.getName())
                .eq(query.getStatus() != null, ApiApp::getStatus, query.getStatus())
                .orderByDesc(ApiApp::getCreateTime)));
    }

    @Operation(summary = "新增应用(自动颁发Key)")
    @PostMapping
    public Result<Void> add(@RequestBody ApiApp app) {
        app.setAppId(IdUtil.simpleUUID()); // 自动生成AppKey
        app.setAppSecret(RandomUtil.randomString(32)); // 自动生成Secret
        app.setStatus(1);
        apiAppMapper.insert(app);
        return Result.ok();
    }

    @Operation(summary = "重置密钥")
    @PutMapping("/{id}/reset")
    public Result<String> resetSecret(@PathVariable Long id) {
        ApiApp app = apiAppMapper.selectById(id);
        if (app == null)
            return Result.fail("应用不存在");

        String newSecret = RandomUtil.randomString(32);
        app.setAppSecret(newSecret);
        apiAppMapper.updateById(app);
        return Result.ok(newSecret);
    }

    @Operation(summary = "删除应用")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        apiAppMapper.deleteById(id);
        return Result.ok();
    }
}
