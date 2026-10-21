package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordRouteGenerationCodeRuleTest {

    private static final Path SOURCE = Path.of(
            "src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/"
                    + "MesProBatchRecordRouteGenerationServiceImpl.java");

    @Test
    void generatedProcessCode_usesSharedAllocator() throws Exception {
        String source = Files.readString(SOURCE, StandardCharsets.UTF_8);

        assertTrue(source.contains("processCreationService.create(process)"));
        assertFalse(source.contains("PROCESS_CODE_PREFIX"));
        assertTrue(source.contains("routeProcessFlowEdgeMapper.insert"),
                "eDHR 自动生成路线必须同步写入工序流转边。");
        assertFalse(source.contains(".nextProcessId("),
                "eDHR 自动生成路线不得继续写入旧的 nextProcessId 链字段。");
    }
}
