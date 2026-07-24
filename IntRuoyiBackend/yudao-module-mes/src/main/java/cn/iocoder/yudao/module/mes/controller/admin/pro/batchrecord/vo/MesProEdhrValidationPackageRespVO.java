package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - eDHR 验证包 Response VO")
@Data
@Accessors(chain = true)
public class MesProEdhrValidationPackageRespVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "验证包编码")
    private String packageCode;

    @Schema(description = "验证包名称")
    private String packageName;

    @Schema(description = "客户项目名称")
    private String customerProjectName;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "客户现场")
    private String siteName;

    @Schema(description = "系统范围")
    private String systemScope;

    @Schema(description = "验证范围")
    private String validationScope;

    @Schema(description = "发布标签")
    private String releaseTag;

    @Schema(description = "数据库结构版本")
    private String schemaVersion;

    @Schema(description = "目标环境")
    private String targetEnvironment;

    @Schema(description = "验证包状态")
    private String validationStatus;

    @Schema(description = "是否具备OQ Ready")
    private Boolean oqReady;

    @Schema(description = "验证负责人")
    private String validationOwnerName;

    @Schema(description = "QA负责人")
    private String qaOwnerName;

    @Schema(description = "阻断原因")
    private String blockedReason;

    @Schema(description = "追溯矩阵摘要JSON")
    private String traceSummaryJson;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
