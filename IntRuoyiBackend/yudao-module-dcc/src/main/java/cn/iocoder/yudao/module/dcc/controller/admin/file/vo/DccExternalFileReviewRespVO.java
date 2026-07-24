package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DccExternalFileReviewRespVO {

    private Long controlledFileId;
    private String externalSource;
    private String externalOwner;
    private String reviewReason;
    private List<Long> participantUserIds;
    private String reviewConclusion;
    private String conclusionComment;
    private LocalDateTime closedTime;
}
