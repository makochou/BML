package com.bml.module.system.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.PageResult;
import com.bml.core.common.result.Result;
import com.bml.module.system.dto.SysLoginLogQuery;
import com.bml.module.system.service.SysLoginLogService;
import com.bml.module.system.util.AuditCsvExportUtils;
import com.bml.module.system.vo.SysLoginLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 系统登录日志控制器。
 * <p>
 * 提供登录日志分页查询、详情查看、删除、清空和安全导出能力。登录日志用于追踪登录成功、登录失败、登出和修改密码等认证安全事件。
 * </p>
 *
 * @author BML Team
 */
@Tag(name = "系统登录日志", description = "用户认证安全事件审计")
@RestController
@RequestMapping("/system/loginlog")
@RequiredArgsConstructor
public class SysLoginLogController extends BaseController {

    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final SysLoginLogService loginLogService;

    /**
     * 分页查询登录日志。
     *
     * @param query 查询条件
     * @return 分页日志数据
     */
    @Operation(summary = "分页查询系统登录日志")
    @PreAuthorize("@ss.hasPermi('system:loginlog:list')")
    @GetMapping("/page")
    public Result<PageResult<SysLoginLogVO>> page(SysLoginLogQuery query) {
        return Result.ok(loginLogService.selectLoginLogPage(query));
    }

    /**
     * 查询登录日志详情。
     *
     * @param id 日志ID
     * @return 登录日志详情
     */
    @Operation(summary = "查询系统登录日志详情")
    @PreAuthorize("@ss.hasPermi('system:loginlog:query')")
    @GetMapping("/{id}")
    public Result<SysLoginLogVO> getInfo(@PathVariable Long id) {
        return Result.ok(loginLogService.selectLoginLogById(id));
    }

    /**
     * 导出登录日志 CSV。
     *
     * @param query    查询条件
     * @param response HTTP响应
     * @throws IOException 写出失败时由全局异常处理器统一处理
     */
    @Operation(summary = "导出系统登录日志")
    @PreAuthorize("@ss.hasPermi('system:loginlog:export')")
    @GetMapping("/export")
    public void export(SysLoginLogQuery query, HttpServletResponse response) throws IOException {
        SysLoginLogQuery exportQuery = query == null ? new SysLoginLogQuery() : query;
        exportQuery.setPageNum(1);
        exportQuery.setPageSize(10000);
        List<SysLoginLogVO> records = loginLogService.selectLoginLogPage(exportQuery).getRecords();
        List<List<?>> rows = new ArrayList<>();
        for (SysLoginLogVO item : records) {
            rows.add(Arrays.asList(
                        item.getId(),
                        item.getUsername(),
                        item.getIpaddr(),
                        item.getLoginLocation(),
                        item.getBrowser(),
                        item.getOs(),
                        item.getStatus() != null && item.getStatus() == 1 ? "成功" : "失败",
                        item.getMsg(),
                        item.getLoginTime() == null ? "" : item.getLoginTime()));
        }
        AuditCsvExportUtils.writeCsv(response,
                "login-log-" + LocalDateTime.now().format(EXPORT_TIME_FORMATTER) + ".csv",
                List.of("日志ID", "账号", "IP地址", "登录地点", "浏览器", "操作系统", "状态", "消息", "时间"),
                rows);
    }

    /**
     * 批量删除登录日志。
     *
     * @param ids 日志ID集合
     * @return 操作结果
     */
    @Operation(summary = "批量删除系统登录日志")
    @PreAuthorize("@ss.hasPermi('system:loginlog:remove')")
    @DeleteMapping
    public Result<Void> remove(@RequestBody List<Long> ids) {
        return toAjax(loginLogService.deleteLoginLogs(ids));
    }

    /**
     * 清空登录日志。
     *
     * @return 操作结果
     */
    @Operation(summary = "清空系统登录日志")
    @PreAuthorize("@ss.hasPermi('system:loginlog:clean')")
    @DeleteMapping("/clean")
    public Result<Void> clean() {
        return toAjax(loginLogService.cleanLoginLogs() >= 0);
    }
}
