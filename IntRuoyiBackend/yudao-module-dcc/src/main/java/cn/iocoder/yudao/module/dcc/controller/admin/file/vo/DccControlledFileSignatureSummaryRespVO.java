package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DccControlledFileSignatureSummaryRespVO {

    private Long id;
    private String taskId;
    private Long actorId;
    private String actionType;
    private String taskActionResult;
    private Long revisionId;
    private String versionNo;
    private String meaningCode;
    private String controlledCopyHashStatus;
    private String evidenceStatus;
    private String evidenceHashShort;
    private String actorUsernameSnapshot;
    private String actorNicknameSnapshot;
    private Long actorDeptIdSnapshot;
    private String actorDeptNameSnapshot;
    private String actorPostNamesSnapshot;
    private String actorRoleNamesSnapshot;
    private String signaturePurpose;
    private String authorizationBasis;
    private String authenticationMethod;
    private String recordVersionSnapshot;
    private String recordHashSnapshot;
    private String clientIpSnapshot;
    private String userAgentSnapshot;
    private String snapshotStatus;
    private String signatureMode;
    private String comment;
    private LocalDateTime signedAt;
}
