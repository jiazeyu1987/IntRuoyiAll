package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackMaterialReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDefectReasonMapper;
import cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesFrontlineLossReasonValidator;
import cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesFrontlineLossReasonValidatorImpl;
import cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackMaterialSubmissionValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class MesFrontlineRuntimeConfigProcessScopeTest {

    @Test
    void submitValidationMustUseCurrentRouteProcessForLossDeviceAndParameterRules() throws Exception {
        String submitService = readSource("service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitServiceImpl.java");
        String lossValidator = readSource("service/pro/feedback/frontline/MesFrontlineLossReasonValidator.java");
        String payload = readSource("controller/admin/pro/feedback/vo/frontline/MesProFrontlineFeedbackPayloadReqVO.java");
        String splitter = readSource("service/pro/feedback/frontline/MesProFrontlineFeedbackPayloadSplitter.java");
        String materialValidator = readSource("service/pro/feedback/frontline/MesProFrontlineFeedbackMaterialSubmissionValidator.java");

        assertTrue(payload.contains("lossDetails"), "submit payload must carry all loss detail ids and quantities");
        assertTrue(payload.contains("selectedDevices"), "submit payload must carry all selected device snapshots");
        assertTrue(payload.contains("deviceParameterReadings"), "submit payload must carry selected device parameter readings");
        assertTrue(lossValidator.contains("requireSnapshotLossReasons"),
                "loss validator must validate all loss details from the maximized runtime snapshot");
        int materialValidation = submitService.indexOf("materialSubmissionValidator.validate(");
        int processValidation = submitService.indexOf("lossReasonSnapshot = validateEmptyMaterialSubmission(");
        int signatureWrite = submitService.indexOf("signatureService.recordProductionSubmitSignature(");
        assertTrue(materialValidation >= 0 && processValidation >= 0 && signatureWrite > materialValidation
                        && signatureWrite > processValidation && materialValidator.contains("detailTotal.compareTo(lossQuantity) != 0")
                        && submitService.contains("materialSubmissionValidator.validateProcessPayload("),
                "both process/material loss totals must use frozen-snapshot validation before signature/business writes");
        assertTrue(submitService.contains("validateDeviceSelections")
                        && submitService.contains("validateAndApplyParameterReading")
                        && submitService.contains("unknown device parameter")
                        && submitService.contains("duplicate device parameter"),
                "submit service must validate all selected devices and parameters from the runtime snapshot");
        assertTrue(splitter.contains("hasActualLoss") && splitter.contains("zeroLossConfirmed")
                        && splitter.contains("lossDecision"),
                "signed production event payload must freeze explicit loss facts");
    }

    @Test
    void unequalProcessLossTotalIsRejectedBeforeFrozenReasonResolution() {
        var reasons = mock(MesFrontlineLossReasonValidator.class);
        var validator = new MesProFrontlineFeedbackMaterialSubmissionValidator(reasons);
        var error = assertThrows(ServiceException.class, () -> validator.validateProcessPayload(
                payload(new BigDecimal("2")), List.of()));
        assertTrue(error.getMessage().contains("损耗数量必须等于各损耗原因数量之和"));
        verifyNoInteractions(reasons);
    }

    @Test
    void unequalMaterialLossTotalIsRejectedBeforeFrozenReasonResolution() {
        var reasons = mock(MesFrontlineLossReasonValidator.class);
        var validator = new MesProFrontlineFeedbackMaterialSubmissionValidator(reasons);
        var material = new MesProFrontlineFeedbackMaterialReqVO().setMaterialId(501L)
                .setOutputQuantity(BigDecimal.TEN).setLossQuantity(new BigDecimal("2"))
                .setLossDetails(payload(BigDecimal.ONE).getLossDetails());
        var error = assertThrows(ServiceException.class, () -> validator.validate(
                List.of(frozenMaterial()), List.of(), List.of(material)));
        assertTrue(error.getMessage().contains("损耗数量必须等于各损耗原因数量之和"));
        verifyNoInteractions(reasons);
    }

    @Test
    void equalProcessAndMaterialLossTotalsUseRealFrozenReasonIdentity() {
        var mapper = mock(MesProcessPoolDefectReasonMapper.class);
        var validator = new MesProFrontlineFeedbackMaterialSubmissionValidator(new MesFrontlineLossReasonValidatorImpl(mapper));
        var reasons = List.of(new MesFrontlineDefectReasonOption(8301L, "LOSS", "LOSS-001", "冻结损耗原因"));
        var payload = payload(BigDecimal.ONE);
        assertEquals("LOSS-001", validator.validateProcessPayload(payload, reasons).reasonCode());
        var material = new MesProFrontlineFeedbackMaterialReqVO().setMaterialId(501L)
                .setOutputQuantity(BigDecimal.TEN).setLossQuantity(BigDecimal.ONE).setLossDetails(payload.getLossDetails());
        var result = validator.validate(List.of(frozenMaterial()), reasons, List.of(material));
        assertEquals(BigDecimal.ONE, result.totalLossQuantity());
        assertEquals(BigDecimal.TEN, result.progressQuantity());
        assertEquals("冻结损耗原因", result.materials().get(0).lossDetails().get(0).getReasonName());
        verifyNoInteractions(mapper);
    }

    private static MesProFrontlineFeedbackPayloadReqVO payload(BigDecimal totalLoss) {
        return new MesProFrontlineFeedbackPayloadReqVO().setOutputQuantity(BigDecimal.TEN).setLossQuantity(totalLoss)
                .setLossDetails(List.of(new MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO()
                        .setReasonId(8301L).setQuantity(BigDecimal.ONE)));
    }

    private static MesFrontlineProcessMaterial frozenMaterial() {
        return new MesFrontlineProcessMaterial(501L, "OUTPUT-501", "冻结输出物料", null, BigDecimal.ONE);
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
