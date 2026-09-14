package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DccNasUncontrolledImportSelectedReqVO {

    @NotBlank(message = "selectionScope is required")
    private String selectionScope;

    @NotBlank(message = "idempotencyKey is required")
    private String idempotencyKey;

    @Valid
    @NotEmpty(message = "selectedFiles is required")
    private List<SelectedFile> selectedFiles;

    @Data
    public static class SelectedFile {

        @NotNull(message = "auditFileId is required")
        private Long auditFileId;

        @NotBlank(message = "sourceSignature is required")
        private String sourceSignature;

        @NotBlank(message = "localRelativePath is required")
        private String localRelativePath;
    }
}
