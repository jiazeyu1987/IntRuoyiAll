package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "Admin - DCC source governance batch execution request")
@Data
public class DccControlledFileSourceGovernanceExecuteReqVO {

    @Schema(description = "Manifest SHA-256", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String manifestSha256;

    @Schema(description = "Request fingerprint SHA-256", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String requestSha256;

    @Schema(description = "Maximum number of READY items to process", defaultValue = "100")
    @Min(1)
    @Max(200)
    private int batchSize = 100;
}
