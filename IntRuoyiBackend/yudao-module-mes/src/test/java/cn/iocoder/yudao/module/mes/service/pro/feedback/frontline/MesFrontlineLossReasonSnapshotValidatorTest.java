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
