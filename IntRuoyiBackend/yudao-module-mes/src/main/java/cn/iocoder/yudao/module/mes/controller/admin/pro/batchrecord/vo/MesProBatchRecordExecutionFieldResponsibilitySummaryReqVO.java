package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionResponsibilityEvidenceStatus;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionResponsibilityValueOrigin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO {

    @NotNull(message = "executionId 不能为空")
    private Long executionId;

    @NotNull(message = "pageNo 不能为空")
    @Min(value = 1, message = "pageNo 必须大于等于 1")
    private Integer pageNo = 1;

    @NotNull(message = "pageSize 不能为空")
    @Min(value = 1, message = "pageSize 必须大于等于 1")
    @Max(value = 200, message = "pageSize 不能超过 200")
    private Integer pageSize = 50;

    private String fieldKeyword;

    private MesProBatchRecordExecutionResponsibilityEvidenceStatus evidenceStatus;

    private MesProBatchRecordExecutionResponsibilityValueOrigin valueOrigin;

    private Long actorId;
}
