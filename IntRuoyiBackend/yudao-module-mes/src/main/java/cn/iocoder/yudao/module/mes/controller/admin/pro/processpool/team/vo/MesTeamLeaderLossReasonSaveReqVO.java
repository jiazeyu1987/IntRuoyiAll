package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 生产组长损耗原因新增 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderLossReasonSaveReqVO {

    @Schema(description = "路线工序编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "7101")
    @NotNull(message = "路线工序不能为空")
    private Long routeProcessId;

    @Schema(description = "原因编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "LOSS-001")
    @NotBlank(message = "原因编码不能为空")
    private String reasonCode;

    @Schema(description = "原因名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "正常损耗")
    @NotBlank(message = "原因名称不能为空")
    private String reasonName;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

    @Schema(description = "备注")
    private String remark;

}
