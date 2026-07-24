package cn.iocoder.yudao.module.dcc.controller.admin.training.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DccTrainingExecutionRespVO {

    private Long progressId;
    private Long controlledFileId;
    private Long categoryId;
    private String fileName;
    private String title;
    private String fileNumber;
    private String versionNo;
    private String fileStatus;
    private Long userId;
    private List<Long> departmentIds;
    private Integer requiredViewSeconds;
    private Integer accumulatedViewSeconds;
    private Boolean eligibleToAcknowledge;
    private LocalDateTime firstViewedAt;
    private LocalDateTime lastViewedAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime publishedTime;
    private String status;
}
