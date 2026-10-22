package cn.iocoder.yudao.module.system.service.gxpaudit;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only contract; no reverse dependency on the signature module. */
public record SignatureAuditExpectation(Long tenantId, Long signatureId, String evidenceFormatVersion,
        String subjectId, String subjectVersion, Long operatorId, String reason, String contentHash,
        String idempotencyKey, Map<String, Object> afterState) {
    public SignatureAuditExpectation {
        if (tenantId == null || tenantId <= 0 || signatureId == null || signatureId <= 0
                || operatorId == null || operatorId <= 0 || afterState == null) {
            throw new IllegalArgumentException("Signature audit expectation requires positive tenant/signature/operator IDs and frozen state");
        }
        afterState = Collections.unmodifiableMap(new LinkedHashMap<>(afterState));
    }
}
