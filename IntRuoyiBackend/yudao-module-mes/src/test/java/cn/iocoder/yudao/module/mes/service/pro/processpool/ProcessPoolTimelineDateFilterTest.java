package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelinePageReqVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.event;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.mapper;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.pageReq;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.service;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProcessPoolTimelineDateFilterTest {

    @Test
    void shouldExpandSubmitDateToServerTimeWindow() {
        ProcessPoolTimelineTestSupport.InMemoryTimelineReadMapper mapper = mapper(
                event(1001L, "2026-07-30T08:30:00", 2001L, 6001L, 9001L, "PRODUCTION", 30001L));
        ProcessPoolTimelinePageReqVO reqVO = pageReq();

        service(mapper).getTimelinePage(reqVO);

        assertEquals(LocalDateTime.parse("2026-07-30T00:00:00"), mapper.getLastPageQuery().getSubmittedAtStart());
        assertEquals(LocalDateTime.parse("2026-07-31T00:00:00"), mapper.getLastPageQuery().getSubmittedAtEnd());
    }

    @Test
    void shouldFailFastWhenSubmitDateMissing() {
        ProcessPoolTimelinePageReqVO reqVO = pageReq();
        reqVO.setSubmitDate(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service(mapper()).getTimelinePage(reqVO));

        assertEquals("工序池时间轴查询必须提供提交日期", ex.getMessage());
    }

    @Test
    void shouldOnlyReturnTheSelectedDay() {
        ProcessPoolTimelineTestSupport.InMemoryTimelineReadMapper mapper = mapper(
                event(1001L, "2026-07-29T23:59:59", 2001L, 6001L, 9001L, "PRODUCTION", 30001L),
                event(1002L, "2026-07-30T00:00:00", 2001L, 6001L, 9001L, "PRODUCTION", 30001L),
                event(1003L, "2026-07-30T23:59:59", 2001L, 6001L, 9001L, "PRODUCTION", 30001L),
                event(1004L, "2026-07-31T00:00:00", 2001L, 6001L, 9001L, "PRODUCTION", 30001L));
        ProcessPoolTimelinePageReqVO reqVO = pageReq();
        reqVO.setSubmitDate(LocalDate.of(2026, 7, 30));

        var page = service(mapper).getTimelinePage(reqVO);

        assertEquals(2L, page.getTotal());
        assertEquals(1002L, page.getList().get(0).getId());
        assertEquals(1003L, page.getList().get(1).getId());
    }
}
