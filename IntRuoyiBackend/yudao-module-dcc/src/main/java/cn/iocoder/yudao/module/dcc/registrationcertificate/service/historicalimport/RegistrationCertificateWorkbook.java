package cn.iocoder.yudao.module.dcc.registrationcertificate.service.historicalimport;

import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/** Reads the three certificate registers in the initialization workbook. */
public final class RegistrationCertificateWorkbook {
    public record Entry(String sheet, int row, String company, String product, DccRegistrationCertificateFacts facts) {}
    private static final List<String> DOMESTIC = List.of("公司名称","项目代码","产品名称","注册证号","首次获证日期","生效日期","有效期至","类别","备注");
    private static final List<String> CE = List.of("序号","公司名称","产品英文名称","商标","产品中文名称","MDR分类","MDR规则","MDR证书编号","MDR签发日期","MDR有效期至","EMDN代码","MDD分类","MDD证书编号","MDD首次签发日期","MDD有效期至","延期函编号","延期函签发日期","延期函有效期至","CE标志");
    private static final List<String> FDA = List.of("K号持有人","序号","K号","列名编号","产品英文名称","产品中文名称","产品代码","分类","法规监管代码","批准或列名日期","文件编号","列名状态","备注");
    private final DataFormatter formatter = new DataFormatter(Locale.ROOT);

    public List<Entry> read(InputStream input) throws IOException {
        try (var book = new XSSFWorkbook(input)) {
            var entries = new ArrayList<Entry>();
            for (String name : List.of("国内注册证","CE证书","FDA证书")) {
                var sheet = book.getSheet(name);
                if (sheet == null) throw invalidParamException("缺少工作表：" + name);
                if (sheet.getLastRowNum() > 10000 || sheet.getNumMergedRegions() > 2000)
                    throw invalidParamException(name + " 超出导入行数或合并区域限制");
                readSheet(sheet, entries);
            }
            if (entries.isEmpty()) throw invalidParamException("工作簿没有注册证记录");
            return entries;
        }
    }

    private void readSheet(Sheet sheet, List<Entry> entries) {
        String name = sheet.getSheetName();
        boolean domestic = name.equals("国内注册证");
        boolean ce = name.equals("CE证书");
        List<String> labels = domestic ? DOMESTIC : ce ? CE : FDA;
        var merged = sheet.getMergedRegions();
        String sectionCompany = null;
        boolean header = false;
        for (int n = 0; n <= sheet.getLastRowNum(); n++) {
            Row row = sheet.getRow(n);
            if (row == null) continue;
            String[] v = new String[labels.size()];
            for (int c = 0; c < v.length; c++) v[c] = value(row.getCell(c));
            if (Arrays.stream(v).allMatch(Objects::isNull)) continue;
            String first = v[0];
            if (domestic && n == 0) {
                if (!DOMESTIC.equals(Arrays.asList(v))) throw problem(sheet,n,"国内注册证表头不匹配");
                header = true;
                continue;
            }
            if (ce && "No".equalsIgnoreCase(first)) {
                if (!heading(v[1], "公司名称 Company Name")
                        || !heading(v[2], "Product Name (English)", "Product English Name")
                        || (v[3] != null && !heading(v[3], "Trademark"))
                        || !heading(v[4], "Product Name (Chinese)", "Chinese Name")
                        || !heading(v[5], "MDR Certificate") || !heading(v[11], "MDD Certificate")
                        || !heading(v[18], "CE Marker")
                        || !heading(at(sheet, n + 1, 5), "Class")
                        || !headings(sheet, n + 1, 7, "Certificate No", "Issue Date", "Expiry Date", "EMDN Code", "Class")
                        || !headings(sheet, n + 2, 12, "Certificate No", "Initial Issue Date", "Expiry Date", "Extension Letter No", "Issue Date", "Expiry Date"))
                    throw problem(sheet,n,"CE证书表头不匹配");
                n += 2;
                header = true;
                continue;
            }
            if (!domestic && !ce && "K holder".equalsIgnoreCase(first)) {
                if (!headings(sheet, n, 1, "No.", "K number", "Listing number")
                        || !heading(v[4], "Product name in EN", "Product name (English)")
                        || !heading(v[5], "Product name in CN", "Product name (Chinese)")
                        || !headings(sheet, n, 6, "Product code", "Class", "Regulation number",
                            "Cleared /exempt listing date", "File No.", "Listing status", "Remark"))
                    throw problem(sheet,n,"FDA证书表头不匹配");
                sectionCompany = null;
                header = true;
                continue;
            }
            if (!domestic && first != null && Arrays.stream(v,1,v.length).allMatch(Objects::isNull)
                    && (ce ? first.startsWith("Refer to Attached Certificates") : first.endsWith("FDA产品清单"))) continue;
            if (!header) throw problem(sheet,n,"缺少表头");
            if (!domestic) {
                for (var region : merged) {
                    if (region.getFirstRow() >= n || region.getLastRow() < n || region.getFirstColumn()!=region.getLastColumn()) continue;
                    int c = region.getFirstColumn();
                    if (c < v.length) v[c] = at(sheet,region.getFirstRow(),c);
                }
            }
            var f = new DccRegistrationCertificateFacts();
            f.setHistoricalSheet(name);
            f.setHistoricalRow(n+1);
            var original = new LinkedHashMap<String,String>();
            for (int c=0;c<v.length;c++) if (v[c]!=null) original.put(labels.get(c),v[c]);
            f.setHistoricalValues(original);
            String holder = domestic ? v[0] : ce ? v[1] : v[0];
            String company = holder == null ? null : holder.lines().findFirst().orElseThrow().strip();
            String product = domestic ? v[2] : ce ? v[4] : v[5];
            if ("客户".equals(company) && !ce && !domestic) {
                f.setCustomerHeld(true);
                company = sectionCompany;
            } else if (company != null) {
                sectionCompany = company;
            }
            if (company == null || product == null) throw problem(sheet,n,"缺少所属公司或产品中文名称");
            f.setCertificateHolder(holder);
            if (domestic) {
                f.setCertificateType("DOMESTIC");
                f.setProjectCode(v[1]);
                f.setCertificateNo(v[3]);
                f.setFirstObtainedDate(date(sheet,n,v[4]));
                f.setEffectiveDate(date(sheet,n,v[5]));
                f.setExpiryDate(date(sheet,n,v[6]));
                f.setClassification(v[7]);
                f.setRemark(v[8]);
            } else if (ce) {
                f.setCertificateType("CE");
                f.setProductNameEn(v[2]);
                f.setTradeName(v[3]);
                f.setClassification(v[5]);
                f.setNotifiedBodyCode(v[18]);
                var certificates = new ArrayList<DccRegistrationCertificateFacts.CeCertificate>();
                addCe(certificates,"MDD",v[12],date(sheet,n,v[13]));
                addCe(certificates,"MDR",v[7],date(sheet,n,v[8]));
                f.setCeCertificates(certificates);
                if (!certificates.isEmpty()) f.setCertificateNo(certificates.get(certificates.size()-1).getCertificateNo());
                f.setExpiryDate(v[7] != null ? date(sheet,n,v[9]) : date(sheet,n,v[14]));
            } else {
                f.setCertificateType("FDA");
                f.setProductNameEn(v[4]);
                f.setProductCode(v[6]);
                f.setClassification(v[7]);
                f.setListingNumber(v[3]);
                f.setRegulationCode(v[8]);
                f.setRemark(v[12]);
                if ("510(k) Exempt".equalsIgnoreCase(v[2])) f.setSubmissionType("EXEMPT");
                else f.setKpNumber(v[2]);
                // The source mixes clearance and listing dates in one column.
                readFdaDates(sheet, n, v[9], f);
            }
            entries.add(new Entry(name,n+1,company,product,f));
        }
        if (!header) throw problem(sheet,0,"缺少表头");
    }

    private static void readFdaDates(Sheet sheet, int row, String value, DccRegistrationCertificateFacts facts) {
        if (value == null) return;
        for (String line : value.lines().toList()) {
            var match = java.util.regex.Pattern.compile("^(\\d{4}[./-]\\d{1,2}[./-]\\d{1,2})\\s*(.*)$").matcher(line.strip());
            if (!match.matches()) continue; // The original mixed text remains visible in the source details.
            LocalDate parsed = date(sheet, row, match.group(1));
            String annotation = match.group(2);
            if (annotation.contains("列名") || (annotation.isEmpty() && "EXEMPT".equals(facts.getSubmissionType()))) {
                facts.setListingDate(parsed);
            } else if (annotation.contains("获批") || annotation.contains("批准")) {
                facts.setApprovalDate(parsed);
            }
        }
    }

    private static void addCe(List<DccRegistrationCertificateFacts.CeCertificate> list,String kind,String no,LocalDate issue) {
        if (no == null) return;
        var item = new DccRegistrationCertificateFacts.CeCertificate();
        item.setRevision(kind + (no.contains("Rev.") ? " " + no.substring(no.indexOf("Rev.")) : ""));
        item.setCertificateNo(no);
        item.setIssueDate(issue);
        list.add(item);
    }

    private String at(Sheet sheet,int row,int col) {
        return sheet.getRow(row)==null ? null : value(sheet.getRow(row).getCell(col));
    }

    private boolean headings(Sheet sheet, int row, int column, String... expected) {
        for (int i = 0; i < expected.length; i++) {
            if (!heading(at(sheet, row, column + i), expected[i])) return false;
        }
        return true;
    }

    private static boolean heading(String value, String... expected) {
        if (value == null) return false;
        String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKC)
                .replaceAll("\\s+", " ").replace(" )", ")").strip();
        return Arrays.stream(expected).anyMatch(normalized::equalsIgnoreCase);
    }

    private String value(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType()==CellType.FORMULA || cell.getCellType()==CellType.ERROR)
            throw problem(cell.getSheet(),cell.getRowIndex(),"请先将公式或错误单元格转成有效值：" + cell.getAddress());
        if (cell.getCellType()==CellType.NUMERIC && DateUtil.isCellDateFormatted(cell))
            return cell.getLocalDateTimeCellValue().toLocalDate().toString();
        String value = formatter.formatCellValue(cell).strip();
        return value.isEmpty() || value.equals("/") ? null : value;
    }

    private static LocalDate date(Sheet sheet,int row,String value) {
        if (value==null) return null;
        String[] p=value.split("[./-]",-1);
        try {
            if (p.length==3) return LocalDate.of(Integer.parseInt(p[0]),Integer.parseInt(p[1]),Integer.parseInt(p[2]));
        } catch (NumberFormatException | java.time.DateTimeException e) {
            throw problem(sheet,row,"日期无效：" + value);
        }
        throw problem(sheet,row,"日期无效：" + value);
    }

    private static RuntimeException problem(Sheet sheet,int row,String message) {
        return invalidParamException(sheet.getSheetName()+" 第"+(row+1)+"行："+message);
    }
}
