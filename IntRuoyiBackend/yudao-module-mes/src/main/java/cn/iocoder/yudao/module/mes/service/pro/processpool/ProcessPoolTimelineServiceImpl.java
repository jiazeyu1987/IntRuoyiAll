package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelinePageReqVO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolTimelineReadMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.ProcessPoolTimelineEventReadDO;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Validated
public class ProcessPoolTimelineServiceImpl implements ProcessPoolTimelineService {

    private static final String SUBMIT_DATE_REQUIRED_MESSAGE = "工序池时间轴查询必须提供提交日期";
    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<ProcessPoolTimelineEventRespVO.LossDetailRespVO>> LOSS_DETAIL_TYPE =
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
        return toDetailRespVO(event);
    }

    private void prepareSubmitDateWindow(ProcessPoolTimelinePageReqVO reqVO) {
        if (reqVO == null || reqVO.getSubmitDate() == null) {
            throw new IllegalArgumentException(SUBMIT_DATE_REQUIRED_MESSAGE);
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
                .setProcessCode(event.getProcessCode())
                .setProcessName(event.getProcessName())
                .setTemplateType(event.getTemplateType())
                .setTemplateTypeName(event.getTemplateTypeName())
                .setWorkOrderId(event.getWorkOrderId())
                .setWorkOrderCode(event.getWorkOrderCode())
                .setWorkOrderName(event.getWorkOrderName())
                .setProductId(event.getProductId())
                .setProductCode(event.getProductCode())
                .setProductName(event.getProductName())
                .setPqcTaskId(event.getPqcTaskId())
                .setInspectionType(event.getInspectionType())
                .setPqcBusinessDate(event.getPqcBusinessDate())
                .setPqcShiftCode(event.getPqcShiftCode())
                .setRoundNo(event.getRoundNo())
                .setSourceFeedbackId(event.getSourceFeedbackId())
                .setSourceRecordbookEntryId(event.getSourceRecordbookEntryId())
                .setSourceRecordbookEventId(event.getSourceRecordbookEventId())
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

    private void fillProductionSubmissionPayload(ProcessPoolTimelineEventReadDO event,
                                                 ProcessPoolTimelineEventRespVO respVO) {
        Map<String, Object> payload = parseOriginalPayload(event.getOriginalPayloadJson());
        if (payload == null || payload.isEmpty()) {
            return;
        }
        respVO.setOutputQuantity(toBigDecimal(payload.get("outputQuantity")))
                .setLossQuantity(toBigDecimal(payload.get("lossQuantity")))
                .setLossDetails(convertValue(payload.get("lossDetails"), LOSS_DETAIL_TYPE))
                .setSelectedDevice(convertValue(payload.get("selectedDevice"),
                        ProcessPoolTimelineEventRespVO.SelectedDeviceRespVO.class))
                .setDeviceParameterReadings(convertValue(payload.get("deviceParameterReadings"),
                        DEVICE_PARAMETER_READING_TYPE));
    }

    private Map<String, Object> parseOriginalPayload(String originalPayloadJson) {
        return JsonUtils.parseObject(originalPayloadJson, PAYLOAD_TYPE);
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

}
