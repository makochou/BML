package com.bml.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * BML Backend 启动类
 *
 * @author BML Team
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.bml"})
public class BmlApplication {

    public static void main(String[] args) {
        SpringApplication.run(BmlApplication.class, args);
    }
}
