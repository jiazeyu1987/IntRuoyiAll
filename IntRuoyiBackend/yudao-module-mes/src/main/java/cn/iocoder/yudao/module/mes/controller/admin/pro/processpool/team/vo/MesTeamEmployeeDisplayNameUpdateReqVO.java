package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 生产人员显示名更新 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamEmployeeDisplayNameUpdateReqVO {

    @Schema(description = "生产人员档案编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "8801")
    @NotNull(message = "生产人员档案编号不能为空")
    private Long employeeProfileId;

    @Schema(description = "生产人员显示名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三-A")
    @NotBlank(message = "生产人员显示名不能为空")
    private String displayName;
}
