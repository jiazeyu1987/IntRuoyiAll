package cn.iocoder.yudao.module.mdm.api.companyscope.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MdmRoleCompanyScopeCreateReqDTO {

    @NotNull
    private Long roleId;
    @NotNull
    private Long companyId;
    @NotBlank
    private String status;

}
