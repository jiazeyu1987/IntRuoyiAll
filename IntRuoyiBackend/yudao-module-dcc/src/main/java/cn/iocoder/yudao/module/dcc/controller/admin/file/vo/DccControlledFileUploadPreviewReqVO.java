package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class DccControlledFileUploadPreviewReqVO {

    @Schema(description = "Exactly one controlled source file or drawing PDF", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "files is required")
    private MultipartFile[] files;

    @Schema(description = "Controlled file category id", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "10")
    @NotNull(message = "categoryId is required")
    private Long categoryId;

    @Schema(description = "Upload session id", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "dcc-upload-session-20260528")
    @NotBlank(message = "sessionId is required")
    private String sessionId;

    @Schema(description = "Upload purpose for controlled source validation: SOURCE, DRAWING_PDF, TRAINING_RECORD or EXTERNAL_REVIEW_OUTPUT",
            example = "SOURCE")
    @NotBlank(message = "purpose is required")
    private String purpose;

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "exactly one file is required")
    public boolean isSingleFile() {
        return files != null && files.length == 1 && files[0] != null && !files[0].isEmpty();
    }
}
