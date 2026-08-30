package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProEdhrNonconformanceReviewPageReqVO extends PageParam {

    private String reviewCode;

    private String sourceType;

    private Long batchExecutionId;

    private String batchExecutionCode;

    private String workOrderCode;

    private String batchCode;

    private String reviewStatus;

    private String disposition;
}
