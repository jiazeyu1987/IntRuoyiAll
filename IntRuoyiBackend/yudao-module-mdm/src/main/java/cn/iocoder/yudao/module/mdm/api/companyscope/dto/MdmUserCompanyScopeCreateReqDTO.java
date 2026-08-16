package cn.iocoder.yudao.module.mdm.api.companyscope.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MdmUserCompanyScopeCreateReqDTO {

    @NotNull
    private Long userId;
    @NotNull
    private Long companyId;
    @NotBlank
    private String status;

}
