package cn.iocoder.yudao.module.mdm.controller.admin.product.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MdmProductSaveReqVO {

    private Long id;

    @NotBlank(message = "productCode is required")
    private String productCode;

    private String dccProductCode;

    @NotBlank(message = "nameCn is required")
    private String nameCn;

    private String nameEn;

    private String modelSpecification;

    private String category;

    private String status;

}
