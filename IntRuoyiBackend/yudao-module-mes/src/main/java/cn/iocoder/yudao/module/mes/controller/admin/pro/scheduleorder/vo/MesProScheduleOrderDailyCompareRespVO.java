package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - MES 排产工单按天计划实际对比 Response VO")
@Data
public class MesProScheduleOrderDailyCompareRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "排产工单编号", example = "100")
    private Long scheduleOrderId;

    @Schema(description = "排产工单工序编号", example = "200")
    private Long scheduleOrderProcessId;

    @Schema(description = "工序编号", example = "300")
    private Long processId;

    @Schema(description = "计划日期")
    private LocalDate planDate;

    @Schema(description = "计划数量", example = "100.000000")
    private BigDecimal plannedQuantity;

    @Schema(description = "实际报工数量", example = "80.000000")
    private BigDecimal actualQuantity;

    @Schema(description = "差异数量", example = "-20.000000")
    private BigDecimal diffQuantity;

    @Schema(description = "状态：0 正常，1 提前，2 滞后，3 无计划有报工，4 有计划无报工")
    private Integer status;

    @Schema(description = "状态名称", example = "滞后")
    private String statusLabel;

    @Schema(description = "备注")
    private String remark;

}
