package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.pdfbox.preflight.Format;
import org.apache.pdfbox.preflight.PreflightDocument;
import org.apache.pdfbox.preflight.ValidationResult;
import org.apache.pdfbox.preflight.parser.PreflightParser;
import org.apache.pdfbox.preflight.utils.ByteArrayDataSource;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrBatchArchivePdfAComplianceTest {

    private static final String FONT_PATH = "C:/Windows/Fonts/simhei.ttf";
    private static final String SYMBOL_FONT_PATH = "C:/Windows/Fonts/seguisym.ttf";

    @Test
    void render_producesValidPdfA1b() throws Exception {
        byte[] bytes = MesProEdhrBatchArchivePrintablePdfRenderer.render(
                validManifest().toJSONString(), FONT_PATH, SYMBOL_FONT_PATH);
        writeVisualSampleWhenRequested(bytes);

        ValidationResult result = validatePdfA1b(bytes);

        assertTrue(result.isValid(), () -> "PDF/A-1b validation errors: " + result.getErrorsList().stream()
                .map(error -> error.getErrorCode() + ":" + error.getDetails())
                .collect(Collectors.joining("; ")));
    }

    @Test
    void render_activeOrderPrintIncludesFrozenProductionParametersAndInspectionWithoutLegacyForms() throws Exception {
        JSONObject manifest = validManifest();
        manifest.put("bodyForms", new JSONArray());
        JSONObject event = new JSONObject().fluentPut("id", 901).fluentPut("routeProcessId", 101)
                .fluentPut("signatureId", 902).fluentPut("actualEmployeeId", 903)
                .fluentPut("serverSubmitTime", 1794111408000L)
                .fluentPut("rawPayload", "{\"outputQuantity\":10,\"lossQuantity\":0,\"inputMaterialDetails\":[{\"materialCode\":\"INPUT-001\",\"materialName\":\"投入材料\",\"batchCodes\":[\"LOT-001\"],\"batchEvidenceStatus\":\"CONFIRMED\"}],\"materialDetails\":[{\"materialName\":\"球囊\",\"outputQuantity\":10,\"lossQuantity\":0}],"
                        + "\"deviceParameterReadings\":[{\"parameterName\":\"压力\",\"value\":2,\"unit\":\"MPa\"}]}");
        JSONObject frozen = new JSONObject().fluentPut("productionFacts", new JSONObject()
                .fluentPut("events", new JSONArray().fluentAdd(event)).fluentPut("reviews", new JSONArray()))
                .fluentPut("snapshots", new JSONArray().fluentAdd(new JSONObject()
                        .fluentPut("routeProcessId", 101).fluentPut("processNameSnapshot", "装配")))
                .fluentPut("pqcTasks", new JSONArray().fluentAdd(new JSONObject().fluentPut("id", 904)
                        .fluentPut("routeProcessId", 101).fluentPut("roundNo", 2).fluentPut("inspectionType", "PATROL")))
                .fluentPut("pqcDetails", new JSONArray().fluentAdd(new JSONObject().fluentPut("routeProcessId", 101)
                        .fluentPut("itemName", "气密性").fluentPut("measuredValue", "合格")));
        manifest.put("activeOrderSource", new JSONObject().fluentPut("completionReceiptId", 905)
                .fluentPut("formalSourceSnapshot", frozen));

        byte[] bytes = MesProEdhrBatchArchivePrintablePdfRenderer.render(manifest.toJSONString(), FONT_PATH, SYMBOL_FONT_PATH);
        assertTrue(validatePdfA1b(bytes).isValid());
        try (PDDocument document = PDDocument.load(bytes)) {
            String text = new org.apache.pdfbox.text.PDFTextStripper().getText(document);
            assertTrue(text.contains("活跃订单生产记录"));
            assertTrue(text.contains("装配") && text.contains("球囊") && text.contains("压力 | 2 MPa"));
            assertTrue(text.contains("PQC检验记录") && text.contains("气密性") && text.contains("合格"));
            assertTrue(text.contains("2026-11-08 12:16:48") && !text.contains("1794111408000"));
            assertTrue(text.contains("投入物料") && text.contains("INPUT-001") && text.contains("LOT-001"));
            assertTrue(text.contains("完成数量：10") && text.contains("损耗数量：0"));
            org.junit.jupiter.api.Assertions.assertFalse(text.contains("当前批次没有可打印的已填表单"));
        }
    }

    private void writeVisualSampleWhenRequested(byte[] bytes) throws Exception {
        String outputPath = System.getProperty("mes.pdfa.visualSample");
        if (outputPath == null || outputPath.isBlank()) {
            return;
        }
        Path path = Path.of(outputPath).toAbsolutePath().normalize();
        Files.createDirectories(path.getParent());
        Files.write(path, bytes);
    }

    @Test
    void render_rejectsMissingPrintableLayout() {
        JSONObject manifest = validManifest();
        JSONObject form = manifest.getJSONArray("bodyForms").getJSONObject(0);
        form.remove("sheetLayoutJson");
        form.put("executionSnapshotJson", "{}");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> MesProEdhrBatchArchivePrintablePdfRenderer.render(
                        manifest.toJSONString(), FONT_PATH, SYMBOL_FONT_PATH));

        assertTrue(error.getMessage().contains("printable layout"));
    }

    @Test
    void validator_rejectsOrdinaryPdf() throws Exception {
        byte[] ordinaryPdf;
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(out);
            ordinaryPdf = out.toByteArray();
        }

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new MesProEdhrPdfAValidator().validateOrThrow(ordinaryPdf));

        assertTrue(error.getMessage().contains("PDF/A-1b"));
    }

    private ValidationResult validatePdfA1b(byte[] bytes) throws Exception {
        ByteArrayDataSource dataSource = new ByteArrayDataSource(new ByteArrayInputStream(bytes));
        PreflightParser parser = new PreflightParser(dataSource);
        parser.parse(Format.PDF_A1B);
        try (PreflightDocument document = parser.getPreflightDocument()) {
            document.validate();
            return document.getResult();
        }
    }

    private JSONObject validManifest() {
        JSONObject layout = new JSONObject();
        layout.put("rows", new JSONObject().fluentPut("1", new JSONObject()
                .fluentPut("cells", new JSONObject().fluentPut("1", new JSONObject().fluentPut("text", "批号")))));
        layout.put("cols", new JSONObject().fluentPut("1", new JSONObject().fluentPut("width", 180)));

        JSONObject form = new JSONObject();
        form.put("processName", "归档测试工序");
        form.put("batchRecordReportName", "生产记录");
        form.put("executionCode", "BRE-PDFA-001");
        form.put("submittedAt", "2026-08-31 00:00:00");
        form.put("sheetLayoutJson", layout.toJSONString());
        form.put("executionSnapshotJson", new JSONObject().fluentPut("layout", layout).toJSONString());
        form.put("cellValuesJson", "[]");
        form.put("signatureCellMarkers", new JSONArray());
        form.put("signatureRecords", new JSONArray());

        JSONObject manifest = new JSONObject();
        manifest.put("schemaVersion", "EDHR_BATCH_PRINTABLE_ARCHIVE_V1");
        manifest.put("batchCode", "BATCH-PDFA-001");
        manifest.put("routeName", "PDF/A 测试路线");
        manifest.put("routeCode", "RT-PDFA");
        manifest.put("generatedAt", "2026-08-31 00:00:00");
        manifest.put("aggregateHash", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        manifest.put("bodyForms", new JSONArray().fluentAdd(form));
        manifest.put("appendixSpecialNodes", new JSONArray());
        manifest.put("dossierItems", new JSONArray());
        manifest.put("changeEvents", new JSONArray());
        return manifest;
    }
}
