package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.resource;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - MES 产品工艺资源 Response VO")
@Data
public class MesProRouteResourceRespVO {

    @Schema(description = "行键")
    private String rowKey;

    @Schema(description = "资源类型：MACHINE/WORKER/UNCONFIGURED")
    private String resourceType;

    @Schema(description = "路线产品编号")
    private Long routeProductId;

    @Schema(description = "产品编号")
    private Long productId;

    @Schema(description = "产品编码")
    private String productCode;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "工艺路线编号")
    private Long routeId;

    @Schema(description = "工艺路线编码")
    private String routeCode;

    @Schema(description = "工艺路线名称")
    private String routeName;

    @Schema(description = "路线工序编号")
    private Long routeProcessId;

    @Schema(description = "工序编号")
    private Long processId;

    @Schema(description = "工序编码")
    private String processCode;

    @Schema(description = "工序名称")
    private String processName;

    @Schema(description = "工序序号")
    private Integer sort;

    @Schema(description = "批记录报表 ID")
    private String batchRecordReportId;

    @Schema(description = "批记录报表编码")
    private String batchRecordReportCode;

    @Schema(description = "批记录报表名称")
    private String batchRecordReportName;

    @Schema(description = "工位编号")
    private Long workstationId;

    @Schema(description = "工位编码")
    private String workstationCode;

    @Schema(description = "工位名称")
    private String workstationName;

    @Schema(description = "工位设备绑定编号")
    private Long workstationMachineId;

    @Schema(description = "设备编号")
    private Long machineryId;

    @Schema(description = "设备编码")
    private String machineryCode;

    @Schema(description = "设备名称")
    private String machineryName;

    @Schema(description = "设备数量")
    private Integer machineryQuantity;

    @Schema(description = "设备单台标准小时产能")
    private BigDecimal machineryStandardHourlyCapacity;

    @Schema(description = "工位人员绑定编号")
    private Long workstationWorkerId;

    @Schema(description = "岗位编号")
    private Long postId;

    @Schema(description = "人工人数")
    private Integer workerQuantity;

    @Schema(description = "单人标准小时产能")
    private BigDecimal singleStandardHourlyCapacity;

    @Schema(description = "预算小时产能")
    private BigDecimal budgetHourlyCapacity;

    @Schema(description = "预算日产能")
    private BigDecimal budgetDailyCapacity;

    @Schema(description = "产能来源")
    private String capacitySource;
}
