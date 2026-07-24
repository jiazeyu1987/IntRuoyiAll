package cn.iocoder.yudao.module.mes.service.pro.route;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProRouteCreatorPermissionBackfillMigrationContractTest {

    @Test
    void migration_shouldBackfillRouteCreatorObjectPermissionsWithoutDestructiveDataChanges() throws Exception {
        Path migration = Path.of(System.getProperty("user.dir"))
                .getParent()
                .resolve(Path.of("sql", "mysql",
                        "20260722_mes_route_creator_route_edit_permission_backfill.sql"));
        String sql = Files.readString(migration, StandardCharsets.UTF_8);

        assertTrue(sql.contains("release-migration"), "migration must declare release metadata");
        assertTrue(sql.contains("ensure_mes_route_creator_route_edit_permission_backfill"),
                "migration must use a named idempotent procedure");
        assertTrue(sql.contains("`mes_pro_route`"), "migration must source real route rows");
        assertTrue(sql.contains("`mes_pro_edhr_permission_scope`"),
                "migration must create missing ROUTE permission scopes");
        assertTrue(sql.contains("`mes_pro_edhr_permission_rule`"),
                "migration must create missing creator permission rules");
        assertTrue(sql.contains("`route`.`creator` REGEXP '^[0-9]+$'"),
                "migration must only cast numeric route creators");
        assertTrue(sql.contains("CAST(`route`.`creator` AS UNSIGNED)"),
                "migration must bind creator user ids from route audit data");
        assertTrue(sql.contains("'VIEW'") && sql.contains("'ROUTE_EDIT'") && sql.contains("'PERMISSION_ADMIN'"),
                "migration must grant the complete route-owner ability set");
        assertTrue(sql.contains("NOT EXISTS"), "migration must be idempotent");
        assertTrue(sql.contains("`route`.`id` = 922119"),
                "migration must assert the reported route is repaired when present");
        assertTrue(sql.contains("SIGNAL SQLSTATE '45000'"),
                "migration must fail fast if reported route cannot be repaired");
        assertFalse(sql.matches("(?is).*\\bDELETE\\s+FROM\\b.*"),
                "migration must not delete data");
        assertFalse(sql.matches("(?is).*\\bTRUNCATE\\s+TABLE\\b.*"),
                "migration must not truncate data");
    }
}
