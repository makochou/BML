package com.bml.app.config;

import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.stereotype.Component;

/**
 * 自定义 Flyway 中文启动日志拦截器
 */
@Component
public class FlywayChineseLogInterceptor implements Callback {

    @Override
    public boolean supports(Event event, Context context) {
        return event == Event.AFTER_VALIDATE
                || event == Event.AFTER_MIGRATE
                || event == Event.BEFORE_EACH_MIGRATE
                || event == Event.AFTER_EACH_MIGRATE;
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return true;
    }

    @Override
    public void handle(Event event, Context context) {
        switch (event) {
            case AFTER_VALIDATE:
                System.out.println("\u001B[34m[数据库初始化] \t已检测 SQL 脚本文件，格式全部合法。\u001B[0m");
                break;
            case AFTER_MIGRATE:
                System.out.println("\u001B[32m[数据库初始化] \t已比对当前表结构，已经是最新版本，跳过更新。\u001B[0m");
                break;
            case BEFORE_EACH_MIGRATE:
                String script = (context != null && context.getMigrationInfo() != null)
                        ? context.getMigrationInfo().getScript()
                        : "未知脚本";
                System.out.println("\u001B[33m  └─> 发现新的 SQL 表结构变更，正在执行脚本: " + script + " ...\u001B[0m");
                break;
            case AFTER_EACH_MIGRATE:
                String doneScript = (context != null && context.getMigrationInfo() != null)
                        ? context.getMigrationInfo().getScript()
                        : "未知脚本";
                System.out.println("\u001B[32m      [" + doneScript + "] 更新成功！\u001B[0m");
                break;
            default:
                break;
        }
    }

    @Override
    public String getCallbackName() {
        return "BmlChineseFlywayInterceptor";
    }
}
