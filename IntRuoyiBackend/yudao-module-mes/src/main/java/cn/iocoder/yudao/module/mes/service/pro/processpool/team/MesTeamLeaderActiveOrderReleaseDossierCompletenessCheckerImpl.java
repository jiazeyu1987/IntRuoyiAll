package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class MesTeamLeaderActiveOrderReleaseDossierCompletenessCheckerImpl
        implements MesTeamLeaderActiveOrderReleaseDossierCompletenessChecker {

    private static final List<String> REQUIRED_DOCUMENT_TYPES =
            List.of("BATCH_RECORD", "PROCESS_INSPECTION", "LOSS_REPORT");

    @Override
    public MesTeamLeaderActiveOrderReleaseDossierCompletenessResult check(
            MesTeamLeaderActiveOrderReleaseDossierCompletenessCommand command) {
        List<MesTeamLeaderActiveOrderReleaseBlocker> blockers = new ArrayList<>();
        if (command == null || command.getBatchExecutionId() == null || command.getBatchExecutionId() <= 0
                || StrUtil.isBlank(command.getSourceSnapshotHash())) {
            blockers.add(blocker("DOSSIER_COMPLETENESS_BLOCKED", "DOSSIER", null,
                    "放行 dossier 缺少当前批次或来源快照", "请使用当前批次正式来源重新生成 dossier"));
            return result(blockers);
        }
        List<MesTeamLeaderActiveOrderReleaseDocumentEvidence> documents = command.getDocuments() == null
                ? List.of() : command.getDocuments().stream().filter(Objects::nonNull).toList();
        for (String documentType : REQUIRED_DOCUMENT_TYPES) {
            List<MesTeamLeaderActiveOrderReleaseDocumentEvidence> matches = documents.stream()
                    .filter(document -> documentType.equals(document.getDocumentType()))
                    .toList();
            if (matches.isEmpty()) {
                blockers.add(blocker("DOSSIER_COMPLETENESS_BLOCKED", documentType, null,
                        "缺少该类正式文档证据",
                        "请完成当前批次正式文档及字段审计"));
                continue;
            }
            for (MesTeamLeaderActiveOrderReleaseDocumentEvidence document : matches) {
                validateDocument(command, document, blockers);
            }
        }
        if (command.getReleaseApprovalRuleId() == null
                || command.getReleaseOwnerCandidateUserIds() == null
                || command.getReleaseOwnerCandidateUserIds().stream().filter(Objects::nonNull).findAny().isEmpty()) {
            blockers.add(blocker("RELEASE_OWNER_REQUIRED", "RELEASE_OWNER", command.getReleaseApprovalRuleId(),
                    "正式放行审批规则或候选责任人缺失", "请配置正式 RELEASE_APPROVE 审批责任人"));
        }
        return result(blockers);
    }

    private void validateDocument(
            MesTeamLeaderActiveOrderReleaseDossierCompletenessCommand command,
            MesTeamLeaderActiveOrderReleaseDocumentEvidence document,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        boolean executionComplete = Objects.equals(command.getBatchExecutionId(), document.getBatchExecutionId())
                && document.getBatchExecutionTaskId() != null
                && nonEmpty(document.getBatchRecordExecutionIds())
                && nonEmpty(document.getTargetReportIds())
                && nonEmpty(document.getTargetDefinitionIds())
                && nonEmpty(document.getTargetVersionIds())
                && nonBlank(document.getTargetSnapshotHashes())
                && nonEmpty(document.getFieldAuditIds())
                && document.getRequiredFieldCount() > 0
                && document.getAuditedRequiredFieldCount() == document.getRequiredFieldCount();
        boolean signaturesComplete = validSignatures(document.getSignatureEvidence());
        if (!executionComplete || !signaturesComplete) {
            blockers.add(blocker("DOSSIER_COMPLETENESS_BLOCKED", document.getDocumentType(),
                    document.getBatchExecutionTaskId(),
                    "正式 execution、任务、模板、字段审计或双签证据不完整",
                    "请补齐当前批次正式文档执行和字段级审计"));
        }
        boolean sourceComplete = document.isSourceConsistent()
                && Objects.equals(command.getSourceSnapshotHash(), document.getSourceSnapshotHash())
                && nonEmpty(document.getSourceObjectIds())
                && nonBlank(document.getSourceValueHashes());
        if (!sourceComplete) {
            blockers.add(blocker("LOSS_REPORT".equals(document.getDocumentType())
                            ? "LOSS_SOURCE_REQUIRED" : "DOSSIER_COMPLETENESS_BLOCKED",
                    document.getDocumentType(), document.getBatchExecutionTaskId(),
                    "文档来源对象、来源值哈希或当前快照一致性不完整",
                    "请基于当前激活批次正式来源重新生成文档"));
        }
    }

    private boolean validSignatures(List<MesTeamLeaderActiveOrderReleaseSignatureEvidence> evidence) {
        if (evidence == null) {
            return false;
        }
        Set<String> roles = new LinkedHashSet<>();
        for (MesTeamLeaderActiveOrderReleaseSignatureEvidence item : evidence) {
            if (item == null || StrUtil.isBlank(item.getRole()) || StrUtil.isBlank(item.getSourceType())
                    || item.getSourceId() == null || item.getSignatureId() == null || item.getUserId() == null
                    || item.getSignedAt() == null || StrUtil.isBlank(item.getEvidenceHash())) {
                return false;
            }
            roles.add(item.getRole());
        }
        return roles.contains("FILLER") && roles.contains("REVIEWER");
    }

    private boolean nonEmpty(List<?> values) {
        return values != null && !values.isEmpty() && values.stream().noneMatch(Objects::isNull);
    }

    private boolean nonBlank(List<String> values) {
        return values != null && !values.isEmpty() && values.stream().allMatch(StrUtil::isNotBlank);
    }

    private MesTeamLeaderActiveOrderReleaseDossierCompletenessResult result(
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        return new MesTeamLeaderActiveOrderReleaseDossierCompletenessResult()
                .setComplete(blockers.isEmpty())
                .setBlockers(List.copyOf(blockers));
    }

    private MesTeamLeaderActiveOrderReleaseBlocker blocker(
            String type, String objectType, Object objectId, String reason, String suggestion) {
        return new MesTeamLeaderActiveOrderReleaseBlocker()
                .setBlockerType(type)
                .setObjectType(objectType)
                .setObjectId(objectId == null ? null : String.valueOf(objectId))
                .setReason(reason)
                .setSuggestion(suggestion);
    }
}
