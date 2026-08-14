package cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - MES 可用夜班与产能状态 Response VO")
@Data
public class MesProSchedulerWorkbenchNightShiftCapacityStatusRespVO {

    @Schema(description = "可用夜班班次数")
    private Integer availableShiftCount;

    @Schema(description = "已配置夜班产能的产线数")
    private Integer capacityLineCount;

    @Schema(description = "是否存在可用夜班产能")
    private Boolean available;

    @Schema(description = "夜班明细")
    private List<NightShift> shifts;

    @Data
    public static class NightShift {
        private Long planId;
        private Long shiftId;
        private String shiftName;
        private String startTime;
        private String endTime;
        private Integer capacityLineCount;
    }
}
