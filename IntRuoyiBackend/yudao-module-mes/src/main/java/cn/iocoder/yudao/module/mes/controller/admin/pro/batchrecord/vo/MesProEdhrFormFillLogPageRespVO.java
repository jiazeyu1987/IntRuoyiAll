package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrFormFillLogPageRespVO {

    private Long auditBatchId;
    private Long executionId;
    private String executionCode;
    private String batchRecordReportId;
    private String formName;
    private Long batchExecutionId;
    private String batchCode;
    private String workOrderCode;
    private Long actorId;
    private String actorName;
    private LocalDateTime changedAt;
    private Integer fieldCount;
    private String cellSummary;
    private String contextStatus;
    private String hashStatus;
}
