package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 运行控制台决策向导推荐 Response VO")
@Data
public class RuntimeControlWizardRecommendationRespVO {

    @Schema(description = "场景编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String scenario;

    @Schema(description = "推荐动作", requiredMode = Schema.RequiredMode.REQUIRED)
    private String recommendedAction;

    @Schema(description = "推荐动作名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String recommendedActionLabel;

    @Schema(description = "所需证据")
    private List<String> requiredEvidence;

    @Schema(description = "必需责任人角色")
    private List<String> requiredOwnerRoles;

    @Schema(description = "阻断原因")
    private List<String> blockingReasons;

    @Schema(description = "回滚候选")
    private List<RuntimeControlRollbackCandidateRespVO> rollbackCandidates;

    @Schema(description = "恢复候选")
    private List<RuntimeControlRestoreCandidateRespVO> restoreCandidates;
}
