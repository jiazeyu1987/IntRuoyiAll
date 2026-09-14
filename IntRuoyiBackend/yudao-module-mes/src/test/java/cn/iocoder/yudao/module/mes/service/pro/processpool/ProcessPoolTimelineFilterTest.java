package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelinePageReqVO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.ProcessPoolTimelineEventReadDO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

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

    @Test
    void shouldFilterPqcLeaderSubmissionByProductTaskRoundReviewStatusAndKeepPageTotal() {
        ProcessPoolTimelinePageReqVO reqVO = pageReq();
        reqVO.setPageSize(1);
        setField(reqVO, "productKeyword", "PP-88");
        setField(reqVO, "inspectionType", "PATROL");
        setField(reqVO, "roundNo", 2);
        setField(reqVO, "submissionReviewStatus", "REJECTED");

        var timelineService = service(mapper(
                pqcSubmission(event(2001L, "2026-07-30T08:30:00", 2001L, 6001L, 9001L,
                        "PQC_SIMPLIFIED", 30001L), 91001L, "PP-88", "球囊扩张压力泵", "PATROL", 2, "REJECTED"),
                pqcSubmission(event(2002L, "2026-07-30T08:31:00", 2001L, 6001L, 9001L,
                        "PQC_SIMPLIFIED", 30001L), 91001L, "PP-88", "球囊扩张压力泵", "PATROL", 2, "REJECTED"),
                pqcSubmission(event(2003L, "2026-07-30T08:32:00", 2001L, 6001L, 9001L,
                        "PQC_SIMPLIFIED", 30002L), 91002L, "OTHER", "其它产品", "PATROL", 2, "REJECTED"),
                pqcSubmission(event(2004L, "2026-07-30T08:33:00", 2001L, 6001L, 9001L,
                        "PQC_SIMPLIFIED", 30001L), 91001L, "PP-88", "球囊扩张压力泵", "FIRST", 2, "REJECTED"),
                pqcSubmission(event(2005L, "2026-07-30T08:34:00", 2001L, 6001L, 9001L,
                        "PQC_SIMPLIFIED", 30001L), 91001L, "PP-88", "球囊扩张压力泵", "PATROL", 1, "REJECTED"),
                pqcSubmission(event(2006L, "2026-07-30T08:35:00", 2001L, 6001L, 9001L,
                        "PQC_SIMPLIFIED", 30001L), 91001L, "PP-88", "球囊扩张压力泵", "PATROL", 2, "APPROVED")
        ));

        var firstPage = timelineService.getTimelinePage(reqVO);
        assertEquals(2L, firstPage.getTotal());
        assertEquals(1, firstPage.getList().size());
        assertEquals(2001L, firstPage.getList().get(0).getId());

        reqVO.setPageNo(2);
        var secondPage = timelineService.getTimelinePage(reqVO);
        assertEquals(2L, secondPage.getTotal());
        assertEquals(1, secondPage.getList().size());
        assertEquals(2002L, secondPage.getList().get(0).getId());
    }

    @Test
    void shouldUseCountAndPageQueriesWithoutPerRowDetailLookupsForPqcPagination() {
        ProcessPoolTimelinePageReqVO reqVO = pageReq();
        reqVO.setPageSize(1);
        setField(reqVO, "productKeyword", "PP-88");
        setField(reqVO, "inspectionType", "PATROL");
        setField(reqVO, "roundNo", 2);
        setField(reqVO, "submissionReviewStatus", "REJECTED");

        ProcessPoolTimelineTestSupport.InMemoryTimelineReadMapper mapper = mapper(
                pqcSubmission(event(2101L, "2026-07-30T08:30:00", 2001L, 6001L, 9001L,
                        "PQC_SIMPLIFIED", 30001L), 91001L, "PP-88", "球囊扩张压力泵", "PATROL", 2, "REJECTED"),
                pqcSubmission(event(2102L, "2026-07-30T08:31:00", 2001L, 6001L, 9001L,
                        "PQC_SIMPLIFIED", 30001L), 91001L, "PP-88", "球囊扩张压力泵", "PATROL", 2, "REJECTED")
        );
        var timelineService = service(mapper);

        timelineService.getTimelinePage(reqVO);
        assertEquals(1, mapper.getCountQueryCalls(), "AC-D32 每页只能执行一次 count 查询。");
        assertEquals(1, mapper.getPageQueryCalls(), "AC-D32 每页只能执行一次 page 查询。");
        assertEquals(0, mapper.getDetailQueryCalls(), "AC-D32 分页不得为列表每行执行 detail 查询。");

        reqVO.setPageNo(2);
        timelineService.getTimelinePage(reqVO);
        assertEquals(2, mapper.getCountQueryCalls(), "AC-D32 翻页只能新增一次 count 查询。");
        assertEquals(2, mapper.getPageQueryCalls(), "AC-D32 翻页只能新增一次 page 查询。");
        assertEquals(0, mapper.getDetailQueryCalls(), "AC-D32 翻页仍不得执行逐行 detail 查询。");
    }

    @Test
    void shouldUseCountAndPageQueriesWithoutDetailLookupsForDailyCloseSubmissionSummary() {
        ProcessPoolTimelinePageReqVO reqVO = pageReq();
        reqVO.setPageSize(20);
        reqVO.setEmployeeUserIds(java.util.Set.of(2001L, 2002L));

        ProcessPoolTimelineTestSupport.InMemoryTimelineReadMapper mapper = mapper(
                event(2201L, "2026-07-30T08:30:00", 2001L, 6001L, 9001L, "PRODUCTION", 30001L),
                event(2202L, "2026-07-30T08:31:00", 2002L, 6001L, 9001L, "PRODUCTION", 30001L)
        );
        var timelineService = service(mapper);

        var page = timelineService.getTimelinePage(reqVO);

        assertEquals(2L, page.getTotal());
        assertEquals(2, page.getList().size());
        assertEquals(1, mapper.getCountQueryCalls(), "AC-D12/AC-D38 日结提交摘要只能执行一次 count 查询。");
        assertEquals(1, mapper.getPageQueryCalls(), "AC-D12/AC-D38 日结提交摘要只能执行一次 page 查询。");
        assertEquals(0, mapper.getDetailQueryCalls(), "AC-D12/AC-D38 日结摘要不得逐行读取提交详情。");
    }

    private static ProcessPoolTimelineEventReadDO pqcSubmission(ProcessPoolTimelineEventReadDO event,
                                                               Long productId,
                                                               String productCode,
                                                               String productName,
                                                               String inspectionType,
                                                               Integer roundNo,
                                                               String submissionReviewStatus) {
        setField(event, "productId", productId);
        setField(event, "productCode", productCode);
        setField(event, "productName", productName);
        setField(event, "inspectionType", inspectionType);
        setField(event, "roundNo", roundNo);
        event.setSubmissionReviewStatus(submissionReviewStatus);
        return event;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("AC-D32 缺少提交看板字段：" + fieldName, ex);
        }
    }
}
