package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

final class BatchRecordReportTestFixtures {

    private static final String PRESSURE_PUMP_RECORD_DOC = "/fixtures/pressure-pump-record.doc";

    private BatchRecordReportTestFixtures() {
    }

    static Path pressurePumpRecordDoc() {
        return fixturePath(PRESSURE_PUMP_RECORD_DOC);
    }

    private static Path fixturePath(String resourcePath) {
        URL resource = BatchRecordReportTestFixtures.class.getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("missing fixture " + resourcePath);
        }
        try {
            return Path.of(resource.toURI());
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("invalid fixture URI " + resourcePath, ex);
        }
    }
}
