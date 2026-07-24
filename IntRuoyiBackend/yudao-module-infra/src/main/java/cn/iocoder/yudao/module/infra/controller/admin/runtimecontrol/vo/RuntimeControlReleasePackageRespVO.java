package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 运行控制台发布包候选 Response VO")
@Data
public class RuntimeControlReleasePackageRespVO {

    @Schema(description = "发布包编号（NAS 目录名）", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "26-05-29_21-05-42")
    private String releaseTag;

    @Schema(description = "发布包目录名")
    private String packageDirectoryName;

    @Schema(description = "release-manifest.json 路径")
    private String manifestPath;

    @Schema(description = "构建时间")
    private String builtAt;

    @Schema(description = "发布范围")
    private String publishScope;

    @Schema(description = "发布包组件范围：full/intruoyi/backend/frontend/website")
    private String component;

    @Schema(description = "是否包含 Website/展厅构筑包")
    private Boolean includeShowroomBuildPackage;

    @Schema(description = "是否包含 OnlyOffice")
    private Boolean onlyOfficeIncluded;

    @Schema(description = "镜像标签")
    private String imageTag;

    @Schema(description = "是否存在完整 checksum")
    private Boolean checksumPresent;

    @Schema(description = "是否已标记测试通过")
    private Boolean tested;

    @Schema(description = "测试通过时间")
    private String testedAt;

    @Schema(description = "测试标记操作人")
    private String operatorName;

    @Schema(description = "测试通过绑定的恢复集候选编号")
    private String testedRecoverySetCandidateId;

    @Schema(description = "测试通过绑定的恢复集编号")
    private String testedRecoverySetId;

    @Schema(description = "测试通过绑定的恢复集 manifest SHA-256")
    private String testedRecoverySetManifestHash;

    @Schema(description = "候选状态：AVAILABLE/BLOCKED")
    private String status;

    @Schema(description = "阻断原因")
    private List<String> blockedReasons;
}
