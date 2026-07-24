package cn.iocoder.yudao.module.srm.controller.admin.framework.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SrmFrameworkPlanPageReqVO extends PageParam {

    private String frameworkPlanNo;

    private String planTitle;

    private String supplierName;

    private String planStatus;
}
