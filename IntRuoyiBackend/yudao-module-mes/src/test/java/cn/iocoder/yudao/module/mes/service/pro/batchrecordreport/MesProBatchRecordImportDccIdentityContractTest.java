package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordImportDccIdentityContractTest {

    private static final Path CONTROLLER = Path.of(
            "src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecordreport/"
                    + "MesProBatchRecordReportController.java");
    private static final Path REPORT_SERVICE = Path.of(
            "src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/"
                    + "MesProBatchRecordReportServiceImpl.java");
    private static final Path GENERATION_SERVICE = Path.of(
            "src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/"
                    + "MesProBatchRecordRouteGenerationServiceImpl.java");

    @Test
    void uploadedWord_requiresExplicitDccProjectCodeIdentity() throws Exception {
        String source = read(CONTROLLER);

        assertTrue(source.contains("@RequestParam(\"dccProjectCodeId\") Long dccProjectCodeId"),
                "Word 导入预检和提交必须接收明确的 DCC 项目代码 ID。");
    }

    @Test
    void routeGovernance_usesFormalRouteDccBinding() throws Exception {
        String source = read(REPORT_SERVICE);

        assertTrue(source.contains("routeDccProjectBindingMapper"),
                "Word 导入必须从正式路线-DCC绑定读取路线治理关系。");
        assertFalse(source.contains("selectEnabledListByProjectName(projectName)"),
                "Word 导入不得再按产品名称枚举 DCC 项目代码。");
        assertFalse(source.contains("projectCode.getProjectCode()), StrUtil.trim(item.getCode())"),
                "Word 导入不得再用物料编码等值匹配 DCC 项目代码。");
    }

    @Test
    void routeGeneration_usesSelectedDccIdentityAndFormalBinding() throws Exception {
        String source = read(GENERATION_SERVICE);

        assertTrue(source.contains("dccProjectCodeId"),
                "路线生成必须接收用户选择的 DCC 项目代码 ID。");
        assertTrue(source.contains("routeDccProjectBindingMapper"),
                "新建路线必须写入正式路线-DCC绑定，升级路线必须校验该绑定。");
        assertFalse(source.contains("selectEnabledListByProjectName(productName)"),
                "路线生成不得再通过产品名称枚举多个 DCC 项目代码。");
    }

    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
