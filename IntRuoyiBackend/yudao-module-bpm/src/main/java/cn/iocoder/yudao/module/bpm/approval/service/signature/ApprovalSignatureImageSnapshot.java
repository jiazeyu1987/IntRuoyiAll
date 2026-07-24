package cn.iocoder.yudao.module.bpm.approval.service.signature;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApprovalSignatureImageSnapshot {

    Long imageId;
    Integer versionNo;
    Long fileId;
    String fileUrl;
    String sha256;
    String contentType;
    Long fileSize;
    String imageStatus;
    String verifiedStatus;
}
