package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 生产组长损耗原因修改 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderLossReasonUpdateReqVO {

    @Schema(description = "原因名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "调机损耗")
    @NotBlank(message = "原因名称不能为空")
    private String reasonName;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

    @Schema(description = "备注")
    private String remark;

}
