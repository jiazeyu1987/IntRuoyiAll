package cn.iocoder.yudao.module.mdm.controller.admin.companyscope.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Schema(description = "管理后台 - 企业公司范围保存 Request VO")
@Data
public class MdmCompanyScopeSaveReqVO {

    @Schema(description = "授权记录编号，更新时必填")
    @Positive(message = "授权记录不合法")
    private Long id;

    @Schema(description = "范围类型：USER、ROLE", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请选择授权对象类型")
    private String scopeType;

    @Schema(description = "授权对象编号：用户编号或角色编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择授权对象")
    @Positive(message = "请选择授权对象")
    private Long principalId;

    @Schema(description = "授权公司编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择授权公司")
    @Positive(message = "请选择授权公司")
    private Long companyId;

    @Schema(description = "状态：ENABLE、DISABLE", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请选择状态")
    private String status;
}
