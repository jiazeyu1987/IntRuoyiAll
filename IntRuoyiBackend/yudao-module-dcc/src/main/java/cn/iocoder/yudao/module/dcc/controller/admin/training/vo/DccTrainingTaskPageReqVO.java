package cn.iocoder.yudao.module.dcc.controller.admin.training.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DccTrainingTaskPageReqVO extends PageParam {

    private Long categoryId;
    private String status;
}
