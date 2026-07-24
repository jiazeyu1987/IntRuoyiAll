package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionCellValueVO {

    @NotNull(message = "rowIndex 不能为空")
    private Integer rowIndex;

    @NotNull(message = "columnIndex 不能为空")
    private Integer columnIndex;

    @NotNull(message = "value 不能为空")
    private Object value;

    private String valueType;

    private String valueDisplay;

    private String valueHash;

    private String unit;
}
