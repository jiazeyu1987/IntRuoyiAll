package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDefectReasonMapper;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineDefectReasonOption;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class MesFrontlineLossReasonSnapshotValidatorTest {

    @Test
    void otherLossUsesServerIdentityForProcessAndMaterialSubmission() {
        var mapper = mock(MesProcessPoolDefectReasonMapper.class);
        var reasonValidator = new MesFrontlineLossReasonValidatorImpl(mapper);
        var validator = new MesProFrontlineFeedbackMaterialSubmissionValidator(reasonValidator);
        var detail = new MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO()
                .setReasonId(-1L).setReasonCode("FORGED").setReasonName("伪造名称").setQuantity(BigDecimal.ONE);
        var reasons = List.of(MesFrontlineDefectReasonOption.OTHER_LOSS);
        var payload = new MesProFrontlineFeedbackPayloadReqVO().setOutputQuantity(BigDecimal.TEN)
                .setLossQuantity(BigDecimal.ONE).setLossDetails(List.of(detail));
        assertEquals("其他损耗", validator.validateProcessPayload(payload, reasons).reasonName());
        assertEquals("SYSTEM_OTHER_LOSS", payload.getLossDetails().get(0).getReasonCode());
        var material = new cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackMaterialReqVO()
                .setMaterialId(501L).setOutputQuantity(BigDecimal.TEN).setLossQuantity(BigDecimal.ONE)
                .setLossDetails(List.of(detail));
        var result = validator.validate(List.of(new cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterial(
                501L, "OUTPUT-501", "输出物料", null, BigDecimal.ONE)), reasons, List.of(material));
        assertEquals(-1L, result.materials().get(0).lossDetails().get(0).getReasonId());
        assertEquals("其他损耗", result.materials().get(0).lossDetails().get(0).getReasonName());
        assertEquals(BigDecimal.ONE, result.totalLossQuantity());
        assertEquals("其他损耗", reasonValidator.requireEnabledLossReason(1001L, -1L, BigDecimal.ONE).reasonName());
        verifyNoInteractions(mapper);
    }

    @Test
    void otherLossMustBePresentInSignedRuntimeSnapshot() {
        var validator = new MesFrontlineLossReasonValidatorImpl(mock(MesProcessPoolDefectReasonMapper.class));
        var detail = new MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO()
                .setReasonId(-1L).setQuantity(BigDecimal.ONE);
        assertThrows(ServiceException.class, () -> validator.requireSnapshotLossReasons(
                List.of(), List.of(detail), BigDecimal.ONE));
    }

    @Test
    void validatesLossReasonFromSnapshotWithoutDatabaseRead() {
        MesProcessPoolDefectReasonMapper mapper = mock(MesProcessPoolDefectReasonMapper.class);
        MesFrontlineLossReasonValidator validator = new MesFrontlineLossReasonValidatorImpl(mapper);
        MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO detail =
                new MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO()
                        .setReasonId(8301L).setQuantity(new BigDecimal("2.500"));

        List<MesFrontlineLossReasonSnapshot> result = validator.requireSnapshotLossReasons(
                List.of(new MesFrontlineDefectReasonOption(8301L, "LOSS", "LOSS-001", "正常损耗")),
                List.of(detail), new BigDecimal("2.500"));

        assertEquals("LOSS-001", result.get(0).reasonCode());
        verifyNoInteractions(mapper);
    }

    @Test
    void rejectsReasonThatWasAddedAfterSnapshot() {
        MesProcessPoolDefectReasonMapper mapper = mock(MesProcessPoolDefectReasonMapper.class);
        MesFrontlineLossReasonValidator validator = new MesFrontlineLossReasonValidatorImpl(mapper);
        MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO detail =
                new MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO()
                        .setReasonId(9999L).setQuantity(BigDecimal.ONE);

        assertThrows(ServiceException.class, () -> validator.requireSnapshotLossReasons(
                List.of(new MesFrontlineDefectReasonOption(8301L, "LOSS", "LOSS-001", "正常损耗")),
                List.of(detail), BigDecimal.ONE));
        verifyNoInteractions(mapper);
    }

}
