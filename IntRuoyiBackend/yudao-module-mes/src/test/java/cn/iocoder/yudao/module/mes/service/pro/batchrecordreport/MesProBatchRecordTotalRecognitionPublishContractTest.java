package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordTotalRecognitionPublishContractTest {

    private static final Path ROOT = Path.of("..").toAbsolutePath().normalize();

    @Test
    void publishJsonEndpointAndServiceMustUseProjectCodeRouteBindingAndCandidateGovernance() throws Exception {
        String controller = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecordreport/MesProBatchRecordReportController.java");
        String service = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordReportServiceImpl.java");
        String serviceInterface = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordReportService.java");
        String deviceSync = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordRecognitionDeviceSyncService.java");

        assertContains(controller, "/total-recognition-json/publish");
        assertContains(controller, "BatchRecordTotalRecognitionPublishReqVO");
        assertContains(controller, "BatchRecordTotalRecognitionPublishRespVO");
        assertContains(controller, "publishTotalRecognitionJson");
        assertContains(controller, "@RequestBody");
        assertContains(controller, "form:parser:production-batch-record:publish");

        assertContains(serviceInterface, "MesProBatchRecordTotalRecognitionPublishResult publishTotalRecognitionJson");
        assertContains(service, "routeDccProjectBindingMapper.selectCurrentListByDccProjectCodeId");
        assertContains(service, "routeVersionWorkflowService.createCandidate");
        assertContains(service, "routeGenerationService.generateRouteOnlyForUploadedWord");
        assertContains(service, "recognitionDeviceSyncService.sync");
        assertContains(service, "updateProjectCodeTotalRecognitionJson");
        assertContains(service, "buildRouteParsedTablesFromTotalRecognitionJson");
        assertContains(service, "validatePublishProcessNames");

        assertContains(deviceSync, "schemaVersion");
        assertContains(deviceSync, "resolveProcessByName");
        assertContains(deviceSync, "buildDeviceGroupKey");
        assertContains(deviceSync, "buildParameterCode");
        assertContains(deviceSync, "productionProcessConfigs");
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(ROOT.resolve(relativePath));
    }

    private static void assertContains(String content, String expected) {
        assertTrue(content.contains(expected), "Expected content to include: " + expected);
    }
}
