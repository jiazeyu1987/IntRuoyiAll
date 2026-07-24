package cn.iocoder.yudao.module.mdm.controller.admin.product.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MdmProductPageReqVO extends PageParam {

    private String keyword;
    private String productCode;
    private String dccProductCode;
    private String status;

}
