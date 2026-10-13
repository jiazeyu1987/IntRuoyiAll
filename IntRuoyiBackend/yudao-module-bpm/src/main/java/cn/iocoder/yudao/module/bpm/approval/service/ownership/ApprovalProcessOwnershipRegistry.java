package cn.iocoder.yudao.module.bpm.approval.service.ownership;

import cn.iocoder.yudao.module.bpm.api.task.dto.ApprovalProcessAuthorityDTO;
import java.util.Map;

/** Closed formal writer registry; absence is an error, never a default native owner. */
public final class ApprovalProcessOwnershipRegistry {
    private ApprovalProcessOwnershipRegistry() { }
    private static final Map<String,String> WRITERS = Map.of(
            "DCC_CONTROLLED_FILE_SUBMIT", "DCC_TASK:CONTROLLED_FILE",
            "FORM_ACTION_SUBMIT", "NATIVE_FORM:FORM_ACTION",
            "BUSINESS_APPROVAL_SUBMIT", "NATIVE_BUSINESS:BUSINESS_REQUEST",
            "EDHR_BUSINESS_APPROVAL_SUBMIT", "NATIVE_EXTERNAL_EDHR:BUSINESS_REQUEST",
            "REGISTRATION_CERTIFICATE_SUBMIT", "NATIVE_BUSINESS:REGISTRATION_REQUEST",
            "BPM_MANUAL_START", "NATIVE_BPM:BPM_START",
            "OA_LEAVE_SUBMIT", "NATIVE_BUSINESS:OA_LEAVE",
            "CRM_CONTRACT_SUBMIT", "NATIVE_BUSINESS:CRM_CONTRACT",
            "CRM_RECEIVABLE_SUBMIT", "NATIVE_BUSINESS:CRM_RECEIVABLE");
    public static void require(ApprovalProcessAuthorityDTO fact) {
        if (fact == null || fact.tenantId() == null || fact.tenantId() < 0
                || !java.util.Objects.equals(WRITERS.get(fact.writerCode()), fact.projectionCode()+":"+fact.authorityType()))
            throw new IllegalStateException("APPROVAL_OWNERSHIP_FORMAL_WRITER_REQUIRED");
        text(fact.authorityId(), 64); text(fact.authorityVersion(), 64); text(fact.writerCode(), 64);
        if (fact.expectedBusinessKey() != null && fact.expectedBusinessKey().length() > 255)
            throw new IllegalArgumentException("APPROVAL_OWNERSHIP_BUSINESS_KEY_CAPACITY");
    }
    static void text(String value, int max) {
        if (value == null || value.isBlank() || value.length() > max)
            throw new IllegalArgumentException("APPROVAL_OWNERSHIP_IDENTITY_CAPACITY");
    }
}
