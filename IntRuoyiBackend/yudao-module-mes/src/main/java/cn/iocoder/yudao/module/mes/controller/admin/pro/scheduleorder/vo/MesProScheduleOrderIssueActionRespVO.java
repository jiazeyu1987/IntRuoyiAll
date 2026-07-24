package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - MES 排产工单问题处理动作 Response VO")
@Data
public class MesProScheduleOrderIssueActionRespVO {

    @Schema(description = "动作名称", example = "维护路线")
    private String actionLabel;

    @Schema(description = "目标前端路由名称", example = "MesProRouteEdit")
    private String targetRouteName;

    @Schema(description = "目标前端路由参数")
    private Map<String, Object> targetQuery;

    @Schema(description = "执行动作所需权限", example = "mes:pro-route:schedule-config:update")
    private String requiredPermission;

}
