package cn.iocoder.yudao.module.dcc.service.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileSignatureEvidence {

    private Long revisionId;
    private String versionNo;
    private Long sourceFileId;
    private String sourceFileHash;
    private String sourceFileHashAlgorithm;
    private String sourceFileHashStatus;
    private Long controlledCopyFileId;
    private String controlledCopyHash;
    private String controlledCopyHashAlgorithm;
    private String controlledCopyHashStatus;
    private Long signatureImageId;
    private Integer signatureImageVersionNo;
    private Long signatureImageFileId;
    private String signatureImageFileUrl;
    private String signatureImageSha256;
    private String signatureImageContentType;
    private Long signatureImageFileSize;
    private String signatureImageStatusSnapshot;
    private String signatureImageVerifiedStatus;
    private String evidencePayloadVersion;
    private String evidenceKeyVersion;
    private String evidenceHash;
    private String evidenceHashAlgorithm;
    private String evidenceStatus;
    private String canonicalPayload;
    private String recordVersionSnapshot;
    private String recordHashSnapshot;

}
