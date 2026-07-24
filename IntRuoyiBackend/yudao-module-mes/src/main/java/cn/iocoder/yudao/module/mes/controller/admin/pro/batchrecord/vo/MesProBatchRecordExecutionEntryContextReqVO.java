package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionEntryContextReqVO {

    @NotNull(message = "workOrderId 不能为空")
    private Long workOrderId;

    private Long routeId;

    private Long processId;

    private Long routeProcessId;

    private Long taskId;

    private Long workstationId;

    @NotNull(message = "batchRecordReportId 不能为空")
    private String batchRecordReportId;

    private String batchCode;
}
