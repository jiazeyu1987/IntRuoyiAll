package cn.iocoder.yudao.module.dcc.controller.admin.protection.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DccUploadSizePolicyRespVO {

    private Long id;
    private String policyCode;
    private String scopeType;
    private Long categoryId;
    private String purpose;
    private Long maxBytes;
    private Boolean enabled;
    private Integer priority;
    private String policyVersion;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private String changeReason;
    private LocalDateTime createTime;

}
