package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileAttachmentRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAttachmentDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAttachmentMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFilePreviewKindEnum;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketBoundFile;
import cn.iocoder.yudao.module.dcc.service.upload.DccTrainingUploadTicketBoundFile;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;

@Service
@Validated
public class DccControlledFileAttachmentServiceImpl implements DccControlledFileAttachmentService {

    @Resource
    private DccControlledFileAttachmentMapper attachmentMapper;
    @Resource private DccControlledFileMapper controlledFileMapper;
    @Resource private DccOfflineTrainingRecordService offlineTraining;
    @Resource private AdminUserApi users;
    @Resource private FileMapper fileMapper;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileTemporaryFileMapper temporaryFileMapper;
    @Resource private cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketService uploadTicketService;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileCheckoutMapper checkoutMapper;
    @Resource private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService projectAccessService;
    @Resource private DccPublicUploadPlacementService publicUploadPlacementService;
    @Resource private DccControlledFileAssignmentScopeService assignmentScopeService;
    @Resource private DccControlledFileSourceOwnershipService sourceOwnershipService;
    @Resource private DccControlledFileVersionPolicy versionPolicy;

    @Override
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void bindCheckinAttachments(Long actorId, Long baseFileId, Long workingFileId,
            List<cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileAttachmentUploadTicketReqVO> tickets) {
        requireAttachmentTransaction(actorId, baseFileId, workingFileId);
        if (tickets == null) throw new IllegalArgumentException("请明确本次检入的普通附件清单");
        publicUploadPlacementService.lockSourceLocation(actorId, baseFileId);
        var source = requireAttachmentVersion(baseFileId);
        var target = requireAttachmentVersion(workingFileId);
        requireSameAttachmentIdentity(source, target);
        requireAttachmentActor(actorId, source);
        var sourceVersion = versionPolicy.requireStored(source);
        var targetVersion = versionPolicy.requireStored(target);
        var checkout = checkoutMapper.selectActiveByMasterId(TenantContextHolder.getRequiredTenantId(), source.getMasterId());
        if (checkout == null || !Objects.equals(checkout.getTenantId(), source.getTenantId())
                || !"ACTIVE".equals(checkout.getStatus()) || Boolean.TRUE.equals(checkout.getDeleted())
                || !Objects.equals(checkout.getActorId(), actorId) || !Objects.equals(checkout.getBaseIterationId(), baseFileId)
                || !Objects.equals(checkout.getMasterId(), source.getMasterId())
                || !Objects.equals(target.getRequesterId(), actorId) || !"WORKING".equals(target.getStatus())
                || target.getProcessInstanceId() != null || target.getRevisionChangeType() != null
                || (!java.util.Set.of("WORKING", "REJECTED", "WITHDRAWN", "PENDING_APPLICANT_REWORK")
                        .contains(String.valueOf(source.getStatus())) && !versionPolicy.isControlledBaseline(source))
                || !targetVersion.isWorkingIteration() || targetVersion.compareTo(sourceVersion) <= 0
                || !sourceVersion.formal().display().equals(targetVersion.formal().display())
                || !Objects.equals(target.getPredecessorControlledFileId(), baseFileId)) {
            throw new IllegalArgumentException("普通附件必须由真实检出人保存到本次检入的新工作版本");
        }
        if (!attachmentMapper.selectListByControlledFileId(workingFileId).isEmpty())
            throw new IllegalArgumentException("本次工作版本已保存附件，请走精确检入重放核对完整清单");
        var resolved = new java.util.ArrayList<DccUploadTicketBoundFile>();
        var seenTickets = new java.util.HashSet<String>();
        var seenSessions = new java.util.HashSet<String>();
        for (var input : tickets) {
            if (input == null || StrUtil.isBlank(input.getUploadTicket()) || StrUtil.isBlank(input.getSessionId())
                    || !input.getUploadTicket().equals(input.getUploadTicket().trim())
                    || !seenTickets.add(input.getUploadTicket()) || !seenSessions.add(input.getSessionId()))
                throw new IllegalArgumentException("每个普通附件必须使用独立会话和唯一正式上传票据");
            DccSourceUploadSession.require(input.getSessionId(), DccSourceUploadSession.checkinPrefix(baseFileId));
            var file = uploadTicketService.resolveForBinding(new cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketResolveCommand(
                    input.getUploadTicket(), actorId, source.getCategoryId(), input.getSessionId(), "ATTACHMENT"));
            var ticket = temporaryFileMapper.selectOne(
                    cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO::getUploadTicket, input.getUploadTicket());
            if (ticket == null || !Objects.equals(ticket.getTenantId(), source.getTenantId())
                    || Boolean.TRUE.equals(ticket.getDeleted()) || !Objects.equals(ticket.getUploaderId(), actorId)
                    || !Objects.equals(ticket.getCategoryId(), source.getCategoryId()) || !"ATTACHMENT".equals(ticket.getPurpose())
                    || !Objects.equals(ticket.getSessionId(), input.getSessionId()) || ticket.getCreateTime() == null)
                throw new IllegalArgumentException("普通附件正式票据与本次检入的租户、上传人或版本不一致");
            verifyOrdinaryBytes(file.storageFileId(), file.fileName(), file.contentType(), file.fileSize(), ticket.getFileSha256());
            if (Objects.equals(file.storageFileId(), target.getSourceFileId())
                    || Objects.equals(file.storageFileId(), target.getDrawingPdfFileId())
                    || Objects.equals(file.storageFileId(), source.getTrainingRecordFileId()))
                throw new IllegalArgumentException("普通附件不能冒用源文件、图纸PDF或历史培训记录");
            resolved.add(file);
        }
        // Resolve the complete manifest before the first write. Both steps join the caller's transaction.
        for (var input : tickets) uploadTicketService.markBound(new cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketMarkBoundCommand(
                input.getUploadTicket(), actorId, source.getCategoryId(), input.getSessionId(), "ATTACHMENT", workingFileId));
        bindAttachments(workingFileId, resolved);
    }

    @Override
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void freezeOrdinaryAttachments(Long actorId, Long selectedFileId, Long candidateFileId,
                                          List<Long> expectedOrdinaryAttachmentIds) {
        requireAttachmentTransaction(actorId, selectedFileId, candidateFileId);
        if (expectedOrdinaryAttachmentIds == null || expectedOrdinaryAttachmentIds.stream().anyMatch(id -> id == null || id <= 0)
                || expectedOrdinaryAttachmentIds.stream().distinct().count() != expectedOrdinaryAttachmentIds.size())
            throw new IllegalArgumentException("请核对所选工作版本完整且唯一的普通附件清单");
        publicUploadPlacementService.lockSourceLocation(actorId, selectedFileId);
        var source = requireAttachmentVersion(selectedFileId);
        var target = requireAttachmentVersion(candidateFileId);
        requireSameAttachmentIdentity(source, target);
        requireAttachmentActor(actorId, source);
        if (!"WORKING".equals(source.getStatus()) || !versionPolicy.requireStored(source).isWorkingIteration()
                || !"WORKING".equals(target.getStatus()) || versionPolicy.requireStored(target).isWorkingIteration()
                || target.getProcessInstanceId() != null || !Objects.equals(target.getRequesterId(), actorId)
                || !Objects.equals(target.getSelectedIterationControlledFileId(), selectedFileId)
                || !Objects.equals(target.getSelectedIterationVersionNo(), source.getVersionNo())
                || !Objects.equals(target.getPredecessorControlledFileId(), selectedFileId))
            throw new IllegalArgumentException("普通附件冻结需要精确所选工作稿和本次正式候选版本");
        requireFormalAttachmentTarget(source, target);
        if ("REPLACEMENT".equals(target.getRevisionChangeType())) {
            projectAccessService.assertProjectOwner(actorId, source.getDccProjectCodeId());
        } else if (!java.util.Set.of("PARTIAL", "INITIAL").contains(String.valueOf(target.getRevisionChangeType()))
                || !Objects.equals(source.getRequesterId(), actorId)) {
            throw new IllegalArgumentException("仅工作稿申请人或合法换版OWNER可以冻结本次普通附件");
        }
        var sourceRows = ordinaryRows(source);
        if (!expectedOrdinaryAttachmentIds.equals(sourceRows.stream().map(DccControlledFileAttachmentDO::getId).toList()))
            throw new IllegalArgumentException("所选工作版本的普通附件清单已变化，请重新读取并确认");
        for (var row : sourceRows) requireOrdinaryFacts(row);
        var existing = ordinaryRows(target);
        if (!existing.isEmpty()) {
            if (existing.size() != sourceRows.size()) throw new IllegalArgumentException("正式候选已有不同普通附件清单");
            for (int i = 0; i < existing.size(); i++) {
                var row = existing.get(i); var original = sourceRows.get(i);
                requireOrdinaryFacts(row);
                if (!sameOrdinaryFacts(original, row) || Objects.equals(original.getStorageFileId(), row.getStorageFileId())
                        || row.getApplicationProcessInstanceId() != null)
                    throw new IllegalArgumentException("正式候选普通附件重放与原冻结事实不一致");
            }
            return;
        }
        for (var original : sourceRows) {
            var copy = sourceOwnershipService.createVerifiedCopy(original.getStorageFileId());
            // Register immediately, so second attachment or any later caller failure removes all new copies.
            DccControlledFileVersionSourceRollback.enlist(sourceOwnershipService, copy);
            if (!copy.isolatedCopy() || Objects.equals(copy.sourceFileId(), original.getStorageFileId())
                    || !Objects.equals(copy.sourceSha256(), original.getFileSha256()))
                throw new IllegalStateException("普通附件独立副本与所选工作版本摘要不一致");
            var row = DccControlledFileAttachmentDO.builder().controlledFileId(candidateFileId)
                    .storageFileId(copy.sourceFileId()).originalFileName(original.getOriginalFileName())
                    .contentType(original.getContentType()).fileSize(original.getFileSize()).fileSha256(original.getFileSha256())
                    .sortNo(original.getSortNo()).attachmentPurpose("ATTACHMENT")
                    .uploadedBy(original.getUploadedBy()).uploadedUsernameSnapshot(original.getUploadedUsernameSnapshot())
                    .uploadedNicknameSnapshot(original.getUploadedNicknameSnapshot()).uploadedAt(original.getUploadedAt()).build();
            row.setTenantId(source.getTenantId());
            if (attachmentMapper.insert(row) != 1 || row.getId() == null || row.getId() <= 0)
                throw new IllegalStateException("正式候选普通附件冻结未返回真实持久化结果");
        }
    }

    @Override
    @Transactional(propagation=Propagation.MANDATORY,readOnly=true,rollbackFor=Exception.class)
    public OrdinaryAttachmentManifest readOrdinaryAttachmentManifest(Long actorId, Long selectedFileId) {
        if (TenantContextHolder.isIgnore() || actorId == null || actorId <= 0 || selectedFileId == null || selectedFileId <= 0
                || !TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalArgumentException("普通附件事实读取需要精确身份和正式业务事务");
        var selected = readAttachmentVersion(selectedFileId);
        requireAttachmentActor(actorId, selected);
        var facts = new java.util.ArrayList<OrdinaryAttachmentFact>();
        for (var row : ordinaryRows(selected)) {
            requireOrdinaryFacts(row);
            facts.add(new OrdinaryAttachmentFact(row.getId(), selectedFileId, row.getStorageFileId(), row.getSortNo(),
                    row.getAttachmentPurpose(), row.getOriginalFileName(), row.getContentType(), row.getFileSize(), row.getFileSha256(),
                    row.getUploadedBy(), row.getUploadedUsernameSnapshot(), row.getUploadedNicknameSnapshot(), row.getUploadedAt()));
        }
        String canonical = cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(
                java.util.Arrays.asList("DCC_ORDINARY_ATTACHMENT_MANIFEST_V1", selected.getTenantId(), selectedFileId, facts));
        return new OrdinaryAttachmentManifest(facts, cn.hutool.crypto.digest.DigestUtil.sha256Hex(canonical));
    }

    @Override
    @Transactional(propagation=Propagation.MANDATORY,readOnly=true,rollbackFor=Exception.class)
    public void requireFrozenOrdinaryAttachmentManifest(Long actorId, Long selectedFileId, Long candidateFileId,
                                                        OrdinaryAttachmentManifest expected) {
        requireAttachmentTransaction(actorId, selectedFileId, candidateFileId);
        if (expected == null) throw new IllegalArgumentException("正式候选缺少完整冻结普通附件事实");
        var source = requireAttachmentVersion(selectedFileId); var target = requireAttachmentVersion(candidateFileId);
        requireSameAttachmentIdentity(source, target); requireAttachmentActor(actorId, source);
        if (!Objects.equals(target.getRequesterId(), actorId) || !Objects.equals(target.getSelectedIterationControlledFileId(), selectedFileId)
                || !Objects.equals(target.getSelectedIterationVersionNo(), source.getVersionNo())
                || !Objects.equals(target.getPredecessorControlledFileId(), selectedFileId))
            throw new IllegalArgumentException("正式候选冻结附件重放与精确所选工作稿不一致");
        var current = readOrdinaryAttachmentManifest(actorId, selectedFileId);
        if (!expected.equals(current)) throw new IllegalArgumentException("工作稿普通附件冻结事实已改变");
        var rows = ordinaryRows(target);
        if (rows.size() != current.facts().size()) throw new IllegalArgumentException("正式候选冻结附件清单不完整");
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i); var fact = current.facts().get(i); requireOrdinaryFacts(row);
            if (Objects.equals(row.getStorageFileId(), fact.storageFileId()) || !Objects.equals(row.getSortNo(), fact.sortNo())
                    || !Objects.equals(row.getOriginalFileName(), fact.fileName()) || !Objects.equals(row.getFileSize(), fact.fileSize())
                    || !Objects.equals(row.getContentType(), fact.contentType()) || !Objects.equals(row.getFileSha256(), fact.fileSha256())
                    || !Objects.equals(row.getUploadedBy(), fact.uploadedBy()) || !Objects.equals(row.getUploadedAt(), fact.uploadedAt())
                    || !Objects.equals(row.getUploadedUsernameSnapshot(), fact.uploadedUsername())
                    || !Objects.equals(row.getUploadedNicknameSnapshot(), fact.uploadedNickname())
                    || !Objects.equals(row.getApplicationProcessInstanceId(), target.getProcessInstanceId()))
                throw new IllegalArgumentException("正式候选冻结附件与原上传事实、顺序或独立字节不一致");
        }
    }

    private void requireAttachmentTransaction(Long actorId, Long sourceId, Long targetId) {
        if (TenantContextHolder.isIgnore() || actorId == null || actorId <= 0 || sourceId == null || sourceId <= 0
                || targetId == null || targetId <= 0 || Objects.equals(sourceId, targetId)
                || !TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive())
            throw new IllegalArgumentException("普通附件写入需要真实租户、操作者、两个精确版本和正式业务事务");
        TenantContextHolder.getRequiredTenantId();
    }

    private void requireFormalAttachmentTarget(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO source,
                                               cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO target) {
        if ("INITIAL".equals(target.getRevisionChangeType())) {
            if (source.getRevisionBaseActiveControlledFileId() != null || target.getRevisionBaseActiveControlledFileId() != null
                    || target.getRevisionSourceControlledFileId() != null || target.getRevisionSourceVersionNo() != null
                    || !versionPolicy.requireStored(source).formal().display().equals(target.getVersionNo()))
                throw new IllegalArgumentException("初始正式候选必须对应所选工作稿的准确正式版本身份");
            return;
        }
        Long baselineId = target.getRevisionSourceControlledFileId();
        if (baselineId == null || !Objects.equals(baselineId, target.getRevisionBaseActiveControlledFileId())
                || !Objects.equals(baselineId, source.getRevisionBaseActiveControlledFileId()))
            throw new IllegalArgumentException("正式候选的受控来源与所选工作稿基线不一致");
        var baseline = requireAttachmentVersion(baselineId);
        requireSameAttachmentIdentity(baseline, source);
        if (baseline.getControlledTime() == null || !DccControlledFileVersionPolicy.isCurrentControlledStatus(baseline.getStatus())
                || !Objects.equals(baseline.getVersionNo(), target.getRevisionSourceVersionNo())
                || !versionPolicy.formalTarget(baseline, target.getRevisionChangeType()).display().equals(target.getVersionNo()))
            throw new IllegalArgumentException("正式候选版本必须按准确受控来源和本次局部变更或换版意图生成");
    }

    private cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO requireAttachmentVersion(Long id) {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        var file = controlledFileMapper.selectByIdAndTenantForUpdate(tenant, id);
        return requireAttachmentVersionIdentity(file, id, tenant);
    }

    private cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO readAttachmentVersion(Long id) {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        return requireAttachmentVersionIdentity(controlledFileMapper.selectById(id), id, tenant);
    }

    private cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO requireAttachmentVersionIdentity(
            cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO file, Long id, Long tenant) {
        if (file == null || !Objects.equals(file.getId(), id) || !Objects.equals(file.getTenantId(), tenant)
                || Boolean.TRUE.equals(file.getDeleted()) || file.getMasterId() == null || file.getMasterId() <= 0
                || file.getDccProjectCodeId() == null || file.getCategoryId() == null)
            throw new IllegalArgumentException("普通附件对应的精确版本或租户不存在");
        return file;
    }

    private void requireSameAttachmentIdentity(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO source,
                                               cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO target) {
        if (!Objects.equals(source.getMasterId(), target.getMasterId())
                || !Objects.equals(source.getDccProjectCodeId(), target.getDccProjectCodeId())
                || !Objects.equals(source.getCategoryId(), target.getCategoryId())
                || !Objects.equals(source.getFileTypeTaxonomyId(), target.getFileTypeTaxonomyId())
                || StrUtil.isBlank(source.getFileNumber()) || !Objects.equals(source.getFileNumber(), target.getFileNumber())
                || !Objects.equals(source.getDirectoryId(), target.getDirectoryId()))
            throw new IllegalArgumentException("普通附件不能跨逻辑文件、项目、类别或目录保存");
    }

    private void requireAttachmentActor(Long actorId, cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO source) {
        var actor = users.getUser(actorId);
        if (actor == null || !Objects.equals(actor.getId(), actorId)
                || !Objects.equals(actor.getTenantId(), source.getTenantId()) || !Integer.valueOf(0).equals(actor.getStatus())
                || !assignmentScopeService.isWithinAssignedFileScope(actorId, source.getId())
                || !permissionSupport.hasCategoryPermission(source.getCategoryId(), actorId,
                    cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum.UPLOAD))
            throw new IllegalArgumentException("普通附件操作者没有本租户当前版本的有效上传权限");
        projectAccessService.assertProjectEditorOrOwner(actorId, source.getDccProjectCodeId());
    }

    private List<DccControlledFileAttachmentDO> ordinaryRows(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO file) {
        var rows = Objects.requireNonNull(attachmentMapper.selectListByControlledFileId(file.getId()), "正式附件清单");
        for (var row : rows) {
            if (!Objects.equals(row.getTenantId(), file.getTenantId()) || !Objects.equals(row.getControlledFileId(), file.getId())
                    || Boolean.TRUE.equals(row.getDeleted()) || row.getId() == null || row.getId() <= 0
                    || !java.util.Set.of("ATTACHMENT", "TRAINING_RECORD").contains(String.valueOf(row.getAttachmentPurpose())))
                throw new IllegalArgumentException("附件清单含错误版本、租户或未记录用途的事实");
        }
        return rows.stream().filter(row -> "ATTACHMENT".equals(row.getAttachmentPurpose())).toList();
    }

    private void requireOrdinaryFacts(DccControlledFileAttachmentDO row) {
        if (row.getUploadedBy() == null || row.getUploadedBy() <= 0 || row.getUploadedAt() == null
                || StrUtil.isBlank(row.getUploadedUsernameSnapshot()) || StrUtil.isBlank(row.getUploadedNicknameSnapshot())
                || row.getSortNo() == null || row.getSortNo() < 0)
            throw new IllegalArgumentException("普通附件缺少原始上传人、时间或清单顺序事实");
        verifyOrdinaryBytes(row.getStorageFileId(), row.getOriginalFileName(), row.getContentType(), row.getFileSize(), row.getFileSha256());
    }

    private void verifyOrdinaryBytes(Long storageId, String name, String type, Long size, String hash) {
        var file = storageId == null ? null : fileMapper.selectById(storageId);
        if (file == null || storageId <= 0 || !Objects.equals(file.getId(), storageId) || Boolean.TRUE.equals(file.getDeleted())
                || StrUtil.isBlank(name) || !Objects.equals(file.getName(), name) || StrUtil.isBlank(type)
                || !Objects.equals(file.getType(), type) || size == null || size <= 0 || !Objects.equals(file.getSize(), size)
                || file.getConfigId() == null || StrUtil.isBlank(file.getPath()) || hash == null || !hash.matches("[a-f0-9]{64}")
                || !Objects.equals(sourceOwnershipService.inspectSource(storageId).sourceSha256(), hash))
            throw new IllegalArgumentException("普通附件实际存储内容与已保存身份或SHA-256摘要不一致");
    }

    private boolean sameOrdinaryFacts(DccControlledFileAttachmentDO source, DccControlledFileAttachmentDO target) {
        return Objects.equals(source.getOriginalFileName(), target.getOriginalFileName())
                && Objects.equals(source.getContentType(), target.getContentType()) && Objects.equals(source.getFileSize(), target.getFileSize())
                && Objects.equals(source.getFileSha256(), target.getFileSha256()) && Objects.equals(source.getSortNo(), target.getSortNo())
                && Objects.equals(source.getUploadedBy(), target.getUploadedBy()) && Objects.equals(source.getUploadedAt(), target.getUploadedAt())
                && Objects.equals(source.getUploadedUsernameSnapshot(), target.getUploadedUsernameSnapshot())
                && Objects.equals(source.getUploadedNicknameSnapshot(), target.getUploadedNicknameSnapshot());
    }

    /** Caller resolves the formal actor/category/round-bound ticket before entering this command.
     * Attachment, ticket binding, file projection and receive-task progression share its transaction. */
    @Override
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public Long bindTrainingRecord(Long actorId,Long controlledFileId,String processInstanceId,
                                   DccTrainingUploadTicketBoundFile trainingFile) {
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("培训附件绑定必须与票据、文件和流程推进使用同一正式事务");
        Long tenant=TenantContextHolder.getRequiredTenantId();
        if(trainingFile==null || trainingFile.file()==null || trainingFile.fileSha256()==null
                || !trainingFile.fileSha256().matches("[a-f0-9]{64}"))
            throw new IllegalArgumentException("培训附件缺少正式票据的准确SHA-256摘要");
        DccUploadTicketBoundFile boundFile=trainingFile.file();
        if (actorId==null || actorId<=0 || controlledFileId==null || controlledFileId<=0
                || processInstanceId==null || processInstanceId.isBlank() || !processInstanceId.equals(processInstanceId.strip())
                || processInstanceId.length()>128 || boundFile==null || boundFile.uploadTicket()==null
                || boundFile.uploadTicket().isBlank() || boundFile.storageFileId()==null || boundFile.storageFileId()<=0
                || boundFile.fileName()==null || boundFile.fileName().isBlank() || boundFile.fileName().length()>512
                || boundFile.contentType()==null || boundFile.contentType().isBlank() || boundFile.contentType().length()>255
                || boundFile.fileSize()==null || boundFile.fileSize()<=0)
            throw new IllegalArgumentException("培训附件需要精确版本、真实轮次及已验证上传文件");
        var current=controlledFileMapper.selectByIdAndTenantForUpdate(tenant,controlledFileId);
        if (current==null || !Objects.equals(current.getId(),controlledFileId) || !Objects.equals(current.getTenantId(),tenant)
                || Boolean.TRUE.equals(current.getDeleted()) || !Objects.equals(current.getRequesterId(),actorId)
                || !processInstanceId.equals(current.getProcessInstanceId()) || !Boolean.TRUE.equals(current.getNeedTraining()))
            throw new IllegalArgumentException("仅本轮文件上传人可绑定对应版本的培训记录");
        var account=users.getUser(actorId);
        if (account==null || !Objects.equals(account.getId(),actorId) || !Objects.equals(account.getTenantId(),tenant)
                || !Integer.valueOf(0).equals(account.getStatus()) || account.getUsername()==null || account.getUsername().isBlank()
                || account.getUsername().length()>128 || account.getNickname()==null || account.getNickname().isBlank()
                || account.getNickname().length()>128)
            throw new IllegalArgumentException("培训附件上传人必须是本租户正式启用账号");
        if (!List.of(actorId).equals(offlineTraining.requireConfiguredRecipients(current)))
            throw new IllegalStateException("培训附件上传职责与本轮上传人不一致");
        var physical=fileMapper.selectById(boundFile.storageFileId());
        if (physical==null || !Objects.equals(physical.getId(),boundFile.storageFileId()) || Boolean.TRUE.equals(physical.getDeleted())
                || !Objects.equals(physical.getName(),boundFile.fileName()) || !Objects.equals(physical.getType(),boundFile.contentType())
                || !Objects.equals(physical.getSize(),boundFile.fileSize()) || physical.getConfigId()==null
                || physical.getPath()==null || physical.getPath().isBlank())
            throw new IllegalArgumentException("培训附件实际存储文件与正式上传票据不一致");
        var existing=attachmentMapper.selectTrainingRecordForUpdate(tenant,controlledFileId,processInstanceId);
        if (existing!=null) {
            if (!Objects.equals(existing.getTenantId(),tenant) || !Objects.equals(existing.getControlledFileId(),controlledFileId)
                    || Boolean.TRUE.equals(existing.getDeleted())
                    || !processInstanceId.equals(existing.getApplicationProcessInstanceId()) || !"TRAINING_RECORD".equals(existing.getAttachmentPurpose())
                    || !Objects.equals(existing.getUploadedBy(),actorId) || !Objects.equals(existing.getStorageFileId(),boundFile.storageFileId())
                    || !Objects.equals(existing.getOriginalFileName(),boundFile.fileName()) || !Objects.equals(existing.getContentType(),boundFile.contentType())
                    || !Objects.equals(existing.getFileSize(),boundFile.fileSize()) || !Objects.equals(existing.getFileSha256(),trainingFile.fileSha256())
                    || existing.getId()==null || existing.getUploadedAt()==null
                    || existing.getUploadedUsernameSnapshot()==null || existing.getUploadedUsernameSnapshot().isBlank()
                    || existing.getUploadedNicknameSnapshot()==null || existing.getUploadedNicknameSnapshot().isBlank()
                    || current.getTrainingRecordFileId()!=null && !Objects.equals(current.getTrainingRecordFileId(),existing.getStorageFileId()))
                throw new IllegalArgumentException("同版本同轮培训附件已存在不同正文或不完整上传事实");
            return existing.getId();
        }
        var waiting=offlineTraining.requireUpload(actorId,current);
        if (waiting==null || !processInstanceId.equals(waiting.processInstanceId()) || waiting.file()==null
                || !Objects.equals(waiting.file().getId(),controlledFileId))
            throw new IllegalStateException("培训附件没有精确的活跃上传任务");
        var attachment=DccControlledFileAttachmentDO.builder().controlledFileId(controlledFileId)
                .storageFileId(boundFile.storageFileId()).originalFileName(boundFile.fileName()).contentType(boundFile.contentType())
                .fileSize(boundFile.fileSize()).fileSha256(trainingFile.fileSha256()).sortNo(Integer.MAX_VALUE).attachmentPurpose("TRAINING_RECORD")
                .applicationProcessInstanceId(processInstanceId).uploadedBy(actorId).uploadedUsernameSnapshot(account.getUsername())
                .uploadedNicknameSnapshot(account.getNickname()).uploadedAt(LocalDateTime.now().withNano(0)).build();
        attachment.setTenantId(tenant);
        if (attachmentMapper.insert(attachment)!=1 || attachment.getId()==null || attachment.getId()<=0)
            throw new IllegalStateException("培训附件绑定未返回真实持久化结果");
        return attachment.getId();
    }

    @Override
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void bindAttachments(Long controlledFileId, List<DccUploadTicketBoundFile> attachmentFiles) {
        if (attachmentFiles == null || attachmentFiles.isEmpty()) {
            return;
        }
        if (controlledFileId==null || controlledFileId<=0
                || !TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("附件绑定需要正式版本及调用方业务事务");
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var owner = controlledFileMapper.selectByIdAndTenantForUpdate(tenant, controlledFileId);
        if (owner == null || !Objects.equals(owner.getId(), controlledFileId) || !Objects.equals(owner.getTenantId(), tenant)
                || Boolean.TRUE.equals(owner.getDeleted()) || !"WORKING".equals(owner.getStatus()))
            throw new IllegalStateException("普通附件只能在本版本申请保存事务内绑定");
        var rows = new java.util.ArrayList<DccControlledFileAttachmentDO>();
        var seen = new java.util.HashSet<String>();
        for (DccUploadTicketBoundFile file : attachmentFiles) {
            if (file == null || file.storageFileId() == null || file.storageFileId()<=0
                    || StrUtil.isBlank(file.fileName()) || file.fileName().length()>512 || StrUtil.isBlank(file.uploadTicket())
                    || !seen.add(file.uploadTicket())) {
                throw exception(CONTROLLED_FILE_NOT_EXISTS);
            }
            var ticket = temporaryFileMapper.selectOne(
                    cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO::getUploadTicket, file.uploadTicket());
            if (ticket == null || !Objects.equals(ticket.getTenantId(), tenant) || Boolean.TRUE.equals(ticket.getDeleted())
                    || !"ATTACHMENT".equals(ticket.getPurpose()) || !"BOUND".equals(ticket.getStatus())
                    || !Objects.equals(ticket.getBoundControlledFileId(), controlledFileId)
                    || !Objects.equals(ticket.getUploaderId(), owner.getRequesterId()) || !Objects.equals(ticket.getCategoryId(), owner.getCategoryId())
                    || !Objects.equals(ticket.getStorageFileId(), file.storageFileId())
                    || !Objects.equals(ticket.getOriginalFileName(), file.fileName()) || !Objects.equals(ticket.getContentType(), file.contentType())
                    || !Objects.equals(ticket.getFileSize(), file.fileSize()) || ticket.getCreateTime() == null
                    || ticket.getFileSha256() == null || !ticket.getFileSha256().matches("[a-f0-9]{64}"))
                throw new IllegalStateException("普通附件与本次正式上传票据、上传人或版本不一致");
            var actor = users.getUser(ticket.getUploaderId());
            if (actor == null || !Objects.equals(actor.getId(), ticket.getUploaderId()) || !Objects.equals(actor.getTenantId(), tenant)
                    || !Integer.valueOf(0).equals(actor.getStatus()) || StrUtil.isBlank(actor.getUsername()) || StrUtil.isBlank(actor.getNickname()))
                throw new IllegalStateException("普通附件缺少真实上传人账号快照");
            var physical = fileMapper.selectById(file.storageFileId());
            if (physical == null || !Objects.equals(physical.getId(), file.storageFileId()) || Boolean.TRUE.equals(physical.getDeleted())
                    || !Objects.equals(physical.getName(), file.fileName()) || !Objects.equals(physical.getType(), file.contentType())
                    || !Objects.equals(physical.getSize(), file.fileSize()) || physical.getConfigId() == null || StrUtil.isBlank(physical.getPath()))
                throw new IllegalStateException("普通附件与实际存储文件不一致");
            var attachment=DccControlledFileAttachmentDO.builder()
                    .controlledFileId(controlledFileId)
                    .storageFileId(file.storageFileId())
                    .originalFileName(file.fileName())
                    .contentType(file.contentType())
                    .fileSize(file.fileSize())
                    .sortNo(rows.size())
                    .attachmentPurpose("ATTACHMENT")
                    .fileSha256(ticket.getFileSha256()).uploadedBy(actor.getId()).uploadedUsernameSnapshot(actor.getUsername())
                    .uploadedNicknameSnapshot(actor.getNickname()).uploadedAt(ticket.getCreateTime())
                    .applicationProcessInstanceId(owner.getProcessInstanceId())
                    .build();
            attachment.setTenantId(tenant);
            rows.add(attachment);
        }
        for (var attachment : rows) {
            if (attachmentMapper.insert(attachment)!=1 || attachment.getId() == null)
                throw new IllegalStateException("附件绑定未返回真实持久化结果");
        }
    }

    @Override
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void bindApplicationRound(Long controlledFileId, String processInstanceId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive() || processInstanceId == null || processInstanceId.isBlank())
            throw new IllegalStateException("普通附件申请轮次绑定需要真实业务事务和BPM身份");
        Long tenant = TenantContextHolder.getRequiredTenantId();
        var rows = attachmentMapper.selectListByControlledFileId(controlledFileId);
        if (rows.isEmpty()) return;
        var file = controlledFileMapper.selectByIdAndTenantForUpdate(tenant, controlledFileId);
        if (file == null || !Objects.equals(file.getId(), controlledFileId) || !Objects.equals(file.getTenantId(), tenant)
                || Boolean.TRUE.equals(file.getDeleted()) || !processInstanceId.equals(file.getProcessInstanceId()))
            throw new IllegalStateException("普通附件申请轮次与当前正式文件不一致");
        for (var row : rows) {
            if (!Objects.equals(row.getTenantId(), tenant) || !Objects.equals(row.getControlledFileId(), controlledFileId)
                    || Boolean.TRUE.equals(row.getDeleted())) throw new IllegalStateException("普通附件清单包含其它版本或租户");
            // An attachment's first application is immutable, including when that version is resubmitted.
            if ("ATTACHMENT".equals(row.getAttachmentPurpose()) && row.getApplicationProcessInstanceId() == null
                    && row.getUploadedBy() != null && row.getUploadedAt() != null) {
                if (attachmentMapper.updateById(DccControlledFileAttachmentDO.builder().id(row.getId())
                        .applicationProcessInstanceId(processInstanceId).build()) != 1)
                    throw new IllegalStateException("普通附件申请轮次未成功绑定");
            }
        }
    }

    @Override
    public List<DccControlledFileAttachmentRespVO> listAttachments(Long controlledFileId) {
        return attachmentMapper.selectListByControlledFileId(controlledFileId).stream()
                .map(this::toRespVO)
                .toList();
    }

    @Override
    public DccControlledFileAttachmentBinary getAttachment(Long controlledFileId, Long attachmentId) {
        Long tenant=TenantContextHolder.getRequiredTenantId();
        if (controlledFileId==null || controlledFileId<=0 || attachmentId==null || attachmentId<=0)
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        DccControlledFileAttachmentDO attachment = attachmentId == null ? null : attachmentMapper.selectById(attachmentId);
        if (attachment == null || !Objects.equals(attachment.getId(),attachmentId)
                || !Objects.equals(attachment.getTenantId(),tenant) || Boolean.TRUE.equals(attachment.getDeleted())
                || !controlledFileId.equals(attachment.getControlledFileId())) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        return new DccControlledFileAttachmentBinary(attachment.getId(), attachment.getStorageFileId(),
                attachment.getOriginalFileName(), attachment.getContentType(), attachment.getFileSize());
    }

    @Override
    public List<Long> listControlledFileIdsByStorageFileId(Long storageFileId) {
        if (storageFileId == null) {
            return List.of();
        }
        return attachmentMapper.selectList(new LambdaQueryWrapperX<DccControlledFileAttachmentDO>()
                        .eq(DccControlledFileAttachmentDO::getStorageFileId, storageFileId))
                .stream()
                .map(DccControlledFileAttachmentDO::getControlledFileId)
                .distinct()
                .toList();
    }

    private DccControlledFileAttachmentRespVO toRespVO(DccControlledFileAttachmentDO attachment) {
        DccControlledFileAttachmentRespVO respVO = new DccControlledFileAttachmentRespVO();
        respVO.setAttachmentId(attachment.getId());
        respVO.setControlledFileId(attachment.getControlledFileId());
        respVO.setFileName(attachment.getOriginalFileName());
        respVO.setContentType(attachment.getContentType());
        respVO.setFileSize(attachment.getFileSize());
        respVO.setSortNo(attachment.getSortNo());
        respVO.setAttachmentPurpose(attachment.getAttachmentPurpose());
        respVO.setApplicationProcessInstanceId(attachment.getApplicationProcessInstanceId());
        respVO.setUploadedBy(attachment.getUploadedBy());
        respVO.setUploadedUsernameSnapshot(attachment.getUploadedUsernameSnapshot());
        respVO.setUploadedNicknameSnapshot(attachment.getUploadedNicknameSnapshot());
        respVO.setUploadedAt(attachment.getUploadedAt());
        respVO.setPreviewKind(DccControlledFilePreviewKindEnum
                .resolve(attachment.getOriginalFileName(), attachment.getContentType()).getCode());
        return respVO;
    }

}
