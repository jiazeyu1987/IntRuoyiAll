package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DccControlledFileUploadTemporaryStatusRespVO {

    private String requestId;
    private Integer temporaryFileCount;
    private Boolean bindable;
    private String sessionId;
    private String purpose;
    private String status;
    private LocalDateTime expireTime;
    private String cleanupStatus;
    private String cleanupReason;
    private LocalDateTime cleanupTime;
    private Integer cleanedCount;

}
