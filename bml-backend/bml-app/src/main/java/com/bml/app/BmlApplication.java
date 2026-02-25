package com.bml.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.PrintStream;
import java.util.Properties;

/**
 * BML Backend 启动类
 *
 * <p>
 * 启用 {@code @EnableScheduling} 以激活所有 {@code @Scheduled} 定时任务，
 * 如 {@link com.bml.module.system.task.ServerAlertJob} 的服务器资源巡检。
 * </p>
 *
 * @author BML Team
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = { "com.bml" })
@MapperScan("com.bml.**.mapper")
public class BmlApplication {

        public static void main(String[] args) {
                SpringApplication app = new SpringApplication(BmlApplication.class);

                // 1. 编程式配置日志 (替代 logback-spring.xml)
                Properties properties = new Properties();
                properties.setProperty("logging.pattern.console",
                                "%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(%5p) %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n%wEx");
                properties.setProperty(
                                "logging.level.org.springframework.data.repository.config.RepositoryConfigurationDelegate",
                                "WARN");
                properties.setProperty("logging.level.org.apache.catalina.core", "WARN");
                properties.setProperty("logging.level.org.springframework.boot.web.embedded.tomcat", "WARN");
                properties.setProperty("logging.level.org.redisson.connection.ConnectionsHolder", "WARN");
                properties.setProperty("logging.level.org.mariadb.jdbc.message.server.ErrorPacket", "ERROR");
                properties.setProperty("logging.level.org.flywaydb.core.internal.database.base.Database", "ERROR");
                properties.setProperty("logging.level.org.flywaydb.core.internal.command", "WARN");
                properties.setProperty("logging.level.com.baomidou.mybatisplus.core.metadata.TableInfoHelper", "ERROR");
                properties.setProperty("logging.level.com.baomidou.mybatisplus.core.injector.DefaultSqlInjector",
                                "ERROR");
                properties.setProperty("logging.level.com.bml.app.BmlApplication", "WARN");
                properties.setProperty("logging.level.com.bml", "INFO");
                app.setDefaultProperties(properties);

                // 2. 编程式配置启动横幅 (替代 banner.txt)
                app.setBanner(new Banner() {
                        @Override
                        public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
                                String version = environment.getProperty("spring.application.version", "Unknown");
                                out.println(
                                                "\u001B[94m===================================================================================================");
                                out.println(
                                                "  ____    __  __   _                     ____                  _                           _");
                                out.println(
                                                " |  _ \\  |  \\/  | | |                   |  _ \\                | |                         | |");
                                out.println(
                                                " | |_) | | \\  / | | |         ______    | |_) |   __ _    ___ | | __   ___   _ __     __  | |");
                                out.println(
                                                " |  _ <  | |\\/| | | |        |______|   |  _ <   / _` |  / __|| |/ /  / _ \\ | '_ \\   / _` | |");
                                out.println(
                                                " | |_) | | |  | | | |____               | |_) | | (_| | | (__ |   <  |  __/ | | | | | (_| | |");
                                out.println(
                                                " |____/  |_|  |_| |______|              |____/   \\__,_|  \\___||_|\\_\\  \\___| |_| |_|  \\__,_|_|");
                                out.println("                                                                                        ");
                                out.println(
                                                " :: BML Enterprise Core Engine ::                                                          (v"
                                                                + version + ")");
                                out.println(
                                                "===================================================================================================\u001B[0m");
                                out.println();
                        }
                });

                // 3. 启动应用
                ApplicationContext context = app.run(args);

                // 4. 控制台启动成功汇报面板 (替代 StartupLogRunner.java)
                Environment env = context.getEnvironment();
                String port = env.getProperty("server.port", "8080");
                String path = env.getProperty("server.servlet.context-path", "");

                System.out.println("\n");
                System.out.println(
                                "\u001B[32m=========================================================================\u001B[0m");
                System.out.println("\u001B[32m    [BML 引擎] 系统启动成功，核心服务已全部就绪！\u001B[0m");
                System.out.println(
                                "\u001B[32m=========================================================================\u001B[0m");
                System.out.println(
                                "\u001B[36m    ▶ 统一业务通讯基站 (API入口): \thttp://localhost:" + port + path + "\u001B[0m");
                System.out.println(
                                "\u001B[32m    ▶ 引擎运行状态自检接口: \thttp://localhost:" + port + path
                                                + "/actuator/health  (无鉴权)\u001B[0m");
                System.out.println(
                                "\u001B[32m=========================================================================\u001B[0m");
                System.out.println();
        }
}
