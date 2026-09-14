package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionDraftReuploadRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionGovernanceImpactRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionGovernanceInspectionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionGovernanceMetricsRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionGovernanceRollbackReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionGovernanceSummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionMigrationConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionMigrationConfirmRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionMigrationDiffRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordDefinitionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionMigrationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrProcessFormPermissionRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrUnifiedChangeRequestMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordDefinitionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMigrationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordImportResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.DccProjectResolver;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_UNIFIED_CHANGE_IMPACT_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_UNIFIED_CHANGE_SIGNOFF_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_UNIFIED_CHANGE_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_VERSION_CONFIRM_BLOCKER;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_VERSION_CONFIRM_SCOPE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_VERSION_DRAFT_REUPLOAD_INVALID;

@Service
public class MesProBatchRecordVersionGovernanceServiceImpl implements MesProBatchRecordVersionGovernanceService {

    private static final String OBJECT_TYPE_BATCH_RECORD_VERSION = "BATCH_RECORD_VERSION";
    private static final String CHANGE_TYPE_ROLLBACK = "ROLLBACK";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PRECHECK_FAILED = "PRECHECK_FAILED";
    private static final String STATUS_PRECHECK_PASSED = "PRECHECK_PASSED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_VOIDED = "VOIDED";
    private static final String RISK_BLOCKER = "BLOCKER";
    private static final String RISK_CONFIRM_REQUIRED = "CONFIRM_REQUIRED";
    private static final String INSPECTION_PASS = "PASS";
    private static final String INSPECTION_BLOCKED = "BLOCKED";

    @Resource
    private MesProBatchRecordDefinitionMapper definitionMapper;
    @Resource
    private MesProBatchRecordVersionMapper versionMapper;
    @Resource
    private MesProBatchRecordVersionMigrationItemMapper migrationItemMapper;
    @Resource
    private MesProBatchRecordExecutionMapper executionMapper;
    @Resource
    private MesProEdhrBatchExecutionTaskMapper taskMapper;
    @Resource
    private MesProRouteFlowProcessBatchRecordMapper routeBindingMapper;
    @Resource
    private MesProEdhrProcessFormPermissionRuleMapper permissionRuleMapper;
    @Resource
    private MesProEdhrUnifiedChangeRequestMapper unifiedChangeRequestMapper;
    @Resource
    private MesProEdhrUnifiedChangeService unifiedChangeService;
    @Resource
    private MesProBatchRecordReportService batchRecordReportService;
    @Resource
    private DccProjectResolver dccProjectResolver;
    @Resource
    private MesProRouteVersionMapper routeVersionMapper;

    @Override
    public MesProBatchRecordVersionGovernanceSummaryRespVO getSummary(Long definitionId) {
        MesProBatchRecordDefinitionDO definition = requireDefinition(definitionId);
        MesProBatchRecordVersionDO currentVersion = definition.getCurrentVersionId() == null
                ? null : versionMapper.selectById(definition.getCurrentVersionId());
        Long currentVersionId = definition.getCurrentVersionId();
        Long blockingInspectionCount = currentVersionId == null ? 0L : buildInspectionIssueCount(currentVersionId);
        return new MesProBatchRecordVersionGovernanceSummaryRespVO()
                .setDefinitionId(definition.getId())
                .setCurrentVersionId(currentVersionId)
                .setCurrentVersionNo(currentVersion == null ? null : currentVersion.getVersionNo())
                .setVersionCount(versionMapper.countByDefinitionId(definition.getId()))
                .setActiveExecutionCount(currentVersionId == null ? 0L : executionMapper.countByBatchRecordVersionId(currentVersionId))
                .setHistoricalExecutionCount(currentVersionId == null ? 0L
                        : executionMapper.countByBatchRecordDefinitionIdAndNotVersionId(definition.getId(), currentVersionId))
                .setSlotBindingCount(currentVersionId == null ? 0L : routeBindingMapper.countByBatchRecordVersionId(currentVersionId))
                .setRollbackPendingCount(unifiedChangeRequestMapper.countByControlledObjectChangeTypeAndStatus(
                        OBJECT_TYPE_BATCH_RECORD_VERSION, String.valueOf(definition.getId()), CHANGE_TYPE_ROLLBACK,
                        MesProEdhrUnifiedChangeServiceImpl.STATUS_SUBMITTED))
                .setBlockingInspectionCount(blockingInspectionCount);
    }

    @Override
    public MesProBatchRecordVersionGovernanceImpactRespVO getImpact(Long versionId) {
        requireVersion(versionId);
        List<MesProRouteFlowProcessBatchRecordDO> routeBindings = routeBindingMapper.selectListByBatchRecordVersionId(versionId);
        List<MesProEdhrBatchExecutionTaskDO> tasks = taskMapper.selectListByBatchRecordVersionId(versionId);
        List<MesProBatchRecordExecutionDO> executions = executionMapper.selectListByBatchRecordVersionId(versionId);
        Long blockerCount = migrationItemMapper.countByVersionIdAndRiskLevel(versionId, "BLOCKER");
        Long confirmRequiredCount = migrationItemMapper.countByVersionIdAndRiskLevel(versionId, "CONFIRM_REQUIRED");
        return new MesProBatchRecordVersionGovernanceImpactRespVO()
                .setVersionId(versionId)
                .setExecutionCount((long) executions.size())
                .setTaskCount((long) tasks.size())
                .setRouteBindingCount((long) routeBindings.size())
                .setPermissionRuleCount(permissionRuleMapper.countByBatchRecordVersionId(versionId))
                .setSlotConfigSnapshotHashes(uniqueNonBlankHashes(routeBindings, tasks, executions))
                .setOwnerRoleKeys(uniqueOwnerRoleKeys(routeBindings, tasks))
                .setRiskLevel(resolveRiskLevel(blockerCount, confirmRequiredCount));
    }

    @Override
    public MesProBatchRecordVersionGovernanceInspectionRespVO getInspection(Long versionId) {
        requireVersion(versionId);
        Long issueCount = buildInspectionIssueCount(versionId);
        String inspectionStatus = issueCount > 0 ? INSPECTION_BLOCKED : INSPECTION_PASS;
        return new MesProBatchRecordVersionGovernanceInspectionRespVO()
                .setVersionId(versionId)
                .setInspectionCode("EDHR_VERSION_GOVERNANCE")
                .setInspectionStatus(inspectionStatus)
                .setIssueCount(issueCount)
                .setIssueSummary(issueCount > 0 ? "存在版本迁移阻断项或需人工确认项" : "版本治理巡检未发现阻断项")
                .setNextAction(issueCount > 0 ? "处理迁移阻断项后再升版或回滚" : "可进入审批或生产使用");
    }

    @Override
    public MesProBatchRecordVersionGovernanceMetricsRespVO getMetrics(Long versionId) {
        MesProBatchRecordVersionDO version = requireVersion(versionId);
        Long issueCount = buildInspectionIssueCount(versionId);
        return new MesProBatchRecordVersionGovernanceMetricsRespVO()
                .setVersionId(versionId)
                .setPendingApprovalCount(versionMapper.countByDefinitionIdAndStatus(version.getDefinitionId(), STATUS_PENDING_APPROVAL))
                .setApprovedVersionCount(versionMapper.countByDefinitionIdAndStatus(version.getDefinitionId(), STATUS_APPROVED))
                .setRollbackRequestCount(unifiedChangeRequestMapper.countByControlledObjectAndChangeType(
                        OBJECT_TYPE_BATCH_RECORD_VERSION, String.valueOf(version.getDefinitionId()), CHANGE_TYPE_ROLLBACK))
                .setConfirmRequiredItemCount(migrationItemMapper.countByVersionIdAndRiskLevel(versionId, "CONFIRM_REQUIRED"))
                .setBlockerItemCount(migrationItemMapper.countByVersionIdAndRiskLevel(versionId, "BLOCKER"))
                .setLatestInspectionStatus(issueCount > 0 ? INSPECTION_BLOCKED : INSPECTION_PASS);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrUnifiedChangeRespVO requestRollback(MesProBatchRecordVersionGovernanceRollbackReqVO reqVO) {
        MesProBatchRecordDefinitionDO definition = requireDefinition(reqVO.getDefinitionId());
        MesProBatchRecordVersionDO targetVersion = requireVersion(reqVO.getTargetVersionId());
        if (!Objects.equals(definition.getId(), targetVersion.getDefinitionId())
                || Objects.equals(definition.getCurrentVersionId(), targetVersion.getId())) {
            throw exception(PRO_EDHR_UNIFIED_CHANGE_STATUS_INVALID);
        }
        MesProBatchRecordVersionDO currentVersion = requireVersion(definition.getCurrentVersionId());
        if (!STATUS_APPROVED.equals(targetVersion.getStatus())) {
            throw exception(PRO_EDHR_UNIFIED_CHANGE_STATUS_INVALID);
        }
        String impactSummaryJson = requireText(reqVO.getImpactSummaryJson(), PRO_EDHR_UNIFIED_CHANGE_IMPACT_REQUIRED);
        String signoffEvidenceHash = requireText(reqVO.getSignoffEvidenceHash(), PRO_EDHR_UNIFIED_CHANGE_SIGNOFF_REQUIRED);

        MesProEdhrUnifiedChangeCreateReqVO createReqVO = new MesProEdhrUnifiedChangeCreateReqVO()
                .setControlledObjectType(OBJECT_TYPE_BATCH_RECORD_VERSION)
                .setControlledObjectId(String.valueOf(definition.getId()))
                .setControlledObjectCode(definition.getBatchRecordName())
                .setCurrentVersion(currentVersion.getVersionNo())
                .setTargetVersion(targetVersion.getVersionNo())
                .setChangeType(CHANGE_TYPE_ROLLBACK)
                .setRiskLevel(getImpact(targetVersion.getId()).getRiskLevel())
                .setReasonCategory("VERSION_ROLLBACK")
                .setReason(reqVO.getReason())
                .setDiffSnapshotJson(buildRollbackDiffSnapshot(definition, currentVersion, targetVersion, signoffEvidenceHash))
                .setImpactSummaryJson(impactSummaryJson)
                .setIdempotencyKey(reqVO.getIdempotencyKey());
        return unifiedChangeService.create(createReqVO);
    }

    @Override
    public MesProBatchRecordVersionMigrationDiffRespVO getMigrationDiff(Long versionId) {
        requireVersion(versionId);
        List<MesProBatchRecordVersionMigrationItemDO> items = migrationItemMapper.selectListByVersionId(versionId);
        Long blockerCount = migrationItemMapper.countByVersionIdAndRiskLevel(versionId, RISK_BLOCKER);
        Long confirmRequiredCount = migrationItemMapper.countByVersionIdAndRiskLevel(versionId, RISK_CONFIRM_REQUIRED);
        Long confirmedCount = migrationItemMapper.countConfirmedByVersionId(versionId);
        Long blockingCount = migrationItemMapper.countBlockingItems(versionId);
        return new MesProBatchRecordVersionMigrationDiffRespVO()
                .setVersionId(versionId)
                .setItems(items.stream().map(this::buildMigrationDiffItem).toList())
                .setBlockerCount(blockerCount)
                .setConfirmRequiredCount(confirmRequiredCount)
                .setConfirmedCount(confirmedCount)
                .setApprovalReady(blockingCount == 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordVersionMigrationConfirmRespVO confirmMigrationItems(
            Long versionId, MesProBatchRecordVersionMigrationConfirmReqVO reqVO) {
        requireVersion(versionId);
        MesProBatchRecordVersionMigrationItemDO idempotentItem =
                migrationItemMapper.selectByVersionIdAndConfirmIdempotencyKey(versionId, reqVO.getIdempotencyKey());
        if (idempotentItem != null) {
            return buildConfirmResp(versionId, reqVO.getItemIds(), idempotentItem.getConfirmedBy(),
                    idempotentItem.getConfirmedAt(), idempotentItem.getConfirmComment(), reqVO.getIdempotencyKey());
        }

        Long confirmedBy = getLoginUserId();
        LocalDateTime confirmedAt = LocalDateTime.now();
        for (Long itemId : reqVO.getItemIds()) {
            MesProBatchRecordVersionMigrationItemDO item = migrationItemMapper.selectById(itemId);
            if (item == null || !Objects.equals(versionId, item.getVersionId())) {
                throw exception(PRO_BATCH_RECORD_REPORT_VERSION_CONFIRM_SCOPE_INVALID, itemId);
            }
            if (RISK_BLOCKER.equals(item.getRiskLevel())) {
                throw exception(PRO_BATCH_RECORD_REPORT_VERSION_CONFIRM_BLOCKER, itemId);
            }
            if (!RISK_CONFIRM_REQUIRED.equals(item.getRiskLevel())) {
                throw exception(PRO_BATCH_RECORD_REPORT_VERSION_CONFIRM_SCOPE_INVALID, itemId);
            }
            migrationItemMapper.updateById(new MesProBatchRecordVersionMigrationItemDO()
                    .setId(itemId)
                    .setConfirmed(true)
                    .setConfirmedBy(confirmedBy)
                    .setConfirmedAt(confirmedAt)
                    .setConfirmComment(reqVO.getComment())
                    .setConfirmIdempotencyKey(reqVO.getIdempotencyKey()));
        }
        promotePrecheckFailedVersionWhenMigrationReady(versionId);
        return buildConfirmResp(versionId, reqVO.getItemIds(), confirmedBy, confirmedAt,
                reqVO.getComment(), reqVO.getIdempotencyKey());
    }

    private void promotePrecheckFailedVersionWhenMigrationReady(Long versionId) {
        if (migrationItemMapper.countBlockingItems(versionId) > 0) {
            return;
        }
        MesProBatchRecordVersionDO version = versionMapper.selectById(versionId);
        if (version == null || !STATUS_PRECHECK_FAILED.equals(version.getStatus())) {
            return;
        }
        versionMapper.updateById(new MesProBatchRecordVersionDO()
                .setId(versionId)
                .setStatus(STATUS_PRECHECK_PASSED));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordVersionDraftReuploadRespVO reuploadDraft(Long versionId, MultipartFile file,
                                                                      List<String> productNames, String remark) {
        MesProBatchRecordVersionDO oldVersion = requireVersion(versionId);
        if (!List.of(STATUS_DRAFT, STATUS_PRECHECK_FAILED, STATUS_REJECTED).contains(oldVersion.getStatus())) {
            throw exception(PRO_BATCH_RECORD_REPORT_VERSION_DRAFT_REUPLOAD_INVALID, versionId);
        }
        MesProBatchRecordDefinitionDO definition = requireDefinition(oldVersion.getDefinitionId());
        versionMapper.updateById(new MesProBatchRecordVersionDO()
                .setId(oldVersion.getId())
                .setStatus(STATUS_VOIDED)
                .setRemark(StrUtil.blankToDefault(remark, "草稿重新上传后作废")));

        Long routeId = oldVersion.getRouteId();
        DccProjectResolver.ResolvedProject selectedProject = dccProjectResolver.requireEnabledByRoute(routeId);
        MesProRouteVersionDO activeRouteVersion = routeVersionMapper.selectActiveByRouteId(routeId);
        MesProRouteVersionDO routeCandidateVersion = routeVersionMapper.selectOpenCandidateByRouteId(routeId);
        MesProBatchRecordImportResult importResult = batchRecordReportService.recognizeUploadedRoute(
                file, definition.getRouteKey(), definition.getBatchRecordName(), "UPGRADE",
                oldVersion.getSourceVersionId(), null, productNames, true, List.of(), productNames,
                true, routeId, activeRouteVersion == null ? null : activeRouteVersion.getId(),
                routeCandidateVersion == null ? null : routeCandidateVersion.getId(),
                selectedProject.dccProjectCodeId(), getLoginUserId());
        Long newVersionId = importResult.batchRecordVersionId();
        if (newVersionId == null || Objects.equals(newVersionId, oldVersion.getId())) {
            throw exception(PRO_BATCH_RECORD_REPORT_VERSION_DRAFT_REUPLOAD_INVALID, versionId);
        }
        if (StrUtil.isNotBlank(remark)) {
            versionMapper.updateById(new MesProBatchRecordVersionDO()
                    .setId(newVersionId)
                    .setRemark(remark));
        }
        return new MesProBatchRecordVersionDraftReuploadRespVO()
                .setVoidedVersionId(oldVersion.getId())
                .setNewVersionId(newVersionId)
                .setVersionNo(importResult.versionNo())
                .setStatus(importResult.versionStatus());
    }

    private MesProBatchRecordDefinitionDO requireDefinition(Long definitionId) {
        MesProBatchRecordDefinitionDO definition = definitionId == null ? null : definitionMapper.selectById(definitionId);
        if (definition == null) {
            throw exception(PRO_EDHR_UNIFIED_CHANGE_STATUS_INVALID);
        }
        return definition;
    }

    private MesProBatchRecordVersionDO requireVersion(Long versionId) {
        MesProBatchRecordVersionDO version = versionId == null ? null : versionMapper.selectById(versionId);
        if (version == null) {
            throw exception(PRO_EDHR_UNIFIED_CHANGE_STATUS_INVALID);
        }
        return version;
    }

    private Long buildInspectionIssueCount(Long versionId) {
        return migrationItemMapper.countBlockingItems(versionId);
    }

    private MesProBatchRecordVersionMigrationDiffRespVO.Item buildMigrationDiffItem(
            MesProBatchRecordVersionMigrationItemDO item) {
        return new MesProBatchRecordVersionMigrationDiffRespVO.Item()
                .setItemId(item.getId())
                .setDiffGroup(StrUtil.blankToDefault(item.getDiffGroup(), item.getItemType()))
                .setDiffType(StrUtil.blankToDefault(item.getDiffType(), "UNCHANGED"))
                .setRiskLevel(item.getRiskLevel())
                .setSourceLogicalKey(item.getSourceLogicalKey())
                .setTargetLogicalKey(item.getTargetLogicalKey())
                .setMatchConfidence(item.getMatchConfidence())
                .setMatchEvidenceJson(item.getMatchEvidenceJson())
                .setRuleType(item.getRuleType())
                .setBusinessOwnerType(item.getBusinessOwnerType())
                .setConfirmed(Boolean.TRUE.equals(item.getConfirmed()))
                .setConfirmedBy(item.getConfirmedBy())
                .setConfirmedAt(item.getConfirmedAt())
                .setConfirmComment(item.getConfirmComment())
                .setMessage(item.getMessage());
    }

    private MesProBatchRecordVersionMigrationConfirmRespVO buildConfirmResp(Long versionId, List<Long> itemIds,
                                                                            Long confirmedBy, LocalDateTime confirmedAt,
                                                                            String comment, String idempotencyKey) {
        return new MesProBatchRecordVersionMigrationConfirmRespVO()
                .setVersionId(versionId)
                .setConfirmedItemIds(itemIds)
                .setConfirmedBy(confirmedBy)
                .setConfirmedAt(confirmedAt)
                .setConfirmComment(comment)
                .setIdempotencyKey(idempotencyKey);
    }

    private List<String> uniqueNonBlankHashes(List<MesProRouteFlowProcessBatchRecordDO> routeBindings,
                                              List<MesProEdhrBatchExecutionTaskDO> tasks,
                                              List<MesProBatchRecordExecutionDO> executions) {
        LinkedHashSet<String> hashes = new LinkedHashSet<>();
        routeBindings.stream()
                .map(MesProRouteFlowProcessBatchRecordDO::getSlotConfigSnapshotHash)
                .filter(StrUtil::isNotBlank)
                .forEach(hashes::add);
        tasks.stream()
                .map(MesProEdhrBatchExecutionTaskDO::getSlotConfigSnapshotHash)
                .filter(StrUtil::isNotBlank)
                .forEach(hashes::add);
        executions.stream()
                .map(MesProBatchRecordExecutionDO::getSlotConfigSnapshotHash)
                .filter(StrUtil::isNotBlank)
                .forEach(hashes::add);
        return hashes.stream().sorted(Comparator.naturalOrder()).toList();
    }

    private List<String> uniqueOwnerRoleKeys(List<MesProRouteFlowProcessBatchRecordDO> routeBindings,
                                             List<MesProEdhrBatchExecutionTaskDO> tasks) {
        LinkedHashSet<String> ownerRoleKeys = new LinkedHashSet<>();
        routeBindings.stream()
                .map(MesProRouteFlowProcessBatchRecordDO::getOwnerRoleKey)
                .filter(StrUtil::isNotBlank)
                .forEach(ownerRoleKeys::add);
        tasks.stream()
                .map(MesProEdhrBatchExecutionTaskDO::getOwnerRoleKey)
                .filter(StrUtil::isNotBlank)
                .forEach(ownerRoleKeys::add);
        return ownerRoleKeys.stream().sorted(Comparator.naturalOrder()).toList();
    }

    private String resolveRiskLevel(Long blockerCount, Long confirmRequiredCount) {
        if (blockerCount != null && blockerCount > 0) {
            return "CRITICAL";
        }
        if (confirmRequiredCount != null && confirmRequiredCount > 0) {
            return "HIGH";
        }
        return "LOW";
    }

    private String requireText(String value, cn.iocoder.yudao.framework.common.exception.ErrorCode errorCode) {
        String text = StrUtil.trim(value);
        if (StrUtil.isBlank(text)) {
            throw exception(errorCode);
        }
        return text;
    }

    private String buildRollbackDiffSnapshot(MesProBatchRecordDefinitionDO definition,
                                             MesProBatchRecordVersionDO currentVersion,
                                             MesProBatchRecordVersionDO targetVersion,
                                             String signoffEvidenceHash) {
        return "{\"changeType\":\"ROLLBACK\",\"definitionId\":" + definition.getId()
                + ",\"fromVersionId\":" + currentVersion.getId()
                + ",\"fromVersionNo\":\"" + currentVersion.getVersionNo()
                + "\",\"targetVersionId\":" + targetVersion.getId()
                + ",\"targetVersionNo\":\"" + targetVersion.getVersionNo()
                + "\",\"signoffEvidenceHash\":\"" + signoffEvidenceHash + "\"}";
    }
}
