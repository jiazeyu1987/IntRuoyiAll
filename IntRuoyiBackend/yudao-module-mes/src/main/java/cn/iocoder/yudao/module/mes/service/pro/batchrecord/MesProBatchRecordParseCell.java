package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;

@Data
public class MesProBatchRecordParseCell {

    private Integer columnIndex;

    private String text;

    private Integer rowSpan;

    private Integer colSpan;
}
