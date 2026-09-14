package cn.iocoder.yudao.module.dcc.service.file;

public record DccControlledFileSourceGovernanceExecutionResult(
        Long controlledFileId,
        String status,
        String action,
        String reasonCode,
        String detail) {
}
