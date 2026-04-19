package com.bml;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;

@SpringBootTest(classes = com.bml.app.BmlApplication.class)
public class DbCheckTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void checkMenus() {
        System.out.println("====== DB MENU CHECK START ======");
        List<Map<String, Object>> menus = jdbcTemplate.queryForList("SELECT id, menu_name, sort, parent_id FROM sys_menu ORDER BY parent_id, sort");
        for (Map<String, Object> menu : menus) {
            System.out.println(menu);
        }
        System.out.println("====== DB MENU CHECK END ======");
        
        System.out.println("====== FLYWAY HISTORY START ======");
        List<Map<String, Object>> history = jdbcTemplate.queryForList("SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank");
        for (Map<String, Object> row : history) {
            System.out.println(row);
        }
        System.out.println("====== FLYWAY HISTORY END ======");
    }
}
