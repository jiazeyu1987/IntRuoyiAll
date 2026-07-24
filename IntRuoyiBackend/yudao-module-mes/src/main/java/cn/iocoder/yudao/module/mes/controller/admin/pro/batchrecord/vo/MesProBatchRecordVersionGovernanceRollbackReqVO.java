package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordVersionGovernanceRollbackReqVO {

    @NotNull(message = "批记录定义不能为空")
    private Long definitionId;

    @NotNull(message = "回滚目标版本不能为空")
    private Long targetVersionId;

    @NotBlank(message = "回滚原因不能为空")
    private String reason;

    @NotBlank(message = "影响面摘要不能为空")
    private String impactSummaryJson;

    @NotBlank(message = "签核证据不能为空")
    private String signoffEvidenceHash;

    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;
}
