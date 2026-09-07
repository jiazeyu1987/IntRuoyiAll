package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_NOTIFICATION_MANAGE_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_FOLLOWUP_FILTER_INVALID;

@Service
public class DccPublicationFollowupQueryServiceImpl implements DccPublicationFollowupQueryService {
    @Resource private DccControlledFileQueryService controlledFileQueryService;
    @Resource private DccPublicationFollowupBatchMapper batchMapper;
    @Resource private DccPublicationNotificationDeliveryMapper deliveryMapper;
    @Resource private DccPublicationNotificationCandidateMapper candidateMapper;
    @Resource private DccPublicationNotificationCandidateReasonMapper reasonMapper;
    @Resource private DccPublicationVisibilityRuleSnapshotMapper visibilityRuleMapper;
    @Resource private DccPublicationVisibilityUserSnapshotMapper visibilityUserMapper;
    @Resource private DccPublicationImpactTaskMapper impactTaskMapper;
    @Resource private DccPublicationRelationDirectionSnapshotMapper directionMapper;
    @Resource private PermissionApi permissionApi;

    @Override
    public DccPublicationFollowupRespVO getFileFollowup(Long userId, Long controlledFileId) {
        controlledFileQueryService.getControlledFile(userId, controlledFileId);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        DccPublicationFollowupBatchDO batch = batchMapper.selectLatestByPublishedControlledFileId(
                tenantId, controlledFileId);
        return batch == null ? null : hydrate(tenantId, List.of(batch)).get(0);
    }

    @Override
    public PageResult<DccPublicationFollowupRespVO> getManagementPage(
            Long userId, DccPublicationFollowupPageReqVO reqVO) {
        requireManage(userId);
        DccPublicationFollowupPageReqVO request = reqVO == null ? new DccPublicationFollowupPageReqVO() : reqVO;
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Page<DccPublicationFollowupBatchDO> page = batchMapper.selectManagementPage(
                new Page<>(request.getPageNo(), request.getPageSize()), tenantId, request.getFileNumber(),
                request.getVersionNo(), request.getBatchStatus(), request.getTaskStatus(),
                request.getNotificationStatus(), parseAssigneeUserId(request.getAssigneeUserId()));
        return new PageResult<>(hydrate(tenantId, page.getRecords()), page.getTotal());
    }

    @Override
    public PageResult<DccPublicationImpactTaskRespVO> getMyImpactTasks(
            Long userId, DccPublicationImpactTaskPageReqVO reqVO) {
        DccPublicationImpactTaskPageReqVO request = reqVO == null
                ? new DccPublicationImpactTaskPageReqVO() : reqVO;
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Page<DccPublicationImpactTaskDO> page = impactTaskMapper.selectAssigneePage(
                new Page<>(request.getPageNo(), request.getPageSize()), tenantId, userId,
                request.getTaskStatus(), request.getRevisionTrackingStatus());
        return new PageResult<>(toTasksWithDirections(tenantId, page.getRecords()), page.getTotal());
    }

    private List<DccPublicationFollowupRespVO> hydrate(Long tenantId,
                                                       List<DccPublicationFollowupBatchDO> batches) {
        if (batches.isEmpty()) return List.of();
        List<Long> batchIds = batches.stream().map(DccPublicationFollowupBatchDO::getId).toList();
        List<DccPublicationNotificationDeliveryDO> deliveries =
                deliveryMapper.selectListByBatchIds(tenantId, batchIds);
        List<Long> candidateIds = deliveries.stream().map(DccPublicationNotificationDeliveryDO::getCandidateId)
                .distinct().toList();
        Map<Long, DccPublicationNotificationCandidateDO> candidates = candidateIds.isEmpty() ? Map.of()
                : candidateMapper.selectListByIds(tenantId, candidateIds).stream().collect(Collectors.toMap(
                DccPublicationNotificationCandidateDO::getId, Function.identity()));
        Map<Long, List<String>> reasons = candidateIds.isEmpty() ? Map.of()
                : reasonMapper.selectListByCandidateIds(tenantId, candidateIds).stream().collect(
                Collectors.groupingBy(DccPublicationNotificationCandidateReasonDO::getCandidateId,
                        Collectors.mapping(DccPublicationNotificationCandidateReasonDO::getReasonSummary,
                                Collectors.toList())));

        List<DccPublicationVisibilityRuleSnapshotDO> rules =
                visibilityRuleMapper.selectListByBatchIds(tenantId, batchIds);
        List<Long> ruleIds = rules.stream().map(DccPublicationVisibilityRuleSnapshotDO::getId).toList();
        Map<Long, List<DccPublicationVisibilityUserSnapshotDO>> usersByRule = ruleIds.isEmpty() ? Map.of()
                : visibilityUserMapper.selectListByRuleIds(tenantId, ruleIds).stream().collect(
                Collectors.groupingBy(DccPublicationVisibilityUserSnapshotDO::getRuleSnapshotId));
        List<DccPublicationImpactTaskDO> tasks = impactTaskMapper.selectListByBatchIds(tenantId, batchIds);
        Map<Long, List<String>> directionMap = directions(tenantId, tasks);

        Map<Long, List<DccPublicationNotificationDeliveryDO>> deliveriesByBatch = deliveries.stream()
                .collect(Collectors.groupingBy(DccPublicationNotificationDeliveryDO::getBatchId));
        Map<Long, List<DccPublicationVisibilityRuleSnapshotDO>> rulesByBatch = rules.stream()
                .collect(Collectors.groupingBy(DccPublicationVisibilityRuleSnapshotDO::getBatchId));
        Map<Long, List<DccPublicationImpactTaskDO>> tasksByBatch = tasks.stream()
                .collect(Collectors.groupingBy(DccPublicationImpactTaskDO::getBatchId));

        return batches.stream().map(batch -> {
            DccPublicationFollowupRespVO vo = toFollowup(batch);
            vo.setNotificationDeliveries(deliveriesByBatch.getOrDefault(batch.getId(), List.of()).stream()
                    .map(row -> toDelivery(row, candidates.get(row.getCandidateId()),
                            reasons.getOrDefault(row.getCandidateId(), List.of()))).toList());
            vo.setVisibilityRules(rulesByBatch.getOrDefault(batch.getId(), List.of()).stream()
                    .map(rule -> toVisibilityRule(rule, usersByRule.getOrDefault(rule.getId(), List.of()))).toList());
            vo.setImpactTasks(tasksByBatch.getOrDefault(batch.getId(), List.of()).stream()
                    .map(task -> toTask(task, directionMap.getOrDefault(
                            task.getPublicationRelationSnapshotId(), List.of()))).toList());
            return vo;
        }).toList();
    }

    private List<DccPublicationImpactTaskRespVO> toTasksWithDirections(
            Long tenantId, List<DccPublicationImpactTaskDO> tasks) {
        Map<Long, List<String>> directionMap = directions(tenantId, tasks);
        return tasks.stream().map(task -> toTask(task, directionMap.getOrDefault(
                task.getPublicationRelationSnapshotId(), List.of()))).toList();
    }

    private Map<Long, List<String>> directions(Long tenantId, List<DccPublicationImpactTaskDO> tasks) {
        List<Long> relationIds = tasks.stream().map(DccPublicationImpactTaskDO::getPublicationRelationSnapshotId)
                .filter(Objects::nonNull).distinct().toList();
        if (relationIds.isEmpty()) return Map.of();
        return directionMapper.selectListByRelationSnapshotIds(tenantId, relationIds).stream().collect(
                Collectors.groupingBy(DccPublicationRelationDirectionSnapshotDO::getRelationSnapshotId,
                        Collectors.mapping(DccPublicationRelationDirectionSnapshotDO::getDirection,
                                Collectors.collectingAndThen(
                                        Collectors.toCollection(java.util.LinkedHashSet::new), List::copyOf))));
    }

    private void requireManage(Long userId) {
        if (!permissionApi.hasAnyRoles(userId, "doc_control")
                || !permissionApi.hasAnyPermissions(userId, "dcc:controlled-file:approve")
                || !permissionApi.hasAnyPermissions(userId,
                "dcc:controlled-file:publication-followup:manage")) {
            throw exception(PUBLICATION_NOTIFICATION_MANAGE_DENIED);
        }
    }

    private DccPublicationFollowupRespVO toFollowup(DccPublicationFollowupBatchDO batch) {
        DccPublicationFollowupRespVO vo = new DccPublicationFollowupRespVO();
        vo.setId(id(batch.getId()));
        vo.setPublishedControlledFileId(id(batch.getPublishedControlledFileId()));
        vo.setFileNumber(batch.getFileNumberSnapshot());
        vo.setFileName(batch.getFileNameSnapshot());
        vo.setVersionNo(batch.getVersionNoSnapshot());
        vo.setStatus(batch.getStatus());
        vo.setPublishedAt(batch.getPublishedAt());
        return vo;
    }

    private DccPublicationNotificationDeliveryRespVO toDelivery(
            DccPublicationNotificationDeliveryDO row, DccPublicationNotificationCandidateDO candidate,
            List<String> reasons) {
        DccPublicationNotificationDeliveryRespVO vo = new DccPublicationNotificationDeliveryRespVO();
        vo.setId(id(row.getId()));
        vo.setBatchId(id(row.getBatchId()));
        vo.setUserId(id(row.getUserId()));
        vo.setUserName(candidate == null ? null : candidate.getUserNameSnapshot());
        vo.setDeptName(candidate == null ? null : candidate.getDeptNameSnapshot());
        vo.setCandidateResolutionStatus(candidate == null ? null : candidate.getResolutionStatus());
        vo.setStatus(row.getStatus());
        vo.setAttemptCount(row.getAttemptCount());
        vo.setSentAt(row.getSentAt());
        vo.setSystemMessageId(id(row.getSystemMessageId()));
        vo.setLastErrorSummary(row.getLastErrorSummary());
        vo.setRowVersion(row.getRowVersion());
        vo.setReasonSummaries(reasons);
        return vo;
    }

    private DccPublicationVisibilityRuleRespVO toVisibilityRule(
            DccPublicationVisibilityRuleSnapshotDO rule,
            List<DccPublicationVisibilityUserSnapshotDO> users) {
        DccPublicationVisibilityRuleRespVO vo = new DccPublicationVisibilityRuleRespVO();
        vo.setId(id(rule.getId()));
        vo.setSourceType(rule.getSourceType());
        vo.setSourceRuleId(id(rule.getSourceRuleId()));
        vo.setSourceScope(rule.getSourceScope());
        vo.setSubjectType(rule.getSubjectType());
        vo.setSubjectId(id(rule.getSubjectId()));
        vo.setSourceSummary(rule.getSourceSummary());
        vo.setResolutionStatus(rule.getResolutionStatus());
        vo.setResolutionMessage(rule.getResolutionMessage());
        vo.setUsers(users.stream().map(this::toVisibilityUser).toList());
        return vo;
    }

    private DccPublicationVisibilityUserRespVO toVisibilityUser(DccPublicationVisibilityUserSnapshotDO row) {
        DccPublicationVisibilityUserRespVO vo = new DccPublicationVisibilityUserRespVO();
        vo.setUserId(id(row.getUserId()));
        vo.setUserName(row.getUserNameSnapshot());
        vo.setDeptId(id(row.getDeptIdSnapshot()));
        vo.setDeptName(row.getDeptNameSnapshot());
        vo.setUserStatus(row.getUserStatusSnapshot());
        vo.setResolutionReason(row.getResolutionReason());
        vo.setAssignmentScopeResult(row.getAssignmentScopeResult());
        return vo;
    }

    private DccPublicationImpactTaskRespVO toTask(
            DccPublicationImpactTaskDO row, List<String> relationDirections) {
        DccPublicationImpactTaskRespVO vo = new DccPublicationImpactTaskRespVO();
        vo.setId(id(row.getId()));
        vo.setBatchId(id(row.getBatchId()));
        vo.setPublishedControlledFileId(id(row.getPublishedControlledFileId()));
        vo.setRelatedMasterId(id(row.getRelatedMasterId()));
        vo.setRelatedActiveControlledFileId(id(row.getRelatedActiveControlledFileId()));
        vo.setRelatedFileNumber(row.getRelatedFileNumberSnapshot());
        vo.setRelatedFileName(row.getRelatedFileNameSnapshot());
        vo.setRelatedVersionNo(row.getRelatedVersionNoSnapshot());
        vo.setAssigneeUserId(id(row.getAssigneeUserId()));
        vo.setAssigneeUserName(row.getAssigneeUserNameSnapshot());
        vo.setTaskStatus(row.getTaskStatus());
        vo.setDecision(row.getDecision());
        vo.setDecisionReason(row.getDecisionReason());
        vo.setRevisionTrackingStatus(row.getRevisionTrackingStatus());
        vo.setLinkedRevisionControlledFileId(id(row.getLinkedRevisionControlledFileId()));
        vo.setLinkedRevisionVersion(row.getLinkedRevisionVersionSnapshot());
        vo.setRowVersion(row.getRowVersion());
        vo.setRelationDirections(relationDirections);
        return vo;
    }

    private String id(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long parseAssigneeUserId(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) throw new NumberFormatException("non-positive");
            return parsed;
        } catch (NumberFormatException ex) {
            throw exception(PUBLICATION_FOLLOWUP_FILTER_INVALID);
        }
    }
}
