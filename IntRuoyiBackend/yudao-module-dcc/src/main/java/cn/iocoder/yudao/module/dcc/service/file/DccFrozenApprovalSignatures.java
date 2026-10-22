package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRouteSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTaskAssigneeSnapshotDO;

import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING;

/** Finalization uses the frozen roster, never the reduced set of runtime tasks. */
final class DccFrozenApprovalSignatures {
    private static final Set<String> LEGACY_STAGES = Set.of("DOC_CONTROL_REVIEW", "MATRIX_REVIEW",
            "MATRIX_APPROVAL", "DOC_CONTROL_APPROVAL");
    private static final Set<String> THREE_WORKFLOW_STAGES = Set.of("DOC_CONTROL_REVIEW", "MATRIX_REVIEW",
            "MATRIX_APPROVAL");

    private DccFrozenApprovalSignatures() {}
    static void requireComplete(DccControlledFileDO file,List<DccControlledFileRouteSnapshotDO> snapshots,List<DccControlledFileSignatureDO> signatures,
            List<DccControlledFileTaskAssigneeSnapshotDO> assignments,DccBoundWorkflowContractResolver.BoundExecution bound){
        if(bound==null||file==null||!Objects.equals(bound.controlledFileId(),file.getId())||!Objects.equals(bound.tenantId(),file.getTenantId())
                ||!Objects.equals(bound.processInstanceId(),file.getProcessInstanceId())||!Objects.equals(bound.processKey(),file.getProcessDefinitionKey()))
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        if(!bound.trainingAfterDocumentControl()){requireComplete(file,snapshots,signatures,assignments);return;}
        if(!DccApplicationExecutionContractService.isPublicContract(file.getPreparedWorkflowContract())||bound.attributeRound()==null
                ||!Objects.equals(file.getPreparedWorkflowContract(),bound.executionContract())
                ||!Objects.equals(bound.processDefinitionId(),file.getPreparedProcessDefinitionId())||!Objects.equals(bound.definitionDigest(),file.getPreparedDefinitionDigest())
                ||snapshots==null||snapshots.size()!=4||signatures==null||assignments==null)throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        var supervisor=snapshots.stream().filter(row->row!=null&&"SUPERVISOR_REVIEW".equals(row.getStageCode())).toList();
        if(supervisor.size()!=1)throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        var stage=supervisor.get(0);var users=parseIds(stage.getResolvedUserIds());var candidates=parseIds(stage.getCandidateSourceIds());
        if(!Objects.equals(file.getId(),stage.getControlledFileId())||!Integer.valueOf(0).equals(stage.getStageNo())||!Integer.valueOf(0).equals(stage.getStageOrder())
                ||!"USER".equals(stage.getCandidateSourceType())||users.size()!=1||!users.equals(candidates)||!Objects.equals(users.get(0),stage.getCandidateSourceId())
                ||!"ANY".equals(stage.getApproveMethod())||Boolean.TRUE.equals(stage.getRequireAllApprovals())||stage.getApproveRatio()!=null
                ||signatures.stream().filter(Objects::nonNull).noneMatch(sig->Objects.equals(users.get(0),sig.getActorId())&&isValidStageSignature(file,"SUPERVISOR_REVIEW",sig)))
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        var nativeStages=snapshots.stream().filter(row->row!=null&&!"SUPERVISOR_REVIEW".equals(row.getStageCode())).toList();
        requireComplete(file,nativeStages,signatures,assignments);
        for(var row:assignments){if(row!=null&&"MATRIX_REVIEW".equals(row.getStageCode())&&Objects.equals(file.getProcessInstanceId(),row.getProcessInstanceId())){
            var signed=signatures.stream().filter(Objects::nonNull).filter(sig->Objects.equals(row.getAssignmentSignatureId(),sig.getId())).toList();
            if(signed.size()!=1||!Objects.equals(row.getAssignedTime(),signed.get(0).getSignedAt()))throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        }}
        if(file.getFileOwnerUserId()==null||file.getFileOwnerUserId()<=0||file.getFileOwnerSignatureId()==null||file.getFileOwnerSelectedTime()==null
                ||!Objects.equals(file.getFileOwnerProcessInstanceId(),file.getProcessInstanceId())||file.getFileOwnerApprovalTaskId()==null
                ||signatures.stream().filter(Objects::nonNull).noneMatch(sig->Objects.equals(file.getFileOwnerSignatureId(),sig.getId())
                    &&Objects.equals(file.getFileOwnerApprovalTaskId(),sig.getTaskId())&&isValidStageSignature(file,"MATRIX_APPROVAL",sig)))
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
    }

    static void requireComplete(DccControlledFileDO file, List<DccControlledFileRouteSnapshotDO> snapshots,
            List<DccControlledFileSignatureDO> signatures,
            List<cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTaskAssigneeSnapshotDO> assignments) {
        if (!isThreeWorkflow(file)) {
            requireComplete(file,snapshots,signatures);
            return;
        }
        if (assignments == null || snapshots == null || signatures == null)
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        List<DccControlledFileRouteSnapshotDO> actualRoster = new ArrayList<>();
        for (var stage : snapshots) {
            if (!"MATRIX_REVIEW".equals(stage.getStageCode())) { actualRoster.add(stage); continue; }
            List<Long> departments = parseIds(stage.getCandidateSourceIds());
            List<Long> actors = new ArrayList<>();
            for (Long department : departments) {
                var matches = assignments.stream().filter(row -> Objects.equals(file.getId(),row.getControlledFileId())
                        && Objects.equals(department,row.getDepartmentId())
                        && "MATRIX_REVIEW".equals(row.getStageCode())
                        && Objects.equals(file.getProcessInstanceId(),row.getProcessInstanceId())).toList();
                if (matches.size() != 1) throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
                var row = matches.get(0);
                if (row.getAssignmentSignatureId() == null || row.getAssignedTime() == null
                        || row.getBpmTaskId() == null || signatures.stream().noneMatch(signature ->
                        Objects.equals(row.getAssignmentSignatureId(),signature.getId())
                                && Objects.equals(file.getId(),signature.getControlledFileId())
                                && Objects.equals(file.getId(),signature.getRevisionId())
                                && Objects.equals(file.getVersionNo(),signature.getVersionNo())
                                && signatureMatchesWorkflowRound(file,signature)
                                && Objects.equals(row.getLeaderUserId(),signature.getActorId())
                                && Objects.equals(row.getBpmTaskId(),signature.getTaskId())
                                && "MATRIX_REVIEW_ASSIGN".equals(signature.getMeaningCode())
                                && Boolean.TRUE.equals(signature.getPasswordVerified())
                                && signature.getSignedAt()!=null && StrUtil.isNotBlank(signature.getEvidenceHash())
                                && "ASSIGN".equals(signature.getActionType()) && "VALID".equals(signature.getEvidenceStatus()))
                        || signatures.stream().noneMatch(signature -> Objects.equals(row.getBpmTaskId(),signature.getTaskId())
                                && Objects.equals(row.getAssigneeUserId(),signature.getActorId())
                                && isValidStageSignature(file,"MATRIX_REVIEW",signature)))
                    throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
                actors.add(row.getAssigneeUserId());
            }
            var copy = DccControlledFileRouteSnapshotDO.builder().controlledFileId(stage.getControlledFileId())
                    .stageCode(stage.getStageCode()).candidateSourceType("DEPT")
                    .candidateSourceIds(stage.getCandidateSourceIds()).approveMethod(stage.getApproveMethod())
                    .requireAllApprovals(stage.getRequireAllApprovals())
                    .resolvedUserIds(actors.stream().map(String::valueOf).collect(Collectors.joining(","))).build();
            actualRoster.add(copy);
        }
        requireComplete(file,actualRoster,signatures);
    }

    static void requireComplete(DccControlledFileDO file, List<DccControlledFileRouteSnapshotDO> snapshots,
                                List<DccControlledFileSignatureDO> signatures) {
        Set<String> expectedStages = isThreeWorkflow(file) ? THREE_WORKFLOW_STAGES : LEGACY_STAGES;
        if (file == null || file.getId() == null || StrUtil.isBlank(file.getVersionNo())
                || snapshots == null || snapshots.size() != expectedStages.size() || signatures == null) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        }
        Set<String> seen = new HashSet<>();
        for (var stage : snapshots) {
            if (stage == null || !Objects.equals(file.getId(), stage.getControlledFileId())
                    || !expectedStages.contains(stage.getStageCode()) || !seen.add(stage.getStageCode())
                    || StrUtil.isBlank(stage.getResolvedUserIds())) {
                throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
            }
            List<Long> requiredUsers = parseIds(stage.getResolvedUserIds());
            boolean departmentObligations = "DEPT".equalsIgnoreCase(stage.getCandidateSourceType());
            if (departmentObligations) {
                List<Long> departmentIds = parseIds(stage.getCandidateSourceIds());
                if (new HashSet<>(departmentIds).size() != departmentIds.size()
                        || departmentIds.size() != requiredUsers.size()) {
                    throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
                }
            } else if (new HashSet<>(requiredUsers).size() != requiredUsers.size()) {
                throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
            }
            Map<String, Long> signedTaskActors = signatures.stream().filter(Objects::nonNull)
                    .filter(signature -> isValidStageSignature(file, stage.getStageCode(), signature))
                    .filter(signature -> requiredUsers.contains(signature.getActorId()))
                    .collect(Collectors.toMap(DccControlledFileSignatureDO::getTaskId,
                            DccControlledFileSignatureDO::getActorId, (first, duplicate) -> {
                                if (!Objects.equals(first, duplicate)) {
                                    throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
                                }
                                return first;
                            }));
            Map<Long, Long> signedUserCounts = signedTaskActors.values().stream()
                    .collect(Collectors.groupingBy(userId -> userId, Collectors.counting()));
            if (Boolean.TRUE.equals(stage.getRequireAllApprovals()) || "ALL".equals(stage.getApproveMethod())) {
                if (departmentObligations ? !coversRequiredDepartmentApprovals(requiredUsers, signedUserCounts)
                        : !signedUserCounts.keySet().containsAll(requiredUsers)) {
                    throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
                }
            } else if ("ANY".equals(stage.getApproveMethod()) && signedTaskActors.isEmpty()) {
                throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
            } else {
                if (!"ANY".equals(stage.getApproveMethod())) {
                    throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
                }
            }
        }
    }

    private static List<Long> parseIds(String rawIds) {
        if (StrUtil.isBlank(rawIds)) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        }
        List<Long> ids = new ArrayList<>();
        try {
            for (String raw : rawIds.split(",", -1)) {
                long id = Long.parseLong(raw.trim());
                if (id <= 0) throw new NumberFormatException("invalid id");
                ids.add(id);
            }
        } catch (NumberFormatException invalid) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        }
        return ids;
    }

    private static boolean isValidStageSignature(DccControlledFileDO file, String stageCode,
                                                 DccControlledFileSignatureDO signature) {
        return signature.getId() != null && Objects.equals(file.getId(), signature.getControlledFileId())
                && Objects.equals(file.getId(), signature.getRevisionId())
                && Objects.equals(file.getVersionNo(), signature.getVersionNo())
                && signatureMatchesWorkflowRound(file,signature)
                && "APPROVE".equals(signature.getActionType())
                && (stageCode + "_APPROVE").equals(signature.getMeaningCode())
                && Boolean.TRUE.equals(signature.getPasswordVerified()) && signature.getSignedAt() != null
                && StrUtil.isNotBlank(signature.getTaskId()) && StrUtil.isNotBlank(signature.getEvidenceHash())
                && "VALID".equals(signature.getEvidenceStatus());
    }

    private static boolean signatureMatchesWorkflowRound(DccControlledFileDO file, DccControlledFileSignatureDO signature) {
        if(isThreeWorkflow(file)) return StrUtil.isNotBlank(file.getProcessInstanceId())
                && DccControlledFileSignatureEvidenceServiceImpl.PAYLOAD_VERSION_V4_WORKFLOW.equals(signature.getEvidencePayloadVersion())
                && Objects.equals(file.getProcessInstanceId(),signature.getProcessInstanceId());
        return !DccControlledFileSignatureEvidenceServiceImpl.PAYLOAD_VERSION_V4_WORKFLOW.equals(signature.getEvidencePayloadVersion())
                || Objects.equals(file.getProcessInstanceId(),signature.getProcessInstanceId());
    }

    private static boolean coversRequiredDepartmentApprovals(List<Long> requiredUsers,
                                                              Map<Long, Long> signedUserCounts) {
        Map<Long, Long> requiredCounts = requiredUsers.stream()
                .collect(Collectors.groupingBy(userId -> userId, Collectors.counting()));
        return requiredCounts.entrySet().stream().allMatch(entry ->
                signedUserCounts.getOrDefault(entry.getKey(), 0L) >= entry.getValue());
    }

    private static boolean isThreeWorkflow(DccControlledFileDO file) {
        return file != null && (DccControlledFileProcessDefinitionKeys.UPLOAD.equals(file.getProcessDefinitionKey())
                || DccControlledFileProcessDefinitionKeys.REVISION.equals(file.getProcessDefinitionKey()));
    }
}
