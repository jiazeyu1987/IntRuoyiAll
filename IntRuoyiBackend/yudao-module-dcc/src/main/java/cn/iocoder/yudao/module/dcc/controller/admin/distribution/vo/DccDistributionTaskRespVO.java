package cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

@Data
public class DccDistributionTaskRespVO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long recipientId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long distributionId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long controlledFileId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long categoryId;
    private String fileName;
    private String title;
    private String fileNumber;
    private String versionNo;
    private String fileStatus;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long departmentId;
    private String distributionMedium;
    private LocalDateTime readAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime publishedTime;
    private String status;
}
