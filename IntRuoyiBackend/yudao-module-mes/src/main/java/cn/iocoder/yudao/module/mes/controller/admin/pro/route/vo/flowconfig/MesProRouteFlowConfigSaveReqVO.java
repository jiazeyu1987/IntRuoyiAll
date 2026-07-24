package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - MES 工艺流程配置保存 Request VO")
@Data
public class MesProRouteFlowConfigSaveReqVO {

    @Schema(description = "工艺路线ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "工艺路线不能为空")
    private Long routeId;

    @Schema(description = "路线版本编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "路线版本编号不能为空")
    private Long routeVersionId;

    @Schema(description = "用途类型，由专用保存接口写入", example = "SCHEDULE")
    private String useType;

    @Schema(description = "流程配置版本", example = "ROUTE-20260610-SCHEDULE-V1")
    private String configVersion;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "工序流程配置列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "工序流程配置不能为空")
    @Valid
    private List<MesProRouteFlowProcessConfigSaveReqVO> processConfigs;

}
