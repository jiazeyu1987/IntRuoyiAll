package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordRouteGovernanceContractTest {

    private static final Path SERVICE = Path.of(
            "src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/"
                    + "MesProBatchRecordReportServiceImpl.java");
    private static final Path ROUTE_GENERATION_SERVICE = Path.of(
            "src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/"
                    + "MesProBatchRecordRouteGenerationServiceImpl.java");
    private static final Path PREFLIGHT_RESULT = Path.of(
            "src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/"
                    + "MesProBatchRecordImportPreflightResult.java");
    private static final Path CONTROLLER = Path.of(
            "src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecordreport/"
                    + "MesProBatchRecordReportController.java");
    private static final Path ROUTE_MAPPER = Path.of(
            "src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/route/MesProRouteMapper.java");

    @Test
    void preflightContract_exposesRouteGovernanceFields() throws Exception {
        String source = read(PREFLIGHT_RESULT);

        assertTrue(source.contains("String routeGovernanceStatus"),
                "预检结果必须返回 routeGovernanceStatus。");
        assertTrue(source.contains("Boolean routeUpgradeRequired"),
                "预检结果必须返回 routeUpgradeRequired，前端据此提示是否升版本。");
        assertTrue(source.contains("List<DuplicateRoute> duplicateRoutes"),
                "预检结果必须返回 duplicateRoutes，历史同名多路线时列出编码和 ID。");
        assertTrue(source.contains("record DuplicateRoute"),
                "duplicateRoutes 必须是结构化对象，不能只返回拼接字符串。");
    }

    @Test
    void recognizeUploadedContract_requiresRouteUpgradeConfirmationAndExpectedIds() throws Exception {
        String source = read(CONTROLLER);

        assertTrue(source.contains("@RequestParam(value = \"routeUpgradeConfirmed\""),
                "导入写接口必须接收 routeUpgradeConfirmed。");
        assertTrue(source.contains("@RequestParam(value = \"expectedRouteId\""),
                "导入写接口必须接收 expectedRouteId 防并发变化。");
        assertTrue(source.contains("@RequestParam(value = \"expectedRouteVersionId\""),
                "导入写接口必须接收 expectedRouteVersionId 防并发变化。");
    }

    @Test
    void routeGeneration_reusesUniqueRouteAndBlocksHistoricalDuplicates() throws Exception {
        String mapperSource = read(ROUTE_MAPPER);
        String serviceSource = read(SERVICE);
        String generationSource = read(ROUTE_GENERATION_SERVICE);

        assertTrue(mapperSource.contains("selectListByName"),
                "路线治理必须能按路线名称查询全部同名路线。");
        assertTrue(serviceSource.contains("PRO_BATCH_RECORD_REPORT_ROUTE_DUPLICATE"),
                "同名多路线必须 fail fast，不能自动选择最新或任意一条。");
        assertTrue(serviceSource.contains("PRO_BATCH_RECORD_REPORT_ROUTE_UPGRADE_CONFIRM_REQUIRED"),
                "已有唯一同名路线时，导入写入前必须校验用户确认升版本。");
        assertFalse(generationSource.contains("refreshExistingRouteForUploadedWord"),
                "Word 重建已有路线不得直接删除 active 工序、流转关系和绑定。");
        assertFalse(generationSource.contains("createNextActiveRouteVersion"),
                "Word 重建已有路线不得直接创建下一 active 路线版本。");
        assertTrue(generationSource.contains("createOrUpdateCandidateRouteVersion"),
                "Word 重建已有路线必须创建或更新唯一候选路线版本，等待发布后再切 active。");
        assertTrue(generationSource.contains("STATUS_DRAFT"),
                "候选路线版本必须使用 DRAFT 状态，不能作为生产 active 使用。");
    }

    @Test
    void dccProjectGovernanceStatusEndpoint_isMesOwnedAndProjectNameBased() throws Exception {
        Path controller = Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/dccprojectgovernance/"
                        + "MesProDccProjectGovernanceController.java");
        Path service = Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/dccprojectgovernance/"
                        + "MesProDccProjectGovernanceServiceImpl.java");

        assertTrue(Files.exists(controller), "MES 必须提供 DCC 项目治理只读状态接口。");
        assertTrue(Files.exists(service), "MES 必须提供项目名称聚合状态服务。");
        String controllerSource = read(controller);
        String serviceSource = read(service);
        assertTrue(controllerSource.contains("/mes/pro/dcc-project-governance/status"),
                "治理状态接口路径必须由 MES 模块提供。");
        assertTrue(serviceSource.contains("projectName"),
                "治理状态聚合必须按 DCC 项目名称匹配。");
        assertTrue(serviceSource.contains("LOSS_REPORT")
                        && serviceSource.contains("PROCESS_INSPECTION")
                        && serviceSource.contains("PARAMETER_RECORD"),
                "治理状态必须覆盖损耗单、过程检验单、参数记录表。");
    }

    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
