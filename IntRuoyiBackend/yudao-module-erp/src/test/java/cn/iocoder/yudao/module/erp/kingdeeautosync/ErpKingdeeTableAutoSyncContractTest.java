package cn.iocoder.yudao.module.erp.kingdeeautosync;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpKingdeeTableAutoSyncContractTest {

    private static final Path ERP_MAIN = Path.of("src/main/java/cn/iocoder/yudao/module/erp");

    @Test
    void controller_mustExposeProfileConfigPermissionBoundApis() throws IOException {
        String source = read(ERP_MAIN.resolve("controller/admin/kingdeeautosync/ErpKingdeeTableAutoSyncController.java"));

        assertContains(source, "@RequestMapping(\"/erp/kingdee-table-auto-sync\")");
        assertContains(source, "@GetMapping(\"/plan/get\")");
        assertContains(source, "@PutMapping(\"/plan/save\")");
        assertContains(source, "@GetMapping(\"/sync-types\")");
        assertContains(source, "@PostMapping(\"/plan/run-once\")");
        assertContains(source, "@GetMapping(\"/run/page\")");
        assertContains(source, "@GetMapping(\"/watermark/list\")");
        assertContains(source, "mes:pro-batch-record-execution:golden-finger");
        assertFalse(source.contains("erp:kingdee-sync:query"), "Profile config APIs must use the profile config permission boundary.");
    }

    @Test
    void serviceAndPersistence_mustStoreTenantPlanAndReuseOfficialSyncRuntime() throws IOException {
        String service = read(ERP_MAIN.resolve("service/kingdeeautosync/ErpKingdeeTableAutoSyncService.java"));
        String impl = read(ERP_MAIN.resolve("service/kingdeeautosync/ErpKingdeeTableAutoSyncServiceImpl.java"));
        String planDo = read(ERP_MAIN.resolve("dal/dataobject/kingdeeautosync/ErpKingdeeTableAutoSyncPlanDO.java"));
        String itemDo = read(ERP_MAIN.resolve("dal/dataobject/kingdeeautosync/ErpKingdeeTableAutoSyncPlanItemDO.java"));
        String itemMapper = read(ERP_MAIN.resolve("dal/mysql/kingdeeautosync/ErpKingdeeTableAutoSyncPlanItemMapper.java"));
        String planResp = read(ERP_MAIN.resolve("controller/admin/kingdeeautosync/vo/ErpKingdeeTableAutoSyncPlanRespVO.java"));
        String planSaveReq = read(ERP_MAIN.resolve("controller/admin/kingdeeautosync/vo/ErpKingdeeTableAutoSyncPlanSaveReqVO.java"));

        for (String token : new String[]{
                "getPlan()",
                "savePlan(",
                "getSyncTypes()",
                "runOnce()",
                "getRunPage(",
                "getWatermarks()",
                "executeAutoForCurrentTenant()"
        }) {
            assertContains(service, token);
        }
        assertContains(planDo, "@TableName(\"erp_kingdee_table_auto_sync_plan\")");
        assertContains(itemDo, "@TableName(\"erp_kingdee_table_auto_sync_plan_item\")");
        assertContains(itemMapper, "selectListByPlanId");
        assertContains(impl, "ErpKingdeeSyncAdminService");
        assertContains(impl, "JobHandler");
        assertContains(impl, "SpringUtil.getBean");
        assertContains(impl, "ErpKingdeeTableAutoSyncTypeEnum.list()");
        assertContains(impl, "setHandlerParam(\"\")");
        assertContains(planResp, "@JsonFormat(pattern = \"HH:mm:ss\")");
        assertContains(planSaveReq, "@JsonFormat(pattern = \"HH:mm:ss\")");
        assertContains(impl, "savePlanItems(");
        assertContains(impl, "markAutoRunDateAfterSuccess(");
        int successMessageIndex = impl.indexOf("ERP 表格自动同步完成");
        int markAutoRunDateIndex = impl.indexOf("markAutoRunDateAfterSuccess(plan.getId());");
        int failureHandlerIndex = impl.indexOf("} catch (Exception ex)");
        assertTrue(successMessageIndex >= 0 && markAutoRunDateIndex > successMessageIndex
                        && failureHandlerIndex > markAutoRunDateIndex,
                "Auto retry date must be marked only after all selected handlers succeed.");
        assertFalse(impl.contains("ErpNasTableSyncTypeEnum"), "ERP table auto sync must not reuse NAS export enum.");
        assertFalse(impl.contains("deleteByPlanId"), "Plan item save must not logical-delete rows before reinserting.");
    }

    @Test
    void syncTypeMapping_mustCoverAllOfficialKingdeeSyncTypesAndHandlers() throws IOException {
        String officialEnum = read(ERP_MAIN.resolve("enums/sync/ErpKingdeeSyncTypeEnum.java"));
        String autoEnum = read(ERP_MAIN.resolve("enums/kingdeeautosync/ErpKingdeeTableAutoSyncTypeEnum.java"));

        for (String token : new String[]{
                "PRODUCT",
                "STOCK",
                "PURCHASE_ORDER",
                "SALE_ORDER",
                "PRODUCTION_ORDER",
                "PRODUCTION_PICK_LIST",
                "PRODUCTION_MATERIAL_LIST",
                "BOM"
        }) {
            assertContains(officialEnum, token);
            assertContains(autoEnum, "ErpKingdeeSyncTypeEnum." + token);
        }
        for (String handlerName : new String[]{
                "kingdeeProductItemSyncJob",
                "kingdeeStockSyncJob",
                "kingdeePurchaseOrderSyncJob",
                "kingdeeSaleOrderSyncJob",
                "kingdeeProductionOrderSyncJob",
                "kingdeeProductionPickListSyncJob",
                "kingdeeProductionMaterialListSyncJob",
                "kingdeeBomSyncJob"
        }) {
            assertContains(autoEnum, handlerName);
        }
    }

    @Test
    void jobHandler_mustExecuteCurrentTenantPlanUnderTenantJob() throws IOException {
        String source = read(ERP_MAIN.resolve("job/kingdeeautosync/ErpKingdeeTableAutoSyncJob.java"));

        assertContains(source, "@Component(\"erpKingdeeTableAutoSyncJob\")");
        assertContains(source, "implements JobHandler");
        assertContains(source, "@TenantJob");
        assertContains(source, "execute(String param)");
        assertContains(source, "executeAutoForCurrentTenant()");
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path);
    }

    private static void assertContains(String source, String token) {
        assertTrue(source.contains(token), "Missing token: " + token);
    }
}
