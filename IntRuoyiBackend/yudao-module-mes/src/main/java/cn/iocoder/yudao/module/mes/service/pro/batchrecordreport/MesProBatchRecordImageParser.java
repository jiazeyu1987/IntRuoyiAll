package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import java.util.List;

public interface MesProBatchRecordImageParser {

    List<MesProBatchRecordParsedTable> parse(String originalFileName, byte[] bytes);
}
