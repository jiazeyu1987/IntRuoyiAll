package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DccProjectCodeAssignmentAuditRespVO {

    private Long id;
    private Long changeId;
    private Long assignmentId;
    private String assignmentNo;
    private Long projectCodeId;
    private String projectName;
    private String projectCode;
    private Long controlledFileId;
    private String fileNumber;
    private String fileName;
    private Long operatorUserId;
    private String operatorNickname;
    private String fieldName;
    private String fieldLabel;
    private String oldValueText;
    private String newValueText;
    private String source;
    private String changeReason;
    private LocalDateTime changedTime;

}
