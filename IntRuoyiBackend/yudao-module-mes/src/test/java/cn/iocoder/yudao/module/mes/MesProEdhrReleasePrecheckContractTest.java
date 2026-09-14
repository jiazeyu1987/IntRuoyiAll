package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.MesProEdhrReleaseSettingController;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrReleaseDossierRequirementSettingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrReleaseDossierRequirementSettingUpdateReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseDossierRequirementSettingService;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        assertTrue(source.contains("materialGateManifestHash"));
        assertTrue(source.contains("extractMaterialGateManifestHash"));
        assertTrue(source.contains("fourMaterialGateService"));
        assertTrue(source.contains("CHECK_DOSSIER_INCOMING_INSPECTION_REPORT"));
        assertTrue(source.contains("CHECK_DOSSIER_STERILIZATION_REPORT"));
        assertTrue(source.contains("CHECK_DOSSIER_FINISHED_PRODUCT_INSPECTION_REPORT"));
        assertTrue(source.contains("CHECK_DOSSIER_FINISHED_PRODUCT_INSPECTION_RECORD"));
        assertTrue(source.contains("public MesProEdhrReleaseRespVO approve("));
        assertTrue(source.contains("public MesProEdhrReleaseRespVO reject("));
        assertTrue(source.contains("public MesProEdhrReleaseRespVO withdraw("));
        assertTrue(source.contains("public PageResult<MesProEdhrReleaseEventRespVO> getEventPage("));
    }

    @Test
    void dossierRequirementSettingControllerUsesGoldenFingerPermission() throws Exception {
        RequestMapping mapping = MesProEdhrReleaseSettingController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/edhr-release-setting"}, mapping.value());

        Method get = MesProEdhrReleaseSettingController.class.getDeclaredMethod("getDossierRequirements");
        assertArrayEquals(new String[]{"/dossier-requirements"}, get.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-batch-record-execution:golden-finger')",
                get.getAnnotation(PreAuthorize.class).value());

        Method update = MesProEdhrReleaseSettingController.class.getDeclaredMethod("updateDossierRequirements",
                EdhrReleaseDossierRequirementSettingUpdateReqVO.class);
        assertArrayEquals(new String[]{"/dossier-requirements"}, update.getAnnotation(PutMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-batch-record-execution:golden-finger')",
                update.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void dossierRequirementSettingServiceAndVoExposeCompleteBooleanContract() throws Exception {
        assertEquals("mes.edhr.release.dossier.requirements",
                MesProEdhrReleaseDossierRequirementSettingService.CONFIG_KEY);
        MesProEdhrReleaseDossierRequirementSettingService.class.getDeclaredMethod("getRequirementSetting");
        MesProEdhrReleaseDossierRequirementSettingService.class.getDeclaredMethod("updateRequirementSetting",
                EdhrReleaseDossierRequirementSettingUpdateReqVO.class);
        MesProEdhrReleaseDossierRequirementSettingService.class.getDeclaredMethod("getRequirementState");
        MesProEdhrReleaseDossierRequirementSettingService.class.getDeclaredMethod("requireCurrentConfigHash",
                String.class);

        requireGetter(EdhrReleaseDossierRequirementSettingRespVO.class, "getIncomingInspectionReportRequired");
        requireGetter(EdhrReleaseDossierRequirementSettingRespVO.class, "getSterilizationReportRequired");
        requireGetter(EdhrReleaseDossierRequirementSettingRespVO.class, "getFinishedProductInspectionReportRequired");
        requireGetter(EdhrReleaseDossierRequirementSettingRespVO.class, "getFinishedProductInspectionRecordRequired");
        requireGetter(EdhrReleaseDossierRequirementSettingRespVO.class, "getConfigKey");
        requireGetter(EdhrReleaseDossierRequirementSettingRespVO.class, "getConfigHash");

        for (String fieldName : new String[]{
                "incomingInspectionReportRequired",
                "sterilizationReportRequired",
                "finishedProductInspectionReportRequired",
                "finishedProductInspectionRecordRequired"}) {
            Field field = EdhrReleaseDossierRequirementSettingUpdateReqVO.class.getDeclaredField(fieldName);
            assertNotNull(field.getAnnotation(NotNull.class), fieldName + " must be required");
            requireGetter(EdhrReleaseDossierRequirementSettingUpdateReqVO.class,
                    "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1));
        }
    }

    @Test
    void sqlKeepsRequiredReleaseObjectsAndPermissions() throws Exception {
        String precheckSql = read("sql/mysql/20260618_mes_edhr_release_precheck_engine.sql");
        String lifecycleSql = read("sql/mysql/20260618_mes_edhr_release_transaction_lifecycle.sql");
        String visibleTabsSql = read("sql/mysql/20260702_mes_edhr_seven_visible_tabs.sql");
        String traceMenuSql = read("sql/mysql/20260714_mes_edhr_release_trace_menu.sql");
        String dossierRequirementSql = read("sql/mysql/20260726_mes_edhr_release_dossier_requirements.sql");

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
        assertTrue(dossierRequirementSql.contains("mes.edhr.release.dossier.requirements"));
        assertTrue(dossierRequirementSql.contains("\"incomingInspectionReportRequired\":false"));
        assertTrue(dossierRequirementSql.contains("\"sterilizationReportRequired\":false"));
        assertTrue(dossierRequirementSql.contains("\"finishedProductInspectionReportRequired\":false"));
        assertTrue(dossierRequirementSql.contains("\"finishedProductInspectionRecordRequired\":false"));
        assertTrue(dossierRequirementSql.contains("Missing infra_config table"));
        assertFalse(dossierRequirementSql.toUpperCase().contains("INSERT IGNORE"));
    }

    private static void requireGetter(Class<?> type, String getterName) throws Exception {
        type.getDeclaredMethod(getterName);
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
