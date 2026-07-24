package cn.iocoder.yudao.module.dcc.signature.service.csv;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public record SignatureGovernanceCsvPackageResult(String packageId,
                                                  SignatureGovernanceCsvPackageStatus status,
                                                  Set<SignatureGovernanceCsvMaterialType> materialTypes,
                                                  List<SignatureGovernanceCsvTraceRelation> traceRelations,
                                                  boolean engineeringVerificationPassed,
                                                  boolean qaApproved,
                                                  List<SignatureGovernanceCsvBlocker> blockers) {

    public SignatureGovernanceCsvPackageResult {
        if (status == null) {
            throw new IllegalArgumentException("CSV package result status is required");
        }
        materialTypes = immutableMaterialTypes(materialTypes);
        traceRelations = traceRelations == null ? List.of() : List.copyOf(traceRelations);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }

    private static Set<SignatureGovernanceCsvMaterialType> immutableMaterialTypes(
            Set<SignatureGovernanceCsvMaterialType> materialTypes) {
        EnumSet<SignatureGovernanceCsvMaterialType> copy = materialTypes == null || materialTypes.isEmpty()
                ? EnumSet.noneOf(SignatureGovernanceCsvMaterialType.class) : EnumSet.copyOf(materialTypes);
        return Collections.unmodifiableSet(copy);
    }
}
