package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - MES 工艺路线工序流程配置保存 Request VO")
@Data
public class MesProRouteFlowProcessConfigSaveReqVO {

    @Schema(description = "工艺路线工序ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "工艺路线工序不能为空")
    private Long routeProcessId;

    @Schema(description = "当前用途是否启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;

    @Schema(description = "批记录执行模式：SEQUENTIAL/PARALLEL", example = "SEQUENTIAL")
    private String executionMode;

    @Schema(description = "生产数量系数，工序计划数量=成品数量*生产数量系数，默认 1", example = "3.000000")
    private BigDecimal productionQuantityFactor;

    @Schema(description = "用途内批记录报表列表")
    @Valid
    private List<MesProRouteFlowBatchRecordSaveReqVO> batchRecordReports;

    @Schema(description = "用途内动态表单中心绑定列表")
    @Valid
    private List<MesProRouteFlowFormBindingSaveReqVO> formBindings;

    @Schema(description = "是否显式保存过批记录表单绑定快照")
    private Boolean batchRecordBindingSnapshotExplicit;

    @Schema(description = "备注")
    private String remark;

}
