package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "Admin - Prepare a frozen DCC source governance manifest")
@Data
public class DccControlledFileSourceGovernancePrepareReqVO {

    @NotBlank
    private String taskKey;

    @Min(1)
    @Max(200)
    private int batchSize = 100;

    @Min(0)
    private Long startAfterControlledFileId = 0L;
}
