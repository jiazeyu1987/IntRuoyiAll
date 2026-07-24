package cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - MES 生产工序 Response VO")
@Data
public class MesProProcessRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "工序编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "PROCESS001")
    private String code;

    @Schema(description = "产品名称", example = "球囊扩张导管")
    private String productName;

    @Schema(description = "工序名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "下料工序")
    private String name;

    @Schema(description = "工艺要求", example = "按照图纸尺寸进行切割")
    private String attention;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "人工班次产能", example = "740")
    private BigDecimal manualShiftCapacity;

    @Schema(description = "设备数量合计", example = "5")
    private Integer machineryQuantityTotal;

    @Schema(description = "当前可用班次产能", example = "500")
    private BigDecimal availableShiftCapacityTotal;

    @Schema(description = "产能来源：MACHINE/WORKER/UNCONFIGURED", example = "MACHINE")
    private String capacitySource;

    @Schema(description = "生产系数", example = "1.000000")
    private BigDecimal productionQuantityFactor;

    @Schema(description = "路线工序班次产能", example = "315.000000")
    private BigDecimal shiftCapacity;

    @Schema(description = "所属工艺路线列表")
    private List<RouteSimpleRespVO> routeList;

    @Schema(description = "多工艺路线排产产能是否不一致", example = "true")
    private Boolean routeCapacityConflict;

    @Schema(description = "多工艺路线排产产能不一致提示", example = "多条工艺路线的排产产能不一致，请进入工艺流程使用覆盖产能处理。")
    private String routeCapacityConflictMessage;

    @Schema(description = "工作站名称，多个以顿号拼接", example = "WS-A 一号工作站")
    private String workstationNames;

    @Schema(description = "工作站列表")
    private List<WorkstationSimpleRespVO> workstations;

    @Schema(description = "批记录表单名称，多个以顿号拼接", example = "组装Ⅰ工序生产记录")
    private String batchRecordFormNames;

    @Schema(description = "批记录表单链接列表")
    private List<BatchRecordFormLinkRespVO> batchRecordForms;

    @Schema(description = "损耗单名称，多个以顿号拼接", example = "损耗记录表")
    private String lossReportFormNames;

    @Schema(description = "损耗单链接列表")
    private List<BatchRecordFormLinkRespVO> lossReportForms;

    @Schema(description = "过程检验单名称，多个以顿号拼接", example = "过程检验记录表")
    private String processInspectionFormNames;

    @Schema(description = "过程检验单链接列表")
    private List<BatchRecordFormLinkRespVO> processInspectionForms;

    @Schema(description = "参数记录表名称，多个以顿号拼接", example = "设备参数记录表")
    private String parameterRecordFormNames;

    @Schema(description = "参数记录表链接列表")
    private List<BatchRecordFormLinkRespVO> parameterRecordForms;

    @Schema(description = "备注", example = "金属板材下料")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "管理后台 - MES 生产工序所属工艺路线 Response VO")
    @Data
    public static class RouteSimpleRespVO {

        @Schema(description = "工艺路线编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
        private Long id;

        @Schema(description = "路线工序编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
        private Long routeProcessId;

        @Schema(description = "工艺路线编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "ROUTE-PUMP")
        private String code;

        @Schema(description = "工艺路线名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "压力泵")
        private String name;

        @Schema(description = "该路线工序排产班次产能", example = "315.000000")
        private BigDecimal shiftCapacity;

    }

    @Schema(description = "管理后台 - MES 生产工序工作站 Response VO")
    @Data
    public static class WorkstationSimpleRespVO {

        @Schema(description = "工作站编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "301")
        private Long id;

        @Schema(description = "工作站编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "WS-A")
        private String code;

        @Schema(description = "工作站名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "一号工作站")
        private String name;

    }

    @Schema(description = "管理后台 - MES 工序批记录表单链接 Response VO")
    @Data
    public static class BatchRecordFormLinkRespVO {

        @Schema(description = "报表编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ER130A41E19498")
        private String reportId;

        @Schema(description = "报表名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "大包装工序生产记录")
        private String reportName;

    }

}
