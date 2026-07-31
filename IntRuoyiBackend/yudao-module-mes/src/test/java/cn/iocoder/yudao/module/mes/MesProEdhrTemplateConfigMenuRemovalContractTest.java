package cn.iocoder.yudao.module.mes;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrTemplateConfigMenuRemovalContractTest {

    @Test
    void menuMigrationRemovesLegacyTemplateConfigPageButKeepsPermission() throws Exception {
        Path projectDir = findProjectDir();
        String sql = Files.readString(projectDir.resolve("sql/mysql/20260715_mes_edhr_template_config_menu_removal.sql"),
                StandardCharsets.UTF_8);

        assertTrue(sql.contains("release-migration"), "menu removal SQL must declare release migration metadata");
        assertTrue(sql.contains("SIGNAL SQLSTATE '45000'"), "migration must fail fast when target menu identity is wrong");
        assertTrue(sql.contains("`name` = 'eDHR批记录'"), "parent menu 900220 must use the approved eDHR batch-record label");
        assertTrue(sql.contains("WHERE `id` = 900002"), "migration must target fixed menu id 900002");
        assertTrue(sql.contains("'mes:pro-batch-record-template:query'"), "template query permission must be retained");
        assertTrue(sql.contains("`type` = 3"), "900002 must become a BUTTON permission row");
        assertTrue(sql.contains("`path` = ''"), "legacy route path must be cleared");
        assertTrue(sql.contains("`component` = ''"), "legacy route component must be cleared");
        assertTrue(sql.contains("`component_name` = ''"), "legacy component name must be cleared");
        assertTrue(sql.contains("`visible` = b'0'"), "legacy menu row must be invisible");
        assertTrue(sql.contains("900365") && sql.contains("`sort` = 0"), "批记录表单 must become the first visible child");
        assertTrue(sql.contains("900033") && sql.contains("`sort` = 1"), "批次执行 must shift after 批记录表单");
        assertTrue(sql.contains("WHEN 900025 THEN '表单追溯'") && sql.contains("WHEN 900025 THEN 2"),
                "表单追溯 must remain visible after 批次执行");
        assertFalse(sql.contains("WHEN 900235 THEN '变更与异常'"),
                "变更与异常 must not be restored as an independent visible child");
        assertFalse(sql.contains("WHEN 900260 THEN '放行与归档'"),
                "放行与归档 must not be restored as an independent visible child");
        assertFalse(sql.contains("WHEN 900260 THEN '/mes/pro/feedback/edhr-release'"),
                "放行与归档 must not restore its standalone route path");
        assertFalse(sql.contains("WHEN 900260 THEN 'mes/pro/edhr-release/ReleasePage'"),
                "放行与归档 must not restore its standalone frontend component");
        assertTrue(sql.contains("WHEN 900260 THEN 'eDHR放行查询'")
                        && sql.contains("WHEN 900260 THEN 'mes:pro-edhr-release:query'"),
                "放行权限 must be retained as a hidden form-trace permission row");
        assertTrue(sql.contains("WHERE `id` IN (900235, 900260)"),
                "old change and release labels must be converted to hidden permission rows together");
        assertTrue(sql.contains("WHEN 900432 THEN '表单日志'") && sql.contains("WHEN 900432 THEN 3"),
                "表单日志 must shift directly after 表单追溯");
        assertFalse(sql.matches("(?is).*DELETE\\s+FROM\\s+`?system_(menu|role_menu|tenant_package)`?.*"),
                "migration must not delete menu, role-menu, or tenant package data");

        Path frontendRoot = projectDir.getParent().resolve("IntRuoyiFronted");
        assertFalse(Files.exists(frontendRoot.resolve("src/views/mes/pro/batchrecordtemplate/index.vue")),
                "legacy template config page must be removed from frontend source");
        assertTrue(Files.exists(frontendRoot.resolve("src/views/mes/pro/batchrecord-shared/DesignerWrapper.vue")),
                "DesignerWrapper must be retained through the shared batch record module");
        assertTrue(Files.exists(frontendRoot.resolve("src/views/mes/pro/batchrecord-shared/batchRecordTemplateRules.ts")),
                "template rule helpers must be retained through the shared batch record module");
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
