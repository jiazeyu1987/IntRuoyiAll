package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;

@Data
public class MesProBatchRecordExecutionPageReqVO extends PageParam {

    private Long templateId;

    private Long workOrderId;

    private Long batchExecutionId;

    private String instanceScope;

    private String sharedFormKey;

    private Long routeProcessId;

    private Long taskId;

    private Long workstationId;

    private String batchRecordReportId;

    private String activeContextKey;

    private String batchCode;

    private Integer status;
}
