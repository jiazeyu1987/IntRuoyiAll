package cn.iocoder.yudao.module.dcc.service.file;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class DccUnifiedSignatureResult {

    Long signatureId;

    Long controlledFileId;

    Long revisionId;

    String versionNo;

    String meaningCode;

    String controlledCopyHashStatus;

    String evidenceStatus;

    String evidenceHash;

    LocalDateTime signedAt;

}
