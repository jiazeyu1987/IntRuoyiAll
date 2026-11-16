package cn.iocoder.yudao.module.mdm.controller.admin.companyscope.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 新建自动可用所属公司 Request VO")
@Data
public class MdmCompanyScopeCreateCompanyReqVO {

    @Schema(description = "公司名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入公司名称")
    @Size(max = 255, message = "公司名称不能超过255个字符")
    private String name;
}
