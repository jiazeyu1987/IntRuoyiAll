package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineDetailRespVO;
import org.junit.jupiter.api.Test;

import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.event;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.mapper;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.service;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProcessPoolTimelineTraceabilityTest {

    @Test
    void shouldReturnReadonlyDetailWithRawPayloadAndTraceabilityState() {
        ProcessPoolTimelineDetailRespVO detail = service(mapper(
                event(1001L, "2026-07-30T08:30:00", 2001L, 6001L, 9001L, "PRODUCTION", 30001L)
        )).getTimelineDetail(1001L);

        assertEquals("{\"outputQuantity\":10,\"lossQuantity\":1}", detail.getOriginalPayloadJson());
        assertEquals("PQC 检测成功", detail.getPqcSummary());
        assertEquals("PARTIAL", detail.getFifoAllocationStatus());
        assertEquals("已分配 6，待分配 4", detail.getFifoAllocationSummary());
        assertEquals("PENDING", detail.getAuditCopyStatus());
        assertEquals("审核副本待生成", detail.getAuditCopySummary());
        assertEquals("原始记录暂无修改", detail.getModificationHistorySummary());
        assertFalse(detail.getReadonlyActions().getCanModifyOriginalRecord());
        assertFalse(detail.getReadonlyActions().getCanGenerateAuditCopy());
        assertFalse(detail.getReadonlyActions().getCanExecuteFifoAllocation());
    }

    @Test
    void shouldFailFastWhenEventDetailMissing() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service(mapper()).getTimelineDetail(404L));

        assertEquals("工序池提交事件不存在，eventId=404", ex.getMessage());
    }
}
