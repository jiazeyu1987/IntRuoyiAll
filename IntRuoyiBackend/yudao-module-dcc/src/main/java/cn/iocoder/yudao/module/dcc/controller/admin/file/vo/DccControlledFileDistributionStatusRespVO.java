package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

import java.util.List;

@Data
public class DccControlledFileDistributionStatusRespVO {

    private Long id;
    private Long departmentId;
    private String distributionMedium;
    private String status;
    private Long acknowledgedBy;
    private java.time.LocalDateTime acknowledgedAt;
    private Long recoveredBy;
    private java.time.LocalDateTime recoveredAt;
    private List<Long> recipientUserIds;
    private List<DccControlledFileDistributionRecipientStatusRespVO> recipients;
}
