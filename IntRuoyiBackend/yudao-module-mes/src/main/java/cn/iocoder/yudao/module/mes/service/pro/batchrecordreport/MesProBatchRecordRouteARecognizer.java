package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class MesProBatchRecordRouteARecognizer implements MesProBatchRecordRouteRecognizer {

    public static final String ROUTE_KEY = MesProBatchRecordRecognitionRouteKeys.A;

    private final MesProBatchRecordDocParser docParser;

    public MesProBatchRecordRouteARecognizer(MesProBatchRecordDocParser docParser) {
        this.docParser = docParser;
    }

    @Override
    public String routeKey() {
        return ROUTE_KEY;
    }

    @Override
    public List<MesProBatchRecordParsedTable> recognize(Path sourcePath, byte[] sourceBytes, String originalFileName) {
        return recognize(sourceBytes);
    }

    public List<MesProBatchRecordParsedTable> recognize(byte[] bytes) {
        return docParser.parse(bytes);
    }
}
