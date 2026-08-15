package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BatchRecordRepeatRowGroupMappingVO {

    private String sourceType;
    private String sourceFieldCode;
    private String sourceFieldName;
    private String sourceValueType;
    private Integer templateTargetRowIndex;
    private Integer templateTargetColumnIndex;
    private String templateTargetCellKey;
    private String targetValueType;
    private String projectionTargetCellKey;
}