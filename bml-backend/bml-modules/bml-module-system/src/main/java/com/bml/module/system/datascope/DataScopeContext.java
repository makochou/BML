package com.bml.module.system.datascope;

/**
 * 数据权限上下文。
 * <p>
 * 使用 ThreadLocal 在 AOP 与 Service 查询逻辑之间传递 SQL 片段，
 * 每次请求结束后必须清理，避免线程复用导致串数据。
 * </p>
 *
 * @author BML Team
 */
public final class DataScopeContext {

    private static final ThreadLocal<String> DATA_SCOPE_SQL = new ThreadLocal<>();

    private DataScopeContext() {
    }

    public static void setDataScopeSql(String sql) {
        DATA_SCOPE_SQL.set(sql);
    }

    public static String getDataScopeSql() {
        return DATA_SCOPE_SQL.get();
    }

    public static void clear() {
        DATA_SCOPE_SQL.remove();
    }
}
