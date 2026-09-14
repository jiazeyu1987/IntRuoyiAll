package cn.iocoder.yudao.module.dcc.service.file;

public record DccControlledFileSourceGovernanceDecision(
        String status,
        String action,
        String reasonCode,
        String detail) {
}
