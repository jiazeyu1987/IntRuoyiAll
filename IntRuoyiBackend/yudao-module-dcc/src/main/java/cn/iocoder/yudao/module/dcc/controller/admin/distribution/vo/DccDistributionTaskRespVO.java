package cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DccDistributionTaskRespVO {

    private Long recipientId;
    private Long distributionId;
    private Long controlledFileId;
    private Long categoryId;
    private String fileName;
    private String title;
    private String fileNumber;
    private String versionNo;
    private String fileStatus;
    private Long userId;
    private Long departmentId;
    private String distributionMedium;
    private LocalDateTime readAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime publishedTime;
    private String status;
}
