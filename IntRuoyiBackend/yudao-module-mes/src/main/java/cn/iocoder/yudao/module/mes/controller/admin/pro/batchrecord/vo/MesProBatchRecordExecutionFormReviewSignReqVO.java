package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFormReviewSignReqVO {

    @NotNull(message = "executionId 不能为空")
    private Long executionId;

    @NotNull(message = "workTaskId 不能为空")
    private Long workTaskId;

    @NotBlank(message = "password 不能为空")
    private String password;

    private String comment;

    @Valid
    private MesProBatchRecordExecutionSignatureTimeReqVO signatureTime;
}
