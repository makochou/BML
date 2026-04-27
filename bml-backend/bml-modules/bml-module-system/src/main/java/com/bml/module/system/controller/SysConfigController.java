package com.bml.module.system.controller;

import com.bml.core.common.result.Result;
import com.bml.module.system.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * 系统配置控制器
 * <p>
 * 提供系统运行时配置的增删改查能力，以及登录页品牌图片的上传与获取。
 * 配置项以 key-value 形式存储在 sys_config 表中，支持中台管理平台动态修改。
 * </p>
 *
 * <h3>品牌图片（Branding）存储策略：</h3>
 * <ul>
 *   <li>图片保存在服务器本地目录 {@code ${bml.branding.storage-dir:data/branding/}}</li>
 *   <li>图片 URL 通过 sys_config 表记录，前端通过 {@code /auth/login/config} 获取</li>
 *   <li>图片获取接口 {@code /system/config/branding/{filename}} 无需认证</li>
 * </ul>
 *
 * @author BML Team
 */
@Tag(name = "系统配置管理", description = "系统参数配置、品牌图片管理")
@RestController
@RequestMapping("/system/config")
public class SysConfigController {

    private static final Logger log = LoggerFactory.getLogger(SysConfigController.class);

    /** 品牌图片存储根目录 */
    @Value("${bml.branding.storage-dir:data/branding}")
    private String brandingStorageDir;

    @Resource
    private SysConfigService configService;

    // ────────────────────────────────────────────
    // 配置项 CRUD
    // ────────────────────────────────────────────

    /**
     * 查询登录页及品牌相关配置（验证码开关、背景图、侧边栏 Logo 等）
     * <p>
     * 同时返回 sys.login.* 和 sys.sidebar.* 前缀的全部配置项，
     * 供前端登录页、业务系统布局等统一读取。
     * </p>
     */
    @Operation(summary = "查询登录配置", description = "查询 sys.login.* 和 sys.sidebar.* 前缀的所有配置项")
    @GetMapping("/login")
    public Result<Map<String, String>> getLoginConfig() {
        Map<String, String> configs = new java.util.HashMap<>(configService.getConfigsByPrefix("sys.login."));
        configs.putAll(configService.getConfigsByPrefix("sys.sidebar."));
        return Result.ok(configs);
    }

    /**
     * 批量更新配置项
     *
     * @param configs key-value 键值对
     */
    @Operation(summary = "批量更新配置", description = "更新或新增多个配置项")
    @PutMapping("/batch")
    public Result<Void> batchUpdate(@RequestBody Map<String, String> configs) {
        configs.forEach((key, value) -> configService.upsertConfig(key, value, key));
        return Result.ok();
    }

    // ────────────────────────────────────────────
    // 品牌图片管理
    // ────────────────────────────────────────────

    /**
     * 上传品牌图片（登录页背景、侧边栏 Logo、favicon）
     * <p>
     * 上传成功后将图片访问路径写入 sys_config 对应的配置项中。
     * </p>
     *
     * @param type 图片类型：loginBg（登录页背景）、sidebarLogo（侧边栏 Logo）、favicon（浏览器标签图标）
     * @param file 图片文件
     * @return 图片访问 URL
     */
    @Operation(summary = "上传品牌图片", description = "上传登录页背景/侧边栏 Logo/favicon")
    @PostMapping("/branding/upload")
    public Result<Map<String, String>> uploadBrandingImage(
            @RequestParam("type") String type,
            @RequestParam("file") MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return Result.badRequest("请选择图片文件");
        }

        // 校验图片类型
        String configKey = switch (type) {
            case "loginBg" -> "sys.login.bgImage";
            case "sidebarLogo" -> "sys.sidebar.logo";
            case "favicon" -> "sys.login.favicon";
            default -> null;
        };
        if (configKey == null) {
            return Result.badRequest("不支持的图片类型：" + type);
        }

        // 校验文件后缀
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.')).toLowerCase();
        }
        if (!ext.matches("\\.(png|jpg|jpeg|gif|svg|ico|webp)")) {
            return Result.badRequest("仅支持 PNG/JPG/GIF/SVG/ICO/WebP 格式");
        }

        // 保存文件
        String filename = type + ext;
        try {
            Path dir = Paths.get(brandingStorageDir);
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("[Branding] 品牌图片已保存: type={}, path={}", type, target);
        } catch (IOException e) {
            log.error("[Branding] 保存品牌图片失败", e);
            return Result.error("保存失败：" + e.getMessage());
        }

        // 更新配置项
        // 在 URL 末尾追加时间戳参数，确保每次上传后浏览器都能获取到最新图片，
        // 彻底避免浏览器缓存导致的"上传成功但显示旧图"问题。
        String imageUrl = "/api/system/config/branding/" + filename + "?t=" + System.currentTimeMillis();
        configService.upsertConfig(configKey, imageUrl, "登录页品牌图片-" + type);

        return Result.ok(Map.of("url", imageUrl));
    }

    /**
     * 获取品牌图片（无需认证）
     * <p>
     * 用于前端直接通过 URL 引用品牌图片，无需 Bearer Token。
     * </p>
     *
     * @param filename 文件名
     * @return 图片二进制流
     */
    @Operation(summary = "获取品牌图片", description = "通过文件名获取上传的品牌图片")
    @GetMapping("/branding/{filename}")
    public void getBrandingImage(
            @PathVariable String filename,
            jakarta.servlet.http.HttpServletResponse response) {

        // 安全校验：防止路径穿越
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            response.setStatus(400);
            return;
        }

        Path filePath = Paths.get(brandingStorageDir).resolve(filename);
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            response.setStatus(404);
            return;
        }

        // 推断 Content-Type
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        String contentType = switch (ext) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "svg" -> "image/svg+xml";
            case "ico" -> "image/x-icon";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };

        response.setContentType(contentType);
        // 禁用缓存：品牌图片可能随时被替换，必须每次都从服务器获取最新版本。
        // 使用 no-cache 而非 no-store，允许浏览器缓存但每次都需要向服务器验证是否有更新。
        // 配合 URL 时间戳参数，可彻底解决"上传新图但显示旧图"的问题。
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        try {
            Files.copy(filePath, response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("[Branding] 读取品牌图片失败: {}", filename, e);
            response.setStatus(500);
        }
    }

    /**
     * 删除品牌图片（恢复默认）
     *
     * @param type 图片类型
     */
    @Operation(summary = "删除品牌图片", description = "删除已上传的品牌图片，恢复系统默认")
    @DeleteMapping("/branding/{type}")
    public Result<Void> deleteBrandingImage(@PathVariable String type) {
        String configKey = switch (type) {
            case "loginBg" -> "sys.login.bgImage";
            case "sidebarLogo" -> "sys.sidebar.logo";
            case "favicon" -> "sys.login.favicon";
            default -> null;
        };
        if (configKey == null) {
            return Result.badRequest("不支持的图片类型：" + type);
        }

        // 清空配置值（恢复前端默认图片）
        configService.upsertConfig(configKey, "", "登录页品牌图片-" + type);
        return Result.ok();
    }
}
