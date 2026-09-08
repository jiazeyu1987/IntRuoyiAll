package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DccPublicationTimelineEventRespVO {
    private String eventId;
    private Integer sequenceNo;
    private String sourceType;
    private String sourceLabel;
    private String actionLabel;
    private LocalDateTime occurredAt;
    private String actorId;
    private String objectId;
    private String objectLabel;
    private String statusBeforeLabel;
    private String statusAfterLabel;
    private String decisionLabel;
    private List<String> directionLabels;
    private String reason;
    private String errorSummary;
    private String systemMessageId;
    private Integer attemptCount;
    private String assigneeBefore;
    private String assigneeAfter;
    private String linkedRevisionControlledFileId;
    private String linkedRevisionVersion;
}
