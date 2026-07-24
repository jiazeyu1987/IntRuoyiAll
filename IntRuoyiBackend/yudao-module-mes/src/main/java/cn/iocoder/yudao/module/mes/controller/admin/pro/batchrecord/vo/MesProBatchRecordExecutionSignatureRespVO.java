package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MesProBatchRecordExecutionSignatureRespVO {

    private Long id;

    private Long executionId;
    private String executionCode;

    private Long actorId;

    private String actorName;
    private String actorNickname;
    private String actorUsernameSnapshot;
    private String actorNicknameSnapshot;
    private Long actorDeptIdSnapshot;
    private String actorDeptNameSnapshot;
    private String actorPostNamesSnapshot;
    private String actorRoleNamesSnapshot;

    private String actionType;

    private String signatureMode;

    private Boolean passwordVerified;

    private String comment;

    private String processInstanceId;

    private String bpmTaskId;

    private String bpmTaskDefinitionKey;
    private String taskDefinitionKey;

    private String bpmTaskName;

    private String signatureCellKey;

    private Integer signatureRowIndex;

    private Integer signatureColumnIndex;

    private String reviewSourceType;

    private Long reviewSourceId;

    private String reviewSourceName;

    private String approvalResult;

    private String reason;
    private String meaningText;
    private String signaturePurpose;
    private String authorizationBasis;
    private String authenticationMethod;
    private String recordVersionSnapshot;
    private String recordHashSnapshot;
    private String clientIpSnapshot;
    private String userAgentSnapshot;
    private String snapshotStatus;

    private LocalDateTime signedAt;

    private LocalDateTime selectedSignedAt;

    private LocalDateTime signatureDisplayAt;

    private String signatureTimeMode;

    private String selectedTimeZone;

    private String selectedTimeReason;

    private String selectedTimePolicyVersion;

    private String selectedTimeAuditHash;
}
