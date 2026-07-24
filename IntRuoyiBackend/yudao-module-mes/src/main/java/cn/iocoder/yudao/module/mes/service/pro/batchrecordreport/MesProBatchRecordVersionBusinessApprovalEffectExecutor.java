package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalEffectResult;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalEffectExecutor;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalErrorCode;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordDefinitionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionApprovalEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordDefinitionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionApprovalEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMigrationItemMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MesProBatchRecordVersionBusinessApprovalEffectExecutor implements BusinessApprovalEffectExecutor {

    public static final String EXECUTOR_CODE = "MES_BATCH_RECORD_VERSION_PUBLISH";
    public static final String PROCESS_DEFINITION_KEY = "mes-batch-record-version-approval-v1";
    private static final String OBJECT_TYPE = "BATCH_RECORD_VERSION";
    private static final String ACTION_CODE = "PUBLISH";
    private static final String STATUS_PRECHECK_PASSED = "PRECHECK_PASSED";
    private static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    @Resource
    private MesProBatchRecordVersionMapper versionMapper;
    @Resource
    private MesProBatchRecordDefinitionMapper definitionMapper;
    @Resource
    private MesProBatchRecordVersionMigrationItemMapper migrationItemMapper;
    @Resource
    private MesProBatchRecordVersionApprovalEventMapper approvalEventMapper;

    @Override
    public String getExecutorCode() {
        return EXECUTOR_CODE;
    }

    public String getBpmProcessDefinitionKey() {
        return PROCESS_DEFINITION_KEY;
    }

    @Override
    public void precheck(BusinessApprovalContext context) {
        requirePublishContext(context);
    }

    @Override
    public BusinessApprovalEffectResult executeDirect(BusinessApprovalContext context,
                                                      BusinessApprovalRequest request) {
        requirePublishContext(context);
        MesProBatchRecordVersionDO version = requireVersionWithStatus(context, STATUS_PRECHECK_PASSED);
        requirePublishPreconditions(version);
        publishVersion(version, requireActorUserId(context.getApplicantUserId(), "applicantUserId"));
        recordEvent(version, request, "DIRECT", STATUS_APPROVED, context.getApplicantUserId(), null);
        return BusinessApprovalEffectResult.completed(STATUS_APPROVED);
    }

    @Override
    public BusinessApprovalEffectResult markPending(BusinessApprovalContext context,
                                                    BusinessApprovalRequest request) {
        requirePublishContext(context);
        MesProBatchRecordVersionDO version = requireVersionWithStatus(context, STATUS_PRECHECK_PASSED);
        requirePublishPreconditions(version);
        Long applicantUserId = requireActorUserId(context.getApplicantUserId(), "applicantUserId");
        MesProBatchRecordVersionDO update = new MesProBatchRecordVersionDO();
        update.setId(version.getId());
        update.setStatus(STATUS_PENDING_APPROVAL);
        update.setSubmittedBy(applicantUserId);
        update.setSubmittedAt(LocalDateTime.now());
        update.setApprovalInstanceId(requireProcessInstanceId(request));
        requireUpdated(versionMapper.updateById(update), "mark pending batch record version");
        recordEvent(version, request, "PENDING", STATUS_PENDING_APPROVAL, applicantUserId, null);
        return BusinessApprovalEffectResult.pending(STATUS_PENDING_APPROVAL);
    }

    @Override
    public BusinessApprovalEffectResult executeApproved(BusinessApprovalContext context,
                                                        BusinessApprovalRequest request,
                                                        Long actorUserId) {
        requirePublishContext(context);
        MesProBatchRecordVersionDO version = requireVersionWithStatus(context, STATUS_PENDING_APPROVAL);
        Long approverUserId = requireActorUserId(actorUserId, "actorUserId");
        publishVersion(version, approverUserId);
        recordEvent(version, request, "APPROVED", STATUS_APPROVED, approverUserId, null);
        return BusinessApprovalEffectResult.completed(STATUS_APPROVED);
    }

    @Override
    public BusinessApprovalEffectResult reject(BusinessApprovalContext context,
                                               BusinessApprovalRequest request,
                                               Long actorUserId,
                                               String reason) {
        requirePublishContext(context);
        MesProBatchRecordVersionDO version = requireVersionWithStatus(context, STATUS_PENDING_APPROVAL);
        Long rejectUserId = requireActorUserId(actorUserId, "actorUserId");
        String rejectReason = StrUtil.trimToNull(reason);
        MesProBatchRecordVersionDO update = new MesProBatchRecordVersionDO();
        update.setId(version.getId());
        update.setStatus(STATUS_REJECTED);
        update.setApprovedBy(rejectUserId);
        update.setApprovedAt(LocalDateTime.now());
        update.setRejectReason(rejectReason);
        requireUpdated(versionMapper.updateById(update), "reject batch record version");
        recordEvent(version, request, "REJECTED", STATUS_REJECTED, rejectUserId, rejectReason);
        return BusinessApprovalEffectResult.rejected(STATUS_REJECTED);
    }

    @Override
    public BusinessApprovalEffectResult cancel(BusinessApprovalContext context,
                                               BusinessApprovalRequest request,
                                               Long actorUserId,
                                               String reason) {
        requirePublishContext(context);
        MesProBatchRecordVersionDO version = requireVersionWithStatus(context, STATUS_PENDING_APPROVAL);
        Long cancelUserId = requireActorUserId(actorUserId, "actorUserId");
        String cancelReason = StrUtil.trimToNull(reason);
        MesProBatchRecordVersionDO update = new MesProBatchRecordVersionDO();
        update.setId(version.getId());
        update.setStatus(STATUS_REJECTED);
        update.setApprovedBy(cancelUserId);
        update.setApprovedAt(LocalDateTime.now());
        update.setRejectReason(cancelReason);
        requireUpdated(versionMapper.updateById(update), "cancel batch record version approval");
        recordEvent(version, request, "CANCELLED", STATUS_REJECTED, cancelUserId, cancelReason);
        return BusinessApprovalEffectResult.cancelled(STATUS_REJECTED);
    }

    private void publishVersion(MesProBatchRecordVersionDO version, Long actorUserId) {
        requireUpdated(definitionMapper.updateCurrentVersionIfMatch(
                version.getDefinitionId(), version.getSourceVersionId(), version.getId()),
                "publish batch record version");
        versionMapper.obsoleteApprovedVersionsExcept(version.getDefinitionId(), version.getId());
        MesProBatchRecordVersionDO update = new MesProBatchRecordVersionDO();
        update.setId(version.getId());
        update.setStatus(STATUS_APPROVED);
        update.setApprovedBy(actorUserId);
        update.setApprovedAt(LocalDateTime.now());
        requireUpdated(versionMapper.updateById(update), "approve batch record version");
    }

    private void requirePublishContext(BusinessApprovalContext context) {
        if (context == null
                || !OBJECT_TYPE.equals(context.getObjectType())
                || !ACTION_CODE.equals(context.getActionCode())) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "MES batch record version publish approval context is invalid");
        }
        parseVersionId(context);
    }

    private MesProBatchRecordVersionDO requireVersionWithStatus(BusinessApprovalContext context, String expectedStatus) {
        Long versionId = parseVersionId(context);
        MesProBatchRecordVersionDO version = versionMapper.selectByIdForUpdate(versionId);
        if (version == null || !expectedStatus.equals(version.getStatus())) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "MES batch record version status is invalid: versionId=" + versionId
                            + ", expected=" + expectedStatus
                            + ", actual=" + (version == null ? null : version.getStatus()));
        }
        return version;
    }

    private void requirePublishPreconditions(MesProBatchRecordVersionDO version) {
        MesProBatchRecordDefinitionDO definition = definitionMapper.selectByIdForUpdate(version.getDefinitionId());
        if (definition == null) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "MES batch record definition does not exist: " + version.getDefinitionId());
        }
        MesProBatchRecordVersionDO pendingVersion =
                versionMapper.selectPendingApprovalByDefinitionIdForUpdate(version.getDefinitionId());
        if (pendingVersion != null && !pendingVersion.getId().equals(version.getId())) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_PENDING_CONFLICT,
                    "MES batch record version already has pending approval: definitionId=" + version.getDefinitionId());
        }
        if (migrationItemMapper.countBlockingItems(version.getId()) > 0) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "MES batch record version migration is blocked: versionId=" + version.getId());
        }
    }

    private Long parseVersionId(BusinessApprovalContext context) {
        try {
            return Long.valueOf(context.getObjectId());
        } catch (RuntimeException ex) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "MES batch record version id is invalid: " + (context == null ? null : context.getObjectId()));
        }
    }

    private Long requireActorUserId(Long actorUserId, String fieldName) {
        if (actorUserId == null) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "MES batch record version actor is required: " + fieldName);
        }
        return actorUserId;
    }

    private String requireProcessInstanceId(BusinessApprovalRequest request) {
        String processInstanceId = request == null ? null : request.getProcessInstanceId();
        if (StrUtil.isBlank(processInstanceId)) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_PROCESS_NOT_STARTED,
                    "MES batch record version approval process instance is required");
        }
        return StrUtil.trim(processInstanceId);
    }

    private void recordEvent(MesProBatchRecordVersionDO version,
                             BusinessApprovalRequest request,
                             String approvalResult,
                             String processedResult,
                             Long actorUserId,
                             String remark) {
        MesProBatchRecordVersionApprovalEventDO event = new MesProBatchRecordVersionApprovalEventDO();
        event.setDefinitionId(version.getDefinitionId());
        event.setVersionId(version.getId());
        event.setApprovalInstanceId(request == null ? null : request.getProcessInstanceId());
        event.setApprovalEventId(request == null || request.getRequestId() == null
                ? null : String.valueOf(request.getRequestId()) + ":" + approvalResult);
        event.setApprovalResult(approvalResult);
        event.setProcessedResult(processedResult);
        event.setActorUserId(actorUserId);
        event.setProcessedAt(LocalDateTime.now());
        event.setRemark(remark);
        approvalEventMapper.insert(event);
    }

    private void requireUpdated(int updated, String action) {
        if (updated <= 0) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "MES batch record version update failed: " + action);
        }
    }

}
