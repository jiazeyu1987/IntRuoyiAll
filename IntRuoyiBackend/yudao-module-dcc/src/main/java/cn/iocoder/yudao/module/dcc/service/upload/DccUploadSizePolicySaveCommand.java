package cn.iocoder.yudao.module.dcc.service.upload;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DccUploadSizePolicySaveCommand {

    private String policyCode;
    private String scopeType;
    private Long categoryId;
    private String purpose;
    private Long maxBytes;
    private Boolean enabled;
    private String policyVersion;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private String changeReason;

}
