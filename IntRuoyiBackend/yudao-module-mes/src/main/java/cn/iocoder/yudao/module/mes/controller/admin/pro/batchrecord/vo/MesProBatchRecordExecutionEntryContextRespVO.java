package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionEntryContextRespVO {

    private Long workOrderId;

    private Long routeId;

    private String routeCode;

    private String routeName;

    private Long processId;

    private String processCode;

    private String processName;

    private Long routeProcessId;

    private Long taskId;

    private Long workstationId;

    private String workstationCode;

    private String workstationName;

    private String batchRecordReportId;

    private String batchRecordReportCode;

    private String batchRecordReportName;

    private String batchCode;

    private Boolean canOpen;

    private Boolean bindingResolved;

    private Long activeExecutionId;

    private Integer activeExecutionStatus;

    private String activeContextKey;
}
