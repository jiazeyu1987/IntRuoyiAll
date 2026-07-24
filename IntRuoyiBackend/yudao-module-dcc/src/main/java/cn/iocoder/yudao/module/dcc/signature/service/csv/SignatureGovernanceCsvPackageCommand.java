package cn.iocoder.yudao.module.dcc.signature.service.csv;

import java.util.List;

public record SignatureGovernanceCsvPackageCommand(String packageId,
                                                   String qualityOwner,
                                                   List<SignatureGovernanceCsvMaterial> materials,
                                                   List<SignatureGovernanceCsvTraceRelation> traceRelations,
                                                   List<SignatureGovernanceCsvTrainingRecord> trainingRecords,
                                                   List<SignatureGovernanceCsvChangeControl> changeControls,
                                                   SignatureGovernanceCsvQaApproval qaApproval,
                                                   String recoveryEvidenceRef,
                                                   boolean engineeringVerificationPassed) {

    public SignatureGovernanceCsvPackageCommand {
        if (isBlank(packageId)) {
            throw new IllegalArgumentException("CSV packageId is required");
        }
        packageId = packageId.trim();
        qualityOwner = trimToNull(qualityOwner);
        materials = materials == null ? List.of() : List.copyOf(materials);
        traceRelations = traceRelations == null ? List.of() : List.copyOf(traceRelations);
        trainingRecords = trainingRecords == null ? List.of() : List.copyOf(trainingRecords);
        changeControls = changeControls == null ? List.of() : List.copyOf(changeControls);
        recoveryEvidenceRef = trimToNull(recoveryEvidenceRef);
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
