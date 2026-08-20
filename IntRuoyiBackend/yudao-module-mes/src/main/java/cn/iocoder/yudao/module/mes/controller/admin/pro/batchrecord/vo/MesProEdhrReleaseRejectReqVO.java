package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrReleaseRejectReqVO {

    @NotNull(message = "放行事务不能为空")
    private Long releaseTransactionId;

    @NotBlank(message = "驳回幂等键不能为空")
    private String idempotencyKey;

    @NotBlank(message = "驳回原因不能为空")
    private String rejectReason;

    public MesProEdhrReleaseRejectReqVO setReleaseTransactionId(Long releaseTransactionId) {
        this.releaseTransactionId = releaseTransactionId;
        return this;
    }

    public MesProEdhrReleaseRejectReqVO setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
        return this;
    }

    public MesProEdhrReleaseRejectReqVO setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
        return this;
    }
}
