package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;

import java.util.List;

@Data
public class MesProBatchRecordParseTable {

    private Integer tableIndex;

    private String title;

    private Integer rowCount;

    private Integer columnCount;

    private String templateCode;

    private String templateName;

    private String productName;

    private String sheetLayoutJson;

    private String metaJson;

    private List<MesProBatchRecordParseRow> rows;
}
