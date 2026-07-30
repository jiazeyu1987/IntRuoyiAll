package cn.iocoder.yudao.module.mes.service.pro.processpool;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessPoolTimelineRevisionSummaryTest {

    @Test
    void timelineMapperReadsRevisionSummaryWithoutWriteActions() throws Exception {
        String xml = Files.readString(Path.of("src", "main", "resources", "mapper",
                "pro", "processpool", "MesProProcessPoolTimelineReadMapper.xml"), StandardCharsets.UTF_8);

        assertTrue(xml.contains("mes_pro_process_pool_event_revision"));
        assertTrue(xml.contains("modificationHistorySummary"));
        assertTrue(xml.contains("原始记录已修改"));
        assertTrue(xml.contains("原始记录暂无修改"));
        assertFalse(xml.contains("updateOriginalRecord("));
        assertFalse(xml.contains("createReviewCopy("));
    }
}
