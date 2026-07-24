package cn.iocoder.yudao.module.dcc.signature.service.csv;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SignatureGovernanceCsvServiceImpl implements SignatureGovernanceCsvService {

    @Override
    public SignatureGovernanceCsvPackageResult evaluatePackage(SignatureGovernanceCsvPackageCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("CSV package command is required");
        }
        List<SignatureGovernanceCsvBlocker> blockers = new ArrayList<>();
        if (isBlank(command.qualityOwner())) {
            blockers.add(blocker(SignatureGovernanceCsvBlockerCode.QUALITY_OWNER_MISSING));
        }
        EnumSet<SignatureGovernanceCsvMaterialType> approvedMaterialTypes = validateMaterials(command, blockers);
        if (command.traceRelations().isEmpty()
                || command.traceRelations().stream().anyMatch(relation -> relation == null || !relation.isComplete())) {
            blockers.add(blocker(SignatureGovernanceCsvBlockerCode.TRACE_RELATION_MISSING));
        }
        if (command.trainingRecords().isEmpty()
                || command.trainingRecords().stream().noneMatch(record -> record != null && record.isEffective())) {
            blockers.add(blocker(SignatureGovernanceCsvBlockerCode.TRAINING_RECORD_MISSING));
        }
        if (command.changeControls().isEmpty()
                || command.changeControls().stream().noneMatch(change -> change != null && change.isApproved())) {
            blockers.add(blocker(SignatureGovernanceCsvBlockerCode.CHANGE_CONTROL_MISSING));
        }
        boolean qaApproved = command.qaApproval() != null && command.qaApproval().isApproved();
        if (!qaApproved) {
            blockers.add(blocker(SignatureGovernanceCsvBlockerCode.QA_APPROVAL_MISSING));
        }
        if (isBlank(command.recoveryEvidenceRef())) {
            blockers.add(blocker(SignatureGovernanceCsvBlockerCode.RECOVERY_EVIDENCE_MISSING));
        }
        SignatureGovernanceCsvPackageStatus status = blockers.isEmpty()
                ? SignatureGovernanceCsvPackageStatus.READY : SignatureGovernanceCsvPackageStatus.BLOCKED;
        return new SignatureGovernanceCsvPackageResult(command.packageId(), status, approvedMaterialTypes,
                command.traceRelations(), command.engineeringVerificationPassed(), qaApproved, blockers);
    }

    @Override
    public SignatureGovernanceCsvReleaseGateResult evaluateReleaseGate(SignatureGovernanceCsvReleaseGateCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("CSV release gate command is required");
        }
        SignatureGovernanceCsvPackageResult packageResult = evaluatePackage(command.packageCommand());
        SignatureGovernanceCsvReleaseGateStatus status = packageResult.blockers().isEmpty()
                ? SignatureGovernanceCsvReleaseGateStatus.GO : SignatureGovernanceCsvReleaseGateStatus.BLOCKED;
        return new SignatureGovernanceCsvReleaseGateResult(command.releaseId(), status, packageResult,
                packageResult.engineeringVerificationPassed(), packageResult.qaApproved(), packageResult.blockers());
    }

    private static EnumSet<SignatureGovernanceCsvMaterialType> validateMaterials(
            SignatureGovernanceCsvPackageCommand command, List<SignatureGovernanceCsvBlocker> blockers) {
        EnumSet<SignatureGovernanceCsvMaterialType> approvedMaterialTypes = EnumSet.noneOf(
                SignatureGovernanceCsvMaterialType.class);
        Map<SignatureGovernanceCsvMaterialType, SignatureGovernanceCsvMaterial> materialByType =
                command.materials().stream()
                        .filter(material -> material != null)
                        .collect(Collectors.toMap(SignatureGovernanceCsvMaterial::type, Function.identity(),
                                (existing, ignored) -> existing));
        for (SignatureGovernanceCsvMaterialType requiredType : SignatureGovernanceCsvMaterialType.requiredMaterials()) {
            SignatureGovernanceCsvMaterial material = materialByType.get(requiredType);
            if (material == null) {
                blockers.add(blocker(missingMaterialCode(requiredType)));
                continue;
            }
            if (!material.isComplete()) {
                blockers.add(blocker(SignatureGovernanceCsvBlockerCode.QUALITY_MATERIAL_INCOMPLETE));
                continue;
            }
            if (!material.isApproved()) {
                blockers.add(blocker(SignatureGovernanceCsvBlockerCode.QUALITY_MATERIAL_NOT_APPROVED));
                continue;
            }
            approvedMaterialTypes.add(requiredType);
        }
        return approvedMaterialTypes;
    }

    private static SignatureGovernanceCsvBlockerCode missingMaterialCode(SignatureGovernanceCsvMaterialType type) {
        return switch (type) {
            case URS -> SignatureGovernanceCsvBlockerCode.URS_MISSING;
            case FRS -> SignatureGovernanceCsvBlockerCode.FRS_MISSING;
            case RISK_ASSESSMENT -> SignatureGovernanceCsvBlockerCode.RISK_ASSESSMENT_MISSING;
            case IQ -> SignatureGovernanceCsvBlockerCode.IQ_MISSING;
            case OQ -> SignatureGovernanceCsvBlockerCode.OQ_MISSING;
            case PQ -> SignatureGovernanceCsvBlockerCode.PQ_MISSING;
            case TRACE_MATRIX -> SignatureGovernanceCsvBlockerCode.TRACE_MATRIX_MISSING;
            case ELECTRONIC_SIGNATURE_SOP -> SignatureGovernanceCsvBlockerCode.ELECTRONIC_SIGNATURE_SOP_MISSING;
            case EVIDENCE_INDEX -> SignatureGovernanceCsvBlockerCode.EVIDENCE_INDEX_MISSING;
        };
    }

    private static SignatureGovernanceCsvBlocker blocker(SignatureGovernanceCsvBlockerCode code) {
        return SignatureGovernanceCsvBlocker.of(code, message(code), impact(code));
    }

    private static String message(SignatureGovernanceCsvBlockerCode code) {
        return switch (code) {
            case QUALITY_OWNER_MISSING -> "CSV quality owner is missing";
            case URS_MISSING -> "URS quality material is missing";
            case FRS_MISSING -> "FRS quality material is missing";
            case RISK_ASSESSMENT_MISSING -> "Risk assessment quality material is missing";
            case IQ_MISSING -> "IQ quality material is missing";
            case OQ_MISSING -> "OQ quality material is missing";
            case PQ_MISSING -> "PQ quality material is missing";
            case TRACE_MATRIX_MISSING -> "Trace matrix quality material is missing";
            case ELECTRONIC_SIGNATURE_SOP_MISSING -> "Electronic signature SOP quality material is missing";
            case EVIDENCE_INDEX_MISSING -> "Evidence index quality material is missing";
            case QUALITY_MATERIAL_INCOMPLETE ->
                    "A quality material is missing document metadata, evidence, change control, or signature meaning";
            case QUALITY_MATERIAL_NOT_APPROVED -> "A quality material is not approved";
            case TRACE_RELATION_MISSING -> "CSV trace relation is missing or incomplete";
            case TRAINING_RECORD_MISSING -> "Effective training record is missing";
            case CHANGE_CONTROL_MISSING -> "Approved change control is missing";
            case QA_APPROVAL_MISSING -> "QA approval is missing";
            case RECOVERY_EVIDENCE_MISSING -> "Recovery evidence is missing";
        };
    }

    private static String impact(SignatureGovernanceCsvBlockerCode code) {
        return switch (code) {
            case QUALITY_OWNER_MISSING ->
                    "Release gate cannot assign quality accountability and must remain BLOCKED.";
            case URS_MISSING, FRS_MISSING, RISK_ASSESSMENT_MISSING, IQ_MISSING, OQ_MISSING, PQ_MISSING,
                    TRACE_MATRIX_MISSING, ELECTRONIC_SIGNATURE_SOP_MISSING, EVIDENCE_INDEX_MISSING,
                    QUALITY_MATERIAL_INCOMPLETE, QUALITY_MATERIAL_NOT_APPROVED ->
                    "CSV package evidence is incomplete and cannot be used as release approval.";
            case TRACE_RELATION_MISSING ->
                    "Requirements, design, tests, evidence, owner, blocker, and approval cannot be traced.";
            case TRAINING_RECORD_MISSING ->
                    "Release gate cannot prove users were trained on the electronic signature SOP.";
            case CHANGE_CONTROL_MISSING ->
                    "Release gate cannot prove the governed change was approved.";
            case QA_APPROVAL_MISSING ->
                    "Engineering verification cannot auto-approve QA; release gate must remain BLOCKED.";
            case RECOVERY_EVIDENCE_MISSING ->
                    "Release gate cannot prove retention and recovery evidence is available.";
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
