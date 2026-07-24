package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProBatchRecordParsedTable {

    private Integer sourceTableIndex;

    private Integer sourceTopLevelTableIndex;

    private Integer sourceSplitIndex;

    private String tableTitle;

    private Integer rowCount;

    private Integer columnCount;

    private List<Integer> columnWidths;

    private Boolean preserveSourceGrid;

    private Boolean routeBSource;

    private MesProBatchRecordDocumentFrame documentFrame;

    private List<List<MesProBatchRecordParsedCell>> rows;
}
