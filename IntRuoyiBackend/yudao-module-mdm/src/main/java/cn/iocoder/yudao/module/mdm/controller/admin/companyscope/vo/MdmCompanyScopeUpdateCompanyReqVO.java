package cn.iocoder.yudao.module.mdm.controller.admin.companyscope.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 修改所属公司名称 Request VO")
@Data
public class MdmCompanyScopeUpdateCompanyReqVO {

    @Schema(description = "公司编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择公司")
    @Positive(message = "公司编号不合法")
    private Long id;

    @Schema(description = "公司名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入公司名称")
    @Size(max = 255, message = "公司名称不能超过255个字符")
    private String name;
}
