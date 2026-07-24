package cn.iocoder.yudao.module.mes;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrReleasePrecheckContractTest {

    private static final Path ROOT = resolveRepoRoot();

    @Test
    void controllerExposesMergedReleaseLifecycleEndpoints() throws Exception {
        String source = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrReleaseController.java");

        assertTrue(source.contains("@RequestMapping(\"/mes/pro/edhr-release\")"));
        assertTrue(source.contains("@GetMapping(\"/page\")"));
        assertTrue(source.contains("@GetMapping(\"/get\")"));
        assertTrue(source.contains("@PostMapping(\"/precheck\")"));
        assertTrue(source.contains("@PostMapping(\"/submit\")"));
        assertTrue(source.contains("@PostMapping(\"/approve\")"));
        assertTrue(source.contains("@PostMapping(\"/reject\")"));
        assertTrue(source.contains("@PostMapping(\"/withdraw\")"));
        assertTrue(source.contains("@GetMapping(\"/check-item/page\")"));
        assertTrue(source.contains("@GetMapping(\"/event/page\")"));
        assertTrue(source.contains("mes:pro-edhr-release:query"));
        assertTrue(source.contains("mes:pro-edhr-release:precheck"));
        assertTrue(source.contains("mes:pro-edhr-release:submit"));
        assertTrue(source.contains("mes:pro-edhr-release:approve"));
        assertTrue(source.contains("mes:pro-edhr-release:reject"));
        assertTrue(source.contains("mes:pro-edhr-release:withdraw"));
        assertTrue(source.contains("mes:pro-edhr-release:event-query"));

        assertFalse(source.contains("/intervene"));
    }

    @Test
    void serviceKeepsStructuredGateAndReleaseLifecycle() throws Exception {
        String source = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImpl.java");

        assertTrue(source.contains("CHECK_DHR_COMPLETENESS"));
        assertTrue(source.contains("CHECK_INSPECTION_RESULT"));
        assertTrue(source.contains("CHECK_DEVIATION_CLOSED"));
        assertTrue(source.contains("CHECK_REWORK_CLOSED"));
        assertTrue(source.contains("CHECK_SCRAP_RECORDED"));
        assertTrue(source.contains("CHECK_INVENTORY_CONSISTENCY"));
        assertTrue(source.contains("closeOpenByReleaseTransactionId"));
        assertTrue(source.contains("STATUS_PRECHECK_FAILED"));
        assertTrue(source.contains("STATUS_PENDING_APPROVAL"));
        assertTrue(source.contains("STATUS_RELEASED"));
        assertTrue(source.contains("STATUS_REJECTED"));
        assertTrue(source.contains("STATUS_WITHDRAWN"));
        assertTrue(source.contains("EVENT_TYPE_SUBMIT"));
        assertTrue(source.contains("EVENT_TYPE_APPROVE"));
        assertTrue(source.contains("EVENT_TYPE_REJECT"));
        assertTrue(source.contains("EVENT_TYPE_WITHDRAW"));
        assertTrue(source.contains("public MesProEdhrReleaseRespVO submit("));
        assertTrue(source.contains("public MesProEdhrReleaseRespVO approve("));
        assertTrue(source.contains("public MesProEdhrReleaseRespVO reject("));
        assertTrue(source.contains("public MesProEdhrReleaseRespVO withdraw("));
        assertTrue(source.contains("public PageResult<MesProEdhrReleaseEventRespVO> getEventPage("));
    }

    @Test
    void sqlKeepsRequiredReleaseObjectsAndPermissions() throws Exception {
        String precheckSql = read("sql/mysql/20260618_mes_edhr_release_precheck_engine.sql");
        String lifecycleSql = read("sql/mysql/20260618_mes_edhr_release_transaction_lifecycle.sql");
        String visibleTabsSql = read("sql/mysql/20260702_mes_edhr_seven_visible_tabs.sql");
        String traceMenuSql = read("sql/mysql/20260714_mes_edhr_release_trace_menu.sql");

        assertTrue(precheckSql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_edhr_release_transaction`"));
        assertTrue(precheckSql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_edhr_release_check_item`"));
        assertTrue(precheckSql.contains("mes:pro-edhr-release:query"));
        assertTrue(precheckSql.contains("mes:pro-edhr-release:precheck"));
        assertTrue(precheckSql.contains("mes:pro-edhr-release:submit"));
        assertTrue(precheckSql.contains("mes:pro-edhr-release:approve"));
        assertTrue(precheckSql.contains("Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR release menus"));

        assertTrue(lifecycleSql.contains("CREATE TABLE IF NOT EXISTS `mes_pro_edhr_release_transaction_event`"));
        assertTrue(lifecycleSql.contains("mes:pro-edhr-release:reject"));
        assertTrue(lifecycleSql.contains("mes:pro-edhr-release:withdraw"));
        assertTrue(lifecycleSql.contains("mes:pro-edhr-release:event-query"));
        assertTrue(lifecycleSql.contains("Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR release transaction menus"));

        assertTrue(precheckSql.contains("900260, '放行追溯'"),
                "fresh installs must create the release page as a trace list");
        assertTrue(visibleTabsSql.contains("900260 AS `id`, '放行追溯' AS `name`"),
                "visible eDHR tab migration must use the short trace label");
        assertTrue(traceMenuSql.contains("SET `name` = '放行追溯'"),
                "idempotent menu migration must rename existing release menu rows");
        assertFalse(visibleTabsSql.contains("放行与归档"),
                "visible tab label must no longer present release/archive as an operation entry");
        assertFalse(traceMenuSql.contains("放行与归档"),
                "menu rename migration must not reintroduce the old operation-entry label");

        assertFalse(precheckSql.toUpperCase().contains("INSERT IGNORE"));
        assertFalse(lifecycleSql.toUpperCase().contains("INSERT IGNORE"));
    }

    private String read(String relativePath) throws Exception {
        Path path = ROOT.resolve(relativePath);
        assertTrue(Files.exists(path), relativePath + " must exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static Path resolveRepoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        if (Files.exists(current.resolve("sql/mysql"))) {
            return current;
        }
        return current.getParent();
    }
}
