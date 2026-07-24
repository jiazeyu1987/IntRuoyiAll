package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - eDHR 追溯关系创建 Request VO")
@Data
public class MesProEdhrValidationTraceLinkCreateReqVO {

    @Schema(description = "验证包ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "验证包ID不能为空")
    private Long packageId;

    @Schema(description = "来源条目ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "11")
    @NotNull(message = "来源条目ID不能为空")
    private Long sourceItemId;

    @Schema(description = "目标条目ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "21")
    @NotNull(message = "目标条目ID不能为空")
    private Long targetItemId;

    @Schema(description = "追溯类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "URS_FRS")
    @NotBlank(message = "追溯类型不能为空")
    private String linkType;

    @Schema(description = "责任人", requiredMode = Schema.RequiredMode.REQUIRED, example = "验证负责人")
    @NotBlank(message = "责任人不能为空")
    private String ownerName;

    @Schema(description = "下一步动作", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "下一步动作不能为空")
    private String nextAction;

    @Schema(description = "备注")
    private String remark;
}
