package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 运行控制台总览 Response VO")
@Data
public class RuntimeControlOverviewRespVO {

    @Schema(description = "环境列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> environments;

    @Schema(description = "组件列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> components;

    @Schema(description = "状态矩阵", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Map<String, RuntimeControlStatusRespVO>> statuses;
}
