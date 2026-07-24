package cn.iocoder.yudao.module.bpm.approval.service.signature;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApprovalSignatureRecordResult {

    Long recordId;

    Long signatureImageId;

    String signatureImageFileUrl;

}
