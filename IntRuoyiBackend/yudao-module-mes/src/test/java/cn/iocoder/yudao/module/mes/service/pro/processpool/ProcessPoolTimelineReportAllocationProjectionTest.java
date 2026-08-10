package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelinePageReqVO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolTimelineReadMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.ProcessPoolTimelineEventReadDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.ProcessPoolTimelineReportAllocationReadDO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessPoolTimelineReportAllocationProjectionTest {

    @Test
    void shouldBatchProjectCurrentAllocationsAndReleasedState() {
        ProcessPoolTimelineEventReadDO event = ProcessPoolTimelineTestSupport.event(
                1001L, "2026-07-30T08:30:00", 2001L, 6001L, 9001L, "PRODUCTION", 30001L);
        MesProProcessPoolTimelineReadMapper mapper = new MesProProcessPoolTimelineReadMapper() {
            @Override
            public Long selectTimelineCount(ProcessPoolTimelinePageReqVO reqVO) {
                return 1L;
            }

            @Override
            public List<ProcessPoolTimelineEventReadDO> selectTimelinePage(ProcessPoolTimelinePageReqVO reqVO) {
                return List.of(event);
            }

            @Override
            public ProcessPoolTimelineEventReadDO selectTimelineDetailById(Long id) {
                return event;
            }

            @Override
            public List<ProcessPoolTimelineReportAllocationReadDO> selectReportAllocationsByEventIds(
                    List<Long> eventIds) {
                assertEquals(List.of(1001L), eventIds);
                return List.of(
                        allocation(90001L, 8101L, "WO-A", "100", true),
                        allocation(90002L, 8102L, "WO-C", "50", false));
            }
        };

        PageResult<ProcessPoolTimelineEventRespVO> page = new ProcessPoolTimelineServiceImpl(mapper)
                .getTimelinePage(ProcessPoolTimelineTestSupport.pageReq());

        ProcessPoolTimelineEventRespVO actual = page.getList().get(0);
        assertEquals(2, actual.getReportAllocations().size());
        assertEquals("WO-A", actual.getReportAllocations().get(0).getWorkOrderCode());
        assertTrue(actual.getReportAllocations().get(0).getReleased());
        assertEquals(0, new BigDecimal("150").compareTo(actual.getReportAllocatedQuantity()));
        assertEquals(0, new BigDecimal("-140").compareTo(actual.getReportUnallocatedQuantity()));
    }

    @Test
    void shouldUseEventGrainWorkbenchAndHistoryFiltersWithoutReviewStatus() throws Exception {
        String xml = Files.readString(Path.of(
                "src/main/resources/mapper/pro/processpool/MesProProcessPoolTimelineReadMapper.xml"),
                StandardCharsets.UTF_8);

        assertTrue(xml.contains("reqVO.allocationView == 'WORKBENCH'"));
        assertTrue(xml.contains("reqVO.allocationView == 'HISTORY'"));
        assertTrue(xml.contains("allocation.lifecycle_status = 'CURRENT'"));
        assertTrue(xml.contains("release_transaction.release_status = 'RELEASED'"));
        assertTrue(xml.contains("selectReportAllocationsByEventIds"));
    }

    private static ProcessPoolTimelineReportAllocationReadDO allocation(
            Long allocationId, Long activeOrderId, String workOrderCode, String quantity, boolean released) {
        return new ProcessPoolTimelineReportAllocationReadDO()
                .setEventId(1001L)
                .setAllocationId(allocationId)
                .setActiveOrderId(activeOrderId)
                .setWorkOrderId(activeOrderId + 1000)
                .setWorkOrderCode(workOrderCode)
                .setAllocatedQuantity(new BigDecimal(quantity))
                .setReleased(released);
    }
}
