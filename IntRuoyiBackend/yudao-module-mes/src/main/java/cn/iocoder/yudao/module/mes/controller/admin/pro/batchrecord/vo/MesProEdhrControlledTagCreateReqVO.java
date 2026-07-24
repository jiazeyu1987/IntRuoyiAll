package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - MES eDHR 受控标签创建 Request VO")
@Data
public class MesProEdhrControlledTagCreateReqVO {

    @Schema(description = "标签编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "标签编码不能为空")
    private String tagCode;

    @Schema(description = "标签名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "标签名称不能为空")
    private String tagName;

    @Schema(description = "标签类型")
    private String tagType;

    @Schema(description = "备注")
    private String remark;
}
