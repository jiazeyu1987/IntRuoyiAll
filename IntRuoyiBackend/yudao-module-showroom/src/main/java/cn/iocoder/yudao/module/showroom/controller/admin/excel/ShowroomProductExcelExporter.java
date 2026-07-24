package cn.iocoder.yudao.module.showroom.controller.admin.excel;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.converters.longconverter.LongStringConverter;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.excel.core.handler.ColumnWidthMatchStyleStrategy;
import cn.iocoder.yudao.framework.excel.core.handler.SelectSheetWriteHandler;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductShowroomWorkbookRowDTO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.award.ShowroomAwardExcelExportRow;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.product.ShowroomProductExcelVO;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ShowroomProductExcelExporter {

    private static final Pattern ADMIN_FILE_URL = Pattern.compile("^/admin-api/infra/file/(\\d+)/get/(.+)$");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final String PRODUCT_IMAGE_HEADER = "产品图";
    private static final String SELLING_POINTS_COPY_HEADER = "卖点文案";
    private static final String AWARD_SHEET_NAME = "奖项";
    private static final String PRODUCT_MASTER_SHEET_NAME = "产品主数据";
    private static final String NARRATION_SHEET_NAME = "讲解音频";
    private static final String KEYWORD_SHEET_NAME = "关键词中英对照";
    private static final int AWARD_SEQUENCE_COLUMN = 0;
    private static final int AWARD_NAME_CN_COLUMN = 1;
    private static final int AWARD_DATE_COLUMN = 2;
    private static final int AWARD_ISSUER_COLUMN = 3;
    private static final int AWARD_COVER_COLUMN = 4;
    private static final byte[] TEMPLATE_AWARD_COVER_IMAGE = java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9WnXl1QAAAAASUVORK5CYII=");

    private ShowroomProductExcelExporter() {
    }

    public static void write(HttpServletResponse response, String filename, String sheetName,
                             List<ShowroomProductExcelVO> rows, FileService fileService) throws IOException {
        byte[] content = buildWorkbook(sheetName, rows, List.of(), List.of(), List.of(), List.of(), fileService);
        writeResponse(response, filename, content);
    }

    public static void write(HttpServletResponse response, String filename, String productSheetName,
                             List<ShowroomProductExcelVO> productRows,
                             List<ShowroomAwardExcelExportRow> awardRows,
                             List<ShowroomNarrationExcelRow> narrationRows,
                             List<ShowroomKeywordExcelRow> keywordRows,
                             FileService fileService) throws IOException {
        byte[] content = buildWorkbook(productSheetName, productRows, List.of(), awardRows, narrationRows, keywordRows,
                fileService);
        writeResponse(response, filename, content);
    }

    public static void writeTemplate(HttpServletResponse response, String filename, String productSheetName,
                                     List<ShowroomProductExcelVO> productRows,
                                     List<ShowroomKeywordExcelRow> keywordRows) throws IOException {
        List<ShowroomAwardExcelExportRow> awardRows = List.of(new ShowroomAwardExcelExportRow(
                "AWARD-001", "001", "示例奖项", "2026年度", "示例颁发单位", "",
                TEMPLATE_AWARD_COVER_IMAGE));
        byte[] content = buildWorkbook(productSheetName, productRows, List.of(), awardRows, List.of(), keywordRows, null);
        writeResponse(response, filename, content);
    }

    private static void writeResponse(HttpServletResponse response, String filename, byte[] content) throws IOException {
        response.getOutputStream().write(content);
        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(filename));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
    }

    public static byte[] buildWorkbook(String sheetName, List<ShowroomProductExcelVO> rows,
                                       List<MdmProductShowroomWorkbookRowDTO> productMasterRows,
                                       List<ShowroomAwardExcelExportRow> awardRows,
                                       List<ShowroomNarrationExcelRow> narrationRows,
                                       List<ShowroomKeywordExcelRow> keywordRows,
                                       FileService fileService) throws IOException {
        try (ByteArrayOutputStream baseOutput = new ByteArrayOutputStream()) {
            FastExcelFactory.write(baseOutput, ShowroomProductExcelVO.class)
                    .autoCloseStream(false)
                    .registerWriteHandler(new ColumnWidthMatchStyleStrategy())
                    .registerWriteHandler(new SelectSheetWriteHandler(ShowroomProductExcelVO.class))
                    .registerConverter(new LongStringConverter())
                    .sheet(sheetName)
                    .doWrite(rows);
            try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baseOutput.toByteArray()));
                 ByteArrayOutputStream finalOutput = new ByteArrayOutputStream()) {
                Sheet sheet = workbook.getSheet(sheetName);
                if (sheet == null) {
                    throw new IllegalStateException("SHOWROOM_PRODUCT_EXPORT_EXCEL_BROKEN: 产品列表工作表不存在");
                }
                int sellingPointsColumn = requireHeaderColumn(sheet, SELLING_POINTS_COPY_HEADER);
                int productImageColumn = requireHeaderColumn(sheet, PRODUCT_IMAGE_HEADER);
                applySellingPointsWrap(sheet, sellingPointsColumn);
                embedProductImages(workbook, sheet, productImageColumn, rows, fileService);
                writeProductMasterSheet(workbook, productMasterRows);
                writeAwardSheet(workbook, awardRows, fileService);
                writeNarrationSheet(workbook, narrationRows);
                writeKeywordSheet(workbook, keywordRows);
                workbook.write(finalOutput);
                return finalOutput.toByteArray();
            }
        }
    }

    private static int requireHeaderColumn(Sheet sheet, String header) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_EXPORT_EXCEL_BROKEN: 产品列表表头为空");
        }
        for (int cellIndex = 0; cellIndex < headerRow.getLastCellNum(); cellIndex++) {
            Cell cell = headerRow.getCell(cellIndex);
            if (cell != null && header.equals(cell.getStringCellValue())) {
                return cellIndex;
            }
        }
        throw new IllegalStateException("SHOWROOM_PRODUCT_EXPORT_EXCEL_BROKEN: 产品列表缺少表头 " + header);
    }

    private static void applySellingPointsWrap(Sheet sheet, int sellingPointsColumn) {
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            Cell cell = row.getCell(sellingPointsColumn);
            if (cell == null) {
                cell = row.createCell(sellingPointsColumn);
            }
            CellStyle style = sheet.getWorkbook().createCellStyle();
            style.cloneStyleFrom(cell.getCellStyle());
            style.setWrapText(true);
            cell.setCellStyle(style);
        }
    }

    private static void embedProductImages(Workbook workbook, Sheet sheet, int productImageColumn,
                                           List<ShowroomProductExcelVO> rows, FileService fileService) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        sheet.setColumnWidth(productImageColumn, Math.max(sheet.getColumnWidth(productImageColumn), 18 * 256));
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        CreationHelper creationHelper = workbook.getCreationHelper();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            ShowroomProductExcelVO row = rows.get(rowIndex);
            String coverImage = normalizeText(row == null ? null : row.getCoverImage());
            if (coverImage.isEmpty()) {
                continue;
            }
            byte[] content = resolveCoverImageContent(ExportImageContext.product(row), coverImage, fileService);
            int pictureType = resolvePictureType(ExportImageContext.product(row), coverImage, content);
            Row sheetRow = sheet.getRow(rowIndex + 1);
            if (sheetRow == null) {
                sheetRow = sheet.createRow(rowIndex + 1);
            }
            sheetRow.setHeightInPoints(Math.max(sheetRow.getHeightInPoints(), 72F));
            Cell productImageCell = sheetRow.getCell(productImageColumn);
            if (productImageCell == null) {
                productImageCell = sheetRow.createCell(productImageColumn);
            }
            productImageCell.setCellValue("");
            int pictureIndex = workbook.addPicture(content, pictureType);
            ClientAnchor anchor = creationHelper.createClientAnchor();
            anchor.setCol1(productImageColumn);
            anchor.setRow1(rowIndex + 1);
            anchor.setCol2(productImageColumn + 1);
            anchor.setRow2(rowIndex + 2);
            anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
            drawing.createPicture(anchor, pictureIndex);
        }
    }

    private static void writeProductMasterSheet(Workbook workbook, List<MdmProductShowroomWorkbookRowDTO> rows) {
        Sheet existingSheet = workbook.getSheet(PRODUCT_MASTER_SHEET_NAME);
        if (existingSheet != null) {
            workbook.removeSheetAt(workbook.getSheetIndex(existingSheet));
        }
        Sheet sheet = workbook.createSheet(PRODUCT_MASTER_SHEET_NAME);
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("产品编码");
        headerRow.createCell(1).setCellValue("DCC产品编号");
        headerRow.createCell(2).setCellValue("中文名称");
        headerRow.createCell(3).setCellValue("英文名称");
        headerRow.createCell(4).setCellValue("型号规格");
        headerRow.createCell(5).setCellValue("产品分类");
        sheet.setColumnWidth(0, 22 * 256);
        sheet.setColumnWidth(1, 22 * 256);
        sheet.setColumnWidth(2, 28 * 256);
        sheet.setColumnWidth(3, 32 * 256);
        sheet.setColumnWidth(4, 28 * 256);
        sheet.setColumnWidth(5, 20 * 256);
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            MdmProductShowroomWorkbookRowDTO row = rows.get(rowIndex);
            Row sheetRow = sheet.createRow(rowIndex + 1);
            sheetRow.createCell(0).setCellValue(normalizeText(row == null ? null : row.getProductCode()));
            sheetRow.createCell(1).setCellValue(normalizeText(row == null ? null : row.getDccProductCode()));
            sheetRow.createCell(2).setCellValue(normalizeText(row == null ? null : row.getNameCn()));
            sheetRow.createCell(3).setCellValue(normalizeText(row == null ? null : row.getNameEn()));
            sheetRow.createCell(4).setCellValue(normalizeText(row == null ? null : row.getModelSpecification()));
            sheetRow.createCell(5).setCellValue(normalizeText(row == null ? null : row.getCategory()));
        }
    }

    private static void writeAwardSheet(Workbook workbook, List<ShowroomAwardExcelExportRow> awardRows,
                                        FileService fileService) {
        Sheet existingSheet = workbook.getSheet(AWARD_SHEET_NAME);
        if (existingSheet != null) {
            workbook.removeSheetAt(workbook.getSheetIndex(existingSheet));
        }
        Sheet sheet = workbook.createSheet(AWARD_SHEET_NAME);
        sheet.setColumnWidth(AWARD_SEQUENCE_COLUMN, 12 * 256);
        sheet.setColumnWidth(AWARD_NAME_CN_COLUMN, 24 * 256);
        sheet.setColumnWidth(AWARD_DATE_COLUMN, 18 * 256);
        sheet.setColumnWidth(AWARD_ISSUER_COLUMN, 24 * 256);
        sheet.setColumnWidth(AWARD_COVER_COLUMN, 18 * 256);
        if (awardRows == null || awardRows.isEmpty()) {
            return;
        }
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        CreationHelper creationHelper = workbook.getCreationHelper();
        for (int rowIndex = 0; rowIndex < awardRows.size(); rowIndex++) {
            ShowroomAwardExcelExportRow awardRow = awardRows.get(rowIndex);
            ExportImageContext context = ExportImageContext.award(awardRow);
            Row sheetRow = sheet.createRow(rowIndex);
            sheetRow.setHeightInPoints(Math.max(sheetRow.getHeightInPoints(), 72F));
            sheetRow.createCell(AWARD_SEQUENCE_COLUMN).setCellValue(requireAwardSequence(awardRow));
            sheetRow.createCell(AWARD_NAME_CN_COLUMN).setCellValue(normalizeText(awardRow == null ? null : awardRow.nameCn()));
            sheetRow.createCell(AWARD_DATE_COLUMN).setCellValue(normalizeText(awardRow == null ? null : awardRow.awardDateText()));
            sheetRow.createCell(AWARD_ISSUER_COLUMN).setCellValue(normalizeText(awardRow == null ? null : awardRow.issuer()));
            sheetRow.createCell(AWARD_COVER_COLUMN).setCellValue("");
            byte[] content = resolveAwardCoverImageContent(awardRow, context, fileService);
            int pictureType = resolvePictureType(context, normalizeText(awardRow.coverImage()), content);
            int pictureIndex = workbook.addPicture(content, pictureType);
            ClientAnchor anchor = creationHelper.createClientAnchor();
            anchor.setCol1(AWARD_COVER_COLUMN);
            anchor.setRow1(rowIndex);
            anchor.setCol2(AWARD_COVER_COLUMN + 1);
            anchor.setRow2(rowIndex + 1);
            anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
            drawing.createPicture(anchor, pictureIndex);
        }
    }

    private static void writeNarrationSheet(Workbook workbook, List<ShowroomNarrationExcelRow> narrationRows) {
        Sheet existingSheet = workbook.getSheet(NARRATION_SHEET_NAME);
        if (existingSheet != null) {
            workbook.removeSheetAt(workbook.getSheetIndex(existingSheet));
        }
        Sheet sheet = workbook.createSheet(NARRATION_SHEET_NAME);
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("目标类型");
        headerRow.createCell(1).setCellValue("目标编码");
        headerRow.createCell(2).setCellValue("目标名称");
        headerRow.createCell(3).setCellValue("语言");
        headerRow.createCell(4).setCellValue("讲解稿");
        headerRow.createCell(5).setCellValue("音频文件ID");
        headerRow.createCell(6).setCellValue("音频地址");
        headerRow.createCell(7).setCellValue("音频时长(秒)");
        headerRow.createCell(8).setCellValue("音色");
        sheet.setColumnWidth(0, 14 * 256);
        sheet.setColumnWidth(1, 20 * 256);
        sheet.setColumnWidth(2, 24 * 256);
        sheet.setColumnWidth(3, 10 * 256);
        sheet.setColumnWidth(4, 48 * 256);
        sheet.setColumnWidth(5, 18 * 256);
        sheet.setColumnWidth(6, 56 * 256);
        sheet.setColumnWidth(7, 16 * 256);
        sheet.setColumnWidth(8, 18 * 256);
        if (narrationRows == null || narrationRows.isEmpty()) {
            return;
        }
        for (int rowIndex = 0; rowIndex < narrationRows.size(); rowIndex++) {
            ShowroomNarrationExcelRow row = narrationRows.get(rowIndex);
            Row sheetRow = sheet.createRow(rowIndex + 1);
            sheetRow.createCell(0).setCellValue(normalizeText(row.targetType()));
            sheetRow.createCell(1).setCellValue(normalizeText(row.targetCode()));
            sheetRow.createCell(2).setCellValue(normalizeText(row.targetName()));
            sheetRow.createCell(3).setCellValue(normalizeText(row.language()));
            sheetRow.createCell(4).setCellValue(normalizeText(row.scriptText()));
            sheetRow.createCell(5).setCellValue(row.audioFileId() == null ? "" : String.valueOf(row.audioFileId()));
            sheetRow.createCell(6).setCellValue(normalizeText(row.audioUrl()));
            sheetRow.createCell(7).setCellValue(row.audioDurationSeconds() == null
                    ? ""
                    : String.valueOf(row.audioDurationSeconds()));
            sheetRow.createCell(8).setCellValue(normalizeText(row.voice()));
        }
    }

    private static void writeKeywordSheet(Workbook workbook, List<ShowroomKeywordExcelRow> keywordRows) {
        Sheet existingSheet = workbook.getSheet(KEYWORD_SHEET_NAME);
        if (existingSheet != null) {
            workbook.removeSheetAt(workbook.getSheetIndex(existingSheet));
        }
        Sheet sheet = workbook.createSheet(KEYWORD_SHEET_NAME);
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue(ShowroomKeywordExcelImportExtras.NAME_ZH_HEADER);
        headerRow.createCell(1).setCellValue(ShowroomKeywordExcelImportExtras.NAME_EN_HEADER);
        sheet.setColumnWidth(0, 26 * 256);
        sheet.setColumnWidth(1, 32 * 256);
        if (keywordRows == null || keywordRows.isEmpty()) {
            return;
        }
        for (int rowIndex = 0; rowIndex < keywordRows.size(); rowIndex++) {
            ShowroomKeywordExcelRow row = keywordRows.get(rowIndex);
            Row sheetRow = sheet.createRow(rowIndex + 1);
            sheetRow.createCell(0).setCellValue(normalizeText(row.nameZh()));
            sheetRow.createCell(1).setCellValue(normalizeText(row.nameEn()));
        }
    }

    private static String requireAwardSequence(ShowroomAwardExcelExportRow row) {
        String sequence = normalizeText(row == null ? null : row.sequenceText());
        if (!sequence.isEmpty()) {
            return sequence;
        }
        String awardCode = normalizeText(row == null ? null : row.awardCode());
        Matcher matcher = Pattern.compile("^AWARD-(\\d+)$").matcher(awardCode);
        if (!matcher.matches()) {
            throw new IllegalStateException("SHOWROOM_AWARD_EXPORT_CODE_INVALID: 奖项编码无法导出序号，奖项编码 "
                    + awardCode(row) + "，奖项名称 " + awardName(row));
        }
        return matcher.group(1);
    }

    private static byte[] resolveAwardCoverImageContent(ShowroomAwardExcelExportRow row, ExportImageContext context,
                                                        FileService fileService) {
        byte[] inlineContent = row == null ? null : row.coverImageContent();
        if (inlineContent != null && inlineContent.length > 0) {
            return inlineContent;
        }
        String coverImage = normalizeText(row == null ? null : row.coverImage());
        if (coverImage.isEmpty()) {
            throw new IllegalStateException("SHOWROOM_AWARD_EXPORT_IMAGE_MISSING: 奖项封面不能为空，奖项编码 "
                    + awardCode(row) + "，奖项名称 " + awardName(row));
        }
        return resolveCoverImageContent(context, coverImage, fileService);
    }

    private static byte[] resolveCoverImageContent(ExportImageContext context, String coverImage,
                                                   FileService fileService) {
        Matcher matcher = ADMIN_FILE_URL.matcher(coverImage);
        if (matcher.matches()) {
            return resolveInternalCoverImageContent(context, coverImage, matcher, fileService);
        }
        return resolveExternalCoverImageContent(context, coverImage);
    }

    private static byte[] resolveInternalCoverImageContent(ExportImageContext context, String coverImage,
                                                           Matcher matcher, FileService fileService) {
        if (fileService == null) {
            throw new IllegalStateException(context.missingCode() + ": " + context.label()
                    + "文件服务不可用，" + context.identity());
        }
        Long configId = Long.valueOf(matcher.group(1));
        String path = URLDecoder.decode(matcher.group(2), StandardCharsets.UTF_8);
        try {
            byte[] content = fileService.getFileContent(configId, path);
            if (content == null || content.length == 0) {
                throw new IllegalStateException(context.missingCode() + ": " + context.label()
                        + "封面文件为空，" + context.identity() + "，cover_image=" + coverImage);
            }
            return content;
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(context.missingCode() + ": " + context.label()
                    + "封面文件读取失败，" + context.identity() + "，cover_image=" + coverImage, exception);
        }
    }

    private static byte[] resolveExternalCoverImageContent(ExportImageContext context, String coverImage) {
        URI uri;
        try {
            uri = new URI(coverImage);
        } catch (URISyntaxException exception) {
            throw new IllegalStateException(context.unsupportedCode() + ": " + context.label()
                    + "封面地址不支持，" + context.identity() + "，cover_image=" + coverImage, exception);
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalStateException(context.unsupportedCode() + ": " + context.label()
                    + "封面地址不支持，" + context.identity() + "，cover_image=" + coverImage);
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(context.missingCode() + ": " + context.label()
                        + "封面文件读取失败，" + context.identity() + "，cover_image=" + coverImage
                        + "，status=" + response.statusCode());
            }
            byte[] content = response.body();
            if (content == null || content.length == 0) {
                throw new IllegalStateException(context.missingCode() + ": " + context.label()
                        + "封面文件为空，" + context.identity() + "，cover_image=" + coverImage);
            }
            return content;
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(context.missingCode() + ": " + context.label()
                    + "封面文件读取失败，" + context.identity() + "，cover_image=" + coverImage, exception);
        }
    }

    private static int resolvePictureType(ExportImageContext context, String coverImage, byte[] content) {
        if (isPng(content)) {
            return Workbook.PICTURE_TYPE_PNG;
        }
        if (isJpeg(content)) {
            return Workbook.PICTURE_TYPE_JPEG;
        }
        throw new IllegalStateException(context.unsupportedCode() + ": " + context.label()
                + "封面图片格式不支持，" + context.identity() + "，cover_image=" + coverImage);
    }

    private static boolean isPng(byte[] content) {
        return content.length >= 8
                && (content[0] & 0xFF) == 0x89
                && content[1] == 0x50
                && content[2] == 0x4E
                && content[3] == 0x47
                && content[4] == 0x0D
                && content[5] == 0x0A
                && content[6] == 0x1A
                && content[7] == 0x0A;
    }

    private static boolean isJpeg(byte[] content) {
        return content.length >= 3
                && (content[0] & 0xFF) == 0xFF
                && (content[1] & 0xFF) == 0xD8
                && (content[2] & 0xFF) == 0xFF;
    }

    private static String productCode(ShowroomProductExcelVO row) {
        String productCode = normalizeText(row == null ? null : row.getProductCode());
        return productCode.isEmpty() ? "<unknown>" : productCode;
    }

    private static String awardCode(ShowroomAwardExcelExportRow row) {
        String awardCode = normalizeText(row == null ? null : row.awardCode());
        return awardCode.isEmpty() ? "<unknown>" : awardCode;
    }

    private static String awardName(ShowroomAwardExcelExportRow row) {
        String awardName = normalizeText(row == null ? null : row.nameCn());
        return awardName.isEmpty() ? "<unknown>" : awardName;
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private record ExportImageContext(String label, String identity, String missingCode, String unsupportedCode) {

        static ExportImageContext product(ShowroomProductExcelVO row) {
            return new ExportImageContext("产品图", "产品编码 " + productCode(row),
                    "SHOWROOM_PRODUCT_EXPORT_IMAGE_MISSING", "SHOWROOM_PRODUCT_EXPORT_IMAGE_UNSUPPORTED");
        }

        static ExportImageContext award(ShowroomAwardExcelExportRow row) {
            return new ExportImageContext("奖项", "奖项编码 " + awardCode(row) + "，奖项名称 " + awardName(row),
                    "SHOWROOM_AWARD_EXPORT_IMAGE_MISSING", "SHOWROOM_AWARD_EXPORT_IMAGE_UNSUPPORTED");
        }
    }
}
