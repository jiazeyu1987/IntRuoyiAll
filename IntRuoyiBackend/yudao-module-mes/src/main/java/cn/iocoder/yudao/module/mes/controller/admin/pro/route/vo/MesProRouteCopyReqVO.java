package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - MES 工艺路线复制 Request VO")
@Data
public class MesProRouteCopyReqVO {

    @Schema(description = "源路线编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "源路线编号不能为空")
    private Long sourceRouteId;

    @Schema(description = "新路线编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "A1-COPY")
    @NotBlank(message = "新路线编码不能为空")
    private String targetCode;

    @Schema(description = "新路线名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "A1-副本")
    @NotBlank(message = "新路线名称不能为空")
    private String targetName;

}
