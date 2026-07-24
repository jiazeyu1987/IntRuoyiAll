package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - eDHR 部署授权接口证据创建 Request VO")
@Data
public class MesProEdhrDeploymentCreateReqVO {

    @Schema(description = "交付项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "交付项目ID不能为空")
    private Long projectId;

    @Schema(description = "部署证据名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "部署证据名称不能为空")
    private String deploymentName;

    @Schema(description = "客户项目名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "客户项目名称不能为空")
    private String customerProjectName;

    @Schema(description = "目标环境", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "目标环境不能为空")
    private String targetEnvironment;

    @Schema(description = "是否具备环境授权")
    private Boolean environmentAuthorized;

    @Schema(description = "环境检查摘要")
    private String environmentCheckSummary;

    @Schema(description = "服务器检查")
    private String serverSummary;

    @Schema(description = "网络检查")
    private String networkSummary;

    @Schema(description = "对象存储检查")
    private String objectStorageSummary;

    @Schema(description = "容量检查")
    private String capacitySummary;

    @Schema(description = "权限检查")
    private String permissionSummary;

    @Schema(description = "发布标签", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "发布标签不能为空")
    private String releaseTag;

    @Schema(description = "安装包或制品版本")
    private String artifactVersion;

    @Schema(description = "制品校验值")
    private String artifactChecksum;

    @Schema(description = "数据库结构版本", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "数据库结构版本不能为空")
    private String schemaVersion;

    @Schema(description = "迁移清单")
    private String migrationManifest;

    @Schema(description = "required SQL 清单")
    private String requiredSqlManifest;

    @Schema(description = "应用导入结果")
    private String appImportResult;

    @Schema(description = "备注")
    private String remark;
}

