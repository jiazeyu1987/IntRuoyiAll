package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 运行控制台运维动作 Request VO")
@Data
public class RuntimeControlActionReqVO {

    @Schema(description = "动作", requiredMode = Schema.RequiredMode.REQUIRED, example = "publish-test")
    @NotBlank(message = "动作不能为空")
    private String action;

    @Schema(description = "操作原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;

    @Schema(description = "生产确认文本", example = "PROD")
    private String prodConfirmText;

    @Schema(description = "运维目标环境：backup-now 支持 test/prod，restore-data 支持 test/backup", example = "test")
    private String targetEnvironment;

    @Schema(description = "备份类型：FULL/INCREMENTAL，仅 backup-now 使用", example = "FULL")
    private String backupKind;

    @Schema(description = "发布范围：固定 app-release，仅发布程序包且不包含数据库或对象存储数据", example = "app-release")
    private String publishScope;

    @Schema(description = "构建发布包时是否包含 OnlyOffice；构建发布包必填", example = "false")
    private Boolean includeOnlyOffice;

    @Schema(description = "构建发布包时是否包含 Website/展厅构筑包；构建发布包必填", example = "false")
    private Boolean includeShowroomBuildPackage;

    @Schema(description = "是否启用 Smart Release Phase 1 report-only 报告/预检", example = "true")
    private Boolean enableSmartReleaseReport;

    @Schema(description = "发布包编号", example = "20260528_220000")
    private String releaseTag;

    @JsonIgnore
    private String sourceSelectionId;

    @JsonIgnore
    private String expectedMaintenanceCommit;

    @JsonIgnore
    private String expectedApplicationCommit;

    @JsonIgnore
    private String expectedFrontendCommit;

    @JsonIgnore
    private String releaseWorkflowId;

    @JsonIgnore
    private Long releaseWorkflowExpectedStateVersion;

    @JsonIgnore
    private String preassignedOperationId;

    @Schema(description = "测试验证结论；标记测试通过必填")
    private String testConclusion;

    @Schema(description = "测试验收结果；标记测试通过固定为 PASS")
    private String testResult;

    @Schema(description = "测试服数据库快应用 SQL 文件路径", example = "D:/tmp/test-db-hotfix.sql")
    private String sqlPath;

    @Schema(description = "回滚目标镜像标签，内部脚本参数，由服务端候选解析；请求传入将被拒绝")
    private String selectedImageTag;

    @Schema(description = "回滚目标镜像候选编号")
    private String selectedImageCandidateId;

    @Schema(description = "恢复数据使用的备份点，内部脚本参数，由服务端候选解析；请求传入将被拒绝")
    private String selectedBackupId;

    @Schema(description = "恢复数据使用的恢复集候选编号")
    private String selectedRecoverySetCandidateId;

    @Schema(description = "恢复集编号，内部脚本参数，由服务端候选解析；请求传入将被拒绝")
    private String recoverySetId;

    @Schema(description = "恢复集 manifest SHA-256，内部脚本参数，由服务端候选解析；请求传入将被拒绝")
    private String recoverySetManifestHash;

    @Schema(description = "恢复集程序版本，内部脚本参数，由服务端候选解析；请求传入将被拒绝")
    private String recoverySetProgramVersion;

    @Schema(description = "恢复集 Redis 策略，内部脚本参数，由服务端候选解析；请求传入将被拒绝")
    private String recoverySetRedisPolicy;

    @JsonIgnore
    private String testOperationId;

    @JsonIgnore
    private String testOperationEvidencePath;
}
