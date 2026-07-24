package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - MES 工艺路线工序流程配置 Response VO")
@Data
public class MesProRouteFlowProcessConfigRespVO {

    @Schema(description = "工艺路线工序ID", example = "100")
    private Long routeProcessId;

    @Schema(description = "工序排序", example = "1")
    private Integer sort;

    @Schema(description = "工序编码", example = "B010")
    private String processCode;

    @Schema(description = "工序名称", example = "吹球囊成型")
    private String processName;

    @Schema(description = "用途类型", example = "SCHEDULE")
    private String useType;

    @Schema(description = "当前用途是否启用", example = "true")
    private Boolean enabled;

    @Schema(description = "当前工艺流程用途配置是否启用", example = "true")
    private Boolean routeConfigEnabled;

    @Schema(description = "批记录执行模式：SEQUENTIAL/PARALLEL", example = "SEQUENTIAL")
    private String executionMode;

    @Schema(description = "生产数量系数，工序计划数量=成品数量*生产数量系数", example = "3.000000")
    private BigDecimal productionQuantityFactor;

    @Schema(description = "用途内批记录报表列表")
    private List<MesProRouteFlowBatchRecordRespVO> batchRecordReports;

    @Schema(description = "用途内动态表单中心绑定列表")
    private List<MesProRouteFlowFormBindingRespVO> formBindings;

    @Schema(description = "基础工序默认批记录报表ID", example = "report-base-001")
    private String baseBatchRecordReportId;

    @Schema(description = "备注")
    private String remark;

}
