package cn.iocoder.yudao.module.mes.service.md.autocode;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.tenant.TenantCreatedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MesFeedbackCodeInitializerTest {
    private JdbcTemplate jdbc;
    private MesFeedbackCodeInitializer initializer;

    @BeforeEach void setup() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE system_tenant (id bigint PRIMARY KEY)");
        jdbc.execute("INSERT INTO system_tenant VALUES (186)");
        jdbc.execute("CREATE TABLE mes_md_auto_code_rule (id bigint AUTO_INCREMENT PRIMARY KEY,tenant_id bigint,"
                + "project_id bigint,code varchar(64),name varchar(128),max_length int,padded int,status int,deleted int DEFAULT 0)");
        jdbc.execute("CREATE TABLE mes_md_auto_code_part (tenant_id bigint,rule_id bigint,sort int,type int,length int,"
                + "fix_character varchar(64),serial_start_no int,serial_step int,cycle_flag int)");
        initializer = new MesFeedbackCodeInitializer(jdbc);
        TenantContextHolder.setTenantId(186L);
    }

    @AfterEach void close() { TenantContextHolder.clear(); }

    @Test void newTenantCanResolveFormalFeedbackRuleAndParts() {
        initializer.initialize(new TenantCreatedEvent(186L));
        assertEquals("PRO_FEEDBACK_CODE", jdbc.queryForObject("SELECT code FROM mes_md_auto_code_rule", String.class));
        assertEquals("FB-", jdbc.queryForObject("SELECT fix_character FROM mes_md_auto_code_part WHERE sort=1", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT serial_start_no FROM mes_md_auto_code_part WHERE sort=2", Integer.class));
        initializer.initialize(new TenantCreatedEvent(186L));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM mes_md_auto_code_part", Integer.class));
    }

    @Test void existingCustomRuleIsPreserved() {
        initializer.initialize(new TenantCreatedEvent(186L));
        jdbc.update("UPDATE mes_md_auto_code_part SET fix_character='CUSTOM-' WHERE sort=1");
        initializer.initialize(new TenantCreatedEvent(186L));
        assertEquals("CUSTOM-", jdbc.queryForObject("SELECT fix_character FROM mes_md_auto_code_part WHERE sort=1", String.class));
    }

    @Test void mismatchedTenantCannotInitializeAnotherTenant() {
        assertThrows(IllegalStateException.class, () -> initializer.initialize(new TenantCreatedEvent(1L)));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_md_auto_code_rule", Integer.class));
    }
}
