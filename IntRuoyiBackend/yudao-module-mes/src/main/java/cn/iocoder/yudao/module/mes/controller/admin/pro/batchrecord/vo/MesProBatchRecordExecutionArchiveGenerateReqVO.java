package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - MES 批记录执行归档生成 Request VO")
@Data
public class MesProBatchRecordExecutionArchiveGenerateReqVO {

    @Schema(description = "执行记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "9")
    @NotNull(message = "执行记录编号不能为空")
    private Long executionId;

    @Schema(description = "归档类型：PDF、EXCEL", requiredMode = Schema.RequiredMode.REQUIRED, example = "PDF")
    @NotBlank(message = "归档类型不能为空")
    private String artifactType;

    @Schema(description = "电子签名密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "******")
    @NotBlank(message = "电子签名密码不能为空")
    private String sealPassword;

    @Schema(description = "封存说明", example = "最终批记录归档")
    private String comment;

    @Valid
    private MesProBatchRecordExecutionSignatureTimeReqVO signatureTime;

    @Schema(description = "是否强制重新生成", example = "false")
    private Boolean regenerate;
}
