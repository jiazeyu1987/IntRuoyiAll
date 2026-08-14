package cn.iocoder.yudao.module.erp.nastablesync;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpNasTableSyncContractTest {

    private static final Path ERP_MAIN = Path.of("src/main/java/cn/iocoder/yudao/module/erp");
    private static final Path INFRA_MAIN = Path.of("../yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra");

    @Test
    void controller_mustExposeProfileConfigPermissionBoundApis() throws IOException {
        String source = read(ERP_MAIN.resolve("controller/admin/nastablesync/ErpNasTableSyncController.java"));

        assertContains(source, "@RequestMapping(\"/erp/nas-table-sync\")");
        assertContains(source, "@GetMapping(\"/plan/get\")");
        assertContains(source, "@PutMapping(\"/plan/save\")");
        assertContains(source, "@GetMapping(\"/sync-types\")");
        assertContains(source, "@PostMapping(\"/plan/test-nas-write\")");
        assertContains(source, "@PostMapping(\"/plan/run-once\")");
        assertContains(source, "@GetMapping(\"/run/page\")");
        assertContains(source, "mes:pro-batch-record-execution:golden-finger");
    }

    @Test
    void serviceAndPersistence_mustKeepBusinessConfigOutsideJobParam() throws IOException {
        String service = read(ERP_MAIN.resolve("service/nastablesync/ErpNasTableSyncService.java"));
        String impl = read(ERP_MAIN.resolve("service/nastablesync/ErpNasTableSyncServiceImpl.java"));
        String planDo = read(ERP_MAIN.resolve("dal/dataobject/nastablesync/ErpNasTableSyncPlanDO.java"));
        String itemMapper = read(ERP_MAIN.resolve("dal/mysql/nastablesync/ErpNasTableSyncPlanItemMapper.java"));
        String planResp = read(ERP_MAIN.resolve("controller/admin/nastablesync/vo/ErpNasTableSyncPlanRespVO.java"));
        String planSaveReq = read(ERP_MAIN.resolve("controller/admin/nastablesync/vo/ErpNasTableSyncPlanSaveReqVO.java"));

        for (String token : new String[]{
                "getPlan()",
                "savePlan(",
                "getSyncTypes()",
                "testNasWrite(",
                "runOnce()",
                "getRunPage("
        }) {
            assertContains(service, token);
        }
        assertContains(planDo, "@TableName(\"erp_nas_table_sync_plan\")");
        assertContains(itemMapper, "selectListByPlanId");
        assertContains(impl, "setHandlerParam(\"\")");
        assertContains(impl, "JobService");
        assertContains(planResp, "@JsonFormat(pattern = \"HH:mm:ss\")");
        assertContains(planSaveReq, "@JsonFormat(pattern = \"HH:mm:ss\")");
        assertContains(impl, "savePlanItems(");
        assertFalse(impl.contains("deleteByPlanId"), "Plan item save must not logical-delete rows before reinserting.");
    }

    @Test
    void jobHandler_mustExecuteCurrentTenantPlanUnderTenantJob() throws IOException {
        String source = read(ERP_MAIN.resolve("job/nastablesync/ErpNasTableAutoSyncJob.java"));

        assertContains(source, "@Component(\"erpNasTableAutoSyncJob\")");
        assertContains(source, "implements JobHandler");
        assertContains(source, "@TenantJob");
        assertContains(source, "execute(String param)");
        assertContains(source, "executeAutoForCurrentTenant()");
    }

    @Test
    void nasUpload_mustUseExplicitInputStreamWriteApi() throws IOException {
        String service = read(INFRA_MAIN.resolve("service/file/NasBrowserService.java"));
        String impl = read(INFRA_MAIN.resolve("service/file/NasBrowserServiceImpl.java"));

        assertContains(service, "void writeFile(String path, InputStream inputStream)");
        assertContains(service, "void writeFile(NasConnectionConfig config, String path, InputStream inputStream)");
        assertContains(impl, "SMB2CreateDisposition.FILE_OVERWRITE_IF");
        assertContains(impl, "writeFile(String normalizedRelativePath, InputStream inputStream)");
    }

    @Test
    void erpModule_mustDeclareDirectInfraDependencyForNasAndJobServices() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));

        assertContains(pom, "<artifactId>yudao-module-infra</artifactId>");
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path);
    }

    private static void assertContains(String source, String token) {
        assertTrue(source.contains(token), "Missing token: " + token);
    }
}
