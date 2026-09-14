package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DccPublicationNotificationDeliveryRespVO {
    private String id;
    private String batchId;
    private String userId;
    private String userName;
    private String deptName;
    private String candidateResolutionStatus;
    private String status;
    private Integer attemptCount;
    private LocalDateTime sentAt;
    private String systemMessageId;
    private String lastErrorSummary;
    private Integer rowVersion;
    private List<String> reasonSummaries;
}
