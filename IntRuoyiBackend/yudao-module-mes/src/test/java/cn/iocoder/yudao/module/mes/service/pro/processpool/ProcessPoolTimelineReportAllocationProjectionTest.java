package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelinePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackMaterialDO;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessPoolTimelineReportAllocationProjectionTest {

    @Test
    void shouldBatchProjectCurrentAllocationsAndReleasedState() {
        ProcessPoolTimelineEventReadDO event = ProcessPoolTimelineTestSupport.event(
                1001L, "2026-07-30T08:30:00", 2001L, 6001L, 9001L, "PRODUCTION", 30001L)
                .setReportAllocatedQuantity(new BigDecimal("150"))
                .setReportUnallocatedQuantity(new BigDecimal("-140"));
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
                        allocation(90001L, 8101L, "WO-A", "100", "60", true, true),
                        allocation(90002L, 8102L, "WO-C", "50", "0", false, false));
            }

            @Override
            public List<MesProFeedbackMaterialDO> selectFeedbackMaterialsByFeedbackIds(List<Long> feedbackIds) {
                return List.of();
            }
        };

        PageResult<ProcessPoolTimelineEventRespVO> page = new ProcessPoolTimelineServiceImpl(mapper)
                .getTimelinePage(ProcessPoolTimelineTestSupport.pageReq());

        ProcessPoolTimelineEventRespVO actual = page.getList().get(0);
        assertEquals(2, actual.getReportAllocations().size());
        assertEquals("WO-A", actual.getReportAllocations().get(0).getWorkOrderCode());
        assertTrue(actual.getReportAllocations().get(0).getReleased());
        assertEquals(0, new BigDecimal("60").compareTo(
                actual.getReportAllocations().get(0).getOverageQuantity()));
        assertTrue(actual.getReportAllocations().get(0).getNeedsAdjustment());
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
        assertTrue(xml.contains("reqVO.requirePositiveOutputQuantity == true"));
        assertTrue(xml.contains("pool_event.report_output_quantity"));
        assertTrue(xml.contains("allocation.lifecycle_status = 'CURRENT'"));
        assertTrue(xml.contains("release_transaction.release_status = 'RELEASED'"));
        assertTrue(xml.contains("selectReportAllocationsByEventIds"));
        assertTrue(xml.contains("planned_quantity_snapshot"));
        assertTrue(xml.contains("total_allocated_quantity"));
        assertTrue(xml.contains("AS overageQuantity"));
    }

    @Test
    void shouldReadLegacySingleObjectSelectedDevicesPayload() {
        ProcessPoolTimelineEventReadDO event = ProcessPoolTimelineTestSupport.event(
                        1002L, "2026-07-30T08:35:00", 2001L, 6001L, 9001L, "PRODUCTION", 30001L)
                .setOriginalPayloadJson("""
                        {
                          "outputQuantity": 10,
                          "lossQuantity": 0,
                          "selectedDevices": {
                            "deviceId": 9001,
                            "deviceCode": "B09393",
                            "deviceName": "超声波清洗机"
                          }
                        }
                        """);

        PageResult<ProcessPoolTimelineEventRespVO> page = ProcessPoolTimelineTestSupport
                .service(ProcessPoolTimelineTestSupport.mapper(event))
                .getTimelinePage(ProcessPoolTimelineTestSupport.pageReq());

        ProcessPoolTimelineEventRespVO actual = page.getList().get(0);
        assertEquals(1, actual.getSelectedDevices().size());
        assertEquals(9001L, actual.getSelectedDevices().get(0).getDeviceId());
        assertEquals("B09393", actual.getSelectedDevices().get(0).getDeviceCode());
        assertEquals("超声波清洗机", actual.getSelectedDevices().get(0).getDeviceName());
    }

    @Test
    void shouldMergeFormalFeedbackMaterialNameAndLegacySingleObjectMaterialDeviceSnapshot() {
        ProcessPoolTimelineEventReadDO event = ProcessPoolTimelineTestSupport.event(
                        1003L, "2026-07-30T08:40:00", 2001L, 6001L, 9001L, "PRODUCTION", 30001L)
                .setSourceFeedbackId(7001L)
                .setOriginalPayloadJson("""
                        {
                          "materialDetails": [
                            {
                              "materialId": 81001,
                              "outputQuantity": 1111,
                              "lossQuantity": 1
                            }
                          ]
                        }
                        """);
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
                return List.of();
            }

            @Override
            public List<MesProFeedbackMaterialDO> selectFeedbackMaterialsByFeedbackIds(List<Long> feedbackIds) {
                assertEquals(List.of(7001L), feedbackIds);
                return List.of(new MesProFeedbackMaterialDO()
                        .setFeedbackId(7001L)
                        .setMaterialId(81001L)
                        .setMaterialCode("MAT-81001")
                        .setMaterialName("弹簧")
                        .setOutputQuantity(new BigDecimal("1111"))
                        .setLossQuantity(new BigDecimal("1"))
                        .setSelectedDeviceJson("""
                                {"deviceId":980021,"deviceCode":"B09031","deviceName":"超声波清洗机"}
                                """));
            }
        };

        PageResult<ProcessPoolTimelineEventRespVO> page = new ProcessPoolTimelineServiceImpl(mapper)
                .getTimelinePage(ProcessPoolTimelineTestSupport.pageReq());

        ProcessPoolTimelineEventRespVO.MaterialDetailRespVO material = page.getList().get(0).getMaterialDetails().get(0);
        assertEquals("弹簧", material.getMaterialName());
        assertEquals("MAT-81001", material.getMaterialCode());
        assertEquals(1, material.getSelectedDevices().size());
        assertEquals("B09031", material.getSelectedDevices().get(0).getDeviceCode());
        assertEquals("超声波清洗机", material.getSelectedDevices().get(0).getDeviceName());
    }

    @Test
    void shouldFailWhenFormalOrderProcessTargetIsMissing() {
        ProcessPoolTimelineEventReadDO event = ProcessPoolTimelineTestSupport.event(
                1001L, "2026-07-30T08:30:00", 2001L, 6001L, 9001L, "PRODUCTION", 30001L)
                .setReportAllocatedQuantity(new BigDecimal("150"))
                .setReportUnallocatedQuantity(new BigDecimal("-140"));
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
                return List.of(allocation(90001L, 8101L, "WO-A", "100", null, null, false));
            }

            @Override
            public List<MesProFeedbackMaterialDO> selectFeedbackMaterialsByFeedbackIds(List<Long> feedbackIds) {
                return List.of();
            }
        };

        assertThrows(IllegalStateException.class, () -> new ProcessPoolTimelineServiceImpl(mapper)
                .getTimelinePage(ProcessPoolTimelineTestSupport.pageReq()));
    }

    private static ProcessPoolTimelineReportAllocationReadDO allocation(
            Long allocationId, Long activeOrderId, String workOrderCode, String quantity,
            String overageQuantity, Boolean needsAdjustment, boolean released) {
        return new ProcessPoolTimelineReportAllocationReadDO()
                .setEventId(1001L)
                .setAllocationId(allocationId)
                .setActiveOrderId(activeOrderId)
                .setWorkOrderId(activeOrderId + 1000)
                .setWorkOrderCode(workOrderCode)
                .setAllocatedQuantity(new BigDecimal(quantity))
                .setOverageQuantity(overageQuantity == null ? null : new BigDecimal(overageQuantity))
                .setNeedsAdjustment(needsAdjustment)
                .setReleased(released);
    }
}
