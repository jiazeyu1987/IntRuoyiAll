package cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;
import lombok.Data;

/** Only an expected exact batch identity. Policy decisions and roster values are server-owned. */
@Data
public class DccAutomaticDistributionRetryReqVO {
    @NotNull @Positive private Long controlledFileId;
    @NotNull @Positive private Long batchId;
    @NotBlank @Size(max=64) private String versionNo;
    @NotBlank @Size(max=128) private String approvalProcessInstanceId;
    @JsonAnySetter
    public void rejectUnexpectedField(String name,Object value) {
        throw new IllegalArgumentException("自动下发重试只接受精确批次身份，不接受额外字段："+name);
    }
}
