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
