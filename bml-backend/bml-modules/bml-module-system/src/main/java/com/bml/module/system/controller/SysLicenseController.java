package com.bml.module.system.controller;

import com.bml.core.common.enums.GlobalErrorCode;
import com.bml.core.common.result.Result;
import com.bml.core.framework.license.BmlLicense;
import com.bml.core.framework.license.BmlLicenseHolder;
import com.bml.core.framework.license.LicenseQuotaEnforcer;
import com.bml.module.system.entity.SysAlert;
import com.bml.module.system.service.ISysAlertService;
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

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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
@Tag(name = "授权管理", description = "许可证上传与状态查询")
@RestController
@RequestMapping("/system/license")
public class SysLicenseController {

    private final BmlLicenseHolder licenseHolder;

    /**
     * 使用 DataSource 原生 JDBC 查询当前配额使用量。
     * 由于 sys_api_account 实体属于 bml-module-api 模块，
     * 此处通过原生 JDBC 直接计数以避免跨模块编译依赖。
     * DataSource 由 Spring Boot 数据源自动配置提供，无需额外引入依赖。
     */
    private final DataSource dataSource;

    /**
     * 许可证配额强制执行器。
     * 许可证更新/上传后自动冻结超额用户，确保系统始终符合授权约束。
     */
    private final LicenseQuotaEnforcer licenseQuotaEnforcer;

    /**
     * 系统告警服务。
     * 许可证配额降级时自动写入告警记录，由前端告警中心统一展示。
     */
    private final ISysAlertService sysAlertService;

    public SysLicenseController(BmlLicenseHolder licenseHolder, DataSource dataSource,
                                LicenseQuotaEnforcer licenseQuotaEnforcer,
                                ISysAlertService sysAlertService) {
        this.licenseHolder = licenseHolder;
        this.dataSource = dataSource;
        this.licenseQuotaEnforcer = licenseQuotaEnforcer;
        this.sysAlertService = sysAlertService;
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

        // 填充当前配额使用量，供前端配额进度展示
        populateCurrentUsage(vo);

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

        // 配额降级时自动冻结超额资源（用户 + API 账号）
        LicenseQuotaEnforcer.EnforceResult enforceResult =
                licenseQuotaEnforcer.enforceAll(licenseHolder.getCurrentLicense());

        // 配额降级记录写入告警中心（由前端轮询自动弹窗展示）
        createQuotaDowngradeAlerts(enforceResult);

        LicenseStatusVO vo = LicenseStatusVO.from(
                licenseHolder.getCurrentLicense(),
                licenseHolder.isEnabled(),
                licenseHolder.getLastError());
        vo.setFilePath(licenseHolder.resolveLicensePath().toAbsolutePath().toString());
        applyEnforceResult(vo, enforceResult);
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

        // 6. 配额降级时自动冻结超额资源（用户 + API 账号）
        LicenseQuotaEnforcer.EnforceResult enforceResult =
                licenseQuotaEnforcer.enforceAll(licenseHolder.getCurrentLicense());
        if (enforceResult.hasEnforcement()) {
            log.info("[License] 许可证更新后配额降级执行报告：冻结用户={}, 冻结API账号={}",
                    enforceResult.getFrozenUserCount(), enforceResult.getFrozenApiAccountCount());
        }

        // 配额降级记录写入告警中心（由前端轮询自动弹窗展示）
        createQuotaDowngradeAlerts(enforceResult);

        LicenseStatusVO vo = LicenseStatusVO.from(
                licenseHolder.getCurrentLicense(),
                licenseHolder.isEnabled(),
                licenseHolder.getLastError());
        vo.setFilePath(licenseHolder.resolveLicensePath().toAbsolutePath().toString());
        applyEnforceResult(vo, enforceResult);
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

    // ======================== 私有方法 ========================

    /**
     * 许可证配额降级后，将降级详情写入 sys_alert 告警表。
     * <p>
     * 写入的告警会被前端 30 秒轮询自动捕获，并通过 AlertToast 弹窗 + 告警中心统一展示。
     * 每种被降级的资源维度生成一条独立告警，便于运维逐条确认处理。
     * </p>
     *
     * @param result 配额强制执行结果
     */
    private void createQuotaDowngradeAlerts(LicenseQuotaEnforcer.EnforceResult result) {
        if (result == null || !result.hasEnforcement()) {
            return;
        }

        // 业务用户被冻结
        if (result.getFrozenUserCount() > 0) {
            SysAlert alert = new SysAlert();
            alert.setAlertType("LICENSE_QUOTA_DOWNGRADE");
            alert.setAlertLevel("warning");
            alert.setAlertTitle("业务用户上限降级");
            alert.setAlertContent(String.format(
                    "许可证配额调整：已自动停用 %d 个最近创建的业务用户，请前往用户管理页面确认被停用的资源。",
                    result.getFrozenUserCount()));
            alert.setReadStatus(0);
            sysAlertService.save(alert);
        }

        // API 账号被冻结
        if (result.getFrozenApiAccountCount() > 0) {
            SysAlert alert = new SysAlert();
            alert.setAlertType("LICENSE_QUOTA_DOWNGRADE");
            alert.setAlertLevel("warning");
            alert.setAlertTitle("API 账号上限降级");
            alert.setAlertContent(String.format(
                    "许可证配额调整：已自动停用 %d 个最近创建的 API 账号，请前往授权管理页面确认被停用的资源。",
                    result.getFrozenApiAccountCount()));
            alert.setReadStatus(0);
            sysAlertService.save(alert);
        }

        // API 来源用户被冻结
        if (result.getFrozenApiUserCount() > 0) {
            SysAlert alert = new SysAlert();
            alert.setAlertType("LICENSE_QUOTA_DOWNGRADE");
            alert.setAlertLevel("warning");
            alert.setAlertTitle("API 来源用户上限降级");
            alert.setAlertContent(String.format(
                    "许可证配额调整：已自动停用 %d 个最近由 API 创建的业务用户，请前往用户管理页面确认被停用的资源。",
                    result.getFrozenApiUserCount()));
            alert.setReadStatus(0);
            sysAlertService.save(alert);
        }

        log.info("[License] 配额降级告警已写入告警中心：冻结用户={}, 冻结API账号={}, 冻结API用户={}",
                result.getFrozenUserCount(), result.getFrozenApiAccountCount(), result.getFrozenApiUserCount());
    }

    /**
     * 将配额强制执行结果映射到 VO。
     *
     * @param vo 许可证状态 VO
     * @param result 配额强制执行结果
     */
    private void applyEnforceResult(LicenseStatusVO vo, LicenseQuotaEnforcer.EnforceResult result) {
        if (result == null) {
            return;
        }
        vo.setFrozenUserCount(result.getFrozenUserCount() > 0 ? result.getFrozenUserCount() : null);
        vo.setFrozenApiAccountCount(result.getFrozenApiAccountCount() > 0 ? result.getFrozenApiAccountCount() : null);
        vo.setFrozenApiUserCount(result.getFrozenApiUserCount() > 0 ? result.getFrozenApiUserCount() : null);
    }

    /**
     * 填充当前配额使用量。
     * <p>
     * 通过原生 JDBC 直接查询数据库计数，避免跨模块编译依赖。
     * 同时统计「总数」和「启用数」，供前端展示详细的配额使用分布。
     * 查询失败时不影响主流程，仅记录日志并保留使用量为 null。
     * </p>
     *
     * @param vo 许可证状态 VO
     */
    private void populateCurrentUsage(LicenseStatusVO vo) {
        // 注意：BaseEntity 使用 @TableLogic 逻辑删除（deleted=0 存在，deleted=1 已删除），
        // 此处必须加 WHERE deleted = 0 与 MyBatis-Plus 的 count() 保持一致。

        // 业务用户统计 (系统/管理员手工创建的，或所有用户)
        // 按照要求，“业务用户上限”通常指所有可登录的业务用户。如果也包含API创建的用户，就是查全表。
        // 原有逻辑是查全表，我们保持不变。
        vo.setCurrentTotalUsers(countTable("sys_user"));
        vo.setActiveTotalUsers(countTableByStatus("sys_user", 1));

        // API 账号自身维度统计
        vo.setCurrentApiAccounts(countTable("sys_api_account"));
        vo.setActiveApiAccounts(countTableByStatus("sys_api_account", 1));

        // API 账号全局累计创建的业务用户统计
        // 由于安全框架设计：API账号鉴权后构造的 OpenApiLoginUser 会将其 userId 设为负数的 accountId。
        // 因此 create_by < 0 的记录即为所有 API 账号创建的资源。
        vo.setCurrentApiUsers(countSysUserCreatedByApi());
        vo.setActiveApiUsers(countActiveSysUserCreatedByApi());
    }

    private long countSysUserCreatedByApi() {
        String sql = "SELECT COUNT(*) FROM sys_user WHERE deleted = 0 AND create_by < 0";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (Exception ex) {
            log.debug("[License] 查询 API 创建用户总量失败: {}", ex.getMessage());
            return 0L;
        }
    }

    private long countActiveSysUserCreatedByApi() {
        String sql = "SELECT COUNT(*) FROM sys_user WHERE deleted = 0 AND status = 1 AND create_by < 0";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (Exception ex) {
            log.debug("[License] 查询 API 创建的活跃用户量失败: {}", ex.getMessage());
            return 0L;
        }
    }

    /**
     * 通过原生 JDBC 统计指定表中未删除记录数。
     * 查询失败时返回 0（表可能尚未创建），不影响主流程。
     *
     * @param tableName 表名
     * @return 记录数
     */
    private long countTable(String tableName) {
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE deleted = 0";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (Exception ex) {
            log.debug("[License] 查询 {} 数量失败（表可能尚未创建）: {}", tableName, ex.getMessage());
            return 0L;
        }
    }

    /**
     * 通过原生 JDBC 统计指定表中指定状态的未删除记录数。
     *
     * @param tableName 表名
     * @param status 状态值（1=启用，0=停用）
     * @return 记录数
     */
    private long countTableByStatus(String tableName, int status) {
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE deleted = 0 AND status = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (Exception ex) {
            log.debug("[License] 查询 {} status={} 数量失败: {}", tableName, status, ex.getMessage());
            return 0L;
        }
    }
}
