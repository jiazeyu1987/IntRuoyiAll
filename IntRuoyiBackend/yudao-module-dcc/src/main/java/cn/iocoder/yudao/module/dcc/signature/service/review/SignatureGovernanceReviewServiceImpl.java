package cn.iocoder.yudao.module.dcc.signature.service.review;

import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SignatureGovernanceReviewServiceImpl implements SignatureGovernanceReviewService {

    @Override
    public SignatureGovernanceReviewBatchEvaluation createBatch(SignatureGovernanceReviewBatchCommand command) {
        return evaluateBatch(command);
    }

    @Override
    public SignatureGovernanceReviewBatchEvaluation evaluateBatch(SignatureGovernanceReviewBatchCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Signature review batch command is required");
        }
        BlockerCollector blockers = new BlockerCollector();
        validateBatchPrerequisites(command, blockers);
        if (!blockers.isEmpty()) {
            return SignatureGovernanceReviewBatchEvaluation.blocked(blockers.values());
        }

        List<SignatureGovernanceReviewSnapshotItem> snapshotItems = command.projections().stream()
                .filter(projection -> command.scopeModules().contains(projection.moduleCode()))
                .map(this::toSnapshotItem)
                .toList();
        String snapshotHash = snapshotHash(snapshotItems);
        return SignatureGovernanceReviewBatchEvaluation.collected("review-" + snapshotHash.substring(0, 12),
                snapshotHash, snapshotItems);
    }

    @Override
    public SignatureGovernanceReviewClosureResult signReview(SignatureGovernanceReviewClosureCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Signature review closure command is required");
        }
        BlockerCollector blockers = new BlockerCollector();
        validateSnapshot(command, blockers);
        validateReviewSignatureStrategy(command, blockers);
        validateRemediations(command, blockers);
        validateRequiredRemediation(command, blockers);
        if (!blockers.isEmpty()) {
            return SignatureGovernanceReviewClosureResult.blocked(blockers.values());
        }
        return SignatureGovernanceReviewClosureResult.signedResult();
    }

    @Override
    public SignatureGovernanceReviewClosureResult closeBatch(SignatureGovernanceReviewClosureCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Signature review closure command is required");
        }
        BlockerCollector blockers = new BlockerCollector();
        validateSnapshot(command, blockers);
        validateReviewSignatureStrategy(command, blockers);
        if (!command.reviewSigned()) {
            blockers.add(SignatureGovernanceReviewBlockerCode.REVIEW_SIGNATURE_MISSING,
                    "Review signature is missing",
                    "The periodic review batch cannot close until the quality review signature is recorded");
        }
        validateRemediations(command, blockers);
        validateRequiredRemediation(command, blockers);
        if (!blockers.isEmpty()) {
            return SignatureGovernanceReviewClosureResult.blocked(blockers.values());
        }
        return SignatureGovernanceReviewClosureResult.closedResult();
    }

    private static void validateBatchPrerequisites(SignatureGovernanceReviewBatchCommand command,
                                                   BlockerCollector blockers) {
        if (command.reviewOwner() == null) {
            blockers.add(SignatureGovernanceReviewBlockerCode.REVIEW_OWNER_MISSING,
                    "Review owner is missing",
                    "The periodic review batch cannot be created without a quality owner");
        }
        if (command.periodCode() == null || command.ruleVersion() == null || command.dueDate() == null) {
            blockers.add(SignatureGovernanceReviewBlockerCode.PERIOD_RULE_MISSING,
                    "Review period, due date, or rule version is missing",
                    "The batch cannot freeze a defensible review scope without period rules");
        }
        if (command.scopeModules() == null || command.scopeModules().isEmpty()) {
            blockers.add(SignatureGovernanceReviewBlockerCode.DATA_SOURCE_MISSING,
                    "Review data source scope is missing",
                    "The batch cannot collect DCC, eDHR, Showroom, or IntAuth signature facts");
        } else if (command.permittedModules() == null || !command.permittedModules().containsAll(command.scopeModules())) {
            blockers.add(SignatureGovernanceReviewBlockerCode.DATA_SOURCE_PERMISSION_MISSING,
                    "Review data source permission is missing",
                    "The batch cannot collect source records that the reviewer is not permitted to read");
        }
        if (command.scopeModules() != null && !command.scopeModules().isEmpty()
                && missingProjectionModules(command.scopeModules(), command.projections()).size() > 0) {
            blockers.add(SignatureGovernanceReviewBlockerCode.SAMPLE_PROJECTION_MISSING,
                    "Required source projection sample is missing",
                    "The batch cannot mark missing DCC, eDHR, Showroom, or IntAuth samples as compliant");
        }
        if (!command.reviewSignatureStrategyConfigured()) {
            blockers.add(SignatureGovernanceReviewBlockerCode.REVIEW_SIGNATURE_STRATEGY_MISSING,
                    "Review signature strategy is missing",
                    "The batch must remain blocked until the quality review signature policy is confirmed");
        }
    }

    private static Set<SignatureGovernanceModuleCode> missingProjectionModules(
            Set<SignatureGovernanceModuleCode> scopeModules,
            List<SignatureGovernanceReviewSourceProjection> projections) {
        EnumSet<SignatureGovernanceModuleCode> missing = EnumSet.copyOf(scopeModules);
        if (projections != null) {
            projections.stream().map(SignatureGovernanceReviewSourceProjection::moduleCode).forEach(missing::remove);
        }
        return missing;
    }

    private SignatureGovernanceReviewSnapshotItem toSnapshotItem(SignatureGovernanceReviewSourceProjection projection) {
        return new SignatureGovernanceReviewSnapshotItem(projection.moduleCode(), projection.sourceTable(),
                projection.sourceId(), projection.sourceHash(), projection.actionCode(), projection.meaningCode(),
                projection.findingCode());
    }

    private static void validateSnapshot(SignatureGovernanceReviewClosureCommand command, BlockerCollector blockers) {
        if (command.batchId() == null || command.snapshotHash() == null || command.snapshotItems().isEmpty()) {
            blockers.add(SignatureGovernanceReviewBlockerCode.SNAPSHOT_MISSING,
                    "Review batch snapshot is missing",
                    "The review must close against the frozen snapshot rather than a live source query");
        }
    }

    private static void validateReviewSignatureStrategy(SignatureGovernanceReviewClosureCommand command,
                                                        BlockerCollector blockers) {
        if (!command.reviewSignatureStrategyConfigured()) {
            blockers.add(SignatureGovernanceReviewBlockerCode.REVIEW_SIGNATURE_STRATEGY_MISSING,
                    "Review signature strategy is missing",
                    "The review signature and close action must remain blocked until the strategy is confirmed");
        }
    }

    private static void validateRemediations(SignatureGovernanceReviewClosureCommand command,
                                             BlockerCollector blockers) {
        for (SignatureGovernanceReviewRemediation remediation : command.remediations()) {
            if (SignatureGovernanceReviewRemediationStatus.OPEN.equals(remediation.status())) {
                blockers.add(SignatureGovernanceReviewBlockerCode.OPEN_REMEDIATION,
                        "Open remediation exists: " + remediation.sourceRef(),
                        "The review cannot be signed or closed while remediation is open");
            } else if (SignatureGovernanceReviewRemediationStatus.OVERDUE.equals(remediation.status())) {
                blockers.add(SignatureGovernanceReviewBlockerCode.OVERDUE_REMEDIATION,
                        "Overdue remediation exists: " + remediation.sourceRef(),
                        "The review cannot be signed or closed while remediation is overdue");
            } else if (SignatureGovernanceReviewRemediationStatus.PENDING_REVIEW.equals(remediation.status())) {
                blockers.add(SignatureGovernanceReviewBlockerCode.PENDING_REMEDIATION_REVIEW,
                        "Remediation is pending quality review: " + remediation.sourceRef(),
                        "The review cannot be signed or closed until remediation review is complete");
            }
        }
    }

    private static void validateRequiredRemediation(SignatureGovernanceReviewClosureCommand command,
                                                    BlockerCollector blockers) {
        Map<String, SignatureGovernanceReviewRemediation> remediationBySourceRef = command.remediations().stream()
                .collect(Collectors.toMap(SignatureGovernanceReviewRemediation::sourceRef, Function.identity(),
                        (left, right) -> left));
        for (SignatureGovernanceReviewSnapshotItem item : command.snapshotItems()) {
            if (!item.findingCode().isRemediationRequired()) {
                continue;
            }
            SignatureGovernanceReviewRemediation remediation = remediationBySourceRef.get(item.sourceRef());
            if (remediation == null) {
                blockers.add(SignatureGovernanceReviewBlockerCode.REQUIRED_REMEDIATION_MISSING,
                        "Required remediation or approved exception is missing: " + item.sourceRef(),
                        "Abnormal signature evidence cannot be closed as compliant by default");
            }
        }
    }

    private static String snapshotHash(List<SignatureGovernanceReviewSnapshotItem> snapshotItems) {
        String canonicalSnapshot = snapshotItems.stream()
                .sorted(Comparator.comparing(SignatureGovernanceReviewServiceImpl::canonicalItem))
                .map(SignatureGovernanceReviewServiceImpl::canonicalItem)
                .collect(Collectors.joining("\n"));
        return sha256(canonicalSnapshot);
    }

    private static String canonicalItem(SignatureGovernanceReviewSnapshotItem item) {
        return item.moduleCode().name() + "|" + item.sourceTable() + "|" + item.sourceId() + "|"
                + item.sourceHash() + "|" + item.actionCode() + "|" + item.meaningCode() + "|"
                + item.findingCode().name();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is required for signature review snapshots", ex);
        }
    }

    private static final class BlockerCollector {

        private final Map<SignatureGovernanceReviewBlockerCode, SignatureGovernanceReviewBlocker> blockers =
                new LinkedHashMap<>();

        void add(SignatureGovernanceReviewBlockerCode code, String message, String impact) {
            blockers.putIfAbsent(code, SignatureGovernanceReviewBlocker.of(code, message, impact));
        }

        boolean isEmpty() {
            return blockers.isEmpty();
        }

        List<SignatureGovernanceReviewBlocker> values() {
            return List.copyOf(blockers.values());
        }
    }
}
