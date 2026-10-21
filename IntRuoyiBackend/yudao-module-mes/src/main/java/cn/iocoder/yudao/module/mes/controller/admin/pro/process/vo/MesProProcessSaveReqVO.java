package cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - MES 生产工序修改 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class MesProProcessSaveReqVO extends MesProProcessCreateReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "工序编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "GX-0001")
    @NotBlank(message = "工序编码不能为空")
    private String code;
}
