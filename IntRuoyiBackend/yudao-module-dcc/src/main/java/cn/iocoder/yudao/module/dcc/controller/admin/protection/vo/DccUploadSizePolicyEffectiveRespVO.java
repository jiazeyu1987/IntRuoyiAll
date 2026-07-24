package cn.iocoder.yudao.module.dcc.controller.admin.protection.vo;

import lombok.Data;

@Data
public class DccUploadSizePolicyEffectiveRespVO {

    private Long policyId;
    private String policyCode;
    private String scopeType;
    private Long categoryId;
    private String purpose;
    private Long maxBytes;
    private String policyVersion;
    private Integer policyPriority;
    private Integer scopePriority;

}
