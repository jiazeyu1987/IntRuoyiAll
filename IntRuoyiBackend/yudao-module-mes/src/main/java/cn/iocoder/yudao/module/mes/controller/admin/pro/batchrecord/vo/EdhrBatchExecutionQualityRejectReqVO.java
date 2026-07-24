package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EdhrBatchExecutionQualityRejectReqVO {

    @NotNull(message = "批次执行 ID 不能为空")
    private Long id;

    @NotBlank(message = "拒收原因不能为空")
    private String reason;

    @NotBlank(message = "签名密码不能为空")
    private String password;

    @Valid
    private MesProBatchRecordExecutionSignatureTimeReqVO signatureTime;
}
