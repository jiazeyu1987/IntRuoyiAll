package cn.iocoder.yudao.module.system.service.gxpaudit;

import lombok.Builder;

@Builder
public record GxpAuditPolicyActivationCommand(Long tenantId, Long actorId, String approvalReference,
                                              String requestId, String coverageReportHash) {
}
