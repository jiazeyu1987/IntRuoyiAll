package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTeamLeaderWorkbenchRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineDetailRespVO;
import org.junit.jupiter.api.Test;

import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.event;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.mapper;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.pageReq;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.service;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProcessPoolTeamLeaderWorkbenchServiceTest {

    @Test
    void shouldExposeVisibleEventsAndStatusSummaryForTeamLeaderWorkbench() {
        var production = event(1001L, "2026-07-30T08:30:00", 2001L, 6001L, 9001L,
                "PRODUCTION_SIMPLIFIED", 30001L)
                .setPqcResult(null)
                .setPqcSummary(null)
                .setFifoAllocationStatus("PENDING")
                .setAuditCopyStatus("PENDING")
                .setModificationHistorySummary("原始记录暂无修改");
        var pqcFailure = event(1002L, "2026-07-30T09:10:00", 2002L, 6002L, 9002L,
                "PQC_SIMPLIFIED", 30002L)
                .setPqcResult("FAILURE")
                .setPqcSummary("PQC 检测失败")
                .setFifoAllocationStatus("ALLOCATED")
                .setAuditCopyStatus("SUBMITTED")
                .setModificationHistorySummary("原始记录已修改 1 次");
        ProcessPoolTeamLeaderWorkbenchService workbenchService = new ProcessPoolTeamLeaderWorkbenchServiceImpl(
                service(mapper(production, pqcFailure)));

        ProcessPoolTeamLeaderWorkbenchRespVO workbench = workbenchService.getWorkbench(pageReq());

        assertEquals(2L, workbench.getTotal());
        assertEquals(2, workbench.getEvents().size());
        assertEquals(2, workbench.getSummary().getVisibleEventCount());
        assertEquals(1, workbench.getSummary().getPqcFailureCount());
        assertEquals(1, workbench.getSummary().getFifoPendingCount());
        assertEquals(1, workbench.getSummary().getFifoAllocatedCount());
        assertEquals(1, workbench.getSummary().getAuditCopyPendingCount());
        assertEquals(1, workbench.getSummary().getAuditCopySubmittedCount());
        assertEquals(1, workbench.getSummary().getModifiedRecordCount());
        assertEquals("WO-30001", workbench.getEvents().get(0).getWorkOrderCode());
        assertEquals("WO-30002", workbench.getEvents().get(1).getWorkOrderCode());
    }

    @Test
    void shouldExposeReadonlyDetailThroughTeamLeaderWorkbench() {
        ProcessPoolTeamLeaderWorkbenchService workbenchService = new ProcessPoolTeamLeaderWorkbenchServiceImpl(
                service(mapper(event(1001L, "2026-07-30T08:30:00", 2001L, 6001L, 9001L,
                        "PRODUCTION_SIMPLIFIED", 30001L))));

        ProcessPoolTimelineDetailRespVO detail = workbenchService.getDetail(1001L);

        assertEquals("{\"outputQuantity\":10,\"lossQuantity\":1}", detail.getOriginalPayloadJson());
        assertEquals("PQC 检测成功", detail.getPqcSummary());
        assertEquals("PARTIAL", detail.getFifoAllocationStatus());
        assertEquals("PENDING", detail.getAuditCopyStatus());
        assertEquals("原始记录暂无修改", detail.getModificationHistorySummary());
        assertFalse(detail.getReadonlyActions().getCanModifyOriginalRecord());
        assertFalse(detail.getReadonlyActions().getCanGenerateAuditCopy());
        assertFalse(detail.getReadonlyActions().getCanExecuteFifoAllocation());
    }
}
