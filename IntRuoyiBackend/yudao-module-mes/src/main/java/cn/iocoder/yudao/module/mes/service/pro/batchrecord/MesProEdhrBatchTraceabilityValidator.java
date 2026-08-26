package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityBlocker.ACTIVE_ORDER_SOURCE_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityBlocker.BATCH_PROVISION_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityBlocker.ENTRY_SCENARIO_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityBlocker.ENTRY_TYPE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityBlocker.ENTRY_TYPE_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityBlocker.INDEPENDENT_CREDENTIAL_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityBlocker.TRACE_IDEMPOTENCY_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityBlocker.TRACE_SOURCE_CONFLICT;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityBlocker.TRACE_SOURCE_REQUIRED;

public class MesProEdhrBatchTraceabilityValidator {

    private static final Set<String> ENTRY_TYPES = Set.of(
            MesProEdhrBatchTraceEntryType.ACTIVE_ORDER_COMPLETION,
            MesProEdhrBatchTraceEntryType.PQC_INDEPENDENT,
            MesProEdhrBatchTraceEntryType.MANUAL,
            MesProEdhrBatchTraceEntryType.SCHEDULED);
    private static final Set<String> PROVISION_STATUSES = Set.of(
            MesProEdhrBatchTraceProvisionStatus.CREATED,
            MesProEdhrBatchTraceProvisionStatus.REUSED,
            MesProEdhrBatchTraceProvisionStatus.BATCH_PROVISIONING);
    private static final Set<String> ACTIVE_ORDER_REQUIRED_LINK_TYPES = Set.of(
            MesProEdhrBatchTraceLinkType.ACTIVE_ORDER,
            MesProEdhrBatchTraceLinkType.WORK_ORDER,
            MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE,
            MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE_LINE,
            MesProEdhrBatchTraceLinkType.PRODUCTION_SUBMIT,
            MesProEdhrBatchTraceLinkType.PRODUCTION_SIGNATURE,
            MesProEdhrBatchTraceLinkType.PRODUCTION_LEADER_REVIEW,
            MesProEdhrBatchTraceLinkType.PQC_TASK,
            MesProEdhrBatchTraceLinkType.PQC_SUBMISSION,
            MesProEdhrBatchTraceLinkType.PQC_SIGNATURE,
            MesProEdhrBatchTraceLinkType.PQC_LEADER_CONFIRMATION,
            MesProEdhrBatchTraceLinkType.PQC_AGGREGATE_DETAIL,
            MesProEdhrBatchTraceLinkType.BATCH_RECORD_RECEIPT,
            MesProEdhrBatchTraceLinkType.PROCESS_INSPECTION_RECEIPT,
            MesProEdhrBatchTraceLinkType.COMPLETION_BACKFILL_RECEIPT,
            MesProEdhrBatchTraceLinkType.BATCH_PROVISION_RECEIPT);

    public MesProEdhrBatchTraceValidationResult validate(MesProEdhrBatchTraceCaptureCommand command) {
        if (command == null || command.getBatchExecutionId() == null
                || command.getBatchProvisionReceiptId() == null
                || !PROVISION_STATUSES.contains(command.getBatchProvisionStatus())) {
            return MesProEdhrBatchTraceValidationResult.blocked(BATCH_PROVISION_REQUIRED, "batch-provision");
        }
        if (isBlank(command.getEntryType())) {
            return MesProEdhrBatchTraceValidationResult.blocked(ENTRY_TYPE_REQUIRED, "entry");
        }
        if (!ENTRY_TYPES.contains(command.getEntryType())) {
            return MesProEdhrBatchTraceValidationResult.blocked(ENTRY_TYPE_INVALID, "entry");
        }
        if (isBlank(command.getOriginKey()) || isBlank(command.getSourceSnapshotHash())
                || isBlank(command.getSourceBundleHash())
                || isBlank(command.getIdempotencyKey())) {
            return MesProEdhrBatchTraceValidationResult.blocked(TRACE_IDEMPOTENCY_REQUIRED, "origin");
        }
        if (command.getReleaseApplicationId() != null) {
            return MesProEdhrBatchTraceValidationResult.blocked(ENTRY_SCENARIO_MISMATCH,
                    "release-decision-must-be-appended-after-capture");
        }
        if (MesProEdhrBatchTraceEntryType.ACTIVE_ORDER_COMPLETION.equals(command.getEntryType())) {
            if (command.getActiveOrderId() == null || command.getWorkOrderId() == null
                    || command.getCompletionTransactionId() == null
                    || command.getCompletionVersion() == null || command.getCompletionVersion() <= 0
                    || command.getCompletionBackfillReceiptId() == null
                    || isBlank(command.getCompletionBackfillReceiptHash())
                    || command.getPickListBindingId() == null || command.getPickListId() == null
                    || command.getPickListBindingVersion() == null || command.getPickListBindingVersion() <= 0
                    || command.getHasActualLoss() == null || isBlank(command.getSourceSnapshotHash())) {
                return MesProEdhrBatchTraceValidationResult.blocked(ACTIVE_ORDER_SOURCE_REQUIRED, "active-order");
            }
            if (command.getSourceCredentialId() != null) {
                return MesProEdhrBatchTraceValidationResult.blocked(ENTRY_SCENARIO_MISMATCH, "active-order");
            }
        } else {
            if (command.getWorkOrderId() == null || command.getActiveOrderId() != null
                    || command.getCompletionTransactionId() != null
                    || command.getCompletionBackfillReceiptId() != null || command.getPickListBindingId() != null) {
                return MesProEdhrBatchTraceValidationResult.blocked(ENTRY_SCENARIO_MISMATCH, "entry");
            }
            if (command.getSourceCredentialId() == null || isBlank(command.getSourceCredentialHash())) {
                return MesProEdhrBatchTraceValidationResult.blocked(INDEPENDENT_CREDENTIAL_REQUIRED, "credential");
            }
        }
        MesProEdhrBatchTraceValidationResult sourcesResult = validateSources(command.getSources());
        if (!sourcesResult.valid()) {
            return sourcesResult;
        }
        if (!calculateSourceBundleHash(command.getSources()).equalsIgnoreCase(command.getSourceBundleHash())) {
            return MesProEdhrBatchTraceValidationResult.blocked(TRACE_SOURCE_CONFLICT, "source-bundle-hash");
        }
        MesProEdhrBatchTraceValidationResult coverageResult = validateFormalCoverage(command);
        if (!coverageResult.valid()) {
            return coverageResult;
        }
        MesProEdhrBatchTraceValidationResult snapshotResult = validateSourceSnapshotBinding(command);
        if (!snapshotResult.valid()) {
            return snapshotResult;
        }
        MesProEdhrBatchTraceValidationResult identityResult = validateFormalSourceBindings(command);
        if (!identityResult.valid()) {
            return identityResult;
        }
        return MesProEdhrBatchTraceValidationResult.ok();
    }

    private MesProEdhrBatchTraceValidationResult validateSourceSnapshotBinding(
            MesProEdhrBatchTraceCaptureCommand command) {
        if (!MesProEdhrBatchTraceEntryType.ACTIVE_ORDER_COMPLETION.equals(command.getEntryType())) {
            return MesProEdhrBatchTraceValidationResult.ok();
        }
        List<MesProEdhrBatchTraceSource> pickListSources = command.getSources().stream()
                .filter(source -> MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE.equals(source.getLinkType())
                        || MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE_LINE.equals(source.getLinkType()))
                .toList();
        if (pickListSources.isEmpty() || pickListSources.stream()
                .anyMatch(source -> !command.getSourceSnapshotHash().equalsIgnoreCase(source.getSnapshotHash()))) {
            return MesProEdhrBatchTraceValidationResult.blocked(TRACE_SOURCE_CONFLICT,
                    "pick-list-source-snapshot-hash");
        }
        return MesProEdhrBatchTraceValidationResult.ok();
    }

    private MesProEdhrBatchTraceValidationResult validateFormalSourceBindings(
            MesProEdhrBatchTraceCaptureCommand command) {
        if (MesProEdhrBatchTraceEntryType.ACTIVE_ORDER_COMPLETION.equals(command.getEntryType())) {
            if (!hasExactlyOneSourceId(command.getSources(), MesProEdhrBatchTraceLinkType.ACTIVE_ORDER,
                    command.getActiveOrderId())
                    || !hasExactlyOneSourceId(command.getSources(), MesProEdhrBatchTraceLinkType.WORK_ORDER,
                    command.getWorkOrderId())
                    || !hasExactlyOneSourceId(command.getSources(), MesProEdhrBatchTraceLinkType.MATERIAL_ISSUE,
                    command.getPickListId())
                    || !hasExactlyOneSourceIdAndHash(command.getSources(),
                    MesProEdhrBatchTraceLinkType.COMPLETION_BACKFILL_RECEIPT,
                    command.getCompletionBackfillReceiptId(), command.getCompletionBackfillReceiptHash())
                    || !hasExactlyOneSourceIdAndHash(command.getSources(),
                    MesProEdhrBatchTraceLinkType.BATCH_PROVISION_RECEIPT,
                    command.getBatchProvisionReceiptId(), command.getSourceSnapshotHash())) {
                return MesProEdhrBatchTraceValidationResult.blocked(TRACE_SOURCE_CONFLICT,
                        "formal-source-command-identity");
            }
            return MesProEdhrBatchTraceValidationResult.ok();
        }
        if (!hasExactlyOneSourceIdAndHash(command.getSources(),
                MesProEdhrBatchTraceLinkType.BATCH_PROVISION_RECEIPT,
                command.getBatchProvisionReceiptId(), command.getSourceSnapshotHash())) {
            return MesProEdhrBatchTraceValidationResult.blocked(TRACE_SOURCE_CONFLICT,
                    "batch-provision-source-command-identity");
        }
        if (!hasExactlyOneSourceId(command.getSources(), MesProEdhrBatchTraceLinkType.WORK_ORDER,
                command.getWorkOrderId())) {
            return MesProEdhrBatchTraceValidationResult.blocked(TRACE_SOURCE_CONFLICT,
                    "work-order-source-command-identity");
        }
        return MesProEdhrBatchTraceValidationResult.ok();
    }

    private boolean hasExactlyOneSourceId(List<MesProEdhrBatchTraceSource> sources, String linkType, Long sourceId) {
        List<MesProEdhrBatchTraceSource> matches = sources.stream()
                .filter(source -> linkType.equals(source.getLinkType()))
                .toList();
        return matches.size() == 1 && Objects.equals(matches.get(0).getSourceObjectId(), sourceId);
    }

    private boolean hasExactlyOneSourceIdAndHash(List<MesProEdhrBatchTraceSource> sources, String linkType,
                                                 Long sourceId, String snapshotHash) {
        List<MesProEdhrBatchTraceSource> matches = sources.stream()
                .filter(source -> linkType.equals(source.getLinkType()))
                .toList();
        return matches.size() == 1 && Objects.equals(matches.get(0).getSourceObjectId(), sourceId)
                && snapshotHash != null && snapshotHash.equalsIgnoreCase(matches.get(0).getSnapshotHash());
    }

    private MesProEdhrBatchTraceValidationResult validateFormalCoverage(
            MesProEdhrBatchTraceCaptureCommand command) {
        Set<String> linkTypes = command.getSources().stream()
                .map(MesProEdhrBatchTraceSource::getLinkType)
                .collect(Collectors.toSet());
        if (!linkTypes.containsAll(requiredLinkTypesFor(command.getEntryType()))) {
                return MesProEdhrBatchTraceValidationResult.blocked(TRACE_SOURCE_REQUIRED,
                        "formal-sources");
        }
        if (MesProEdhrBatchTraceEntryType.ACTIVE_ORDER_COMPLETION.equals(command.getEntryType())) {
            boolean hasLossReport = linkTypes.contains(MesProEdhrBatchTraceLinkType.LOSS_REPORT_RECEIPT);
            boolean hasLossFact = linkTypes.contains(MesProEdhrBatchTraceLinkType.LOSS_FACT);
            boolean hasNoLossFact = linkTypes.contains(MesProEdhrBatchTraceLinkType.NO_LOSS_CONFIRMED);
            if (Boolean.TRUE.equals(command.getHasActualLoss())) {
                if (!hasLossFact || !hasLossReport || hasNoLossFact
                        || !hasRelationStatus(command.getSources(), MesProEdhrBatchTraceLinkType.LOSS_FACT,
                        "HAS_LOSS")) {
                    return MesProEdhrBatchTraceValidationResult.blocked(TRACE_SOURCE_REQUIRED, "loss-report-receipt");
                }
            }
            if (Boolean.FALSE.equals(command.getHasActualLoss())) {
                if (!hasNoLossFact || hasLossFact || hasLossReport
                        || !hasRelationStatus(command.getSources(), MesProEdhrBatchTraceLinkType.NO_LOSS_CONFIRMED,
                        "NO_LOSS", "NO_LOSS_CONFIRMED")) {
                    return MesProEdhrBatchTraceValidationResult.blocked(ENTRY_SCENARIO_MISMATCH, "no-loss");
                }
            }
        }
        return MesProEdhrBatchTraceValidationResult.ok();
    }

    private boolean hasRelationStatus(List<MesProEdhrBatchTraceSource> sources, String linkType,
                                      String... statuses) {
        Set<String> allowed = Set.of(statuses);
        return sources.stream().filter(source -> linkType.equals(source.getLinkType()))
                .anyMatch(source -> allowed.contains(source.getRelationStatus()));
    }

    static Set<String> requiredLinkTypesFor(String entryType) {
        if (MesProEdhrBatchTraceEntryType.ACTIVE_ORDER_COMPLETION.equals(entryType)) {
            return ACTIVE_ORDER_REQUIRED_LINK_TYPES;
        }
        if (!ENTRY_TYPES.contains(entryType)) {
            return Set.of("__INVALID_ENTRY_TYPE__");
        }
        return Set.of(MesProEdhrBatchTraceLinkType.WORK_ORDER,
                MesProEdhrBatchTraceLinkType.BATCH_PROVISION_RECEIPT);
    }

    private MesProEdhrBatchTraceValidationResult validateSources(List<MesProEdhrBatchTraceSource> sources) {
        if (sources == null || sources.isEmpty()) {
            return MesProEdhrBatchTraceValidationResult.blocked(TRACE_SOURCE_REQUIRED, "sources");
        }
        Set<String> sourceIdentities = new HashSet<>();
        for (MesProEdhrBatchTraceSource source : sources) {
            if (source == null || isBlank(source.getLinkType()) || isBlank(source.getSourceObjectType())
                    || (source.getSourceObjectId() == null && source.getSourceLineId() == null
                    && source.getSourceEventId() == null) || isBlank(source.getSnapshotJson())
                    || isBlank(source.getSnapshotHash())) {
                return MesProEdhrBatchTraceValidationResult.blocked(TRACE_SOURCE_REQUIRED, "source");
            }
            String canonicalIdentity = identityOf(source);
            if (!isBlank(source.getSourceIdentityKey())
                    && !canonicalIdentity.equals(source.getSourceIdentityKey())) {
                return MesProEdhrBatchTraceValidationResult.blocked(TRACE_SOURCE_CONFLICT,
                        "source-identity-key");
            }
            if (!sourceIdentities.add(canonicalIdentity)) {
                return MesProEdhrBatchTraceValidationResult.blocked(TRACE_SOURCE_CONFLICT, "source-identity");
            }
            if (MesProEdhrBatchTraceLinkType.RELEASE_DECISION.equals(source.getLinkType())) {
                return MesProEdhrBatchTraceValidationResult.blocked(TRACE_SOURCE_CONFLICT,
                        "release-decision-must-be-appended-after-capture");
            }
            String calculatedHash = MesProEdhrBatchTraceSourceHash.calculate(
                    source.getLinkType(), source.getSnapshotJson());
            if (!calculatedHash.equalsIgnoreCase(source.getSnapshotHash())) {
                return MesProEdhrBatchTraceValidationResult.blocked(TRACE_SOURCE_CONFLICT, "source-hash");
            }
        }
        return MesProEdhrBatchTraceValidationResult.ok();
    }

    public String calculateSourceBundleHash(List<MesProEdhrBatchTraceSource> sources) {
        List<Map<String, Object>> entries = new ArrayList<>();
        for (MesProEdhrBatchTraceSource source : sources) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("linkType", source.getLinkType());
            entry.put("sourceObjectType", source.getSourceObjectType());
            entry.put("sourceObjectId", source.getSourceObjectId());
            entry.put("sourceLineId", source.getSourceLineId());
            entry.put("sourceEventId", source.getSourceEventId());
            entry.put("sourceVersion", source.getSourceVersion());
            entry.put("sourceIdentityKey", identityOf(source));
            entry.put("snapshotHash", source.getSnapshotHash());
            entry.put("relationStatus", source.getRelationStatus());
            entry.put("relationReason", source.getRelationReason());
            entries.add(entry);
        }
        entries.sort(Comparator.comparing(entry -> String.valueOf(entry.get("sourceIdentityKey"))));
        String bundleJson = JsonUtils.toJsonString(entries);
        return DigestUtil.sha256Hex(MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(bundleJson));
    }

    private String identityOf(MesProEdhrBatchTraceSource source) {
        return String.join(":", Objects.toString(source.getLinkType(), ""),
                Objects.toString(source.getSourceObjectType(), ""), Objects.toString(source.getSourceObjectId(), ""),
                Objects.toString(source.getSourceLineId(), ""), Objects.toString(source.getSourceEventId(), ""));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
