package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
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
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PDF_CONVERSION_CONFIG_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PDF_CONVERSION_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PUBLISH_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_EVIDENCE_INVALID;
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
        String eventKey = platformAdapter.nextFinalizationRetryEventKey(file);
        platformAdapter.recordFinalizationRetried(file, userId, eventKey);
        runFinalizationWithFailureHandling(file, userId, eventKey);
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
        if (DccControlledFileStatusEnum.ACTIVE.getStatus().equals(initial.getStatus())) return;
        String expectedStatus = initial.getStatus();
        try {
            transactionTemplate.executeWithoutResult(ignored -> {
                Long tenantId = TenantContextHolder.getRequiredTenantId();
                DccControlledFileMasterDO master = controlledFileMasterMapper.selectByIdForUpdate(initial.getMasterId());
                DccControlledFileDO file = controlledFileMapper.selectByIdAndTenantForUpdate(tenantId, initial.getId());
                if (master == null || file == null || !Objects.equals(master.getId(), file.getMasterId())) {
                    throw exception(CONTROLLED_FILE_NOT_EXISTS);
                }
                validateApprovalEventIdentity(file, event);
                if (DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus())) return;
                if (!DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus().equals(file.getStatus())
                        && !DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus().equals(file.getStatus())) {
                    throw new IllegalStateException("DCC approval cannot finalize status " + file.getStatus());
                }
                if (file.getPublishedFileId() == null || file.getStampedFileId() == null) {
                    throw exception(CONTROLLED_FILE_PUBLISH_NOT_ALLOWED);
                }
                List<cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO> approvalSignatures =
                        approvalSignatureMapper.selectListByControlledFileId(file.getId());
                DccFrozenApprovalSignatures.requireComplete(file,
                        routeSnapshotMapper.selectListByControlledFileId(file.getId()), approvalSignatures);
                verifyApprovalSignatureEvidence(file, approvalSignatures);
                if (DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus().equals(file.getStatus())) {
                    LocalDateTime approvedAt = LocalDateTime.now();
                    if (controlledFileMapper.markReadyToPublishAfterApproval(tenantId, file.getId(), event.getId(),
                            event.getProcessDefinitionKey(), file.getStatus(), approvedAt, event.getActorUserId()) != 1) {
                        throw new IllegalStateException("DCC approval lost its status CAS");
                    }
                    file.setApprovedTime(approvedAt);
                    file.setStatus(DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus());
                }
                // READY_TO_PUBLISH is an internal transition in this transaction, never a separate user task.
                platformAdapter.recordFinalizationStarted(file, event.getActorUserId(), event.getId());
                finalizeRevision(file, master, false, event.getActorUserId(), event.getId(), true);
            });
        } catch (RuntimeException failure) {
            String reason = resolveFailureReason(failure);
            scheduleFailureAfterRollback(initial, expectedStatus, event.getActorUserId(), reason, event.getId());
            throw toFinalizationException(reason, failure);
        }
    }

    private void markRejectedAfterApprovalEvent(DccControlledFileDO initial, BpmProcessInstanceStatusEvent event) {
        validateApprovalEventIdentity(initial, event);
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
            if (shouldIgnoreRejectReplay(file.getStatus())) {
                return;
            }
            if (!WITHDRAW_EVENT_STATUSES.contains(file.getStatus())) {
                throw new IllegalStateException("DCC reject cannot transition status " + file.getStatus());
            }
            LocalDateTime rejectedTime = LocalDateTime.now();
            int updated = controlledFileMapper.markRejectedAfterApprovalEvent(tenantId, file.getId(), event.getId(),
                    file.getStatus(), rejectedTime, event.getReason(), event.getActorUserId());
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
        for (var signature : signatures) {
            if (signature == null || signature.getId() == null
                    || !Objects.equals(file.getId(), signature.getControlledFileId())
                    || !Objects.equals(file.getVersionNo(), signature.getVersionNo())
                    || !"APPROVE".equals(signature.getActionType())
                    || !"VALID".equals(signature.getEvidenceStatus())) {
                continue;
            }
            var verification = signatureManagementService.verifySignatureEvidence(signature.getId());
            if (verification == null || !Objects.equals(signature.getId(), verification.getSignatureId())
                    || !"VALID".equals(verification.getVerificationStatus())) {
                throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_INVALID);
            }
        }
    }

    private boolean isOrdinaryApprovalCandidate(DccControlledFileDO file) {
        return DccControlledFileProcessTypeEnum.CONTROLLED_FILE.getCode().equals(file.getProcessType())
                && (DccControlledFileChangeTypeEnum.NEW.getCode().equals(file.getChangeType())
                || DccControlledFileChangeTypeEnum.REVISION.getCode().equals(file.getChangeType()));
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
        if (file.getPublishedFileId() == null || file.getStampedFileId() == null) {
            throw exception(CONTROLLED_FILE_PUBLISH_NOT_ALLOWED);
        }
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
        DccControlledFileDO file = controlledFileMapper.selectById(fileId);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        DccControlledFileMasterDO master = controlledFileMasterMapper.selectById(file.getMasterId());
        if (master == null) {
            throw new IllegalStateException("Controlled file master is missing for finalization");
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
            return new PublishedArtifact(file.getPublishedFileId(), file.getStampedFileId());
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
                .approvedTime(LocalDateTime.now())
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
                    || !DccControlledFileStatusEnum.WORKING.getStatus().equals(iteration.getStatus())) {
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

    private void validateApprovalEventIdentity(DccControlledFileDO file,
                                               BpmProcessInstanceStatusEvent event) {
        if (!Objects.equals(file.getProcessInstanceId(), event.getId())
                || !Objects.equals(file.getProcessDefinitionKey(), event.getProcessDefinitionKey())
                || !Objects.equals(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY,
                event.getProcessDefinitionKey())) {
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
        PublishedArtifact(Long publishedFileId, Long stampedFileId) {
            this(publishedFileId, stampedFileId, LocalDateTime.now());
        }
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
