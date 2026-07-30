package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 班组员工绑定禁用 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamEmployeeBindingDisableReqVO {

    @Schema(description = "员工绑定编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "8201")
    @NotNull
    private Long bindingId;
}
