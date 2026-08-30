package cn.iocoder.yudao.module.mdm.controller.admin.companyscope.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MdmCompanyScopePageReqVO extends PageParam {

    private String scopeType;
    private Long companyId;
    private String status;
    private String keyword;
}
