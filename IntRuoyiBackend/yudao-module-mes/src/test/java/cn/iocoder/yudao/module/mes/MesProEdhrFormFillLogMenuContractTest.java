package cn.iocoder.yudao.module.mes;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrFormFillLogMenuContractTest {

    @Test
    void menuMigrationAndFrontendRouteExposeReadOnlyFormFillLogPage() throws Exception {
        Path projectDir = findProjectDir();
        String sql = Files.readString(projectDir.resolve("sql/mysql/20260713_mes_edhr_form_fill_log_menu.sql"),
                StandardCharsets.UTF_8);
        String route = Files.readString(projectDir.getParent()
                        .resolve("IntRuoyiFronted/src/router/modules/remaining.ts"),
                StandardCharsets.UTF_8);

        assertTrue(sql.contains("release-migration"), "menu SQL must declare release migration metadata");
        assertTrue(sql.contains("900432, '表单日志', 'mes:pro-edhr-form-fill-log:query', 2, 6, 900220"),
                "page menu must be read-only query permission");
        assertTrue(sql.contains("900433, '表单日志查询', 'mes:pro-edhr-form-fill-log:query', 3"),
                "query button permission must exist");
        assertTrue(sql.contains("SET `name` = '表单日志'"), "existing page menu row must be renamed idempotently");
        assertTrue(sql.contains("`sort` = 6"), "page menu sort must place it after 放行追溯 sort=5");
        assertTrue(sql.contains("WHERE `id` = 900303"),
                "version governance row must be shifted when an existing DB has the same sort");
        assertTrue(sql.contains("`sort` <= 6"),
                "version governance shift must only apply when it blocks the requested menu position");
        assertTrue(sql.contains("900220"), "form fill log must stay under the eDHR parent menu");
        assertTrue(sql.contains("'/mes/pro/feedback/edhr-form-fill-log'"), "menu path must match frontend route");
        assertTrue(sql.contains("'mes/pro/edhr/FormFillLogPage'"), "menu component must match page file");
        assertTrue(sql.contains("'MesProEdhrFormFillLogPage'"), "menu component name must match route name");
        assertTrue(sql.contains("system_tenant_package"), "tenant package menu_ids must be updated");
        assertTrue(sql.contains("JSON_VALID"), "tenant package JSON must fail fast on invalid menu_ids");
        assertTrue(sql.contains("system_role_menu"), "tenant admin role-menu binding must be updated");
        assertTrue(sql.contains("'tenant_admin'"), "role binding must target tenant_admin roles");
        assertFalse(sql.contains("mes:pro-edhr-form-fill-log:update"), "log module must not add update permission");
        assertFalse(sql.contains("mes:pro-edhr-form-fill-log:create"), "log module must not add create permission");

        assertTrue(route.contains("path: 'pro/feedback/edhr-form-fill-log'"),
                "remaining route must expose the log page path");
        assertTrue(route.contains("@/views/mes/pro/edhr/FormFillLogPage.vue"),
                "remaining route must load the target page");
        assertTrue(route.contains("name: 'MesProEdhrFormFillLogPage'"),
                "remaining route name must match menu component name");
        assertTrue(route.contains("permission: ['mes:pro-edhr-form-fill-log:query']"),
                "remaining route must use read-only query permission");
        assertTrue(route.contains("title: '表单日志'"),
                "remaining route title must use the shortened menu name");
    }

    private static Path findProjectDir() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("sql/mysql"))
                    && Files.exists(current.resolve("yudao-module-mes"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate ruoyi-vue-pro project directory");
    }
}
