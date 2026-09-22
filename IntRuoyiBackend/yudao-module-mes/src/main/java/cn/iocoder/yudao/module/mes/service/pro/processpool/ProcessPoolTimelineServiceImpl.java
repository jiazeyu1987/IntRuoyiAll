package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelinePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackMaterialDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolTimelineReadMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.ProcessPoolTimelineEventReadDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.ProcessPoolTimelineReportAllocationReadDO;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Validated
public class ProcessPoolTimelineServiceImpl implements ProcessPoolTimelineService {

    private static final String PAGE_REQUEST_REQUIRED_MESSAGE = "工序池时间轴查询参数不能为空";
    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<ProcessPoolTimelineEventRespVO.LossDetailRespVO>> LOSS_DETAIL_TYPE =
            new TypeReference<>() {
            };
    private static final TypeReference<List<ProcessPoolTimelineEventRespVO.MaterialDetailRespVO>> MATERIAL_DETAIL_TYPE =
            new TypeReference<>() {
            };
    private static final TypeReference<List<ProcessPoolTimelineEventRespVO.SelectedDeviceRespVO>> SELECTED_DEVICE_LIST_TYPE =
            new TypeReference<>() {
            };
    private static final TypeReference<List<ProcessPoolTimelineEventRespVO.DeviceParameterReadingRespVO>>
            DEVICE_PARAMETER_READING_TYPE = new TypeReference<>() {
            };

    private final MesProProcessPoolTimelineReadMapper timelineReadMapper;

    public ProcessPoolTimelineServiceImpl(MesProProcessPoolTimelineReadMapper timelineReadMapper) {
        this.timelineReadMapper = timelineReadMapper;
    }

    @Override
    public PageResult<ProcessPoolTimelineEventRespVO> getTimelinePage(ProcessPoolTimelinePageReqVO reqVO) {
        prepareSubmitDateWindow(reqVO);
        Long total = timelineReadMapper.selectTimelineCount(reqVO);
        if (total == null || total == 0L) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        List<ProcessPoolTimelineEventRespVO> list = timelineReadMapper.selectTimelinePage(reqVO).stream()
                .map(this::toEventRespVO)
                .toList();
        list = enrichPqcSubmissionGroupRows(reqVO, list);
        fillFeedbackMaterialDetails(list);
        fillReportAllocations(list);
        return new PageResult<>(list, total);
    }

    @Override
    public ProcessPoolTimelineDetailRespVO getTimelineDetail(Long eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("工序池提交事件编号不能为空");
        }
        ProcessPoolTimelineEventReadDO event = timelineReadMapper.selectTimelineDetailById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("工序池提交事件不存在，eventId=" + eventId);
        }
        ProcessPoolTimelineDetailRespVO detail = toDetailRespVO(event);
        fillFeedbackMaterialDetails(List.of(detail));
        fillReportAllocations(List.of(detail));
        return detail;
    }

    private void prepareSubmitDateWindow(ProcessPoolTimelinePageReqVO reqVO) {
        if (reqVO == null) {
            throw new IllegalArgumentException(PAGE_REQUEST_REQUIRED_MESSAGE);
        }
        if (reqVO.getSubmitDate() == null) {
            reqVO.setSubmittedAtStart(null).setSubmittedAtEnd(null);
            return;
        }
        reqVO.setSubmittedAtStart(reqVO.getSubmitDate().atStartOfDay())
                .setSubmittedAtEnd(reqVO.getSubmitDate().plusDays(1).atStartOfDay());
    }

    private ProcessPoolTimelineEventRespVO toEventRespVO(ProcessPoolTimelineEventReadDO event) {
        ProcessPoolTimelineEventRespVO respVO = new ProcessPoolTimelineEventRespVO();
        copyEventFields(event, respVO);
        return respVO;
    }

    private ProcessPoolTimelineDetailRespVO toDetailRespVO(ProcessPoolTimelineEventReadDO event) {
        ProcessPoolTimelineDetailRespVO respVO = new ProcessPoolTimelineDetailRespVO();
        copyEventFields(event, respVO);
        respVO.setReadonlyActions(new ProcessPoolTimelineDetailRespVO.ReadonlyActions()
                .setCanModifyOriginalRecord(false)
                .setCanGenerateAuditCopy(false)
                .setCanExecuteFifoAllocation(false));
        return respVO;
    }

    private void copyEventFields(ProcessPoolTimelineEventReadDO event, ProcessPoolTimelineEventRespVO respVO) {
        respVO.setId(event.getId())
                .setProcessPoolId(event.getProcessPoolId())
                .setSubmittedAt(event.getSubmittedAt())
                .setLoginUserId(event.getLoginUserId())
                .setLoginUserName(event.getLoginUserName())
                .setActualEmployeeUserId(event.getActualEmployeeUserId())
                .setActualEmployeeUserName(event.getActualEmployeeUserName())
                .setSignatureEmployeeUserId(event.getSignatureEmployeeUserId())
                .setSignatureEmployeeUserName(event.getSignatureEmployeeUserName())
                .setElectronicSignatureId(event.getElectronicSignatureId())
                .setDeviceId(event.getDeviceId())
                .setDeviceCode(event.getDeviceCode())
                .setDeviceName(event.getDeviceName())
                .setWorkstationId(event.getWorkstationId())
                .setWorkstationCode(event.getWorkstationCode())
                .setWorkstationName(event.getWorkstationName())
                .setRouteId(event.getRouteId())
                .setRouteCode(event.getRouteCode())
                .setRouteProcessId(event.getRouteProcessId())
                .setProcessId(event.getProcessId())
                .setProcessCode(resolveDisplayProcessCode(event))
                .setProcessName(resolveDisplayProcessName(event))
                .setTemplateType(event.getTemplateType())
                .setTemplateTypeName(event.getTemplateTypeName())
                .setWorkOrderId(event.getWorkOrderId())
                .setWorkOrderCode(event.getWorkOrderCode())
                .setWorkOrderName(event.getWorkOrderName())
                .setProductId(event.getProductId())
                .setProductCode(event.getProductCode())
                .setProductName(event.getProductName())
                .setActiveOrderId(event.getActiveOrderId())
                .setBatchExecutionId(event.getBatchExecutionId())
                .setReleased(Boolean.TRUE.equals(event.getReleased()))
                .setPqcTaskId(event.getPqcTaskId())
                .setInspectionType(event.getInspectionType())
                .setPqcBusinessDate(event.getPqcBusinessDate())
                .setPqcShiftCode(event.getPqcShiftCode())
                .setRoundNo(event.getRoundNo())
                .setPqcSubmissionGroupId(event.getPqcSubmissionGroupId())
                .setGroupedEventIds(event.getGroupedEventIds())
                .setGroupedOriginalPayloadJsons(event.getGroupedOriginalPayloadJsons())
                .setSourceFeedbackId(event.getSourceFeedbackId())
                .setSourceRecordbookEntryId(event.getSourceRecordbookEntryId())
                .setSourceRecordbookEventId(event.getSourceRecordbookEventId())
                .setOutputQuantity(event.getReportOutputQuantity())
                .setReportAllocatedQuantity(event.getReportAllocatedQuantity())
                .setReportUnallocatedQuantity(event.getReportUnallocatedQuantity())
                .setReportManagementStatus(event.getReportManagementStatus())
                .setReportReleaseStatus(event.getReportReleaseStatus())
                .setSubmittedSummary(event.getSubmittedSummary())
                .setOriginalPayloadJson(event.getOriginalPayloadJson())
                .setPqcResult(event.getPqcResult())
                .setPqcSummary(event.getPqcSummary())
                .setProcessInspectionAggregationStatus(event.getProcessInspectionAggregationStatus())
                .setProcessInspectionReviewId(event.getProcessInspectionReviewId())
                .setProcessInspectionAggregatedAt(event.getProcessInspectionAggregatedAt())
                .setFifoAllocationStatus(event.getFifoAllocationStatus())
                .setFifoAllocationSummary(event.getFifoAllocationSummary())
                .setAuditCopyStatus(event.getAuditCopyStatus())
                .setAuditCopySummary(event.getAuditCopySummary())
                .setSubmissionReviewStatus(event.getSubmissionReviewStatus())
                .setSubmissionReviewRemark(event.getSubmissionReviewRemark())
                .setSubmissionReviewLeaderUserId(event.getSubmissionReviewLeaderUserId())
                .setSubmissionReviewLeaderUserName(event.getSubmissionReviewLeaderUserName())
                .setSubmissionReviewedAt(event.getSubmissionReviewedAt())
                .setModificationHistorySummary(event.getModificationHistorySummary());
        fillProductionSubmissionPayload(event, respVO);
    }

    private List<ProcessPoolTimelineEventRespVO> enrichPqcSubmissionGroupRows(ProcessPoolTimelinePageReqVO reqVO,
                                                                              List<ProcessPoolTimelineEventRespVO> list) {
        if (list.isEmpty() || reqVO == null
                || !MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION.equals(reqVO.getEventType())) {
            return list;
        }
        List<String> groupIds = list.stream()
                .map(ProcessPoolTimelineEventRespVO::getPqcSubmissionGroupId)
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .toList();
        Map<String, List<ProcessPoolTimelineEventReadDO>> eventPayloadsByGroupId = groupIds.isEmpty()
                ? Map.of()
                : timelineReadMapper.selectPqcSubmissionGroupPayloadsByGroupIds(groupIds).stream()
                .filter(row -> StrUtil.isNotBlank(row.getPqcSubmissionGroupId()))
                .collect(Collectors.groupingBy(row -> row.getPqcSubmissionGroupId().trim(),
                        LinkedHashMap::new, Collectors.toList()));
        List<ProcessPoolTimelineEventRespVO> enrichedRows = new ArrayList<>();
        for (ProcessPoolTimelineEventRespVO row : list) {
            PqcSubmissionRowAccumulator accumulator = new PqcSubmissionRowAccumulator(row);
            List<ProcessPoolTimelineEventReadDO> groupedPayloads = StrUtil.isBlank(row.getPqcSubmissionGroupId())
                    ? List.of()
                    : eventPayloadsByGroupId.getOrDefault(row.getPqcSubmissionGroupId().trim(), List.of());
            if (groupedPayloads.isEmpty()) {
                accumulator.add(row.getId(), row.getOriginalPayloadJson());
            } else {
                groupedPayloads.forEach(groupedPayload ->
                        accumulator.add(groupedPayload.getId(), groupedPayload.getOriginalPayloadJson()));
            }
            enrichedRows.add(accumulator.toRow());
        }
        return enrichedRows;
    }

    private class PqcSubmissionRowAccumulator {
        private final ProcessPoolTimelineEventRespVO representative;
        private final List<Long> eventIds = new ArrayList<>();
        private final List<String> payloadJsons = new ArrayList<>();
        private final List<Map<String, Object>> payloads = new ArrayList<>();
        private final Map<String, Object> itemDetailByKey = new LinkedHashMap<>();
        private final Map<String, Object> itemResultByKey = new LinkedHashMap<>();

        private PqcSubmissionRowAccumulator(ProcessPoolTimelineEventRespVO representative) {
            this.representative = representative;
        }

        private void add(Long eventId, String originalPayloadJson) {
            if (eventId == null) {
                throw new IllegalStateException("PQC提交分组缺少来源事件编号，不能合并显示");
            }
            eventIds.add(eventId);
            if (StrUtil.isNotBlank(originalPayloadJson)) {
                payloadJsons.add(originalPayloadJson);
                Map<String, Object> payload = parseOriginalPayload(originalPayloadJson);
                if (payload != null) {
                    payloads.add(payload);
                    appendPqcItems(itemDetailByKey, payload.get("pqcItemDetails"));
                    appendPqcItems(itemResultByKey, payload.get("itemResults"));
                }
            }
        }

        private ProcessPoolTimelineEventRespVO toRow() {
            representative.setGroupedEventIds(List.copyOf(eventIds));
            representative.setGroupedOriginalPayloadJsons(List.copyOf(payloadJsons));
            if (eventIds.size() <= 1 || payloads.isEmpty()) {
                return representative;
            }
            representative.setOriginalPayloadJson(buildGroupedPqcPayloadJson());
            return representative;
        }

        private String buildGroupedPqcPayloadJson() {
            Map<String, Object> groupedPayload = new LinkedHashMap<>(payloads.get(0));
            groupedPayload.put("groupedEventIds", List.copyOf(eventIds));
            groupedPayload.put("groupedOriginalPayloadJsons", List.copyOf(payloadJsons));
            groupedPayload.put("pqcItemDetails", new ArrayList<>(itemDetailByKey.values()));
            groupedPayload.put("itemResults", new ArrayList<>(itemResultByKey.values()));
            groupedPayload.put("actualInspectionQuantity", resolveGroupedQuantity("actualInspectionQuantity"));
            groupedPayload.put("scrapQuantity", resolveGroupedQuantity("scrapQuantity"));
            return JsonUtils.toJsonString(groupedPayload);
        }

        private String resolveGroupedQuantity(String key) {
            List<String> values = payloads.stream()
                    .map(payload -> payload.get(key))
                    .filter(Objects::nonNull)
                    .map(value -> String.valueOf(value).trim())
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .toList();
            if (values.isEmpty()) {
                return null;
            }
            return String.join("、", values);
        }

        private void appendPqcItems(Map<String, Object> target, Object source) {
            if (!(source instanceof List<?> items)) {
                return;
            }
            for (int index = 0; index < items.size(); index++) {
                Object item = items.get(index);
                if (!(item instanceof Map<?, ?> rawItem)) {
                    continue;
                }
                Object rawKey = rawItem.get("itemCode");
                String key = rawKey == null ? "item-" + index : String.valueOf(rawKey).trim();
                if (StrUtil.isBlank(key)) {
                    key = "item-" + index;
                }
                if (!target.containsKey(key)) {
                    target.put(key, item);
                }
            }
        }
    }

    private void fillProductionSubmissionPayload(ProcessPoolTimelineEventReadDO event,
                                                 ProcessPoolTimelineEventRespVO respVO) {
        Map<String, Object> payload = parseOriginalPayload(event.getOriginalPayloadJson());
        if (payload == null || payload.isEmpty()) {
            return;
        }
        if (!MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT.equals(event.getEventType())) {
            respVO.setOutputQuantity(toBigDecimal(payload.get("outputQuantity")));
        }
        respVO.setLossQuantity(toBigDecimal(payload.get("lossQuantity")))
                .setLossDetails(convertValue(payload.get("lossDetails"), LOSS_DETAIL_TYPE))
                .setMaterialDetails(convertValue(payload.get("materialDetails"), MATERIAL_DETAIL_TYPE))
                .setSelectedDevice(convertValue(payload.get("selectedDevice"),
                        ProcessPoolTimelineEventRespVO.SelectedDeviceRespVO.class))
                .setSelectedDevices(toSelectedDevices(payload.get("selectedDevices")))
                .setDeviceParameterReadings(convertValue(payload.get("deviceParameterReadings"),
                        DEVICE_PARAMETER_READING_TYPE));
    }

    private void fillFeedbackMaterialDetails(List<? extends ProcessPoolTimelineEventRespVO> events) {
        if (events.isEmpty()) {
            return;
        }
        Map<Long, ProcessPoolTimelineEventRespVO> eventByFeedbackId = events.stream()
                .filter(event -> event.getSourceFeedbackId() != null)
                .collect(Collectors.toMap(ProcessPoolTimelineEventRespVO::getSourceFeedbackId,
                        Function.identity(), (a, b) -> a, LinkedHashMap::new));
        if (eventByFeedbackId.isEmpty()) {
            return;
        }
        Map<Long, List<MesProFeedbackMaterialDO>> materialByFeedbackId =
                timelineReadMapper.selectFeedbackMaterialsByFeedbackIds(List.copyOf(eventByFeedbackId.keySet()))
                        .stream()
                        .collect(Collectors.groupingBy(MesProFeedbackMaterialDO::getFeedbackId,
                                LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<Long, ProcessPoolTimelineEventRespVO> entry : eventByFeedbackId.entrySet()) {
            List<MesProFeedbackMaterialDO> materials = materialByFeedbackId.getOrDefault(entry.getKey(), List.of());
            if (!materials.isEmpty()) {
                entry.getValue().setMaterialDetails(mergeFeedbackMaterialDetails(
                        entry.getValue().getMaterialDetails(), materials));
            }
        }
    }

    private List<ProcessPoolTimelineEventRespVO.MaterialDetailRespVO> mergeFeedbackMaterialDetails(
            List<ProcessPoolTimelineEventRespVO.MaterialDetailRespVO> payloadMaterials,
            List<MesProFeedbackMaterialDO> feedbackMaterials) {
        Map<Long, ProcessPoolTimelineEventRespVO.MaterialDetailRespVO> payloadByMaterialId =
                payloadMaterials == null ? Map.of() : payloadMaterials.stream()
                        .filter(material -> material.getMaterialId() != null)
                        .collect(Collectors.toMap(ProcessPoolTimelineEventRespVO.MaterialDetailRespVO::getMaterialId,
                                Function.identity(), (a, b) -> a, LinkedHashMap::new));
        return feedbackMaterials.stream()
                .map(material -> mergeFeedbackMaterialDetail(payloadByMaterialId.get(material.getMaterialId()),
                        material))
                .toList();
    }

    private ProcessPoolTimelineEventRespVO.MaterialDetailRespVO mergeFeedbackMaterialDetail(
            ProcessPoolTimelineEventRespVO.MaterialDetailRespVO payload,
            MesProFeedbackMaterialDO material) {
        ProcessPoolTimelineEventRespVO.MaterialDetailRespVO result =
                payload == null ? new ProcessPoolTimelineEventRespVO.MaterialDetailRespVO() : payload;
        result.setMaterialId(material.getMaterialId())
                .setMaterialCode(StrUtil.blankToDefault(result.getMaterialCode(), material.getMaterialCode()))
                .setMaterialName(StrUtil.blankToDefault(result.getMaterialName(), material.getMaterialName()))
                .setOutputQuantity(result.getOutputQuantity() == null
                        ? material.getOutputQuantity() : result.getOutputQuantity())
                .setLossQuantity(result.getLossQuantity() == null
                        ? material.getLossQuantity() : result.getLossQuantity());
        if (result.getLossDetails() == null || result.getLossDetails().isEmpty()) {
            result.setLossDetails(convertJson(material.getLossDetailsJson(), LOSS_DETAIL_TYPE));
        }
        if ((result.getSelectedDevices() == null || result.getSelectedDevices().isEmpty())
                && StrUtil.isNotBlank(material.getSelectedDeviceJson())) {
            result.setSelectedDevices(convertSelectedDevicesJson(material.getSelectedDeviceJson()));
        }
        if (result.getDeviceParameterReadings() == null || result.getDeviceParameterReadings().isEmpty()) {
            result.setDeviceParameterReadings(convertJson(material.getDeviceParameterReadingsJson(),
                    DEVICE_PARAMETER_READING_TYPE));
        }
        return result;
    }

    private <T> T convertJson(String json, TypeReference<T> typeReference) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        return JsonUtils.parseObject(json, typeReference);
    }

    private List<ProcessPoolTimelineEventRespVO.SelectedDeviceRespVO> convertSelectedDevicesJson(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        String trimmed = json.trim();
        if (trimmed.startsWith("[")) {
            return JsonUtils.parseObject(trimmed, SELECTED_DEVICE_LIST_TYPE);
        }
        if (trimmed.startsWith("{")) {
            ProcessPoolTimelineEventRespVO.SelectedDeviceRespVO device = JsonUtils.parseObject(trimmed,
                    ProcessPoolTimelineEventRespVO.SelectedDeviceRespVO.class);
            return device == null ? null : List.of(device);
        }
        throw new IllegalArgumentException("selectedDeviceJson 快照格式无效，必须是数组或单设备对象");
    }

    private Map<String, Object> parseOriginalPayload(String originalPayloadJson) {
        return JsonUtils.parseObject(originalPayloadJson, PAYLOAD_TYPE);
    }

    private void fillReportAllocations(List<? extends ProcessPoolTimelineEventRespVO> events) {
        if (events.isEmpty()) {
            return;
        }
        Map<Long, ProcessPoolTimelineEventRespVO> eventById = events.stream().collect(Collectors.toMap(
                ProcessPoolTimelineEventRespVO::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Map<Long, List<ProcessPoolTimelineReportAllocationReadDO>> allocationsByEvent =
                timelineReadMapper.selectReportAllocationsByEventIds(List.copyOf(eventById.keySet())).stream()
                        .collect(Collectors.groupingBy(ProcessPoolTimelineReportAllocationReadDO::getEventId,
                                LinkedHashMap::new, Collectors.toList()));
        for (ProcessPoolTimelineEventRespVO event : events) {
            List<ProcessPoolTimelineReportAllocationReadDO> allocations =
                    allocationsByEvent.getOrDefault(event.getId(), List.of());
            List<ProcessPoolTimelineEventRespVO.ReportAllocationRespVO> lines = allocations.stream()
                    .map(line -> new ProcessPoolTimelineEventRespVO.ReportAllocationRespVO()
                            .setAllocationId(line.getAllocationId())
                            .setActiveOrderId(line.getActiveOrderId())
                            .setWorkOrderId(line.getWorkOrderId())
                            .setWorkOrderCode(line.getWorkOrderCode())
                            .setAllocatedQuantity(line.getAllocatedQuantity())
                            .setOverageQuantity(requireAllocationOverage(line))
                            .setNeedsAdjustment(Boolean.TRUE.equals(line.getNeedsAdjustment()))
                            .setReleased(Boolean.TRUE.equals(line.getReleased()))
                            .setEditable(!Boolean.TRUE.equals(line.getReleased())))
                    .toList();
            event.setReportAllocations(lines);
        }
    }

    private static BigDecimal requireAllocationOverage(ProcessPoolTimelineReportAllocationReadDO line) {
        if (line.getOverageQuantity() == null || line.getNeedsAdjustment() == null) {
            throw new IllegalStateException("Missing formal order-process overage for report allocation "
                    + line.getAllocationId());
        }
        return line.getOverageQuantity();
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(String.valueOf(value).trim());
    }

    private static <T> T convertValue(Object value, Class<T> targetClass) {
        return value == null ? null : JsonUtils.getObjectMapper().convertValue(value, targetClass);
    }

    private static <T> T convertValue(Object value, TypeReference<T> typeReference) {
        return value == null ? null : JsonUtils.getObjectMapper().convertValue(value, typeReference);
    }

    private static List<ProcessPoolTimelineEventRespVO.SelectedDeviceRespVO> toSelectedDevices(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List<?>) {
            return convertValue(value, SELECTED_DEVICE_LIST_TYPE);
        }
        if (value instanceof Map<?, ?>) {
            return List.of(convertValue(value, ProcessPoolTimelineEventRespVO.SelectedDeviceRespVO.class));
        }
        throw new IllegalArgumentException("selectedDevices 快照格式无效，必须是数组或单设备对象");
    }

    private static String resolveDisplayProcessCode(ProcessPoolTimelineEventReadDO event) {
        if (event == null) {
            return null;
        }
        if (StrUtil.isNotBlank(event.getProcessCode())) {
            return event.getProcessCode();
        }
        if (MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION.equals(event.getEventType())) {
            return event.getQaProcessCode();
        }
        return event.getProcessCode();
    }

    private static String resolveDisplayProcessName(ProcessPoolTimelineEventReadDO event) {
        if (event == null) {
            return null;
        }
        if (StrUtil.isNotBlank(event.getProcessName())) {
            return event.getProcessName();
        }
        if (MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION.equals(event.getEventType())) {
            return event.getQaProcessName();
        }
        return event.getProcessName();
    }

}
