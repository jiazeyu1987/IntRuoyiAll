package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackMaterialReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineDefectReasonOption;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterial;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProFrontlineFeedbackMaterialSubmissionValidatorTest {

    @Mock
    private MesFrontlineLossReasonValidator lossReasonValidator;

    private MesProFrontlineFeedbackMaterialSubmissionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new MesProFrontlineFeedbackMaterialSubmissionValidator(lossReasonValidator);
    }

    @Test
    void validate_returnsDirectMinimumProgressAndPreservesEveryMaterialQuantity() {
        List<MesFrontlineProcessMaterial> frozenMaterials = frozenMaterials();
        List<MesFrontlineDefectReasonOption> reasons = List.of(
                new MesFrontlineDefectReasonOption(8301L, "LOSS", "LOSS-001", "正常损耗"));
        MesProFrontlineFeedbackMaterialReqVO spring = material(501L, "5", "1");
        spring.setLossDetails(List.of(new MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO()
                .setReasonId(8301L).setReasonCode("CLIENT-CODE").setReasonName("客户端名称")
                .setQuantity(BigDecimal.ONE)));
        when(lossReasonValidator.requireSnapshotLossReasons(eq(reasons), any(), eq(BigDecimal.ONE)))
                .thenReturn(List.of(new MesFrontlineLossReasonSnapshot(8301L, "LOSS-001", "正常损耗")));

        MesProFrontlineFeedbackMaterialSubmission result = validator.validate(
                frozenMaterials, reasons, List.of(spring, material(502L, "3", "0")));

        assertEquals(new BigDecimal("3"), result.progressQuantity());
        assertEquals(BigDecimal.ONE, result.totalLossQuantity());
        assertEquals(List.of(501L, 502L), result.materials().stream()
                .map(MesProFrontlineFeedbackMaterialSubmission.Material::materialId).toList());
        assertEquals(new BigDecimal("5"), result.materials().get(0).outputQuantity());
        assertEquals(new BigDecimal("3"), result.materials().get(1).outputQuantity());
        assertEquals("LOSS-001", result.materials().get(0).lossDetails().get(0).getReasonCode());
        assertEquals("正常损耗", result.materials().get(0).lossDetails().get(0).getReasonName());
    }

    @Test
    void validate_rejectsMissingFrozenMaterialAsWholeSubmission() {
        ServiceException error = assertThrows(ServiceException.class, () -> validator.validate(
                frozenMaterials(), List.of(), List.of(material(501L, "5", "0"))));

        assertTrue(error.getMessage().contains("物料集合与冻结工序不一致"));
    }

    @Test
    void validate_acceptsExplicitZeroAndReturnsZeroProgress() {
        MesProFrontlineFeedbackMaterialSubmission result = validator.validate(
                frozenMaterials(), List.of(), List.of(
                        material(501L, "5", "0"),
                        material(502L, "0", "0")));

        assertEquals(BigDecimal.ZERO, result.progressQuantity());
    }

    private static List<MesFrontlineProcessMaterial> frozenMaterials() {
        return List.of(
                new MesFrontlineProcessMaterial(501L, "A001", "弹簧", null, BigDecimal.ONE),
                new MesFrontlineProcessMaterial(502L, "A002", "杠杆", null, BigDecimal.ONE));
    }

    private static MesProFrontlineFeedbackMaterialReqVO material(
            Long materialId, String outputQuantity, String lossQuantity) {
        return new MesProFrontlineFeedbackMaterialReqVO()
                .setMaterialId(materialId)
                .setOutputQuantity(new BigDecimal(outputQuantity))
                .setLossQuantity(new BigDecimal(lossQuantity))
                .setLossDetails(List.of())
                .setDeviceParameterReadings(List.of());
    }
}
