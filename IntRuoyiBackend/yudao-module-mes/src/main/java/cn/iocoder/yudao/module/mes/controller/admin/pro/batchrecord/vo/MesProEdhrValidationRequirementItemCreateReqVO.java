package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - eDHR 验证条目创建 Request VO")
@Data
public class MesProEdhrValidationRequirementItemCreateReqVO {

    @Schema(description = "验证包ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "验证包ID不能为空")
    private Long packageId;

    @Schema(description = "条目编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "URS-001")
    @NotBlank(message = "条目编号不能为空")
    private String itemCode;

    @Schema(description = "条目名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "用户需求1")
    @NotBlank(message = "条目名称不能为空")
    private String itemName;

    @Schema(description = "条目类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "URS")
    @NotBlank(message = "条目类型不能为空")
    private String itemType;

    @Schema(description = "条目版本", requiredMode = Schema.RequiredMode.REQUIRED, example = "v1")
    @NotBlank(message = "条目版本不能为空")
    private String itemVersion;

    @Schema(description = "条目状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "ACTIVE")
    @NotBlank(message = "条目状态不能为空")
    private String itemStatus;

    @Schema(description = "责任人", requiredMode = Schema.RequiredMode.REQUIRED, example = "验证负责人")
    @NotBlank(message = "责任人不能为空")
    private String ownerName;

    @Schema(description = "签核角色", requiredMode = Schema.RequiredMode.REQUIRED, example = "QA")
    @NotBlank(message = "签核角色不能为空")
    private String signoffRole;

    @Schema(description = "来源文档", requiredMode = Schema.RequiredMode.REQUIRED, example = "URS-001.docx")
    @NotBlank(message = "来源文档不能为空")
    private String sourceDocument;

    @Schema(description = "业务过程")
    private String businessProcess;

    @Schema(description = "验收标准")
    private String acceptanceCriteria;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;
}
