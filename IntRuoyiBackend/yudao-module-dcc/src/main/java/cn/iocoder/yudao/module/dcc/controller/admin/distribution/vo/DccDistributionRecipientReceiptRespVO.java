package cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/** Independent persisted send/open/sign facts for one exact version obligation. */
@Data
public class DccDistributionRecipientReceiptRespVO {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long controlledFileId;
    private String versionNo;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long distributionId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long recipientId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long departmentId;
    /** 当前接收部门名称；签名时部门仍使用独立快照字段。 */
    private String departmentName;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;
    private String currentUserName;
    private String signerNameSnapshot;
    private String signerDepartmentNameSnapshot;
    private String messageStatus;
    private String errorMessage;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private LocalDateTime acknowledgedAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long signatureId;
    private LocalDateTime signedAt;
    private LocalDateTime distributionCompletedAt;
}
