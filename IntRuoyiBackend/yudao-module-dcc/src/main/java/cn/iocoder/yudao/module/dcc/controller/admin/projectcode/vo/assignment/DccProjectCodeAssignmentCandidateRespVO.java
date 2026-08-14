package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment;

import lombok.Data;

@Data
public class DccProjectCodeAssignmentCandidateRespVO {

    private Long id;
    private Long masterId;
    private String fileName;
    private String fileNumber;
    private String versionNo;
    private String status;
    private Long currentProjectCodeId;
    private String currentProjectName;
    private String currentProjectCode;
    private Boolean selectable;
    private String disabledReason;
}
