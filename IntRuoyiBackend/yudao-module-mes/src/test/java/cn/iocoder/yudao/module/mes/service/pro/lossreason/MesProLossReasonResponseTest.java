package cn.iocoder.yudao.module.mes.service.pro.lossreason;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.MesProcessPoolTeamLeaderController;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderDetailRespVO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetail;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MesProLossReasonResponseTest {
    @Test void activeOrderResponseIncludesEachSubmittedLossReasonAndExactIdentity() {
        var submission = new MesTeamLeaderActiveOrderDetail.SubmissionDetail().setLossDetails(List.of(
                new MesTeamLeaderActiveOrderDetail.LossDetail().setReasonId(2000000000000000001L)
                        .setReasonCode("SHYY-001").setReasonName("调机损耗").setQuantity(new BigDecimal("2"))));
        MesTeamLeaderActiveOrderDetailRespVO.SubmissionDetail response = ReflectionTestUtils.invokeMethod(
                MesProcessPoolTeamLeaderController.class, "toActiveOrderSubmissionDetailRespVO", submission);
        assertEquals(1, response.getLossDetails().size());
        assertEquals(2000000000000000001L, response.getLossDetails().get(0).getReasonId());
        assertEquals("调机损耗", response.getLossDetails().get(0).getReasonName());
        assertEquals(new BigDecimal("2"), response.getLossDetails().get(0).getQuantity());
    }
}
