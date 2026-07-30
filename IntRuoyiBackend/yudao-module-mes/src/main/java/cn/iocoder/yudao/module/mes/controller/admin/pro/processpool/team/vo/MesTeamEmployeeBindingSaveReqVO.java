package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 班组员工绑定新增 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamEmployeeBindingSaveReqVO {

    @Schema(description = "工序编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "6001")
    @NotNull
    private Long processId;

    @Schema(description = "员工用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    @NotNull
    private Long employeeUserId;
}
