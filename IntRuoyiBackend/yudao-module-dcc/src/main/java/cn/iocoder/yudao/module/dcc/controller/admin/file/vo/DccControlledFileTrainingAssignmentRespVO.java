package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DccControlledFileTrainingAssignmentRespVO {

    private Long id;
    private Long userId;
    private String status;
    private LocalDateTime acknowledgedAt;
    private Integer accumulatedViewSeconds;
    private Integer requiredViewSeconds;
    private Boolean eligibleToAcknowledge;
}
