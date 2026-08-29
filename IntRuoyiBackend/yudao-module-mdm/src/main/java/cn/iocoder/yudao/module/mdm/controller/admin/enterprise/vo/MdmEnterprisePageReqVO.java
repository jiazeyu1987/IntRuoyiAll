package cn.iocoder.yudao.module.mdm.controller.admin.enterprise.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MdmEnterprisePageReqVO extends PageParam {

    private String keyword;
    private String enterpriseCode;
    private String name;
    private String type;
    private String status;

}
