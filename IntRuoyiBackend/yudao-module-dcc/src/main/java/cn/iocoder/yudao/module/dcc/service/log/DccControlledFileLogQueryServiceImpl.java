package cn.iocoder.yudao.module.dcc.service.log;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.log.vo.DccControlledFileLogPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.log.vo.DccControlledFileLogRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAccessLogDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMetadataChangeDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMetadataChangeItemDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingProgressDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeAssignmentFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileAccessEventDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAccessLogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMetadataChangeItemMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMetadataChangeMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingProgressMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileAccessEventMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

@Service
public class DccControlledFileLogQueryServiceImpl implements DccControlledFileLogQueryService {

    private static final String TYPE_CONTROLLED_FILE_AUDIT = "CONTROLLED_FILE_AUDIT";
    private static final String TYPE_FILE_SUBMISSION = "FILE_SUBMISSION";
    private static final String TYPE_FILE_APPROVAL = "FILE_APPROVAL";
    private static final String TYPE_FILE_RELEASE = "FILE_RELEASE";
    private static final String TYPE_FILE_DISTRIBUTION = "FILE_DISTRIBUTION";
    private static final String TYPE_FILE_REVISION = "FILE_REVISION";
    private static final String TYPE_FILE_OBSOLETE = "FILE_OBSOLETE";
    private static final String TYPE_PROJECT_CODE_ASSIGNMENT = "PROJECT_CODE_ASSIGNMENT";
    private static final String TYPE_PROJECT_CODE_CHANGE = "PROJECT_CODE_CHANGE";
    private static final String TYPE_TRAINING_EXECUTION = "TRAINING_EXECUTION";

    private static final Map<String, String> LOG_TYPE_LABELS = Map.of(
            TYPE_CONTROLLED_FILE_AUDIT, "访问",
            TYPE_FILE_SUBMISSION, "提交",
            TYPE_FILE_APPROVAL, "审批",
            TYPE_FILE_RELEASE, "放行",
            TYPE_FILE_DISTRIBUTION, "分发",
            TYPE_FILE_REVISION, "升版",
            TYPE_FILE_OBSOLETE, "作废",
            TYPE_PROJECT_CODE_ASSIGNMENT, "修正任务",
            TYPE_PROJECT_CODE_CHANGE, "修正追溯",
            TYPE_TRAINING_EXECUTION, "培训"
    );

    private static final Map<String, String> ACCESS_ACTION_LABELS = Map.of(
            "UPLOAD", "上传",
            "PREVIEW", "预览",
            "OFFICE_READ", "OnlyOffice 阅读",
            "DOWNLOAD", "下载",
            "DIRECT_LINK", "直链拦截",
            "TOKEN_VALIDATE", "令牌校验",
            "TEMP_CLEANUP", "临时清理"
    );

    private static final Map<String, String> ACCESS_RESULT_LABELS = Map.of(
            "SUCCESS", "成功",
            "ALLOWED", "允许",
            "DENIED", "拒绝",
            "FAILED", "失败"
    );

    private static final Map<String, String> ASSIGNMENT_STATUS_LABELS = Map.of(
            "ACTIVE", "生效",
            "REVOKED", "已撤回",
            "EXPIRED", "已过期"
    );

    private static final Map<String, String> TRAINING_STATUS_LABELS = Map.of(
            "PENDING_VIEW", "待阅读",
            "READY_TO_ACKNOWLEDGE", "待确认",
            "ACKNOWLEDGED", "已确认"
    );

    private static final Map<String, String> FILE_STATUS_LABELS = Map.ofEntries(
            Map.entry("DRAFT", "草稿"),
            Map.entry("PENDING_DOC_CONTROL_REVIEW", "评审中"),
            Map.entry("PENDING_MATRIX_REVIEW", "评审中"),
            Map.entry("PENDING_MATRIX_APPROVAL", "审批中"),
            Map.entry("PENDING_DOC_CONTROL_APPROVAL", "审批中"),
            Map.entry("PENDING_APPLICANT_REWORK", "返工中"),
            Map.entry("PENDING_APPLICANT_TRAINING_RECORD", "待培训"),
            Map.entry("FINALIZING", "放行中"),
            Map.entry("TRAINING_IN_PROGRESS", "培训中"),
            Map.entry("PENDING_MANUAL_DISTRIBUTION", "待分发"),
            Map.entry("ACTIVE", "现行"),
            Map.entry("REJECTED", "已驳回"),
            Map.entry("WITHDRAWN", "已撤回"),
            Map.entry("OBSOLETE", "已作废"),
            Map.entry("SUPERSEDED", "已替换"),
            Map.entry("FINALIZATION_FAILED", "放行失败"),
            Map.entry("APPROVED", "已审批")
    );

    private static final Map<String, String> DISTRIBUTION_STATUS_LABELS = Map.of(
            "PENDING", "待阅读",
            "SENT", "已发送",
            "READ", "已阅读",
            "ACKNOWLEDGED", "已确认",
            "RECOVERED", "已回收"
    );

    private static final Map<String, String> DISTRIBUTION_MEDIUM_LABELS = Map.of(
            "PUBLIC_FOLDER", "公共区",
            "PAPER", "纸质"
    );

    @Resource
    private DccControlledFileAccessLogMapper accessLogMapper;
    @Resource
    private DccControlledFileAccessEventMapper accessEventMapper;
    @Resource
    private DccControlledFileDistributionMapper distributionMapper;
    @Resource
    private DccProjectCodeAssignmentMapper assignmentMapper;
    @Resource
    private DccProjectCodeAssignmentFileMapper assignmentFileMapper;
    @Resource
    private DccControlledFileMetadataChangeMapper changeMapper;
    @Resource
    private DccControlledFileMetadataChangeItemMapper changeItemMapper;
    @Resource
    private DccControlledFileTrainingProgressMapper trainingProgressMapper;
    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccProjectCodeMapper projectCodeMapper;
    @Resource
    private AdminUserApi adminUserApi;

    @Override
    public PageResult<DccControlledFileLogRespVO> getLogPage(DccControlledFileLogPageReqVO reqVO) {
        requireValidRequest(reqVO);
        List<LogCandidate> candidates = new ArrayList<>();
        if (matchesRequestedType(reqVO, TYPE_CONTROLLED_FILE_AUDIT)) {
            candidates.addAll(buildControlledFileAuditCandidates());
        }
        if (matchesAnyRequestedType(reqVO, TYPE_FILE_SUBMISSION, TYPE_FILE_APPROVAL, TYPE_FILE_RELEASE,
                TYPE_FILE_REVISION, TYPE_FILE_OBSOLETE)) {
            candidates.addAll(buildControlledFileLifecycleCandidates(reqVO));
        }
        if (matchesRequestedType(reqVO, TYPE_FILE_DISTRIBUTION)) {
            candidates.addAll(buildDistributionCandidates());
        }
        if (matchesRequestedType(reqVO, TYPE_PROJECT_CODE_ASSIGNMENT)) {
            candidates.addAll(buildProjectCodeAssignmentCandidates());
        }
        if (matchesRequestedType(reqVO, TYPE_PROJECT_CODE_CHANGE)) {
            candidates.addAll(buildProjectCodeChangeCandidates());
        }
        if (matchesRequestedType(reqVO, TYPE_TRAINING_EXECUTION)) {
            candidates.addAll(buildTrainingExecutionCandidates());
        }

        List<DccControlledFileLogRespVO> filteredRows = candidates.stream()
                .filter(candidate -> matchesFilters(candidate, reqVO))
                .sorted(Comparator.comparing(LogCandidate::occurredAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(LogCandidate::sourceRecordId,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(LogCandidate::logType,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .map(LogCandidate::row)
                .toList();
        return slicePage(reqVO, filteredRows);
    }

    private List<LogCandidate> buildControlledFileLifecycleCandidates(DccControlledFileLogPageReqVO reqVO) {
        List<DccControlledFileDO> files = controlledFileMapper.selectList();
        Map<Long, AdminUserRespDTO> userMap = selectUserMap(files.stream()
                .flatMap(file -> java.util.stream.Stream.of(file.getSubmitterId(), file.getObsoletedBy()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        List<LogCandidate> candidates = new ArrayList<>();
        for (DccControlledFileDO file : files) {
            addFileSubmissionLikeCandidate(reqVO, candidates, file, userMap);
            addFileApprovalCandidate(reqVO, candidates, file);
            addFileReleaseCandidate(reqVO, candidates, file);
        }
        return candidates;
    }

    private void addFileSubmissionLikeCandidate(DccControlledFileLogPageReqVO reqVO, List<LogCandidate> candidates,
                                                DccControlledFileDO file, Map<Long, AdminUserRespDTO> userMap) {
        String changeType = StrUtil.blankToDefault(file.getChangeType(), "NEW");
        if (StrUtil.equals(changeType, "REVISION")) {
            addFileLifecycleCandidate(reqVO, candidates, TYPE_FILE_REVISION, file, file.getSubmittedTime(),
                    "升版", label(FILE_STATUS_LABELS, file.getStatus()), file.getSubmitterId(), userMap,
                    "版本 " + nullToDash(file.getVersionNo()), null);
            return;
        }
        if (StrUtil.equals(changeType, "OBSOLETE")) {
            LocalDateTime occurredAt = firstNonNull(file.getObsoletedTime(), file.getSubmittedTime());
            Long operatorUserId = firstNonNull(file.getObsoletedBy(), file.getSubmitterId());
            addFileLifecycleCandidate(reqVO, candidates, TYPE_FILE_OBSOLETE, file, occurredAt,
                    "作废", resolveObsoleteResultLabel(file), operatorUserId, userMap,
                    "版本 " + nullToDash(file.getVersionNo()), file.getObsoleteReason());
            return;
        }
        addFileLifecycleCandidate(reqVO, candidates, TYPE_FILE_SUBMISSION, file, file.getSubmittedTime(),
                "提交", "已提交", file.getSubmitterId(), userMap,
                "版本 " + nullToDash(file.getVersionNo()), null);
    }

    private void addFileApprovalCandidate(DccControlledFileLogPageReqVO reqVO, List<LogCandidate> candidates,
                                          DccControlledFileDO file) {
        addFileLifecycleCandidate(reqVO, candidates, TYPE_FILE_APPROVAL, file, file.getApprovedTime(),
                "审批", "通过", null, Map.of(), "流程 " + nullToDash(file.getProcessInstanceId()), null);
    }

    private void addFileReleaseCandidate(DccControlledFileLogPageReqVO reqVO, List<LogCandidate> candidates,
                                         DccControlledFileDO file) {
        addFileLifecycleCandidate(reqVO, candidates, TYPE_FILE_RELEASE, file, file.getPublishedTime(),
                "放行", "已放行", null, Map.of(), "版本 " + nullToDash(file.getVersionNo()), null);
    }

    private void addFileLifecycleCandidate(DccControlledFileLogPageReqVO reqVO, List<LogCandidate> candidates,
                                           String logType, DccControlledFileDO file, LocalDateTime occurredAt,
                                           String actionLabel, String resultLabel, Long operatorUserId,
                                           Map<Long, AdminUserRespDTO> userMap, String relatedObject, String reason) {
        if (occurredAt == null || !matchesRequestedType(reqVO, logType)) {
            return;
        }
        DccControlledFileLogRespVO row = newRow(logType, file.getId(), occurredAt, actionLabel, resultLabel);
        row.setFileNumber(file.getFileNumber());
        row.setFileName(resolveFileName(file));
        row.setVersionNo(file.getVersionNo());
        row.setOperatorUserId(operatorUserId);
        row.setOperatorName(resolveUserName(userMap, operatorUserId));
        row.setRelatedObject(relatedObject);
        row.setSummary(joinNotBlank(" / ", row.getFileNumber(), row.getFileName(), row.getVersionNo(),
                actionLabel, resultLabel));
        row.setReason(reason);
        row.setDetailJson(detailJson(Map.of(
                "changeType", blankToEmpty(file.getChangeType()),
                "status", blankToEmpty(file.getStatus()),
                "processInstanceId", blankToEmpty(file.getProcessInstanceId()),
                "dccProjectCodeId", file.getDccProjectCodeId() == null ? "" : String.valueOf(file.getDccProjectCodeId()),
                "fileTypeTaxonomyId", file.getFileTypeTaxonomyId() == null ? "" : String.valueOf(file.getFileTypeTaxonomyId())
        )));
        candidates.add(new LogCandidate(row, logType, file.getStatus(), idSet(file.getId()), file.getDccProjectCodeId(),
                null, operatorUserId, null, keywordText(row)));
    }

    private List<LogCandidate> buildDistributionCandidates() {
        List<DccControlledFileDistributionDO> distributions = distributionMapper.selectList();
        Map<Long, DccControlledFileDO> fileMap = selectFileMap(distributions,
                DccControlledFileDistributionDO::getControlledFileId);
        Map<Long, AdminUserRespDTO> userMap = selectUserMap(distributions.stream()
                .flatMap(distribution -> java.util.stream.Stream.of(distribution.getAcknowledgedBy(),
                        distribution.getRecoveredBy()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        return distributions.stream()
                .map(distribution -> toDistributionCandidate(distribution,
                        fileMap.get(distribution.getControlledFileId()), userMap))
                .toList();
    }

    private LogCandidate toDistributionCandidate(DccControlledFileDistributionDO distribution, DccControlledFileDO file,
                                                 Map<Long, AdminUserRespDTO> userMap) {
        Long operatorUserId = firstNonNull(distribution.getRecoveredBy(), distribution.getAcknowledgedBy());
        LocalDateTime occurredAt = resolveDistributionOccurredAt(distribution);
        String mediumLabel = label(DISTRIBUTION_MEDIUM_LABELS, distribution.getDistributionMedium());
        String statusLabel = label(DISTRIBUTION_STATUS_LABELS, distribution.getStatus());
        String departmentText = distribution.getDepartmentId() == null ? null : "部门" + distribution.getDepartmentId();
        DccControlledFileLogRespVO row = newRow(TYPE_FILE_DISTRIBUTION, distribution.getId(), occurredAt,
                "分发", statusLabel);
        row.setFileNumber(file != null ? file.getFileNumber() : null);
        row.setFileName(file != null ? resolveFileName(file) : null);
        row.setVersionNo(file != null ? file.getVersionNo() : null);
        row.setOperatorUserId(operatorUserId);
        row.setOperatorName(resolveUserName(userMap, operatorUserId));
        row.setRelatedObject(departmentText);
        row.setSummary(joinNotBlank(" / ", row.getFileNumber(), row.getFileName(), departmentText, mediumLabel,
                statusLabel));
        row.setDetailJson(detailJson(Map.of(
                "departmentId", distribution.getDepartmentId() == null ? "" : String.valueOf(distribution.getDepartmentId()),
                "distributionMedium", blankToEmpty(distribution.getDistributionMedium()),
                "status", blankToEmpty(distribution.getStatus()),
                "acknowledgedAt", distribution.getAcknowledgedAt() == null ? "" : String.valueOf(distribution.getAcknowledgedAt()),
                "recoveredAt", distribution.getRecoveredAt() == null ? "" : String.valueOf(distribution.getRecoveredAt())
        )));
        return new LogCandidate(row, TYPE_FILE_DISTRIBUTION, distribution.getStatus(),
                idSet(distribution.getControlledFileId()), file != null ? file.getDccProjectCodeId() : null,
                null, operatorUserId, null, keywordText(row));
    }

    private List<LogCandidate> buildControlledFileAuditCandidates() {
        List<DccControlledFileAccessLogDO> accessLogs = accessLogMapper.selectList();
        Map<Long, DccControlledFileAccessEventDO> eventMap = selectMap(accessLogs,
                DccControlledFileAccessLogDO::getAccessEventId, accessEventMapper::selectBatchIds,
                DccControlledFileAccessEventDO::getId);
        Map<Long, DccControlledFileDO> fileMap = selectFileMap(accessLogs,
                DccControlledFileAccessLogDO::getControlledFileId);
        Map<Long, AdminUserRespDTO> userMap = selectUserMap(accessLogs.stream()
                .map(DccControlledFileAccessLogDO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        return accessLogs.stream()
                .map(accessLog -> toControlledFileAuditCandidate(accessLog, eventMap, fileMap, userMap))
                .toList();
    }

    private LogCandidate toControlledFileAuditCandidate(DccControlledFileAccessLogDO accessLog,
                                                        Map<Long, DccControlledFileAccessEventDO> eventMap,
                                                        Map<Long, DccControlledFileDO> fileMap,
                                                        Map<Long, AdminUserRespDTO> userMap) {
        DccControlledFileAccessEventDO event = accessLog.getAccessEventId() == null
                ? null : eventMap.get(accessLog.getAccessEventId());
        Long controlledFileId = firstNonNull(accessLog.getControlledFileId(),
                event != null ? event.getControlledFileId() : null);
        DccControlledFileDO file = fileMap.get(controlledFileId);
        Long userId = firstNonNull(accessLog.getUserId(), event != null ? event.getUserId() : null);
        String actionType = firstNotBlank(accessLog.getActionType(), event != null ? event.getAccessType() : null);
        String result = firstNotBlank(accessLog.getResult(), event != null ? event.getResult() : null);
        LocalDateTime occurredAt = event != null ? event.getOccurredAt() : accessLog.getCreateTime();
        String actionLabel = label(ACCESS_ACTION_LABELS, actionType);
        String resultLabel = label(ACCESS_RESULT_LABELS, result);
        DccControlledFileLogRespVO row = newRow(TYPE_CONTROLLED_FILE_AUDIT, accessLog.getId(), occurredAt,
                actionLabel, resultLabel);
        row.setFileNumber(file != null ? file.getFileNumber() : null);
        row.setFileName(file != null ? resolveFileName(file) : null);
        row.setVersionNo(firstNotBlank(file != null ? file.getVersionNo() : null, accessLog.getFileVersionNo(),
                event != null ? event.getFileVersionNo() : null));
        row.setOperatorUserId(userId);
        row.setOperatorName(resolveUserName(userMap, userId));
        row.setRelatedObject("受控文件访问");
        row.setSummary(joinNotBlank(" / ", row.getFileNumber(), row.getFileName(), actionLabel, resultLabel));
        row.setReason(firstNotBlank(accessLog.getReason(), event != null ? event.getFailureReason() : null));
        row.setDetailJson(detailJson(Map.of(
                "accessEventCode", blankToEmpty(firstNotBlank(accessLog.getAccessEventCode(),
                        event != null ? event.getAccessEventCode() : null)),
                "requestId", blankToEmpty(firstNotBlank(accessLog.getRequestId(),
                        event != null ? event.getRequestId() : null)),
                "sourceIp", blankToEmpty(firstNotBlank(accessLog.getSourceIp(),
                        event != null ? event.getSourceIp() : null)),
                "userAgent", blankToEmpty(firstNotBlank(accessLog.getUserAgent(),
                        event != null ? event.getUserAgent() : null)),
                "purpose", blankToEmpty(firstNotBlank(accessLog.getPurpose(), event != null ? event.getPurpose() : null))
        )));
        return new LogCandidate(row, actionType, result, idSet(controlledFileId), null, null, userId, null,
                keywordText(row));
    }

    private List<LogCandidate> buildProjectCodeAssignmentCandidates() {
        List<DccProjectCodeAssignmentDO> assignments = assignmentMapper.selectList();
        Map<Long, DccProjectCodeDO> projectCodeMap = selectMap(assignments,
                DccProjectCodeAssignmentDO::getProjectCodeId, projectCodeMapper::selectBatchIds,
                DccProjectCodeDO::getId);
        Map<Long, List<DccProjectCodeAssignmentFileDO>> assignmentFileMap = assignmentFileMapper.selectList().stream()
                .collect(Collectors.groupingBy(DccProjectCodeAssignmentFileDO::getAssignmentId));
        Set<Long> userIds = assignments.stream()
                .flatMap(assignment -> java.util.stream.Stream.of(assignment.getAssignedBy(),
                        assignment.getAssigneeUserId(), assignment.getRevokedBy()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> userMap = selectUserMap(userIds);
        return assignments.stream()
                .map(assignment -> toProjectCodeAssignmentCandidate(assignment,
                        projectCodeMap.get(assignment.getProjectCodeId()),
                        assignmentFileMap.getOrDefault(assignment.getId(), List.of()), userMap))
                .toList();
    }

    private LogCandidate toProjectCodeAssignmentCandidate(DccProjectCodeAssignmentDO assignment,
                                                          DccProjectCodeDO projectCode,
                                                          List<DccProjectCodeAssignmentFileDO> assignmentFiles,
                                                          Map<Long, AdminUserRespDTO> userMap) {
        Long operatorUserId = firstNonNull(assignment.getAssignedBy(), assignment.getAssigneeUserId());
        LocalDateTime occurredAt = firstNonNull(assignment.getAssignedTime(), assignment.getCreateTime());
        String result = assignment.getStatus();
        DccControlledFileLogRespVO row = newRow(TYPE_PROJECT_CODE_ASSIGNMENT, assignment.getId(), occurredAt,
                "修正指派", label(ASSIGNMENT_STATUS_LABELS, result));
        DccProjectCodeAssignmentFileDO firstFile = assignmentFiles.isEmpty() ? null : assignmentFiles.get(0);
        row.setFileNumber(firstFile != null ? firstFile.getFileNumberSnapshot() : null);
        row.setFileName(resolveAssignmentFileName(assignment, firstFile, assignmentFiles));
        row.setOperatorUserId(operatorUserId);
        row.setOperatorName(resolveUserName(userMap, operatorUserId));
        row.setRelatedObject(resolveProjectCodeText(projectCode));
        row.setSummary(joinNotBlank(" / ", assignment.getAssignmentNo(),
                "指派给 " + nullToDash(resolveUserName(userMap, assignment.getAssigneeUserId())),
                row.getRelatedObject()));
        row.setReason(firstNotBlank(assignment.getAssignmentReason(), assignment.getRevokeReason()));
        row.setDetailJson(detailJson(Map.of(
                "assignmentNo", blankToEmpty(assignment.getAssignmentNo()),
                "scopeMode", blankToEmpty(assignment.getScopeMode()),
                "assigneeUserId", assignment.getAssigneeUserId() == null ? "" : String.valueOf(assignment.getAssigneeUserId()),
                "fileCount", assignment.getFileCount() == null ? "" : String.valueOf(assignment.getFileCount()),
                "changedFileCount", assignment.getChangedFileCount() == null ? "" : String.valueOf(assignment.getChangedFileCount()),
                "changedFieldCount", assignment.getChangedFieldCount() == null ? "" : String.valueOf(assignment.getChangedFieldCount())
        )));
        String keywordText = keywordText(row) + " " + assignment.getAssignmentNo() + " " + resolveProjectCodeText(projectCode);
        return new LogCandidate(row, TYPE_PROJECT_CODE_ASSIGNMENT, result,
                assignmentFiles.stream()
                        .map(DccProjectCodeAssignmentFileDO::getControlledFileId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()),
                assignment.getProjectCodeId(), assignment.getId(), operatorUserId, null, keywordText);
    }

    private List<LogCandidate> buildProjectCodeChangeCandidates() {
        List<DccControlledFileMetadataChangeItemDO> items = changeItemMapper.selectList();
        Map<Long, DccControlledFileMetadataChangeDO> changeMap = selectMap(items,
                DccControlledFileMetadataChangeItemDO::getChangeId, changeMapper::selectBatchIds,
                DccControlledFileMetadataChangeDO::getId);
        Map<Long, DccControlledFileDO> fileMap = selectFileMap(items,
                DccControlledFileMetadataChangeItemDO::getControlledFileId);
        Map<Long, DccProjectCodeDO> projectCodeMap = selectMap(items,
                DccControlledFileMetadataChangeItemDO::getProjectCodeId, projectCodeMapper::selectBatchIds,
                DccProjectCodeDO::getId);
        Map<Long, DccProjectCodeAssignmentDO> assignmentMap = selectMap(items,
                DccControlledFileMetadataChangeItemDO::getAssignmentId, assignmentMapper::selectBatchIds,
                DccProjectCodeAssignmentDO::getId);
        Map<Long, AdminUserRespDTO> userMap = selectUserMap(items.stream()
                .map(DccControlledFileMetadataChangeItemDO::getOperatorUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        return items.stream()
                .map(item -> toProjectCodeChangeCandidate(item, changeMap, fileMap, projectCodeMap, assignmentMap, userMap))
                .toList();
    }

    private LogCandidate toProjectCodeChangeCandidate(DccControlledFileMetadataChangeItemDO item,
                                                       Map<Long, DccControlledFileMetadataChangeDO> changeMap,
                                                       Map<Long, DccControlledFileDO> fileMap,
                                                       Map<Long, DccProjectCodeDO> projectCodeMap,
                                                       Map<Long, DccProjectCodeAssignmentDO> assignmentMap,
                                                       Map<Long, AdminUserRespDTO> userMap) {
        DccControlledFileMetadataChangeDO change = item.getChangeId() == null ? null : changeMap.get(item.getChangeId());
        DccControlledFileDO file = item.getControlledFileId() == null ? null : fileMap.get(item.getControlledFileId());
        DccProjectCodeDO projectCode = item.getProjectCodeId() == null ? null : projectCodeMap.get(item.getProjectCodeId());
        DccProjectCodeAssignmentDO assignment = item.getAssignmentId() == null ? null : assignmentMap.get(item.getAssignmentId());
        LocalDateTime occurredAt = firstNonNull(item.getChangedTime(), change != null ? change.getChangedTime() : null);
        DccControlledFileLogRespVO row = newRow(TYPE_PROJECT_CODE_CHANGE, item.getId(), occurredAt,
                "字段修改", "成功");
        row.setFileNumber(file != null ? file.getFileNumber() : null);
        row.setFileName(file != null ? resolveFileName(file) : null);
        row.setVersionNo(file != null ? file.getVersionNo() : null);
        row.setOperatorUserId(item.getOperatorUserId());
        row.setOperatorName(resolveUserName(userMap, item.getOperatorUserId()));
        row.setRelatedObject(resolveProjectCodeText(projectCode));
        String valueChangeText = joinNotBlank(" -> ", item.getOldValueText(), item.getNewValueText());
        row.setSummary(joinNotBlank(" / ", item.getFieldLabel(), valueChangeText));
        row.setOldValueText(item.getOldValueText());
        row.setNewValueText(item.getNewValueText());
        row.setReason(change != null ? change.getChangeReason() : null);
        row.setDetailJson(detailJson(Map.of(
                "changeId", item.getChangeId() == null ? "" : String.valueOf(item.getChangeId()),
                "assignmentNo", assignment == null ? "" : blankToEmpty(assignment.getAssignmentNo()),
                "fieldName", blankToEmpty(item.getFieldName()),
                "fieldLabel", blankToEmpty(item.getFieldLabel()),
                "source", change == null ? "" : blankToEmpty(change.getSource())
        )));
        String keywordText = joinNotBlank(" ", keywordText(row), item.getFieldName(), item.getFieldLabel(),
                resolveProjectCodeText(projectCode));
        return new LogCandidate(row, TYPE_PROJECT_CODE_CHANGE, "SUCCESS",
                idSet(item.getControlledFileId()), item.getProjectCodeId(), item.getAssignmentId(),
                item.getOperatorUserId(), item.getFieldName(), keywordText);
    }

    private List<LogCandidate> buildTrainingExecutionCandidates() {
        List<DccControlledFileTrainingProgressDO> progressList = trainingProgressMapper.selectList();
        Map<Long, DccControlledFileDO> fileMap = selectFileMap(progressList,
                DccControlledFileTrainingProgressDO::getControlledFileId);
        Map<Long, AdminUserRespDTO> userMap = selectUserMap(progressList.stream()
                .map(DccControlledFileTrainingProgressDO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        return progressList.stream()
                .map(progress -> toTrainingExecutionCandidate(progress,
                        fileMap.get(progress.getControlledFileId()), userMap))
                .toList();
    }

    private LogCandidate toTrainingExecutionCandidate(DccControlledFileTrainingProgressDO progress,
                                                      DccControlledFileDO file,
                                                      Map<Long, AdminUserRespDTO> userMap) {
        String status = resolveTrainingStatus(progress);
        LocalDateTime occurredAt = firstNonNull(progress.getAcknowledgedAt(), progress.getLastViewedAt(),
                progress.getFirstViewedAt(), progress.getCreateTime());
        DccControlledFileLogRespVO row = newRow(TYPE_TRAINING_EXECUTION, progress.getId(), occurredAt,
                resolveTrainingActionLabel(progress), label(TRAINING_STATUS_LABELS, status));
        row.setFileNumber(file != null ? file.getFileNumber() : null);
        row.setFileName(file != null ? resolveFileName(file) : null);
        row.setVersionNo(file != null ? file.getVersionNo() : null);
        row.setOperatorUserId(progress.getUserId());
        row.setOperatorName(resolveUserName(userMap, progress.getUserId()));
        row.setRelatedObject("培训执行");
        row.setSummary(joinNotBlank(" / ", row.getFileNumber(), row.getFileName(), row.getActionLabel(), row.getResultLabel()));
        row.setDetailJson(detailJson(Map.of(
                "requiredViewSeconds", progress.getRequiredViewSeconds() == null ? "" : String.valueOf(progress.getRequiredViewSeconds()),
                "accumulatedViewSeconds", progress.getAccumulatedViewSeconds() == null ? "" : String.valueOf(progress.getAccumulatedViewSeconds()),
                "firstViewedAt", progress.getFirstViewedAt() == null ? "" : String.valueOf(progress.getFirstViewedAt()),
                "lastViewedAt", progress.getLastViewedAt() == null ? "" : String.valueOf(progress.getLastViewedAt()),
                "acknowledgedAt", progress.getAcknowledgedAt() == null ? "" : String.valueOf(progress.getAcknowledgedAt())
        )));
        return new LogCandidate(row, resolveTrainingActionType(progress), status,
                idSet(progress.getControlledFileId()), file != null ? file.getDccProjectCodeId() : null,
                null, progress.getUserId(), null, keywordText(row));
    }

    private DccControlledFileLogRespVO newRow(String logType, Long sourceRecordId, LocalDateTime occurredAt,
                                              String actionLabel, String resultLabel) {
        DccControlledFileLogRespVO row = new DccControlledFileLogRespVO();
        row.setId(logType + ":" + sourceRecordId);
        row.setLogType(logType);
        row.setSourceRecordId(sourceRecordId);
        row.setOccurredAt(occurredAt);
        row.setActionLabel(actionLabel);
        row.setResultLabel(resultLabel);
        return row;
    }

    private boolean matchesFilters(LogCandidate candidate, DccControlledFileLogPageReqVO reqVO) {
        return matchesExact(reqVO.getActionType(), candidate.actionType())
                && matchesExact(reqVO.getResult(), candidate.result())
                && matchesIdSet(reqVO.getControlledFileId(), candidate.controlledFileIds())
                && matchesLong(reqVO.getProjectCodeId(), candidate.projectCodeId())
                && matchesLong(reqVO.getAssignmentId(), candidate.assignmentId())
                && matchesLong(reqVO.getOperatorUserId(), candidate.operatorUserId())
                && matchesExact(reqVO.getFieldName(), candidate.fieldName())
                && matchesOccurredAt(candidate.occurredAt(), reqVO.getOccurredAt())
                && matchesKeyword(candidate, reqVO.getKeyword());
    }

    private boolean matchesKeyword(LogCandidate candidate, String keyword) {
        String trimmedKeyword = StrUtil.trimToNull(keyword);
        if (trimmedKeyword == null) {
            return true;
        }
        return StrUtil.containsIgnoreCase(candidate.keywordText(), trimmedKeyword);
    }

    private boolean matchesOccurredAt(LocalDateTime occurredAt, LocalDateTime[] range) {
        if (range == null) {
            return true;
        }
        if (occurredAt == null) {
            return false;
        }
        if (range[0] != null && occurredAt.isBefore(range[0])) {
            return false;
        }
        return range[1] == null || !occurredAt.isAfter(range[1]);
    }

    private boolean matchesRequestedType(DccControlledFileLogPageReqVO reqVO, String logType) {
        String requestedType = StrUtil.trimToNull(reqVO.getLogType());
        return requestedType == null || StrUtil.equals(requestedType, logType);
    }

    private boolean matchesAnyRequestedType(DccControlledFileLogPageReqVO reqVO, String... logTypes) {
        for (String logType : logTypes) {
            if (matchesRequestedType(reqVO, logType)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesExact(String requested, String actual) {
        String normalized = StrUtil.trimToNull(requested);
        return normalized == null || StrUtil.equals(normalized, actual);
    }

    private boolean matchesLong(Long requested, Long actual) {
        return requested == null || Objects.equals(requested, actual);
    }

    private boolean matchesIdSet(Long requested, Set<Long> actualIds) {
        return requested == null || actualIds.contains(requested);
    }

    private PageResult<DccControlledFileLogRespVO> slicePage(DccControlledFileLogPageReqVO reqVO,
                                                             List<DccControlledFileLogRespVO> rows) {
        if (rows.isEmpty()) {
            return PageResult.empty(0L);
        }
        int pageNo = Math.max(reqVO.getPageNo() == null ? 1 : reqVO.getPageNo(), 1);
        int pageSize = Math.max(reqVO.getPageSize() == null ? 10 : reqVO.getPageSize(), 1);
        int fromIndex = Math.min((pageNo - 1) * pageSize, rows.size());
        int toIndex = Math.min(fromIndex + pageSize, rows.size());
        return new PageResult<>(rows.subList(fromIndex, toIndex), (long) rows.size());
    }

    private void requireValidRequest(DccControlledFileLogPageReqVO reqVO) {
        if (reqVO == null) {
            throw new IllegalArgumentException("DCC controlled file log query is required");
        }
        LocalDateTime[] occurredAt = reqVO.getOccurredAt();
        if (occurredAt == null) {
            return;
        }
        if (occurredAt.length != 2) {
            throw new IllegalArgumentException("occurredAt requires start and end");
        }
        if (occurredAt[0] == null && occurredAt[1] == null) {
            throw new IllegalArgumentException("occurredAt requires at least one boundary");
        }
        if (occurredAt[0] != null && occurredAt[1] != null && occurredAt[0].isAfter(occurredAt[1])) {
            throw new IllegalArgumentException("occurredAt start must be before or equal to end");
        }
    }

    private <T, R> Map<Long, R> selectMap(List<T> sources, Function<T, Long> idGetter,
                                          Function<Collection<Long>, List<R>> selector,
                                          Function<R, Long> resultIdGetter) {
        List<Long> ids = sources.stream().map(idGetter).filter(Objects::nonNull).distinct().toList();
        return ids.isEmpty() ? Map.of() : convertMap(selector.apply(ids), resultIdGetter);
    }

    private <T> Map<Long, DccControlledFileDO> selectFileMap(List<T> sources, Function<T, Long> idGetter) {
        return selectMap(sources, idGetter, controlledFileMapper::selectBatchIds, DccControlledFileDO::getId);
    }

    private Map<Long, AdminUserRespDTO> selectUserMap(Collection<Long> userIds) {
        List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<AdminUserRespDTO> users = adminUserApi.getUserList(ids);
        if (users == null) {
            throw new IllegalStateException("admin user api returned null");
        }
        return users.stream()
                .filter(user -> user.getId() != null)
                .collect(Collectors.toMap(AdminUserRespDTO::getId, user -> user,
                        (left, right) -> left, LinkedHashMap::new));
    }

    private String resolveUserName(Map<Long, AdminUserRespDTO> userMap, Long userId) {
        if (userId == null) {
            return null;
        }
        AdminUserRespDTO user = userMap.get(userId);
        return user != null && StrUtil.isNotBlank(user.getNickname())
                ? user.getNickname()
                : String.valueOf(userId);
    }

    private String resolveFileName(DccControlledFileDO file) {
        return firstNotBlank(file.getFileName(), file.getTitle());
    }

    private String resolveProjectCodeText(DccProjectCodeDO projectCode) {
        if (projectCode == null) {
            return null;
        }
        return joinNotBlank(" / ", projectCode.getProjectName(), projectCode.getProjectCode());
    }

    private String resolveAssignmentFileName(DccProjectCodeAssignmentDO assignment,
                                             DccProjectCodeAssignmentFileDO firstFile,
                                             List<DccProjectCodeAssignmentFileDO> assignmentFiles) {
        if (firstFile != null && assignmentFiles.size() == 1) {
            return firstFile.getFileNameSnapshot();
        }
        Integer fileCount = assignment.getFileCount();
        if (fileCount != null && fileCount > 0) {
            return fileCount + " 个文件";
        }
        return assignmentFiles.isEmpty() ? null : assignmentFiles.size() + " 个文件";
    }

    private String resolveObsoleteResultLabel(DccControlledFileDO file) {
        return StrUtil.equals(file.getStatus(), "OBSOLETE") ? "已作废" : label(FILE_STATUS_LABELS, file.getStatus());
    }

    private LocalDateTime resolveDistributionOccurredAt(DccControlledFileDistributionDO distribution) {
        if (StrUtil.equals(distribution.getStatus(), "RECOVERED") && distribution.getRecoveredAt() != null) {
            return distribution.getRecoveredAt();
        }
        if ((StrUtil.equals(distribution.getStatus(), "ACKNOWLEDGED") || StrUtil.equals(distribution.getStatus(), "READ"))
                && distribution.getAcknowledgedAt() != null) {
            return distribution.getAcknowledgedAt();
        }
        return firstNonNull(distribution.getUpdateTime(), distribution.getCreateTime());
    }

    private String resolveTrainingStatus(DccControlledFileTrainingProgressDO progress) {
        if (progress.getAcknowledgedAt() != null) {
            return "ACKNOWLEDGED";
        }
        if (safeSeconds(progress.getAccumulatedViewSeconds()) >= safeSeconds(progress.getRequiredViewSeconds())
                && safeSeconds(progress.getRequiredViewSeconds()) > 0) {
            return "READY_TO_ACKNOWLEDGE";
        }
        return "PENDING_VIEW";
    }

    private String resolveTrainingActionType(DccControlledFileTrainingProgressDO progress) {
        if (progress.getAcknowledgedAt() != null) {
            return "TRAINING_ACKNOWLEDGE";
        }
        if (progress.getLastViewedAt() != null || progress.getFirstViewedAt() != null) {
            return "TRAINING_VIEW";
        }
        return "TRAINING_PENDING";
    }

    private String resolveTrainingActionLabel(DccControlledFileTrainingProgressDO progress) {
        return switch (resolveTrainingActionType(progress)) {
            case "TRAINING_ACKNOWLEDGE" -> "培训确认";
            case "TRAINING_VIEW" -> "培训阅读";
            default -> "培训待执行";
        };
    }

    private int safeSeconds(Integer value) {
        return value == null ? 0 : value;
    }

    private Set<Long> idSet(Long value) {
        return value == null ? Set.of() : Set.of(value);
    }

    private String label(Map<String, String> labels, String value) {
        if (StrUtil.isBlank(value)) {
            return "-";
        }
        return labels.getOrDefault(value, value);
    }

    private String detailJson(Map<String, ?> values) {
        return JsonUtils.toJsonString(values);
    }

    private String keywordText(DccControlledFileLogRespVO row) {
        return joinNotBlank(" ", row.getLogType(), LOG_TYPE_LABELS.get(row.getLogType()), row.getActionLabel(),
                row.getResultLabel(), row.getFileNumber(), row.getFileName(), row.getVersionNo(),
                row.getOperatorName(), row.getRelatedObject(), row.getSummary(), row.getOldValueText(),
                row.getNewValueText(), row.getReason(), row.getDetailJson());
    }

    private String joinNotBlank(String delimiter, String... values) {
        return java.util.Arrays.stream(values)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining(delimiter));
    }

    private String nullToDash(String value) {
        return StrUtil.isBlank(value) ? "-" : value;
    }

    private String blankToEmpty(String value) {
        return StrUtil.blankToDefault(value, "");
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private record LogCandidate(DccControlledFileLogRespVO row, String actionType, String result,
                                Set<Long> controlledFileIds, Long projectCodeId, Long assignmentId,
                                Long operatorUserId, String fieldName, String keywordText) {

        private LocalDateTime occurredAt() {
            return row.getOccurredAt();
        }

        private Long sourceRecordId() {
            return row.getSourceRecordId();
        }

        private String logType() {
            return row.getLogType();
        }
    }

}
