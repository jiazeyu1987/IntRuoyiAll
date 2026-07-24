package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionTrackingEventRespVO {

    private Long eventId;
    private Long executionId;
    private String eventType;
    private String actionType;
    private String evidenceCategory;
    private String evidenceCategoryName;
    private String processInstanceId;
    private String bpmTaskId;
    private String taskDefinitionKey;
    private String nodeName;
    private Long actorId;
    private String actorName;
    private String result;
    private String comment;
    private String rejectReason;
    private Long signatureId;
    private LocalDateTime occurredAt;
}
