package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldAuditDetailReqVO {

    @NotNull(message = "executionId 不能为空")
    private Long executionId;

    private Long auditBatchId;

    private Long auditItemId;
}
