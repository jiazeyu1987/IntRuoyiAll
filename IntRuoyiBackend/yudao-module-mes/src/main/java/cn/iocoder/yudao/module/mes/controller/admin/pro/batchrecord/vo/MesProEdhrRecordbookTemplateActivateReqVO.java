package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - MES eDHR 记录本模板启用 Request VO")
@Data
public class MesProEdhrRecordbookTemplateActivateReqVO {

    @Schema(description = "模板 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "模板 ID 不能为空")
    private Long id;

    @Schema(description = "备注")
    private String remark;
}
