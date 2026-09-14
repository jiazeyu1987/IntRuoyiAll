package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelinePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackMaterialDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.event;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.mapper;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.pageReq;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineTestSupport.service;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessPoolTimelineQueryTest {

    @Test
    void shouldReturnDailyEventsOrderedByServerSubmittedAtDesc() {
        ProcessPoolTimelineTestSupport.InMemoryTimelineReadMapper mapper = mapper(
                event(1002L, "2026-07-30T09:10:00", 2001L, 6001L, 9001L, "PRODUCTION_SIMPLIFIED", 30001L),
                event(1001L, "2026-07-30T08:30:00", 2002L, 6002L, 9002L, "PQC_SIMPLIFIED", 30002L));
        ProcessPoolTimelinePageReqVO reqVO = pageReq();

        PageResult<ProcessPoolTimelineEventRespVO> page = service(mapper).getTimelinePage(reqVO);

        assertEquals(2L, page.getTotal());
        assertEquals(1002L, page.getList().get(0).getId());
        assertEquals(LocalDateTime.parse("2026-07-30T09:10:00"), page.getList().get(0).getSubmittedAt());
        assertEquals("张可莹", page.getList().get(0).getActualEmployeeUserName());
        assertEquals("张可莹", page.getList().get(0).getSignatureEmployeeUserName());
        assertEquals("device-account-A", page.getList().get(0).getLoginUserName());
        assertEquals("PRODUCTION_SIMPLIFIED", page.getList().get(0).getTemplateType());
        assertEquals("WO-30001", page.getList().get(0).getWorkOrderCode());
        assertEquals("{\"outputQuantity\":10,\"lossQuantity\":1}", page.getList().get(0).getOriginalPayloadJson());
        assertEquals("REJECTED", page.getList().get(0).getSubmissionReviewStatus());
        assertEquals("压力填写不正确，已要求修正", page.getList().get(0).getSubmissionReviewRemark());
        assertEquals(3001L, page.getList().get(0).getSubmissionReviewLeaderUserId());
        assertEquals("生产组长", page.getList().get(0).getSubmissionReviewLeaderUserName());
        assertEquals(LocalDateTime.parse("2026-07-30T09:30:00"),
                page.getList().get(0).getSubmissionReviewedAt());
    }

    @Test
    void shouldExposePqcProcessInspectionAggregationStatus() {
        ProcessPoolTimelineTestSupport.InMemoryTimelineReadMapper mapper = mapper(
                event(2001L, "2026-07-30T08:30:00", 2001L, 6001L, 9001L, "PQC_SIMPLIFIED", 30001L)
                        .setProcessInspectionAggregationStatus("AGGREGATED")
                        .setProcessInspectionReviewId(7003L)
                        .setProcessInspectionAggregatedAt(LocalDateTime.parse("2026-07-30T09:45:00")));

        PageResult<ProcessPoolTimelineEventRespVO> page = service(mapper).getTimelinePage(pageReq());

        assertEquals("AGGREGATED", page.getList().get(0).getProcessInspectionAggregationStatus());
        assertEquals(7003L, page.getList().get(0).getProcessInspectionReviewId());
        assertEquals(LocalDateTime.parse("2026-07-30T09:45:00"),
                page.getList().get(0).getProcessInspectionAggregatedAt());
    }

    @Test
    void shouldDisplayPqcQaProcessWhenRouteProcessIsMissing() {
        ProcessPoolTimelineTestSupport.InMemoryTimelineReadMapper mapper = mapper(
                event(3001L, "2026-07-30T10:30:00", 2001L, null, 9001L, "PQC_SIMPLIFIED", 30001L)
                        .setEventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                        .setProcessCode(null)
                        .setProcessName(null)
                        .setQaProcessId(6101L)
                        .setQaProcessCode("QA-P01")
                        .setQaProcessName("清洗"));

        PageResult<ProcessPoolTimelineEventRespVO> page = service(mapper).getTimelinePage(pageReq());

        assertEquals("QA-P01", page.getList().get(0).getProcessCode());
        assertEquals("清洗", page.getList().get(0).getProcessName());
    }

    @Test
    void shouldEnrichProductionMaterialNamesFromFormalFeedbackMaterials() {
        ProcessPoolTimelineTestSupport.InMemoryTimelineReadMapper mapper = mapper(
                event(4001L, "2026-07-30T10:30:00", 2001L, 6001L, 9001L,
                        "PRODUCTION_SIMPLIFIED", 30001L)
                        .setEventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                        .setSourceFeedbackId(74001L)
                        .setOriginalPayloadJson("""
                                {"outputQuantity":10,"lossQuantity":0,
                                 "materialDetails":[{"materialId":501,"outputQuantity":10,"lossQuantity":0}]}
                                """))
                .addFeedbackMaterial(new MesProFeedbackMaterialDO()
                        .setFeedbackId(74001L)
                        .setMaterialId(501L)
                        .setMaterialCode("A001")
                        .setMaterialName("弹簧")
                        .setOutputQuantity(new java.math.BigDecimal("10"))
                        .setLossQuantity(java.math.BigDecimal.ZERO));

        PageResult<ProcessPoolTimelineEventRespVO> page = service(mapper).getTimelinePage(pageReq());

        assertEquals("弹簧", page.getList().get(0).getMaterialDetails().get(0).getMaterialName());
        assertEquals("A001", page.getList().get(0).getMaterialDetails().get(0).getMaterialCode());
    }
}
