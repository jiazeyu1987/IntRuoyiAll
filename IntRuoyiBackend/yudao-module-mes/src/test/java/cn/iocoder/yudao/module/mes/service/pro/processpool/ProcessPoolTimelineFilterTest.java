package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelinePageReqVO;
import org.junit.jupiter.api.Test;

import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.event;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.mapper;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.pageReq;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.service;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessPoolTimelineFilterTest {

    @Test
    void shouldFilterByEmployeeProcessDeviceTemplateAndWorkOrder() {
        ProcessPoolTimelinePageReqVO reqVO = pageReq();
        reqVO.setEmployeeUserId(2001L);
        reqVO.setProcessId(6001L);
        reqVO.setDeviceId(9001L);
        reqVO.setTemplateType("PRODUCTION");
        reqVO.setWorkOrderId(30001L);

        var page = service(mapper(
                event(1001L, "2026-07-30T08:30:00", 2001L, 6001L, 9001L, "PRODUCTION", 30001L),
                event(1002L, "2026-07-30T08:31:00", 2002L, 6001L, 9001L, "PRODUCTION", 30001L),
                event(1003L, "2026-07-30T08:32:00", 2001L, 6002L, 9001L, "PRODUCTION", 30001L),
                event(1004L, "2026-07-30T08:33:00", 2001L, 6001L, 9002L, "PRODUCTION", 30001L),
                event(1005L, "2026-07-30T08:34:00", 2001L, 6001L, 9001L, "PQC_SIMPLE", 30001L),
                event(1006L, "2026-07-30T08:35:00", 2001L, 6001L, 9001L, "PRODUCTION", 30002L)
        )).getTimelinePage(reqVO);

        assertEquals(1L, page.getTotal());
        assertEquals(1001L, page.getList().get(0).getId());
    }
}
