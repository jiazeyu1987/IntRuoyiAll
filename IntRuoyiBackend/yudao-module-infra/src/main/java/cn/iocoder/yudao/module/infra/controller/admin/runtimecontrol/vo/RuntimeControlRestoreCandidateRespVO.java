package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 运行控制台恢复备份候选 Response VO")
@Data
public class RuntimeControlRestoreCandidateRespVO {

    @Schema(description = "服务端生成候选编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String candidateId;

    @Schema(description = "备份点编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String backupId;

    @Schema(description = "镜像标签")
    private String imageTag;

    @Schema(description = "恢复集编号")
    private String recoverySetId;

    @Schema(description = "恢复集状态：COMPLETE/BLOCKED")
    private String recoverySetStatus;

    @Schema(description = "恢复集程序版本")
    private String programVersion;

    @Schema(description = "Redis 恢复策略")
    private String redisPolicy;

    @Schema(description = "运行配置清单路径")
    private String configurationManifestPath;

    @Schema(description = "运行编排配置路径")
    private String configurationComposePath;

    @Schema(description = "恢复集 manifest SHA-256")
    private String recoverySetManifestHash;

    @Schema(description = "恢复集组件摘要")
    private Map<String, String> componentSummary;

    @Schema(description = "DCC 备份模式：baseline/incremental")
    private String dccBackupMode;

    @Schema(description = "DCC 增量链状态")
    private String dccChainStatus;

    @Schema(description = "DCC 当前段增删改复用统计")
    private Map<String, String> dccChangeSummary;

    @Schema(description = "manifest 路径")
    private String manifestPath;

    @Schema(description = "checksum 路径")
    private String checksumPath;

    @Schema(description = "恢复演练报告路径")
    private String rehearsalReportPath;

    @Schema(description = "现场快照路径")
    private String snapshotPath;

    @Schema(description = "候选状态：AVAILABLE/BLOCKED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @Schema(description = "阻断原因")
    private List<String> blockedReasons;
}
