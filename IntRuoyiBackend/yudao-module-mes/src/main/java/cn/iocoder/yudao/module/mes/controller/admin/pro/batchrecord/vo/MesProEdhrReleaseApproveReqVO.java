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

    private Long workTaskId;

    private Integer expectedVersion;

    @NotBlank(message = "批准幂等键不能为空")
    private String idempotencyKey;

    @NotBlank(message = "签核证据不能为空")
    private String signoffEvidenceHash;

    private String approvalOpinion;

    public MesProEdhrReleaseApproveReqVO setReleaseTransactionId(Long releaseTransactionId) {
        this.releaseTransactionId = releaseTransactionId;
        return this;
    }

    public MesProEdhrReleaseApproveReqVO setWorkTaskId(Long workTaskId) {
        this.workTaskId = workTaskId;
        return this;
    }

    public MesProEdhrReleaseApproveReqVO setExpectedVersion(Integer expectedVersion) {
        this.expectedVersion = expectedVersion;
        return this;
    }

    public MesProEdhrReleaseApproveReqVO setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
        return this;
    }

    public MesProEdhrReleaseApproveReqVO setSignoffEvidenceHash(String signoffEvidenceHash) {
        this.signoffEvidenceHash = signoffEvidenceHash;
        return this;
    }

    public MesProEdhrReleaseApproveReqVO setApprovalOpinion(String approvalOpinion) {
        this.approvalOpinion = approvalOpinion;
        return this;
    }
}
