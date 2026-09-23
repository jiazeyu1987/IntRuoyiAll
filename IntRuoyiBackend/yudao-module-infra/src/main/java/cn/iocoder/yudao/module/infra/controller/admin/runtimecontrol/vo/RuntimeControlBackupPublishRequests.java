package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** No client-controlled host, path, command, source commit or environment fields. */
public final class RuntimeControlBackupPublishRequests {
    private RuntimeControlBackupPublishRequests() { }
    public record Preview(@NotBlank @Size(max = 120) String reason,
                          @NotBlank String sourceSelectionId, @NotBlank String idempotencyKey) { }
    public record Authorization(@NotBlank String previewId, @NotBlank String prodConfirmText) { }
    public record Publish(@NotBlank String previewId, @NotBlank String authorizationId,
                          @NotBlank String idempotencyKey, @NotBlank String prodConfirmText) { }
}
