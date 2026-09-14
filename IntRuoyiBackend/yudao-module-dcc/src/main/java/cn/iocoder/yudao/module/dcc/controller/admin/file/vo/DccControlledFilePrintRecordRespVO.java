package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DCC controlled print record response.
 */
@Data
public class DccControlledFilePrintRecordRespVO {

    private Long id;
    private Long controlledFileId;
    private String fileNumber;
    private String versionNo;
    private String printNo;
    private String purpose;
    private Integer copies;
    private String receivingDepartment;
    private String useLocation;
    private Long printUserId;
    private String printUserName;
    private LocalDateTime printTime;
    private String approvalStatus;
    private Long approvalUserId;
    private String approvalUserName;
    private LocalDateTime approvalTime;

}
