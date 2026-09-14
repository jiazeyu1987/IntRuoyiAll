package cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - MES QA 通用检验规程套保存 Request VO")
@Data
public class MesQaCommonRegulationSetSaveReqVO {

    @Schema(description = "通用检验规程套 ID")
    private Long id;

    @Schema(description = "套编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "通用检验规程套编号不能为空")
    private String setCode;

    @Schema(description = "套名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "通用检验规程套名称不能为空")
    private String setName;

    @Schema(description = "套状态：ENABLED/DISABLED")
    private String setStatus;

    @Schema(description = "备注")
    private String remark;
}
