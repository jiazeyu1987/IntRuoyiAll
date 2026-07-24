package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldResponsibilityExportReqVO {

    @NotNull(message = "executionId 不能为空")
    private Long executionId;

    @Pattern(regexp = "XLSX", message = "format 仅支持 XLSX")
    private String format = "XLSX";
}
