package cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo;
import lombok.Data;
import java.time.LocalDateTime;
/** Exact-version batch outcome; an empty recipient list is never delivery success evidence. */
@Data
public class DccAutomaticDistributionResultRespVO {
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long batchId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long controlledFileId;
    private String versionNo;
    private String approvalProcessInstanceId;
    private String eventKey;
    private String status;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime failureAt;
    private LocalDateTime preparedAt;
    private LocalDateTime sentAt;
    private LocalDateTime snapshotAt;
    private String policyVersion;
}
