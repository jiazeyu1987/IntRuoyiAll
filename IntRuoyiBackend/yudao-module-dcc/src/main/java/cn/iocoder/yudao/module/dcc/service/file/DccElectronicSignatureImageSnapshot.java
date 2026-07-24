package cn.iocoder.yudao.module.dcc.service.file;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DccElectronicSignatureImageSnapshot {

    Long imageId;
    Integer versionNo;
    Long fileId;
    String fileUrl;
    String fileName;
    String contentType;
    Long fileSize;
    String sha256;
    String imageStatus;
    String verifiedStatus;
    byte[] content;
}
