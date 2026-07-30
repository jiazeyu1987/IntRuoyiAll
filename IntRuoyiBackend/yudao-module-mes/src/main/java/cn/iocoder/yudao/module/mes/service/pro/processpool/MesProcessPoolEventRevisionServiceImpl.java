package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDiffDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionDiffMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_CHANGE_REASON_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_DIFF_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_FIFO_LOCK_STATUS_UNKNOWN;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_SIGNATURE_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_SIGNATURE_REUSED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_SIGNATURE_EMPLOYEE_MISMATCH;

@Service
@Validated
public class MesProcessPoolEventRevisionServiceImpl implements MesProcessPoolEventRevisionService {

    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProProcessPoolEventRevisionMapper revisionMapper;
    private final MesProProcessPoolEventRevisionDiffMapper revisionDiffMapper;
    private final MesProcessPoolFifoAllocationService fifoAllocationService;

    public MesProcessPoolEventRevisionServiceImpl(MesProProcessPoolEventMapper eventMapper,
                                                  MesProProcessPoolEventRevisionMapper revisionMapper,
                                                  MesProProcessPoolEventRevisionDiffMapper revisionDiffMapper,
                                                  MesProcessPoolFifoAllocationService fifoAllocationService) {
        this.eventMapper = eventMapper;
        this.revisionMapper = revisionMapper;
        this.revisionDiffMapper = revisionDiffMapper;
        this.fifoAllocationService = fifoAllocationService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long updateOriginalRecord(MesProcessPoolEventRevisionUpdateReqBO reqBO) {
        validateRequest(reqBO);
        MesProProcessPoolEventDO event = eventMapper.selectById(reqBO.getEventId());
        if (event == null) {
            throw exception(PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS, reqBO.getEventId());
        }
        validateJsonPayload(event.getRawPayload(), "rawPayload");
        validateSignature(reqBO, event);
        validateDiffAndFifoLocks(reqBO);

        LocalDateTime serverRevisionTime = LocalDateTime.now();
        MesProProcessPoolEventRevisionDO revision = MesProProcessPoolEventRevisionDO.builder()
                .eventId(event.getId())
                .poolId(event.getPoolId())
                .workOrderId(event.getWorkOrderId())
                .routeId(event.getRouteId())
                .routeProcessId(event.getRouteProcessId())
                .processId(event.getProcessId())
                .beforePayload(event.getRawPayload())
                .afterPayload(reqBO.getAfterPayload())
                .changeReason(reqBO.getChangeReason().trim())
                .revisionSignatureId(reqBO.getRevisionSignatureId())
                .revisionSignatureUserId(reqBO.getRevisionSignatureUserId())
                .revisionSignatureSnapshot(reqBO.getRevisionSignatureSnapshot())
                .modifiedByUserId(reqBO.getModifiedByUserId())
                .serverRevisionTime(serverRevisionTime)
                .revisionStatus(MesProProcessPoolEventRevisionDO.STATUS_EFFECTIVE)
                .build();
        revisionMapper.insert(revision);

        for (MesProcessPoolEventRevisionFieldChangeBO field : reqBO.getChangedFields()) {
            revisionDiffMapper.insert(toDiffDO(revision.getId(), event.getId(), field));
        }

        eventMapper.updateById(new MesProProcessPoolEventDO()
                .setId(event.getId())
                .setRawPayload(reqBO.getAfterPayload()));
        return revision.getId();
    }

    private void validateRequest(MesProcessPoolEventRevisionUpdateReqBO reqBO) {
        if (reqBO == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "revisionRequest");
        }
        requirePositive(reqBO.getEventId(), "eventId");
        if (StrUtil.isBlank(reqBO.getAfterPayload())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "afterPayload");
        }
        validateJsonPayload(reqBO.getAfterPayload(), "afterPayload");
        if (StrUtil.isBlank(reqBO.getChangeReason())) {
            throw exception(PRO_PROCESS_POOL_REVISION_CHANGE_REASON_REQUIRED);
        }
        requirePositive(reqBO.getRevisionSignatureId(), "revisionSignatureId");
        requirePositive(reqBO.getRevisionSignatureUserId(), "revisionSignatureUserId");
        if (StrUtil.isBlank(reqBO.getRevisionSignatureSnapshot())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "revisionSignatureSnapshot");
        }
        validateJsonPayload(reqBO.getRevisionSignatureSnapshot(), "revisionSignatureSnapshot");
        requirePositive(reqBO.getModifiedByUserId(), "modifiedByUserId");
        if (CollUtil.isEmpty(reqBO.getChangedFields())) {
            throw exception(PRO_PROCESS_POOL_REVISION_DIFF_REQUIRED);
        }
    }

    private void validateSignature(MesProcessPoolEventRevisionUpdateReqBO reqBO, MesProProcessPoolEventDO event) {
        if (!Objects.equals(reqBO.getRevisionSignatureUserId(), reqBO.getModifiedByUserId())) {
            throw exception(PRO_PROCESS_POOL_SIGNATURE_EMPLOYEE_MISMATCH);
        }
        if (Objects.equals(reqBO.getRevisionSignatureId(), event.getSignatureId())) {
            throw exception(PRO_PROCESS_POOL_REVISION_SIGNATURE_REUSED);
        }
        if (eventMapper.selectBySignatureId(reqBO.getRevisionSignatureId()) != null
                || revisionMapper.selectBySignatureId(reqBO.getRevisionSignatureId()) != null) {
            throw exception(PRO_PROCESS_POOL_REVISION_SIGNATURE_DUPLICATE, reqBO.getRevisionSignatureId());
        }
    }

    private void validateDiffAndFifoLocks(MesProcessPoolEventRevisionUpdateReqBO reqBO) {
        for (MesProcessPoolEventRevisionFieldChangeBO field : reqBO.getChangedFields()) {
            validateFieldDiff(field);
            if (Boolean.TRUE.equals(field.getAffectsQuantityFragment())) {
                if (field.getSourceQuantityFragmentId() == null || field.getOriginalField() == null) {
                    throw exception(PRO_PROCESS_POOL_REVISION_FIFO_LOCK_STATUS_UNKNOWN,
                            field.getFieldCode());
                }
                fifoAllocationService.validateOriginalFieldMutationAllowed(
                        field.getSourceQuantityFragmentId(), field.getOriginalField());
            }
        }
    }

    private void validateFieldDiff(MesProcessPoolEventRevisionFieldChangeBO field) {
        if (field == null || StrUtil.isBlank(field.getFieldCode()) || StrUtil.isBlank(field.getFieldName())
                || field.getAffectsQuantityFragment() == null || field.getOriginalField() == null
                || Objects.equals(field.getBeforeValue(), field.getAfterValue())) {
            throw exception(PRO_PROCESS_POOL_REVISION_DIFF_REQUIRED);
        }
    }

    private MesProProcessPoolEventRevisionDiffDO toDiffDO(Long revisionId, Long eventId,
                                                          MesProcessPoolEventRevisionFieldChangeBO field) {
        return MesProProcessPoolEventRevisionDiffDO.builder()
                .revisionId(revisionId)
                .eventId(eventId)
                .fieldCode(field.getFieldCode())
                .fieldName(field.getFieldName())
                .beforeValue(field.getBeforeValue())
                .afterValue(field.getAfterValue())
                .affectsQuantityFragment(Boolean.TRUE.equals(field.getAffectsQuantityFragment()))
                .sourceQuantityFragmentId(field.getSourceQuantityFragmentId())
                .originalFieldCode(field.getOriginalField().name())
                .originalFieldName(toOriginalFieldName(field.getOriginalField()))
                .build();
    }

    private String toOriginalFieldName(MesProcessPoolFragmentOriginalField field) {
        return switch (field) {
            case OUTPUT_QUANTITY -> "输出数量";
            case QUALITY_STATUS -> "质量状态";
            case ALLOCATABLE_STATUS -> "可分配状态";
            case REMARK -> "备注";
        };
    }

    private void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, fieldName);
        }
    }

    private void validateJsonPayload(String payload, String fieldName) {
        if (StrUtil.isBlank(payload)) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, fieldName);
        }
        try {
            JsonUtils.parseTree(payload);
        } catch (RuntimeException ex) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, fieldName);
        }
    }
}
