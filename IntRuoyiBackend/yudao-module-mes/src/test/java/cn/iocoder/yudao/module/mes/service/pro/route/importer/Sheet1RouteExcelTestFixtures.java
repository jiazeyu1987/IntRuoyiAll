package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;

final class Sheet1RouteExcelTestFixtures {

    private static final String BALLOON_CATHETER_PROCESS_WORKBOOK =
            "fixtures/sheet1-route-balloon-catheter.xlsx";

    private Sheet1RouteExcelTestFixtures() {
    }

    static byte[] balloonCatheterProcessWorkbookBytes() throws IOException {
        ClassLoader classLoader = Sheet1RouteExcelTestFixtures.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(BALLOON_CATHETER_PROCESS_WORKBOOK)) {
            if (inputStream == null) {
                throw new NoSuchFileException("src/test/resources/" + BALLOON_CATHETER_PROCESS_WORKBOOK);
            }
            return inputStream.readAllBytes();
        }
    }
}
