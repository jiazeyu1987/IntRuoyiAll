package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import java.nio.file.Path;
import java.util.List;

public interface MesProBatchRecordRouteRecognizer {

    String routeKey();

    List<MesProBatchRecordParsedTable> recognize(Path sourcePath, byte[] sourceBytes, String originalFileName);
}
