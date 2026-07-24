package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasListRespVO;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.file.NasConnectionConfig;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_DMR_SHEET_CATEGORY_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_DMR_SHEET_EXPORT_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_DMR_SHEET_ROOT_CONFIG_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_DMR_SHEET_ROOT_UNAVAILABLE;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_READ_FAILED;

@Slf4j
@Service
public class DccDmrSheetExportServiceImpl implements DccDmrSheetExportService {

    private static final List<String> HEADERS = List.of("序号", "文件名称", "所在文件夹");
    private static final int MAX_SHEET_NAME_LENGTH = 31;

    private final DccDmrSheetExportProperties properties;
    private final NasSettingsService nasSettingsService;
    private final NasBrowserService nasBrowserService;

    public DccDmrSheetExportServiceImpl(DccDmrSheetExportProperties properties,
                                        NasSettingsService nasSettingsService,
                                        NasBrowserService nasBrowserService) {
        this.properties = properties;
        this.nasSettingsService = nasSettingsService;
        this.nasBrowserService = nasBrowserService;
    }

    @Override
    public byte[] exportWorkbook() {
        NasConnectionConfig config = nasSettingsService.getRequiredNasConfig();
        String rootPath = resolveRootPath(config);
        return nasBrowserService.executeInSession(config, scope -> exportWorkbook(scope, rootPath));
    }

    private byte[] exportWorkbook(NasBrowserService.NasSessionScope scope, String rootPath) {
        List<FileNasListRespVO.Item> categoryDirectories = listCategoryDirectories(scope, rootPath);
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Set<String> usedSheetNames = new HashSet<>();
            for (FileNasListRespVO.Item categoryDirectory : categoryDirectories) {
                writeCategorySheet(scope, workbook, usedSheetNames, rootPath, categoryDirectory);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw exception(DCC_DMR_SHEET_EXPORT_FAILED, ex.getMessage());
        }
    }

    private String resolveRootPath(NasConnectionConfig config) {
        String configuredRootPath = StrUtil.trimToNull(properties.getRootPath());
        if (configuredRootPath == null) {
            throw exception(DCC_DMR_SHEET_ROOT_CONFIG_MISSING, "yudao.dcc.dmr-sheet.root-path");
        }
        String relativePath = toNasRelativePath(config, configuredRootPath);
        if (StrUtil.isBlank(relativePath)) {
            throw exception(DCC_DMR_SHEET_ROOT_CONFIG_MISSING, "yudao.dcc.dmr-sheet.root-path");
        }
        return relativePath;
    }

    private String toNasRelativePath(NasConnectionConfig config, String configuredRootPath) {
        String rawPath = configuredRootPath.trim().replace('\\', '/');
        String rootUnc = config.rootUnc().replace('\\', '/');
        if (rawPath.equalsIgnoreCase(rootUnc)) {
            return "";
        }
        if (rawPath.regionMatches(true, 0, rootUnc + "/", 0, rootUnc.length() + 1)) {
            rawPath = rawPath.substring(rootUnc.length() + 1);
        } else if (rawPath.startsWith("//")
                || rawPath.startsWith("/")
                || rawPath.matches("^[A-Za-z]:/.*")) {
            throw exception(DCC_DMR_SHEET_ROOT_UNAVAILABLE,
                    "DMR root path must be under NAS config root: " + configuredRootPath);
        }
        return normalizeRelativePath(rawPath);
    }

    private List<FileNasListRespVO.Item> listCategoryDirectories(
            NasBrowserService.NasSessionScope scope, String rootPath) {
        List<FileNasListRespVO.Item> categoryDirectories;
        try {
            categoryDirectories = scope.listFiles(rootPath).getItems().stream()
                    .filter(item -> Boolean.TRUE.equals(item.getDir()))
                    .sorted(Comparator.comparing(item -> String.valueOf(item.getName()), String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (ServiceException ex) {
            if (DCC_DMR_SHEET_ROOT_UNAVAILABLE.getCode().equals(ex.getCode())) {
                throw ex;
            }
            throw exception(DCC_DMR_SHEET_ROOT_UNAVAILABLE, rootPath + ": " + ex.getMessage());
        }
        if (categoryDirectories.isEmpty()) {
            throw exception(DCC_DMR_SHEET_CATEGORY_MISSING, rootPath);
        }
        return categoryDirectories;
    }

    private void writeCategorySheet(NasBrowserService.NasSessionScope scope, XSSFWorkbook workbook,
                                    Set<String> usedSheetNames, String rootPath,
                                    FileNasListRespVO.Item categoryDirectory) {
        Sheet sheet = workbook.createSheet(resolveUniqueSheetName(categoryDirectory.getName(),
                usedSheetNames));
        writeHeader(sheet);
        List<DmrFileRow> rows = listFileRows(scope, rootPath, categoryDirectory);
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            DmrFileRow item = rows.get(rowIndex);
            Row row = sheet.createRow(rowIndex + 1);
            row.createCell(0).setCellValue(rowIndex + 1);
            row.createCell(1).setCellValue(item.fileName());
            row.createCell(2).setCellValue(item.folderPath());
        }
        sheet.setColumnWidth(0, 10 * 256);
        sheet.setColumnWidth(1, 48 * 256);
        sheet.setColumnWidth(2, 64 * 256);
    }

    private void writeHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        for (int columnIndex = 0; columnIndex < HEADERS.size(); columnIndex++) {
            header.createCell(columnIndex).setCellValue(HEADERS.get(columnIndex));
        }
    }

    private List<DmrFileRow> listFileRows(NasBrowserService.NasSessionScope scope, String rootPath,
                                          FileNasListRespVO.Item categoryDirectory) {
        List<DmrFileRow> rows = new ArrayList<>();
        collectFileRows(scope, rootPath, categoryDirectory.getPath(), rows);
        rows.sort(Comparator.comparing(DmrFileRow::folderPath, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(DmrFileRow::fileName, String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    private void collectFileRows(NasBrowserService.NasSessionScope scope, String rootPath,
                                 String directoryPath, List<DmrFileRow> rows) {
        List<FileNasListRespVO.Item> items;
        try {
            items = scope.listFiles(directoryPath).getItems();
        } catch (ServiceException ex) {
            if (isSkippableAccessDenied(ex)) {
                log.warn("Skip DMR-sheet NAS path because access is denied: {}", directoryPath);
                return;
            }
            if (DCC_DMR_SHEET_EXPORT_FAILED.getCode().equals(ex.getCode())) {
                throw ex;
            }
            throw exception(DCC_DMR_SHEET_EXPORT_FAILED, directoryPath + ": " + ex.getMessage());
        }
        for (FileNasListRespVO.Item item : items) {
            if (Boolean.TRUE.equals(item.getDir())) {
                collectFileRows(scope, rootPath, item.getPath(), rows);
                continue;
            }
            rows.add(new DmrFileRow(item.getName(), relativeFolderPath(rootPath, directoryPath)));
        }
    }

    private String relativeFolderPath(String rootPath, String folderPath) {
        String normalizedRoot = normalizeRelativePath(rootPath);
        String normalizedFolder = normalizeRelativePath(folderPath);
        if (normalizedFolder.equals(normalizedRoot)) {
            return "";
        }
        String prefix = normalizedRoot + "/";
        if (normalizedFolder.startsWith(prefix)) {
            return normalizedFolder.substring(prefix.length());
        }
        return normalizedFolder;
    }

    private boolean isSkippableAccessDenied(ServiceException ex) {
        if (!Objects.equals(FILE_NAS_READ_FAILED.getCode(), ex.getCode())) {
            return false;
        }
        String message = StrUtil.nullToEmpty(ex.getMessage()).toLowerCase();
        return message.contains("access denied")
                || message.contains("access_denied")
                || message.contains("status_access_denied")
                || message.contains("权限")
                || message.contains("拒绝访问");
    }

    private String normalizeRelativePath(String path) {
        String rawPath = path == null ? "" : path.replace('\\', '/').trim();
        List<String> parts = new ArrayList<>();
        for (String part : rawPath.split("/")) {
            if (part.isBlank() || ".".equals(part)) {
                continue;
            }
            if ("..".equals(part)) {
                throw exception(DCC_DMR_SHEET_ROOT_UNAVAILABLE, "DMR root path must not contain '..'");
            }
            parts.add(part);
        }
        return String.join("/", parts);
    }

    private String resolveUniqueSheetName(String directoryName, Set<String> usedSheetNames) {
        String baseName = sanitizeSheetName(stripLeadingSequence(directoryName));
        if (StrUtil.isBlank(baseName)) {
            baseName = sanitizeSheetName(directoryName);
        }
        if (StrUtil.isBlank(baseName)) {
            baseName = "DMR";
        }
        String candidate = truncateSheetName(baseName);
        int index = 2;
        while (usedSheetNames.contains(candidate)) {
            String suffix = "(" + index + ")";
            candidate = truncateSheetName(baseName, suffix.length()) + suffix;
            index++;
        }
        usedSheetNames.add(candidate);
        return candidate;
    }

    private String stripLeadingSequence(String directoryName) {
        return directoryName.replaceFirst("^[0-9０-９]+[.．、_\\-\\s]*", "");
    }

    private String sanitizeSheetName(String sheetName) {
        return StrUtil.trim(sheetName).replaceAll("[\\\\/*?:\\[\\]]", "_");
    }

    private String truncateSheetName(String sheetName) {
        return truncateSheetName(sheetName, 0);
    }

    private String truncateSheetName(String sheetName, int reservedLength) {
        int maxLength = Math.max(1, MAX_SHEET_NAME_LENGTH - reservedLength);
        return sheetName.length() <= maxLength ? sheetName : sheetName.substring(0, maxLength);
    }

    private record DmrFileRow(String fileName, String folderPath) {
    }
}
