package cn.iocoder.yudao.module.dcc.service.file;

public record DccControlledFileSourceGovernancePostflightFinding(
        Long itemId, Long controlledFileId, String status, String reasonCode, String detail) {
}
