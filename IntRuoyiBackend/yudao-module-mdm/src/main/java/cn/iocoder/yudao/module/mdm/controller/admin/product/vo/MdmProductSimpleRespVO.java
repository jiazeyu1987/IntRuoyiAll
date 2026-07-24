package cn.iocoder.yudao.module.mdm.controller.admin.product.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmProductSimpleRespVO {

    private Long id;
    private String productCode;
    private String dccProductCode;
    private String nameCn;
    private String nameEn;
    private String modelSpecification;
    private String category;
    private String status;

}
