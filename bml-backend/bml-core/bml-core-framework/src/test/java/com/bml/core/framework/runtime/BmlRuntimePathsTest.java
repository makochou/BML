package com.bml.core.framework.runtime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BmlRuntimePathsTest {

    private final String originalRuntimeDir = System.getProperty(BmlRuntimePaths.RUNTIME_DIR_PROPERTY);
    private final String originalUserDir = System.getProperty("user.dir");

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        restoreProperty(BmlRuntimePaths.RUNTIME_DIR_PROPERTY, originalRuntimeDir);
        restoreProperty("user.dir", originalUserDir);
    }

    @Test
    void resolveRuntimeDirShouldUseConfiguredDirFirst() {
        Path configuredDir = tempDir.resolve("custom-runtime");
        System.setProperty(BmlRuntimePaths.RUNTIME_DIR_PROPERTY, configuredDir.toString());

        assertEquals(configuredDir.toAbsolutePath().normalize(), BmlRuntimePaths.resolveRuntimeDir());
    }

    @Test
    void resolveRuntimeDirShouldUseBmlAppWhenStartedFromBackendRoot() throws Exception {
        Path backendRoot = tempDir.resolve("bml-backend");
        Path appDir = backendRoot.resolve("bml-app");
        Files.createDirectories(appDir);
        System.setProperty("user.dir", backendRoot.toString());

        assertEquals(appDir.toAbsolutePath().normalize(), BmlRuntimePaths.resolveRuntimeDir());
    }

    @Test
    void resolveRuntimeDirShouldKeepCurrentDirWhenStartedFromBmlApp() throws Exception {
        Path appDir = tempDir.resolve("bml-app");
        Files.createDirectories(appDir);
        System.setProperty("user.dir", appDir.toString());

        assertEquals(appDir.toAbsolutePath().normalize(), BmlRuntimePaths.resolveRuntimeDir());
    }

    @Test
    void resolveRuntimePathShouldAppendRelativePathToRuntimeDir() throws Exception {
        Path backendRoot = tempDir.resolve("bml-backend");
        Path appDir = backendRoot.resolve("bml-app");
        Files.createDirectories(appDir);
        System.setProperty("user.dir", backendRoot.toString());

        assertEquals(appDir.resolve("License").toAbsolutePath().normalize(),
                BmlRuntimePaths.resolveRuntimePath("License"));
    }

    private void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
            return;
        }
        System.setProperty(key, value);
    }
}
