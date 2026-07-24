package cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 工作站 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MesMdWorkstationRespVO {

    @Schema(description = "工作站编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("工作站编号")
    private Long id;

    @Schema(description = "工作站编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "WK001")
    @ExcelProperty("工作站编码")
    private String code;

    @Schema(description = "工作站名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "一号工作站")
    @ExcelProperty("工作站名称")
    private String name;

    @Schema(description = "工作站地点", example = "A区1号线")
    @ExcelProperty("工作站地点")
    private String address;

    @Schema(description = "所在车间编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long workshopId;

    @Schema(description = "车间名称", example = "一号车间")
    @ExcelProperty("车间名称")
    private String workshopName;

    @Schema(description = "工序编号", example = "1")
    private Long processId;

    @Schema(description = "工序名称", example = "打磨")
    @ExcelProperty("工序名称")
    private String processName;

    @Schema(description = "绑定设备摘要", example = "EQ-01 / 裁切机 ×2")
    @ExcelProperty("绑定设备")
    private String machinerySummary;

    @Schema(description = "绑定设备个数", example = "2")
    private Integer machineryCount;

    @Schema(description = "产线编号", example = "1")
    private Long productionLineId;

    @Schema(description = "产线名称", example = "一号产线")
    @ExcelProperty("产线名称")
    private String productionLineName;

    @Schema(description = "线边库编号", example = "1")
    private Long warehouseId;

    @Schema(description = "库区编号", example = "1")
    private Long locationId;

    @Schema(description = "库位编号", example = "1")
    private Long areaId;

    @Schema(description = "单人标准小时产能", example = "12.5")
    private BigDecimal singleStandardHourlyCapacity;

    @Schema(description = "班次小时数", example = "10.5")
    private BigDecimal shiftHours;

    @Schema(description = "理论配置人数", example = "3")
    private Integer configuredWorkerCount;

    @Schema(description = "当前在岗人数", example = "2")
    private Integer currentWorkerCount;

    @Schema(description = "设备标准小时产能", example = "180")
    private BigDecimal machineryStandardHourlyCapacity;

    @Schema(description = "今日产能", example = "96")
    private BigDecimal todayCapacity;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "备注", example = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
