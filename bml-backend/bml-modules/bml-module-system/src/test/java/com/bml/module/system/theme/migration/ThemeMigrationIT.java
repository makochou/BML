package com.bml.module.system.theme.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MariaDBContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 主题相关 Flyway 迁移脚本集成测试。
 * <p>
 * 本测试通过 Testcontainers 启动一个干净的 MariaDB 容器，执行 {@code bml-app}
 * 子模块下的全部 Flyway 迁移脚本，并对 {@code V2.16.0__init_theme.sql}（任务 1.1）
 * 引入的下列契约进行断言：
 * </p>
 * <ol>
 *   <li>所有迁移脚本（含 {@code V2.16.0__init_theme.sql}）均成功执行；</li>
 *   <li>种子预设 {@code PRESET_BEST} 行存在且 {@code is_default = 1}、
 *       {@code is_built_in = 1}；</li>
 *   <li>表 {@code bml_theme_user_setting} 的唯一约束 {@code uk_user_scope}
 *       生效——同一 {@code (user_id, scope)} 重复插入抛出唯一约束冲突。</li>
 * </ol>
 *
 * <p>覆盖需求：</p>
 * <ul>
 *   <li>R7.5：迁移版本号大于现有最高版本号 V2.15.1（由 Flyway 引擎按版本顺序执行验证）；</li>
 *   <li>R7.6：{@code PRESET_BEST} 与至少 1 条备选预设种子数据存在；</li>
 *   <li>R13.3：迁移脚本失败应让启动整体终止（本测试反向校验"成功路径"作为基线，
 *       破坏性场景由 Flyway 引擎本身的失败语义保证）。</li>
 * </ul>
 *
 * <p>执行前提：宿主机已安装并启动 Docker。当 Docker 不可用时，本测试通过
 * {@link Assumptions#assumeTrue(boolean, String)} 主动跳过，不导致整体构建失败。</p>
 *
 * @author BML Team
 */
@DisplayName("主题迁移脚本集成测试 (V2.16.0__init_theme.sql)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ThemeMigrationIT {

    /**
     * 容器启动使用的 MariaDB 镜像版本。
     * <p>
     * 选择 {@code 11.4}（当前最新 LTS 主线）以贴近生产部署形态；该镜像兼容
     * 现有 Flyway 10.x + flyway-mysql 方言。
     * </p>
     */
    private static final String MARIADB_IMAGE = "mariadb:11.4";

    /**
     * 期望的内置默认预设 ID（与迁移脚本种子数据保持一致）。
     */
    private static final String PRESET_BEST_ID = "PRESET_BEST";

    /**
     * 迁移脚本所在的相对路径段。
     * <p>
     * 真实绝对路径在 {@link #setUp()} 中由 {@link #resolveMigrationDir()} 解析，
     * 兼容 IDE 直接运行（工作目录为模块目录）与 {@code mvn -pl} / {@code mvn -am}
     * 等不同启动场景。
     * </p>
     */
    private static final String MIGRATION_RELATIVE_PATH =
            "bml-app/src/main/resources/db/migration";

    private static MariaDBContainer<?> container;

    /**
     * 启动 MariaDB 容器；Docker 不可用时跳过整组测试。
     */
    @BeforeAll
    static void setUp() {
        Assumptions.assumeTrue(
                DockerClientFactory.instance().isDockerAvailable(),
                "Docker 不可用，跳过 ThemeMigrationIT 集成测试。"
        );

        container = new MariaDBContainer<>(MARIADB_IMAGE)
                .withDatabaseName("bml_theme_it")
                .withUsername("bml")
                .withPassword("bml")
                // utf8mb4 与生产保持一致，避免迁移脚本中 utf8mb4_unicode_ci 排序规则不可用
                .withConfigurationOverride("");
        container.start();
    }

    /**
     * 释放容器资源。
     */
    @AfterAll
    static void tearDown() {
        if (container != null && container.isRunning()) {
            container.stop();
        }
    }

    /**
     * 验证：从干净 MariaDB 实例执行全部迁移脚本均成功，且 {@code V2.16.0__init_theme.sql}
     * 被纳入执行序列。
     */
    @Test
    @Order(1)
    @DisplayName("Flyway 在干净 MariaDB 实例上成功执行全部迁移脚本（含 V2.16.0）")
    void shouldRunAllFlywayMigrationsSuccessfully() {
        Path migrationDir = resolveMigrationDir();

        Flyway flyway = Flyway.configure()
                .dataSource(container.getJdbcUrl(),
                        container.getUsername(),
                        container.getPassword())
                .locations("filesystem:" + migrationDir.toAbsolutePath())
                // 干净实例，无需基线
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .load();

        MigrateResult result = flyway.migrate();
        assertNotNull(result, "Flyway 迁移结果不应为 null");
        assertTrue(result.success, "Flyway 迁移应整体成功执行");
        assertTrue(result.migrationsExecuted > 0,
                "至少应执行 1 个迁移脚本，实际：" + result.migrationsExecuted);

        // V2.16.0__init_theme.sql 必须出现在 applied migrations 中
        boolean has2_16_0 = Arrays.stream(flyway.info().applied())
                .map(MigrationInfo::getVersion)
                .filter(v -> v != null)
                .anyMatch(v -> "2.16.0".equals(v.getVersion()));
        assertTrue(has2_16_0,
                "应当包含 V2.16.0__init_theme.sql 的执行记录");
    }

    /**
     * 验证：种子数据 {@code PRESET_BEST} 行存在，{@code is_default = 1}、
     * {@code is_built_in = 1}，且至少存在 1 条备选预设（满足 R7.6）。
     */
    @Test
    @Order(2)
    @DisplayName("PRESET_BEST 种子行存在且 is_default=1，且至少存在 1 条备选预设")
    void shouldSeedPresetBestRowAsDefault() throws SQLException {
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, is_built_in, is_default FROM bml_theme_preset WHERE id = ?")) {
            ps.setString(1, PRESET_BEST_ID);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "PRESET_BEST 行必须存在");
                assertEquals(PRESET_BEST_ID, rs.getString("id"));
                assertEquals(1, rs.getInt("is_built_in"),
                        "PRESET_BEST.is_built_in 应为 1（内置不可改）");
                assertEquals(1, rs.getInt("is_default"),
                        "PRESET_BEST.is_default 应为 1（系统默认预设）");
                assertFalse(rs.next(),
                        "PRESET_BEST 应当唯一，不应存在多条同 id 行");
            }
        }

        // 至少存在 1 条备选预设（is_built_in=1 且 is_default=0）
        try (Connection conn = openConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(*) AS c FROM bml_theme_preset WHERE is_built_in = 1 AND is_default = 0")) {
            assertTrue(rs.next());
            int alternativeCount = rs.getInt("c");
            assertTrue(alternativeCount >= 1,
                    "至少应存在 1 条内置备选预设（is_built_in=1 且 is_default=0），实际：" + alternativeCount);
        }

        // 全表只允许有一条 is_default=1 行
        try (Connection conn = openConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(*) AS c FROM bml_theme_preset WHERE is_default = 1")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt("c"),
                    "is_default=1 的预设全表必须有且仅有 1 条（即 PRESET_BEST）");
        }
    }

    /**
     * 验证：{@code bml_theme_user_setting} 的唯一约束 {@code uk_user_scope (user_id, scope)} 生效，
     * 即同一 {@code (user_id, scope)} 组合插入第二条时数据库拒绝写入。
     */
    @Test
    @Order(3)
    @DisplayName("uk_user_scope 唯一约束生效：相同 (user_id, scope) 重复插入应失败")
    void shouldEnforceUniqueUserScopeConstraint() throws SQLException {
        long userId = 1001L;
        String scope = "ADMIN";
        String profileJson = "{\"primaryColor\":\"#165DFF\"}";

        // 第一次插入应成功
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO bml_theme_user_setting (user_id, scope, preset_ref, profile) VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, userId);
            ps.setString(2, scope);
            ps.setString(3, PRESET_BEST_ID);
            ps.setString(4, profileJson);
            int affected = ps.executeUpdate();
            assertEquals(1, affected, "首次插入应写入 1 行");
        }

        // 第二次插入 (相同 user_id + 相同 scope) 应触发唯一约束冲突
        SQLException ex = assertThrows(SQLException.class, () -> {
            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO bml_theme_user_setting (user_id, scope, preset_ref, profile) VALUES (?, ?, ?, ?)")) {
                ps.setLong(1, userId);
                ps.setString(2, scope);
                ps.setString(3, PRESET_BEST_ID);
                ps.setString(4, profileJson);
                ps.executeUpdate();
            }
        }, "相同 (user_id, scope) 重复插入应抛出 SQLException");

        // 进一步校验：MariaDB 唯一键冲突 SQLState=23000，errorCode=1062
        assertTrue(
                ex instanceof SQLIntegrityConstraintViolationException
                        || "23000".equals(ex.getSQLState())
                        || ex.getErrorCode() == 1062,
                "异常应表征唯一约束冲突，实际：" + ex.getClass().getSimpleName()
                        + " sqlState=" + ex.getSQLState()
                        + " errorCode=" + ex.getErrorCode()
                        + " message=" + ex.getMessage());

        // 不同 scope 应允许写入（再次确认唯一约束作用范围正确）
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO bml_theme_user_setting (user_id, scope, preset_ref, profile) VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, userId);
            ps.setString(2, "BUSINESS");
            ps.setString(3, PRESET_BEST_ID);
            ps.setString(4, profileJson);
            int affected = ps.executeUpdate();
            assertEquals(1, affected,
                    "同 user_id 不同 scope 的组合应允许写入（唯一约束仅作用于 (user_id, scope)）");
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // 内部辅助方法
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 打开一个面向当前容器的数据库连接。
     * <p>调用方负责关闭。</p>
     */
    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
                container.getJdbcUrl(),
                container.getUsername(),
                container.getPassword()
        );
    }

    /**
     * 解析 {@code bml-app/src/main/resources/db/migration} 的绝对路径。
     * <p>
     * 从 {@code user.dir} 起向上查找，兼容下列启动方式：
     * </p>
     * <ul>
     *   <li>IDE 直接运行（工作目录通常为 {@code bml-modules/bml-module-system}）；</li>
     *   <li>{@code mvn -pl bml-modules/bml-module-system -am test}；</li>
     *   <li>{@code mvn test} 在仓库根目录执行。</li>
     * </ul>
     *
     * @return 迁移脚本目录的绝对路径
     * @throws IllegalStateException 当向上查找若干层后仍未命中时
     */
    private static Path resolveMigrationDir() {
        Path cursor = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        for (int i = 0; i < 6; i++) {
            Path candidate = cursor.resolve(MIGRATION_RELATIVE_PATH);
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            Path parent = cursor.getParent();
            if (parent == null) {
                break;
            }
            cursor = parent;
        }
        throw new IllegalStateException(
                "未能从 user.dir=" + System.getProperty("user.dir")
                        + " 向上找到迁移脚本目录 " + MIGRATION_RELATIVE_PATH);
    }
}
