package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryViewMatrixRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionRecipientDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRelatedFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationFollowupBatchDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationCandidateDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationCandidateReasonDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationRelationDirectionSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationRelationSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationVisibilityRuleSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationVisibilityUserSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryViewMatrixRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationFollowupBatchMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationCandidateMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationCandidateReasonMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationRelationDirectionSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationRelationSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationVisibilityRuleSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationVisibilityUserSnapshotMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DccPublicationFollowupServiceImpl implements DccPublicationFollowupService {

    static final String SOURCE_FILE_REQUESTER = "FILE_REQUESTER";
    static final String SOURCE_CURRENT_VIEW_MATRIX = "CURRENT_VIEW_MATRIX";
    static final String SOURCE_PUBLIC_FOLDER_DISTRIBUTION = "PUBLIC_FOLDER_DISTRIBUTION";
    static final String REASON_FILE_OWNER = "FILE_OWNER";
    static final String REASON_FORMAL_DISTRIBUTION = "FORMAL_DISTRIBUTION";
    static final String REASON_RELATED_FILE_OWNER = "RELATED_FILE_OWNER";
    static final String DIRECTION_FORWARD = "FORWARD";
    static final String DIRECTION_REVERSE = "REVERSE";
    private static final String MEDIUM_PUBLIC_FOLDER = "PUBLIC_FOLDER";
    private static final String BATCH_STATUS_PENDING = "PENDING";

    @Resource private DccPublicationFollowupBatchMapper batchMapper;
    @Resource private DccPublicationVisibilityRuleSnapshotMapper visibilityRuleMapper;
    @Resource private DccPublicationVisibilityUserSnapshotMapper visibilityUserMapper;
    @Resource private DccPublicationNotificationCandidateMapper candidateMapper;
    @Resource private DccPublicationNotificationCandidateReasonMapper candidateReasonMapper;
    @Resource private DccPublicationRelationSnapshotMapper relationSnapshotMapper;
    @Resource private DccPublicationRelationDirectionSnapshotMapper relationDirectionMapper;
    @Resource private DccCategoryViewMatrixRuleMapper viewMatrixRuleMapper;
    @Resource private DccControlledFileViewMatrixAccessService viewMatrixAccessService;
    @Resource private DccControlledFileAssignmentScopeService assignmentScopeService;
    @Resource private DccControlledFileDistributionMapper distributionMapper;
    @Resource private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Resource private DccControlledFileRelatedFileService relatedFileService;
    @Resource private DccControlledFileMasterMapper masterMapper;
    @Resource private DccControlledFileMapper controlledFileMapper;
    @Resource private AdminUserApi adminUserApi;
    @Resource private DeptApi deptApi;
    @Resource private DccRelatedFileImpactAssessmentService impactAssessmentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordPublishedRevision(DccControlledFileDO publishedFile, DccControlledFileDO previousActiveFile) {
        requirePublicationIdentity(publishedFile);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (publishedFile.getTenantId() != null && !tenantId.equals(publishedFile.getTenantId())) {
            throw new IllegalStateException("Published controlled file tenant does not match current tenant");
        }
        LocalDateTime frozenAt = publishedFile.getPublishedTime().withNano(0);
        String creationToken = UUID.randomUUID().toString();
        DccPublicationFollowupBatchDO proposedBatch = DccPublicationFollowupBatchDO.builder()
                .publishedControlledFileId(publishedFile.getId())
                .publishedMasterId(publishedFile.getMasterId())
                .previousActiveControlledFileId(previousActiveFile == null ? null : previousActiveFile.getId())
                .dccProjectCodeId(publishedFile.getDccProjectCodeId())
                .categoryId(publishedFile.getCategoryId())
                .directoryId(publishedFile.getDirectoryId())
                .fileTypeTaxonomyLeafId(publishedFile.getFileTypeTaxonomyId())
                .fileNumberSnapshot(publishedFile.getFileNumber())
                .fileNameSnapshot(publishedFile.getFileName())
                .versionNoSnapshot(publishedFile.getVersionNo())
                .status(BATCH_STATUS_PENDING)
                .publishedAt(frozenAt)
                .creationToken(creationToken)
                .build();
        proposedBatch.setTenantId(tenantId);
        batchMapper.insertOrKeepExisting(proposedBatch);
        DccPublicationFollowupBatchDO batch = Objects.requireNonNull(
                batchMapper.selectByPublishedControlledFileId(tenantId, publishedFile.getId()),
                "publication follow-up batch must exist after idempotent insert");
        requireMatchingBatchIdentity(batch, publishedFile, previousActiveFile, frozenAt);
        if (!creationToken.equals(batch.getCreationToken())) {
            return;
        }
        if (batch.getId() == null) {
            throw new IllegalStateException("Publication follow-up batch id is missing after insert");
        }

        List<VisibilityRuleDraft> visibilityRules = resolveVisibilityRules(publishedFile);
        Map<Long, RelationDraft> relationDrafts = resolveRelations(tenantId, publishedFile);
        Map<Long, List<CandidateReasonDraft>> candidateReasons = resolveCandidateReasons(
                publishedFile, visibilityRules, relationDrafts);
        applyAssignmentScope(visibilityRules, publishedFile.getId());

        Set<Long> allSnapshotUserIds = new LinkedHashSet<>();
        visibilityRules.forEach(rule -> rule.users().forEach(user -> allSnapshotUserIds.add(user.userId())));
        allSnapshotUserIds.addAll(candidateReasons.keySet());
        UserDirectory userDirectory = resolveUserDirectory(allSnapshotUserIds);

        insertVisibilitySnapshots(batch, publishedFile, visibilityRules, userDirectory);
        insertRelationSnapshots(batch, relationDrafts, userDirectory, frozenAt);
        impactAssessmentService.materializeForPublicationBatch(batch.getId());
        insertNotificationCandidates(batch, candidateReasons, userDirectory);
        impactAssessmentService.resolveLinkedRevisionAfterPublication(publishedFile);
    }

    private List<VisibilityRuleDraft> resolveVisibilityRules(DccControlledFileDO file) {
        List<VisibilityRuleDraft> rules = new ArrayList<>();
        rules.add(new VisibilityRuleDraft(SOURCE_FILE_REQUESTER, file.getId(), "DIRECT", "USER",
                file.getRequesterId(), "发布版本责任人", "RESOLVED", null,
                new ArrayList<>(List.of(new VisibilityUserDraft(file.getRequesterId(), null, "申请人自查")))));

        List<DccCategoryViewMatrixRuleDO> matrixRules = Objects.requireNonNull(
                viewMatrixRuleMapper.selectActiveListByCategoryId(file.getCategoryId()),
                "active view matrix rules must not be null").stream()
                .sorted(Comparator.comparing(DccCategoryViewMatrixRuleDO::getId))
                .toList();
        for (DccCategoryViewMatrixRuleDO matrixRule : matrixRules) {
            DccControlledFileViewMatrixAccessService.ViewMatrixRuleInput input =
                    viewMatrixAccessService.toInput(matrixRule);
            DccControlledFileViewMatrixAccessService.ViewMatrixAccessResolution resolution =
                    Objects.requireNonNull(viewMatrixAccessService.resolveRules(file.getCategoryId(), List.of(input)),
                            "view matrix resolution must not be null");
            List<DccControlledFileViewMatrixAccessService.ViewMatrixAccessSubject> subjects =
                    Objects.requireNonNull(resolution.subjects(), "view matrix subjects must not be null");
            List<DccControlledFileViewMatrixAccessService.ViewMatrixAccessRisk> risks =
                    Objects.requireNonNull(resolution.risks(), "view matrix risks must not be null");
            String resolutionStatus = risks.stream().anyMatch(risk -> Boolean.TRUE.equals(risk.blocking()))
                    ? "UNRESOLVED" : subjects.isEmpty() ? "EMPTY" : "RESOLVED";
            String resolutionMessage = risks.stream().map(
                            DccControlledFileViewMatrixAccessService.ViewMatrixAccessRisk::message)
                    .filter(StrUtil::isNotBlank).distinct().collect(Collectors.joining("; "));
            List<VisibilityUserDraft> users = subjects.stream()
                    .filter(subject -> subject.userId() != null)
                    .map(subject -> new VisibilityUserDraft(subject.userId(), subject.userName(), subject.reason()))
                    .collect(Collectors.toCollection(ArrayList::new));
            rules.add(new VisibilityRuleDraft(SOURCE_CURRENT_VIEW_MATRIX, matrixRule.getId(),
                    matrixRule.getScopeType(), matrixRule.getSubjectType(), matrixRule.getSubjectId(),
                    describeMatrixRule(matrixRule), resolutionStatus, StrUtil.trimToNull(resolutionMessage), users));
        }

        List<DccControlledFileDistributionDO> distributions = Objects.requireNonNull(
                distributionMapper.selectListByControlledFileId(file.getId()),
                "formal distribution records must not be null").stream()
                .filter(distribution -> StrUtil.equalsIgnoreCase(
                        MEDIUM_PUBLIC_FOLDER, distribution.getDistributionMedium()))
                .sorted(Comparator.comparing(DccControlledFileDistributionDO::getId))
                .toList();
        for (DccControlledFileDistributionDO distribution : distributions) {
            List<DccControlledFileDistributionRecipientDO> recipients = Objects.requireNonNull(
                    distributionRecipientMapper.selectListByDistributionId(distribution.getId()),
                    "formal distribution recipients must not be null");
            if (recipients.isEmpty()) {
                throw new IllegalStateException("Public-folder distribution has no formal recipient");
            }
            List<VisibilityUserDraft> users = recipients.stream()
                    .filter(recipient -> recipient.getUserId() != null)
                    .collect(Collectors.toMap(DccControlledFileDistributionRecipientDO::getUserId,
                            recipient -> new VisibilityUserDraft(recipient.getUserId(), null,
                                    "有效电子分发收件人"), (left, right) -> left, LinkedHashMap::new))
                    .values().stream().collect(Collectors.toCollection(ArrayList::new));
            rules.add(new VisibilityRuleDraft(SOURCE_PUBLIC_FOLDER_DISTRIBUTION, distribution.getId(),
                    MEDIUM_PUBLIC_FOLDER, "DEPT", distribution.getDepartmentId(),
                    "正式电子分发部门#" + distribution.getDepartmentId(),
                    users.isEmpty() ? "EMPTY" : "RESOLVED", null, users));
        }
        return rules;
    }

    private Map<Long, RelationDraft> resolveRelations(Long tenantId, DccControlledFileDO publishedFile) {
        List<DccControlledFileRelatedFileDO> forwardRelations = Objects.requireNonNull(
                relatedFileService.listForwardRelations(publishedFile.getId()),
                "forward controlled-file relations must not be null");
        List<DccControlledFileRelatedFileDO> reverseRelations = Objects.requireNonNull(
                relatedFileService.listReverseCurrentActiveRelations(tenantId, publishedFile.getMasterId()),
                "reverse controlled-file relations must not be null");

        Set<Long> reverseSourceFileIds = reverseRelations.stream()
                .map(DccControlledFileRelatedFileDO::getControlledFileId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, DccControlledFileDO> reverseSourceFiles = loadFiles(reverseSourceFileIds,
                "reverse relation source controlled file");
        Map<Long, RelationDraft> drafts = new LinkedHashMap<>();
        for (DccControlledFileRelatedFileDO relation : forwardRelations) {
            Long relatedMasterId = Objects.requireNonNull(relation.getRelatedMasterId(),
                    "forward relation relatedMasterId must not be null");
            if (!publishedFile.getMasterId().equals(relatedMasterId)) {
                drafts.computeIfAbsent(relatedMasterId, RelationDraft::new).directions().add(
                        direction(DIRECTION_FORWARD, relation));
            }
        }
        for (DccControlledFileRelatedFileDO relation : reverseRelations) {
            DccControlledFileDO sourceFile = Objects.requireNonNull(reverseSourceFiles.get(relation.getControlledFileId()),
                    "reverse relation source controlled file is missing");
            Long relatedMasterId = Objects.requireNonNull(sourceFile.getMasterId(),
                    "reverse relation source masterId must not be null");
            if (!publishedFile.getMasterId().equals(relatedMasterId)) {
                drafts.computeIfAbsent(relatedMasterId, RelationDraft::new).directions().add(
                        direction(DIRECTION_REVERSE, relation));
            }
        }
        if (drafts.isEmpty()) {
            return drafts;
        }
        Set<Long> relatedMasterIds = Set.copyOf(drafts.keySet());
        Map<Long, DccControlledFileMasterDO> masters = Objects.requireNonNull(
                masterMapper.selectBatchIds(relatedMasterIds), "related masters must not be null").stream()
                .collect(Collectors.toMap(DccControlledFileMasterDO::getId, Function.identity()));
        if (!masters.keySet().containsAll(relatedMasterIds)) {
            throw new IllegalStateException("A related controlled-file master is missing");
        }
        Set<Long> activeFileIds = masters.values().stream()
                .map(DccControlledFileMasterDO::getCurrentActiveControlledFileId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, DccControlledFileDO> activeFiles = loadFiles(activeFileIds,
                "related current active controlled file");
        drafts.forEach((masterId, draft) -> {
            DccControlledFileMasterDO master = masters.get(masterId);
            draft.master(master);
            if (master.getCurrentActiveControlledFileId() != null) {
                draft.activeFile(Objects.requireNonNull(activeFiles.get(master.getCurrentActiveControlledFileId()),
                        "related current active controlled file is missing"));
            }
        });
        return drafts;
    }

    private Map<Long, List<CandidateReasonDraft>> resolveCandidateReasons(
            DccControlledFileDO publishedFile, List<VisibilityRuleDraft> visibilityRules,
            Map<Long, RelationDraft> relationDrafts) {
        Map<Long, List<CandidateReasonDraft>> reasons = new LinkedHashMap<>();
        addCandidateReason(reasons, publishedFile.getRequesterId(),
                new CandidateReasonDraft(REASON_FILE_OWNER, publishedFile.getId(), null, "发布文件责任人"));
        for (VisibilityRuleDraft rule : visibilityRules) {
            if (!SOURCE_PUBLIC_FOLDER_DISTRIBUTION.equals(rule.sourceType())) {
                continue;
            }
            List<DccControlledFileDistributionRecipientDO> recipients = Objects.requireNonNull(
                    distributionRecipientMapper.selectListByDistributionId(rule.sourceRuleId()),
                    "formal distribution recipients must not be null");
            for (DccControlledFileDistributionRecipientDO recipient : recipients) {
                addCandidateReason(reasons, recipient.getUserId(), new CandidateReasonDraft(
                        REASON_FORMAL_DISTRIBUTION, recipient.getId(), null, "正式电子分发收件人"));
            }
        }
        relationDrafts.values().forEach(draft -> {
            DccControlledFileDO activeFile = draft.activeFile();
            if (activeFile != null && activeFile.getRequesterId() != null) {
                addCandidateReason(reasons, activeFile.getRequesterId(), new CandidateReasonDraft(
                        REASON_RELATED_FILE_OWNER, draft.relatedMasterId(), draft.relatedMasterId(),
                        "关联文件影响负责人"));
            }
        });
        return reasons;
    }

    private void applyAssignmentScope(List<VisibilityRuleDraft> rules, Long publishedControlledFileId) {
        Set<Long> candidateUserIds = rules.stream().flatMap(rule -> rule.users().stream())
                .map(VisibilityUserDraft::userId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> allowedUserIds = Objects.requireNonNull(
                assignmentScopeService.filterBusinessVisibleUserIds(candidateUserIds, publishedControlledFileId),
                "assignment-scope filtered user ids must not be null");
        for (VisibilityRuleDraft rule : rules) {
            int before = rule.users().size();
            rule.users().removeIf(user -> !allowedUserIds.contains(user.userId()));
            int excluded = before - rule.users().size();
            if (excluded == 0) {
                continue;
            }
            rule.resolutionStatus(rule.users().isEmpty()
                    ? "FILTERED_BY_ASSIGNMENT" : "RESOLVED_WITH_ASSIGNMENT_FILTER");
            rule.resolutionMessage(appendMessage(rule.resolutionMessage(),
                    "项目分配硬范围排除 " + excluded + " 人；该范围是约束条件，不是 VIEW 授权来源"));
        }
    }

    private UserDirectory resolveUserDirectory(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return new UserDirectory(Map.of(), Map.of());
        }
        Map<Long, AdminUserRespDTO> users = Objects.requireNonNull(adminUserApi.getUserList(userIds),
                        "publication snapshot users must not be null").stream()
                .filter(user -> user != null && user.getId() != null)
                .collect(Collectors.toMap(AdminUserRespDTO::getId, Function.identity(), (left, right) -> left,
                        LinkedHashMap::new));
        Set<Long> deptIds = users.values().stream().map(AdminUserRespDTO::getDeptId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, DeptRespDTO> departments = deptIds.isEmpty() ? Map.of() : Objects.requireNonNull(
                        deptApi.getDeptList(deptIds), "publication snapshot departments must not be null").stream()
                .filter(dept -> dept != null && dept.getId() != null)
                .collect(Collectors.toMap(DeptRespDTO::getId, Function.identity(), (left, right) -> left,
                        LinkedHashMap::new));
        return new UserDirectory(users, departments);
    }

    private void insertVisibilitySnapshots(DccPublicationFollowupBatchDO batch, DccControlledFileDO file,
                                           List<VisibilityRuleDraft> rules, UserDirectory directory) {
        for (VisibilityRuleDraft rule : rules) {
            DccPublicationVisibilityRuleSnapshotDO snapshot = DccPublicationVisibilityRuleSnapshotDO.builder()
                    .batchId(batch.getId()).sourceType(rule.sourceType()).sourceRuleId(rule.sourceRuleId())
                    .sourceScope(rule.sourceScope()).subjectType(rule.subjectType()).subjectId(rule.subjectId())
                    .dccProjectCodeId(file.getDccProjectCodeId()).categoryId(file.getCategoryId())
                    .directoryId(file.getDirectoryId()).sourceSummary(rule.sourceSummary())
                    .resolutionStatus(rule.resolutionStatus()).resolutionMessage(rule.resolutionMessage()).build();
            snapshot.setTenantId(batch.getTenantId());
            visibilityRuleMapper.insert(snapshot);
            if (snapshot.getId() == null) {
                throw new IllegalStateException("Publication visibility rule snapshot id is missing after insert");
            }
            for (VisibilityUserDraft user : rule.users()) {
                AdminUserRespDTO userProfile = directory.users().get(user.userId());
                DeptRespDTO dept = userProfile == null || userProfile.getDeptId() == null
                        ? null : directory.departments().get(userProfile.getDeptId());
                DccPublicationVisibilityUserSnapshotDO userSnapshot =
                        DccPublicationVisibilityUserSnapshotDO.builder()
                                .batchId(batch.getId()).ruleSnapshotId(snapshot.getId()).userId(user.userId())
                                .userNameSnapshot(displayUser(userProfile, user.suggestedName()))
                                .deptIdSnapshot(userProfile == null ? null : userProfile.getDeptId())
                                .deptNameSnapshot(dept == null ? null : dept.getName())
                                .userStatusSnapshot(userProfile == null ? null : userProfile.getStatus())
                                .resolutionReason(user.reason()).assignmentScopeResult("ALLOWED").build();
                userSnapshot.setTenantId(batch.getTenantId());
                visibilityUserMapper.insert(userSnapshot);
            }
        }
    }

    private void insertRelationSnapshots(DccPublicationFollowupBatchDO batch,
                                         Map<Long, RelationDraft> relationDrafts,
                                         UserDirectory directory, LocalDateTime frozenAt) {
        for (RelationDraft draft : relationDrafts.values()) {
            DccControlledFileMasterDO master = Objects.requireNonNull(draft.master(),
                    "related master snapshot source must not be null");
            DccControlledFileDO active = draft.activeFile();
            AdminUserRespDTO responsible = active == null || active.getRequesterId() == null
                    ? null : directory.users().get(active.getRequesterId());
            DccPublicationRelationSnapshotDO snapshot = DccPublicationRelationSnapshotDO.builder()
                    .batchId(batch.getId()).relatedMasterId(draft.relatedMasterId())
                    .relatedActiveControlledFileId(active == null ? null : active.getId())
                    .relatedFileNumberSnapshot(active == null ? master.getFileNumber() : active.getFileNumber())
                    .relatedFileNameSnapshot(active == null ? master.getFileName() : active.getFileName())
                    .relatedVersionNoSnapshot(active == null ? null : active.getVersionNo())
                    .responsibleUserIdSnapshot(active == null ? null : active.getRequesterId())
                    .responsibleUserNameSnapshot(displayUser(responsible, null))
                    .responsibleUserStatusSnapshot(responsible == null ? null : responsible.getStatus())
                    .resolutionStatus(active == null ? "NO_ACTIVE_VERSION" : "RESOLVED")
                    .frozenAt(frozenAt).build();
            snapshot.setTenantId(batch.getTenantId());
            relationSnapshotMapper.insert(snapshot);
            if (snapshot.getId() == null) {
                throw new IllegalStateException("Publication relation snapshot id is missing after insert");
            }
            for (RelationDirectionDraft direction : draft.directions()) {
                DccPublicationRelationDirectionSnapshotDO directionSnapshot =
                        DccPublicationRelationDirectionSnapshotDO.builder()
                                .batchId(batch.getId()).relationSnapshotId(snapshot.getId())
                                .direction(direction.direction()).sourceRelationId(direction.sourceRelationId())
                                .sourceControlledFileId(direction.sourceControlledFileId())
                                .targetControlledFileId(direction.targetControlledFileId())
                                .relationSourceSnapshot(direction.relationSource()).build();
                directionSnapshot.setTenantId(batch.getTenantId());
                relationDirectionMapper.insert(directionSnapshot);
            }
        }
    }

    private void insertNotificationCandidates(DccPublicationFollowupBatchDO batch,
                                              Map<Long, List<CandidateReasonDraft>> candidateReasons,
                                              UserDirectory directory) {
        for (Map.Entry<Long, List<CandidateReasonDraft>> entry : candidateReasons.entrySet()) {
            Long userId = entry.getKey();
            AdminUserRespDTO user = directory.users().get(userId);
            DeptRespDTO dept = user == null || user.getDeptId() == null
                    ? null : directory.departments().get(user.getDeptId());
            DccPublicationNotificationCandidateDO candidate = DccPublicationNotificationCandidateDO.builder()
                    .batchId(batch.getId()).userId(userId).userNameSnapshot(displayUser(user, null))
                    .deptIdSnapshot(user == null ? null : user.getDeptId())
                    .deptNameSnapshot(dept == null ? null : dept.getName())
                    .userStatusSnapshot(user == null ? null : user.getStatus())
                    .resolutionStatus(user == null ? "MISSING"
                            : Integer.valueOf(0).equals(user.getStatus()) ? "ACTIVE" : "INACTIVE").build();
            candidate.setTenantId(batch.getTenantId());
            candidateMapper.insert(candidate);
            if (candidate.getId() == null) {
                throw new IllegalStateException("Publication notification candidate id is missing after insert");
            }
            for (CandidateReasonDraft reason : entry.getValue()) {
                DccPublicationNotificationCandidateReasonDO reasonSnapshot =
                        DccPublicationNotificationCandidateReasonDO.builder()
                                .batchId(batch.getId()).candidateId(candidate.getId())
                                .reasonType(reason.reasonType()).sourceId(reason.sourceId())
                                .relatedMasterId(reason.relatedMasterId()).reasonSummary(reason.summary()).build();
                reasonSnapshot.setTenantId(batch.getTenantId());
                candidateReasonMapper.insert(reasonSnapshot);
            }
        }
    }

    private Map<Long, DccControlledFileDO> loadFiles(Collection<Long> fileIds, String description) {
        if (fileIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, DccControlledFileDO> files = Objects.requireNonNull(controlledFileMapper.selectBatchIds(fileIds),
                        description + " list must not be null").stream()
                .collect(Collectors.toMap(DccControlledFileDO::getId, Function.identity()));
        if (!files.keySet().containsAll(fileIds)) {
            throw new IllegalStateException(description + " is missing");
        }
        return files;
    }

    private RelationDirectionDraft direction(String direction, DccControlledFileRelatedFileDO relation) {
        if (StrUtil.isBlank(relation.getRelationSource())) {
            throw new IllegalStateException("Controlled-file relation source must not be blank");
        }
        return new RelationDirectionDraft(direction,
                Objects.requireNonNull(relation.getId(), "relation id must not be null"),
                Objects.requireNonNull(relation.getControlledFileId(), "relation source file id must not be null"),
                Objects.requireNonNull(relation.getRelatedControlledFileId(), "relation target file id must not be null"),
                relation.getRelationSource());
    }

    private void addCandidateReason(Map<Long, List<CandidateReasonDraft>> reasons, Long userId,
                                    CandidateReasonDraft reason) {
        if (userId == null) {
            return;
        }
        reasons.computeIfAbsent(userId, ignored -> new ArrayList<>()).add(reason);
    }

    private String describeMatrixRule(DccCategoryViewMatrixRuleDO rule) {
        String label = StrUtil.blankToDefault(rule.getSubjectLabel(), rule.getExcelColumnLetter());
        return StrUtil.blankToDefault(label, rule.getSubjectType() + "#" + rule.getSubjectId());
    }

    private String displayUser(AdminUserRespDTO user, String suggestedName) {
        if (user == null) {
            return StrUtil.trimToNull(suggestedName);
        }
        return StrUtil.blankToDefault(user.getNickname(), StrUtil.trimToNull(user.getUsername()));
    }

    private String appendMessage(String current, String addition) {
        return StrUtil.isBlank(current) ? addition : current + "; " + addition;
    }

    private void requirePublicationIdentity(DccControlledFileDO file) {
        if (file == null || file.getId() == null || file.getMasterId() == null || file.getCategoryId() == null
                || file.getRequesterId() == null || StrUtil.isBlank(file.getFileNumber())
                || StrUtil.isBlank(file.getFileName()) || StrUtil.isBlank(file.getVersionNo())
                || file.getPublishedTime() == null) {
            throw new IllegalArgumentException("Published controlled file identity is incomplete");
        }
    }

    private void requireMatchingBatchIdentity(DccPublicationFollowupBatchDO batch,
                                              DccControlledFileDO publishedFile,
                                              DccControlledFileDO previousActiveFile,
                                              LocalDateTime publishedAt) {
        Long previousActiveId = previousActiveFile == null ? null : previousActiveFile.getId();
        boolean matches = Objects.equals(batch.getTenantId(), TenantContextHolder.getRequiredTenantId())
                && Objects.equals(batch.getPublishedControlledFileId(), publishedFile.getId())
                && Objects.equals(batch.getPublishedMasterId(), publishedFile.getMasterId())
                && Objects.equals(batch.getPreviousActiveControlledFileId(), previousActiveId)
                && Objects.equals(batch.getDccProjectCodeId(), publishedFile.getDccProjectCodeId())
                && Objects.equals(batch.getCategoryId(), publishedFile.getCategoryId())
                && Objects.equals(batch.getDirectoryId(), publishedFile.getDirectoryId())
                && Objects.equals(batch.getFileTypeTaxonomyLeafId(), publishedFile.getFileTypeTaxonomyId())
                && Objects.equals(batch.getFileNumberSnapshot(), publishedFile.getFileNumber())
                && Objects.equals(batch.getFileNameSnapshot(), publishedFile.getFileName())
                && Objects.equals(batch.getVersionNoSnapshot(), publishedFile.getVersionNo())
                && Objects.equals(batch.getPublishedAt(), publishedAt);
        if (!matches) {
            throw new IllegalStateException("Publication follow-up batch identity conflict for controlled file "
                    + publishedFile.getId());
        }
    }

    private record VisibilityUserDraft(Long userId, String suggestedName, String reason) {
    }

    private static final class VisibilityRuleDraft {
        private final String sourceType;
        private final Long sourceRuleId;
        private final String sourceScope;
        private final String subjectType;
        private final Long subjectId;
        private final String sourceSummary;
        private String resolutionStatus;
        private String resolutionMessage;
        private final List<VisibilityUserDraft> users;

        private VisibilityRuleDraft(String sourceType, Long sourceRuleId, String sourceScope, String subjectType,
                                    Long subjectId, String sourceSummary, String resolutionStatus,
                                    String resolutionMessage, List<VisibilityUserDraft> users) {
            this.sourceType = sourceType;
            this.sourceRuleId = sourceRuleId;
            this.sourceScope = sourceScope;
            this.subjectType = subjectType;
            this.subjectId = subjectId;
            this.sourceSummary = sourceSummary;
            this.resolutionStatus = resolutionStatus;
            this.resolutionMessage = resolutionMessage;
            this.users = users;
        }

        private String sourceType() { return sourceType; }
        private Long sourceRuleId() { return sourceRuleId; }
        private String sourceScope() { return sourceScope; }
        private String subjectType() { return subjectType; }
        private Long subjectId() { return subjectId; }
        private String sourceSummary() { return sourceSummary; }
        private String resolutionStatus() { return resolutionStatus; }
        private void resolutionStatus(String value) { resolutionStatus = value; }
        private String resolutionMessage() { return resolutionMessage; }
        private void resolutionMessage(String value) { resolutionMessage = value; }
        private List<VisibilityUserDraft> users() { return users; }
    }

    private static final class RelationDraft {
        private final Long relatedMasterId;
        private final List<RelationDirectionDraft> directions = new ArrayList<>();
        private DccControlledFileMasterDO master;
        private DccControlledFileDO activeFile;

        private RelationDraft(Long relatedMasterId) { this.relatedMasterId = relatedMasterId; }
        private Long relatedMasterId() { return relatedMasterId; }
        private List<RelationDirectionDraft> directions() { return directions; }
        private DccControlledFileMasterDO master() { return master; }
        private void master(DccControlledFileMasterDO value) { master = value; }
        private DccControlledFileDO activeFile() { return activeFile; }
        private void activeFile(DccControlledFileDO value) { activeFile = value; }
    }

    private record RelationDirectionDraft(String direction, Long sourceRelationId,
                                          Long sourceControlledFileId, Long targetControlledFileId,
                                          String relationSource) {
    }

    private record CandidateReasonDraft(String reasonType, Long sourceId, Long relatedMasterId, String summary) {
    }

    private record UserDirectory(Map<Long, AdminUserRespDTO> users, Map<Long, DeptRespDTO> departments) {
    }
}
