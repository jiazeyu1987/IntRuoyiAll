package cn.iocoder.yudao.module.srm.controller.admin.framework.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SrmFrameworkAgreementPageReqVO extends PageParam {

    private String agreementNo;

    private String frameworkPlanNo;

    private String supplierName;
}
