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
import java.util.ArrayList;
import java.util.Comparator;
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
    @Resource private DccPublicationNotificationAuditMapper notificationAuditMapper;
    @Resource private DccPublicationImpactAuditMapper impactAuditMapper;
    @Resource private DccControlledFileMapper controlledFileMapper;
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
        List<DccPublicationNotificationAuditDO> notificationAudits = Objects.requireNonNull(
                notificationAuditMapper.selectListByBatchIds(tenantId, batchIds),
                "publication notification audits must not be null");
        List<DccPublicationImpactAuditDO> impactAudits = Objects.requireNonNull(
                impactAuditMapper.selectListByBatchIds(tenantId, batchIds),
                "publication impact audits must not be null");

        Map<Long, List<DccPublicationNotificationDeliveryDO>> deliveriesByBatch = deliveries.stream()
                .collect(Collectors.groupingBy(DccPublicationNotificationDeliveryDO::getBatchId));
        Map<Long, List<DccPublicationVisibilityRuleSnapshotDO>> rulesByBatch = rules.stream()
                .collect(Collectors.groupingBy(DccPublicationVisibilityRuleSnapshotDO::getBatchId));
        Map<Long, List<DccPublicationImpactTaskDO>> tasksByBatch = tasks.stream()
                .collect(Collectors.groupingBy(DccPublicationImpactTaskDO::getBatchId));
        Map<Long, DccPublicationNotificationDeliveryDO> deliveriesById = deliveries.stream()
                .collect(Collectors.toMap(DccPublicationNotificationDeliveryDO::getId, Function.identity()));
        Map<Long, DccPublicationImpactTaskDO> tasksById = tasks.stream()
                .collect(Collectors.toMap(DccPublicationImpactTaskDO::getId, Function.identity()));
        Map<Long, List<DccPublicationNotificationAuditDO>> notificationAuditsByBatch = notificationAudits.stream()
                .collect(Collectors.groupingBy(DccPublicationNotificationAuditDO::getBatchId));
        Map<Long, List<DccPublicationImpactAuditDO>> impactAuditsByBatch = impactAudits.stream()
                .collect(Collectors.groupingBy(DccPublicationImpactAuditDO::getBatchId));

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
            vo.setTimeline(buildTimeline(batch,
                    notificationAuditsByBatch.getOrDefault(batch.getId(), List.of()),
                    impactAuditsByBatch.getOrDefault(batch.getId(), List.of()), deliveriesById,
                    candidates, tasksById, directionMap));
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

    private List<DccPublicationTimelineEventRespVO> buildTimeline(
            DccPublicationFollowupBatchDO batch,
            List<DccPublicationNotificationAuditDO> notificationAudits,
            List<DccPublicationImpactAuditDO> impactAudits,
            Map<Long, DccPublicationNotificationDeliveryDO> deliveriesById,
            Map<Long, DccPublicationNotificationCandidateDO> candidates,
            Map<Long, DccPublicationImpactTaskDO> tasksById,
            Map<Long, List<String>> directions) {
        List<TimelineDraft> drafts = new ArrayList<>();
        Map<Long, String> linkedRevisionVersionById = linkedRevisionVersions(impactAudits);
        DccPublicationTimelineEventRespVO created = baseTimeline(
                "BATCH:" + batch.getId(), "BATCH", "发布批次", "发布后续批次已创建",
                Objects.requireNonNull(batch.getPublishedAt(), "publication batch time must not be null"),
                batch.getCreator(), id(batch.getId()), batch.getFileNumberSnapshot() + " / " + batch.getVersionNoSnapshot());
        created.setStatusAfterLabel(batchStatusLabel("PENDING"));
        created.setDirectionLabels(List.of());
        drafts.add(new TimelineDraft(created, 0, Objects.requireNonNull(batch.getId())));

        for (DccPublicationNotificationAuditDO audit : notificationAudits) {
            DccPublicationNotificationDeliveryDO delivery = deliveriesById.get(audit.getDeliveryId());
            if (delivery == null) throw new IllegalStateException("notification audit delivery is missing");
            DccPublicationNotificationCandidateDO candidate = candidates.get(delivery.getCandidateId());
            DccPublicationTimelineEventRespVO event = baseTimeline(
                    "NOTIFICATION:" + audit.getId(), "NOTIFICATION", "站内通知",
                    notificationActionLabel(audit.getActionType()), requireOccurredAt(audit.getOccurredAt()),
                    id(audit.getActorId()), id(delivery.getId()), candidate == null
                            ? "收件用户 #" + delivery.getUserId()
                            : Objects.requireNonNullElse(candidate.getUserNameSnapshot(),
                            "收件用户 #" + delivery.getUserId()));
            event.setStatusBeforeLabel(notificationStatusLabel(audit.getStatusBefore()));
            event.setStatusAfterLabel(notificationStatusLabel(audit.getStatusAfter()));
            event.setDirectionLabels(List.of());
            event.setReason(audit.getReason());
            event.setErrorSummary(audit.getErrorSummary());
            event.setSystemMessageId(id(audit.getSystemMessageId()));
            if (isNotificationAttemptAction(audit.getActionType())) {
                event.setAttemptCount(Objects.requireNonNull(audit.getAttemptCount(),
                        "notification attempt audit count must not be null"));
            }
            drafts.add(new TimelineDraft(event, 1, Objects.requireNonNull(audit.getId())));
        }
        for (DccPublicationImpactAuditDO audit : impactAudits) {
            DccPublicationImpactTaskDO task = tasksById.get(audit.getTaskId());
            if (task == null) throw new IllegalStateException("impact audit task is missing");
            DccPublicationTimelineEventRespVO event = baseTimeline(
                    "IMPACT:" + audit.getId(), "IMPACT", "影响评估",
                    impactActionLabel(audit.getActionType()), requireOccurredAt(audit.getOccurredAt()),
                    id(audit.getActorId()), id(task.getId()), impactObjectLabel(task));
            event.setStatusBeforeLabel(impactStatusLabel(audit.getStatusBefore()));
            event.setStatusAfterLabel(impactStatusLabel(audit.getStatusAfter()));
            event.setDecisionLabel(decisionLabel(audit.getDecisionSnapshot()));
            event.setDirectionLabels(directions.getOrDefault(task.getPublicationRelationSnapshotId(), List.of())
                    .stream().map(this::directionLabel).toList());
            event.setReason(audit.getReason());
            if ("REASSIGN".equals(audit.getActionType())) {
                event.setAssigneeBefore(id(audit.getAssigneeBefore()));
                event.setAssigneeAfter(id(Objects.requireNonNull(audit.getAssigneeAfter(),
                        "reassign audit assigneeAfter must not be null")));
            }
            if (isLinkedRevisionAction(audit.getActionType())) {
                Long linkedRevisionId = Objects.requireNonNull(audit.getLinkedRevisionControlledFileId(),
                        "linked revision audit controlled file id must not be null");
                event.setLinkedRevisionControlledFileId(id(linkedRevisionId));
                event.setLinkedRevisionVersion(resolveLinkedRevisionVersion(
                        linkedRevisionId, task, linkedRevisionVersionById));
            }
            drafts.add(new TimelineDraft(event, 2, Objects.requireNonNull(audit.getId())));
        }
        drafts.sort(Comparator.comparing((TimelineDraft draft) -> draft.event().getOccurredAt())
                .thenComparingInt(TimelineDraft::sourceOrder).thenComparingLong(TimelineDraft::recordId));
        for (int index = 0; index < drafts.size(); index++) {
            drafts.get(index).event().setSequenceNo(index + 1);
        }
        return drafts.stream().map(TimelineDraft::event).toList();
    }

    private Map<Long, String> linkedRevisionVersions(List<DccPublicationImpactAuditDO> impactAudits) {
        List<Long> linkedRevisionIds = impactAudits.stream()
                .filter(audit -> audit != null && isLinkedRevisionAction(audit.getActionType()))
                .map(DccPublicationImpactAuditDO::getLinkedRevisionControlledFileId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (linkedRevisionIds.isEmpty()) {
            return Map.of();
        }
        return Objects.requireNonNull(controlledFileMapper.selectBatchIds(linkedRevisionIds),
                        "linked revision controlled files must not be null")
                .stream()
                .filter(Objects::nonNull)
                .filter(file -> file.getId() != null)
                .collect(Collectors.toMap(DccControlledFileDO::getId, DccControlledFileDO::getVersionNo,
                        (left, right) -> left));
    }

    private String resolveLinkedRevisionVersion(Long linkedRevisionId, DccPublicationImpactTaskDO task,
                                                Map<Long, String> linkedRevisionVersionById) {
        if (Objects.equals(linkedRevisionId, task.getLinkedRevisionControlledFileId())
                && task.getLinkedRevisionVersionSnapshot() != null
                && !task.getLinkedRevisionVersionSnapshot().isBlank()) {
            return task.getLinkedRevisionVersionSnapshot();
        }
        String versionNo = linkedRevisionVersionById.get(linkedRevisionId);
        if (versionNo == null || versionNo.isBlank()) {
            throw new IllegalStateException("linked revision version is missing for " + linkedRevisionId);
        }
        return versionNo;
    }

    private DccPublicationTimelineEventRespVO baseTimeline(
            String eventId, String sourceType, String sourceLabel, String actionLabel,
            java.time.LocalDateTime occurredAt, String actorId, String objectId, String objectLabel) {
        DccPublicationTimelineEventRespVO event = new DccPublicationTimelineEventRespVO();
        event.setEventId(eventId);
        event.setSourceType(sourceType);
        event.setSourceLabel(sourceLabel);
        event.setActionLabel(actionLabel);
        event.setOccurredAt(occurredAt);
        event.setActorId(actorId);
        event.setObjectId(objectId);
        event.setObjectLabel(objectLabel);
        return event;
    }

    private java.time.LocalDateTime requireOccurredAt(java.time.LocalDateTime occurredAt) {
        return Objects.requireNonNull(occurredAt, "publication audit occurredAt must not be null");
    }

    private String impactObjectLabel(DccPublicationImpactTaskDO task) {
        String name = Objects.requireNonNullElse(task.getRelatedFileNameSnapshot(), "关联文件");
        return task.getRelatedFileNumberSnapshot() == null
                ? name : name + " / " + task.getRelatedFileNumberSnapshot();
    }

    private String notificationActionLabel(String code) {
        return switch (Objects.requireNonNullElse(code, "")) {
            case "MATERIALIZE" -> "生成通知记录";
            case "ATTEMPT" -> "尝试发送通知";
            case "RETRY" -> "重试发送通知";
            case "SENT" -> "通知发送成功";
            case "FAILED" -> "通知发送失败";
            default -> unknownAction(code);
        };
    }

    private boolean isNotificationAttemptAction(String code) {
        return "ATTEMPT".equals(code) || "RETRY".equals(code)
                || "SENT".equals(code) || "FAILED".equals(code);
    }

    private boolean isLinkedRevisionAction(String code) {
        return "LINK_REVISION".equals(code) || "RESOLVE_REVISION".equals(code);
    }

    private String impactActionLabel(String code) {
        return switch (Objects.requireNonNullElse(code, "")) {
            case "MATERIALIZE" -> "生成影响评估任务";
            case "START" -> "开始影响评估";
            case "DECIDE" -> "提交评估结论";
            case "REASSIGN" -> "转派影响评估";
            case "REOPEN" -> "重新打开影响评估";
            case "LINK_REVISION" -> "关联大版本";
            case "RESOLVE_REVISION" -> "关联大版本已发布";
            default -> unknownAction(code);
        };
    }

    private String notificationStatusLabel(String code) {
        return switch (Objects.requireNonNullElse(code, "")) {
            case "PENDING" -> "待发送";
            case "SENT" -> "已发送";
            case "FAILED" -> "发送失败";
            case "" -> null;
            default -> unknownStatus(code);
        };
    }

    private String impactStatusLabel(String code) {
        return switch (Objects.requireNonNullElse(code, "")) {
            case "PENDING" -> "待处理";
            case "UNASSIGNED" -> "待文控分配";
            case "IN_REVIEW" -> "评估中";
            case "COMPLETED" -> "已完成";
            case "" -> null;
            default -> unknownStatus(code);
        };
    }

    private String batchStatusLabel(String code) {
        return switch (Objects.requireNonNullElse(code, "")) {
            case "PENDING" -> "待处理";
            case "" -> null;
            default -> unknownStatus(code);
        };
    }

    private String decisionLabel(String code) {
        return switch (Objects.requireNonNullElse(code, "")) {
            case "NO_REVISION_REQUIRED" -> "无需升版";
            case "REVISION_REQUIRED" -> "需要升版";
            case "" -> null;
            default -> "未知结论（" + code + "）";
        };
    }

    private String directionLabel(String code) {
        return switch (Objects.requireNonNullElse(code, "")) {
            case "FORWARD" -> "正向关联";
            case "REVERSE" -> "反向引用";
            default -> "未知方向（" + code + "）";
        };
    }

    private String unknownAction(String code) {
        return "未知动作（" + Objects.requireNonNullElse(code, "") + "）";
    }

    private String unknownStatus(String code) {
        return "未知状态（" + Objects.requireNonNullElse(code, "") + "）";
    }

    private record TimelineDraft(DccPublicationTimelineEventRespVO event, int sourceOrder, long recordId) {
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
