package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BatchRecordRepeatRowGroupRecordVO {

    private Integer recordSequence;
    private Integer startRowIndex;
    private Integer endRowIndex;
    private String recordKey;
}