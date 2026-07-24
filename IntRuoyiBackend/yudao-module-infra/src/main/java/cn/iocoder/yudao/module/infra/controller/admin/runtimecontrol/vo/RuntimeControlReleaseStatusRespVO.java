package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 运行控制台发布状态只读快照 Response VO")
@Data
public class RuntimeControlReleaseStatusRespVO {

    @Schema(description = "发布包候选")
    private List<RuntimeControlReleasePackageRespVO> releasePackages;

    @Schema(description = "目标环境状态")
    private Map<String, Map<String, RuntimeControlStatusRespVO>> targetStates;

    @Schema(description = "最近发布操作")
    private List<RuntimeControlOperationRespVO> recentOperations;

    @Schema(description = "当前测试服发布包")
    private String testCurrentReleaseTag;

    @Schema(description = "最近已测试通过发布包")
    private String latestTestedReleaseTag;
}
