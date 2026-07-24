package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class MesProBatchRecordExecutionSaveDraftReqVO {

    @NotNull(message = "id 不能为空")
    private Long id;

    @NotNull(message = "cellValues 不能为空")
    @Valid
    private List<MesProBatchRecordExecutionCellValueVO> cellValues;

    private String remark;
}
