package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BatchRecordCellLinkCellVO {

    private Integer rowIndex;
    private Integer columnIndex;
    private String cellKey;
    private String sourceType;
    private String sourceFieldCode;
    private String sourceFieldName;
    private String label;
    private String valueType;
    private String componentFlag;
    private Boolean required;
    private Boolean readonly;
    private Boolean signatureCell;
    private Boolean linkableAsSource;
    private Boolean linkableAsTarget;
}
