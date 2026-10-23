package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DccWorkingApplicationStorageRecoveryReqVO(
        @NotNull Boolean confirmed,
        @NotNull Long expectedOriginalProofId,
        @NotBlank String expectedOriginalProofSha256,
        @NotBlank String expectedObservedSha256,
        @NotBlank String requestId,
        @NotBlank String changeReason) {}
