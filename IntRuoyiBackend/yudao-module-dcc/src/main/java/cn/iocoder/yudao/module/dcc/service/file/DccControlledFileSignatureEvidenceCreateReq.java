package cn.iocoder.yudao.module.dcc.service.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileSignatureEvidenceCreateReq {

    private Long tenantId;
    private Long controlledFileId;
    private String taskId;
    private String taskActionResult;
    private String meaningCode;
    private Long signerUserId;
    private Long signerDeptId;
    private String signerUsername;
    private String signerNickname;
    private String signerDeptName;
    private String signerPostNames;
    private String signerRoleNames;
    private String signaturePurpose;
    private String authorizationBasis;
    private String authenticationMethod;
    private LocalDateTime signedAt;
    private String reasonText;
    private String controlledCopyHashStatus;
    private Long controlledCopyFileId;
    private Long signatureImageId;
    private Integer signatureImageVersionNo;
    private Long signatureImageFileId;
    private String signatureImageFileUrl;
    private String signatureImageSha256;
    private String signatureImageContentType;
    private Long signatureImageFileSize;
    private String signatureImageStatusSnapshot;
    private String signatureImageVerifiedStatus;

}
