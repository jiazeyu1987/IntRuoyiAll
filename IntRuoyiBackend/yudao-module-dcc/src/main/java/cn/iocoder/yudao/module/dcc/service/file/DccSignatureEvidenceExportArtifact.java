package cn.iocoder.yudao.module.dcc.service.file;

public record DccSignatureEvidenceExportArtifact(
        String fileName,
        String contentType,
        byte[] bytes
) {
}
