package cn.iocoder.yudao.module.dcc.registrationcertificate.service.export;

import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.query.DccRegistrationCertificatePageItem;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.time.LocalDate;
import java.util.*;

/** The supplied workbook owns all headings, widths and styles. Source IDs are version IDs. */
public class RegistrationCertificateExportWorkbook {
    public record Line(long recordId, String type, int section, String group, Object[] values, String link) {}
    public static class MappingUnavailable extends RuntimeException {
        public MappingUnavailable(String reason) { super(reason); }
    }

    public static final String CE_STRUCTURE_NOTICE = "CE未找到可验证的两段业务关系，记录按未分段列表输出，第二段仅保留模板表头";
    public static final String FDA_STRUCTURE_NOTICE = "FDA按系统每条记录的Listing字段输出，未合并或推断跨记录Listing关系";

    public Line map(DccRegistrationCertificatePageItem row) {
        var f = DccRegistrationCertificateFacts.parse(row.getFactsJson());
        // These are values persisted on this record by the importer, never values read from a sample workbook.
        var original = f.getHistoricalValues() == null ? Map.<String,String>of() : f.getHistoricalValues();
        String group = String.valueOf(row.getOwnerCompanyId());
        return switch (Objects.toString(f.getCertificateType(), "")) {
            case "DOMESTIC" -> new Line(row.getVersionId(), "DOMESTIC", 0, group, new Object[]{
                    row.getOwnerCompanyName(), row.getProjectCode(), row.getProductName(), f.getCertificateNo(),
                    date(f.getFirstObtainedDate(), original.get("首次获证日期")),
                    date(f.getEffectiveDate(), original.get("生效日期")),
                    date(f.getExpiryDate(), original.get("有效期至")), f.getClassification(), f.getRemark()}, null);
            // -1 means unsegmented. Neither historical row numbers nor free-text revisions establish a segment.
            case "CE" -> {
                var ce = "CE证书".equals(f.getHistoricalSheet()) ? original : Map.<String,String>of();
                yield new Line(row.getVersionId(), "CE", -1, group, new Object[]{
                        null, row.getOwnerCompanyName(), f.getProductNameEn(), f.getTradeName(), row.getProductName(),
                        ce.get("MDR分类"), ce.get("MDR规则"), ce.get("MDR证书编号"), date(null, ce.get("MDR签发日期")),
                        date(null, ce.get("MDR有效期至")), ce.get("EMDN代码"), ce.get("MDD分类"), ce.get("MDD证书编号"),
                        date(null, ce.get("MDD首次签发日期")), date(null, ce.get("MDD有效期至")), ce.get("延期函编号"),
                        date(null, ce.get("延期函签发日期")), date(null, ce.get("延期函有效期至")), ce.get("CE标志")}, null);
            }
            case "FDA" -> {
                var fda = "FDA证书".equals(f.getHistoricalSheet()) ? original : Map.<String,String>of();
                // A missing holder does not establish a common holder group.
                String holderGroup = f.getCertificateHolder() == null || f.getCertificateHolder().isBlank()
                        ? ":record:" + row.getVersionId() : ":holder:" + f.getCertificateHolder();
                yield new Line(row.getVersionId(), "FDA", 0, group + holderGroup, new Object[]{
                        f.getCertificateHolder(), null,
                        "EXEMPT".equals(f.getSubmissionType()) ? "510(k) Exempt" : f.getKpNumber(),
                        f.getListingNumber(), f.getProductNameEn(), row.getProductName(), f.getProductCode(),
                        f.getClassification(), f.getRegulationCode(), fdaDate(f, fda),
                        fda.get("文件编号"), fda.get("列名状态"), f.getRemark()}, validLink(f.getRegistrationLink()));
            }
            default -> throw new MappingUnavailable("注册证类型未配置导出模板");
        };
    }

    public static List<String> structureNotices(String type) {
        return switch (Objects.toString(type, "")) {
            case "CE" -> List.of(CE_STRUCTURE_NOTICE);
            case "FDA" -> List.of(FDA_STRUCTURE_NOTICE);
            default -> List.of();
        };
    }

    private String validLink(String link) {
        if (link == null || link.isBlank()) return null;
        try {
            var uri = java.net.URI.create(link);
            return Set.of("http", "https").contains(Objects.toString(uri.getScheme(), "").toLowerCase(Locale.ROOT))
                    && uri.getHost() != null ? link : null;
        } catch (IllegalArgumentException invalid) { return null; }
    }

    private Object fdaDate(DccRegistrationCertificateFacts f, Map<String,String> original) {
        if (f.getApprovalDate() != null && f.getListingDate() != null)
            return f.getApprovalDate() + " 获批\n" + f.getListingDate() + " 列名";
        if (f.getApprovalDate() != null) return f.getApprovalDate();
        if (f.getListingDate() != null) return f.getListingDate();
        return date(null, original.get("批准或列名日期"));
    }

    private Object date(LocalDate value, String original) {
        if (value != null) return value;
        if (original != null && original.matches("\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}")) {
            var parts = original.split("[-/.]");
            return LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        }
        return original;
    }

    public void write(List<Line> lines, OutputStream output, int maxRows, Runnable checkDeadline) throws IOException {
        try (var input = getClass().getResourceAsStream("/registration-certificate/export-template.xlsx")) {
            if (input == null) throw new IOException("注册证导出模板缺失");
            try (var book = new XSSFWorkbook(input)) {
                if (book.getNumberOfSheets() != 3 || !List.of("国内注册证", "CE证书", "FDA证书").equals(
                        java.util.stream.IntStream.range(0, 3).mapToObj(book::getSheetName).toList()))
                    throw new IOException("注册证导出模板结构无效");
                var domestic = lines.stream().filter(l -> l.type.equals("DOMESTIC")).toList();
                var ce1 = grouped(lines.stream().filter(l -> l.type.equals("CE") && l.section <= 0).toList());
                var ce2 = grouped(lines.stream().filter(l -> l.type.equals("CE") && l.section == 1).toList());
                var fda = grouped(lines.stream().filter(l -> l.type.equals("FDA")).toList());
                if (domestic.size() + 1 > maxRows || ce1.size() + ce2.size() + 8 > maxRows || fda.size() + 2 > maxRows)
                    throw new IOException("明细展开超出工作表行数上限，请缩小导出范围");
                fill(book, book.getSheetAt(0), 1, domestic, -1, -1, checkDeadline);
                var ce = book.getSheetAt(1);
                int shift = Math.max(1, ce1.size()) - 1;
                if (shift > 0) ce.shiftRows(4, 7, shift, true, false);
                fill(book, ce, 3, ce1, 0, 1, checkDeadline);
                fill(book, ce, 7 + shift, ce2, 0, 1, checkDeadline);
                fill(book, book.getSheetAt(2), 2, fda, 1, 0, checkDeadline);
                checkDeadline.run();
                book.write(output);
                checkDeadline.run();
            }
        }
    }

    private List<Line> grouped(List<Line> lines) {
        Map<String,List<Line>> groups = new LinkedHashMap<>();
        for (var line : lines) groups.computeIfAbsent(line.group, k -> new ArrayList<>()).add(line);
        return groups.values().stream().flatMap(Collection::stream).toList();
    }

    private void fill(Workbook book, Sheet sheet, int start, List<Line> lines, int sequenceColumn,
                      int groupColumn, Runnable checkDeadline) {
        Row example = sheet.getRow(start);
        short height = example.getHeight();
        var styles = new ArrayList<CellStyle>();
        for (int c = 0; c < example.getLastCellNum(); c++) styles.add(example.getCell(c).getCellStyle());
        Map<Short,CellStyle> dateStyles = new HashMap<>();
        int groupStart = start;
        for (int i = 0; i < lines.size(); i++) {
            checkDeadline.run();
            var line = lines.get(i);
            Row row = sheet.getRow(start + i);
            if (row == null) row = sheet.createRow(start + i);
            row.setHeight(height);
            for (int c = 0; c < styles.size(); c++) {
                Cell cell = row.createCell(c, CellType.BLANK);
                cell.setCellStyle(styles.get(c));
                Object value = c == sequenceColumn ? i + 1 : line.values[c];
                if (value instanceof LocalDate day) {
                    cell.setCellValue(day);
                    var style = styles.get(c);
                    if (!DateUtil.isADateFormat(style.getDataFormat(), style.getDataFormatString())) {
                        style = dateStyles.computeIfAbsent(style.getIndex(), key -> {
                            var copy = book.createCellStyle(); copy.cloneStyleFrom(styles.get(cIndex(styles, key)));
                            copy.setDataFormat(book.createDataFormat().getFormat("yyyy-mm-dd")); return copy;
                        });
                        cell.setCellStyle(style);
                    }
                } else if (value instanceof Number number) cell.setCellValue(number.doubleValue());
                else if (value != null) cell.setCellValue(value.toString());
            }
            if (line.link != null && !line.link.isBlank() && row.getCell(2).getCellType() == CellType.STRING
                    && !row.getCell(2).getStringCellValue().isBlank()) {
                var uri = java.net.URI.create(line.link);
                if (!Set.of("http", "https").contains(Objects.toString(uri.getScheme(), "").toLowerCase(Locale.ROOT)) || uri.getHost() == null)
                    throw new IllegalArgumentException("FDA注册证链接无效");
                var hyperlink = book.getCreationHelper().createHyperlink(HyperlinkType.URL);
                hyperlink.setAddress(line.link); row.getCell(2).setHyperlink(hyperlink);
            }
            if (groupColumn >= 0 && (i == lines.size() - 1 || !line.group.equals(lines.get(i + 1).group))) {
                if (start + i > groupStart) sheet.addMergedRegion(new CellRangeAddress(groupStart, start + i, groupColumn, groupColumn));
                groupStart = start + i + 1;
            }
        }
    }

    private int cIndex(List<CellStyle> styles, short index) {
        for (int c = 0; c < styles.size(); c++) if (styles.get(c).getIndex() == index) return c;
        throw new IllegalArgumentException("模板日期样式不存在");
    }
}
