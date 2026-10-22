package cn.iocoder.yudao.module.mes;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MesTeamEmployeeOwnershipSchemaTest {

    @Test
    void mysqlMigrationMustGuardEnabledOwnerWithoutRewritingHistory() throws Exception {
        Path cwd = Path.of("").toAbsolutePath();
        Path backend = "yudao-module-mes".equals(cwd.getFileName().toString()) ? cwd.getParent()
                : "IntRuoyiBackend".equals(cwd.getFileName().toString()) ? cwd : cwd.resolve("IntRuoyiBackend");
        Path migration = backend.resolve("sql/mysql/20260928_mes_formal_employee_enabled_owner.sql");
        assertTrue(Files.exists(migration), "正式员工启用归属必须有独立 MySQL 约束迁移");
        String sql = Files.readString(migration).toLowerCase();
        assertTrue(sql.contains("generated always as"));
        assertTrue(sql.contains("enabled_formal_user_id"));
        assertTrue(sql.contains("uk_mes_pp_employee_enabled_user"));
        assertTrue(sql.contains("(tenant_id, enabled_formal_user_id)"));
        assertTrue(sql.contains("information_schema.columns"));
        assertTrue(sql.contains("information_schema.statistics"));
        assertTrue(sql.contains("having count(*) > 1"));
        assertTrue(sql.contains("signal sqlstate '45000'"));
        assertFalse(sql.contains("update mes_pro_process_pool_team_employee_profile"));
        assertFalse(sql.contains("delete from mes_pro_process_pool_team_employee_profile"));
        assertFalse(sql.contains("add column if not exists"));
        assertTrue(sql.indexOf("having count(*) > 1") < sql.indexOf("add column enabled_formal_user_id"));
    }
}
