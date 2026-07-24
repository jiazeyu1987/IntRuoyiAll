package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DccControlledFileDistributionRecipientStatusRespVO {

    private Long id;
    private Long userId;
    private LocalDateTime readAt;
    private LocalDateTime acknowledgedAt;
    private String ackComment;

}
