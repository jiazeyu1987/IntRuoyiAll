package cn.iocoder.yudao.module.mdm.api.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmProductRespDTO {

    private Long id;
    private String productCode;
    private String dccProductCode;
    private String nameCn;
    private String nameEn;
    private String modelSpecification;
    private String category;
    private String status;

}
