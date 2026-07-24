package cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DccDistributionTaskPageReqVO extends PageParam {

    private Long categoryId;
    private String status;
}
