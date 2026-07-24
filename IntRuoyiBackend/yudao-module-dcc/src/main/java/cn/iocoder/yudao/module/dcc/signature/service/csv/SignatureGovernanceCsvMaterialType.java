package cn.iocoder.yudao.module.dcc.signature.service.csv;

import java.util.EnumSet;

public enum SignatureGovernanceCsvMaterialType {

    URS,
    FRS,
    RISK_ASSESSMENT,
    IQ,
    OQ,
    PQ,
    TRACE_MATRIX,
    ELECTRONIC_SIGNATURE_SOP,
    EVIDENCE_INDEX;

    public static EnumSet<SignatureGovernanceCsvMaterialType> requiredMaterials() {
        return EnumSet.allOf(SignatureGovernanceCsvMaterialType.class);
    }
}
