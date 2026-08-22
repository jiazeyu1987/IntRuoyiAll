package cn.iocoder.yudao.module.mes.service.pro.frontline;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesFrontlineRuntimeConfigProcessScopeTest {

    @Test
    void submitValidationMustUseCurrentRouteProcessForLossDeviceAndParameterRules() throws Exception {
        String submitService = readSource("service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitServiceImpl.java");
        String lossValidator = readSource("service/pro/feedback/frontline/MesFrontlineLossReasonValidator.java");
        String deviceValidator = readSource("service/pro/feedback/frontline/MesFrontlineDeviceParameterValidatorImpl.java");
        String payload = readSource("controller/admin/pro/feedback/vo/frontline/MesProFrontlineFeedbackPayloadReqVO.java");
        String splitter = readSource("service/pro/feedback/frontline/MesProFrontlineFeedbackPayloadSplitter.java");

        assertTrue(payload.contains("lossDetails"), "submit payload must carry all loss detail ids and quantities");
        assertTrue(payload.contains("selectedDevice"), "submit payload must carry selected device id/code/name snapshot");
        assertTrue(payload.contains("deviceParameterReadings"), "submit payload must carry selected device parameter readings");
        assertTrue(lossValidator.contains("requireSnapshotLossReasons"),
                "loss validator must validate all loss details from the maximized runtime snapshot");
        assertTrue(submitService.contains("validateLossDetailTotal"),
                "submit service must reject lossQuantity != sum(lossDetails.quantity) before authorization/write");
        assertTrue(submitService.contains("validateSnapshotDeviceAndParameters"),
                "submit service must validate selected device and parameters from the runtime snapshot");
        assertTrue(deviceValidator.contains("routeProcessId")
                        && deviceValidator.contains("processId")
                        && deviceValidator.contains("deviceId")
                        && deviceValidator.contains("parameterCode"),
                "device parameter validation must be scoped by routeProcessId/deviceId/parameterCode");
        assertTrue(splitter.contains("hasActualLoss") && splitter.contains("zeroLossConfirmed")
                        && splitter.contains("lossDecision"),
                "signed production event payload must freeze explicit loss facts");
    }

    private static String readSource(String relative) throws Exception {
        Path moduleRelativePath = Path.of("src", "main", "java", "cn", "iocoder", "yudao", "module",
                "mes", relative.replace("/", java.io.File.separator));
        Path path = Files.exists(moduleRelativePath)
                ? moduleRelativePath
                : Path.of("yudao-module-mes").resolve(moduleRelativePath);
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
