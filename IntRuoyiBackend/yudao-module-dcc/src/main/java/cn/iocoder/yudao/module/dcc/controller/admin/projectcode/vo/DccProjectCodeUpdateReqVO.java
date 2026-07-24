package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - DCC 项目代码更新 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class DccProjectCodeUpdateReqVO extends DccProjectCodeSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "编号不能为空")
    private Long id;
}
