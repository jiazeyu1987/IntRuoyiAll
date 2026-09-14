package cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - MES QA 通用检验规程产品绑定 Request VO")
@Data
public class MesQaCommonRegulationBindReqVO {

    @Schema(description = "产品 QA 所选 DCC 项目代码 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "DCC 项目代码不能为空")
    private Long dccProjectCodeId;

    @Schema(description = "通用检验规程套版本 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "通用检验规程套版本不能为空")
    private Long commonRegulationSetVersionId;

    @Schema(description = "变更原因")
    private String changeReason;
}
