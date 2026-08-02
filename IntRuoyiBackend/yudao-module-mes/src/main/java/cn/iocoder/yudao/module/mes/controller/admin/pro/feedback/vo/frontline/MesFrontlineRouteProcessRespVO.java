package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - MES 一线设备账号可切换工序 Response VO")
@Data
public class MesFrontlineRouteProcessRespVO {

    @Schema(description = "工艺路线编号")
    private Long routeId;
    @Schema(description = "工艺路线编码")
    private String routeCode;
    @Schema(description = "工艺路线名称")
    private String routeName;
    @Schema(description = "工艺路线工序编号")
    private Long routeProcessId;
    @Schema(description = "工序编号")
    private Long processId;
    @Schema(description = "工序编码")
    private String processCode;
    @Schema(description = "工序名称")
    private String processName;
    @Schema(description = "工艺路线工序序号")
    private Integer sort;
    @Schema(description = "设备编号")
    private Long deviceId;
    @Schema(description = "设备编码")
    private String deviceCode;
    @Schema(description = "设备名称")
    private String deviceName;
    @Schema(description = "工作站编号")
    private Long workstationId;
    @Schema(description = "工作站编码")
    private String workstationCode;
    @Schema(description = "工作站名称")
    private String workstationName;
    @Schema(description = "活跃订单编号")
    private Long activeOrderId;
    @Schema(description = "PQC 检验任务编号")
    private Long pqcTaskId;
    @Schema(description = "QA 规程发布版本编号")
    private Long regulationVersionId;
    @Schema(description = "检验类型：FIRST/PATROL/FINAL")
    private String inspectionType;
    @Schema(description = "业务日期")
    private LocalDate businessDate;
    @Schema(description = "班次编码")
    private String shiftCode;
    @Schema(description = "轮次")
    private Integer roundNo;
    @Schema(description = "计划检验数量")
    private Integer plannedInspectionQuantity;
    @Schema(description = "QA 规程检验项目")
    private List<PqcInspectionItem> inspectionItems;

    @Data
    public static class PqcInspectionItem {

        @Schema(description = "检验项目编码")
        private String itemCode;
        @Schema(description = "检验项目名称")
        private String itemName;
        @Schema(description = "检验方法")
        private String inspectionMethod;
        @Schema(description = "合格标准")
        private String standardText;
        @Schema(description = "结果类型")
        private String resultType;
    }

}
