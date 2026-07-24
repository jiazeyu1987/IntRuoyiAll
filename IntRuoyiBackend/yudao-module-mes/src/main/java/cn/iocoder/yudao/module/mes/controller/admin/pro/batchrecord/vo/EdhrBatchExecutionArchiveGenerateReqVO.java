package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EdhrBatchExecutionArchiveGenerateReqVO {

    @NotNull(message = "batchExecutionId 不能为空")
    private Long batchExecutionId;

    @NotBlank(message = "artifactType 不能为空")
    private String artifactType;

    @NotNull(message = "workTaskId 不能为空")
    private Long workTaskId;
}
