package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BatchRecordRepeatRowGroupMappingSaveReqVO {

    @NotBlank(message = "来源类型不能为空")
    private String sourceType;
    @NotBlank(message = "来源字段不能为空")
    private String sourceFieldCode;
    private String sourceFieldName;
    private String sourceValueType;
    @NotNull(message = "模板目标行不能为空")
    private Integer templateTargetRowIndex;
    @NotNull(message = "模板目标列不能为空")
    private Integer templateTargetColumnIndex;
    private String templateTargetCellKey;
    private String targetValueType;
}