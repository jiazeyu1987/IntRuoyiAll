package cn.iocoder.yudao.module.mes.service.pro.processpool;

import org.junit.jupiter.api.Test;

import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.event;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.mapper;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.pageReq;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.service;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessPoolTimelineContentSummaryTest {

    @Test
    void shouldExposeProductionAndPqcSummaryForTimelineCards() {
        var event = event(1001L, "2026-07-30T08:30:00", 2001L, 6001L, 9001L, "PRODUCTION", 30001L)
                .setSubmittedSummary("上工序输入 12，产出 10，损耗 1，设备参数温度 50")
                .setPqcResult("FAIL")
                .setPqcSummary("PQC 检测失败：外观不合格");

        var card = service(mapper(event)).getTimelinePage(pageReq()).getList().get(0);

        assertEquals("上工序输入 12，产出 10，损耗 1，设备参数温度 50", card.getSubmittedSummary());
        assertEquals("FAIL", card.getPqcResult());
        assertEquals("PQC 检测失败：外观不合格", card.getPqcSummary());
    }
}
