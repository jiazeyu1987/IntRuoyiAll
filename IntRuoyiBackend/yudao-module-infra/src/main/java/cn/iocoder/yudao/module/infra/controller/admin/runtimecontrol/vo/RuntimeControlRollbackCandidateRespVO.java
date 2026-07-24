package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 运行控制台回滚镜像候选 Response VO")
@Data
public class RuntimeControlRollbackCandidateRespVO {

    @Schema(description = "服务端生成候选编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String candidateId;

    @Schema(description = "备份点编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String backupId;

    @Schema(description = "发布包编号")
    private String releaseTag;

    @Schema(description = "镜像标签")
    private String imageTag;

    @Schema(description = "manifest 路径")
    private String manifestPath;

    @Schema(description = "正式服发布历史路径")
    private String prodHistoryPath;

    @Schema(description = "回滚兼容性状态")
    private String compatibilityStatus;

    @Schema(description = "回滚兼容性证据路径")
    private String compatibilityEvidencePath;

    @Schema(description = "回滚兼容性检查时间")
    private String compatibilityCheckedAt;

    @Schema(description = "回滚兼容性摘要")
    private String compatibilitySummary;

    @Schema(description = "候选状态：AVAILABLE/BLOCKED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @Schema(description = "阻断原因")
    private List<String> blockedReasons;
}
