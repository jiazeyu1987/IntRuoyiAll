package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelinePageReqVO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolTimelineReadMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.ProcessPoolTimelineEventReadDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.ProcessPoolTimelineReportAllocationReadDO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

final class ProcessPoolTimelineTestSupport {

    static final LocalDate TARGET_DATE = LocalDate.of(2026, 7, 30);

    private ProcessPoolTimelineTestSupport() {
    }

    static ProcessPoolTimelinePageReqVO pageReq() {
        ProcessPoolTimelinePageReqVO reqVO = new ProcessPoolTimelinePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setSubmitDate(TARGET_DATE);
        return reqVO;
    }

    static InMemoryTimelineReadMapper mapper(ProcessPoolTimelineEventReadDO... events) {
        return new InMemoryTimelineReadMapper(List.of(events));
    }

    static ProcessPoolTimelineServiceImpl service(InMemoryTimelineReadMapper mapper) {
        return new ProcessPoolTimelineServiceImpl(mapper);
    }

    static ProcessPoolTimelineEventReadDO event(Long id, String submittedAt, Long employeeId,
                                                Long processId, Long deviceId, String templateType,
                                                Long workOrderId) {
        return new ProcessPoolTimelineEventReadDO()
                .setId(id)
                .setProcessPoolId(10L)
                .setSubmittedAt(LocalDateTime.parse(submittedAt))
                .setLoginUserId(100L)
                .setLoginUserName("device-account-A")
                .setActualEmployeeUserId(employeeId)
                .setActualEmployeeUserName(employeeId == 2001L ? "张可莹" : "王鑫")
                .setSignatureEmployeeUserId(employeeId)
                .setSignatureEmployeeUserName(employeeId == 2001L ? "张可莹" : "王鑫")
                .setElectronicSignatureId(8000L + id)
                .setDeviceId(deviceId)
                .setDeviceCode("EQ-" + deviceId)
                .setDeviceName("灌装设备-" + deviceId)
                .setWorkstationId(3001L)
                .setWorkstationCode("WS-01")
                .setWorkstationName("一线工作站")
                .setRouteId(4001L)
                .setRouteCode("ROUTE-A")
                .setRouteProcessId(5000L + processId)
                .setProcessId(processId)
                .setProcessCode("P-" + processId)
                .setProcessName(processId == 6001L ? "粗洗" : "精洗")
                .setTemplateType(templateType)
                .setTemplateTypeName("PRODUCTION_SIMPLIFIED".equals(templateType) ? "生产简化模板" : "PQC 简化模板")
                .setWorkOrderId(workOrderId)
                .setWorkOrderCode("WO-" + workOrderId)
                .setWorkOrderName("生产工单-" + workOrderId)
                .setSourceFeedbackId(7000L + id)
                .setSourceRecordbookEntryId(7100L + id)
                .setSourceRecordbookEventId(7200L + id)
                .setSubmittedSummary("产出 10 / 损耗 1 / 设备参数已记录")
                .setOriginalPayloadJson("{\"outputQuantity\":10,\"lossQuantity\":1}")
                .setPqcResult("SUCCESS")
                .setPqcSummary("PQC 检测成功")
                .setFifoAllocationStatus("PARTIAL")
                .setFifoAllocationSummary("已分配 6，待分配 4")
                .setAuditCopyStatus("PENDING")
                .setAuditCopySummary("审核副本待生成")
                .setSubmissionReviewStatus("REJECTED")
                .setSubmissionReviewRemark("压力填写不正确，已要求修正")
                .setSubmissionReviewLeaderUserId(3001L)
                .setSubmissionReviewLeaderUserName("生产组长")
                .setSubmissionReviewedAt(LocalDateTime.parse("2026-07-30T09:30:00"))
                .setModificationHistorySummary("原始记录暂无修改");
    }

    static final class InMemoryTimelineReadMapper implements MesProProcessPoolTimelineReadMapper {

        private final List<ProcessPoolTimelineEventReadDO> events;
        private ProcessPoolTimelinePageReqVO lastPageQuery;
        private int countQueryCalls;
        private int pageQueryCalls;
        private int detailQueryCalls;

        private InMemoryTimelineReadMapper(List<ProcessPoolTimelineEventReadDO> events) {
            this.events = new ArrayList<>(events);
        }

        ProcessPoolTimelinePageReqVO getLastPageQuery() {
            return lastPageQuery;
        }

        int getCountQueryCalls() {
            return countQueryCalls;
        }

        int getPageQueryCalls() {
            return pageQueryCalls;
        }

        int getDetailQueryCalls() {
            return detailQueryCalls;
        }

        @Override
        public Long selectTimelineCount(ProcessPoolTimelinePageReqVO reqVO) {
            countQueryCalls++;
            lastPageQuery = reqVO;
            return (long) filter(reqVO).count();
        }

        @Override
        public List<ProcessPoolTimelineEventReadDO> selectTimelinePage(ProcessPoolTimelinePageReqVO reqVO) {
            pageQueryCalls++;
            lastPageQuery = reqVO;
            int pageNo = reqVO.getPageNo() == null ? 1 : reqVO.getPageNo();
            int pageSize = reqVO.getPageSize() == null ? 10 : reqVO.getPageSize();
            long offset = (long) (pageNo - 1) * pageSize;
            return filter(reqVO)
                    .sorted(Comparator.comparing(ProcessPoolTimelineEventReadDO::getSubmittedAt)
                            .thenComparing(ProcessPoolTimelineEventReadDO::getId))
                    .skip(offset)
                    .limit(pageSize)
                    .toList();
        }

        @Override
        public ProcessPoolTimelineEventReadDO selectTimelineDetailById(Long id) {
            detailQueryCalls++;
            return events.stream()
                    .filter(event -> Objects.equals(event.getId(), id))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<ProcessPoolTimelineReportAllocationReadDO> selectReportAllocationsByEventIds(List<Long> eventIds) {
            return List.of();
        }

        private Stream<ProcessPoolTimelineEventReadDO> filter(ProcessPoolTimelinePageReqVO reqVO) {
            return events.stream()
                    .filter(event -> reqVO.getSubmittedAtStart() == null
                            || !event.getSubmittedAt().isBefore(reqVO.getSubmittedAtStart()))
                    .filter(event -> reqVO.getSubmittedAtEnd() == null
                            || event.getSubmittedAt().isBefore(reqVO.getSubmittedAtEnd()))
                    .filter(event -> reqVO.getEmployeeUserId() == null
                            || Objects.equals(event.getActualEmployeeUserId(), reqVO.getEmployeeUserId()))
                    .filter(event -> reqVO.getEmployeeUserIds() == null || reqVO.getEmployeeUserIds().isEmpty()
                            || reqVO.getEmployeeUserIds().contains(event.getActualEmployeeUserId()))
                    .filter(event -> reqVO.getProcessId() == null
                            || Objects.equals(event.getProcessId(), reqVO.getProcessId()))
                    .filter(event -> reqVO.getDeviceId() == null
                            || Objects.equals(event.getDeviceId(), reqVO.getDeviceId()))
                    .filter(event -> reqVO.getTemplateType() == null
                            || Objects.equals(event.getTemplateType(), reqVO.getTemplateType()))
                    .filter(event -> reqVO.getWorkOrderId() == null
                            || Objects.equals(event.getWorkOrderId(), reqVO.getWorkOrderId()))
                    .filter(event -> reqVO.getWorkOrderCode() == null
                            || event.getWorkOrderCode().contains(reqVO.getWorkOrderCode()))
                    .filter(event -> reqVO.getProductId() == null
                            || Objects.equals(event.getProductId(), reqVO.getProductId()))
                    .filter(event -> reqVO.getProductKeyword() == null
                            || containsIgnoreCase(event.getProductCode(), reqVO.getProductKeyword())
                            || containsIgnoreCase(event.getProductName(), reqVO.getProductKeyword()))
                    .filter(event -> reqVO.getInspectionType() == null
                            || Objects.equals(event.getInspectionType(), reqVO.getInspectionType()))
                    .filter(event -> reqVO.getRoundNo() == null
                            || Objects.equals(event.getRoundNo(), reqVO.getRoundNo()))
                    .filter(event -> reqVO.getSubmissionReviewStatus() == null
                            || Objects.equals(event.getSubmissionReviewStatus(), reqVO.getSubmissionReviewStatus()));
        }

        private boolean containsIgnoreCase(String source, String keyword) {
            if (source == null || keyword == null) {
                return false;
            }
            return source.toLowerCase().contains(keyword.toLowerCase());
        }
    }
}
