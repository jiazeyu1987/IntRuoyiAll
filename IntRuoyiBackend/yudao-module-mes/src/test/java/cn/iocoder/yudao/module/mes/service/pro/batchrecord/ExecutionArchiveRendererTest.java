package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutionArchiveRendererTest {

    @Test
    void pdfRenderer_generatesRealPdfBytesAndDigestFromRenderContext() {
        MesProBatchRecordExecutionArchiveRenderResult result = new PdfExecutionArchiveRenderer().render(validContext());

        assertEquals("application/pdf", result.getContentType());
        assertEquals("EDHR_ARCHIVE_V1", result.getRenderSourceVersion());
        assertEquals(result.getContent().length, result.getFileSize());
        assertEquals(ExecutionArchiveRendererSupport.sha256(result.getContent()), result.getSha256());
        assertTrue(result.getFileName().startsWith("EDHR-EXE_001-20260524093000"));
        assertArrayEquals("%PDF".getBytes(StandardCharsets.US_ASCII), firstBytes(result.getContent(), 4));
    }

    @Test
    void pdfRenderer_printsFormReviewSignatureMeaningInFinalArchive() throws Exception {
        MesProBatchRecordExecutionArchiveRenderContext context = validContext();
        context.setSignatures(List.of(
                MesProBatchRecordExecutionSignatureDO.builder()
                        .id(101L)
                        .executionId(11L)
                        .actorId(7L)
                        .actionType("SUBMIT")
                        .signatureMode("PASSWORD")
                        .passwordVerified(true)
                        .signedAt(LocalDateTime.of(2026, 5, 24, 9, 20))
                        .comment("submit approved")
                        .build(),
                MesProBatchRecordExecutionSignatureDO.builder()
                        .id(102L)
                        .executionId(11L)
                        .actorId(8L)
                        .actionType("FORM_REVIEW")
                        .signatureMode("PASSWORD")
                        .passwordVerified(true)
                        .signedAt(LocalDateTime.of(2026, 5, 24, 9, 25))
                        .selectedSignedAt(LocalDateTime.of(2026, 5, 24, 9, 10))
                        .signatureDisplayAt(LocalDateTime.of(2026, 5, 24, 9, 10))
                        .signatureTimeMode("USER_SELECTED")
                        .selectedTimeZone("Asia/Shanghai")
                        .selectedTimeReason("复核签名按线下完成时间显示")
                        .selectedTimeAuditHash("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc")
                        .comment("复核无异常")
                        .build()));

        MesProBatchRecordExecutionArchiveRenderResult result = new PdfExecutionArchiveRenderer().render(context);

        try (PDDocument document = PDDocument.load(result.getContent())) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Action=FORM_REVIEW"));
            assertTrue(text.contains("Meaning=表单复核"));
            assertTrue(text.contains("SelectedSignedAt=2026-05-24T09:10"));
            assertTrue(text.contains("DisplaySignedAt=2026-05-24T09:10"));
            assertTrue(text.contains("USER_SELECTED"));
            assertTrue(text.contains("SelectedTimeZone=Asia/Shanghai"));
            assertTrue(text.contains("SelectedTimeReason=复核签名按线下完成时间显示"));
            assertTrue(text.contains("SelectedTimeAuditHash=cccccccc"));
        }
    }

    @Test
    void pdfRenderer_printsAttachmentManifestInFinalArchive() throws Exception {
        MesProBatchRecordExecutionArchiveRenderContext context = validContext();
        context.setAttachmentManifestHeadHash("attachment-head-hash");
        context.setAttachments(List.of(MesProBatchRecordExecutionAttachmentDO.builder()
                .id(201L)
                .fieldKey("visualEvidence")
                .fieldPath("sheet.main.rows[1].cells[2]")
                .fieldLabel("现场图片")
                .attachmentType("IMAGE")
                .attachmentGroupKey("R1C2-IMG-1")
                .fileName("evidence.png")
                .contentType("image/png")
                .fileSize(2048L)
                .sha256("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .attachmentHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .build()));

        MesProBatchRecordExecutionArchiveRenderResult result = new PdfExecutionArchiveRenderer().render(context);

        try (PDDocument document = PDDocument.load(result.getContent())) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Attachment Manifest Head Hash: attachment-head-hash"));
            assertTrue(text.contains("Field=visualEvidence"));
            assertTrue(text.contains("File=evidence.png"));
            assertTrue(text.contains("ContentType=image/png"));
            assertTrue(text.contains("Size=2048"));
            assertTrue(text.contains("SHA256=aaaaaaaa"));
            assertTrue(text.contains("AttachmentHash=bbbbbbbb"));
        }
    }

    @Test
    void excelRenderer_generatesControlledWorkbookSheetsAndDigestFromRenderContext() throws Exception {
        MesProBatchRecordExecutionArchiveRenderResult result = new ExcelExecutionArchiveRenderer().render(validContext());

        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", result.getContentType());
        assertEquals("EDHR_ARCHIVE_V1", result.getRenderSourceVersion());
        assertEquals(result.getContent().length, result.getFileSize());
        assertEquals(ExecutionArchiveRendererSupport.sha256(result.getContent()), result.getSha256());
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getContent()))) {
            assertEquals(4, workbook.getNumberOfSheets());
            assertEquals("Summary", workbook.getSheetName(0));
            assertEquals("Record", workbook.getSheetName(1));
            assertEquals("Signatures", workbook.getSheetName(2));
            assertEquals("Manifest", workbook.getSheetName(3));
            assertEquals("Execution Snapshot Hash", workbook.getSheet("Manifest").getRow(1).getCell(0).getStringCellValue());
            assertEquals("snapshot-hash", workbook.getSheet("Manifest").getRow(1).getCell(1).getStringCellValue());
            assertEquals("Cell Values Hash", workbook.getSheet("Manifest").getRow(2).getCell(0).getStringCellValue());
            assertEquals("cell-hash", workbook.getSheet("Manifest").getRow(2).getCell(1).getStringCellValue());
            assertEquals("Signature Hash", workbook.getSheet("Manifest").getRow(3).getCell(0).getStringCellValue());
            assertEquals("signature-hash", workbook.getSheet("Manifest").getRow(3).getCell(1).getStringCellValue());
            assertEquals("Approval Snapshot ID", workbook.getSheet("Manifest").getRow(4).getCell(0).getStringCellValue());
            assertEquals("77", workbook.getSheet("Manifest").getRow(4).getCell(1).getStringCellValue());
            assertEquals("Approval Snapshot Hash", workbook.getSheet("Manifest").getRow(5).getCell(0).getStringCellValue());
            assertEquals("approval-snapshot-hash", workbook.getSheet("Manifest").getRow(5).getCell(1).getStringCellValue());
            assertEquals("Selected Signed At", workbook.getSheet("Signatures").getRow(0).getCell(7).getStringCellValue());
            assertEquals("Display Signed At", workbook.getSheet("Signatures").getRow(0).getCell(8).getStringCellValue());
            assertEquals("Signature Time Mode", workbook.getSheet("Signatures").getRow(0).getCell(9).getStringCellValue());
            assertEquals("2026-05-24T09:10", workbook.getSheet("Signatures").getRow(1).getCell(7).getStringCellValue());
            assertEquals("2026-05-24T09:10", workbook.getSheet("Signatures").getRow(1).getCell(8).getStringCellValue());
            assertEquals("USER_SELECTED", workbook.getSheet("Signatures").getRow(1).getCell(9).getStringCellValue());
            assertEquals("Asia/Shanghai", workbook.getSheet("Signatures").getRow(1).getCell(10).getStringCellValue());
            assertEquals("提交签名按线下完成时间显示",
                    workbook.getSheet("Signatures").getRow(1).getCell(11).getStringCellValue());
            assertEquals("dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd",
                    workbook.getSheet("Signatures").getRow(1).getCell(12).getStringCellValue());
        }
    }

    @Test
    void renderer_rejectsMissingSnapshotWithoutPlaceholderOutput() {
        MesProBatchRecordExecutionArchiveRenderContext context = validContext();
        context.setExecutionSnapshot(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new PdfExecutionArchiveRenderer().render(context));

        assertEquals("EDHR archive execution snapshot is required", ex.getMessage());
    }

    @Test
    void renderer_rejectsMissingSignaturesWithoutPlaceholderOutput() {
        MesProBatchRecordExecutionArchiveRenderContext context = validContext();
        context.setSignatures(List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new ExcelExecutionArchiveRenderer().render(context));

        assertEquals("EDHR archive signatures are required", ex.getMessage());
    }

    @Test
    void printableBatchArchiveRenderer_acceptsDatabaseTimestampSignatureRecords() {
        JSONObject layout = JSON.parseObject("""
                {"rows":{"1":{"cells":{"1":{"text":"提交人","edhrSignature":{"enabled":true,"actionType":"SUBMIT"}}}}},
                "cols":{"1":{"width":120}}}
                """);
        JSONObject form = new JSONObject();
        form.put("processName", "粗洗工序");
        form.put("batchRecordReportName", "生产记录");
        form.put("executionCode", "BRE-DB-TIME");
        form.put("submittedAt", "2026-07-22 10:08:12");
        form.put("sheetLayoutJson", layout.toJSONString());
        form.put("executionSnapshotJson", new JSONObject()
                .fluentPut("layout", layout)
                .fluentPut("fields", new JSONArray())
                .toJSONString());
        form.put("cellValuesJson", "[]");
        form.put("signatureCellMarkers", JSONArray.parseArray("""
                [{"rowIndex":1,"columnIndex":1,"enabled":true,"actionType":"SUBMIT"}]
                """));
        form.put("signatureRecords", JSONArray.parseArray("""
                [{"actionType":"FIELD_CHANGE","actorName":"瑛泰管理员",
                "signedAt":"2026-07-22 10:07:12","signatureDisplayAt":"2026-07-22 10:07:12"},
                {"actionType":"SUBMIT","actorName":"瑛泰管理员",
                "signedAt":"2026-07-22 10:08:12","signatureDisplayAt":"2026-07-22 10:08:12"}]
                """));
        JSONObject manifest = new JSONObject();
        manifest.put("schemaVersion", "EDHR_BATCH_PRINTABLE_ARCHIVE_V1");
        manifest.put("batchCode", "BATCH-DB-TIME");
        manifest.put("routeName", "测试路线");
        manifest.put("routeCode", "RT-DB");
        manifest.put("generatedAt", "2026-07-22 10:30:00");
        manifest.put("aggregateHash", "archive-hash");
        manifest.put("bodyForms", new JSONArray().fluentAdd(form));
        manifest.put("appendixSpecialNodes", new JSONArray());
        manifest.put("dossierItems", new JSONArray());
        manifest.put("changeEvents", new JSONArray());

        byte[] bytes = MesProEdhrBatchArchivePrintablePdfRenderer.render(
                manifest.toJSONString(), "C:/Windows/Fonts/simhei.ttf", "C:/Windows/Fonts/seguisym.ttf");

        assertArrayEquals("%PDF".getBytes(StandardCharsets.US_ASCII), firstBytes(bytes, 4));
    }

    private MesProBatchRecordExecutionArchiveRenderContext validContext() {
        JSONObject snapshot = new JSONObject();
        snapshot.put("templateName", "EDHR Template");
        snapshot.put("rows", JSONArray.parseArray("[{\"label\":\"temperature\",\"cell\":\"A1\"}]"));
        JSONArray cellValues = JSONArray.parseArray("[{\"cell\":\"A1\",\"value\":\"37.2\"}]");
        return MesProBatchRecordExecutionArchiveRenderContext.builder()
                .execution(MesProBatchRecordExecutionDO.builder()
                        .id(11L)
                        .executionCode("EXE 001")
                        .templateCode("TPL-001")
                        .templateName("EDHR Template")
                        .workOrderCode("WO-001")
                        .batchCode("BATCH-001")
                        .status(20)
                        .build())
                .executionSnapshot(snapshot)
                .cellValues(cellValues)
                .signatures(List.of(MesProBatchRecordExecutionSignatureDO.builder()
                        .id(101L)
                        .executionId(11L)
                        .actorId(7L)
                        .actionType("SUBMIT")
                        .signatureMode("PASSWORD")
                        .passwordVerified(true)
                        .signedAt(LocalDateTime.of(2026, 5, 24, 9, 20))
                        .selectedSignedAt(LocalDateTime.of(2026, 5, 24, 9, 10))
                        .signatureDisplayAt(LocalDateTime.of(2026, 5, 24, 9, 10))
                        .signatureTimeMode("USER_SELECTED")
                        .selectedTimeZone("Asia/Shanghai")
                        .selectedTimeReason("提交签名按线下完成时间显示")
                        .selectedTimeAuditHash("dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd")
                        .comment("submit approved")
                        .build()))
                .executionSnapshotHash("snapshot-hash")
                .cellValuesHash("cell-hash")
                .signatureHash("signature-hash")
                .attachments(List.of())
                .attachmentManifestHeadHash("")
                .approvalSnapshotId(77L)
                .approvalSnapshotHash("approval-snapshot-hash")
                .generatedBy(8L)
                .generatedAt(LocalDateTime.of(2026, 5, 24, 9, 30))
                .build();
    }

    private byte[] firstBytes(byte[] content, int length) {
        byte[] bytes = new byte[length];
        System.arraycopy(content, 0, bytes, 0, length);
        return bytes;
    }
}
