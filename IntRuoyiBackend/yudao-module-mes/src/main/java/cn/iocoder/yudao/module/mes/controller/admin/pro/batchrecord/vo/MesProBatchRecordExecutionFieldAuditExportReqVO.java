package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldAuditExportReqVO extends MesProBatchRecordExecutionFieldAuditPageReqVO {

    @NotBlank(message = "format 不能为空")
    private String format;
}
