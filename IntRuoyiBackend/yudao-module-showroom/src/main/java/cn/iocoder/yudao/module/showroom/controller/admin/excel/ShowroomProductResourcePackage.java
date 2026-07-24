package cn.iocoder.yudao.module.showroom.controller.admin.excel;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductShowroomWorkbookRowDTO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.award.ShowroomAwardExcelExportRow;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.product.ShowroomProductExcelVO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class ShowroomProductResourcePackage {

    public static final String SCHEMA_VERSION = "showroom-product-resource-package.v1";
    public static final String MANIFEST_PATH = "manifest.json";
    public static final String WORKBOOK_PATH = "product-data.xlsx";
    public static final String PACKAGE_URL_PREFIX = "package://";
    private static final String ASSET_ROOT = "assets/narration/";
    private static final Pattern ADMIN_FILE_URL = Pattern.compile("^/admin-api/infra/file/(\\d+)/get/(.+)$");

    private ShowroomProductResourcePackage() {
    }

    public static byte[] build(String productSheetName,
                               List<ShowroomProductExcelVO> productRows,
                               List<MdmProductShowroomWorkbookRowDTO> productMasterRows,
                               List<ShowroomAwardExcelExportRow> awardRows,
                               List<ShowroomNarrationExcelRow> narrationRows,
                               List<ShowroomKeywordExcelRow> keywordRows,
                               FileService fileService) throws IOException {
        if (fileService == null) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_RESOURCE_PACKAGE_EXPORT_FAILED: file service is required");
        }
        List<ManifestNarration> manifestNarrations = new ArrayList<>();
        List<ShowroomNarrationExcelRow> packageNarrationRows = new ArrayList<>();
        Map<String, byte[]> assets = new LinkedHashMap<>();
        validateExportProductNarrationAlignment(productRows, narrationRows);
        int sequence = 1;
        for (ShowroomNarrationExcelRow row : narrationRows == null ? List.<ShowroomNarrationExcelRow>of() : narrationRows) {
            ManifestNarration manifestNarration = exportNarration(row, fileService, sequence++);
            manifestNarrations.add(manifestNarration);
            packageNarrationRows.add(new ShowroomNarrationExcelRow(
                    row.targetType(),
                    row.targetCode(),
                    row.targetName(),
                    row.language(),
                    row.scriptText(),
                    null,
                    PACKAGE_URL_PREFIX + manifestNarration.audioAssetPath(),
                    row.audioDurationSeconds(),
                    row.voice()));
            assets.put(manifestNarration.audioAssetPath(), manifestNarration.audioBytes());
        }
        byte[] workbook = ShowroomProductExcelExporter.buildWorkbook(productSheetName, productRows, productMasterRows,
                awardRows, packageNarrationRows, keywordRows, fileService);
        PackageManifest manifest = new PackageManifest(SCHEMA_VERSION, WORKBOOK_PATH,
                manifestNarrations.stream().map(ManifestNarration::withoutBytes).toList());
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            writeZipEntry(zip, MANIFEST_PATH, JsonUtils.toJsonPrettyString(manifest).getBytes(StandardCharsets.UTF_8));
            writeZipEntry(zip, WORKBOOK_PATH, workbook);
            for (Map.Entry<String, byte[]> asset : assets.entrySet()) {
                writeZipEntry(zip, asset.getKey(), asset.getValue());
            }
            zip.finish();
            return output.toByteArray();
        }
    }

    public static ParsedPackage parse(byte[] content) {
        if (content == null || content.length == 0) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_RESOURCE_PACKAGE_IMPORT_FAILED: package content is empty");
        }
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    entries.put(entry.getName(), zip.readAllBytes());
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_RESOURCE_PACKAGE_IMPORT_FAILED: failed to read zip package",
                    exception);
        }
        byte[] manifestBytes = entries.get(MANIFEST_PATH);
        if (manifestBytes == null || manifestBytes.length == 0) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_RESOURCE_PACKAGE_IMPORT_FAILED: manifest.json is missing");
        }
        PackageManifest manifest = JsonUtils.parseObject(manifestBytes, PackageManifest.class);
        if (manifest == null || !SCHEMA_VERSION.equals(manifest.schemaVersion())) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_RESOURCE_PACKAGE_IMPORT_FAILED: unsupported schemaVersion");
        }
        String workbookPath = normalizeRequired(manifest.workbookPath(),
                "SHOWROOM_PRODUCT_RESOURCE_PACKAGE_IMPORT_FAILED: workbookPath is required");
        byte[] workbook = entries.get(workbookPath);
        if (workbook == null || workbook.length == 0) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_RESOURCE_PACKAGE_IMPORT_FAILED: workbook is missing: "
                    + workbookPath);
        }
        Map<String, byte[]> assets = new LinkedHashMap<>();
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            if (!MANIFEST_PATH.equals(entry.getKey()) && !workbookPath.equals(entry.getKey())) {
                assets.put(entry.getKey(), entry.getValue());
            }
        }
        return new ParsedPackage(workbook, assets, manifest);
    }

    public static void validateImportProductNarrationAlignment(List<ShowroomProductExcelVO> productRows,
                                                               List<ShowroomNarrationExcelImportRow> narrationRows,
                                                               ParsedPackage parsedPackage) {
        Set<String> productCodes = intProductCodes(productRows);
        if (productCodes.isEmpty()) {
            return;
        }
        Map<String, Set<String>> workbookLanguagesByCode = importProductNarrationLanguages(narrationRows);
        validateProductNarrationCodesInProductList(workbookLanguagesByCode.keySet(), productCodes,
                "SHOWROOM_PRODUCT_RESOURCE_PACKAGE_NARRATION_TARGET_MISMATCH");
        Map<String, Set<String>> manifestLanguagesByCode = manifestProductNarrationLanguages(parsedPackage);
        validateProductNarrationCodesInProductList(manifestLanguagesByCode.keySet(), productCodes,
                "SHOWROOM_PRODUCT_RESOURCE_PACKAGE_NARRATION_TARGET_MISMATCH");
        requireIntProductNarrationLanguages(productCodes, workbookLanguagesByCode,
                "SHOWROOM_PRODUCT_RESOURCE_PACKAGE_NARRATION_MISSING");
        requireIntProductNarrationLanguages(productCodes, manifestLanguagesByCode,
                "SHOWROOM_PRODUCT_RESOURCE_PACKAGE_NARRATION_MISSING");
    }

    public static List<ShowroomNarrationExcelImportRow> applyPackageAudioAssets(
            List<ShowroomNarrationExcelImportRow> rows, ParsedPackage parsedPackage) {
        Map<String, ManifestNarration> manifestByKey = new LinkedHashMap<>();
        for (ManifestNarration narration : parsedPackage.manifest().narrations() == null
                ? List.<ManifestNarration>of() : parsedPackage.manifest().narrations()) {
            manifestByKey.put(narrationKey(narration.targetType(), narration.targetCode(), narration.language()),
                    narration);
        }
        List<ShowroomNarrationExcelImportRow> result = new ArrayList<>();
        for (ShowroomNarrationExcelImportRow row : rows == null ? List.<ShowroomNarrationExcelImportRow>of() : rows) {
            ManifestNarration manifest = manifestByKey.get(narrationKey(row.targetType(), row.targetCode(), row.language()));
            if (manifest == null) {
                throw new IllegalStateException("SHOWROOM_PRODUCT_RESOURCE_PACKAGE_IMPORT_FAILED: narration manifest is missing for "
                        + row.targetType() + " " + row.targetCode() + " " + row.language());
            }
            validatePackageAssetExists(parsedPackage.assets(), manifest.audioAssetPath());
            byte[] audioContent = parsedPackage.assets().get(manifest.audioAssetPath());
            result.add(new ShowroomNarrationExcelImportRow(row.rowNo(), row.targetType(), row.targetCode(),
                    row.targetName(), row.language(), row.scriptText(), null,
                    PACKAGE_URL_PREFIX + manifest.audioAssetPath(), row.audioDurationSeconds(), row.voice(),
                    audioContent));
        }
        return result;
    }

    private static void validateExportProductNarrationAlignment(List<ShowroomProductExcelVO> productRows,
                                                                List<ShowroomNarrationExcelRow> narrationRows) {
        Set<String> productCodes = intProductCodes(productRows);
        if (productCodes.isEmpty()) {
            return;
        }
        Map<String, Set<String>> languagesByCode = exportProductNarrationLanguages(narrationRows);
        validateProductNarrationCodesInProductList(languagesByCode.keySet(), productCodes,
                "SHOWROOM_PRODUCT_RESOURCE_PACKAGE_NARRATION_TARGET_MISMATCH");
        requireIntProductNarrationLanguages(productCodes, languagesByCode,
                "SHOWROOM_PRODUCT_RESOURCE_PACKAGE_NARRATION_MISSING");
    }

    private static Set<String> intProductCodes(List<ShowroomProductExcelVO> productRows) {
        Set<String> productCodes = new LinkedHashSet<>();
        for (ShowroomProductExcelVO row : productRows == null ? List.<ShowroomProductExcelVO>of() : productRows) {
            String productCode = normalizeOptional(row == null ? null : row.getProductCode());
            if (isIntProductCode(productCode)) {
                productCodes.add(productCode);
            }
        }
        return productCodes;
    }

    private static Map<String, Set<String>> exportProductNarrationLanguages(List<ShowroomNarrationExcelRow> narrationRows) {
        Map<String, Set<String>> languagesByCode = new LinkedHashMap<>();
        for (ShowroomNarrationExcelRow row : narrationRows == null ? List.<ShowroomNarrationExcelRow>of() : narrationRows) {
            String targetType = normalizeOptional(row == null ? null : row.targetType());
            if (!"PRODUCT".equalsIgnoreCase(targetType)) {
                continue;
            }
            String targetCode = normalizeOptional(row.targetCode());
            String language = normalizeLanguage(row.language());
            if (!targetCode.isEmpty() && !language.isEmpty()) {
                languagesByCode.computeIfAbsent(targetCode, key -> new LinkedHashSet<>()).add(language);
            }
        }
        return languagesByCode;
    }

    private static Map<String, Set<String>> importProductNarrationLanguages(List<ShowroomNarrationExcelImportRow> narrationRows) {
        Map<String, Set<String>> languagesByCode = new LinkedHashMap<>();
        for (ShowroomNarrationExcelImportRow row : narrationRows == null ? List.<ShowroomNarrationExcelImportRow>of() : narrationRows) {
            String targetType = normalizeOptional(row == null ? null : row.targetType());
            if (!"PRODUCT".equalsIgnoreCase(targetType)) {
                continue;
            }
            String targetCode = normalizeOptional(row.targetCode());
            String language = normalizeLanguage(row.language());
            if (!targetCode.isEmpty() && !language.isEmpty()) {
                languagesByCode.computeIfAbsent(targetCode, key -> new LinkedHashSet<>()).add(language);
            }
        }
        return languagesByCode;
    }

    private static Map<String, Set<String>> manifestProductNarrationLanguages(ParsedPackage parsedPackage) {
        Map<String, Set<String>> languagesByCode = new LinkedHashMap<>();
        List<ManifestNarration> narrations = parsedPackage == null || parsedPackage.manifest() == null
                || parsedPackage.manifest().narrations() == null
                ? List.of()
                : parsedPackage.manifest().narrations();
        for (ManifestNarration narration : narrations) {
            String targetType = normalizeOptional(narration == null ? null : narration.targetType());
            if (!"PRODUCT".equalsIgnoreCase(targetType)) {
                continue;
            }
            String targetCode = normalizeOptional(narration.targetCode());
            String language = normalizeLanguage(narration.language());
            if (!targetCode.isEmpty() && !language.isEmpty()) {
                languagesByCode.computeIfAbsent(targetCode, key -> new LinkedHashSet<>()).add(language);
            }
        }
        return languagesByCode;
    }

    private static void validateProductNarrationCodesInProductList(Set<String> narrationCodes,
                                                                   Set<String> productCodes,
                                                                   String errorCode) {
        List<String> mismatchedCodes = narrationCodes.stream()
                .filter(code -> !productCodes.contains(code))
                .toList();
        if (!mismatchedCodes.isEmpty()) {
            throw new IllegalStateException(errorCode + ": PRODUCT narration targetCode not in product list, actual="
                    + String.join(",", mismatchedCodes) + ", expectedOneOf=" + String.join(",", productCodes));
        }
    }

    private static void requireIntProductNarrationLanguages(Set<String> productCodes,
                                                           Map<String, Set<String>> languagesByCode,
                                                           String errorCode) {
        List<String> missing = new ArrayList<>();
        for (String productCode : productCodes) {
            Set<String> languages = languagesByCode.getOrDefault(productCode, Set.of());
            List<String> missingLanguages = new ArrayList<>();
            if (!languages.contains("ZH")) {
                missingLanguages.add("ZH");
            }
            if (!languages.contains("EN")) {
                missingLanguages.add("EN");
            }
            if (!missingLanguages.isEmpty()) {
                missing.add(productCode + "[" + String.join("/", missingLanguages) + "]");
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException(errorCode + ": INT product narration incomplete, missing="
                    + String.join(",", missing));
        }
    }

    public static boolean isPackageUrl(String audioUrl) {
        return audioUrl != null && audioUrl.trim().startsWith(PACKAGE_URL_PREFIX);
    }

    public static String packageAssetPath(String audioUrl) {
        String normalized = normalizeRequired(audioUrl,
                "SHOWROOM_PRODUCT_RESOURCE_PACKAGE_IMPORT_FAILED: package audio url is required");
        if (!normalized.startsWith(PACKAGE_URL_PREFIX)) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_RESOURCE_PACKAGE_IMPORT_FAILED: unsupported package audio url "
                    + audioUrl);
        }
        return normalized.substring(PACKAGE_URL_PREFIX.length());
    }

    private static ManifestNarration exportNarration(ShowroomNarrationExcelRow row, FileService fileService,
                                                     int sequence) {
        if (row == null) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_RESOURCE_PACKAGE_EXPORT_FAILED: narration row is null");
        }
        String targetType = normalizeRequired(row.targetType(),
                "SHOWROOM_PRODUCT_RESOURCE_PACKAGE_EXPORT_FAILED: narration targetType is required");
        String targetCode = normalizeRequired(row.targetCode(),
                "SHOWROOM_PRODUCT_RESOURCE_PACKAGE_EXPORT_FAILED: narration targetCode is required");
        String language = normalizeRequired(row.language(),
                "SHOWROOM_PRODUCT_RESOURCE_PACKAGE_EXPORT_FAILED: narration language is required");
        String audioUrl = normalizeRequired(row.audioUrl(),
                "SHOWROOM_PRODUCT_RESOURCE_PACKAGE_EXPORT_FAILED: narration audioUrl is required for "
                        + targetType + " " + targetCode + " " + language);
        Matcher matcher = ADMIN_FILE_URL.matcher(audioUrl);
        if (!matcher.matches()) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_RESOURCE_PACKAGE_EXPORT_FAILED: narration audioUrl must be /admin-api/infra/file/... for "
                    + targetType + " " + targetCode + " " + language);
        }
        Long configId = Long.valueOf(matcher.group(1));
        String path = URLDecoder.decode(matcher.group(2), StandardCharsets.UTF_8);
        byte[] bytes;
        try {
            bytes = fileService.getFileContent(configId, path);
        } catch (Exception exception) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_RESOURCE_PACKAGE_EXPORT_FAILED: narration audio read failed for "
                    + targetType + " " + targetCode + " " + language + " url=" + audioUrl, exception);
        }
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_RESOURCE_PACKAGE_EXPORT_FAILED: narration audio is empty for "
                    + targetType + " " + targetCode + " " + language);
        }
        String fileName = fileNameOf(path);
        String extension = extensionOf(fileName);
        if (extension.isEmpty()) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_RESOURCE_PACKAGE_EXPORT_FAILED: narration audio extension is missing for "
                    + targetType + " " + targetCode + " " + language);
        }
        String assetPath = ASSET_ROOT + sanitizePathPart(targetType) + "/" + sanitizePathPart(targetCode) + "/"
                + String.format(Locale.ROOT, "%03d", sequence) + "-" + sanitizePathPart(language) + "." + extension;
        return new ManifestNarration(targetType, targetCode, row.targetName(), language, row.scriptText(),
                row.audioDurationSeconds(), row.voice(), assetPath, bytes);
    }

    private static void validatePackageAssetExists(Map<String, byte[]> assets, String assetPath) {
        String normalized = normalizeRequired(assetPath,
                "SHOWROOM_PRODUCT_RESOURCE_PACKAGE_IMPORT_FAILED: narration audioAssetPath is required");
        byte[] bytes = assets.get(normalized);
        if (bytes == null) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_RESOURCE_PACKAGE_ASSET_MISSING: " + normalized);
        }
        if (bytes.length == 0) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_RESOURCE_PACKAGE_ASSET_EMPTY: " + normalized);
        }
    }

    private static String narrationKey(String targetType, String targetCode, String language) {
        return normalizeKeyPart(targetType) + "|" + normalizeKeyPart(targetCode) + "|" + normalizeKeyPart(language);
    }

    private static String normalizeKeyPart(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeLanguage(String value) {
        return normalizeOptional(value).toUpperCase(Locale.ROOT);
    }

    private static boolean isIntProductCode(String value) {
        return normalizeOptional(value).toUpperCase(Locale.ROOT).startsWith("INT-");
    }

    private static String normalizeRequired(String value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalStateException(message);
        }
        return normalized;
    }

    private static String sanitizePathPart(String value) {
        String normalized = normalizeRequired(value,
                "SHOWROOM_PRODUCT_RESOURCE_PACKAGE_EXPORT_FAILED: path value is required")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("^-+|-+$", "");
        return normalized.isEmpty() ? "item" : normalized;
    }

    private static String fileNameOf(String path) {
        String normalized = path == null ? "" : path.trim();
        int separatorIndex = normalized.lastIndexOf('/');
        return separatorIndex >= 0 ? normalized.substring(separatorIndex + 1) : normalized;
    }

    private static String extensionOf(String fileName) {
        String normalized = fileName == null ? "" : fileName.trim();
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == normalized.length() - 1) {
            return "";
        }
        return normalized.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private static void writeZipEntry(ZipOutputStream zip, String path, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(bytes);
        zip.closeEntry();
    }

    public record ParsedPackage(byte[] workbookBytes, Map<String, byte[]> assets, PackageManifest manifest) {
    }

    public record PackageManifest(String schemaVersion,
                                  String workbookPath,
                                  List<ManifestNarration> narrations) {
    }

    public record ManifestNarration(String targetType,
                                    String targetCode,
                                    String targetName,
                                    String language,
                                    String scriptText,
                                    Integer audioDurationSeconds,
                                    String voice,
                                    String audioAssetPath,
                                    byte[] audioBytes) {
        ManifestNarration withoutBytes() {
            return new ManifestNarration(targetType, targetCode, targetName, language, scriptText,
                    audioDurationSeconds, voice, audioAssetPath, null);
        }
    }
}
