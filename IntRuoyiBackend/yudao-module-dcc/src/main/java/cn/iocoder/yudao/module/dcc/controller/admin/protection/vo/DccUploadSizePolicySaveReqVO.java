package cn.iocoder.yudao.module.dcc.controller.admin.protection.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DccUploadSizePolicySaveReqVO {

    @NotBlank
    @Size(max = 64)
    private String policyCode;

    @NotBlank
    private String scopeType;

    private Long categoryId;
    private String purpose;

    @NotNull
    @Positive
    private Long maxBytes;

    @NotNull
    private Boolean enabled;

    @NotBlank
    @Size(max = 64)
    private String policyVersion;

    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;

    @NotBlank
    @Size(max = 500)
    private String changeReason;

}
