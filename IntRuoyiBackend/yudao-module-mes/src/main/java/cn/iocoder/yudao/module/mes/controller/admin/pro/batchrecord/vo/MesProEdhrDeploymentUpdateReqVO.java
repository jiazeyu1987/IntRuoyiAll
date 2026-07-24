package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - eDHR 部署授权接口补证据 Request VO")
@Data
public class MesProEdhrDeploymentUpdateReqVO {

    @Schema(description = "部署证据ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "部署证据ID不能为空")
    private Long deploymentId;

    @Schema(description = "目标环境")
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

    @Schema(description = "发布标签")
    private String releaseTag;

    @Schema(description = "安装包或制品版本")
    private String artifactVersion;

    @Schema(description = "制品校验值")
    private String artifactChecksum;

    @Schema(description = "数据库结构版本")
    private String schemaVersion;

    @Schema(description = "迁移清单")
    private String migrationManifest;

    @Schema(description = "required SQL 清单")
    private String requiredSqlManifest;

    @Schema(description = "应用导入结果")
    private String appImportResult;

    @Schema(description = "授权范围")
    private String licenseScope;

    @Schema(description = "授权有效期")
    private LocalDate licenseValidUntil;

    @Schema(description = "授权文件证据")
    private String licenseFileEvidence;

    @Schema(description = "授权校验结果")
    private String licenseCheckResult;

    @Schema(description = "客户授权确认")
    private String customerLicenseConfirmation;

    @Schema(description = "接口范围")
    private String interfaceScope;

    @Schema(description = "接口版本")
    private String interfaceVersion;

    @Schema(description = "联调环境")
    private String integrationEnvironment;

    @Schema(description = "真实请求证据")
    private String requestEvidence;

    @Schema(description = "真实响应证据")
    private String responseEvidence;

    @Schema(description = "接口失败项数量")
    private Integer interfaceFailureCount;

    @Schema(description = "失败整改措施")
    private String remediationAction;

    @Schema(description = "复测证据")
    private String retestEvidence;

    @Schema(description = "接口确认人")
    private String interfaceConfirmedBy;
}
