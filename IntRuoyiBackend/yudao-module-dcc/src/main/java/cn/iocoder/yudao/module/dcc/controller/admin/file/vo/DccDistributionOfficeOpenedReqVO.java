package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Existing preview evidence, validated again against the authenticated recipient and successful callback. */
@Data
public class DccDistributionOfficeOpenedReqVO {
    @NotBlank private String viewerToken;
    @NotBlank private String accessEventCode;
    @NotBlank private String watermarkTraceCode;
    @NotBlank private String viewerTokenId;
    @NotBlank private String viewerTokenNonce;
}
