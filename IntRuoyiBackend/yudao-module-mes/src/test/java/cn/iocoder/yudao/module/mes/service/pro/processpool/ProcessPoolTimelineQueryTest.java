package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelinePageReqVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.event;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.mapper;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.pageReq;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.service;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessPoolTimelineQueryTest {

    @Test
    void shouldReturnDailyEventsOrderedByServerSubmittedAt() {
        ProcessPoolTimelineTestSupport.InMemoryTimelineReadMapper mapper = mapper(
                event(1002L, "2026-07-30T09:10:00", 2001L, 6001L, 9001L, "PRODUCTION_SIMPLIFIED", 30001L),
                event(1001L, "2026-07-30T08:30:00", 2002L, 6002L, 9002L, "PQC_SIMPLIFIED", 30002L));
        ProcessPoolTimelinePageReqVO reqVO = pageReq();

        PageResult<ProcessPoolTimelineEventRespVO> page = service(mapper).getTimelinePage(reqVO);

        assertEquals(2L, page.getTotal());
        assertEquals(1001L, page.getList().get(0).getId());
        assertEquals(LocalDateTime.parse("2026-07-30T08:30:00"), page.getList().get(0).getSubmittedAt());
        assertEquals("王鑫", page.getList().get(0).getActualEmployeeUserName());
        assertEquals("王鑫", page.getList().get(0).getSignatureEmployeeUserName());
        assertEquals("device-account-A", page.getList().get(0).getLoginUserName());
        assertEquals("PQC_SIMPLIFIED", page.getList().get(0).getTemplateType());
        assertEquals("WO-30002", page.getList().get(0).getWorkOrderCode());
    }
}
