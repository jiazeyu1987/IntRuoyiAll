package cn.iocoder.yudao.module.dcc.controller.admin.signature.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - DCC电子签名证据详情 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class DccSignatureEvidenceRespVO extends DccElectronicSignatureRespVO {

    @Schema(description = "签名记录 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "8400123")
    private Long signatureId;

    @Schema(description = "规范化载荷字段顺序")
    private List<String> canonicalPayloadFieldOrder;

    @Schema(description = "规范化载荷")
    private String canonicalPayload;

    @Schema(description = "校验状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "VALID")
    private String verificationStatus;

    @Schema(description = "校验时间")
    private LocalDateTime verifiedAt;
}
