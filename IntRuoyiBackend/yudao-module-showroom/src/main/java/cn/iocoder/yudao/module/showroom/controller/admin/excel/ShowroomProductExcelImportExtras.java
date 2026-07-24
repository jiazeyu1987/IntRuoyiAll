package cn.iocoder.yudao.module.showroom.controller.admin.excel;

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
import java.util.HashSet;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ShowroomProductExcelImportExtras {

    private static final String SHEET_NAME = "产品列表";
    private static final List<String> REQUIRED_HEADERS = List.of(
            "展品编码", "产品名-中文", "产品名-英文", "展柜名称", "持证公司", "在售/在研", "BU",
            "在售国家", "适应症", "型号规格", "注册证信息", "卖点文案", "产品图", "奖项", "原材料表单");
    private static final String AUTHORITY_PRODUCT_NAME_HEADER = "产品名-中文";
    private static final String OLD_PRODUCT_NAME_HEADER = "产品-中文";
    private static final String LEGACY_PRODUCT_HEADER = "产品";
    private static final String SELLING_POINTS_COPY_HEADER = "卖点文案";
    private static final String PRODUCT_IMAGE_HEADER = "产品图";

    private ShowroomProductExcelImportExtras() {
    }

    public static Map<Integer, ShowroomProductImportExtra> read(byte[] content) throws IOException {
        if (content == null || content.length == 0) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_IMPORT_EXCEL_EMPTY: 产品导入文件内容不能为空");
        }
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                throw new IllegalStateException("SHOWROOM_PRODUCT_IMPORT_HEADER_INVALID: 产品导入文件缺少 Sheet `产品列表`");
            }
            DataFormatter dataFormatter = new DataFormatter();
            HeaderColumns columns = resolveHeaderColumns(sheet, dataFormatter);

            Map<Integer, ShowroomProductImportExtra> extrasByRowNo = new LinkedHashMap<>();
            readProductNames(sheet, columns.productNameColumn(), dataFormatter, extrasByRowNo);
            readSellingPointsCopy(sheet, columns.sellingPointsCopyColumn(), dataFormatter, extrasByRowNo);
            readProductImages(workbook, sheet, columns.productImageColumn(), extrasByRowNo);
            return Map.copyOf(extrasByRowNo);
        }
    }

    private static void readProductNames(Sheet sheet, Integer productNameColumn,
                                         DataFormatter dataFormatter,
                                         Map<Integer, ShowroomProductImportExtra> extrasByRowNo) {
        if (productNameColumn == null) {
            return;
        }
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            String productName = normalizeCellText(row.getCell(productNameColumn), dataFormatter);
            if (productName.isEmpty()) {
                continue;
            }
            int rowNo = rowIndex + 1;
            ShowroomProductImportExtra current = extrasByRowNo.get(rowNo);
            extrasByRowNo.put(rowNo, new ShowroomProductImportExtra(productName,
                    current == null ? null : current.sellingPointsCopy(),
                    current == null ? null : current.coverImage()));
        }
    }

    private static void readSellingPointsCopy(Sheet sheet, Integer sellingPointsCopyColumn,
                                              DataFormatter dataFormatter,
                                              Map<Integer, ShowroomProductImportExtra> extrasByRowNo) {
        if (sellingPointsCopyColumn == null) {
            return;
        }
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            String sellingPointsCopy = normalizeCellText(row.getCell(sellingPointsCopyColumn), dataFormatter);
            if (sellingPointsCopy.isEmpty()) {
                continue;
            }
            int rowNo = rowIndex + 1;
            ShowroomProductImportExtra current = extrasByRowNo.get(rowNo);
            extrasByRowNo.put(rowNo, new ShowroomProductImportExtra(
                    current == null ? null : current.productName(),
                    sellingPointsCopy,
                    current == null ? null : current.coverImage()));
        }
    }

    private static void readProductImages(Workbook workbook, Sheet sheet, Integer productImageColumn,
                                          Map<Integer, ShowroomProductImportExtra> extrasByRowNo) {
        if (productImageColumn == null) {
            return;
        }
        if (!(workbook instanceof XSSFWorkbook) || !(sheet instanceof org.apache.poi.xssf.usermodel.XSSFSheet xssfSheet)) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_IMPORT_IMAGE_UNSUPPORTED: 产品图导入仅支持 xlsx 文件");
        }
        XSSFDrawing drawing = xssfSheet.getDrawingPatriarch();
        if (drawing == null) {
            return;
        }
        List<AnchoredImage> images = new ArrayList<>();
        for (XSSFShape shape : drawing.getShapes()) {
            if (!(shape instanceof XSSFPicture picture)) {
                continue;
            }
            XSSFClientAnchor anchor = picture.getClientAnchor();
            if (anchor == null || anchor.getRow1() <= 0 || anchor.getCol1() != productImageColumn) {
                continue;
            }
            XSSFPictureData pictureData = picture.getPictureData();
            if (pictureData == null || pictureData.getData() == null || pictureData.getData().length == 0) {
                throw new IllegalStateException("SHOWROOM_PRODUCT_IMPORT_IMAGE_BROKEN: 产品图图片内容为空，Excel 行号 "
                        + (anchor.getRow1() + 1));
            }
            images.add(new AnchoredImage(anchor.getRow1() + 1, anchor.getCol1(), anchor.getDx1(),
                    pictureData.getData(), normalizeExtension(pictureData.suggestFileExtension()),
                    normalizeMimeType(pictureData.getMimeType())));
        }

        images.stream()
                .sorted(Comparator.comparingInt(AnchoredImage::rowNo)
                        .thenComparingInt(AnchoredImage::column)
                        .thenComparingInt(AnchoredImage::columnOffset))
                .forEach(image -> extrasByRowNo.compute(image.rowNo(), (rowNo, current) -> {
                    if (current != null && current.hasCoverImage()) {
                        return current;
                    }
                    return new ShowroomProductImportExtra(
                            current == null ? null : current.productName(),
                            current == null ? null : current.sellingPointsCopy(),
                            new ShowroomProductImportExtra.ImportedCoverImage(image.content(),
                                    image.fileExtension(), image.mimeType()));
                }));
    }

    private static HeaderColumns resolveHeaderColumns(Sheet sheet, DataFormatter dataFormatter) {
        Row row = sheet.getRow(0);
        if (row == null) {
            return new HeaderColumns(null, null, null);
        }
        Integer productNameColumn = null;
        Integer sellingPointsCopyColumn = null;
        Integer productImageColumn = null;
        Set<String> seenHeaders = new HashSet<>();
        Set<String> duplicateHeaders = new HashSet<>();
        Set<String> actualHeaders = new HashSet<>();
        for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
            String header = normalizeCellText(row.getCell(cellIndex), dataFormatter);
            if (header.isEmpty()) {
                continue;
            }
            actualHeaders.add(header);
            if (!seenHeaders.add(header)) {
                duplicateHeaders.add(header);
            }
            if (LEGACY_PRODUCT_HEADER.equals(header)) {
                productNameColumn = cellIndex;
            } else if (SELLING_POINTS_COPY_HEADER.equals(header)) {
                sellingPointsCopyColumn = cellIndex;
            } else if (PRODUCT_IMAGE_HEADER.equals(header)) {
                productImageColumn = cellIndex;
            }
        }
        validateHeaders(actualHeaders, duplicateHeaders);
        return new HeaderColumns(productNameColumn, sellingPointsCopyColumn, productImageColumn);
    }

    private static void validateHeaders(Set<String> actualHeaders, Set<String> duplicateHeaders) {
        if (!duplicateHeaders.isEmpty()) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_IMPORT_HEADER_INVALID: 产品导入表头重复："
                    + String.join("、", duplicateHeaders));
        }
        if (actualHeaders.contains(OLD_PRODUCT_NAME_HEADER)) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_IMPORT_HEADER_INVALID: 中文名权威列必须使用 `"
                    + AUTHORITY_PRODUCT_NAME_HEADER + "`，不能继续使用 `" + OLD_PRODUCT_NAME_HEADER + "`");
        }
        List<String> missingHeaders = REQUIRED_HEADERS.stream()
                .filter(header -> !actualHeaders.contains(header))
                .toList();
        if (!missingHeaders.isEmpty()) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_IMPORT_HEADER_INVALID: 产品导入文件缺少表头："
                    + String.join("、", missingHeaders));
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
            throw new IllegalStateException("SHOWROOM_PRODUCT_IMPORT_IMAGE_BROKEN: 产品图图片扩展名为空");
        }
        String normalized = extension.startsWith(".") ? extension.substring(1).toLowerCase() : extension.toLowerCase();
        if (!"png".equals(normalized) && !"jpg".equals(normalized) && !"jpeg".equals(normalized)) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_IMPORT_IMAGE_UNSUPPORTED: 产品图图片格式不支持：" + extension);
        }
        return normalized;
    }

    private static String normalizeMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_IMPORT_IMAGE_BROKEN: 产品图图片 MIME 类型为空");
        }
        return mimeType.trim();
    }

    private record HeaderColumns(Integer productNameColumn, Integer sellingPointsCopyColumn,
                                 Integer productImageColumn) {
    }

    private record AnchoredImage(int rowNo, int column, int columnOffset, byte[] content,
                                 String fileExtension, String mimeType) {
    }
}
