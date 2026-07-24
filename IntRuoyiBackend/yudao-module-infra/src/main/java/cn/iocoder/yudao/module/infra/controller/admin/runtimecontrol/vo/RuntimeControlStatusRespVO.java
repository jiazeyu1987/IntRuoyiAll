package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 运行控制台组件状态 Response VO")
@Data
public class RuntimeControlStatusRespVO {

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "running")
    private String status;

    @Schema(description = "HTTP 状态", example = "HTTP 200")
    private String httpStatus;

    @Schema(description = "运行态", example = "running")
    private String runtimeState;

    @Schema(description = "访问地址", example = "http://127.0.0.1:8081/")
    private String url;

    @Schema(description = "端口", example = "8081")
    private Integer port;

    @Schema(description = "当前运行发布包", example = "26-05-29_21-05-42")
    private String currentReleaseTag;

    @Schema(description = "最近操作")
    private RuntimeControlOperationRespVO lastOperation;

    @Schema(description = "是否允许操作", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean actionEnabled;

    @Schema(description = "禁用原因")
    private String blockedReason;
}
