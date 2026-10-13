package cn.iocoder.yudao.module.bpm.approval.service.ownership;

import java.util.Map;

public record NativeApprovalOwnership(Owner owner, String decisionRoute, Map<String, String> decisionQuery,
        String navigationBlocker) {
    public enum Owner { GENERIC, FORM_ACTION, EDHR_EXECUTION, UNCLAIMED_EDHR }

    public static NativeApprovalOwnership generic() {
        return new NativeApprovalOwnership(Owner.GENERIC, null, Map.of(), null);
    }

    public boolean requiresDomainReview() {
        return owner == Owner.EDHR_EXECUTION || owner == Owner.UNCLAIMED_EDHR;
    }
}
