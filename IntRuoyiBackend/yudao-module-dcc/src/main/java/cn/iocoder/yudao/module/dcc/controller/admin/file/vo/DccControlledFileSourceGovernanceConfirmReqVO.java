package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "Admin - DCC source governance batch confirmation request")
@Data
public class DccControlledFileSourceGovernanceConfirmReqVO {

    @Schema(description = "Manifest SHA-256", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String manifestSha256;

    @Schema(description = "Request fingerprint SHA-256", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String requestSha256;
}
