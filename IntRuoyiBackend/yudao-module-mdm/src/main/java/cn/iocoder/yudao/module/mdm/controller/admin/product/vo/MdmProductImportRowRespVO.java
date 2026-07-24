package cn.iocoder.yudao.module.mdm.controller.admin.product.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmProductImportRowRespVO {

    private Integer rowNo;
    private String productCode;
    private String dccProductCode;
    private String nameCn;
    private String nameEn;
    private String modelSpecification;
    private String category;
    private String currentStatus;
    private String importAction;
    private String failureReason;

}
