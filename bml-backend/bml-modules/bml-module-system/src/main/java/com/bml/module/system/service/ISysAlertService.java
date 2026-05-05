package com.bml.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bml.module.system.entity.SysAlert;

import java.time.LocalDate;
import java.util.List;

/**
 * 系统告警通知 Service 接口
 * <p>
 * 提供告警的查询、标记已读、增量轮询等核心能力。
 * 前端通知中心和右下角 Toast 弹窗均依赖本接口。
 * </p>
 *
 * @author BML Team
 */
public interface ISysAlertService extends IService<SysAlert> {

    /**
     * 获取用户未读的告警数量
     *
     * @return 未读数量
     */
    long getUnreadCount();

    /**
     * 获取最近的告警列表 (限制返回最近 N 条数据以防列表过大)
     *
     * @param limit 返回条数
     * @return 告警列表 (按创建时间倒序)
     */
    List<SysAlert> getRecentAlerts(int limit);

    /**
     * 获取指定 ID 之后的最新告警 (前端增量轮询核心接口)
     * <p>
     * 前端定期携带上次已知的最大告警 ID 进行轮询，
     * 服务端仅返回该 ID 之后的增量数据，避免全量查询。
     * </p>
     *
     * @param lastId 上次已知的最大告警 ID，为 null 时返回最近 20 条
     * @return 增量告警列表 (按 ID 升序)
     */
    List<SysAlert> getLatestAlerts(Long lastId);

    /**
     * 将指定的告警标记为已读
     *
     * @param id 告警 ID
     * @return 是否成功
     */
    boolean markAsRead(Long id);

    /**
     * 将所有告警一键标记为已读
     *
     * @return 成功更新的记录数
     */
    int markAllAsRead();

    /**
     * 删除指定的告警记录 (逻辑删除)
     *
     * @param id 告警 ID
     * @return 是否成功
     */
    boolean deleteAlert(Long id);

    /**
     * 获取存在告警记录的日期列表 (前端日期选择器用)
     * <p>
     * 返回所有有告警的日期(去重、倒序)，前端据此高亮日历上的日期。
     * </p>
     *
     * @return 日期列表 (yyyy-MM-dd 格式，倒序排列)
     */
    List<String> getAlertDates();

    /**
     * 按日期查询告警列表
     * <p>
     * 返回指定日期当天的所有告警，按创建时间倒序排列。
     * </p>
     *
     * @param date 查询日期 (yyyy-MM-dd)
     * @return 当日告警列表
     */
    List<SysAlert> getAlertsByDate(LocalDate date);

    /**
     * 智能保存或更新告警（带未读去重逻辑）
     * <p>
     * 核心去重策略：
     * <ul>
     * <li>1. 查询是否存在相同类型和标题的<strong>未读</strong>告警</li>
     * <li>2. 如果存在，则更新该告警的内容、级别和时间（保持未读状态，防止快速重复点击产生多条）</li>
     * <li>3. 如果不存在未读告警，直接创建新告警</li>
     * </ul>
     * 每次许可证变更都是独立的业务事件，即使内容完全相同也应创建新记录。
     * </p>
     *
     * @param alert 待保存的告警对象
     * @return 保存或更新后的告警对象
     */
    SysAlert saveOrUpdateAlert(SysAlert alert);

    /**
     * 按告警类型批量标记为已读
     * <p>
     * 将所有指定类型的未读告警一键标记为已读，
     * 适用于用户希望快速清理某一类告警的场景。
     * </p>
     *
     * @param alertType 告警类型（如：LICENSE_CHANGE、SYSTEM_ERROR 等）
     * @return 实际更新的记录数
     */
    int markAsReadByType(String alertType);

    /**
     * 按告警类型批量删除
     * <p>
     * 将所有指定类型的告警进行逻辑删除，
     * 适用于用户希望彻底清理某一类告警的场景。
     * </p>
     *
     * @param alertType 告警类型（如：LICENSE_CHANGE、SYSTEM_ERROR 等）
     * @return 实际删除的记录数
     */
    int deleteByType(String alertType);

    /**
     * 按告警标题批量标记为已读
     * <p>
     * 将所有指定标题的未读告警一键标记为已读，
     * 适用于用户希望快速清理相同标题告警的场景。
     * </p>
     *
     * @param alertTitle 告警标题（如："API 账号上限升级"）
     * @return 实际更新的记录数
     */
    int markAsReadByTitle(String alertTitle);

    /**
     * 按告警标题批量删除
     * <p>
     * 将所有指定标题的告警进行逻辑删除，
     * 适用于用户希望彻底清理相同标题告警的场景。
     * </p>
     *
     * @param alertTitle 告警标题（如："API 账号上限升级"）
     * @return 实际删除的记录数
     */
    int deleteByTitle(String alertTitle);
}
