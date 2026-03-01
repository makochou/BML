package com.bml.module.api.support;

import com.bml.module.api.service.SysOpenApiRegistryService;
import com.bml.module.api.vo.OpenApiRegistrySyncResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动期开放接口目录自动同步器。
 * <p>
 * 未来企业业务接口会持续新增，如果仍然依赖手工点击“同步接口目录”，
 * 很容易出现代码已上线但 BML 中台授权目录仍停留旧状态的问题。
 * 因此默认在应用启动后自动做一次轻量同步，确保管理端看到的目录与当前运行时一致。
 * </p>
 * <p>
 * 这里选择“记录错误但不中断启动”：
 * 即使同步失败，管理端仍可通过页面按钮手工重试，不应让整个应用因为目录同步问题不可用。
 * </p>
 */
@Slf4j
@Component
public class OpenApiRegistryStartupSynchronizer implements ApplicationRunner {

    private final SysOpenApiRegistryService openApiRegistryService;

    @Value("${bml.openapi.registry.auto-sync-on-startup:true}")
    private boolean autoSyncOnStartup;

    public OpenApiRegistryStartupSynchronizer(SysOpenApiRegistryService openApiRegistryService) {
        this.openApiRegistryService = openApiRegistryService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!autoSyncOnStartup) {
            log.info("OpenAPI目录启动同步已关闭，跳过自动同步。");
            return;
        }
        try {
            OpenApiRegistrySyncResultVO result = openApiRegistryService.syncRegistry();
            log.info("OpenAPI目录启动同步完成：发现{}个接口，新增{}个，更新{}个，停用{}个，跳过{}个。",
                    result.getTotalDiscovered(),
                    result.getInsertedCount(),
                    result.getUpdatedCount(),
                    result.getDisabledCount(),
                    result.getSkippedCount());
        } catch (Exception ex) {
            log.error("OpenAPI目录启动同步失败，可在BML中台手工点击“同步接口目录”重试。", ex);
        }
    }
}
