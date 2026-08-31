package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.AdobePDFSchema;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.schema.XMPBasicSchema;
import org.apache.xmpbox.type.BadFieldValueException;
import org.apache.xmpbox.xml.XmpSerializer;

import java.awt.Color;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class MesProEdhrBatchArchivePrintablePdfRenderer {

    private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
    private static final float MARGIN = 36F;
    private static final float TITLE_FONT_SIZE = 16F;
    private static final float SECTION_FONT_SIZE = 12.5F;
    private static final float TEXT_FONT_SIZE = 10F;
    private static final float TABLE_FONT_SIZE = 8.5F;
    private static final float TABLE_RULE_FONT_SIZE = 7.5F;
    private static final float LINE_HEIGHT = 14F;
    private static final float TABLE_LINE_HEIGHT = 10.5F;
    private static final float CELL_PADDING = 3.5F;
    private static final float DEFAULT_COLUMN_WIDTH = 160F;
    private static final float DEFAULT_ROW_HEIGHT = 30F;
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATABASE_TIME_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATABASE_TIME_MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Color BORDER_COLOR = new Color(31, 41, 55);
    private static final Color STATIC_BG = new Color(243, 244, 246);
    private static final Color FILLABLE_BG = Color.WHITE;
    private static final Color SECTION_BG = new Color(229, 231, 235);
    private static final Color SIGNATURE_BG = new Color(248, 251, 255);
    private static final Color ATTACHMENT_BG = new Color(248, 250, 252);
    private static final Color TEXT_COLOR = new Color(17, 24, 39);
    private static final Color FILLABLE_TEXT = new Color(15, 118, 110);
    private static final Color MUTED_TEXT = new Color(107, 114, 128);
    private static final Color RULE_TEXT = new Color(180, 83, 9);
    private static final String PDF_A_PROFILE = "PDF/A-1b";
    private static final String PDF_CREATOR = "IntRuoyi MES";
    private static final String PDF_PRODUCER = "IntRuoyi MES PDFBox 2.0.32";
    private static final String SRGB_PROFILE_NAME = "sRGB IEC61966-2.1";
    private static final ZoneId ARCHIVE_ZONE = ZoneId.of("Asia/Shanghai");

    private MesProEdhrBatchArchivePrintablePdfRenderer() {
    }

    static byte[] render(String manifestJson, String fontPath, String symbolFontPath) {
        JSONObject manifest = JSON.parseObject(manifestJson);
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            File fontFile = new File(fontPath);
            if (!fontFile.isFile()) {
                throw new IllegalStateException("EDHR batch archive PDF font is missing: " + fontPath);
            }
            File symbolFontFile = new File(symbolFontPath);
            if (!symbolFontFile.isFile()) {
                throw new IllegalStateException("EDHR batch archive PDF symbol font is missing: " + symbolFontPath);
            }
            PDType0Font font = PDType0Font.load(document, fontFile);
            PDType0Font symbolFont = PDType0Font.load(document, symbolFontFile);
            configurePdfA1b(document, manifest);
            PdfCanvas canvas = new PdfCanvas(document, font, symbolFont);
            canvas.writeTitle("打印版 eDHR 已填表单归档");
            writeBatchSummary(canvas, manifest);
            writeBodyForms(canvas, jsonArrayObjects(manifest, "bodyForms"));
            writeSpecialNodes(canvas, jsonArrayObjects(manifest, "appendixSpecialNodes"));
            writeEvidenceAppendix(canvas, manifest);
            canvas.close();
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to render printable eDHR batch archive PDF", ex);
        }
    }

    private static void configurePdfA1b(PDDocument document, JSONObject manifest) throws IOException {
        String batchCode = StrUtil.blankToDefault(manifest.getString("batchCode"), "UNKNOWN");
        String title = "eDHR 批次最终归档 - " + batchCode;
        String subject = "电子批记录长期归档";
        String keywords = "eDHR,PDF/A-1b," + batchCode;
        Calendar generatedAt = resolveGeneratedAt(manifest.getString("generatedAt"));

        document.setVersion(1.4F);
        PDDocumentInformation information = document.getDocumentInformation();
        information.setTitle(title);
        information.setAuthor(PDF_CREATOR);
        information.setSubject(subject);
        information.setKeywords(keywords);
        information.setCreator(PDF_CREATOR);
        information.setProducer(PDF_PRODUCER);
        information.setCreationDate(generatedAt);
        information.setModificationDate(generatedAt);

        XMPMetadata xmp = XMPMetadata.createXMPMetadata();
        DublinCoreSchema dublinCore = xmp.createAndAddDublinCoreSchema();
        dublinCore.setTitle(title);
        dublinCore.addCreator(PDF_CREATOR);
        dublinCore.setDescription(subject);
        dublinCore.setFormat("application/pdf");

        AdobePDFSchema adobePdf = xmp.createAndAddAdobePDFSchema();
        adobePdf.setProducer(PDF_PRODUCER);
        adobePdf.setKeywords(keywords);
        adobePdf.setPDFVersion("1.4");

        XMPBasicSchema basic = xmp.createAndAddXMPBasicSchema();
        basic.setCreatorTool(PDF_CREATOR);
        basic.setCreateDate(generatedAt);
        basic.setModifyDate(generatedAt);
        basic.setMetadataDate(generatedAt);

        PDFAIdentificationSchema identification = xmp.createAndAddPFAIdentificationSchema();
        identification.setPart(1);
        try {
            identification.setConformance("B");
        } catch (BadFieldValueException ex) {
            throw new IOException("Failed to configure PDF/A-1b conformance metadata", ex);
        }

        ByteArrayOutputStream xmpBytes = new ByteArrayOutputStream();
        try {
            new XmpSerializer().serialize(xmp, xmpBytes, true);
        } catch (javax.xml.transform.TransformerException ex) {
            throw new IOException("Failed to serialize PDF/A metadata", ex);
        }
        PDMetadata metadata = new PDMetadata(document);
        metadata.importXMPMetadata(xmpBytes.toByteArray());

        PDDocumentCatalog catalog = document.getDocumentCatalog();
        catalog.setMetadata(metadata);
        catalog.setLanguage("zh-CN");
        byte[] profileBytes = ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData();
        try (ByteArrayInputStream profileStream = new ByteArrayInputStream(profileBytes)) {
            PDOutputIntent outputIntent = new PDOutputIntent(document, profileStream);
            outputIntent.setInfo(SRGB_PROFILE_NAME);
            outputIntent.setOutputCondition(SRGB_PROFILE_NAME);
            outputIntent.setOutputConditionIdentifier(SRGB_PROFILE_NAME);
            outputIntent.setRegistryName("http://www.color.org");
            catalog.addOutputIntent(outputIntent);
        }
    }

    private static Calendar resolveGeneratedAt(String raw) {
        LocalDateTime parsed = parseDateTime(raw);
        if (parsed == null) {
            throw new IllegalStateException("EDHR batch archive generatedAt is invalid for " + PDF_A_PROFILE);
        }
        return GregorianCalendar.from(parsed.atZone(ARCHIVE_ZONE));
    }

    private static void writeBatchSummary(PdfCanvas canvas, JSONObject manifest) throws IOException {
        canvas.writeSectionTitle("批次摘要");
        canvas.writeKeyValue("批次", manifest.getString("batchCode"));
        canvas.writeKeyValue("路线", joinNonBlank(manifest.getString("routeName"), manifest.getString("routeCode")));
        canvas.writeKeyValue("打印快照版本", manifest.getString("schemaVersion"));
        canvas.writeKeyValue("manifest 快照版本", manifest.getString("schemaVersion"));
        canvas.writeKeyValue("生成时间", formatDateTime(manifest.getString("generatedAt")));
        canvas.writeKeyValue("归档哈希", manifest.getString("aggregateHash"));
        canvas.writeBlankLine();
    }

    private static void writeBodyForms(PdfCanvas canvas, List<JSONObject> forms) throws IOException {
        canvas.writeSectionTitle("已填表单正文");
        if (forms.isEmpty()) {
            canvas.writeParagraph("当前批次没有可打印的已填表单。");
            canvas.writeBlankLine();
            return;
        }
        boolean first = true;
        for (JSONObject form : forms) {
            if (!first) {
                canvas.newPage();
            }
            first = false;
            String formTitle = joinNonBlank(form.getString("processName"), form.getString("batchRecordReportName"));
            canvas.writeSectionTitle(StrUtil.blankToDefault(formTitle, "已填表单"));
            canvas.writeKeyValue("执行编号", form.getString("executionCode"));
            canvas.writeKeyValue("提交时间", formatDateTime(form.getString("submittedAt")));
            canvas.writeKeyValue("完成时间", formatDateTime(StrUtil.blankToDefault(
                    form.getString("closedAt"), form.getString("approvedAt"))));
            canvas.writeKeyValue("表单标题", resolveFormTableTitle(form));
            writeSignatureSummary(canvas, form);
            canvas.writeBlankLine();
            renderFormTable(canvas, form);
            String remark = form.getString("remark");
            if (StrUtil.isNotBlank(remark)) {
                canvas.writeBlankLine();
                canvas.writeKeyValue("备注", remark);
            }
        }
    }

    private static void writeSignatureSummary(PdfCanvas canvas, JSONObject form) throws IOException {
        List<JSONObject> signatures = jsonArrayObjects(form, "signatureRecords");
        if (signatures.isEmpty()) {
            return;
        }
        canvas.writeSubTitle("签名记录");
        for (JSONObject signature : signatures) {
            List<String> parts = new ArrayList<>();
            parts.add("签署含义=" + value(resolveSignaturePurpose(signature)));
            parts.add("签名人=" + value(resolveSignatureActorName(signature)));
            parts.add("签名时间=" + formatSignatureDateTime(signature));
            String recordHash = resolveSignatureRecordHash(signature);
            if (StrUtil.isNotBlank(recordHash)) {
                parts.add("记录哈希=" + recordHash);
            }
            String timeAuditHash = StrUtil.trim(signature.getString("selectedTimeAuditHash"));
            if (StrUtil.isNotBlank(timeAuditHash)) {
                parts.add("时间哈希=" + timeAuditHash);
            }
            String line = String.join(" | ", parts);
            canvas.writeParagraph(line);
        }
    }

    private static void renderFormTable(PdfCanvas canvas, JSONObject form) throws IOException {
        JSONObject layout = resolveLayout(form);
        if (layout == null) {
            throw missingPrintableLayout(form, "layout");
        }
        JSONObject rows = layout.getJSONObject("rows");
        if (rows == null || rows.isEmpty()) {
            throw missingPrintableLayout(form, "rows");
        }
        List<Integer> rowIndexes = sortedIndexes(rows.keySet());
        List<Integer> columnIndexes = collectColumnIndexes(layout);
        if (columnIndexes.isEmpty()) {
            throw missingPrintableLayout(form, "columns");
        }
        Map<Integer, Float> columnWidthMap = resolveColumnWidths(layout, columnIndexes, canvas.availableWidth());
        Map<String, JSONObject> cellValueMap = parseCellValueMap(form.getString("cellValuesJson"));
        Map<String, JSONObject> signatureMarkerMap = parseSignatureMarkerMap(form.getJSONArray("signatureCellMarkers"));
        Map<String, String> attachmentRuleMap = parseAttachmentRuleMap(form.getString("executionSnapshotJson"));
        Map<String, JSONObject> signatureRecordMap = latestSignatureRecordByAction(form.getJSONArray("signatureRecords"));
        Set<String> coveredCells = collectCoveredCells(rows);
        Map<Integer, Float> rowHeightMap = resolveRowHeights(rows, rowIndexes, columnIndexes, columnWidthMap,
                cellValueMap, signatureMarkerMap, signatureRecordMap, attachmentRuleMap);
        int columnCount = columnIndexes.size();

        for (int rowIndex : rowIndexes) {
            JSONObject row = rows.getJSONObject(String.valueOf(rowIndex));
            float rowHeight = rowHeightMap.getOrDefault(rowIndex, DEFAULT_ROW_HEIGHT);
            float requiredHeight = requiredHeightForRow(rows, rowIndexes, rowHeightMap, rowIndex);
            canvas.ensureTableSpace(requiredHeight);
            float rowTop = canvas.cursorY();
            float x = canvas.margin();
            for (int columnIndex : columnIndexes) {
                float baseColumnWidth = columnWidthMap.getOrDefault(columnIndex, DEFAULT_COLUMN_WIDTH);
                if (coveredCells.contains(cellKey(rowIndex, columnIndex))) {
                    x += baseColumnWidth;
                    continue;
                }
                JSONObject rawCell = row == null || row.getJSONObject("cells") == null
                        ? null : row.getJSONObject("cells").getJSONObject(String.valueOf(columnIndex));
                Merge merge = normalizeMerge(rawCell);
                float width = sumColumnWidths(columnIndexes, columnWidthMap, columnIndex, merge.colSpan());
                float height = sumRowHeights(rowIndexes, rowHeightMap, rowIndex, merge.rowSpan());
                JSONObject signatureMarker = resolveSignatureMarker(rawCell, signatureMarkerMap, rowIndex, columnIndex);
                JSONObject signatureRecord = signatureMarker == null ? null
                        : signatureRecordMap.get(StrUtil.blankToDefault(signatureMarker.getString("actionType"), ""));
                String text = resolveCellText(rowIndex, columnIndex, rawCell, cellValueMap, signatureMarker, signatureRecord);
                String attachmentRuleText = attachmentRuleMap.get(cellKey(rowIndex, columnIndex));
                boolean fillable = rawCell != null && rawCell.getJSONObject("fillForm") != null;
                boolean signatureCell = signatureMarker != null;
                boolean sectionTitle = isSectionTitle(rawCell, merge, columnCount);
                Color background = determineCellBackground(fillable, signatureCell, sectionTitle, attachmentRuleText);
                Color textColor = determineCellTextColor(fillable, signatureCell, text);
                canvas.drawCell(x, rowTop, width, height, background, text, attachmentRuleText, textColor);
                x += baseColumnWidth;
            }
            canvas.consumeTableRow(rowHeight);
        }
    }

    private static IllegalStateException missingPrintableLayout(JSONObject form, String part) {
        return new IllegalStateException("EDHR batch archive printable layout is missing " + part
                + ": executionCode=" + StrUtil.blankToDefault(form.getString("executionCode"), "UNKNOWN"));
    }

    private static void writeSpecialNodes(PdfCanvas canvas, List<JSONObject> nodes) throws IOException {
        canvas.newPage();
        canvas.writeSectionTitle("特殊节点附录");
        if (nodes.isEmpty()) {
            canvas.writeParagraph("当前批次没有特殊节点摘要。");
            return;
        }
        for (JSONObject node : nodes) {
            canvas.writeSubTitle(StrUtil.blankToDefault(node.getString("processName"), node.getString("nodeType")));
            canvas.writeKeyValue("节点类型", node.getString("nodeType"));
            canvas.writeKeyValue("状态", formatTaskStatus(node.getInteger("status")));
            canvas.writeKeyValue("操作人", value(node.get("operatorId")));
            canvas.writeKeyValue("操作时间", formatDateTime(value(node.get("operatedAt"))));
            JSONObject payload = safeJsonObject(node.getString("specialPayloadJson"));
            if (payload != null) {
                if (StrUtil.isNotBlank(payload.getString("skipReason"))) {
                    canvas.writeKeyValue("跳过原因", payload.getString("skipReason"));
                }
                if (StrUtil.isNotBlank(payload.getString("sterilizationBatchNo"))) {
                    canvas.writeKeyValue("灭菌批次", payload.getString("sterilizationBatchNo"));
                }
            }
            List<JSONObject> attachments = jsonArrayObjects(node, "specialAttachments");
            if (!attachments.isEmpty()) {
                canvas.writeKeyValue("特殊附件", joinAttachmentNames(attachments));
            }
            if (payload != null && !payload.isEmpty()) {
                canvas.writeKeyValue("specialPayloadJson", payload.toJSONString());
            }
            canvas.writeBlankLine();
        }
    }

    private static void writeEvidenceAppendix(PdfCanvas canvas, JSONObject manifest) throws IOException {
        canvas.newPage();
        canvas.writeSectionTitle("归档证据附录");
        writeReleaseApprovalAppendix(canvas, manifest);
        writeAttachmentAppendix(canvas, manifest);
        writeDossierAppendix(canvas, manifest);
        writeChangeEventAppendix(canvas, manifest);
        writeDomainTraceAppendix(canvas, manifest);
        canvas.writeKeyValue("归档哈希", manifest.getString("aggregateHash"));
    }

    private static void writeReleaseApprovalAppendix(PdfCanvas canvas, JSONObject manifest) throws IOException {
        JSONObject release = manifest.getJSONObject("releaseTransactionSnapshot");
        if (release == null || release.isEmpty()) {
            return;
        }
        canvas.writeSubTitle("放行审核与批准");
        canvas.writeKeyValue("放行单号", release.getString("releaseCode"));
        canvas.writeKeyValue("放行状态", release.getString("releaseStatus"));
        canvas.writeKeyValue("预检时间", formatDateTime(release.getString("lastPrecheckAt")));
        canvas.writeKeyValue("审核人/提交人", value(release.get("submittedByName")));
        canvas.writeKeyValue("审核时间", formatDateTime(release.getString("submittedAt")));
        canvas.writeKeyValue("批准人", value(release.get("approvedByName")));
        canvas.writeKeyValue("批准时间", formatDateTime(release.getString("approvedAt")));
        canvas.writeKeyValue("审批意见", release.getString("approvalOpinion"));
        canvas.writeKeyValue("签名证据哈希", release.getString("approvalSignoffEvidenceHash"));

        List<JSONObject> events = jsonArrayObjects(manifest, "releaseEvents");
        if (events.isEmpty()) {
            return;
        }
        canvas.writeSubTitle("放行事件");
        for (JSONObject event : events) {
            canvas.writeParagraph(String.join(" | ",
                    "事件=" + value(event.get("eventType")),
                    "状态=" + joinNonBlank(value(event.get("fromStatus")), value(event.get("toStatus"))),
                    "操作人=" + value(event.get("actorUserId")),
                    "原因=" + value(firstNonBlank(event.get("reason"), event.get("opinion"))),
                    "证据=" + value(firstNonBlank(event.get("evidenceHash"), event.get("signoffEvidenceHash"))),
                    "时间=" + formatDateTime(event.getString("occurredAt"))));
        }
        canvas.writeBlankLine();
    }

    private static void writeAttachmentAppendix(PdfCanvas canvas, JSONObject manifest) throws IOException {
        canvas.writeSubTitle("附件清单");
        List<String> lines = new ArrayList<>();
        for (JSONObject form : jsonArrayObjects(manifest, "bodyForms")) {
            for (JSONObject attachment : jsonArrayObjects(form, "attachmentSummaries")) {
                lines.add(joinNonBlank(
                        attachment.getString("fileName"),
                        attachment.getString("fieldLabel"),
                        attachment.getString("attachmentType")));
            }
        }
        for (JSONObject node : jsonArrayObjects(manifest, "appendixSpecialNodes")) {
            for (JSONObject attachment : jsonArrayObjects(node, "specialAttachments")) {
                lines.add(joinNonBlank(
                        attachment.getString("fileName"),
                        attachment.getString("fieldLabel"),
                        attachment.getString("attachmentType")));
            }
        }
        if (lines.isEmpty()) {
            canvas.writeParagraph("无附件");
            return;
        }
        for (String line : lines) {
            canvas.writeParagraph(line);
        }
    }

    private static void writeDossierAppendix(PdfCanvas canvas, JSONObject manifest) throws IOException {
        canvas.writeSubTitle("卷宗项");
        List<JSONObject> items = jsonArrayObjects(manifest, "dossierItems");
        if (items.isEmpty()) {
            canvas.writeParagraph("无卷宗项");
            return;
        }
        for (JSONObject item : items) {
            canvas.writeParagraph(String.join(" | ",
                    "类型=" + value(item.get("itemType")),
                    "名称=" + value(item.get("itemName")),
                    "来源单号=" + value(item.get("sourceDocCode")),
                    "结果=" + value(item.get("sourceDocResult"))));
        }
    }

    private static void writeChangeEventAppendix(PdfCanvas canvas, JSONObject manifest) throws IOException {
        canvas.writeSubTitle("返工/驳回/变更事件");
        List<JSONObject> events = jsonArrayObjects(manifest, "changeEvents");
        if (events.isEmpty()) {
            canvas.writeParagraph("无返工/驳回/变更事件");
            return;
        }
        for (JSONObject event : events) {
            canvas.writeParagraph(String.join(" | ",
                    "变更编码=" + value(event.get("changeCode")),
                    "类型=" + value(event.get("changeType")),
                    "状态=" + value(event.get("changeStatus")),
                    "生效时间=" + formatDateTime(value(event.get("effectiveAt")))));
        }
    }

    private static void writeDomainTraceAppendix(PdfCanvas canvas, JSONObject manifest) throws IOException {
        canvas.writeSubTitle("审计追踪");
        List<JSONObject> forms = jsonArrayObjects(manifest, "bodyForms");
        if (forms.isEmpty()) {
            canvas.writeParagraph("无审计追踪摘要");
            return;
        }
        for (JSONObject form : forms) {
            JSONObject trace = form.getJSONObject("domainTraceSummary");
            if (trace == null || trace.isEmpty()) {
                continue;
            }
            canvas.writeParagraph(String.join(" | ",
                    "表单=" + joinNonBlank(form.getString("processName"), form.getString("executionCode")),
                    "状态=" + value(trace.get("status")),
                    "追踪=" + value(trace.get("snapshotHash")),
                    "验证时间=" + formatDateTime(value(trace.get("verifiedAt")))));
        }
    }

    private static JSONObject resolveLayout(JSONObject form) {
        JSONObject snapshot = safeJsonObject(form.getString("executionSnapshotJson"));
        JSONObject snapshotLayout = resolveLayoutCandidate(snapshot == null ? null : snapshot.getJSONObject("layout"));
        if (snapshotLayout != null) {
            return snapshotLayout;
        }
        return resolveLayoutCandidate(safeJsonObject(form.getString("sheetLayoutJson")));
    }

    private static JSONObject resolveLayoutCandidate(JSONObject candidate) {
        if (candidate == null) {
            return null;
        }
        JSONObject rows = candidate.getJSONObject("rows");
        return rows == null || rows.isEmpty() ? null : candidate;
    }

    private static String resolveFormTableTitle(JSONObject form) {
        JSONObject meta = safeJsonObject(form.getString("metaJson"));
        String title = meta == null ? null : meta.getString("tableTitle");
        return StrUtil.blankToDefault(title, form.getString("batchRecordReportName"));
    }

    private static List<Integer> sortedIndexes(Set<String> rawIndexes) {
        return rawIndexes.stream()
                .map(index -> StrUtil.isNumeric(index) ? Integer.valueOf(index) : null)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
    }

    private static List<Integer> collectColumnIndexes(JSONObject layout) {
        Set<Integer> indexes = new HashSet<>();
        JSONObject cols = layout.getJSONObject("cols");
        if (cols != null) {
            indexes.addAll(sortedIndexes(cols.keySet()));
        }
        JSONObject rows = layout.getJSONObject("rows");
        if (rows != null) {
            for (String rowKey : rows.keySet()) {
                JSONObject row = rows.getJSONObject(rowKey);
                JSONObject cells = row == null ? null : row.getJSONObject("cells");
                if (cells == null) {
                    continue;
                }
                for (String columnKey : cells.keySet()) {
                    if (!StrUtil.isNumeric(columnKey)) {
                        continue;
                    }
                    int columnIndex = Integer.parseInt(columnKey);
                    indexes.add(columnIndex);
                    Merge merge = normalizeMerge(cells.getJSONObject(columnKey));
                    for (int offset = 1; offset < merge.colSpan(); offset++) {
                        indexes.add(columnIndex + offset);
                    }
                }
            }
        }
        return indexes.stream().sorted().toList();
    }

    private static Map<Integer, Float> resolveColumnWidths(JSONObject layout, List<Integer> columnIndexes, float availableWidth) {
        Map<Integer, Float> rawWidths = new LinkedHashMap<>();
        JSONObject cols = layout.getJSONObject("cols");
        float total = 0F;
        for (int columnIndex : columnIndexes) {
            float width = DEFAULT_COLUMN_WIDTH;
            if (cols != null && cols.getJSONObject(String.valueOf(columnIndex)) != null) {
                Number configured = cols.getJSONObject(String.valueOf(columnIndex)).getFloat("width");
                if (configured != null && configured.floatValue() > 0) {
                    width = configured.floatValue();
                }
            }
            rawWidths.put(columnIndex, width);
            total += width;
        }
        Map<Integer, Float> widths = new LinkedHashMap<>();
        for (int columnIndex : columnIndexes) {
            widths.put(columnIndex, total <= 0F ? availableWidth / columnIndexes.size()
                    : availableWidth * (rawWidths.get(columnIndex) / total));
        }
        return widths;
    }

    private static Map<Integer, Float> resolveRowHeights(JSONObject rows,
                                                         List<Integer> rowIndexes,
                                                         List<Integer> columnIndexes,
                                                         Map<Integer, Float> columnWidthMap,
                                                         Map<String, JSONObject> cellValueMap,
                                                         Map<String, JSONObject> signatureMarkerMap,
                                                         Map<String, JSONObject> signatureRecordMap,
                                                         Map<String, String> attachmentRuleMap) {
        Map<Integer, Float> rowHeightMap = new LinkedHashMap<>();
        int columnCount = columnIndexes.size();
        for (int rowIndex : rowIndexes) {
            JSONObject row = rows.getJSONObject(String.valueOf(rowIndex));
            float baseHeight = DEFAULT_ROW_HEIGHT;
            Number configuredHeight = row == null ? null : row.getFloat("height");
            if (configuredHeight != null && configuredHeight.floatValue() > 0) {
                baseHeight = Math.max(24F, configuredHeight.floatValue());
            }
            float resolved = baseHeight;
            for (int columnIndex : columnIndexes) {
                JSONObject rawCell = row == null || row.getJSONObject("cells") == null
                        ? null : row.getJSONObject("cells").getJSONObject(String.valueOf(columnIndex));
                if (rawCell == null) {
                    continue;
                }
                Merge merge = normalizeMerge(rawCell);
                if (merge.rowSpan() > 1) {
                    continue;
                }
                JSONObject signatureMarker = resolveSignatureMarker(rawCell, signatureMarkerMap, rowIndex, columnIndex);
                JSONObject signatureRecord = signatureMarker == null ? null
                        : signatureRecordMap.get(StrUtil.blankToDefault(signatureMarker.getString("actionType"), ""));
                String text = resolveCellText(rowIndex, columnIndex, rawCell, cellValueMap, signatureMarker, signatureRecord);
                String ruleText = attachmentRuleMap.get(cellKey(rowIndex, columnIndex));
                float width = sumColumnWidths(columnIndexes, columnWidthMap, columnIndex, merge.colSpan());
                float estimated = estimateCellHeight(text, ruleText, width);
                if (isSectionTitle(rawCell, merge, columnCount)) {
                    estimated = Math.max(estimated, 28F);
                }
                resolved = Math.max(resolved, estimated);
            }
            rowHeightMap.put(rowIndex, resolved);
        }
        return rowHeightMap;
    }

    private static float requiredHeightForRow(JSONObject rows,
                                              List<Integer> rowIndexes,
                                              Map<Integer, Float> rowHeightMap,
                                              int rowIndex) {
        JSONObject row = rows.getJSONObject(String.valueOf(rowIndex));
        JSONObject cells = row == null ? null : row.getJSONObject("cells");
        float required = rowHeightMap.getOrDefault(rowIndex, DEFAULT_ROW_HEIGHT);
        if (cells == null) {
            return required;
        }
        for (String columnKey : cells.keySet()) {
            if (!StrUtil.isNumeric(columnKey)) {
                continue;
            }
            JSONObject cell = cells.getJSONObject(columnKey);
            Merge merge = normalizeMerge(cell);
            required = Math.max(required, sumRowHeights(rowIndexes, rowHeightMap, rowIndex, merge.rowSpan()));
        }
        return required;
    }

    private static float sumColumnWidths(List<Integer> columnIndexes, Map<Integer, Float> columnWidthMap,
                                         int startColumnIndex, int colSpan) {
        float width = 0F;
        int startPosition = columnIndexes.indexOf(startColumnIndex);
        if (startPosition < 0) {
            return width;
        }
        for (int offset = 0; offset < colSpan && startPosition + offset < columnIndexes.size(); offset++) {
            width += columnWidthMap.getOrDefault(columnIndexes.get(startPosition + offset), DEFAULT_COLUMN_WIDTH);
        }
        return width;
    }

    private static float sumRowHeights(List<Integer> rowIndexes, Map<Integer, Float> rowHeightMap,
                                       int startRowIndex, int rowSpan) {
        float height = 0F;
        int startPosition = rowIndexes.indexOf(startRowIndex);
        if (startPosition < 0) {
            return height;
        }
        for (int offset = 0; offset < rowSpan && startPosition + offset < rowIndexes.size(); offset++) {
            height += rowHeightMap.getOrDefault(rowIndexes.get(startPosition + offset), DEFAULT_ROW_HEIGHT);
        }
        return height;
    }

    private static Set<String> collectCoveredCells(JSONObject rows) {
        Set<String> covered = new HashSet<>();
        for (String rowKey : rows.keySet()) {
            if (!StrUtil.isNumeric(rowKey)) {
                continue;
            }
            int rowIndex = Integer.parseInt(rowKey);
            JSONObject row = rows.getJSONObject(rowKey);
            JSONObject cells = row == null ? null : row.getJSONObject("cells");
            if (cells == null) {
                continue;
            }
            for (String columnKey : cells.keySet()) {
                if (!StrUtil.isNumeric(columnKey)) {
                    continue;
                }
                int columnIndex = Integer.parseInt(columnKey);
                Merge merge = normalizeMerge(cells.getJSONObject(columnKey));
                for (int rowOffset = 0; rowOffset < merge.rowSpan(); rowOffset++) {
                    for (int columnOffset = 0; columnOffset < merge.colSpan(); columnOffset++) {
                        if (rowOffset == 0 && columnOffset == 0) {
                            continue;
                        }
                        covered.add(cellKey(rowIndex + rowOffset, columnIndex + columnOffset));
                    }
                }
            }
        }
        return covered;
    }

    private static String resolveCellText(int rowIndex,
                                          int columnIndex,
                                          JSONObject rawCell,
                                          Map<String, JSONObject> cellValueMap,
                                          JSONObject signatureMarker,
                                          JSONObject signatureRecord) {
        String signatureText = resolveSignatureText(signatureMarker, signatureRecord);
        if (signatureText != null) {
            return signatureText;
        }
        JSONObject cellValue = cellValueMap.get(cellKey(rowIndex, columnIndex));
        if (cellValue != null) {
            return stringifyCellValue(cellValue);
        }
        JSONObject fillForm = rawCell == null ? null : rawCell.getJSONObject("fillForm");
        Object fillValue = fillForm == null ? null : fillForm.get("value");
        if (fillValue != null && !StrUtil.isBlankIfStr(fillValue)) {
            return stringifyValue(fillValue);
        }
        if (rawCell != null && rawCell.get("value") != null && !StrUtil.isBlankIfStr(rawCell.get("value"))) {
            return stringifyValue(rawCell.get("value"));
        }
        if (rawCell != null && rawCell.get("text") != null && !StrUtil.isBlankIfStr(rawCell.get("text"))) {
            return stringifyValue(rawCell.get("text"));
        }
        return "";
    }

    private static JSONObject resolveSignatureMarker(JSONObject rawCell,
                                                     Map<String, JSONObject> signatureMarkerMap,
                                                     int rowIndex,
                                                     int columnIndex) {
        JSONObject directMarker = rawCell == null ? null : rawCell.getJSONObject("edhrSignature");
        if (directMarker != null && Boolean.TRUE.equals(directMarker.getBoolean("enabled"))) {
            return directMarker;
        }
        return signatureMarkerMap.get(cellKey(rowIndex, columnIndex));
    }

    private static String resolveSignatureText(JSONObject signatureMarker, JSONObject signatureRecord) {
        if (signatureMarker == null || !Boolean.TRUE.equals(signatureMarker.getBoolean("enabled"))
                || StrUtil.isBlank(signatureMarker.getString("actionType"))) {
            return null;
        }
        if (signatureRecord == null) {
            return "未签名";
        }
        List<String> lines = new ArrayList<>();
        lines.add("签名人:" + value(resolveSignatureActorName(signatureRecord)));
        lines.add("含义:" + value(resolveSignaturePurpose(signatureRecord)));
        lines.add("时间:" + formatSignatureDateTime(signatureRecord));
        String recordHash = resolveSignatureRecordHash(signatureRecord);
        if (StrUtil.isNotBlank(recordHash)) {
            lines.add("记录哈希:" + recordHash);
        }
        return String.join("\n", lines);
    }

    private static String resolveSignatureActorName(JSONObject signatureRecord) {
        String actorName = firstNonBlank(
                signatureRecord.getString("actorName"),
                signatureRecord.getString("actorNicknameSnapshot"),
                null);
        if (StrUtil.isBlank(actorName)) {
            throw new IllegalStateException("EDHR batch archive signature actor name is required, actionType="
                    + value(signatureRecord.getString("actionType")));
        }
        return actorName;
    }

    private static String resolveSignaturePurpose(JSONObject signatureRecord) {
        String purpose = StrUtil.blankToDefault(
                StrUtil.trim(signatureRecord.getString("signaturePurpose")),
                signatureMeaning(signatureRecord.getString("actionType")));
        if (StrUtil.isBlank(purpose)) {
            throw new IllegalStateException("EDHR batch archive signature purpose is required, actionType="
                    + value(signatureRecord.getString("actionType")));
        }
        return purpose;
    }

    private static String resolveSignatureRecordHash(JSONObject signatureRecord) {
        String recordHash = firstNonBlank(
                signatureRecord.getString("recordHashSnapshot"),
                signatureRecord.getString("aggregateHash"),
                firstNonBlank(
                        signatureRecord.getString("fieldAuditHeadHash"),
                        signatureRecord.getString("cellValuesHash"),
                        null));
        if (StrUtil.isBlank(recordHash)) {
            throw new IllegalStateException("EDHR batch archive signature record hash is required, actionType="
                    + value(signatureRecord.getString("actionType")));
        }
        return recordHash;
    }

    private static String formatSignatureDateTime(JSONObject signatureRecord) {
        String formatted = formatDateTime(firstNonBlank(
                signatureRecord.getString("signatureDisplayAt"),
                signatureRecord.getString("selectedSignedAt"),
                signatureRecord.getString("signedAt")));
        String timeZone = StrUtil.trim(signatureRecord.getString("selectedTimeZone"));
        if ("--".equals(formatted)) {
            throw new IllegalStateException("EDHR batch archive signature time is required, actionType="
                    + value(signatureRecord.getString("actionType")));
        }
        if (StrUtil.isBlank(timeZone)) {
            throw new IllegalStateException("EDHR batch archive signature time zone is required, actionType="
                    + value(signatureRecord.getString("actionType")));
        }
        return formatted + " (" + timeZone + ")";
    }

    private static String signatureMeaning(String actionType) {
        return switch (String.valueOf(actionType)) {
            case MesProBatchRecordExecutionSignatureService.ACTION_FIELD_CHANGE -> "字段变更";
            case MesProBatchRecordExecutionSignatureService.ACTION_FORM_REVIEW -> "表单复核";
            case MesProBatchRecordExecutionSignatureService.ACTION_SUBMIT -> "提交审批";
            case MesProBatchRecordExecutionSignatureService.ACTION_APPROVE -> "最终批准";
            case MesProBatchRecordExecutionSignatureService.ACTION_REVIEW_APPROVE -> "审核签名";
            case MesProBatchRecordExecutionSignatureService.ACTION_REJECT -> "审批驳回";
            case MesProBatchRecordExecutionSignatureService.ACTION_ARCHIVE_SEAL -> "归档封存";
            case MesProBatchRecordExecutionSignatureService.ACTION_PRODUCTION_SUBMIT -> "一线生产报工提交";
            case MesProBatchRecordExecutionSignatureService.ACTION_PQC_SUBMIT -> "PQC检验提交";
            case MesProBatchRecordExecutionSignatureService.ACTION_TEAM_LEADER_REVIEW -> "组长复核";
            default -> null;
        };
    }

    private static Map<String, JSONObject> parseCellValueMap(String cellValuesJson) {
        Map<String, JSONObject> map = new HashMap<>();
        if (StrUtil.isBlank(cellValuesJson)) {
            return map;
        }
        JSONArray array = JSON.parseArray(cellValuesJson);
        if (array == null) {
            return map;
        }
        for (Object item : array) {
            if (!(item instanceof JSONObject cellValue)) {
                continue;
            }
            Integer rowIndex = cellValue.getInteger("rowIndex");
            Integer columnIndex = cellValue.getInteger("columnIndex");
            if (rowIndex == null || columnIndex == null) {
                continue;
            }
            map.put(cellKey(rowIndex, columnIndex), cellValue);
        }
        return map;
    }

    private static Map<String, JSONObject> parseSignatureMarkerMap(JSONArray markers) {
        Map<String, JSONObject> map = new HashMap<>();
        if (markers == null) {
            return map;
        }
        for (Object item : markers) {
            if (!(item instanceof JSONObject marker)) {
                continue;
            }
            Integer rowIndex = marker.getInteger("rowIndex");
            Integer columnIndex = marker.getInteger("columnIndex");
            if (rowIndex == null || columnIndex == null || !Boolean.TRUE.equals(marker.getBoolean("enabled"))) {
                continue;
            }
            map.put(cellKey(rowIndex, columnIndex), marker);
        }
        return map;
    }

    private static Map<String, String> parseAttachmentRuleMap(String executionSnapshotJson) {
        Map<String, String> map = new HashMap<>();
        JSONObject snapshot = safeJsonObject(executionSnapshotJson);
        if (snapshot == null || snapshot.getJSONArray("fields") == null) {
            return map;
        }
        for (Object item : snapshot.getJSONArray("fields")) {
            if (!(item instanceof JSONObject field)) {
                continue;
            }
            Integer rowIndex = field.getInteger("rowIndex");
            Integer columnIndex = field.getInteger("columnIndex");
            if (rowIndex == null || columnIndex == null) {
                continue;
            }
            String text = formatAttachmentRule(field);
            if (StrUtil.isNotBlank(text)) {
                map.put(cellKey(rowIndex, columnIndex), text);
            }
        }
        return map;
    }

    private static Map<String, JSONObject> latestSignatureRecordByAction(JSONArray signatures) {
        Map<String, JSONObject> map = new HashMap<>();
        if (signatures == null) {
            return map;
        }
        List<JSONObject> records = new ArrayList<>();
        for (Object item : signatures) {
            if (item instanceof JSONObject record) {
                records.add(record);
            }
        }
        records.sort(Comparator.comparing(MesProEdhrBatchArchivePrintablePdfRenderer::signatureSortTime,
                Comparator.nullsLast(LocalDateTime::compareTo)));
        for (JSONObject record : records) {
            String actionType = StrUtil.blankToDefault(record.getString("actionType"), "");
            if (StrUtil.isNotBlank(actionType)) {
                map.put(actionType, record);
            }
        }
        return map;
    }

    private static LocalDateTime signatureSortTime(JSONObject record) {
        return parseDateTime(firstNonBlank(
                record.getString("signatureDisplayAt"),
                record.getString("selectedSignedAt"),
                record.getString("signedAt")));
    }

    private static String formatAttachmentRule(JSONObject field) {
        JSONObject rule = field.getJSONObject("attachmentRule");
        if (rule == null || rule.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        parts.add(Boolean.TRUE.equals(rule.getBoolean("required")) ? "必需附件" : "可选附件");
        Integer minCount = rule.getInteger("minCount");
        if (minCount != null && minCount > 0) {
            parts.add("至少 " + minCount + " 个");
        }
        Integer maxCount = rule.getInteger("maxCount");
        if (maxCount != null && maxCount > 0) {
            parts.add("最多 " + maxCount + " 个");
        }
        String groupKey = StrUtil.trim(rule.getString("groupKey"));
        if (StrUtil.isNotBlank(groupKey)) {
            parts.add("组 " + groupKey);
        }
        return String.join("，", parts);
    }

    private static String stringifyCellValue(JSONObject cellValue) {
        String valueType = StrUtil.blankToDefault(cellValue.getString("valueType"), "").toUpperCase();
        String display = StrUtil.isNotBlank(cellValue.getString("valueDisplay"))
                ? cellValue.getString("valueDisplay").trim()
                : stringifyValue(cellValue.get("value"));
        if ("BOOLEAN".equals(valueType)) {
            Object value = cellValue.get("value");
            boolean checked = Boolean.TRUE.equals(value)
                    || "true".equalsIgnoreCase(String.valueOf(value));
            return checked ? "☑" : "☐";
        }
        if ("NUMBER".equals(valueType)) {
            String unit = StrUtil.trim(cellValue.getString("unit"));
            return StrUtil.isBlank(unit) ? display : display + " " + unit;
        }
        return "null".equals(display) ? "" : display;
    }

    private static String stringifyValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String string) {
            return string;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return JSON.toJSONString(value);
    }

    private static boolean isSectionTitle(JSONObject rawCell, Merge merge, int columnCount) {
        String text = rawCell == null ? "" : StrUtil.trim(rawCell.getString("text"));
        return StrUtil.isNotBlank(text) && merge.colSpan() >= Math.max(3, Math.floorDiv(columnCount, 2));
    }

    private static Color determineCellBackground(boolean fillable, boolean signatureCell,
                                                 boolean sectionTitle, String attachmentRuleText) {
        if (sectionTitle) {
            return SECTION_BG;
        }
        if (signatureCell) {
            return SIGNATURE_BG;
        }
        if (StrUtil.isNotBlank(attachmentRuleText)) {
            return ATTACHMENT_BG;
        }
        return fillable ? FILLABLE_BG : STATIC_BG;
    }

    private static Color determineCellTextColor(boolean fillable, boolean signatureCell, String text) {
        if (signatureCell) {
            return "未签名".equals(text) ? MUTED_TEXT : FILLABLE_TEXT;
        }
        if (fillable) {
            return StrUtil.isBlank(text) ? MUTED_TEXT : FILLABLE_TEXT;
        }
        return StrUtil.isBlank(text) ? MUTED_TEXT : TEXT_COLOR;
    }

    private static float estimateCellHeight(String text, String attachmentRuleText, float width) {
        float textWidth = Math.max(24F, width - (CELL_PADDING * 2));
        int lineCount = Math.max(1, estimateLineCount(text, textWidth, TABLE_FONT_SIZE));
        int ruleLineCount = StrUtil.isBlank(attachmentRuleText) ? 0
                : estimateLineCount(attachmentRuleText, textWidth, TABLE_RULE_FONT_SIZE);
        float height = (CELL_PADDING * 2) + (lineCount * TABLE_LINE_HEIGHT);
        if (ruleLineCount > 0) {
            height += 4F + (ruleLineCount * (TABLE_LINE_HEIGHT - 1F));
        }
        return Math.max(DEFAULT_ROW_HEIGHT, height);
    }

    private static int estimateLineCount(String text, float maxWidth, float fontSize) {
        if (StrUtil.isBlank(text)) {
            return 1;
        }
        int approxCharsPerLine = Math.max(1, (int) Math.floor(maxWidth / Math.max(fontSize, 1F)));
        int count = 0;
        for (String paragraph : text.split("\\R", -1)) {
            count += Math.max(1, (int) Math.ceil((double) paragraph.length() / approxCharsPerLine));
        }
        return Math.max(count, 1);
    }

    private static String joinAttachmentNames(List<JSONObject> attachments) {
        return attachments.stream()
                .map(attachment -> joinNonBlank(
                        attachment.getString("fileName"),
                        attachment.getString("fieldLabel"),
                        attachment.getString("attachmentType")))
                .filter(StrUtil::isNotBlank)
                .reduce((left, right) -> left + "；" + right)
                .orElse("");
    }

    private static List<JSONObject> jsonArrayObjects(JSONObject root, String key) {
        if (root == null || root.getJSONArray(key) == null) {
            return List.of();
        }
        return root.getJSONArray(key).toJavaList(JSONObject.class);
    }

    private static JSONObject safeJsonObject(String rawJson) {
        if (StrUtil.isBlank(rawJson)) {
            return null;
        }
        try {
            return JSON.parseObject(rawJson);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String cellKey(int rowIndex, int columnIndex) {
        return rowIndex + ":" + columnIndex;
    }

    private static String joinNonBlank(String... values) {
        if (values == null || values.length == 0) {
            return "";
        }
        return java.util.Arrays.stream(values)
                .filter(StrUtil::isNotBlank)
                .reduce((left, right) -> left + " / " + right)
                .orElse("");
    }

    private static String formatDateTime(String raw) {
        if (StrUtil.isBlank(raw)) {
            return "--";
        }
        LocalDateTime parsed = parseDateTime(raw);
        return parsed == null ? raw : DISPLAY_TIME.format(parsed);
    }

    private static LocalDateTime parseDateTime(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw);
        } catch (RuntimeException ex) {
            // Fastjson serializes LocalDateTime from MySQL-backed rows as "yyyy-MM-dd HH:mm:ss".
            try {
                return LocalDateTime.parse(raw, DATABASE_TIME_SECONDS);
            } catch (RuntimeException ignored) {
                try {
                    return LocalDateTime.parse(raw, DATABASE_TIME_MINUTES);
                } catch (RuntimeException ignoredAgain) {
                    return null;
                }
            }
        }
    }

    private static String value(Object value) {
        return value == null ? "--" : String.valueOf(value);
    }

    private static String firstNonBlank(String first, String second, String third) {
        if (StrUtil.isNotBlank(first)) {
            return first;
        }
        if (StrUtil.isNotBlank(second)) {
            return second;
        }
        return third;
    }

    private static Object firstNonBlank(Object first, Object second) {
        if (!StrUtil.isBlankIfStr(first)) {
            return first;
        }
        return second;
    }

    private static String formatTaskStatus(Integer status) {
        if (Objects.equals(status, MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_DRAFT)) {
            return "草稿";
        }
        if (Objects.equals(status, MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_SUBMITTED)) {
            return "已提交";
        }
        if (Objects.equals(status, MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_REJECTED)) {
            return "已驳回";
        }
        if (Objects.equals(status, MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_REWORK_REQUIRED)) {
            return "待返工";
        }
        if (Objects.equals(status, MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED)) {
            return "填写完成";
        }
        if (Objects.equals(status, MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_SKIPPED)) {
            return "已跳过";
        }
        if (Objects.equals(status, MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_BLOCKED)) {
            return "已阻塞";
        }
        if (Objects.equals(status, MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING)) {
            return "待处理";
        }
        return value(status);
    }

    private static Merge normalizeMerge(JSONObject rawCell) {
        JSONArray merge = rawCell == null ? null : rawCell.getJSONArray("merge");
        if (merge == null || merge.size() < 2) {
            return new Merge(1, 1);
        }
        Integer rowDelta = merge.getInteger(0);
        Integer columnDelta = merge.getInteger(1);
        int rowSpan = rowDelta == null || rowDelta < 0 ? 1 : rowDelta + 1;
        int colSpan = columnDelta == null || columnDelta < 0 ? 1 : columnDelta + 1;
        return new Merge(rowSpan, colSpan);
    }

    private record Merge(int rowSpan, int colSpan) {
    }

    private static final class PdfCanvas {

        private final PDDocument document;
        private final PDType0Font font;
        private final PDType0Font symbolFont;
        private PDPageContentStream contentStream;
        private float cursorY;

        private PdfCanvas(PDDocument document, PDType0Font font, PDType0Font symbolFont) throws IOException {
            this.document = document;
            this.font = font;
            this.symbolFont = symbolFont;
            addPage();
        }

        private void writeTitle(String text) throws IOException {
            writeTextBlock(text, TITLE_FONT_SIZE, MARGIN, new Color(15, 23, 42), true);
            cursorY -= 8F;
        }

        private void writeSectionTitle(String text) throws IOException {
            writeTextBlock(text, SECTION_FONT_SIZE, MARGIN, new Color(15, 23, 42), true);
        }

        private void writeSubTitle(String text) throws IOException {
            writeTextBlock(text, 11F, MARGIN, TEXT_COLOR, true);
        }

        private void writeKeyValue(String label, String value) throws IOException {
            writeParagraph(label + "：" + StrUtil.blankToDefault(value, "--"));
        }

        private void writeParagraph(String text) throws IOException {
            writeTextBlock(text, TEXT_FONT_SIZE, MARGIN, TEXT_COLOR, false);
        }

        private void writeBlankLine() {
            cursorY -= 6F;
        }

        private void ensureTableSpace(float requiredHeight) throws IOException {
            if (cursorY - requiredHeight >= MARGIN) {
                return;
            }
            addPage();
        }

        private void drawCell(float x, float rowTop, float width, float height, Color background,
                              String text, String attachmentRuleText, Color textColor) throws IOException {
            float bottomY = rowTop - height;
            contentStream.setNonStrokingColor(background);
            contentStream.addRect(x, bottomY, width, height);
            contentStream.fill();
            contentStream.setStrokingColor(BORDER_COLOR);
            contentStream.addRect(x, bottomY, width, height);
            contentStream.stroke();
            float textWidth = Math.max(16F, width - (CELL_PADDING * 2));
            List<String> textLines = wrapText(text, TABLE_FONT_SIZE, textWidth);
            float textY = rowTop - CELL_PADDING - TABLE_FONT_SIZE;
            writeInlineLines(textLines, x + CELL_PADDING, textY, TABLE_FONT_SIZE, textColor,
                    TABLE_LINE_HEIGHT, bottomY + CELL_PADDING + 2F);
            if (StrUtil.isNotBlank(attachmentRuleText)) {
                List<String> ruleLines = wrapText(attachmentRuleText, TABLE_RULE_FONT_SIZE, textWidth);
                float ruleY = Math.min(textY - (textLines.size() * TABLE_LINE_HEIGHT) - 1F,
                        rowTop - height + (TABLE_LINE_HEIGHT * ruleLines.size()) + CELL_PADDING + 4F);
                writeInlineLines(ruleLines, x + CELL_PADDING, ruleY, TABLE_RULE_FONT_SIZE, RULE_TEXT,
                        TABLE_LINE_HEIGHT - 1F, bottomY + CELL_PADDING + 2F);
            }
        }

        private void consumeTableRow(float rowHeight) {
            cursorY -= rowHeight;
        }

        private float cursorY() {
            return cursorY;
        }

        private float margin() {
            return MARGIN;
        }

        private float availableWidth() {
            return PAGE_SIZE.getWidth() - (MARGIN * 2);
        }

        private void newPage() throws IOException {
            addPage();
        }

        private void writeTextBlock(String text, float fontSize, float x, Color color, boolean boldGap)
                throws IOException {
            List<String> lines = wrapText(text, fontSize, PAGE_SIZE.getWidth() - (x + MARGIN));
            float requiredHeight = Math.max(LINE_HEIGHT, lines.size() * LINE_HEIGHT);
            if (cursorY - requiredHeight < MARGIN) {
                addPage();
            }
            contentStream.beginText();
            contentStream.setNonStrokingColor(color);
            contentStream.newLineAtOffset(x, cursorY);
            boolean first = true;
            for (String line : lines) {
                if (!first) {
                    contentStream.newLineAtOffset(0, -LINE_HEIGHT);
                }
                showTextWithFallback(line, fontSize);
                first = false;
            }
            contentStream.endText();
            cursorY -= requiredHeight + (boldGap ? 4F : 0F);
        }

        private void writeInlineLines(List<String> lines, float x, float topY, float fontSize, Color color,
                                      float lineHeight, float minY) throws IOException {
            float currentY = topY;
            for (String line : lines) {
                if (currentY < minY) {
                    break;
                }
                contentStream.beginText();
                contentStream.setNonStrokingColor(color);
                contentStream.newLineAtOffset(x, currentY);
                showTextWithFallback(line, fontSize);
                contentStream.endText();
                currentY -= lineHeight;
            }
        }

        private void addPage() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
            PDPage page = new PDPage(PAGE_SIZE);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            cursorY = page.getMediaBox().getHeight() - MARGIN;
        }

        private void close() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
        }

        private List<String> wrapText(String text, float fontSize, float maxWidth) throws IOException {
            String source = sanitize(text);
            if (source.isEmpty()) {
                return List.of("");
            }
            List<String> result = new ArrayList<>();
            for (String paragraph : source.split("\\R", -1)) {
                if (paragraph.isEmpty()) {
                    result.add("");
                    continue;
                }
                StringBuilder builder = new StringBuilder();
                for (int index = 0; index < paragraph.length(); index++) {
                    char current = paragraph.charAt(index);
                    builder.append(current);
                    if (measureTextWidth(builder.toString(), fontSize) <= maxWidth) {
                        continue;
                    }
                    builder.deleteCharAt(builder.length() - 1);
                    if (builder.isEmpty()) {
                        result.add(String.valueOf(current));
                        builder = new StringBuilder();
                    } else {
                        result.add(builder.toString());
                        builder = new StringBuilder().append(current);
                    }
                }
                if (!builder.isEmpty()) {
                    result.add(builder.toString());
                }
            }
            return result;
        }

        private String sanitize(String value) {
            return StrUtil.nullToEmpty(value).replace('\t', ' ').replace('\r', ' ');
        }

        private float measureTextWidth(String text, float fontSize) throws IOException {
            float width = 0F;
            for (char current : sanitize(text).toCharArray()) {
                width += resolveFont(current).getStringWidth(String.valueOf(current)) / 1000 * fontSize;
            }
            return width;
        }

        private void showTextWithFallback(String text, float fontSize) throws IOException {
            String sanitized = sanitize(text);
            if (sanitized.isEmpty()) {
                return;
            }
            StringBuilder segment = new StringBuilder();
            PDFont activeFont = null;
            for (char current : sanitized.toCharArray()) {
                PDFont nextFont = resolveFont(current);
                if (activeFont != null && activeFont != nextFont && !segment.isEmpty()) {
                    contentStream.setFont(activeFont, fontSize);
                    contentStream.showText(segment.toString());
                    segment.setLength(0);
                }
                activeFont = nextFont;
                segment.append(current);
            }
            if (activeFont != null && !segment.isEmpty()) {
                contentStream.setFont(activeFont, fontSize);
                contentStream.showText(segment.toString());
            }
        }

        private PDFont resolveFont(char current) throws IOException {
            if (canEncode(symbolFont, current) && !canEncode(font, current)) {
                return symbolFont;
            }
            if (canEncode(font, current)) {
                return font;
            }
            if (canEncode(symbolFont, current)) {
                return symbolFont;
            }
            throw new IllegalArgumentException("No glyph for U+"
                    + String.format("%04X", (int) current)
                    + " (" + current + ") in configured printable PDF fonts");
        }

        private boolean canEncode(PDFont candidate, char current) {
            try {
                candidate.encode(String.valueOf(current));
                return true;
            } catch (IllegalArgumentException ex) {
                return false;
            } catch (IOException ex) {
                return false;
            }
        }
    }
}
