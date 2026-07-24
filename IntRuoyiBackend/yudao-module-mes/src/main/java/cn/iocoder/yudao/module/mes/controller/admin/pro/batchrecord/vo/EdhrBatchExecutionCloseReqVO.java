package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EdhrBatchExecutionCloseReqVO {

    @NotNull(message = "id 不能为空")
    private Long id;

    @NotBlank(message = "comment 不能为空")
    private String comment;

    @NotBlank(message = "password 不能为空")
    private String password;

    @Valid
    private MesProBatchRecordExecutionSignatureTimeReqVO signatureTime;
}
