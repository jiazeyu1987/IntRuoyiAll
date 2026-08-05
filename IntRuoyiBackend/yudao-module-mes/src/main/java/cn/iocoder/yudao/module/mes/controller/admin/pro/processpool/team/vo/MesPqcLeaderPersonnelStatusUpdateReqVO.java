package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES PQC 组长人员状态更新 Request VO")
@Data
@Accessors(chain = true)
public class MesPqcLeaderPersonnelStatusUpdateReqVO {

    @Schema(description = "负责范围编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "9001")
    @NotNull(message = "负责范围编号不能为空")
    private Long scopeId;

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    @NotNull(message = "是否启用不能为空")
    private Boolean enabled;
}
