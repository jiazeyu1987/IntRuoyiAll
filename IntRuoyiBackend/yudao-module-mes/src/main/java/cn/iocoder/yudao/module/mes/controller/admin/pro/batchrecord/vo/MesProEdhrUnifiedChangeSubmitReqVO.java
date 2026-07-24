package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrUnifiedChangeSubmitReqVO {

    @NotNull(message = "变更申请不能为空")
    private Long changeRequestId;

    @NotBlank(message = "提交原因不能为空")
    private String reason;

    @NotBlank(message = "签核证据不能为空")
    private String signoffEvidenceHash;

    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;
}
