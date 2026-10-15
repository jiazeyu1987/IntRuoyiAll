package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDistributionRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryTrainingRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionRecipientDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMessageJobDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileObsoleteAuditDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingProgressDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryDistributionRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryTrainingRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMessageJobMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileObsoleteAuditMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingProgressMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileProcessTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileDistributionStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileChangeTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileMasterStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileMessageJobStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFilePreviewKindEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileTrainingStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccDistributionMediumEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileProcessDefinitionKeys;
import cn.iocoder.yudao.module.dcc.service.download.DccDownloadFileBinary;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_MANUAL_RELEASE_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DISTRIBUTION_MEDIUM_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DRAWING_PDF_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PDF_CONVERSION_CONFIG_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PDF_CONVERSION_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PUBLISH_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_EVIDENCE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_STAMP_GENERATION_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_STAMP_RETRY_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_NOT_EXISTS;

@Service
public class DccControlledFileFinalizationServiceImpl implements DccControlledFileFinalizationService {

    static final String MESSAGE_BUSINESS_TYPE_DISTRIBUTION = "DISTRIBUTION";
    static final String MESSAGE_BUSINESS_TYPE_TRAINING = "TRAINING";
    static final String MESSAGE_BUSINESS_TYPE_OBSOLETE = "OBSOLETE";
    static final String MESSAGE_TEMPLATE_DISTRIBUTION = "dcc_distribution";
    static final String MESSAGE_TEMPLATE_TRAINING = "dcc_training";
    private static final String MESSAGE_TEMPLATE_OBSOLETE = "dcc_obsolete";
    private static final String DOC_CONTROL_ROLE = "doc_control";
    private static final String APPROVE_PERMISSION = "dcc:controlled-file:approve";

    private static final Set<String> WITHDRAW_EVENT_STATUSES = Set.of(
            DccControlledFileStatusEnum.PENDING_SUPERVISOR_REVIEW.getStatus(),
            DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus(),
            DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus(),
            DccControlledFileStatusEnum.PENDING_MATRIX_APPROVAL.getStatus(),
            DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus(),
            DccControlledFileStatusEnum.APPROVING.getStatus()
    );
    private static final Set<String> REJECT_REPLAY_IGNORED_STATUSES = Set.of(
            DccControlledFileStatusEnum.REJECTED.getStatus(),
            DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus(),
            DccControlledFileStatusEnum.FINALIZING.getStatus(),
            DccControlledFileStatusEnum.TRAINING_IN_PROGRESS.getStatus(),
            DccControlledFileStatusEnum.PENDING_MANUAL_DISTRIBUTION.getStatus(),
            DccControlledFileStatusEnum.ACTIVE.getStatus(),
            DccControlledFileStatusEnum.SUPERSEDED.getStatus(),
            DccControlledFileStatusEnum.OBSOLETE.getStatus(),
            DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus()
    );

    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Resource
    private DccControlledFileDistributionMapper distributionMapper;
    @Resource
    private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Resource
    private DccControlledFileTrainingMapper trainingMapper;
    @Resource
    private DccControlledFileTrainingAssignmentMapper trainingAssignmentMapper;
    @Resource
    private DccControlledFileTrainingProgressMapper trainingProgressMapper;
    @Resource
    private DccControlledFileMessageJobMapper messageJobMapper;
    @Resource
    private DccControlledFileObsoleteAuditMapper obsoleteAuditMapper;
    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DccFileCategoryDistributionRuleMapper distributionRuleMapper;
    @Resource
    private DccFileCategoryTrainingRuleMapper trainingRuleMapper;
    @Resource
    private FileMapper fileMapper;
    @Resource
    private FileService fileService;
    @Resource
    private DccPdfStampService pdfStampService;
    @Resource
    private DccDocumentPdfConversionService pdfConversionService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private DccControlledFileQueryService queryService;
    @Resource
    private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Resource
    private DccControlledFileMessageDeliveryService messageDeliveryService;
    @Resource
    private DccObsoleteFileStorageService obsoleteFileStorageService;
    @Resource
    private DccControlledContentAdapter platformAdapter;
    @Resource
    private DccControlledFilePendingActionGuard pendingActionGuard;
    @Resource
    private DccControlledFileSignatureBindingService signatureBindingService;
    @Resource
    private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper routeSnapshotMapper;
    @Resource
    private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper approvalSignatureMapper;
    @Resource
    private DccElectronicSignatureManagementService signatureManagementService;
    @Resource
    private DccPublicationFollowupService publicationFollowupService;
    @Resource
    private DccControlledFileFinalizationFailureService finalizationFailureService;
    @Resource
    private BpmTaskService bpmTaskService;
    @Resource
    private DccControlledFileLifecycleService lifecycleService;
    @Resource
    private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTaskAssigneeSnapshotMapper taskAssigneeSnapshotMapper;
    @Resource private DccBoundWorkflowContractResolver boundContracts;
    @Resource private DccDelayedEffectiveConfirmationEvidence confirmationEvidence;
    @Resource private cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService processes;
    @Resource private org.flowable.engine.RuntimeService runtime;
    @Resource private org.flowable.engine.HistoryService history;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAttachmentMapper trainingAttachments;

    @Override
    public void handleProcessInstanceStatusChanged(BpmProcessInstanceStatusEvent event) {
        Long fileId = Long.valueOf(event.getBusinessKey());
        DccControlledFileDO file = controlledFileMapper.selectById(fileId);
        if (file == null) {
            return;
        }
        if (BpmProcessInstanceStatusEnum.APPROVE.getStatus().equals(event.getStatus())) {
            if (isOrdinaryApprovalCandidate(file)) {
                finalizeOrdinaryApproval(file, event);
                return;
            }
            platformAdapter.recordFinalizationStarted(file, event.getActorUserId(), event.getId());
            runFinalizationWithFailureHandling(file, event.getActorUserId(), event.getId());
            return;
        }
        if (BpmProcessInstanceStatusEnum.REJECT.getStatus().equals(event.getStatus())) {
            markRejectedAfterApprovalEvent(file, event);
            return;
        }
        if (BpmProcessInstanceStatusEnum.CANCEL.getStatus().equals(event.getStatus())
                && WITHDRAW_EVENT_STATUSES.contains(file.getStatus())) {
            controlledFileMapper.updateById(DccControlledFileDO.builder()
                    .id(fileId)
                    .status(DccControlledFileStatusEnum.WITHDRAWN.getStatus())
                    .rejectReason(event.getReason())
                    .build());
        }
    }

    @Override
    public void retryStamp(Long userId, Long id) {
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (!DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus().equals(file.getStatus())) {
            throw exception(CONTROLLED_FILE_STAMP_RETRY_NOT_ALLOWED);
        }
        requirePublishPermission(userId, file);
        DccControlledFileDO[] attempted = new DccControlledFileDO[1];
        String[] retryKey = new String[1];
        try {
            transactionTemplate.executeWithoutResult(ignored -> {
                Long tenant = TenantContextHolder.getRequiredTenantId();
                var master = controlledFileMasterMapper.selectByIdForUpdate(file.getMasterId());
                var current = controlledFileMapper.selectByIdAndTenantForUpdate(tenant,id);
                if (master == null || current == null || !Objects.equals(master.getId(),current.getMasterId())
                        || !Objects.equals(master.getTenantId(),tenant))
                    throw exception(CONTROLLED_FILE_NOT_EXISTS);
                requirePublishPermission(userId,current);
                if (current.getControlledTime() != null) return;
                if (!DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus().equals(current.getStatus()))
                    throw exception(CONTROLLED_FILE_STAMP_RETRY_NOT_ALLOWED);
                requireRetryApprovalEvidence(current);
                retryKey[0] = platformAdapter.nextFinalizationRetryEventKey(current);
                attempted[0] = current;
                platformAdapter.recordFinalizationRetried(current,userId,retryKey[0]);
                finalizeRevision(current,master,false,userId,retryKey[0],true);
            });
        } catch (RuntimeException failure) {
            if (attempted[0] == null) throw failure;
            String reason = resolveFailureReason(failure);
            scheduleFailureAfterRollback(attempted[0],DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus(),
                    userId,reason,retryKey[0]);
            throw toFinalizationException(reason,failure);
        }
    }

    @Override
    public void precheckPublishControlledFile(Long userId, Long id) {
        DccControlledFileDO target = controlledFileMapper.selectById(id);
        if (target == null) throw exception(CONTROLLED_FILE_NOT_EXISTS);
        if (!isExternalReviewProcess(target)) {
            throw exception(CONTROLLED_FILE_PUBLISH_NOT_ALLOWED);
        }
        DccControlledFileDO file = requirePublishReadyCandidate(userId, id, true);
        validatePublishDistributionPlans(file);
    }

    @Override
    public void applyApprovedPublishControlledFile(Long userId, Long id, String eventKey) {
        String normalizedEventKey = StrUtil.blankToDefault(StrUtil.trim(eventKey), "dcc-publish:" + id);
        DccControlledFileDO[] attemptedFile = new DccControlledFileDO[1];
        try {
            transactionTemplate.executeWithoutResult(ignored -> {
                Long tenantId = TenantContextHolder.getRequiredTenantId();
                DccControlledFileDO file = requirePublishReadyCandidate(userId, id, false, true);
                DccControlledFileMasterDO master = controlledFileMasterMapper.selectByIdForUpdate(file.getMasterId());
                if (master == null) {
                    throw new IllegalStateException("Controlled file master is missing for finalization");
                }
                int updated = controlledFileMapper.transitionStatus(tenantId, id,
                        DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus(),
                        DccControlledFileStatusEnum.FINALIZING.getStatus(), userId);
                if (updated != 1) {
                    throw exception(CONTROLLED_FILE_PUBLISH_NOT_ALLOWED);
                }
                attemptedFile[0] = file;
                file.setStatus(DccControlledFileStatusEnum.FINALIZING.getStatus());
                file.setFinalizationError(null);
                platformAdapter.recordPublishFinalizationStarted(file, userId, normalizedEventKey);
                finalizeRevision(file, master, false, userId, normalizedEventKey, true);
            });
        } catch (RuntimeException ex) {
            if (attemptedFile[0] != null) {
                String failureReason = resolveFailureReason(ex);
                scheduleFailureAfterRollback(attemptedFile[0],
                        DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus(), userId,
                        failureReason, normalizedEventKey);
                throw toFinalizationException(failureReason, ex);
            }
            throw ex;
        }
    }

    @Override
    public void releaseManualDistribution(Long userId, Long id) {
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (isThreeWorkflowUploadOrRevision(file)) {
            releaseThreeWorkflowManualDistribution(userId, id);
            return;
        }
        if (DccControlledFileProcessTypeEnum.CONTROLLED_FILE.getCode().equals(file.getProcessType())
                || !DccControlledFileStatusEnum.PENDING_MANUAL_DISTRIBUTION.getStatus().equals(file.getStatus())
                || !permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                DccFileCategoryPermissionActionEnum.DISTRIBUTE)) {
            throw exception(CONTROLLED_FILE_MANUAL_RELEASE_NOT_ALLOWED);
        }
        DccFileCategoryDO category = categoryMapper.selectById(file.getCategoryId());
        if (category == null) {
            throw exception(FILE_CATEGORY_NOT_EXISTS);
        }
        if (!allTrainingAcknowledged(file.getId())) {
            throw exception(CONTROLLED_FILE_MANUAL_RELEASE_NOT_ALLOWED);
        }
        DccControlledFileMasterDO master = controlledFileMasterMapper.selectById(file.getMasterId());
        if (master == null) {
            throw new IllegalStateException("Controlled file master is missing for manual release");
        }
        List<ResolvedDistributionPlan> distributionPlans = resolveDistributionPlans(file, category, false);
        PublishedArtifact publishedArtifact = resolveStampedPublishedArtifact(file, false);
        transactionTemplate.executeWithoutResult(status ->
                activateRevision(file, master, category, publishedArtifact, distributionPlans, userId,
                        "dcc-manual-release:" + id));
    }

    private void releaseThreeWorkflowManualDistribution(Long userId, Long id) {
        transactionTemplate.executeWithoutResult(status -> {
            Long tenantId = TenantContextHolder.getRequiredTenantId();
            DccControlledFileDO file = controlledFileMapper.selectByIdAndTenantForUpdate(tenantId, id);
            if (file == null) {
                throw exception(CONTROLLED_FILE_NOT_EXISTS);
            }
            if (!isThreeWorkflowUploadOrRevision(file)
                    || file.getControlledTime() == null
                    || file.getActivatedTime() == null
                    || userId==null || !permissionApi.hasAnyRoles(userId,DOC_CONTROL_ROLE)
                    || !DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus())
                    || !permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                    DccFileCategoryPermissionActionEnum.DISTRIBUTE)
                    || Boolean.TRUE.equals(file.getNeedTraining()) && file.getTrainingRecordFileId() == null) {
                throw exception(CONTROLLED_FILE_MANUAL_RELEASE_NOT_ALLOWED);
            }
            if (file.getDistributedTime() != null) return;
            if(file.getDistributionPayloadHash()==null || !file.getDistributionPayloadHash().matches("[a-fA-F0-9]{64}"))
                throw new IllegalStateException("本文件尚未确认下发部门、人员和方式");
            DccFileCategoryDO category = categoryMapper.selectById(file.getCategoryId());
            if (category == null) throw exception(FILE_CATEGORY_NOT_EXISTS);
            // Frozen recipient rows are persisted and validated by the actual distribution command.
            var confirmed=distributionMapper.selectListByControlledFileId(id);
            if(confirmed==null || confirmed.isEmpty()) throw new IllegalStateException("本文件已确认的下发名单缺失");
            List<ResolvedDistributionPlan> plans = resolveSavedDistributionPlans(confirmed, false);
            createDistributionRecords(file, category, plans);
            if (controlledFileMapper.updateById(DccControlledFileDO.builder().id(id)
                    .distributedTime(lifecycleService.currentTime()).build()) != 1)
                throw new IllegalStateException("下发记录保存失败");
        });
    }

    private boolean isThreeWorkflowUploadOrRevision(DccControlledFileDO file) {
        return file != null && (DccControlledFileProcessDefinitionKeys.UPLOAD.equals(file.getProcessDefinitionKey())
                || DccControlledFileProcessDefinitionKeys.REVISION.equals(file.getProcessDefinitionKey()));
    }

    @Override
    public void activateWithoutApproval(Long id) {
        activateWithoutApproval(id, false);
    }

    @Override
    public void activateWithoutApproval(Long id, boolean skipGovernance) {
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        runFinalizationWithFailureHandling(id, skipGovernance);
    }

    @Override
    public DccControlledFileBinary readPreviewFile(Long userId, Long id) {
        throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
    }

    @Override
    public DccDownloadFileBinary readDownloadFile(Long userId, Long id, Boolean nonControlledWarningConfirmed,
                                                  String downloadRequestId, DccRequestAuditContext auditContext) {
        return queryService.readDownloadFile(userId, id, nonControlledWarningConfirmed, downloadRequestId,
                auditContext);
    }

    private void finalizeOrdinaryApproval(DccControlledFileDO initial, BpmProcessInstanceStatusEvent event) {
        validateApprovalEventIdentity(initial, event);
        if (DccControlledFileStatusEnum.ACTIVE.getStatus().equals(initial.getStatus())
                || initial.getControlledTime() != null) {
            requirePublicCompletedReplay(initial,event);
            return;
        }
        String expectedStatus = initial.getStatus();
        boolean[] publicAttempt = {false};
        boolean[] legacyFailureHandling = {!isThreeWorkflowUploadOrRevision(initial)};
        boolean[] synchronousEnd = {false};
        Long[] operationActor = {event.getActorUserId()};
        boolean[] derivationStarted = {false};
        try {
            transactionTemplate.executeWithoutResult(ignored -> {
                Long tenantId = TenantContextHolder.getRequiredTenantId();
                DccControlledFileMasterDO master = controlledFileMasterMapper.selectByIdForUpdate(initial.getMasterId());
                DccControlledFileDO file = controlledFileMapper.selectByIdAndTenantForUpdate(tenantId, initial.getId());
                if (master == null || file == null || !Objects.equals(master.getId(), file.getMasterId())) {
                    throw exception(CONTROLLED_FILE_NOT_EXISTS);
                }
                validateApprovalEventIdentity(file, event);
                if (DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus())
                        || file.getControlledTime() != null) {requirePublicCompletedReplay(file,event);return;}
                var bound=isThreeWorkflowUploadOrRevision(file)?boundContracts.resolve(file):null;
                if(isThreeWorkflowUploadOrRevision(file) && bound==null) throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
                if(bound!=null) legacyFailureHandling[0]=bound.flowRule()==DccWorkflowDefinitionCatalog.FlowRule.LEGACY_V4_TRAINING_BEFORE_DOCUMENT_CONTROL;
                boolean publicNative=bound!=null && bound.trainingAfterDocumentControl();
                publicAttempt[0]=publicNative;
                if(publicNative) {synchronousEnd[0]=requirePublicEnd(file,event,bound);lifecycleService.requireReviewEffectiveSelection(file);}
                if (!DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus().equals(file.getStatus())
                        && !DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus().equals(file.getStatus())
                        && !(bound!=null && bound.delayedConfirmationSupported()
                            && DccWorkflowDatePolicy.DELAYED.equals(file.getEffectiveMode())
                            && DccDelayedEffectiveConfirmationEvidence.STATUS.equals(file.getStatus()))
                        && !isThreeWorkflowDocControlReview(file)) {
                    throw new IllegalStateException("DCC approval cannot finalize status " + file.getStatus());
                }
                boolean threeWorkflowDocControlReview = isThreeWorkflowDocControlReview(file);
                if ((threeWorkflowDocControlReview || publicNative)
                        ? file.getPublishedFileId() != null || file.getStampedFileId() != null
                        : file.getPublishedFileId() == null || file.getStampedFileId() == null) {
                    throw exception(CONTROLLED_FILE_PUBLISH_NOT_ALLOWED);
                }
                List<cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO> approvalSignatures =
                        approvalSignatureMapper.selectListByControlledFileId(file.getId());
                if(bound!=null) DccFrozenApprovalSignatures.requireComplete(file,
                        routeSnapshotMapper.selectListByControlledFileId(file.getId()), approvalSignatures,
                        taskAssigneeSnapshotMapper.selectListByControlledFileId(file.getId()),bound);
                else DccFrozenApprovalSignatures.requireComplete(file,
                        routeSnapshotMapper.selectListByControlledFileId(file.getId()),approvalSignatures,List.of());
                verifyApprovalSignatureEvidence(file, approvalSignatures,bound);
                NativeFinalizationActors actors=publicNative?requireNativeFinalizationActors(file,event,bound):null;
                if(actors!=null) operationActor[0]=actors.operationActorId();
                if (DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus().equals(file.getStatus())
                        || isThreeWorkflowDocControlReview(file)) {
                    LocalDateTime approvedAt = file.getApprovedTime() == null
                            ? LocalDateTime.now() : file.getApprovedTime();
                    if (controlledFileMapper.markReadyToPublishAfterApproval(tenantId, file.getId(), event.getId(),
                            event.getProcessDefinitionKey(), file.getStatus(), approvedAt, event.getActorUserId()) != 1) {
                        throw new IllegalStateException("DCC approval lost its status CAS");
                    }
                    file.setApprovedTime(approvedAt);
                    file.setStatus(DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus());
                }
                // READY_TO_PUBLISH is an internal transition in this transaction, never a separate user task.
                if(actors!=null) platformAdapter.recordFinalizationStarted(file,actors.approvalActorId(),actors.operationActorId(),event.getId());
                else platformAdapter.recordFinalizationStarted(file,event.getActorUserId(),event.getId());
                derivationStarted[0]=true;
                finalizeRevision(file, master, false, operationActor[0], event.getId(), true);
            });
        } catch (RuntimeException failure) {
            String reason = resolveFailureReason(failure);
            // Official synchronous End occurs before the engine command is committed. Both the last document
            // approval and the training trigger must return to their actual pre-command state on rollback.
            if(legacyFailureHandling[0] || publicAttempt[0] && derivationStarted[0] && !synchronousEnd[0])
                scheduleFailureAfterRollback(initial,expectedStatus,operationActor[0],reason,event.getId());
            throw toFinalizationException(reason, failure);
        }
    }

    private void markRejectedAfterApprovalEvent(DccControlledFileDO initial, BpmProcessInstanceStatusEvent event) {
        validateApprovalEventIdentity(initial, event);
        var initialBound=isThreeWorkflowUploadOrRevision(initial)?boundContracts.resolve(initial):null;
        if(isThreeWorkflowUploadOrRevision(initial) && initialBound==null)throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        if(initialBound!=null && initialBound.trainingAfterDocumentControl()) requirePublicRejectEvidence(initial,event,initialBound);
        if (shouldIgnoreRejectReplay(initial.getStatus())) {
            return;
        }
        if (!WITHDRAW_EVENT_STATUSES.contains(initial.getStatus())) {
            throw new IllegalStateException("DCC reject cannot transition status " + initial.getStatus());
        }
        transactionTemplate.executeWithoutResult(ignored -> {
            Long tenantId = TenantContextHolder.getRequiredTenantId();
            DccControlledFileDO file = controlledFileMapper.selectByIdAndTenantForUpdate(tenantId, initial.getId());
            if (file == null) {
                throw exception(CONTROLLED_FILE_NOT_EXISTS);
            }
                validateApprovalEventIdentity(file, event);
                var bound=isThreeWorkflowUploadOrRevision(file)?boundContracts.resolve(file):null;
                if(isThreeWorkflowUploadOrRevision(file) && bound==null)throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
                if(bound!=null && bound.trainingAfterDocumentControl()) requirePublicRejectEvidence(file,event,bound);
            if (shouldIgnoreRejectReplay(file.getStatus())) {
                return;
            }
            if (!WITHDRAW_EVENT_STATUSES.contains(file.getStatus())) {
                throw new IllegalStateException("DCC reject cannot transition status " + file.getStatus());
            }
            LocalDateTime rejectedTime = LocalDateTime.now();
            int updated = controlledFileMapper.markRejectedAfterApprovalEvent(tenantId, file.getId(), event.getId(),
                    file.getProcessDefinitionKey(), file.getStatus(), rejectedTime, event.getReason(), event.getActorUserId());
            if (updated != 1) {
                throw new IllegalStateException("DCC reject lost its status CAS");
            }
            file.setStatus(DccControlledFileStatusEnum.REJECTED.getStatus());
            file.setRejectedTime(rejectedTime);
            file.setRejectReason(event.getReason());
            platformAdapter.recordRejected(file, event.getActorUserId(), event.getReason(), event.getId());
        });
    }

    private boolean shouldIgnoreRejectReplay(String status) {
        return REJECT_REPLAY_IGNORED_STATUSES.contains(status);
    }

    private void verifyApprovalSignatureEvidence(DccControlledFileDO file,
            List<cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO> signatures) {
        verifyApprovalSignatureEvidence(file,signatures,null);
    }

    /** Rejection validates only the actual rejecting stage. Later approval stages have not happened. */
    private void requirePublicRejectEvidence(DccControlledFileDO file,BpmProcessInstanceStatusEvent event,
            DccBoundWorkflowContractResolver.BoundExecution bound) {
        if(!Objects.equals(file.getTenantId(),TenantContextHolder.getRequiredTenantId()) || !Objects.equals(file.getId().toString(),event.getBusinessKey())
                || !BpmProcessInstanceStatusEnum.REJECT.getStatus().equals(event.getStatus()) || StrUtil.isBlank(event.getReason()))
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        var rejected=Objects.requireNonNull(approvalSignatureMapper.selectListByControlledFileId(file.getId()),"本轮拒绝签名")
                .stream().filter(Objects::nonNull).filter(s->Objects.equals(file.getProcessInstanceId(),s.getProcessInstanceId())
                    && "REJECT".equals(s.getActionType()) && !Boolean.TRUE.equals(s.getDeleted())).toList();
        if(rejected.size()!=1) throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        var signature=rejected.get(0);String meaning=signature.getMeaningCode();
        String stage=meaning!=null&&meaning.endsWith("_REJECT")?meaning.substring(0,meaning.length()-7):null;
        if(stage==null || !Set.of("SUPERVISOR_REVIEW","MATRIX_REVIEW","MATRIX_APPROVAL","DOC_CONTROL_REVIEW").contains(stage)
                || !Objects.equals(signature.getControlledFileId(),file.getId()) || !Objects.equals(signature.getRevisionId(),file.getId())
                || !Objects.equals(signature.getVersionNo(),file.getVersionNo()) || !Objects.equals(signature.getActorId(),event.getActorUserId())
                || StrUtil.isBlank(signature.getComment())
                || !Objects.equals(cn.iocoder.yudao.module.bpm.enums.task.BpmReasonEnum.REJECT_TASK.format(signature.getComment()),event.getReason())
                || !Boolean.TRUE.equals(signature.getPasswordVerified())
                || signature.getSignedAt()==null || !"VALID".equals(signature.getEvidenceStatus()) || !"v4-workflow".equals(signature.getEvidencePayloadVersion()))
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        if(!"REJECTED".equals(file.getStatus()) && !("PENDING_"+stage).equals(file.getStatus()))
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        var task=bpmTaskService.getHistoricTask(signature.getTaskId());
        if(task==null || !Objects.equals(task.getId(),signature.getTaskId()) || !Objects.equals(task.getProcessInstanceId(),file.getProcessInstanceId())
                || !Objects.equals(task.getProcessDefinitionId(),bound.processDefinitionId()) || !String.valueOf(file.getTenantId()).equals(task.getTenantId())
                || !stage.equals(task.getTaskDefinitionKey()) || !String.valueOf(signature.getActorId()).equals(task.getAssignee())
                || task.getEndTime()==null || task.getTaskLocalVariables()==null
                || !Integer.valueOf(cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum.REJECT.getStatus()).equals(task.getTaskLocalVariables().get(
                    cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.TASK_VARIABLE_STATUS))
                || !Objects.equals(signature.getComment(),task.getTaskLocalVariables().get(
                    cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.TASK_VARIABLE_REASON)))
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        var candidates=Objects.requireNonNull(routeSnapshotMapper.selectListByControlledFileId(file.getId()),"冻结拒绝阶段").stream()
                .filter(row->stage.equals(row.getStageCode())).toList();
        if(candidates.size()!=1)throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        if("SUPERVISOR_REVIEW".equals(stage)) {
            var supervisor=candidates.get(0);
            if(!Objects.equals(file.getId(),supervisor.getControlledFileId()) || !Integer.valueOf(0).equals(supervisor.getStageNo())
                    || !Integer.valueOf(0).equals(supervisor.getStageOrder()) || !"USER".equals(supervisor.getCandidateSourceType())
                    || !Objects.equals(signature.getActorId(),supervisor.getCandidateSourceId())
                    || !String.valueOf(signature.getActorId()).equals(supervisor.getCandidateSourceIds())
                    || !String.valueOf(signature.getActorId()).equals(supervisor.getResolvedUserIds())
                    || !"ANY".equals(supervisor.getApproveMethod()) || Boolean.TRUE.equals(supervisor.getRequireAllApprovals()))
                throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        }
        if("MATRIX_REVIEW".equals(stage)) {
            var assigned=taskAssigneeSnapshotMapper.selectListByControlledFileId(file.getId()).stream().filter(row->
                    Objects.equals(row.getProcessInstanceId(),file.getProcessInstanceId()) && Objects.equals(row.getBpmTaskId(),task.getId())
                        && Objects.equals(row.getAssigneeUserId(),signature.getActorId())).toList();
            if(assigned.size()!=1) throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        } else if(candidates.get(0).getResolvedUserIds()==null || java.util.Arrays.stream(candidates.get(0).getResolvedUserIds().split(",",-1))
                .noneMatch(id->id.equals(String.valueOf(signature.getActorId())))) throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        var verified=signatureManagementService.verifySignatureEvidence(signature.getId());
        if(verified==null || !Objects.equals(verified.getSignatureId(),signature.getId()) || !"VALID".equals(verified.getVerificationStatus()))
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_INVALID);
        var process=processes.getProcessInstance(file.getProcessInstanceId());
        Map<String,Object> variables;
        if(process!=null) {
            if(!process.isEnded() || !runtime.getActiveActivityIds(process.getId()).isEmpty()) throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
            variables=process.getProcessVariables();
        } else {
            var completed=processes.getHistoricProcessInstance(file.getProcessInstanceId());
            if(completed==null || completed.getEndTime()==null)throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
            variables=completed.getProcessVariables();
        }
        if(variables==null || !BpmProcessInstanceStatusEnum.REJECT.getStatus().equals(variables.get(
                cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS))
                || !Objects.equals(event.getReason(),variables.get(cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_REASON))
                || !String.valueOf(signature.getActorId()).equals(String.valueOf(variables.get(
                    cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_APPROVER_USER_ID))))
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
    }

    private void verifyApprovalSignatureEvidence(DccControlledFileDO file,
            List<cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO> signatures,
            DccBoundWorkflowContractResolver.BoundExecution bound) {
        for (var signature : signatures) {
            if (signature == null || signature.getId() == null
                    || !Objects.equals(file.getId(), signature.getControlledFileId())
                    || !Objects.equals(file.getVersionNo(), signature.getVersionNo())
                    || !("APPROVE".equals(signature.getActionType()) || "ASSIGN".equals(signature.getActionType()))
                    || !"VALID".equals(signature.getEvidenceStatus())) {
                continue;
            }
            if(isThreeWorkflowUploadOrRevision(file)) {
                if(!Objects.equals(file.getProcessInstanceId(),signature.getProcessInstanceId())) continue;
                if(bound!=null && bound.delayedConfirmationSupported()
                        && DccDelayedEffectiveConfirmationEvidence.MEANING.equals(signature.getMeaningCode())) {
                    var confirmed=confirmationEvidence.requireConfirmed(file,bound);
                    if(!Objects.equals(confirmed.signatureId(),signature.getId()))
                        throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_INVALID);
                    continue;
                }
                if(!"v4-workflow".equals(signature.getEvidencePayloadVersion()))
                    throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_INVALID);
                if("APPROVE".equals(signature.getActionType())) requireCompletedNativeTask(file,signature,bound);
            }
            var verification = signatureManagementService.verifySignatureEvidence(signature.getId());
            if (verification == null || !Objects.equals(signature.getId(), verification.getSignatureId())
                    || !"VALID".equals(verification.getVerificationStatus())) {
                throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_INVALID);
            }
        }
    }

    private void requireCompletedNativeTask(DccControlledFileDO file,
            cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO signature) {
        requireCompletedNativeTask(file,signature,null);
    }

    private void requireCompletedNativeTask(DccControlledFileDO file,
            cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO signature,
            DccBoundWorkflowContractResolver.BoundExecution bound) {
        String meaning=signature.getMeaningCode();
        String stage=meaning!=null && meaning.endsWith("_APPROVE")?meaning.substring(0,meaning.length()-"_APPROVE".length()):null;
        var stages=bound!=null && bound.trainingAfterDocumentControl()
                ?Set.of("SUPERVISOR_REVIEW","MATRIX_REVIEW","MATRIX_APPROVAL","DOC_CONTROL_REVIEW")
                :Set.of("MATRIX_REVIEW","MATRIX_APPROVAL","DOC_CONTROL_REVIEW");
        if(stage==null || !stages.contains(stage))
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        var task=bpmTaskService.getHistoricTask(signature.getTaskId());
        var local=task==null?null:task.getTaskLocalVariables();
        if(task==null || !Objects.equals(signature.getTaskId(),task.getId())
                || !Objects.equals(file.getProcessInstanceId(),task.getProcessInstanceId())
                || !String.valueOf(TenantContextHolder.getRequiredTenantId()).equals(task.getTenantId())
                || bound!=null && !Objects.equals(bound.processDefinitionId(),task.getProcessDefinitionId())
                || !stage.equals(task.getTaskDefinitionKey()) || signature.getActorId()==null
                || !String.valueOf(signature.getActorId()).equals(task.getAssignee())
                || task.getEndTime()==null || local==null
                || !Integer.valueOf(cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum.APPROVE.getStatus()).equals(
                        local.get(cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.TASK_VARIABLE_STATUS)))
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
    }

    /**
     * A failed finalization may be retried only when the approval evidence survived
     * the failed derived-file operation. The failure marker itself is not approval
     * evidence and must never be enough to activate a file.
     */
    private void requireRetryApprovalEvidence(DccControlledFileDO file) {
        List<cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO> signatures =
                approvalSignatureMapper.selectListByControlledFileId(file.getId());
        var bound=isThreeWorkflowUploadOrRevision(file)?boundContracts.resolve(file):null;
        if(isThreeWorkflowUploadOrRevision(file) && bound==null)throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        if(bound!=null) DccFrozenApprovalSignatures.requireComplete(file,routeSnapshotMapper.selectListByControlledFileId(file.getId()),
                signatures,taskAssigneeSnapshotMapper.selectListByControlledFileId(file.getId()),bound);
        else DccFrozenApprovalSignatures.requireComplete(file,routeSnapshotMapper.selectListByControlledFileId(file.getId()),signatures,List.of());
        verifyApprovalSignatureEvidence(file,signatures,bound);
        if(bound!=null && bound.trainingAfterDocumentControl()) {
            requireCommittedPublicApproval(file,bound);
            var doc=lifecycleService.requirePublicDocumentControlEvidence(file,bound);
            var confirmed=delayedConfirmation(file,bound);
            var completed=processes.getHistoricProcessInstance(file.getProcessInstanceId());
            if(!String.valueOf(confirmed==null?doc.getActorId():confirmed.actorId()).equals(String.valueOf(completed.getProcessVariables().get(
                    cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_APPROVER_USER_ID)))
                    || file.getApprovedTime()==null || file.getNeedTraining()==null
                    || !Objects.equals(file.getNeedTraining(),completed.getProcessVariables().get("needTraining")))
                throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
            if(file.getNeedTraining()==null) throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
            var activities=history.createHistoricActivityInstanceQuery().processInstanceId(file.getProcessInstanceId()).activityId("TRAINING").list();
            if(Boolean.TRUE.equals(file.getNeedTraining())) {
                if(activities.size()!=1 || activities.get(0).getEndTime()==null) throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
                requirePublicTrainingRecord(file,bound);
            } else if(!activities.isEmpty() || file.getTrainingRecordFileId()!=null) throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
            lifecycleService.requireReviewEffectiveSelection(file);
        }
    }

    private boolean requirePublicEnd(DccControlledFileDO file,BpmProcessInstanceStatusEvent event,
            DccBoundWorkflowContractResolver.BoundExecution bound) {
        var confirmed=delayedConfirmation(file,bound);
        if(!Objects.equals(file.getTenantId(),TenantContextHolder.getRequiredTenantId())
                || !Objects.equals(file.getId().toString(),event.getBusinessKey())
                || !BpmProcessInstanceStatusEnum.APPROVE.getStatus().equals(event.getStatus())
                || file.getNeedTraining()==null || file.getActivatedTime()!=null)
            throw exception(CONTROLLED_FILE_PUBLISH_NOT_ALLOWED);
        var process=processes.getProcessInstance(file.getProcessInstanceId());
        Map<String,Object> variables;
        if(process!=null) {
            if(!process.isEnded() || process.isSuspended() || !runtime.getActiveActivityIds(process.getId()).isEmpty())
                throw new IllegalStateException("PUBLIC正式End事件没有真实引擎终结事实");
            variables=process.getProcessVariables();
        } else {
            var completed=processes.getHistoricProcessInstance(file.getProcessInstanceId());
            if(completed==null || completed.getEndTime()==null || !Objects.equals(bound.processDefinitionId(),completed.getProcessDefinitionId()))
                throw new IllegalStateException("PUBLIC正式End历史缺少真实完成事实");
            variables=completed.getProcessVariables();
        }
        if(variables==null || !BpmProcessInstanceStatusEnum.APPROVE.getStatus().equals(variables.get(
                cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS))
                || !Objects.equals(file.getNeedTraining(),variables.get("needTraining"))
                || !Objects.equals(String.valueOf(event.getActorUserId()),String.valueOf(variables.get(
                    cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_APPROVER_USER_ID))))
            throw new IllegalStateException("PUBLIC正式End状态或审批来源与冻结实例不一致");
        // During PROCESS_COMPLETED the completed doc task can remain in the SQL query until command flush.
        for(var pending:Objects.requireNonNull(bpmTaskService.getRunningTaskListByProcessInstanceId(file.getProcessInstanceId(),null,null),"本轮任务查询")) {
            if(confirmed!=null && Objects.equals(pending.getId(),confirmed.taskId())
                    && DccDelayedEffectiveConfirmationEvidence.STAGE.equals(pending.getTaskDefinitionKey())) {
                // The sole verifier proves the exact signed engine-approved task in the real End command.
                continue;
            }
            var completed=bpmTaskService.getHistoricTask(pending.getId());
            if(!"DOC_CONTROL_REVIEW".equals(pending.getTaskDefinitionKey()) || completed==null || completed.getEndTime()==null
                    || !Objects.equals(pending.getId(),completed.getId()) || !Objects.equals(file.getProcessInstanceId(),completed.getProcessInstanceId())
                    || !Objects.equals(bound.processDefinitionId(),completed.getProcessDefinitionId())
                    || !String.valueOf(file.getTenantId()).equals(completed.getTenantId())
                    || completed.getTaskLocalVariables()==null || !Integer.valueOf(2).equals(completed.getTaskLocalVariables().get(
                        cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.TASK_VARIABLE_STATUS)))
                throw new IllegalStateException("PUBLIC正式End仍有未完成或非本轮的审批任务");
        }
        var training=history.createHistoricActivityInstanceQuery().processInstanceId(file.getProcessInstanceId()).activityId("TRAINING").list();
        if(Boolean.TRUE.equals(file.getNeedTraining())) {
            if(!(DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus().equals(file.getStatus())
                    || confirmed!=null && DccDelayedEffectiveConfirmationEvidence.STATUS.equals(file.getStatus())) || training.size()!=1
                    || training.get(0).getEndTime()==null || !Objects.equals(bound.processDefinitionId(),training.get(0).getProcessDefinitionId()))
                throw new IllegalStateException("PUBLIC培训结束缺少实际receive完成或本轮READY事实");
        } else if(!training.isEmpty() || file.getTrainingRecordFileId()!=null
                || !(isThreeWorkflowDocControlReview(file) || confirmed!=null
                    && (DccDelayedEffectiveConfirmationEvidence.STATUS.equals(file.getStatus())
                        || DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus().equals(file.getStatus()))))
            throw new IllegalStateException("PUBLIC无需培训结束与本轮正式节点事实不一致");
        if(file.getApprovedTime()==null) throw new IllegalStateException("PUBLIC受控缺少已保存内容批准时间");
        return process!=null;
    }

    private void requireCommittedPublicApproval(DccControlledFileDO file,DccBoundWorkflowContractResolver.BoundExecution bound) {
        if(processes.getProcessInstance(file.getProcessInstanceId())!=null)
            throw new IllegalStateException("PUBLIC技术重试不能跳过仍活跃的原审批或培训节点");
        var completed=processes.getHistoricProcessInstance(file.getProcessInstanceId());
        if(completed==null || completed.getEndTime()==null || !Objects.equals(bound.processDefinitionId(),completed.getProcessDefinitionId())
                || completed.getProcessVariables()==null || !BpmProcessInstanceStatusEnum.APPROVE.getStatus().equals(completed.getProcessVariables().get(
                    cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS)))
            throw new IllegalStateException("PUBLIC技术重试缺少已提交的真实审批完成事实");
    }

    private record NativeFinalizationActors(Long approvalActorId,Long operationActorId,Long documentControlSignatureId) {}

    private void requirePublicCompletedReplay(DccControlledFileDO file,BpmProcessInstanceStatusEvent event) {
        // Completed historical rows are never migrated/re-signed merely to apply new contract metadata.
        if(file.getPreparedWorkflowContract()==null && file.getPreparedProcessDefinitionId()==null && file.getPreparedDefinitionDigest()==null)return;
        var bound=boundContracts.resolve(file);
        if(bound==null) throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        if(!bound.trainingAfterDocumentControl())return;
        if(file.getControlledTime()==null || file.getPublishedFileId()==null || file.getStampedFileId()==null)
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        var doc=lifecycleService.requirePublicDocumentControlEvidence(file,bound);
        var confirmed=delayedConfirmation(file,bound);
        if(confirmed!=null && !Objects.equals(file.getControlledTime(),confirmed.signedAt()))
            throw new IllegalStateException("已受控延迟事件与原确认签名时间不一致");
        if(!Objects.equals(confirmed==null?doc.getActorId():confirmed.actorId(),event.getActorUserId()) || !BpmProcessInstanceStatusEnum.APPROVE.getStatus().equals(event.getStatus()))
            throw new IllegalStateException("PUBLIC已受控事件重放审批来源不一致");
    }

    private NativeFinalizationActors requireNativeFinalizationActors(DccControlledFileDO file,BpmProcessInstanceStatusEvent event,
            DccBoundWorkflowContractResolver.BoundExecution bound) {
        var doc=lifecycleService.requirePublicDocumentControlEvidence(file,bound);
        var confirmed=delayedConfirmation(file,bound);
        Long approvalActor=confirmed==null?doc.getActorId():confirmed.actorId();
        if(!Objects.equals(approvalActor,event.getActorUserId()))
            throw new IllegalStateException("正式End审批来源与真实文控签名人不一致");
        var login=cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser();
        if(login==null || !Objects.equals(login.getTenantId(),file.getTenantId()) || login.getId()==null || login.getId()<=0
                || !Objects.equals(login.getUserType(),cn.iocoder.yudao.framework.common.enums.UserTypeEnum.ADMIN.getValue()))
            throw new IllegalStateException("PUBLIC同步受控需要本租户真实命令操作者");
        var account=adminUserApi.getUser(login.getId());
        if(account==null || !Objects.equals(account.getId(),login.getId()) || !Objects.equals(account.getTenantId(),file.getTenantId())
                || !Integer.valueOf(0).equals(account.getStatus()))
            throw new IllegalStateException("PUBLIC受控操作者不是本租户当前启用账户");
        if(confirmed!=null) {
            if(!Objects.equals(login.getId(),confirmed.actorId()))
                throw new IllegalStateException("延迟确认End命令操作者与真实原编制人签名不一致");
            if(Boolean.TRUE.equals(file.getNeedTraining())) requirePublicTrainingRecord(file,bound);
        } else if(Boolean.TRUE.equals(file.getNeedTraining())) {
            var record=requirePublicTrainingRecord(file,bound);
            if(!Objects.equals(record.getUploadedBy(),login.getId()) || !Objects.equals(file.getRequesterId(),login.getId()))
                throw new IllegalStateException("PUBLIC培训触发操作者与本轮实际上传事实不一致");
        } else if(!Objects.equals(login.getId(),doc.getActorId()))
            throw new IllegalStateException("PUBLIC文控直接结束操作者与真实签名命令不一致");
        return new NativeFinalizationActors(approvalActor,login.getId(),doc.getId());
    }

    private DccDelayedEffectiveConfirmationEvidence.VerifiedConfirmation delayedConfirmation(
            DccControlledFileDO file,DccBoundWorkflowContractResolver.BoundExecution bound) {
        if(!DccWorkflowDatePolicy.DELAYED.equals(file.getEffectiveMode())) return null;
        return confirmationEvidence.requireConfirmed(file,bound);
    }

    private cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAttachmentDO requirePublicTrainingRecord(
            DccControlledFileDO file,DccBoundWorkflowContractResolver.BoundExecution bound) {
        var record=trainingAttachments.selectTrainingRecordForUpdate(file.getTenantId(),file.getId(),file.getProcessInstanceId());
        if(record==null || !Objects.equals(record.getTenantId(),file.getTenantId()) || !Objects.equals(record.getControlledFileId(),file.getId())
                || !"TRAINING_RECORD".equals(record.getAttachmentPurpose()) || !Objects.equals(record.getApplicationProcessInstanceId(),bound.processInstanceId())
                || !Objects.equals(record.getUploadedBy(),file.getRequesterId()) || record.getUploadedAt()==null
                || !Objects.equals(record.getStorageFileId(),file.getTrainingRecordFileId()) || record.getStorageFileId()==null
                || Boolean.TRUE.equals(record.getDeleted()) || StrUtil.hasBlank(record.getFileSha256(),record.getOriginalFileName(),record.getContentType()))
            throw new IllegalStateException("PUBLIC培训完成缺少本轮原上传人正式附件事实");
        var physical=fileService.getFile(record.getStorageFileId());
        if(physical==null || physical.getConfigId()==null || StrUtil.isBlank(physical.getPath()) || !Objects.equals(physical.getId(),record.getStorageFileId())
                || !Objects.equals(physical.getSize(),record.getFileSize()) || !Objects.equals(physical.getName(),record.getOriginalFileName())
                || !Objects.equals(physical.getType(),record.getContentType()))
            throw new IllegalStateException("PUBLIC培训附件正式物理文件身份缺失");
        try {
            var content=fileService.getFileContent(physical.getConfigId(),physical.getPath());
            if(content==null || !record.getFileSha256().equals(cn.hutool.crypto.digest.DigestUtil.sha256Hex(content)))
                throw new IllegalStateException("PUBLIC培训附件物理内容与原保存摘要不一致");
        } catch(RuntimeException failure){throw failure;}
        catch(Exception failure){throw new IllegalStateException("PUBLIC培训附件读取失败",failure);}
        return record;
    }

    private boolean isOrdinaryApprovalCandidate(DccControlledFileDO file) {
        return DccControlledFileProcessTypeEnum.CONTROLLED_FILE.getCode().equals(file.getProcessType())
                && (DccControlledFileChangeTypeEnum.NEW.getCode().equals(file.getChangeType())
                || DccControlledFileChangeTypeEnum.REVISION.getCode().equals(file.getChangeType()));
    }

    private boolean isThreeWorkflowDocControlReview(DccControlledFileDO file) {
        return file != null
                && DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus().equals(file.getStatus())
                && (DccControlledFileProcessDefinitionKeys.UPLOAD.equals(file.getProcessDefinitionKey())
                || DccControlledFileProcessDefinitionKeys.REVISION.equals(file.getProcessDefinitionKey()));
    }

    private DccControlledFileDO requirePublishReadyCandidate(Long userId, Long id, boolean enforcePendingActionGuard) {
        return requirePublishReadyCandidate(userId, id, enforcePendingActionGuard, false);
    }

    private DccControlledFileDO requirePublishReadyCandidate(Long userId, Long id,
                                                             boolean enforcePendingActionGuard,
                                                             boolean lockForUpdate) {
        DccControlledFileDO file = lockForUpdate
                ? controlledFileMapper.selectByIdAndTenantForUpdate(TenantContextHolder.getRequiredTenantId(), id)
                : controlledFileMapper.selectById(id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (!isExternalReviewProcess(file)) {
            throw exception(CONTROLLED_FILE_PUBLISH_NOT_ALLOWED);
        }
        if (!DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus().equals(file.getStatus())) {
            throw exception(CONTROLLED_FILE_PUBLISH_NOT_ALLOWED);
        }
        requirePublishPermission(userId, file);
        DccControlledFileMasterDO master = controlledFileMasterMapper.selectById(file.getMasterId());
        if (master == null) {
            throw exception(CONTROLLED_FILE_PUBLISH_NOT_ALLOWED);
        }
        DccControlledFileDO currentActive = resolvePreviousActiveRevision(master, file.getId());
        assertCandidateAdvancesCurrentActive(file, currentActive);
        if (enforcePendingActionGuard) {
            pendingActionGuard.assertNoPendingBusinessAction(file);
        }
        return file;
    }

    private boolean isExternalReviewProcess(DccControlledFileDO file) {
        return file != null
                && DccControlledFileProcessTypeEnum.EXTERNAL_REVIEW.getCode().equals(file.getProcessType());
    }

    private void requirePublishPermission(Long userId, DccControlledFileDO file) {
        if (userId == null || !permissionApi.hasAnyRoles(userId, DOC_CONTROL_ROLE)
                || !permissionApi.hasAnyPermissions(userId, APPROVE_PERMISSION)
                || !permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                DccFileCategoryPermissionActionEnum.APPROVE)) {
            throw exception(CONTROLLED_FILE_PUBLISH_NOT_ALLOWED);
        }
    }

    private void validatePublishDistributionPlans(DccControlledFileDO file) {
        DccFileCategoryDO category = categoryMapper.selectById(file.getCategoryId());
        if (category == null) {
            throw exception(FILE_CATEGORY_NOT_EXISTS);
        }
        resolveDistributionPlans(file, category, Boolean.TRUE.equals(category.getTrainingRequired()));
    }

    private void runFinalizationWithFailureHandling(Long fileId) {
        runFinalizationWithFailureHandling(fileId, false);
    }

    private void runFinalizationWithFailureHandling(DccControlledFileDO file, Long actorId, String eventKey) {
        String expectedStatus = file.getStatus();
        try {
            transactionTemplate.executeWithoutResult(status ->
                    finalizeRevision(file.getId(), false, actorId, eventKey, true));
        } catch (RuntimeException ex) {
            String failureReason = resolveFailureReason(ex);
            scheduleFailureAfterRollback(file, expectedStatus, actorId, failureReason, eventKey);
            throw toFinalizationException(failureReason, ex);
        }
    }

    private void runFinalizationWithFailureHandling(Long fileId, boolean skipGovernance) {
        DccControlledFileDO file = controlledFileMapper.selectById(fileId);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        String expectedStatus = file.getStatus();
        try {
            transactionTemplate.executeWithoutResult(status -> finalizeRevision(fileId, skipGovernance));
        } catch (RuntimeException ex) {
            String failureReason = resolveFailureReason(ex);
            scheduleFailureAfterRollback(file, expectedStatus, null, failureReason,
                    "dcc-finalization:" + fileId);
            throw toFinalizationException(failureReason, ex);
        }
    }

    private void finalizeRevision(Long fileId) {
        finalizeRevision(fileId, false);
    }

    private void finalizeRevision(Long fileId, boolean skipGovernance) {
        finalizeRevision(fileId, skipGovernance, null, "dcc-finalization:" + fileId, false);
    }

    private void finalizeRevision(Long fileId, boolean skipGovernance, Long actorId, String eventKey,
                                  boolean bindSignatureEvidence) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        DccControlledFileDO identity = controlledFileMapper.selectById(fileId);
        if (identity == null || !Objects.equals(identity.getTenantId(),tenantId)) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        DccControlledFileMasterDO master = controlledFileMasterMapper.selectByIdForUpdate(identity.getMasterId());
        DccControlledFileDO file = controlledFileMapper.selectByIdAndTenantForUpdate(tenantId,fileId);
        if (master == null || file == null || !Objects.equals(master.getId(),file.getMasterId())
                || !Objects.equals(master.getTenantId(),tenantId)) {
            throw new IllegalStateException("Controlled file identity changed during finalization locking");
        }
        finalizeRevision(file, master, skipGovernance, actorId, eventKey, bindSignatureEvidence);
    }

    private void finalizeRevision(DccControlledFileDO file, DccControlledFileMasterDO master,
                                  boolean skipGovernance, Long actorId, String eventKey,
                                  boolean bindSignatureEvidence) {
        if (isObsoleteWorkflow(file)) {
            if (DccControlledFileStatusEnum.OBSOLETE.getStatus().equals(file.getStatus())
                    && master.getCurrentActiveControlledFileId() == null) {
                return;
            }
            finalizeObsoleteWorkflow(file, master, actorId, eventKey);
            return;
        }
        if (file.getControlledTime() != null) return;
        if (file.getPublishedFileId() != null
                && file.getStampedFileId() != null
                && (DccControlledFileStatusEnum.TRAINING_IN_PROGRESS.getStatus().equals(file.getStatus())
                || DccControlledFileStatusEnum.PENDING_MANUAL_DISTRIBUTION.getStatus().equals(file.getStatus())
                || (DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus())
                && Objects.equals(master.getCurrentActiveControlledFileId(), file.getId())))) {
            return;
        }
        DccFileCategoryDO category = categoryMapper.selectById(file.getCategoryId());
        if (category == null) {
            throw exception(FILE_CATEGORY_NOT_EXISTS);
        }
        boolean ordinaryFile = DccControlledFileProcessTypeEnum.CONTROLLED_FILE.getCode().equals(file.getProcessType());
        List<ResolvedDistributionPlan> distributionPlans = skipGovernance || ordinaryFile ? List.of()
                : resolveDistributionPlans(file, category, Boolean.TRUE.equals(category.getTrainingRequired()));
        PublishedArtifact publishedArtifact = resolveStampedPublishedArtifact(file, skipGovernance);
        if (bindSignatureEvidence) {
            signatureBindingService.bindPublishedCopy(file, publishedArtifact.publishedFileId(), actorId, eventKey);
        }
        if (skipGovernance) {
            activateRevisionWithoutGovernance(file, master, publishedArtifact, actorId, eventKey);
            return;
        }
        if (!ordinaryFile && Boolean.TRUE.equals(category.getTrainingRequired())) {
            prepareTrainingGatedRevision(file, category, publishedArtifact, distributionPlans);
            return;
        }
        activateRevision(file, master, category, publishedArtifact, distributionPlans, actorId, eventKey);
    }

    private boolean isObsoleteWorkflow(DccControlledFileDO file) {
        return StrUtil.equalsIgnoreCase(file.getChangeType(), DccControlledFileChangeTypeEnum.OBSOLETE.getCode());
    }

    private void finalizeObsoleteWorkflow(DccControlledFileDO file, DccControlledFileMasterDO master,
                                          Long actorId, String eventKey) {
        DccControlledFileDO previousActive = resolvePreviousActiveRevision(master, file.getId());
        if (previousActive == null) {
            throw new IllegalStateException("Previous active revision is missing for obsolete finalization");
        }
        obsoleteFileStorageService.moveControlledFileArtifactsToObsoleteFolder(previousActive);
        LocalDateTime now = LocalDateTime.now();
        String reason = StrUtil.blankToDefault(file.getObsoleteReason(), "dcc controlled file obsoleted");
        controlledFileMapper.updateById(DccControlledFileDO.builder()
                .id(previousActive.getId())
                .status(DccControlledFileStatusEnum.OBSOLETE.getStatus())
                .obsoletedBy(actorId)
                .obsoletedTime(now)
                .obsoleteReason(reason)
                .build());
        controlledFileMapper.updateById(DccControlledFileDO.builder()
                .id(file.getId())
                .approvedTime(now)
                .status(DccControlledFileStatusEnum.OBSOLETE.getStatus())
                .obsoletedBy(actorId)
                .obsoletedTime(now)
                .obsoleteReason(reason)
                .finalizationError("")
                .build());
        controlledFileMasterMapper.updateById(DccControlledFileMasterDO.builder()
                .id(master.getId())
                .currentActiveControlledFileId(null)
                .status(DccControlledFileMasterStatusEnum.OBSOLETE_CHAIN.getCode())
                .build());
        obsoleteAuditMapper.insert(DccControlledFileObsoleteAuditDO.builder()
                .controlledFileId(previousActive.getId())
                .operatorId(actorId)
                .obsoleteReason(reason)
                .statusBefore(previousActive.getStatus())
                .statusAfter(DccControlledFileStatusEnum.OBSOLETE.getStatus())
                .build());
        for (Long recipientUserId : resolveAffectedRecipientUserIds(previousActive.getId())) {
            createMessageJob(MESSAGE_BUSINESS_TYPE_OBSOLETE, previousActive.getId(), MESSAGE_TEMPLATE_OBSOLETE,
                    recipientUserId, buildObsoleteNotifyParams(previousActive, reason));
        }
        platformAdapter.recordWorkflowObsoleted(previousActive, file, actorId, reason, eventKey);
    }

    private PublishedArtifact resolveStampedPublishedArtifact(DccControlledFileDO file,
                                                              boolean allowPdfStampFailurePassThrough) {
        if (file.getPublishedFileId() != null && file.getStampedFileId() != null) {
            return new PublishedArtifact(file.getPublishedFileId(), file.getStampedFileId(),file.getStampedTime());
        }
        Long sourceFileId = resolveSourceFileId(file);
        FileDO sourceFile = fileMapper.selectById(sourceFileId);
        if (sourceFile == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (DccControlledFilePreviewKindEnum.resolve(sourceFile.getName(), sourceFile.getType())
                != DccControlledFilePreviewKindEnum.PDF) {
            DccConvertedPdf convertedPdf = pdfConversionService.convertToPdf(sourceFile);
            return stampAndStorePdf(file, sourceFile, convertedPdf.content(), convertedPdf.fileName(), false);
        }
        byte[] sourceBytes;
        try {
            sourceBytes = fileService.getFileContent(sourceFile.getConfigId(), sourceFile.getPath());
        } catch (Exception ex) {
            throw new ServiceException(CONTROLLED_FILE_STAMP_GENERATION_FAILED.getCode(),
                    StrUtil.blankToDefault(ex.getMessage(), "Controlled file stamp generation failed"));
        }
        return stampAndStorePdf(file, sourceFile, sourceBytes, sourceFile.getName(), allowPdfStampFailurePassThrough);
    }

    private PublishedArtifact stampAndStorePdf(DccControlledFileDO file, FileDO sourceFile, byte[] sourceBytes,
                                               String stampedFileName, boolean allowPdfStampFailurePassThrough) {
        try {
            byte[] stampedBytes = pdfStampService.stamp(sourceBytes);
            String stampedUrl = fileService.createFile(stampedBytes, stampedFileName, STAMPED_DIRECTORY, "application/pdf");
            FileDO stampedFile = fileMapper.selectFirstOne(FileDO::getUrl, stampedUrl);
            if (stampedFile == null) {
                throw new IllegalStateException("Stamped controlled file is missing after creation");
            }
            LocalDateTime stampedAt = LocalDateTime.now();
            return new PublishedArtifact(stampedFile.getId(), stampedFile.getId(), stampedAt);
        } catch (Exception ex) {
            throw new ServiceException(CONTROLLED_FILE_STAMP_GENERATION_FAILED.getCode(),
                    StrUtil.blankToDefault(ex.getMessage(), "Controlled file stamp generation failed"));
        }
    }

    private Long resolveSourceFileId(DccControlledFileDO file) {
        Long sourceFileId = file.getSourceFileId() != null ? file.getSourceFileId() : file.getOriginalFileId();
        if (sourceFileId == null) {
            throw new IllegalStateException("Controlled file source PDF is missing for finalization");
        }
        FileDO sourceFile = fileMapper.selectById(sourceFileId);
        if (sourceFile != null
                && DccControlledFileUploadTypePolicy.isDrawingSourceName(sourceFile.getName())) {
            if (file.getDrawingPdfFileId() == null) {
                throw exception(CONTROLLED_FILE_DRAWING_PDF_REQUIRED);
            }
            return file.getDrawingPdfFileId();
        }
        return sourceFileId;
    }

    private void prepareTrainingGatedRevision(DccControlledFileDO file, DccFileCategoryDO category,
                                              PublishedArtifact publishedArtifact, List<ResolvedDistributionPlan> distributionPlans) {
        createTrainingRecords(file, category, distributionPlans);
        controlledFileMapper.updateById(DccControlledFileDO.builder()
                .id(file.getId())
                .publishedFileId(publishedArtifact.publishedFileId())
                .stampedFileId(publishedArtifact.stampedFileId())
                .stampedTime(publishedArtifact.stampedTime())
                .status(DccControlledFileStatusEnum.TRAINING_IN_PROGRESS.getStatus())
                .finalizationError("")
                .build());
    }

    private void activateRevision(DccControlledFileDO file, DccControlledFileMasterDO master,
                                  DccFileCategoryDO category, PublishedArtifact publishedArtifact,
                                  List<ResolvedDistributionPlan> distributionPlans,
                                  Long actorId, String eventKey) {
        if (isThreeWorkflowUploadOrRevision(file)) {
            file.setPublishedFileId(publishedArtifact.publishedFileId());
            file.setStampedFileId(publishedArtifact.stampedFileId());
            file.setStampedTime(publishedArtifact.stampedTime());
            DccControlledFileDO previous = resolvePreviousActiveRevision(master, file.getId());
            lifecycleService.completeControl(file, master, actorId);
            // D consumes only the saved selected arrangements from CONTROLLED outbox events.
            // The legacy major-version followup enumerates all affected users and is not this contract.
            return;
        }
        DccControlledFileDO previousActive = resolvePreviousActiveRevision(master, file.getId());
        assertCandidateAdvancesCurrentActive(file, previousActive);
        if (!DccControlledFileProcessTypeEnum.CONTROLLED_FILE.getCode().equals(file.getProcessType())) {
            createDistributionRecords(file, category, distributionPlans);
        }
        supersedeStaleWorkingIterations(master, file);
        supersedePreviousActiveRevision(master, file.getId());
        LocalDateTime publishedAt = LocalDateTime.now().withNano(0);

        controlledFileMapper.updateById(DccControlledFileDO.builder()
                .id(file.getId())
                .publishedFileId(publishedArtifact.publishedFileId())
                .stampedFileId(publishedArtifact.stampedFileId())
                .stampedTime(publishedArtifact.stampedTime())
                .publishedTime(publishedAt)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .finalizationError("")
                .build());
        controlledFileMasterMapper.updateById(DccControlledFileMasterDO.builder()
                .id(master.getId())
                .currentActiveControlledFileId(file.getId())
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                .build());
        file.setPublishedTime(publishedAt);
        file.setStatus(DccControlledFileStatusEnum.ACTIVE.getStatus());
        publicationFollowupService.recordPublishedRevision(file, previousActive);
        platformAdapter.recordFinalized(previousActive, file, actorId, eventKey);
    }

    private DccControlledFileDO resolvePreviousActiveRevision(DccControlledFileMasterDO master, Long newActiveFileId) {
        Long previousActiveId = master.getCurrentActiveControlledFileId();
        if (previousActiveId == null || Objects.equals(previousActiveId, newActiveFileId)) {
            return null;
        }
        DccControlledFileDO previousActive = controlledFileMapper.selectById(previousActiveId);
        if (previousActive == null) {
            throw new IllegalStateException("Previous active revision is missing for supersession");
        }
        return previousActive;
    }

    private void assertCandidateAdvancesCurrentActive(DccControlledFileDO candidate,
                                                      DccControlledFileDO currentActive) {
        if (currentActive == null) {
            return;
        }
        DccControlledFileVersion candidateVersion = DccControlledFileVersion.parse(candidate.getVersionNo());
        DccControlledFileVersion currentVersion = DccControlledFileVersion.parse(currentActive.getVersionNo());
        if (candidateVersion == null || currentVersion == null || candidateVersion.compareTo(currentVersion) <= 0) {
            throw exception(CONTROLLED_FILE_PUBLISH_NOT_ALLOWED);
        }
    }

    private void activateRevisionWithoutGovernance(DccControlledFileDO file,
                                                   DccControlledFileMasterDO master,
                                                   PublishedArtifact publishedArtifact,
                                                   Long actorId,
                                                   String eventKey) {
        DccControlledFileDO previousActive = resolvePreviousActiveRevision(master, file.getId());
        assertCandidateAdvancesCurrentActive(file, previousActive);
        supersedeStaleWorkingIterations(master, file);
        supersedePreviousActiveRevision(master, file.getId());
        controlledFileMapper.updateById(DccControlledFileDO.builder()
                .id(file.getId())
                .publishedFileId(publishedArtifact.publishedFileId())
                .stampedFileId(publishedArtifact.stampedFileId())
                .stampedTime(publishedArtifact.stampedTime())
                .publishedTime(LocalDateTime.now())
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .finalizationError("")
                .build());
        controlledFileMasterMapper.updateById(DccControlledFileMasterDO.builder()
                .id(master.getId())
                .currentActiveControlledFileId(file.getId())
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                .build());
        platformAdapter.recordFinalized(previousActive, file, actorId, eventKey);
    }

    private List<ResolvedDistributionPlan> resolveDistributionPlans(DccControlledFileDO file,
                                                                    DccFileCategoryDO category,
                                                                    boolean resolveRecipientsForPaper) {
        List<DccControlledFileDistributionDO> existingDistributionPlans =
                distributionMapper.selectListByControlledFileId(file.getId());
        if (existingDistributionPlans != null && !existingDistributionPlans.isEmpty()) {
            return resolveSavedDistributionPlans(existingDistributionPlans, resolveRecipientsForPaper);
        }
        return resolveDistributionPlans(category, resolveRecipientsForPaper);
    }

    private List<ResolvedDistributionPlan> resolveDistributionPlans(DccFileCategoryDO category,
                                                                    boolean resolveRecipientsForPaper) {
        if (!Boolean.TRUE.equals(category.getDistributionRequired())) {
            return List.of();
        }
        List<DccFileCategoryDistributionRuleDO> rules = distributionRuleMapper
                .selectList(DccFileCategoryDistributionRuleDO::getCategoryId, category.getId()).stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getActive()))
                .toList();
        if (rules.isEmpty()) {
            throw new IllegalStateException("Missing required distribution departments");
        }
        List<ResolvedDistributionPlan> distributionPlans = new java.util.ArrayList<>();
        for (DccFileCategoryDistributionRuleDO rule : rules) {
            if (rule.getDepartmentId() == null) {
                throw new IllegalStateException("Distribution rule department is missing");
            }
            String distributionMedium = normalizeDistributionMedium(rule.getDistributionMedium());
            boolean shouldResolveRecipients = resolveRecipientsForPaper
                    || !DccDistributionMediumEnum.PAPER.getCode().equals(distributionMedium);
            List<Long> recipientUserIds = List.of();
            if (shouldResolveRecipients) {
                recipientUserIds = resolveDepartmentUserIds(rule.getDepartmentId(), "distribution recipients");
            }
            distributionPlans.add(new ResolvedDistributionPlan(
                    rule.getDepartmentId(),
                    distributionMedium,
                    List.copyOf(recipientUserIds)));
        }
        return distributionPlans;
    }

    private List<ResolvedDistributionPlan> resolveSavedDistributionPlans(
            List<DccControlledFileDistributionDO> existingDistributionPlans,
            boolean resolveRecipientsForPaper) {
        List<ResolvedDistributionPlan> distributionPlans = new java.util.ArrayList<>();
        for (DccControlledFileDistributionDO distribution : existingDistributionPlans) {
            if (distribution.getDepartmentId() == null) {
                throw new IllegalStateException("Distribution department is missing");
            }
            String distributionMedium = normalizeDistributionMedium(distribution.getDistributionMedium());
            List<Long> recipientUserIds = List.of();
            if (DccDistributionMediumEnum.PUBLIC_FOLDER.getCode().equals(distributionMedium)) {
                recipientUserIds = resolveSavedElectronicDistributionRecipients(distribution);
            } else if (resolveRecipientsForPaper) {
                recipientUserIds = resolveDepartmentUserIds(distribution.getDepartmentId(), "training recipients");
            }
            distributionPlans.add(new ResolvedDistributionPlan(
                    distribution.getDepartmentId(),
                    distributionMedium,
                    List.copyOf(recipientUserIds)));
        }
        return distributionPlans;
    }

    private List<Long> resolveSavedElectronicDistributionRecipients(DccControlledFileDistributionDO distribution) {
        List<DccControlledFileDistributionRecipientDO> recipients =
                Objects.requireNonNull(distributionRecipientMapper.selectListByDistributionId(distribution.getId()),
                        "saved electronic distribution recipients must not be null");
        LinkedHashSet<Long> recipientUserIds = new LinkedHashSet<>();
        for (DccControlledFileDistributionRecipientDO recipient : recipients) {
            if (recipient == null || recipient.getUserId() == null) {
                throw new ServiceException(CONTROLLED_FILE_PUBLISH_NOT_ALLOWED.getCode(),
                        "Saved electronic distribution contains blank recipient: distributionId="
                                + distribution.getId());
            }
            recipientUserIds.add(recipient.getUserId());
        }
        if (recipientUserIds.isEmpty()) {
            throw new ServiceException(CONTROLLED_FILE_PUBLISH_NOT_ALLOWED.getCode(),
                    "Single-file electronic distribution requires recipients: distributionId="
                            + distribution.getId());
        }
        List<Long> orderedRecipientUserIds = List.copyOf(recipientUserIds);
        validateSavedElectronicDistributionRecipients(distribution.getId(), orderedRecipientUserIds);
        return orderedRecipientUserIds;
    }

    private void validateSavedElectronicDistributionRecipients(Long distributionId,
                                                               Collection<Long> recipientUserIds) {
        List<AdminUserRespDTO> users = Objects.requireNonNull(adminUserApi.getUserList(recipientUserIds),
                "saved electronic distribution recipient users must not be null");
        Map<Long, AdminUserRespDTO> userMap = new LinkedHashMap<>();
        for (AdminUserRespDTO user : users) {
            if (user != null && user.getId() != null) {
                userMap.putIfAbsent(user.getId(), user);
            }
        }
        List<Long> invalidRecipientUserIds = recipientUserIds.stream()
                .filter(userId -> {
                    AdminUserRespDTO user = userMap.get(userId);
                    return user == null || !CommonStatusEnum.ENABLE.getStatus().equals(user.getStatus());
                })
                .toList();
        if (!invalidRecipientUserIds.isEmpty()) {
            throw new ServiceException(CONTROLLED_FILE_PUBLISH_NOT_ALLOWED.getCode(),
                    "Saved electronic distribution recipients are inactive or missing: distributionId="
                            + distributionId + ", userIds=" + invalidRecipientUserIds);
        }
    }

    private void createDistributionRecords(DccControlledFileDO file, DccFileCategoryDO category,
                                           List<ResolvedDistributionPlan> distributionPlans) {
        List<DccControlledFileDistributionDO> existingDistributionPlans =
                distributionMapper.selectListByControlledFileId(file.getId());
        if (existingDistributionPlans != null && !existingDistributionPlans.isEmpty()) {
            dispatchExistingElectronicDistributionPlan(file, existingDistributionPlans);
            return;
        }
        if (!Boolean.TRUE.equals(category.getDistributionRequired())) {
            return;
        }
        if (distributionPlans == null || distributionPlans.isEmpty()) {
            throw new IllegalStateException("Missing required distribution departments");
        }
        for (ResolvedDistributionPlan distributionPlan : distributionPlans) {
            DccControlledFileDistributionDO distribution = DccControlledFileDistributionDO.builder()
                    .controlledFileId(file.getId())
                    .departmentId(distributionPlan.departmentId())
                    .distributionMedium(distributionPlan.distributionMedium())
                    .status(DccControlledFileDistributionStatusEnum.PENDING.getCode())
                    .build();
            distributionMapper.insert(distribution);
            if (DccDistributionMediumEnum.PAPER.getCode().equals(distributionPlan.distributionMedium())) {
                continue;
            }
            for (Long recipientUserId : distributionPlan.recipientUserIds()) {
                DccControlledFileMessageJobDO messageJob = createMessageJob(
                        MESSAGE_BUSINESS_TYPE_DISTRIBUTION, distribution.getId(),
                        MESSAGE_TEMPLATE_DISTRIBUTION, recipientUserId,
                        buildDistributionNotifyParams(file));
                distributionRecipientMapper.insert(DccControlledFileDistributionRecipientDO.builder()
                        .distributionId(distribution.getId())
                        .userId(recipientUserId)
                        .messageJobId(messageJob.getId())
                        .build());
            }
        }
    }

    private void dispatchExistingElectronicDistributionPlan(DccControlledFileDO file,
                                                            List<DccControlledFileDistributionDO> existingDistributionPlans) {
        for (DccControlledFileDistributionDO distribution : existingDistributionPlans) {
            if (DccDistributionMediumEnum.PAPER.getCode().equals(distribution.getDistributionMedium())) {
                continue;
            }
            if (!DccDistributionMediumEnum.PUBLIC_FOLDER.getCode().equals(distribution.getDistributionMedium())) {
                throw exception(CONTROLLED_FILE_DISTRIBUTION_MEDIUM_INVALID);
            }
            List<DccControlledFileDistributionRecipientDO> recipients =
                    distributionRecipientMapper.selectListByDistributionId(distribution.getId());
            if (recipients.isEmpty()) {
                throw new IllegalStateException("Single-file electronic distribution requires recipients");
            }
            for (DccControlledFileDistributionRecipientDO recipient : recipients) {
                if (recipient.getMessageJobId() != null) {
                    continue;
                }
                DccControlledFileMessageJobDO messageJob = createMessageJob(
                        MESSAGE_BUSINESS_TYPE_DISTRIBUTION, distribution.getId(),
                        MESSAGE_TEMPLATE_DISTRIBUTION, recipient.getUserId(),
                        buildDistributionNotifyParams(file));
                distributionRecipientMapper.updateById(DccControlledFileDistributionRecipientDO.builder()
                        .id(recipient.getId())
                        .messageJobId(messageJob.getId())
                        .build());
            }
        }
    }

    private void createTrainingRecords(DccControlledFileDO file, DccFileCategoryDO category,
                                       List<ResolvedDistributionPlan> distributionPlans) {
        if (!Boolean.TRUE.equals(category.getTrainingRequired())) {
            return;
        }
        if (distributionPlans == null || distributionPlans.isEmpty()) {
            throw new IllegalStateException("Training requires distribution recipients");
        }
        Set<Long> uniqueRecipientUserIds = new LinkedHashSet<>();
        for (ResolvedDistributionPlan distributionPlan : distributionPlans) {
            if (distributionPlan.departmentId() == null) {
                throw new IllegalStateException("Distribution department is missing for training inheritance");
            }
            if (distributionPlan.recipientUserIds().isEmpty()) {
                continue;
            }
            DccControlledFileTrainingDO training = DccControlledFileTrainingDO.builder()
                    .controlledFileId(file.getId())
                    .departmentId(distributionPlan.departmentId())
                    .status(DccControlledFileTrainingStatusEnum.PENDING.getCode())
                    .build();
            trainingMapper.insert(training);
            for (Long assigneeUserId : distributionPlan.recipientUserIds()) {
                uniqueRecipientUserIds.add(assigneeUserId);
                DccControlledFileMessageJobDO messageJob = createMessageJob(
                        MESSAGE_BUSINESS_TYPE_TRAINING, training.getId(),
                        MESSAGE_TEMPLATE_TRAINING, assigneeUserId,
                        buildTrainingNotifyParams(file));
                trainingAssignmentMapper.insert(DccControlledFileTrainingAssignmentDO.builder()
                        .trainingId(training.getId())
                        .userId(assigneeUserId)
                        .messageJobId(messageJob.getId())
                        .status(DccControlledFileTrainingStatusEnum.PENDING.getCode())
                        .build());
            }
        }
        if (uniqueRecipientUserIds.isEmpty()) {
            throw new IllegalStateException("Training requires inherited distribution recipients");
        }
        for (Long recipientUserId : uniqueRecipientUserIds) {
            trainingProgressMapper.insert(DccControlledFileTrainingProgressDO.builder()
                    .controlledFileId(file.getId())
                    .userId(recipientUserId)
                    .requiredViewSeconds(600)
                    .accumulatedViewSeconds(0)
                    .build());
        }
    }

    private static final String STAMPED_DIRECTORY = "dcc/stamped";

    private boolean allTrainingAcknowledged(Long controlledFileId) {
        List<DccControlledFileTrainingDO> trainings = trainingMapper.selectListByControlledFileId(controlledFileId);
        return !trainings.isEmpty() && trainings.stream()
                .allMatch(training -> DccControlledFileTrainingStatusEnum.ACKNOWLEDGED.getCode().equals(training.getStatus()));
    }

    private List<Long> resolveDepartmentUserIds(Long departmentId, String businessContext) {
        List<Long> userIds = adminUserApi.getUserListByDeptIds(List.of(departmentId)).stream()
                .filter(Objects::nonNull)
                .filter(user -> CommonStatusEnum.ENABLE.getStatus().equals(user.getStatus()))
                .map(AdminUserRespDTO::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
        if (userIds.isEmpty()) {
            throw new IllegalStateException("Unable to resolve " + businessContext + " for department " + departmentId);
        }
        return userIds;
    }

    private DccControlledFileMessageJobDO createMessageJob(String businessType, Long businessId,
                                                           String templateCode, Long recipientUserId,
                                                           Map<String, Object> templateParams) {
        DccControlledFileMessageJobDO messageJob = DccControlledFileMessageJobDO.builder()
                .businessType(businessType)
                .businessId(businessId)
                .templateCode(templateCode)
                .recipientUserId(recipientUserId)
                .status(DccControlledFileMessageJobStatusEnum.PENDING.getCode())
                .build();
        messageJobMapper.insert(messageJob);
        messageDeliveryService.dispatchMessageJob(messageJob, templateParams);
        return messageJob;
    }

    private Set<Long> resolveAffectedRecipientUserIds(Long controlledFileId) {
        Set<Long> userIds = new LinkedHashSet<>();
        distributionMapper.selectListByControlledFileId(controlledFileId).forEach(distribution ->
                distributionRecipientMapper.selectListByDistributionId(distribution.getId()).stream()
                        .map(DccControlledFileDistributionRecipientDO::getUserId)
                        .filter(Objects::nonNull)
                        .forEach(userIds::add));
        trainingMapper.selectListByControlledFileId(controlledFileId).forEach(training ->
                trainingAssignmentMapper.selectListByTrainingId(training.getId()).stream()
                        .map(DccControlledFileTrainingAssignmentDO::getUserId)
                        .filter(Objects::nonNull)
                        .forEach(userIds::add));
        return userIds;
    }

    private Map<String, Object> buildDistributionNotifyParams(DccControlledFileDO file) {
        return buildBaseNotifyParams(file);
    }

    private Map<String, Object> buildTrainingNotifyParams(DccControlledFileDO file) {
        return buildBaseNotifyParams(file);
    }

    private Map<String, Object> buildObsoleteNotifyParams(DccControlledFileDO file, String obsoleteReason) {
        Map<String, Object> params = buildBaseNotifyParams(file);
        params.put("reason", StrUtil.blankToDefault(obsoleteReason, "-"));
        return params;
    }

    private Map<String, Object> buildBaseNotifyParams(DccControlledFileDO file) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("title", StrUtil.blankToDefault(file.getTitle(), file.getFileName()));
        params.put("version", StrUtil.blankToDefault(file.getVersionNo(), "-"));
        if (file.getEffectiveDate() != null) {
            params.put("effectiveDate", file.getEffectiveDate().toString());
        }
        return params;
    }

    private void supersedePreviousActiveRevision(DccControlledFileMasterDO master, Long newActiveFileId) {
        Long previousActiveId = master.getCurrentActiveControlledFileId();
        if (previousActiveId == null || Objects.equals(previousActiveId, newActiveFileId)) {
            return;
        }
        DccControlledFileDO previousActive = controlledFileMapper.selectById(previousActiveId);
        if (previousActive == null) {
            throw new IllegalStateException("Previous active revision is missing for supersession");
        }
        controlledFileMapper.updateById(DccControlledFileDO.builder()
                .id(previousActiveId)
                .status(DccControlledFileStatusEnum.SUPERSEDED.getStatus())
                .supersededByFileId(newActiveFileId)
                .build());
    }

    private void supersedeStaleWorkingIterations(DccControlledFileMasterDO master,
                                                  DccControlledFileDO newActiveFile) {
        DccControlledFileVersion newActiveVersion = DccControlledFileVersion.parse(newActiveFile.getVersionNo());
        if (newActiveVersion == null) {
            throw exception(CONTROLLED_FILE_PUBLISH_NOT_ALLOWED);
        }
        List<DccControlledFileDO> chain = Objects.requireNonNull(
                controlledFileMapper.selectListByMasterId(master.getId()),
                "Controlled file version chain must not be null during finalization");
        for (DccControlledFileDO iteration : chain) {
            if (iteration == null || Objects.equals(iteration.getId(), newActiveFile.getId())
                    || !isUnfinishedWorkflowVersion(iteration)) {
                continue;
            }
            DccControlledFileVersion workingVersion = DccControlledFileVersion.parse(iteration.getVersionNo());
            if (workingVersion == null) {
                throw exception(CONTROLLED_FILE_PUBLISH_NOT_ALLOWED);
            }
            if (newActiveVersion.compareTo(workingVersion) <= 0) {
                continue;
            }
            if (controlledFileMapper.updateById(DccControlledFileDO.builder()
                    .id(iteration.getId())
                    .status(DccControlledFileStatusEnum.SUPERSEDED.getStatus())
                    .supersededByFileId(newActiveFile.getId())
                    .build()) != 1) {
                throw new IllegalStateException("Stale working iteration supersession failed: " + iteration.getId());
            }
        }
    }

    private boolean isUnfinishedWorkflowVersion(DccControlledFileDO file) {
        if (file == null || StrUtil.isBlank(file.getStatus())) {
            return false;
        }
        return !Set.of(
                DccControlledFileStatusEnum.ACTIVE.getStatus(),
                DccControlledFileStatusEnum.REJECTED.getStatus(),
                DccControlledFileStatusEnum.WITHDRAWN.getStatus(),
                DccControlledFileStatusEnum.OBSOLETE.getStatus(),
                DccControlledFileStatusEnum.SUPERSEDED.getStatus()
        ).contains(file.getStatus());
    }

    private void validateApprovalEventIdentity(DccControlledFileDO file,
                                               BpmProcessInstanceStatusEvent event) {
        if (!Objects.equals(file.getTenantId(),TenantContextHolder.getRequiredTenantId()) || file.getId()==null
                || !Objects.equals(file.getId().toString(),event.getBusinessKey())
                || !Objects.equals(file.getProcessInstanceId(), event.getId())
                || !Objects.equals(file.getProcessDefinitionKey(), event.getProcessDefinitionKey())
                || StrUtil.isBlank(event.getProcessDefinitionKey())
                || !DccControlledFileProcessDefinitionKeys.NATIVE_FINALIZATION_KEYS
                .contains(event.getProcessDefinitionKey())) {
            throw new IllegalStateException("DCC approval event identity does not match the controlled file");
        }
    }

    private void scheduleFailureAfterRollback(DccControlledFileDO file, String expectedStatus,
                                              Long actorId, String failureReason, String eventKey) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Runnable persistFailure = () -> TenantUtils.execute(tenantId, () ->
                finalizationFailureService.recordFailure(tenantId, file.getId(), expectedStatus,
                        actorId, failureReason, eventKey));
        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            persistFailure.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    persistFailure.run();
                }
            }
        });
    }

    private record PublishedArtifact(Long publishedFileId, Long stampedFileId, LocalDateTime stampedTime) {
    }

    private String resolveFailureReason(RuntimeException ex) {
        if (ex instanceof ServiceException serviceException) {
            return StrUtil.blankToDefault(serviceException.getMessage(), "Controlled file finalization failed");
        }
        return StrUtil.blankToDefault(ex.getMessage(), "Controlled file finalization failed");
    }

    private ServiceException toFinalizationException(String reason, RuntimeException ex) {
        if (ex instanceof ServiceException serviceException
                && (Objects.equals(serviceException.getCode(), CONTROLLED_FILE_STAMP_GENERATION_FAILED.getCode())
                || Objects.equals(serviceException.getCode(), CONTROLLED_FILE_PDF_CONVERSION_CONFIG_MISSING.getCode())
                || Objects.equals(serviceException.getCode(), CONTROLLED_FILE_PDF_CONVERSION_FAILED.getCode())
                || Objects.equals(serviceException.getCode(), CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING.getCode())
                || Objects.equals(serviceException.getCode(), CONTROLLED_FILE_SIGNATURE_EVIDENCE_INVALID.getCode()))) {
            return serviceException;
        }
        return new ServiceException(CONTROLLED_FILE_STAMP_GENERATION_FAILED.getCode(), reason);
    }

    private String normalizeDistributionMedium(String distributionMedium) {
        if (StrUtil.isBlank(distributionMedium)) {
            return DccDistributionMediumEnum.PUBLIC_FOLDER.getCode();
        }
        if (!DccDistributionMediumEnum.isValid(distributionMedium)) {
            throw exception(CONTROLLED_FILE_DISTRIBUTION_MEDIUM_INVALID);
        }
        return distributionMedium;
    }

    private record ResolvedDistributionPlan(Long departmentId, String distributionMedium,
                                                  List<Long> recipientUserIds) {
    }

}
