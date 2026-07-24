package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - MES 工艺路线工序 Response VO")
@Data
public class MesProRouteProcessRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "工艺路线编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long routeId;

    @Schema(description = "工序编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long processId;

    @Schema(description = "工序编码")
    private String processCode;

    @Schema(description = "工序名称")
    private String processName;

    @Schema(description = "工序所属产品名称")
    private String processProductName;

    @Schema(description = "工艺要求")
    private String processAttention;

    @Schema(description = "工序状态")
    private Integer processStatus;

    @Schema(description = "工序人工班次产能")
    private BigDecimal processManualShiftCapacity;

    @Schema(description = "序号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer sort;

    @Schema(description = "直接前置工序")
    private MesProRouteProcessRelationRespVO predecessor;

    @Schema(description = "直接前置工序列表")
    private List<MesProRouteProcessRelationRespVO> predecessors;

    @Schema(description = "直接后续工序")
    private List<MesProRouteProcessRelationRespVO> successors;

    @Schema(description = "工作站编号", example = "1")
    private Long workstationId;

    @Schema(description = "工作站编码")
    private String workstationCode;

    @Schema(description = "工作站名称")
    private String workstationName;

    @Schema(description = "设备数量合计")
    private Integer machineryQuantityTotal;

    @Schema(description = "设备列表")
    private List<MesProRouteProcessMachineryRespVO> machineryList;

    @Schema(description = "人工人数合计")
    private Integer workerQuantityTotal;

    @Schema(description = "工作站人员绑定编号")
    private Long workstationWorkerId;

    @Schema(description = "工序总标准小时产能")
    private BigDecimal processHourlyCapacityTotal;

    @Schema(description = "工序总标准班次产能")
    private BigDecimal processShiftCapacityTotal;

    @Schema(description = "产能来源：MACHINE/WORKER/UNCONFIGURED")
    private String capacitySource;

    @Schema(description = "班次小时数")
    private BigDecimal shiftHours;

    @Schema(description = "今日可用资源数量合计")
    private Integer todayAvailableResourceQuantityTotal;

    @Schema(description = "今日总小时产能")
    private BigDecimal todayHourlyCapacityTotal;

    @Schema(description = "今日总班次产能")
    private BigDecimal todayShiftCapacityTotal;

    @Schema(description = "资源状态：NORMAL/REPAIR/CAPACITY_MISSING/UNCONFIGURED")
    private String resourceStatus;

    @Schema(description = "资源状态原因")
    private String resourceStatusReason;

    @Schema(description = "人工单人标准小时产能")
    private BigDecimal workerSingleStandardHourlyCapacity;

    @Schema(description = "准备时间（分钟）", example = "10")
    private Integer prepareTime;

    @Schema(description = "等待时间（分钟）", example = "5")
    private Integer waitTime;

    @Schema(description = "甘特图显示颜色", example = "#00AEF3")
    private String colorCode;

    @Schema(description = "是否关键工序", example = "false")
    private Boolean keyFlag;

    @Schema(description = "是否质检工序", example = "false")
    private Boolean checkFlag;

    @Schema(description = "默认批记录报表 ID", example = "jimu-report-001")
    private String batchRecordReportId;

    @Schema(description = "默认批记录报表编码", example = "EBR_A_T01")
    private String batchRecordReportCode;

    @Schema(description = "默认批记录报表名称", example = "电子批记录[A]-表1")
    private String batchRecordReportName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
