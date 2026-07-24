package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProBatchRecordExecutionApprovalPageReqVO extends PageParam {

    private String executionCode;

    private String workOrderCode;

    private String batchCode;

    private Long submittedBy;

    private LocalDateTime submittedAtStart;

    private LocalDateTime submittedAtEnd;
}
