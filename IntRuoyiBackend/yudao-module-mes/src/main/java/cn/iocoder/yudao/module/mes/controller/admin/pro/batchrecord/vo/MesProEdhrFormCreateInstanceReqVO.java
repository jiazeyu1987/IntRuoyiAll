package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - MES eDHR 独立表单实例创建 Request VO")
@Data
public class MesProEdhrFormCreateInstanceReqVO {

    @Schema(description = "模板 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "模板 ID 不能为空")
    private Long templateId;

    @Schema(description = "业务范围")
    private String businessScope;

    @Schema(description = "业务对象类型")
    private String businessObjectType;

    @Schema(description = "业务对象 ID")
    private Long businessObjectId;

    @Schema(description = "业务对象编码")
    private String businessObjectCode;

    @Schema(description = "备注")
    private String remark;
}
