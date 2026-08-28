package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordReportDesignerContractTest {

    @Test
    void getEditPathBranchesToFormTemplateDesignerForVirtualReportIds() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/"
                        + "MesProBatchRecordReportServiceImpl.java"));

        assertTrue(source.contains("FORMTPL:"),
                "report service must recognize the formal form-template report prefix");
        assertTrue(source.contains("isFormTemplateReportId(reportId)"),
                "report service must detect virtual form-template report ids before metadata lookup");
        assertTrue(source.contains("ensureFormTemplateDesignerReport"),
                "report service must ensure the virtual designer report exists before opening it");
        assertTrue(source.contains("buildDesignerPath(reportId)"),
                "report service must still return the Jimu designer path after ensuring the virtual report");
    }

    @Test
    void jimuGatewaySynchronizesVirtualDesignerReportsBackToTemplateVersions() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/"
                        + "MesProBatchRecordJimuReportGatewayImpl.java"));

        assertTrue(source.contains("ensureFormTemplateDesignerReport"),
                "Jimu gateway must upsert a virtual designer report for form templates");
        assertTrue(source.contains("templateVersionMapper"),
                "Jimu gateway must be able to sync virtual report edits back to the template version");
        assertTrue(source.contains("updateReportJson"),
                "Jimu gateway must keep virtual report updates on the template version");
        assertTrue(source.contains("FORMTPL:"),
                "Jimu gateway must use the same formal form-template report prefix");
    }
}
