package com.bml.module.system.controller;

import com.bml.core.common.result.Result;
import com.bml.module.system.converter.AlertConverter;
import com.bml.module.system.service.ISysAlertService;
import com.bml.module.system.vo.SysAlertVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 服务器告警通知 API 控制器
 * <p>
 * 为前端通知中心提供完整的告警管理接口，包括：
 * <ul>
 * <li>未读数量查询 — 右上角铃铛 Badge</li>
 * <li>最近告警列表 — 通知面板全量展示</li>
 * <li>增量轮询接口 — 前端定时拉取新告警，驱动 Toast 弹窗</li>
 * <li>已读标记 — 单条 / 全部</li>
 * <li>删除告警 — 逻辑删除</li>
 * </ul>
 * </p>
 *
 * @author BML Team
 */
@Tag(name = "告警监测", description = "告警信息查询与治理")
@RestController
@RequestMapping("/system/alert")
@RequiredArgsConstructor
public class SysAlertController {

    private final ISysAlertService sysAlertService;

    /**
     * 获取未读告警总数
     * <p>
     * 用于前端右上角铃铛的未读 Badge 显示。
     * </p>
     */
    @Operation(summary = "获取未读的告警总数")
    @PreAuthorize("@ss.hasPermi('monitor:alert:list')")
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        return Result.ok(sysAlertService.getUnreadCount());
    }

    /**
     * 获取最近的系统监控告警列表
     * <p>
     * 用于通知面板的全量展示，默认返回最近 50 条。
     * </p>
     */
    @Operation(summary = "获取最近的系统监控告警列表")
    @PreAuthorize("@ss.hasPermi('monitor:alert:list')")
    @GetMapping("/list")
    public Result<List<SysAlertVO>> getRecentAlerts(
            @Parameter(description = "返回条数，默认50") @RequestParam(name = "limit", defaultValue = "50") Integer limit) {
        return Result.ok(AlertConverter.INSTANCE.toVOList(sysAlertService.getRecentAlerts(limit)));
    }

    /**
     * 增量轮询接口 (前端定时调用)
     * <p>
     * 前端每 30 秒调用一次，携带上次已知的最大告警 ID，
     * 服务端仅返回该 ID 之后的增量数据。
     * 首次调用 lastId 可不传，则返回最近 20 条。
     * </p>
     */
    @Operation(summary = "增量轮询 — 获取指定 ID 之后的最新告警")
    @PreAuthorize("@ss.hasPermi('monitor:alert:list')")
    @GetMapping("/latest")
    public Result<List<SysAlertVO>> getLatestAlerts(
            @Parameter(description = "上次已知的最大告警 ID，首次可不传") @RequestParam(name = "lastId", required = false) Long lastId) {
        return Result.ok(AlertConverter.INSTANCE.toVOList(sysAlertService.getLatestAlerts(lastId)));
    }

    /**
     * 将指定的告警置为已读
     */
    @Operation(summary = "将指定的告警置为已读")
    @PreAuthorize("@ss.hasPermi('monitor:alert:edit')")
    @PutMapping("/read/{id}")
    public Result<Boolean> markAsRead(@PathVariable("id") Long id) {
        return Result.ok(sysAlertService.markAsRead(id));
    }

    /**
     * 将所有未读告警一键置为已读
     */
    @Operation(summary = "将所有未读告警一键置为已读")
    @PreAuthorize("@ss.hasPermi('monitor:alert:edit')")
    @PutMapping("/read-all")
    public Result<Integer> markAllAsRead() {
        return Result.ok(sysAlertService.markAllAsRead());
    }

    /**
     * 获取存在告警的日期列表
     * <p>
     * 用于前端日期选择器展示哪些日期有告警记录。
     * </p>
     */
    @Operation(summary = "获取存在告警的日期列表")
    @PreAuthorize("@ss.hasPermi('monitor:alert:list')")
    @GetMapping("/dates")
    public Result<List<String>> getAlertDates() {
        return Result.ok(sysAlertService.getAlertDates());
    }

    /**
     * 按日期查询当天的告警列表
     * <p>
     * 前端选择某个日期后，查询该日期下的所有告警记录。
     * </p>
     */
    @Operation(summary = "按日期查询告警列表")
    @PreAuthorize("@ss.hasPermi('monitor:alert:list')")
    @GetMapping("/by-date")
    public Result<List<SysAlertVO>> getAlertsByDate(
            @Parameter(description = "查询日期 (yyyy-MM-dd)") @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(AlertConverter.INSTANCE.toVOList(sysAlertService.getAlertsByDate(date)));
    }

    /**
     * 删除指定的告警记录 (逻辑删除)
     */
    @Operation(summary = "删除指定的告警记录")
    @PreAuthorize("@ss.hasPermi('monitor:alert:remove')")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteAlert(@PathVariable("id") Long id) {
        return Result.ok(sysAlertService.deleteAlert(id));
    }

    /**
     * 按告警类型批量标记为已读
     * <p>
     * 将所有指定类型的未读告警一键标记为已读。
     * 适用于用户希望快速清理某一类告警的场景。
     * </p>
     */
    @Operation(summary = "按告警类型批量标记为已读")
    @PreAuthorize("@ss.hasPermi('monitor:alert:edit')")
    @PutMapping("/read-by-type")
    public Result<Integer> markAsReadByType(
            @Parameter(description = "告警类型（如：LICENSE_CHANGE、SYSTEM_ERROR 等）") @RequestParam("alertType") String alertType) {
        return Result.ok(sysAlertService.markAsReadByType(alertType));
    }

    /**
     * 按告警类型批量删除
     * <p>
     * 将所有指定类型的告警进行逻辑删除。
     * 适用于用户希望彻底清理某一类告警的场景。
     * </p>
     */
    @Operation(summary = "按告警类型批量删除")
    @PreAuthorize("@ss.hasPermi('monitor:alert:remove')")
    @DeleteMapping("/delete-by-type")
    public Result<Integer> deleteByType(
            @Parameter(description = "告警类型（如：LICENSE_CHANGE、SYSTEM_ERROR 等）") @RequestParam("alertType") String alertType) {
        return Result.ok(sysAlertService.deleteByType(alertType));
    }

    /**
     * 按告警标题批量标记为已读
     * <p>
     * 将所有指定标题的未读告警一键标记为已读。
     * 适用于用户希望快速清理相同标题告警的场景。
     * </p>
     */
    @Operation(summary = "按告警标题批量标记为已读")
    @PreAuthorize("@ss.hasPermi('monitor:alert:edit')")
    @PutMapping("/read-by-title")
    public Result<Integer> markAsReadByTitle(
            @Parameter(description = "告警标题（如：\"API 账号上限升级\"）") @RequestParam("alertTitle") String alertTitle) {
        return Result.ok(sysAlertService.markAsReadByTitle(alertTitle));
    }

    /**
     * 按告警标题批量删除
     * <p>
     * 将所有指定标题的告警进行逻辑删除。
     * 适用于用户希望彻底清理相同标题告警的场景。
     * </p>
     */
    @Operation(summary = "按告警标题批量删除")
    @PreAuthorize("@ss.hasPermi('monitor:alert:remove')")
    @DeleteMapping("/delete-by-title")
    public Result<Integer> deleteByTitle(
            @Parameter(description = "告警标题（如：\"API 账号上限升级\"）") @RequestParam("alertTitle") String alertTitle) {
        return Result.ok(sysAlertService.deleteByTitle(alertTitle));
    }
}
