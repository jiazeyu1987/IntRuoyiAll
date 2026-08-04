package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolQuantityFragmentDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolQuantityFragmentMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreateEventReqDTO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreatePqcInspectionReqDTO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolQuantityFragmentCreateDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_PQC_RESULT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_SIGNATURE_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_SIGNATURE_EMPLOYEE_MISMATCH;

@Service
@Validated
public class MesProcessPoolEventServiceImpl implements MesProcessPoolEventService {

    @Resource
    private MesProProcessPoolMapper processPoolMapper;
    @Resource
    private MesProProcessPoolEventMapper processPoolEventMapper;
    @Resource
    private MesProProcessPoolQuantityFragmentMapper quantityFragmentMapper;
    @Resource
    private MesProProcessPoolPqcRecordMapper pqcRecordMapper;

    @Override
    public Optional<MesProcessPoolSubmitEventResult> findExistingSubmitEvent(MesProcessPoolCreateEventReqDTO reqDTO) {
        requireSubmitLookupContext(reqDTO);
        MesProProcessPoolEventDO existing = processPoolEventMapper.selectSubmitByIdempotency(toLookupEvent(reqDTO));
        return Optional.ofNullable(existing).map(this::toSubmitEventResult);
    }

    @Override
    public Optional<Long> findExistingPqcInspectionTaskId(MesProcessPoolCreatePqcInspectionReqDTO reqDTO) {
        return findExistingPqcInspectionEvent(reqDTO).map(MesProProcessPoolEventDO::getFeedbackSourceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEvent(MesProcessPoolCreateEventReqDTO reqDTO) {
        validateEventRequest(reqDTO);
        validateSignature(reqDTO.getActualEmployeeId(), reqDTO.getSignatureUserId());
        if (isProductionSubmit(reqDTO)) {
            Optional<MesProcessPoolSubmitEventResult> existing = findExistingSubmitEvent(reqDTO);
            if (existing.isPresent()) {
                return existing.get().getProcessPoolEventId();
            }
        }
        validateUniqueSignature(reqDTO.getSignatureId());

        LocalDateTime serverSubmitTime = LocalDateTime.now();
        MesProProcessPoolDO pool = getOrCreatePool(reqDTO, serverSubmitTime);
        MesProProcessPoolEventDO event = buildEvent(reqDTO, pool.getId(), serverSubmitTime);
        processPoolEventMapper.insert(event);

        createQuantityFragments(reqDTO, event);
        updatePoolAfterEvent(pool, event);
        return event.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPqcInspectionEvent(MesProcessPoolCreatePqcInspectionReqDTO reqDTO) {
        if (reqDTO == null) {
            throw missingContext("request");
        }
        requirePositive(reqDTO.getProductionSubmitEventId(), "productionSubmitEventId");
        validatePqcInspectionResult(reqDTO.getInspectionResult());
        Optional<MesProProcessPoolEventDO> existing = findExistingPqcInspectionEvent(reqDTO);
        if (existing.isPresent()) {
            return existing.get().getId();
        }

        Long eventId = createEvent(MesProcessPoolCreateEventReqDTO.builder()
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                .eventIdempotencyKey(reqDTO.getPqcSubmissionIdempotencyKey())
                .workOrderId(reqDTO.getWorkOrderId())
                .routeId(reqDTO.getRouteId())
                .routeProcessId(reqDTO.getRouteProcessId())
                .processId(reqDTO.getProcessId())
                .actualEmployeeId(reqDTO.getActualEmployeeId())
                .deviceAccountId(reqDTO.getDeviceAccountId())
                .deviceId(reqDTO.getDeviceId())
                .workstationId(reqDTO.getWorkstationId())
                .templateType(reqDTO.getTemplateType())
                .feedbackSourceType(reqDTO.getFeedbackSourceType())
                .feedbackSourceId(reqDTO.getFeedbackSourceId())
                .recordbookSourceType(reqDTO.getRecordbookSourceType())
                .recordbookSourceId(reqDTO.getRecordbookSourceId())
                .rawPayload(reqDTO.getRawPayload())
                .clientSubmitTime(reqDTO.getClientSubmitTime())
                .signatureId(reqDTO.getSignatureId())
                .signatureUserId(reqDTO.getSignatureUserId())
                .signatureSnapshot(reqDTO.getSignatureSnapshot())
                .build());

        MesProProcessPoolEventDO event = processPoolEventMapper.selectById(eventId);
        MesProProcessPoolPqcRecordDO pqcRecord = MesProProcessPoolPqcRecordDO.builder()
                .poolId(event.getPoolId())
                .eventId(event.getId())
                .productionSubmitEventId(reqDTO.getProductionSubmitEventId())
                .workOrderId(event.getWorkOrderId())
                .routeId(event.getRouteId())
                .routeProcessId(event.getRouteProcessId())
                .processId(event.getProcessId())
                .actualEmployeeId(event.getActualEmployeeId())
                .signatureId(event.getSignatureId())
                .signatureUserId(event.getSignatureUserId())
                .inspectionResult(reqDTO.getInspectionResult())
                .serverSubmitTime(event.getServerSubmitTime())
                .rawPayload(reqDTO.getRawPayload())
                .processInspectionAggregationStatus(
                        MesProProcessPoolPqcRecordDO.PROCESS_INSPECTION_AGGREGATION_STATUS_PENDING)
                .build();
        pqcRecordMapper.insert(pqcRecord);
        return eventId;
    }

    private void validateEventRequest(MesProcessPoolCreateEventReqDTO reqDTO) {
        if (reqDTO == null) {
            throw missingContext("request");
        }
        requireNotBlank(reqDTO.getEventType(), "eventType");
        requireNotBlank(reqDTO.getEventIdempotencyKey(), "eventIdempotencyKey");
        requirePositive(reqDTO.getWorkOrderId(), "workOrderId");
        requirePositive(reqDTO.getRouteId(), "routeId");
        requirePositive(reqDTO.getRouteProcessId(), "routeProcessId");
        requirePositive(reqDTO.getProcessId(), "processId");
        requirePositive(reqDTO.getActualEmployeeId(), "actualEmployeeId");
        requireNotBlank(reqDTO.getTemplateType(), "templateType");
        requireNotBlank(reqDTO.getFeedbackSourceType(), "feedbackSourceType");
        requirePositive(reqDTO.getFeedbackSourceId(), "feedbackSourceId");
        if (isProductionSubmit(reqDTO)) {
            requirePositive(reqDTO.getRecordbookEntryId(), "recordbookEntryId");
        }
        requireNotBlank(reqDTO.getRecordbookSourceType(), "recordbookSourceType");
        requirePositive(reqDTO.getRecordbookSourceId(), "recordbookSourceId");
        requireNotBlank(reqDTO.getRawPayload(), "rawPayload");
        requirePositive(reqDTO.getSignatureId(), "signatureId");
        requirePositive(reqDTO.getSignatureUserId(), "signatureUserId");
        if (!MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION.equals(reqDTO.getEventType())) {
            requirePositive(reqDTO.getDeviceAccountId(), "deviceAccountId");
            requirePositive(reqDTO.getDeviceId(), "deviceId");
            requirePositive(reqDTO.getWorkstationId(), "workstationId");
        }
        validateQuantityFragments(reqDTO);
    }

    private void validateQuantityFragments(MesProcessPoolCreateEventReqDTO reqDTO) {
        if (CollUtil.isEmpty(reqDTO.getQuantityFragments())) {
            return;
        }
        for (MesProcessPoolQuantityFragmentCreateDTO fragment : reqDTO.getQuantityFragments()) {
            if (fragment == null) {
                throw missingContext("quantityFragments");
            }
            requireNotBlank(fragment.getSourceQuantityType(), "quantityFragments.sourceQuantityType");
            if (fragment.getTotalQuantity() == null || fragment.getTotalQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw missingContext("quantityFragments.totalQuantity");
            }
        }
    }

    private void validateSignature(Long actualEmployeeId, Long signatureUserId) {
        if (!Objects.equals(actualEmployeeId, signatureUserId)) {
            throw exception(PRO_PROCESS_POOL_SIGNATURE_EMPLOYEE_MISMATCH);
        }
    }

    private void validateUniqueSignature(Long signatureId) {
        if (processPoolEventMapper.selectBySignatureId(signatureId) != null) {
            throw exception(PRO_PROCESS_POOL_SIGNATURE_DUPLICATE, signatureId);
        }
    }

    private void validatePqcInspectionResult(String inspectionResult) {
        if (!MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS.equals(inspectionResult)
                && !MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE.equals(inspectionResult)) {
            throw exception(PRO_PROCESS_POOL_PQC_RESULT_INVALID, inspectionResult);
        }
    }

    private MesProProcessPoolDO getOrCreatePool(MesProcessPoolCreateEventReqDTO reqDTO, LocalDateTime serverSubmitTime) {
        MesProProcessPoolDO pool = processPoolMapper.selectByContext(reqDTO.getWorkOrderId(), reqDTO.getRouteId(),
                reqDTO.getRouteProcessId(), reqDTO.getProcessId(), reqDTO.getDeviceId(), reqDTO.getWorkstationId());
        if (pool != null) {
            return pool;
        }
        pool = MesProProcessPoolDO.builder()
                .workOrderId(reqDTO.getWorkOrderId())
                .routeId(reqDTO.getRouteId())
                .routeProcessId(reqDTO.getRouteProcessId())
                .processId(reqDTO.getProcessId())
                .deviceId(reqDTO.getDeviceId())
                .workstationId(reqDTO.getWorkstationId())
                .poolStatus(MesProProcessPoolDO.STATUS_ACTIVE)
                .latestSubmitTime(serverSubmitTime)
                .totalEventCount(0)
                .lastActualEmployeeId(reqDTO.getActualEmployeeId())
                .build();
        processPoolMapper.insert(pool);
        return pool;
    }

    private MesProProcessPoolEventDO buildEvent(MesProcessPoolCreateEventReqDTO reqDTO, Long poolId,
                                                LocalDateTime serverSubmitTime) {
        return MesProProcessPoolEventDO.builder()
                .poolId(poolId)
                .eventType(reqDTO.getEventType())
                .eventIdempotencyKey(reqDTO.getEventIdempotencyKey())
                .workOrderId(reqDTO.getWorkOrderId())
                .routeId(reqDTO.getRouteId())
                .routeProcessId(reqDTO.getRouteProcessId())
                .processId(reqDTO.getProcessId())
                .actualEmployeeId(reqDTO.getActualEmployeeId())
                .deviceAccountId(reqDTO.getDeviceAccountId())
                .deviceId(reqDTO.getDeviceId())
                .workstationId(reqDTO.getWorkstationId())
                .templateType(reqDTO.getTemplateType())
                .feedbackSourceType(reqDTO.getFeedbackSourceType())
                .feedbackSourceId(reqDTO.getFeedbackSourceId())
                .recordbookEntryId(reqDTO.getRecordbookEntryId())
                .recordbookSourceType(reqDTO.getRecordbookSourceType())
                .recordbookSourceId(reqDTO.getRecordbookSourceId())
                .rawPayload(reqDTO.getRawPayload())
                .serverSubmitTime(serverSubmitTime)
                .signatureId(reqDTO.getSignatureId())
                .signatureUserId(reqDTO.getSignatureUserId())
                .signatureSnapshot(reqDTO.getSignatureSnapshot())
                .build();
    }

    private MesProProcessPoolEventDO toLookupEvent(MesProcessPoolCreateEventReqDTO reqDTO) {
        return MesProProcessPoolEventDO.builder()
                .eventIdempotencyKey(reqDTO.getEventIdempotencyKey())
                .workOrderId(reqDTO.getWorkOrderId())
                .routeId(reqDTO.getRouteId())
                .routeProcessId(reqDTO.getRouteProcessId())
                .processId(reqDTO.getProcessId())
                .actualEmployeeId(reqDTO.getActualEmployeeId())
                .deviceAccountId(reqDTO.getDeviceAccountId())
                .deviceId(reqDTO.getDeviceId())
                .workstationId(reqDTO.getWorkstationId())
                .build();
    }

    private MesProcessPoolSubmitEventResult toSubmitEventResult(MesProProcessPoolEventDO event) {
        return new MesProcessPoolSubmitEventResult()
                .setFeedbackId(event.getFeedbackSourceId())
                .setRecordbookEntryId(event.getRecordbookEntryId())
                .setRecordbookEventId(event.getRecordbookSourceId())
                .setProcessPoolEventId(event.getId());
    }

    private void requireSubmitLookupContext(MesProcessPoolCreateEventReqDTO reqDTO) {
        if (reqDTO == null) {
            throw missingContext("request");
        }
        if (!isProductionSubmit(reqDTO)) {
            throw missingContext("eventType");
        }
        requireNotBlank(reqDTO.getEventIdempotencyKey(), "eventIdempotencyKey");
        requirePositive(reqDTO.getWorkOrderId(), "workOrderId");
        requirePositive(reqDTO.getRouteId(), "routeId");
        requirePositive(reqDTO.getRouteProcessId(), "routeProcessId");
        requirePositive(reqDTO.getProcessId(), "processId");
        requirePositive(reqDTO.getActualEmployeeId(), "actualEmployeeId");
        requirePositive(reqDTO.getDeviceAccountId(), "deviceAccountId");
        requirePositive(reqDTO.getDeviceId(), "deviceId");
        requirePositive(reqDTO.getWorkstationId(), "workstationId");
    }

    private Optional<MesProProcessPoolEventDO> findExistingPqcInspectionEvent(
            MesProcessPoolCreatePqcInspectionReqDTO reqDTO) {
        requirePqcLookupContext(reqDTO);
        MesProProcessPoolEventDO existing = processPoolEventMapper.selectPqcByIdempotency(toPqcLookupEvent(reqDTO));
        if (existing == null) {
            return Optional.empty();
        }
        MesProProcessPoolPqcRecordDO pqcRecord = pqcRecordMapper.selectByEventId(existing.getId());
        if (pqcRecord == null || !Objects.equals(pqcRecord.getProductionSubmitEventId(),
                reqDTO.getProductionSubmitEventId())) {
            throw missingContext("productionSubmitEventId");
        }
        return Optional.of(existing);
    }

    private MesProProcessPoolEventDO toPqcLookupEvent(MesProcessPoolCreatePqcInspectionReqDTO reqDTO) {
        return MesProProcessPoolEventDO.builder()
                .eventIdempotencyKey(reqDTO.getPqcSubmissionIdempotencyKey())
                .workOrderId(reqDTO.getWorkOrderId())
                .routeId(reqDTO.getRouteId())
                .routeProcessId(reqDTO.getRouteProcessId())
                .processId(reqDTO.getProcessId())
                .actualEmployeeId(reqDTO.getActualEmployeeId())
                .deviceAccountId(reqDTO.getDeviceAccountId())
                .deviceId(reqDTO.getDeviceId())
                .workstationId(reqDTO.getWorkstationId())
                .feedbackSourceType(reqDTO.getFeedbackSourceType())
                .feedbackSourceId(reqDTO.getFeedbackSourceId())
                .build();
    }

    private void requirePqcLookupContext(MesProcessPoolCreatePqcInspectionReqDTO reqDTO) {
        if (reqDTO == null) {
            throw missingContext("request");
        }
        requirePositive(reqDTO.getProductionSubmitEventId(), "productionSubmitEventId");
        requireNotBlank(reqDTO.getPqcSubmissionIdempotencyKey(), "pqcSubmissionIdempotencyKey");
        requirePositive(reqDTO.getWorkOrderId(), "workOrderId");
        requirePositive(reqDTO.getRouteId(), "routeId");
        requirePositive(reqDTO.getRouteProcessId(), "routeProcessId");
        requirePositive(reqDTO.getProcessId(), "processId");
        requirePositive(reqDTO.getActualEmployeeId(), "actualEmployeeId");
        requirePqcDeviceContext(reqDTO);
        requireNotBlank(reqDTO.getFeedbackSourceType(), "feedbackSourceType");
        requirePositive(reqDTO.getFeedbackSourceId(), "feedbackSourceId");
        requireNotBlank(reqDTO.getRecordbookSourceType(), "recordbookSourceType");
        requirePositive(reqDTO.getRecordbookSourceId(), "recordbookSourceId");
    }

    private void requirePqcDeviceContext(MesProcessPoolCreatePqcInspectionReqDTO reqDTO) {
        boolean hasAnyDeviceContext = reqDTO.getDeviceAccountId() != null
                || reqDTO.getDeviceId() != null
                || reqDTO.getWorkstationId() != null;
        if (!hasAnyDeviceContext) {
            return;
        }
        requirePositive(reqDTO.getDeviceAccountId(), "deviceAccountId");
        requirePositive(reqDTO.getDeviceId(), "deviceId");
        requirePositive(reqDTO.getWorkstationId(), "workstationId");
    }

    private boolean isProductionSubmit(MesProcessPoolCreateEventReqDTO reqDTO) {
        return reqDTO != null
                && Objects.equals(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT, reqDTO.getEventType());
    }

    private void createQuantityFragments(MesProcessPoolCreateEventReqDTO reqDTO, MesProProcessPoolEventDO event) {
        if (CollUtil.isEmpty(reqDTO.getQuantityFragments())) {
            return;
        }
        if (!isProductionSubmit(reqDTO)) {
            throw missingContext("quantityFragments.productionSubmitEventId");
        }
        for (MesProcessPoolQuantityFragmentCreateDTO fragment : reqDTO.getQuantityFragments()) {
            MesProProcessPoolQuantityFragmentDO fragmentDO = MesProProcessPoolQuantityFragmentDO.builder()
                    .poolId(event.getPoolId())
                    .eventId(event.getId())
                    .productionSubmitEventId(event.getId())
                    .workOrderId(event.getWorkOrderId())
                    .routeId(event.getRouteId())
                    .routeProcessId(event.getRouteProcessId())
                    .processId(event.getProcessId())
                    .sourceQuantityType(fragment.getSourceQuantityType())
                    .qualityStatus(fragment.getQualityStatus())
                    .totalQuantity(fragment.getTotalQuantity())
                    .allocatedQuantity(BigDecimal.ZERO)
                    .availableQuantity(fragment.getTotalQuantity())
                    .allocationStatus(MesProProcessPoolQuantityFragmentDO.ALLOCATION_STATUS_AVAILABLE)
                    .locked(Boolean.FALSE)
                    .rawPayload(fragment.getRawPayload())
                    .build();
            quantityFragmentMapper.insert(fragmentDO);
        }
    }

    private void updatePoolAfterEvent(MesProProcessPoolDO pool, MesProProcessPoolEventDO event) {
        Integer currentCount = pool.getTotalEventCount() == null ? 0 : pool.getTotalEventCount();
        MesProProcessPoolDO update = new MesProProcessPoolDO()
                .setId(pool.getId())
                .setLatestEventId(event.getId())
                .setLatestSubmitTime(event.getServerSubmitTime())
                .setTotalEventCount(currentCount + 1)
                .setLastActualEmployeeId(event.getActualEmployeeId());
        processPoolMapper.updateById(update);
    }

    private void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw missingContext(fieldName);
        }
    }

    private void requireNotBlank(String value, String fieldName) {
        if (StrUtil.isBlank(value)) {
            throw missingContext(fieldName);
        }
    }

    private RuntimeException missingContext(String fieldName) {
        return exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, fieldName);
    }
}
