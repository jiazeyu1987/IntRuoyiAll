package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRouteSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;

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
                && "APPROVE".equals(signature.getActionType())
                && (stageCode + "_APPROVE").equals(signature.getMeaningCode())
                && Boolean.TRUE.equals(signature.getPasswordVerified()) && signature.getSignedAt() != null
                && StrUtil.isNotBlank(signature.getTaskId()) && StrUtil.isNotBlank(signature.getEvidenceHash())
                && "VALID".equals(signature.getEvidenceStatus());
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
