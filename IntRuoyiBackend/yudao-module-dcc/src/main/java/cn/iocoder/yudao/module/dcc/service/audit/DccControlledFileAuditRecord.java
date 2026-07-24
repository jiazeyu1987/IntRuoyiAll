package cn.iocoder.yudao.module.dcc.service.audit;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DccControlledFileAuditRecord {

    private Long id;
    private Long accessEventId;
    private String accessEventCode;
    private String watermarkTraceCode;
    private Long controlledFileId;
    private String fileNumber;
    private String fileVersionNo;
    private Long userId;
    private String userIdentifier;
    private String userDisplayName;
    private Long deptId;
    private String deptName;
    private String tenantName;
    private String actionType;
    private String purpose;
    private String result;
    private String failureCode;
    private String reason;
    private String sourceIp;
    private String requestId;
    private String userAgent;
    private String privacyMode;
    private String watermarkPayloadJson;
    private LocalDateTime occurredAt;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createTime;

}
