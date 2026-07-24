package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - MES eDHR 受控标签状态 Request VO")
@Data
public class MesProEdhrControlledTagStatusReqVO {

    @Schema(description = "标签 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "标签 ID 不能为空")
    private Long id;

    @Schema(description = "备注")
    private String remark;
}
