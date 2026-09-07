package cn.iocoder.yudao.module.dcc.service.file;

public record DccPublicationImpactRevisionOption(
        Long controlledFileId, String versionNo, String status, boolean currentActive) {
}
