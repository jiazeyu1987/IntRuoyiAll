package cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 排产员工作台全量数据包导入 Response VO")
@Data
public class MesProSchedulerWorkbenchFullConfigImportRespVO {

    @Schema(description = "用户角色绑定数量", example = "4")
    private Integer userRoleBindingCount;

    @Schema(description = "分配角色数量", example = "7")
    private Integer assignedRoleCount;
}
