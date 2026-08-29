package cn.iocoder.yudao.module.mdm.controller.admin.enterprise.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MdmEnterpriseSaveReqVO {

    private Long id;

    @NotBlank(message = "enterpriseCode is required")
    private String enterpriseCode;

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "type is required")
    private String type;

    @NotBlank(message = "status is required")
    private String status;

}
