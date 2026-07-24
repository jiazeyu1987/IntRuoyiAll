package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder;

import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderProcessRespVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MesProScheduleOrderRespVOContractTest {

    @Test
    void scheduleOrderResponse_shouldExposePlanningTimeFields() throws NoSuchFieldException {
        assertEquals(LocalDateTime.class, MesProScheduleOrderRespVO.class.getDeclaredField("latestStartTime").getType());
        assertEquals(LocalDateTime.class, MesProScheduleOrderRespVO.class.getDeclaredField("plannedStartTime").getType());
        assertEquals(LocalDateTime.class, MesProScheduleOrderRespVO.class.getDeclaredField("plannedEndTime").getType());
    }

    @Test
    void scheduleOrderResponse_shouldExposeSourceSnapshotForTraceability() throws NoSuchFieldException {
        assertEquals(String.class, MesProScheduleOrderRespVO.class.getDeclaredField("sourceSnapshotJson").getType());
    }

    @Test
    void scheduleOrderResponse_shouldExposeProgressExplainFields() throws NoSuchFieldException {
        assertEquals(java.math.BigDecimal.class,
                MesProScheduleOrderRespVO.class.getDeclaredField("effectiveCompletedQuantity").getType());
        assertEquals(java.math.BigDecimal.class,
                MesProScheduleOrderRespVO.class.getDeclaredField("pendingApprovalQuantity").getType());
        assertEquals(java.math.BigDecimal.class,
                MesProScheduleOrderRespVO.class.getDeclaredField("pendingInspectionQuantity").getType());
        assertEquals(java.math.BigDecimal.class,
                MesProScheduleOrderRespVO.class.getDeclaredField("overReportedQuantity").getType());
        assertEquals(Long.class,
                MesProScheduleOrderRespVO.class.getDeclaredField("currentRouteProcessId").getType());
    }

    @Test
    void scheduleOrderResponse_shouldExposeManualFinishFields() throws NoSuchFieldException {
        assertEquals(Boolean.class, MesProScheduleOrderRespVO.class.getDeclaredField("manualFinished").getType());
        assertEquals(LocalDateTime.class,
                MesProScheduleOrderRespVO.class.getDeclaredField("manualFinishedTime").getType());
        assertEquals(Long.class, MesProScheduleOrderRespVO.class.getDeclaredField("manualFinishedBy").getType());
        assertEquals(String.class,
                MesProScheduleOrderRespVO.class.getDeclaredField("manualFinishedReason").getType());
    }

    @Test
    void scheduleOrderResponse_shouldExposeProductionMaterialListFields() throws NoSuchFieldException {
        assertEquals(Integer.class,
                MesProScheduleOrderRespVO.class.getDeclaredField("productionMaterialListCount").getType());
        assertEquals(String.class,
                MesProScheduleOrderRespVO.class.getDeclaredField("productionMaterialListSummary").getType());
    }

    @Test
    void scheduleOrderProcessResponse_shouldExposeFeedbackHistoryFields() throws NoSuchFieldException {
        assertEquals(Integer.class, MesProScheduleOrderProcessRespVO.class.getDeclaredField("feedbackCount").getType());
        assertEquals(LocalDateTime.class,
                MesProScheduleOrderProcessRespVO.class.getDeclaredField("latestFeedbackTime").getType());
        assertEquals(List.class,
                MesProScheduleOrderProcessRespVO.class.getDeclaredField("feedbackHistoryList").getType());
        assertEquals(LocalDateTime.class,
                MesProScheduleOrderProcessRespVO.FeedbackHistoryRespVO.class.getDeclaredField("feedbackTime").getType());
        assertEquals(BigDecimal.class,
                MesProScheduleOrderProcessRespVO.FeedbackHistoryRespVO.class.getDeclaredField("feedbackQuantity").getType());
        assertEquals(String.class,
                MesProScheduleOrderProcessRespVO.FeedbackHistoryRespVO.class.getDeclaredField("feedbackUserNickname").getType());
        assertEquals(String.class,
                MesProScheduleOrderProcessRespVO.FeedbackHistoryRespVO.class.getDeclaredField("statusName").getType());
    }

}
