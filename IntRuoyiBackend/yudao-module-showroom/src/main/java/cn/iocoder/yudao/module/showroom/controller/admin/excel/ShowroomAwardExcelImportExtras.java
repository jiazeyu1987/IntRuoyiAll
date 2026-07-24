package cn.iocoder.yudao.module.showroom.controller.admin.excel;

import cn.iocoder.yudao.module.showroom.controller.admin.ShowroomProductImportMode;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.award.ShowroomAwardExcelImportRow;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.product.ShowroomProductImportExtra;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFPictureData;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ShowroomAwardExcelImportExtras {

    private static final String SHEET_NAME = "奖项";
    private static final int SEQUENCE_COLUMN = 0;
    private static final int NAME_CN_COLUMN = 1;
    private static final int AWARD_DATE_COLUMN = 2;
    private static final int ISSUER_COLUMN = 3;
    private static final int COVER_IMAGE_COLUMN = 4;

    private ShowroomAwardExcelImportExtras() {
    }

    public static List<ShowroomAwardExcelImportRow> read(byte[] content) throws IOException {
        return read(content, ShowroomProductImportMode.STANDARD);
    }

    public static List<ShowroomAwardExcelImportRow> read(byte[] content,
                                                         ShowroomProductImportMode importMode) throws IOException {
        if (content == null || content.length == 0) {
            throw new IllegalStateException("SHOWROOM_AWARD_IMPORT_EXCEL_EMPTY: 奖项导入文件内容不能为空");
        }
        ShowroomProductImportMode resolvedImportMode = importMode == null
                ? ShowroomProductImportMode.STANDARD
                : importMode;
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                throw new IllegalStateException("SHOWROOM_AWARD_IMPORT_SHEET_MISSING: 奖项导入文件缺少 Sheet `奖项`");
            }
            if (!(workbook instanceof XSSFWorkbook) || !(sheet instanceof org.apache.poi.xssf.usermodel.XSSFSheet xssfSheet)) {
                throw new IllegalStateException("SHOWROOM_AWARD_IMPORT_IMAGE_UNSUPPORTED: 奖项图片导入仅支持 xlsx 文件");
            }
            DataFormatter formatter = new DataFormatter();
            Map<Integer, List<AnchoredImage>> imagesByRowNo = readAwardImages(xssfSheet);
            List<ShowroomAwardExcelImportRow> rows = new ArrayList<>();
            for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlankRow(row, formatter)) {
                    continue;
                }
                int rowNo = rowIndex + 1;
                String sequence = normalizeCellText(row.getCell(SEQUENCE_COLUMN), formatter);
                if (sequence.isEmpty()) {
                    throw new IllegalStateException("SHOWROOM_AWARD_IMPORT_REQUIRED_FIELD_MISSING: 第 " + rowNo
                            + " 行奖项序号不能为空");
                }
                String nameCn = normalizeCellText(row.getCell(NAME_CN_COLUMN), formatter);
                if (nameCn.isEmpty()) {
                    throw new IllegalStateException("SHOWROOM_AWARD_IMPORT_REQUIRED_FIELD_MISSING: 第 " + rowNo
                            + " 行奖项中文名不能为空");
                }
                List<AnchoredImage> rowImages = imagesByRowNo.getOrDefault(rowNo, List.of()).stream()
                        .filter(image -> image.column() >= COVER_IMAGE_COLUMN)
                        .sorted(Comparator.comparingInt(AnchoredImage::column)
                                .thenComparingInt(AnchoredImage::columnOffset))
                        .toList();
                boolean hasCoverImage = !rowImages.isEmpty() && rowImages.get(0).column() == COVER_IMAGE_COLUMN;
                if (!hasCoverImage && resolvedImportMode == ShowroomProductImportMode.STANDARD) {
                    throw new IllegalStateException("SHOWROOM_AWARD_IMPORT_COVER_MISSING: 第 " + rowNo
                            + " 行奖项必须在 E 列提供首图封面");
                }
                AnchoredImage cover = hasCoverImage ? rowImages.get(0) : null;
                rows.add(new ShowroomAwardExcelImportRow(rowNo, toAwardCode(sequence), nameCn,
                        normalizeCellText(row.getCell(ISSUER_COLUMN), formatter),
                        normalizeCellText(row.getCell(AWARD_DATE_COLUMN), formatter),
                        cover == null ? null : new ShowroomProductImportExtra.ImportedCoverImage(cover.content(),
                                cover.fileExtension(), cover.mimeType()),
                        hasCoverImage ? Math.max(0, rowImages.size() - 1) : rowImages.size()));
            }
            if (rows.isEmpty()) {
                throw new IllegalStateException("SHOWROOM_AWARD_IMPORT_EMPTY: 奖项页签没有可导入数据");
            }
            return List.copyOf(rows);
        }
    }

    private static Map<Integer, List<AnchoredImage>> readAwardImages(org.apache.poi.xssf.usermodel.XSSFSheet sheet) {
        XSSFDrawing drawing = sheet.getDrawingPatriarch();
        if (drawing == null) {
            return Map.of();
        }
        Map<Integer, List<AnchoredImage>> imagesByRowNo = new LinkedHashMap<>();
        for (XSSFShape shape : drawing.getShapes()) {
            if (!(shape instanceof XSSFPicture picture)) {
                continue;
            }
            XSSFClientAnchor anchor = picture.getClientAnchor();
            if (anchor == null || anchor.getRow1() < 0 || anchor.getCol1() < COVER_IMAGE_COLUMN) {
                continue;
            }
            XSSFPictureData pictureData = picture.getPictureData();
            if (pictureData == null || pictureData.getData() == null || pictureData.getData().length == 0) {
                throw new IllegalStateException("SHOWROOM_AWARD_IMPORT_IMAGE_BROKEN: 奖项图片内容为空，Excel 行号 "
                        + (anchor.getRow1() + 1));
            }
            int rowNo = anchor.getRow1() + 1;
            imagesByRowNo.computeIfAbsent(rowNo, ignored -> new ArrayList<>())
                    .add(new AnchoredImage(rowNo, anchor.getCol1(), anchor.getDx1(),
                            pictureData.getData(), normalizeExtension(pictureData.suggestFileExtension()),
                            normalizeMimeType(pictureData.getMimeType())));
        }
        return imagesByRowNo;
    }

    private static boolean isBlankRow(Row row, DataFormatter formatter) {
        return normalizeCellText(row.getCell(SEQUENCE_COLUMN), formatter).isEmpty()
                && normalizeCellText(row.getCell(NAME_CN_COLUMN), formatter).isEmpty()
                && normalizeCellText(row.getCell(AWARD_DATE_COLUMN), formatter).isEmpty()
                && normalizeCellText(row.getCell(ISSUER_COLUMN), formatter).isEmpty();
    }

    private static String toAwardCode(String sequence) {
        String normalized = sequence.trim();
        try {
            int number = Integer.parseInt(normalized.replaceAll("\\.0+$", ""));
            return "AWARD-" + String.format("%03d", number);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("SHOWROOM_AWARD_IMPORT_REQUIRED_FIELD_MISSING: 奖项序号必须是数字："
                    + sequence);
        }
    }

    private static String normalizeCellText(Cell cell, DataFormatter dataFormatter) {
        if (cell == null) {
            return "";
        }
        return dataFormatter.formatCellValue(cell)
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();
    }

    private static String normalizeExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            throw new IllegalStateException("SHOWROOM_AWARD_IMPORT_IMAGE_BROKEN: 奖项图片扩展名为空");
        }
        String normalized = extension.startsWith(".") ? extension.substring(1).toLowerCase() : extension.toLowerCase();
        if (!"png".equals(normalized) && !"jpg".equals(normalized) && !"jpeg".equals(normalized)) {
            throw new IllegalStateException("SHOWROOM_AWARD_IMPORT_IMAGE_UNSUPPORTED: 奖项图片格式不支持：" + extension);
        }
        return normalized;
    }

    private static String normalizeMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalStateException("SHOWROOM_AWARD_IMPORT_IMAGE_BROKEN: 奖项图片 MIME 类型为空");
        }
        return mimeType.trim();
    }

    private record AnchoredImage(int rowNo, int column, int columnOffset, byte[] content,
                                 String fileExtension, String mimeType) {
    }
}
