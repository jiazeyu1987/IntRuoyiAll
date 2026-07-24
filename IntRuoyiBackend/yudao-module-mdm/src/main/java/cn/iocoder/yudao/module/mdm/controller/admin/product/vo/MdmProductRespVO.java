package cn.iocoder.yudao.module.mdm.controller.admin.product.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmProductRespVO {

    private Long id;
    private String productCode;
    private String dccProductCode;
    private String nameCn;
    private String nameEn;
    private String modelSpecification;
    private String category;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
