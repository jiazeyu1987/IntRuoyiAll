package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitIterationReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.Objects;
import java.util.Set;
import java.util.Arrays;
import java.util.HexFormat;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import cn.hutool.json.JSONUtil;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.*;

@Service
public class DccControlledFileRevisionServiceImpl implements DccControlledFileRevisionService {
    @Resource private DccPublicUploadPlacementService publicUploadPlacementService;
    @Resource(name = "dccControlledFileMapper") private DccControlledFileMapper fileMapper;
    @Resource private DccControlledFileMasterMapper masterMapper;
    @Resource private DccControlledFileCheckoutMapper checkoutMapper;
    @Resource private DccProjectAccessService projectAccessService;
    @Resource private DccControlledFileAssignmentScopeService assignmentScopeService;
    @Resource private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Resource private DccControlledFileSourceOwnershipService sourceOwnershipService;
    @Resource private DccControlledFileRelatedFileService relatedFileService;
    @Resource private DccControlledFileNameClaimService nameClaimService;
    @Resource private DccControlledFileVersionPolicy versionPolicy;
    @Resource private DccWorkflowDatePolicy datePolicy;
    @Resource private cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService bpmProcessInstanceService;
    @Resource private cn.iocoder.yudao.module.bpm.service.task.BpmTaskService bpmTaskService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccControlledFileDO createInitialCandidate(Long userId, Long selectedIterationId,
                                                       DccControlledFileSubmitIterationReqVO request) {
        requireRequest(request);
        if (!"INITIAL".equals(request.getRevisionChangeType())) throw new IllegalArgumentException("initial candidate requires actual INITIAL intent");
        Long tenant = TenantContextHolder.getRequiredTenantId();
        var identity = fileMapper.selectById(selectedIterationId);
        if (identity == null || !tenant.equals(identity.getTenantId()) || identity.getMasterId() == null)
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        publicUploadPlacementService.lockSourceLocation(userId,selectedIterationId);
        var master = masterMapper.selectByIdForUpdate(identity.getMasterId());
        var selected = fileMapper.selectByIdAndTenantForUpdate(tenant, selectedIterationId);
        if (master == null || !tenant.equals(master.getTenantId()) || selected == null
                || !Objects.equals(master.getId(),selected.getMasterId())
                || !Objects.equals(tenant,selected.getTenantId())) throw exception(CONTROLLED_FILE_NOT_EXISTS);
        if (userId == null || !Objects.equals(userId,selected.getRequesterId())
                || !assignmentScopeService.isWithinAssignedFileScope(userId,selectedIterationId))
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        if (master.getDccProjectCodeId()==null || master.getFileTypeTaxonomyLeafId()==null
                || !Objects.equals(master.getDccProjectCodeId(),selected.getDccProjectCodeId())
                || !Objects.equals(master.getFileTypeTaxonomyLeafId(),selected.getFileTypeTaxonomyId())
                || StrUtil.isBlank(master.getNormalizedFileNumber())
                || !Objects.equals(master.getNormalizedFileNumber(),selected.getFileNumber())
                || StrUtil.isBlank(selected.getSourceOriginalFileName())) throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        projectAccessService.assertProjectEditorOrOwner(userId,selected.getDccProjectCodeId());
        if (!permissionSupport.hasCategoryPermission(selected.getCategoryId(),userId,DccFileCategoryPermissionActionEnum.UPLOAD))
            throw exception(DCC_PROJECT_ACCESS_DENIED);
        var chain = Objects.requireNonNull(fileMapper.selectListByMasterIdForUpdate(master.getId()),"initial version chain");
        String key = request.getIdempotencyKey().trim();
        String hash = payloadHash(null,selectedIterationId,request,selected);
        var existing = chain.stream().filter(v -> Objects.equals(userId,v.getRequesterId())
                && key.equals(v.getCreationIdempotencyKey())).findFirst().orElse(null);
        if (existing != null) {
            if (!"INITIAL".equals(existing.getRevisionChangeType())
                    || !Objects.equals(selectedIterationId,existing.getSelectedIterationControlledFileId())
                    || !Objects.equals(hash,existing.getCreationPayloadHash()))
                throw new IllegalArgumentException("initial candidate replay conflicts with actual application facts");
            return existing;
        }
        datePolicy.requireReviewDate(request.getEffectiveDate());
        var selectedVersion = versionPolicy.requireStored(selected);
        if (master.getLatestControlledFileId()!=null || master.getCurrentActiveControlledFileId()!=null
                || !"NEW".equals(selected.getChangeType()) || !"WORKING".equals(selected.getStatus())
                || !selectedVersion.isWorkingIteration() || selected.getRevisionChangeType()!=null
                || selected.getRevisionBaseActiveControlledFileId()!=null)
            throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        if (chain.stream().anyMatch(v -> !sameLogicalIdentity(selected,v)
                || !Objects.equals(selected.getSourceOriginalFileName(),v.getSourceOriginalFileName())
                || !Set.of("WORKING","DRAFT","REJECTED","WITHDRAWN").contains(v.getStatus())
                || v.getSelectedIterationControlledFileId()!=null && v.getRevisionChangeType()!=null))
            throw exception(CONTROLLED_FILE_WORKFLOW_IN_PROGRESS);
        var byId = chain.stream().collect(java.util.stream.Collectors.toMap(DccControlledFileDO::getId,v -> v));
        var ancestor = selected;
        var visited = new java.util.HashSet<Long>();
        while (versionPolicy.requireStored(ancestor).isWorkingIteration()) {
            if (!visited.add(ancestor.getId()) || !"NEW".equals(ancestor.getChangeType())
                    || ancestor.getRevisionBaseActiveControlledFileId()!=null || ancestor.getRevisionChangeType()!=null
                    || !Objects.equals(userId,ancestor.getRequesterId()) || ancestor.getPredecessorControlledFileId()==null)
                throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
            var previous = byId.get(ancestor.getPredecessorControlledFileId());
            if (previous==null || !versionPolicy.requireStored(previous).formal().display().equals(selectedVersion.formal().display())
                    || versionPolicy.requireStored(previous).compareTo(versionPolicy.requireStored(ancestor))>=0)
                throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
            ancestor = previous;
        }
        if (!"NEW".equals(ancestor.getChangeType()) || ancestor.getPredecessorControlledFileId()!=null
                || !Objects.equals(userId,ancestor.getRequesterId())
                || ancestor.getRevisionBaseActiveControlledFileId()!=null
                || ancestor.getRevisionChangeType()!=null && !"INITIAL".equals(ancestor.getRevisionChangeType())
                || !versionPolicy.requireStored(ancestor).display().equals(selectedVersion.formal().display()))
            throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        var checkout = checkoutMapper.selectActiveByMasterId(tenant,master.getId());
        if (checkout!=null) throw exception(CONTROLLED_FILE_ALREADY_CHECKED_OUT,checkout.getActorId());
        nameClaimService.claimExistingVersion(tenant,selected.getSourceOriginalFileName(),master.getDccProjectCodeId(),
                master.getFileTypeTaxonomyLeafId(),master.getNormalizedFileNumber(),master.getId(),selected.getId());
        return freezeCandidate(userId,selected,selectedVersion.formal(),null,request,hash,true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccControlledFileDO createRevision(Long userId, Long baselineId, Long selectedId,
                                              DccControlledFileSubmitIterationReqVO request) {
        requireRequest(request);
        var intent = DccControlledFileRevisionChangeType.requireRevision(request.getRevisionChangeType());
        Long tenant = TenantContextHolder.getRequiredTenantId();
        var initial = fileMapper.selectById(baselineId);
        if (initial == null || !tenant.equals(initial.getTenantId()) || initial.getMasterId() == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        var selectedIdentity=fileMapper.selectById(selectedId);
        if(selectedIdentity==null || !Objects.equals(selectedIdentity.getTenantId(),tenant)
                || !Objects.equals(selectedIdentity.getMasterId(),initial.getMasterId())
                || !Objects.equals(selectedIdentity.getDccProjectCodeId(),initial.getDccProjectCodeId()))
            throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        publicUploadPlacementService.lockSourceLocation(userId,selectedId);
        var master = masterMapper.selectByIdForUpdate(initial.getMasterId());
        if (master == null || !tenant.equals(master.getTenantId())) throw exception(CONTROLLED_FILE_NOT_EXISTS);
        var baseline = fileMapper.selectByIdAndTenantForUpdate(tenant, baselineId);
        var selected = fileMapper.selectByIdAndTenantForUpdate(tenant, selectedId);
        if (baseline == null || selected == null || !Objects.equals(master.getId(), baseline.getMasterId())
                || !Objects.equals(master.getId(), selected.getMasterId())) throw exception(CONTROLLED_FILE_NOT_EXISTS);
        if (!Objects.equals(master.getDccProjectCodeId(), baseline.getDccProjectCodeId())
                || !Objects.equals(master.getFileTypeTaxonomyLeafId(), baseline.getFileTypeTaxonomyId())
                || StrUtil.isBlank(master.getNormalizedFileNumber())
                || !Objects.equals(master.getNormalizedFileNumber(), baseline.getFileNumber())
                || !sameLogicalIdentity(baseline, selected)) {
            throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        }
        if (userId == null || !assignmentScopeService.isWithinAssignedFileScope(userId, baselineId)
                || !assignmentScopeService.isWithinAssignedFileScope(userId, selectedId)) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        projectAccessService.assertProjectEditorOrOwner(userId, baseline.getDccProjectCodeId());
        if (intent == DccControlledFileRevisionChangeType.REPLACEMENT) {
            projectAccessService.assertProjectOwner(userId, baseline.getDccProjectCodeId());
        }
        if (!permissionSupport.hasCategoryPermission(baseline.getCategoryId(), userId, DccFileCategoryPermissionActionEnum.UPLOAD)) {
            throw exception(DCC_PROJECT_ACCESS_DENIED);
        }
        var baselineRead = sourceIdentityProjection(baseline);
        var selectedRead = sourceIdentityProjection(selected);
        if(!Objects.equals(baselineRead.getSourceOriginalFileName(),selectedRead.getSourceOriginalFileName()))
            throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        var chain = Objects.requireNonNull(fileMapper.selectListByMasterIdForUpdate(master.getId()), "version chain");
        String key = request.getIdempotencyKey().trim();
        String payloadHash = payloadHash(baselineId, selectedId, request,selectedRead);
        var existing = chain.stream().filter(v -> Objects.equals(userId, v.getRequesterId())
                && key.equals(v.getCreationIdempotencyKey())).findFirst().orElse(null);
        if (existing != null) {
            if (!intent.name().equals(existing.getRevisionChangeType())
                    || !baselineId.equals(existing.getRevisionSourceControlledFileId())
                    || !selectedId.equals(existing.getSelectedIterationControlledFileId())
                    || !request.getChangeDescription().trim().equals(existing.getChangeDescription())
                    || !Objects.equals(request.getNeedTraining(), existing.getNeedTraining())
                    || !payloadHash.equals(existing.getCreationPayloadHash())) {
                throw new IllegalArgumentException("revision replay payload conflicts with committed intent");
            }
            return existing;
        }
        datePolicy.requireReviewDate(request.getEffectiveDate());
        if (!Objects.equals(master.getLatestControlledFileId(), baselineId)
                || baseline.getControlledTime() == null
                || !DccControlledFileVersionPolicy.isCurrentControlledStatus(baseline.getStatus())) {
            throw new IllegalArgumentException("formal source must be the authoritative latest controlled version");
        }
        var baseVersion = versionPolicy.requireStored(baseline);
        var selectedVersion = versionPolicy.requireStored(selected);
        if (!versionPolicy.isControlledBaseline(baseline) || baseVersion.isWorkingIteration()
                || !"WORKING".equals(selected.getStatus()) || selected.getRevisionChangeType() != null
                || !selectedVersion.isWorkingIteration() || !(baseVersion.display().equals(selectedVersion.formal().display())
                    || DccRevisionReworkPolicy.correctionMatchesBaseline(baseline,selected,chain,versionPolicy))
                || !Objects.equals(baselineId, selected.getRevisionBaseActiveControlledFileId())
                || !userId.equals(selected.getRequesterId()) && intent != DccControlledFileRevisionChangeType.REPLACEMENT
                || StrUtil.isBlank(selectedRead.getSourceOriginalFileName())) {
            throw exception(CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        }
        var checkout = checkoutMapper.selectActiveByMasterId(tenant, master.getId());
        if (checkout != null) {
            throw exception(CONTROLLED_FILE_ALREADY_CHECKED_OUT, checkout.getActorId());
        }
        var attempt=DccRevisionReworkPolicy.requireAttempt(baseline,selected,intent.name(),chain,versionPolicy);
        if (attempt.predecessor()!=null) requireFailedProcess(attempt.predecessor());
        if (chain.stream().filter(v->attempt.predecessor()==null || !Objects.equals(v.getId(),attempt.predecessor().getId())).anyMatch(v -> !tenant.equals(v.getTenantId())
                || !versionPolicy.isControlledBaseline(v) && !Set.of("WORKING", "OBSOLETE", "REJECTED", "WITHDRAWN").contains(v.getStatus())
                || "WORKING".equals(v.getStatus()) && v.getRevisionChangeType() != null)) {
            throw exception(CONTROLLED_FILE_WORKFLOW_IN_PROGRESS);
        }
        // Intent advances its recorded baseline; a different chain maximum must not silently replace the source.
        var targetVersion = versionPolicy.formalTarget(baseline, intent.name());
        // Reserve both exact source name and formal number before copying any body.
        nameClaimService.claimExistingVersion(tenant, selectedRead.getSourceOriginalFileName(),
                master.getDccProjectCodeId(), master.getFileTypeTaxonomyLeafId(), master.getNormalizedFileNumber(), master.getId(),selected.getId());
        var candidate=freezeCandidate(userId,selectedRead,targetVersion,baselineRead,request,payloadHash,false,attempt);
        return candidate;
    }

    private DccControlledFileDO sourceIdentityProjection(DccControlledFileDO file) {
        var projection=new DccControlledFileDO();
        BeanUtils.copyProperties(file,projection);
        projection.setSourceOriginalFileName(nameClaimService.requireSourceName(file));
        return projection;
    }

    private void requireFailedProcess(DccControlledFileDO previous) {
        var history=bpmProcessInstanceService.getHistoricProcessInstance(previous.getProcessInstanceId());
        if(history==null || !Objects.equals(history.getBusinessKey(),previous.getId().toString())
                || !Objects.equals(history.getTenantId(),previous.getTenantId().toString())
                || !Objects.equals(history.getStartUserId(),previous.getRequesterId().toString())
                || !Objects.equals(history.getProcessDefinitionKey(),previous.getProcessDefinitionKey()))
            throw new IllegalArgumentException("failed attempt BPM identity does not match its frozen request");
        var running=bpmProcessInstanceService.getProcessInstance(previous.getProcessInstanceId());
        if("PENDING_APPLICANT_REWORK".equals(previous.getStatus())) {
            var tasks=bpmTaskService.getRunningTaskListByProcessInstanceId(previous.getProcessInstanceId(),null,null);
            if(running==null || history.getEndTime()!=null || tasks.size()!=1 || !"APPLICANT_REWORK".equals(tasks.get(0).getTaskDefinitionKey())
                    || !Objects.equals(tasks.get(0).getAssignee(),previous.getRequesterId().toString()))
                throw new IllegalArgumentException("failed attempt is not at its exact applicant rework task");
        } else if(running!=null || history.getEndTime()==null)throw new IllegalArgumentException("failed attempt BPM must be terminal before resubmission");
    }

    private void requireRequest(DccControlledFileSubmitIterationReqVO request) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("formal revision requires an active transaction");
        }
        if (request == null || StrUtil.isBlank(request.getIdempotencyKey()) || StrUtil.isBlank(request.getChangeDescription())) {
            throw new IllegalArgumentException("formal revision key and change description are required");
        }
        if (request.getProjectAttributes() == null || request.getSelectedSignoffDepartmentIds() == null
                || request.getNeedTraining() == null || request.getEffectiveDate() == null) {
            throw new IllegalArgumentException("formal revision actual attributes, departments, training and effective date are required");
        }
    }

    private DccControlledFileDO freezeCandidate(Long userId, DccControlledFileDO selected,
                                                DccControlledFileVersionPolicy.VersionNumber targetVersion,
                                                DccControlledFileDO baseline, DccControlledFileSubmitIterationReqVO request,
                                                String hash, boolean initial) {
        return freezeCandidate(userId,selected,targetVersion,baseline,request,hash,initial,null);
    }
    private DccControlledFileDO freezeCandidate(Long userId,DccControlledFileDO selected,
                DccControlledFileVersionPolicy.VersionNumber targetVersion,DccControlledFileDO baseline,
                DccControlledFileSubmitIterationReqVO request,String hash,boolean initial,DccRevisionReworkPolicy.Attempt attempt) {
        DccControlledFilePreparedSource frozen = null;
        DccControlledFilePreparedSource frozenPdf = null;
        try {
            frozen = sourceOwnershipService.createVerifiedCopy(selected.getSourceFileId());
            if (!frozen.isolatedCopy() || selected.getSourceSha256() == null || !selected.getSourceSha256().equals(frozen.sourceSha256())) {
                throw new IllegalStateException("selected working iteration source hash changed");
            }
            if (selected.getDrawingPdfFileId() != null) frozenPdf = sourceOwnershipService.createVerifiedCopy(selected.getDrawingPdfFileId());
            var target = new DccControlledFileDO();
            BeanUtils.copyProperties(selected, target);
            target.setId(null);
            target.setCreateTime(null); target.setUpdateTime(null); target.setCreator(null); target.setUpdater(null);
            target.setVersionNo(targetVersion.display()); target.setRevisionCode(targetVersion.majorIdentity());
            target.setIterationNo(targetVersion.iterationNo()); target.setSourceFileId(frozen.sourceFileId());
            target.setOriginalFileId(frozen.sourceFileId());
            target.setDrawingPdfFileId(frozenPdf == null ? null : frozenPdf.sourceFileId());
            target.setChangeType(initial ? "NEW" : "REVISION"); target.setStatus("WORKING");
            if (initial) recordInitialIntent(target);
            target.setCreationIdempotencyKey(request.getIdempotencyKey().trim()); target.setCreationPayloadHash(hash);
            target.setSubmitIdempotencyKey(null); target.setSubmitPayloadHash(null);
            target.setProcessInstanceId(null); target.setProcessDefinitionKey(null);
            target.setSubmittedTime(null); target.setApprovedTime(null); target.setPublishedTime(null);
            target.setControlledTime(null); target.setActivatedTime(null);
            target.setFileOwnerUserId(null);target.setFileOwnerUsernameSnapshot(null);target.setFileOwnerNicknameSnapshot(null);
            target.setFileOwnerSignatureId(null);target.setFileOwnerApprovalTaskId(null);target.setFileOwnerProcessInstanceId(null);target.setFileOwnerSelectedTime(null);
            target.setDistributedTime(null); target.setDistributionPayloadHash(null);
            target.setRejectedTime(null); target.setStampedTime(null); target.setObsoletedTime(null);
            target.setObsoletedBy(null); target.setObsoleteReason(null); target.setSupersededByFileId(null);
            target.setRejectReason(null); target.setFinalizationError(null);
            target.setPublishedFileId(null); target.setStampedFileId(null); target.setTrainingRecordFileId(null);
            target.setCheckedOutBy(null); target.setCheckedOutTime(null); target.setCheckedOutReason(null);
            target.setRequesterId(userId); target.setSubmitterId(userId);
            target.setChangeDescription(request.getChangeDescription().trim());
            target.setNeedTraining(request.getNeedTraining());
            target.setEffectiveDate(request.getEffectiveDate());
            target.setRevisionChangeType(request.getRevisionChangeType());
            target.setRevisionAttemptNo(attempt==null?null:attempt.number());
            target.setReworkPredecessorControlledFileId(attempt==null || attempt.predecessor()==null?null:attempt.predecessor().getId());
            target.setRevisionSourceControlledFileId(baseline == null ? null : baseline.getId()); target.setRevisionSourceVersionNo(baseline == null ? null : baseline.getVersionNo());
            target.setSelectedIterationControlledFileId(selected.getId()); target.setSelectedIterationVersionNo(selected.getVersionNo());
            target.setPredecessorControlledFileId(selected.getId()); target.setRevisionBaseActiveControlledFileId(baseline == null ? null : baseline.getId());
            target.setSourceSha256(frozen.sourceSha256()); target.setPreviousSourceSha256(baseline == null ? null : baseline.getSourceSha256());
            if (fileMapper.insert(target) != 1 || target.getId() == null) throw new IllegalStateException("formal revision insert failed");
            publicUploadPlacementService.inherit(userId,selected.getId(),target.getId(),request.getChangeDescription());
            sourceOwnershipService.claimSubmissionSource(target.getId(), frozen, userId, initial ? "INITIAL" : "REVISION");
            if (initial) relatedFileService.inheritRelatedFiles(selected.getId(), target.getId());
            else relatedFileService.freezeCurrentRelationsForRevision(userId, baseline.getId(), target.getId());
            DccControlledFileVersionSourceRollback.enlist(sourceOwnershipService, frozen, frozenPdf);
            return target;
        } catch (RuntimeException failure) {
            cleanup(frozen, failure); cleanup(frozenPdf, failure);
            throw failure;
        }
    }

    private String payloadHash(Long baseline, Long selected, DccControlledFileSubmitIterationReqVO request, DccControlledFileDO body) {
        var attributes = request.getProjectAttributes();
        if (attributes != null) {
            attributes.validated();
            attributes = new cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes(
                    attributes.targetMarkets().stream().sorted().toList(), attributes.otherMarket(),
                    attributes.licenseHolder(), attributes.actualManufacturer(), attributes.documentTransfer(), attributes.transferTo());
        }
        var departments = request.getSelectedSignoffDepartmentIds();
        if (departments != null) {
            if (departments.stream().anyMatch(id -> id == null || id < 1)
                    || departments.stream().distinct().count() != departments.size()) {
                throw new IllegalArgumentException("signoff department identities must be distinct positive IDs");
            }
            departments = departments.stream().sorted().toList();
        }
        String json = JSONUtil.toJsonStr(Arrays.asList(baseline, selected, request.getRevisionChangeType(),
                request.getChangeDescription().trim(), request.getNeedTraining(),
                request.getSelectedSignoffUserIds(), departments, attributes, request.getEffectiveDate().toString(),
                Arrays.asList(body.getVersionNo(),body.getSourceFileId(),body.getSourceSha256(),body.getDrawingPdfFileId(),body.getSourceOriginalFileName())));
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException failure) { throw new IllegalStateException("SHA-256 unavailable", failure); }
    }

    private boolean sameLogicalIdentity(DccControlledFileDO baseline, DccControlledFileDO selected) {
        return Objects.equals(baseline.getTenantId(), selected.getTenantId())
                && Objects.equals(baseline.getMasterId(), selected.getMasterId())
                && Objects.equals(baseline.getDccProjectCodeId(), selected.getDccProjectCodeId())
                && Objects.equals(baseline.getCategoryId(), selected.getCategoryId())
                && Objects.equals(baseline.getFileTypeTaxonomyId(), selected.getFileTypeTaxonomyId())
                && Objects.equals(baseline.getFileNumber(), selected.getFileNumber());
    }

    private void cleanup(DccControlledFilePreparedSource source, RuntimeException failure) {
        if (source == null) return;
        try { sourceOwnershipService.cleanupPreparedSource(source); }
        catch (RuntimeException cleanupFailure) { failure.addSuppressed(cleanupFailure); }
    }

    @Override public void recordInitialIntent(DccControlledFileDO file) {
        if (file == null || file.getId() != null || !"NEW".equals(file.getChangeType())) {
            throw new IllegalArgumentException("INITIAL applies only to an unpersisted NEW submission");
        }
        file.setRevisionChangeType("INITIAL");
        file.setRevisionSourceControlledFileId(null); file.setRevisionSourceVersionNo(null);
        file.setSelectedIterationControlledFileId(null); file.setSelectedIterationVersionNo(null);
    }
}
