package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DccProjectCodeAssignmentRespVO {

    private Long id;
    private String assignmentNo;
    private Long projectCodeId;
    private String projectName;
    private String projectCode;
    private String scopeMode;
    private Long assigneeUserId;
    private String assigneeNickname;
    private Long assignedBy;
    private LocalDateTime assignedTime;
    private LocalDateTime expireTime;
    private String status;
    private String assignmentReason;
    private Integer fileCount;
    private Integer changedFileCount;
    private Integer changedFieldCount;
    private Long revokedBy;
    private LocalDateTime revokedTime;
    private String revokeReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
