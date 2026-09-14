package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordRouteIdentityContractTest {

    private static final Path REPORT_SERVICE = Path.of(
            "src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/"
                    + "MesProBatchRecordReportServiceImpl.java");
    private static final Path GENERATION_SERVICE = Path.of(
            "src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/"
                    + "MesProBatchRecordRouteGenerationServiceImpl.java");

    @Test
    void wordImport_resolvesGovernedRouteOnlyThroughFormalProductBinding() throws Exception {
        String source = read(REPORT_SERVICE);

        assertFalse(source.contains("routeMapper.selectListByName(projectName)"),
                "Word 导入预检不得按 DCC 项目名称猜测工艺路线。");
        assertTrue(source.contains("dccProjectCodeMapper.selectEnabledListByProjectName(projectName)"),
                "Word 导入预检必须从 DCC 项目代码关系开始定位产品。");
        assertTrue(source.contains("routeProductMapper.selectListByItemIds(dccProductItemIds)"),
                "Word 导入预检必须通过正式路线产品绑定定位路线。");
    }

    @Test
    void wordImport_withoutFrozenRouteId_neverReusesRouteByName() throws Exception {
        String source = read(GENERATION_SERVICE);

        assertFalse(source.contains("routeMapper.selectListByName(routeName)"),
                "Word 导入写入不得按批记录名称定位已有工艺路线。");
        assertTrue(source.contains("routeMapper.selectById(expectedRouteId)"),
                "已有路线升级只能使用预检冻结的 routeId 精确定位。");
    }

    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
