package cn.iocoder.yudao.module.erp.kingdeeautosync;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpKingdeeTableAutoSyncContractTest {

    private static final Path ERP_MAIN = Path.of("src/main/java/cn/iocoder/yudao/module/erp");
    private static final Path MES_MAIN = Path.of("../yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes");
    private static final Pattern DIRECT_PROPERTIES_FIELD = Pattern.compile(
            "private\\s+(?:final\\s+)?ErpKingdeeProperties\\s+\\w+\\s*;");

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
                "STOCK_MOVE",
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
                "kingdeeStockMoveSyncJob",
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
    void officialSyncEntrypoints_mustResolveCurrentActiveConnection() throws IOException {
        for (Path sourcePath : officialSyncServiceEntrypoints()) {
            String source = read(sourcePath);
            assertContains(source, "ErpKingdeeConfigService");
            assertContains(source, "getEffectiveProperties()");
        }

        String configSource = read(ERP_MAIN.resolve("service/config/ErpKingdeeConfigServiceImpl.java"));
        assertContains(configSource, "resolveActiveConnectionType()");
        assertContains(configSource, "KINGDEE_ACTIVE_CONNECTION_CONFIG_MISSING");
    }

    @Test
    void businessSyncServices_mustNotHoldDefaultKingdeePropertiesDirectly() throws IOException {
        for (Path sourcePath : officialSyncServiceEntrypoints()) {
            String source = read(sourcePath);
            assertFalse(DIRECT_PROPERTIES_FIELD.matcher(source).find(),
                    sourcePath + " must use ErpKingdeeConfigService.getEffectiveProperties(), not a direct properties field.");
            assertFalse(source.contains("defaultKingdeeProperties"),
                    sourcePath + " must not own the default Kingdee properties bean.");
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

    @Test
    void allOfficialHandlers_mustExposeExplicitFullSyncMode() throws IOException {
        for (Path sourcePath : fullSyncHandlerSources()) {
            String source = read(sourcePath);
            assertContains(source, "ErpKingdeeFullSyncHandler");
            assertContains(source, "executeFullSync()");
            assertContains(source, "ErpKingdeeSyncTriggerTypeEnum.FULL");
            assertContains(source, "FULL_SYNC_JOB_PARAM");
        }
    }

    @Test
    void fullSyncSubmission_mustUseQuartzAndCommittedRuntimeTransactions() throws IOException {
        String adminService = read(ERP_MAIN.resolve(
                "service/sync/admin/ErpKingdeeSyncAdminServiceImpl.java"));
        String runtimeService = read(ERP_MAIN.resolve(
                "service/sync/runtime/ErpKingdeeSyncRuntimeServiceImpl.java"));
        String transactionService = read(ERP_MAIN.resolve(
                "service/sync/runtime/ErpKingdeeSyncRuntimeTransactionService.java"));

        assertContains(adminService, "TenantJobParam.forTenant");
        assertContains(adminService, "TenantContextHolder.getRequiredTenantId()");
        assertContains(adminService, "getJobPage");
        assertFalse(adminService.contains("handler.executeFullSync()"),
                "HTTP full-sync submission must not execute ERP work synchronously.");
        assertFalse(runtimeService.contains("@Transactional"),
                "The outer runtime call must not hold a transaction across the remote ERP request.");
        assertContains(transactionService, "@Transactional");
        assertContains(transactionService, "runMapper.insert(run)");
        assertContains(transactionService, "ErpKingdeeSyncRunStatusEnum.RUNNING");
    }

    private static List<Path> fullSyncHandlerSources() {
        return List.of(
                ERP_MAIN.resolve("job/stock/KingdeeStockSyncJob.java"),
                ERP_MAIN.resolve("job/stock/KingdeeStockMoveSyncJob.java"),
                ERP_MAIN.resolve("job/purchase/KingdeePurchaseOrderSyncJob.java"),
                ERP_MAIN.resolve("job/sale/KingdeeSaleOrderSyncJob.java"),
                ERP_MAIN.resolve("job/production/KingdeeProductionPickListSyncJob.java"),
                MES_MAIN.resolve("job/md/KingdeeProductItemSyncJob.java"),
                MES_MAIN.resolve("job/md/KingdeeBomSyncJob.java"),
                MES_MAIN.resolve("job/workorder/KingdeeProductionOrderSyncJob.java"),
                MES_MAIN.resolve("job/workorder/KingdeeProductionMaterialListSyncJob.java")
        );
    }

    private static List<Path> officialSyncServiceEntrypoints() {
        return List.of(
                ERP_MAIN.resolve("service/product/sync/ErpKingdeeProductSyncServiceImpl.java"),
                ERP_MAIN.resolve("service/purchase/sync/ErpKingdeePurchaseOrderSyncServiceImpl.java"),
                ERP_MAIN.resolve("service/sale/sync/ErpKingdeeSaleOrderSyncServiceImpl.java"),
                ERP_MAIN.resolve("service/stock/sync/ErpKingdeeStockSyncServiceImpl.java"),
                ERP_MAIN.resolve("service/stock/kingdee/ErpKingdeeInventoryListServiceImpl.java"),
                ERP_MAIN.resolve("service/stock/kingdee/ErpKingdeeStockMoveListServiceImpl.java"),
                ERP_MAIN.resolve("service/production/kingdee/ErpKingdeeProductionPickListServiceImpl.java"),
                ERP_MAIN.resolve("service/sync/admin/ErpKingdeeProductionOrderCreateServiceImpl.java"),
                MES_MAIN.resolve("service/md/item/sync/MesKingdeeProductBomSyncServiceImpl.java"),
                MES_MAIN.resolve("service/md/item/kingdee/MesKingdeeBomListServiceImpl.java"),
                MES_MAIN.resolve("service/pro/workorder/sync/MesKingdeeProductionOrderSyncServiceImpl.java"),
                MES_MAIN.resolve("service/pro/workorder/sync/MesKingdeeProductionMaterialListSyncServiceImpl.java"),
                MES_MAIN.resolve("service/pro/workorder/sync/MesKingdeeWorkOrderBomSyncServiceImpl.java"),
                MES_MAIN.resolve("service/pro/workorder/sync/MesKingdeeProductionOrderCreateServiceImpl.java")
        );
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path);
    }

    private static void assertContains(String source, String token) {
        assertTrue(source.contains(token), "Missing token: " + token);
    }
}
