package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - MES 排产工单排产前检查范围 Response VO")
@Data
public class MesProScheduleOrderPreflightScopeRespVO {

    @Schema(description = "检查范围", example = "SELECTED")
    private String scopeType;

    @Schema(description = "排产工单数量", example = "3")
    private Integer scheduleOrderCount = 0;

}
