package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MesProBatchRecordExecutionCreateReqVO {

    @NotNull(message = "templateId 不能为空")
    private Long templateId;

    @NotNull(message = "workOrderId 不能为空")
    private Long workOrderId;

    @NotBlank(message = "batchCode 不能为空")
    private String batchCode;
}
