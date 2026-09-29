package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Schema(description = "管理后台 - eDHR 关键偏差转不合格评审 Request VO")
@Data
public class MesProEdhrDeviationNcrCreateReqVO {

    @NotNull
    @Schema(description = "正式批记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long batchExecutionId;

    @NotEmpty
    @Size(max = 100)
    @Schema(description = "本次选择的同批关键偏差ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<@NotNull Long> deviationIds;

    @NotBlank
    @Size(max = 500)
    @Schema(description = "不合格原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nonconformanceReason;

    @Size(max = 500)
    private String remark;

    @NotBlank
    @Size(max = 128)
    @Schema(description = "转审幂等键", requiredMode = Schema.RequiredMode.REQUIRED)
    private String idempotencyKey;

    @NotBlank
    @ToString.Exclude
    @Schema(description = "QA电子签名密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String signaturePassword;
}
