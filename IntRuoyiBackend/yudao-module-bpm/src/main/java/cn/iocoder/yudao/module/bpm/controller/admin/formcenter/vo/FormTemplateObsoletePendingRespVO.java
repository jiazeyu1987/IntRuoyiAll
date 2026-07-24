package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import lombok.Data;

@Data
public class FormTemplateObsoletePendingRespVO {

    private Long approvalRequestId;

    private String approvalProcessInstanceId;

    private Long applicantUserId;

    private Boolean canWithdraw;

    private String objectState;

    private String status;

    private String reason;

}
