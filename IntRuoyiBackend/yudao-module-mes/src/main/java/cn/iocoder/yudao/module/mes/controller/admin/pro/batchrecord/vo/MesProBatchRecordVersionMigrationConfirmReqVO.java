package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - eDHR批记录版本迁移项确认 Request VO")
@Data
@Accessors(chain = true)
public class MesProBatchRecordVersionMigrationConfirmReqVO {

    @Schema(description = "迁移项ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "迁移项不能为空")
    private List<Long> itemIds;

    @Schema(description = "确认意见", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "确认意见不能为空")
    @Size(max = 500, message = "确认意见长度不能超过 500 个字符")
    private String comment;

    @Schema(description = "幂等键", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "幂等键不能为空")
    @Size(max = 128, message = "幂等键长度不能超过 128 个字符")
    private String idempotencyKey;
}
