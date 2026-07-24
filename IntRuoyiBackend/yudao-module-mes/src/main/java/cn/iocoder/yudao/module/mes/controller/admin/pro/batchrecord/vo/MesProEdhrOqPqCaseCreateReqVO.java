package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - eDHR OQ/PQ 用例创建 Request VO")
@Data
public class MesProEdhrOqPqCaseCreateReqVO {

    @Schema(description = "验证包ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "验证包ID不能为空")
    private Long packageId;

    @Schema(description = "用例编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用例编号不能为空")
    private String caseCode;

    @Schema(description = "用例名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用例名称不能为空")
    private String caseName;

    @Schema(description = "用例类型：OQ、PQ", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用例类型不能为空")
    private String caseType;

    @Schema(description = "用例版本", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用例版本不能为空")
    private String caseVersion;

    @Schema(description = "步骤编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "步骤编号不能为空")
    private String stepNo;

    @Schema(description = "步骤标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "步骤标题不能为空")
    private String stepTitle;

    @Schema(description = "预期结果", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "预期结果不能为空")
    private String expectedResult;

    @Schema(description = "证据要求", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "证据要求不能为空")
    private String evidenceRequirement;

    @Schema(description = "责任人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "责任人不能为空")
    private String ownerName;

    @Schema(description = "复核人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "复核人不能为空")
    private String reviewerName;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;
}
