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
    private static final Set<String> STAGES = Set.of("DOC_CONTROL_REVIEW", "MATRIX_REVIEW",
            "MATRIX_APPROVAL", "DOC_CONTROL_APPROVAL");

    private DccFrozenApprovalSignatures() {}

    static void requireComplete(DccControlledFileDO file, List<DccControlledFileRouteSnapshotDO> snapshots,
                                List<DccControlledFileSignatureDO> signatures) {
        if (file == null || file.getId() == null || StrUtil.isBlank(file.getVersionNo())
                || snapshots == null || snapshots.size() != STAGES.size() || signatures == null) {
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
        }
        Set<String> seen = new HashSet<>();
        for (var stage : snapshots) {
            if (stage == null || !Objects.equals(file.getId(), stage.getControlledFileId())
                    || !STAGES.contains(stage.getStageCode()) || !seen.add(stage.getStageCode())
                    || StrUtil.isBlank(stage.getResolvedUserIds())) {
                throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
            }
            Set<Long> required = new LinkedHashSet<>();
            try {
                for (String raw : stage.getResolvedUserIds().split(",", -1)) {
                    long id = Long.parseLong(raw.trim());
                    if (id <= 0 || !required.add(id)) throw new NumberFormatException("invalid roster");
                }
            } catch (NumberFormatException invalid) {
                throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
            }
            Set<Long> signed = signatures.stream().filter(Objects::nonNull)
                    .filter(s -> s.getId() != null && Objects.equals(file.getId(), s.getControlledFileId())
                            && Objects.equals(file.getId(), s.getRevisionId())
                            && Objects.equals(file.getVersionNo(), s.getVersionNo())
                            && "APPROVE".equals(s.getActionType())
                            && (stage.getStageCode() + "_APPROVE").equals(s.getMeaningCode())
                            && Boolean.TRUE.equals(s.getPasswordVerified()) && s.getSignedAt() != null
                            && StrUtil.isNotBlank(s.getTaskId()) && StrUtil.isNotBlank(s.getEvidenceHash())
                            && "VALID".equals(s.getEvidenceStatus()))
                    .map(DccControlledFileSignatureDO::getActorId).filter(required::contains)
                    .collect(Collectors.toSet());
            if (Boolean.TRUE.equals(stage.getRequireAllApprovals()) || "ALL".equals(stage.getApproveMethod())) {
                if (!signed.containsAll(required)) throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
            } else if ("ANY".equals(stage.getApproveMethod())) {
                if (signed.isEmpty()) throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
            } else {
                throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
            }
        }
    }
}
