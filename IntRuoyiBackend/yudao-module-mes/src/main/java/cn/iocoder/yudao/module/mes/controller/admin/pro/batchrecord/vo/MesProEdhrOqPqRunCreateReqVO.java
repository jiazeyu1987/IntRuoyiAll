package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - eDHR OQ/PQ 执行创建 Request VO")
@Data
public class MesProEdhrOqPqRunCreateReqVO {

    @Schema(description = "验证包ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "验证包ID不能为空")
    private Long packageId;

    @Schema(description = "用例ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "用例ID不能为空")
    private Long caseId;

    @Schema(description = "执行环境", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "执行环境不能为空")
    private String executionEnvironment;

    @Schema(description = "发布标签", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "发布标签不能为空")
    private String releaseTag;

    @Schema(description = "数据库结构版本", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "数据库结构版本不能为空")
    private String schemaVersion;

    @Schema(description = "执行人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "执行人不能为空")
    private String executorName;

    @Schema(description = "复核人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "复核人不能为空")
    private String reviewerName;

    @Schema(description = "执行时间")
    private LocalDateTime executedAt;

    @Schema(description = "PQ真实业务路径")
    private String realBusinessPath;

    @Schema(description = "PQ真实测试数据来源")
    private String realTestDataSource;

    @Schema(description = "目标环境证明")
    private String targetEnvironmentProof;

    @Schema(description = "附件或证据标识", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "附件或证据标识不能为空")
    private String attachmentEvidence;

    @Schema(description = "证据校验值", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "证据校验值不能为空")
    private String evidenceChecksum;

    @Schema(description = "备注")
    private String remark;
}
