package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EdhrBatchVoidApprovalResolutionRespVO {

    private Long policyId;
    private String policyMode;
    private Boolean requiresBpm;
    private String bpmProcessKey;
    private String effectExecutorCode;

}
