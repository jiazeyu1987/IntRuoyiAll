package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Recovery requests intentionally contain no target, path, executor identity or asserted success. */
public final class RuntimeControlBackupRecoveryRequests {
    private RuntimeControlBackupRecoveryRequests() { }
    public record Inspect(@NotNull @Min(0) Long expectedStateVersion) { }
    public record Recover(@NotNull @Min(0) Long expectedStateVersion,
                          @NotBlank @Pattern(regexp = "br-[0-9a-f-]{36}") String previewId,
                          @NotBlank @Pattern(regexp = "PROD") String prodConfirmText) { }
}
