package cn.iocoder.yudao.module.srm.controller.admin.contract.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SrmProcurementContractPageReqVO extends PageParam {

    private String contractNo;

    private String contractTitle;

    private String sourceType;

    private Long supplierId;

    private String contractStatus;
}
