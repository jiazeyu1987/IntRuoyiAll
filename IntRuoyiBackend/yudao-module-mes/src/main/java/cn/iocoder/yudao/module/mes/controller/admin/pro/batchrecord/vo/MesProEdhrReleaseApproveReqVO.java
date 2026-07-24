package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrReleaseApproveReqVO {

    @NotNull(message = "放行事务不能为空")
    private Long releaseTransactionId;

    @NotBlank(message = "批准幂等键不能为空")
    private String idempotencyKey;

    @NotBlank(message = "签核证据不能为空")
    private String signoffEvidenceHash;

    private String approvalOpinion;
}
