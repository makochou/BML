package com.bml.core.framework.runtime;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * BML 后端运行目录工具。
 * <p>
 * 统一解决多模块工程在不同启动目录下的运行期文件落盘问题：
 * <ol>
 *     <li>从 {@code bml-backend} 根目录启动时，运行目录解析为 {@code bml-backend/bml-app}；</li>
 *     <li>从 {@code bml-app} 目录启动时，运行目录保持为当前 {@code bml-app}；</li>
 *     <li>生产环境通过 Jar 或外部目录启动时，默认使用当前工作目录，避免强依赖源码结构；</li>
 *     <li>如需定制，可通过 JVM 参数 {@code -Dbml.runtime.dir=/path/to/bml-app} 显式指定。</li>
 * </ol>
 * </p>
 *
 * @author BML Team
 */
public final class BmlRuntimePaths {

    /** 运行目录系统属性名称，供 Java 代码与 Logback 配置共同使用。 */
    public static final String RUNTIME_DIR_PROPERTY = "bml.runtime.dir";

    /** 应用启动模块目录名。 */
    private static final String APP_MODULE_NAME = "bml-app";

    private BmlRuntimePaths() {
    }

    /**
     * 初始化运行目录系统属性。
     * <p>
     * 该方法应尽量在 Spring Boot 启动前调用，确保 Logback 初始化时即可读取
     * {@code bml.runtime.dir}，从而把 logs、License 等运行期文件统一落到 bml-app 下。
     * 如果外部已经通过 JVM 参数显式指定，则不会覆盖用户配置。
     * </p>
     *
     * @return 规范化后的运行目录绝对路径
     */
    public static Path initRuntimeDirProperty() {
        Path runtimeDir = resolveRuntimeDir();
        System.setProperty(RUNTIME_DIR_PROPERTY, runtimeDir.toString());
        return runtimeDir;
    }

    /**
     * 获取后端运行目录。
     *
     * @return 规范化后的运行目录绝对路径
     */
    public static Path resolveRuntimeDir() {
        String configuredDir = System.getProperty(RUNTIME_DIR_PROPERTY);
        if (configuredDir != null && !configuredDir.isBlank()) {
            return normalize(Path.of(configuredDir));
        }

        Path userDir = normalize(Path.of(System.getProperty("user.dir", ".")));
        if (APP_MODULE_NAME.equals(userDir.getFileName() == null ? null : userDir.getFileName().toString())) {
            return userDir;
        }

        Path appModuleDir = userDir.resolve(APP_MODULE_NAME);
        if (Files.isDirectory(appModuleDir)) {
            return normalize(appModuleDir);
        }

        return userDir;
    }

    /**
     * 按运行目录解析业务相对路径。
     * <p>
     * 绝对路径原样规范化返回；相对路径统一拼接到运行目录下，避免因 IDE、Maven、Jar
     * 启动目录不同导致文件散落在 bml-backend 根目录。
     * </p>
     *
     * @param path 配置中的路径，可为绝对路径或相对路径
     * @return 规范化后的绝对路径
     */
    public static Path resolveRuntimePath(String path) {
        if (path == null || path.isBlank()) {
            return resolveRuntimeDir();
        }
        Path targetPath = Path.of(path);
        if (targetPath.isAbsolute()) {
            return normalize(targetPath);
        }
        return normalize(resolveRuntimeDir().resolve(targetPath));
    }

    /**
     * 规范化路径并转为绝对路径。
     *
     * @param path 原始路径
     * @return 规范化后的绝对路径
     */
    private static Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }
}
