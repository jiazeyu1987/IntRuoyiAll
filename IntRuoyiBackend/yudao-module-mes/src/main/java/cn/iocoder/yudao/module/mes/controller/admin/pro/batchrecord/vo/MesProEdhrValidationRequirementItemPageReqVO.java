package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - eDHR 验证条目分页 Request VO")
@Data
public class MesProEdhrValidationRequirementItemPageReqVO extends PageParam {

    @Schema(description = "验证包ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "验证包ID不能为空")
    private Long packageId;

    @Schema(description = "条目编号", example = "URS-001")
    private String itemCode;

    @Schema(description = "条目类型", example = "URS")
    private String itemType;

    @Schema(description = "条目状态", example = "ACTIVE")
    private String itemStatus;
}
