package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DccProjectCodeControlledFilePageReqVO extends PageParam {

    private String keyword;
    private String status;
}
