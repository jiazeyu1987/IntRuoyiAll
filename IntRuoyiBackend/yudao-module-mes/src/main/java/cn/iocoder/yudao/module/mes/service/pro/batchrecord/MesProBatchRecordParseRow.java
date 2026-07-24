package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;

import java.util.List;

@Data
public class MesProBatchRecordParseRow {

    private Integer rowIndex;

    private List<MesProBatchRecordParseCell> cells;
}
