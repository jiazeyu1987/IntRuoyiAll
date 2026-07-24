package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DccPaperDistributionRecordRespVO {

    private Long distributionId;
    private Long controlledFileId;
    private String fileNumber;
    private String fileName;
    private String versionNo;
    private Long issuerUserId;
    private String issuerName;
    private List<Long> recipientUserIds;
    private List<String> recipientNames;
    private LocalDateTime issuedAt;
    private Long recovererUserId;
    private String recovererName;
    private LocalDateTime recoveredAt;
    private String status;

}
