package cn.iocoder.yudao.module.system.service.gxpaudit;

import com.fasterxml.jackson.databind.JsonNode;

public record GxpAuditPolicyBundle(String schemaVersion, String policyVersion, String status,
                                   String approvalReference, String policyHash, String artifactHash,
                                   String canonicalPolicyJson, String rawYaml, String rawSchema,
                                   JsonNode policyNode) {

    public boolean approved() {
        return "APPROVED".equals(status);
    }
}
