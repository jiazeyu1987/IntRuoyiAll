package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolProductionReportRevisionLogRespVO;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProcessPoolProductionReportRevisionLogContractTest {

    private static final Path CONTROLLER = Path.of("src/main/java/cn/iocoder/yudao/module/mes/"
            + "controller/admin/pro/processpool/MesProProcessPoolEventRevisionController.java");

    @Test
    void exposesReadableLogEndpointWithCurrentUserScope() throws IOException {
        String source = Files.readString(CONTROLLER);

        assertTrue(source.contains("@GetMapping(\"/production-report-logs\")"));
        assertTrue(source.contains("productionReportRevisionLogService.getLogs(eventId, getLoginUserId())"));
        assertTrue(source.contains("@PreAuthorize(\"@ss.hasPermission('mes:pro-process-pool-team-leader:query')\")"));
    }

    @Test
    void responseContainsOnlyReadableAuditFields() {
        Set<String> topLevelFields = Stream.of(ProcessPoolProductionReportRevisionLogRespVO.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of("modifiedByName", "modifiedAt", "changeReason", "signatureConfirmed", "changes"),
                topLevelFields);

        Set<String> changeFields = Stream.of(
                        ProcessPoolProductionReportRevisionLogRespVO.FieldChangeRespVO.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of("fieldName", "beforeValue", "afterValue"), changeFields);
    }
}
