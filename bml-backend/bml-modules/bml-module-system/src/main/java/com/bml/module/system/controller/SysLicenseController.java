package com.bml.module.system.controller;

import com.bml.core.common.enums.GlobalErrorCode;
import com.bml.core.common.result.Result;
import com.bml.core.framework.license.BmlLicense;
import com.bml.core.framework.license.BmlLicenseHolder;
import com.bml.module.system.vo.LicenseStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 许可证管理控制器。
 * <p>
 * 提供许可证状态查询和文件上传接口。
 * 这两个接口被加入 Spring Security 白名单，在用户未登录时也可访问，
 * 确保系统未激活状态下前端能展示许可证上传页面。
 * </p>
 * <p>
 * 接口说明：
 * <ul>
 *     <li>{@code GET /system/license/status} — 查询许可证状态（无需登录）</li>
 *     <li>{@code POST /system/license/preview} — 预览许可证文件（无需登录，仅验证不保存）</li>
 *     <li>{@code POST /system/license/upload} — 上传许可证文件（无需登录，首次激活）</li>
 *     <li>{@code POST /system/license/update} — 更新许可证文件（无需登录，自动备份旧许可证）</li>
 *     <li>{@code DELETE /system/license/reset} — 删除许可证文件并重置（无需登录，仅用于开发测试）</li>
 * </ul>
 * </p>
 *
 * @author BML Team
 */
@Slf4j
@Tag(name = "许可证管理", description = "许可证上传与状态查询")
@RestController
@RequestMapping("/system/license")
public class SysLicenseController {

    private final BmlLicenseHolder licenseHolder;

    public SysLicenseController(BmlLicenseHolder licenseHolder) {
        this.licenseHolder = licenseHolder;
    }

    /**
     * 查询许可证状态。
     * <p>
     * 前端路由守卫和许可证管理页面均通过此接口判断系统激活状态。
     * 无需登录即可访问。
     * </p>
     *
     * @return 许可证状态信息
     */
    @Operation(summary = "查询许可证状态", description = "无需登录，返回当前系统许可证激活状态")
    @GetMapping("/status")
    public Result<LicenseStatusVO> status() {
        LicenseStatusVO vo = LicenseStatusVO.from(
                licenseHolder.getCurrentLicense(),
                licenseHolder.isEnabled(),
                licenseHolder.getLastError());
        vo.setFilePath(licenseHolder.resolveLicensePath().toAbsolutePath().toString());
        return Result.ok(vo);
    }

    /**
     * 预览许可证文件（仅验证解析，不替换当前许可证）。
     * <p>
     * 用于「更新许可证」前的对比预览，前端可展示新旧许可证差异后再确认上传。
     * 无需登录即可访问。
     * </p>
     *
     * @param file 许可证文件（.lic）
     * @return 新许可证的状态信息（不保存到磁盘）
     */
    @Operation(summary = "预览许可证文件", description = "验证并解析许可证文件但不保存，用于更新前预览对比")
    @PostMapping("/preview")
    public Result<LicenseStatusVO> preview(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "请选择许可证文件");
        }

        String fileContent;
        try {
            fileContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.error("[License] 读取上传文件失败", ex);
            return Result.fail(GlobalErrorCode.LICENSE_PARSE_ERROR);
        }

        // 仅验证和解析，不替换当前许可证
        BmlLicense license = licenseHolder.previewLicense(fileContent);
        return Result.ok(LicenseStatusVO.from(license, licenseHolder.isEnabled(), null));
    }

    /**
     * 上传许可证文件。
     * <p>
     * 接收 {@code .lic} 格式的许可证文件，校验签名后保存到本地磁盘，
     * 并触发 {@link BmlLicenseHolder} 重新加载。
     * </p>
     *
     * @param file 许可证文件（.lic）
     * @return 上传结果（包含许可证详情）
     */
    @Operation(summary = "上传许可证文件", description = "上传 .lic 许可证文件激活系统")
    @PostMapping("/upload")
    public Result<LicenseStatusVO> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "请选择许可证文件");
        }

        // 读取文件内容
        String fileContent;
        try {
            fileContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.error("[License] 读取上传文件失败", ex);
            return Result.fail(GlobalErrorCode.LICENSE_PARSE_ERROR);
        }

        // 验证许可证内容
        boolean valid = licenseHolder.loadFromContent(fileContent);
        if (!valid) {
            return Result.fail(GlobalErrorCode.LICENSE_INVALID.getCode(),
                    licenseHolder.getLastError() != null
                            ? licenseHolder.getLastError()
                            : GlobalErrorCode.LICENSE_INVALID.getMessage());
        }

        // 保存到磁盘
        try {
            Path licensePath = licenseHolder.resolveLicensePath();
            Files.createDirectories(licensePath.getParent());
            Path tempFile = Files.createTempFile(licensePath.getParent(), "bml-lic-", ".tmp");
            Files.writeString(tempFile, fileContent, StandardCharsets.UTF_8);
            Files.move(tempFile, licensePath, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            log.info("[License] 许可证文件已保存: {}", licensePath);
        } catch (IOException ex) {
            log.error("[License] 保存许可证文件失败", ex);
            return Result.fail(GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(), "许可证保存失败: " + ex.getMessage());
        }

        // 重新加载
        licenseHolder.reload();

        LicenseStatusVO vo = LicenseStatusVO.from(
                licenseHolder.getCurrentLicense(),
                licenseHolder.isEnabled(),
                licenseHolder.getLastError());
        vo.setFilePath(licenseHolder.resolveLicensePath().toAbsolutePath().toString());
        return Result.ok(vo);
    }

    /**
     * 更新许可证文件（升级授权）。
     * <p>
     * 适用于客户购买更多用户数、更多功能模块后，使用新许可证替换旧许可证的场景。
     * 与 {@code upload} 接口不同，此接口会：
     * <ol>
     *     <li>先验证新许可证文件的签名和内容合法性；</li>
     *     <li>自动备份当前旧许可证文件（文件名带时间戳后缀）；</li>
     *     <li>保存新许可证文件到磁盘并触发重新加载。</li>
     * </ol>
     * 客户端可先调用 {@code /preview} 接口获取新许可证详情做对比，确认后调用此接口完成更新。
     * </p>
     *
     * @param file 新许可证文件（.lic）
     * @return 更新结果（包含新许可证详情）
     */
    @Operation(summary = "更新许可证文件", description = "备份旧许可证后替换为新许可证，用于授权升级")
    @PostMapping("/update")
    public Result<LicenseStatusVO> update(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "请选择许可证文件");
        }

        // 1. 读取文件内容
        String fileContent;
        try {
            fileContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.error("[License] 读取上传文件失败", ex);
            return Result.fail(GlobalErrorCode.LICENSE_PARSE_ERROR);
        }

        // 2. 预验证新许可证（仅校验签名和格式，不替换当前内存中的许可证）
        try {
            licenseHolder.previewLicense(fileContent);
        } catch (Exception ex) {
            return Result.fail(GlobalErrorCode.LICENSE_INVALID.getCode(),
                    ex.getMessage() != null ? ex.getMessage() : GlobalErrorCode.LICENSE_INVALID.getMessage());
        }

        // 3. 备份旧许可证文件
        try {
            Path backupPath = licenseHolder.backupCurrentLicense();
            if (backupPath != null) {
                log.info("[License] 更新前旧许可证已备份: {}", backupPath.toAbsolutePath());
            }
        } catch (IOException ex) {
            log.error("[License] 备份旧许可证文件失败", ex);
            return Result.fail(GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                    "备份旧许可证失败: " + ex.getMessage());
        }

        // 4. 保存新许可证到磁盘
        try {
            Path licensePath = licenseHolder.resolveLicensePath();
            Files.createDirectories(licensePath.getParent());
            Path tempFile = Files.createTempFile(licensePath.getParent(), "bml-lic-", ".tmp");
            Files.writeString(tempFile, fileContent, StandardCharsets.UTF_8);
            Files.move(tempFile, licensePath, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            log.info("[License] 新许可证文件已保存: {}", licensePath);
        } catch (IOException ex) {
            log.error("[License] 保存新许可证文件失败", ex);
            return Result.fail(GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                    "许可证保存失败: " + ex.getMessage());
        }

        // 5. 重新加载
        licenseHolder.reload();

        LicenseStatusVO vo = LicenseStatusVO.from(
                licenseHolder.getCurrentLicense(),
                licenseHolder.isEnabled(),
                licenseHolder.getLastError());
        vo.setFilePath(licenseHolder.resolveLicensePath().toAbsolutePath().toString());
        return Result.ok(vo);
    }

    /**
     * 删除许可证文件并重置状态。
     * <p>
     * 仅用于开发测试环境，删除磁盘上的许可证文件并清空缓存，
     * 使系统回到未激活状态。
     * </p>
     *
     * @return 重置后的许可证状态
     */
    @Operation(summary = "删除许可证文件", description = "删除磁盘许可证文件并重置状态，仅用于开发测试")
    @DeleteMapping("/reset")
    public Result<LicenseStatusVO> reset() {
        Path licensePath = licenseHolder.resolveLicensePath();
        try {
            if (Files.deleteIfExists(licensePath)) {
                log.info("[License] 许可证文件已删除: {}", licensePath.toAbsolutePath());
            }
        } catch (IOException ex) {
            log.error("[License] 删除许可证文件失败", ex);
            return Result.fail(GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                    "删除失败: " + ex.getMessage());
        }

        // 重新加载（文件已删除，会清空缓存）
        licenseHolder.reload();

        LicenseStatusVO vo = LicenseStatusVO.from(
                licenseHolder.getCurrentLicense(),
                licenseHolder.isEnabled(),
                licenseHolder.getLastError());
        vo.setFilePath(licensePath.toAbsolutePath().toString());
        return Result.ok(vo);
    }
}
