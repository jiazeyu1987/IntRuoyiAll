package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

public final class DccControlledFileUploadTypePolicy {

    public static final String PURPOSE_SOURCE = "SOURCE";
    public static final String PURPOSE_DRAWING_PDF = "DRAWING_PDF";
    public static final String PURPOSE_TRAINING_RECORD = "TRAINING_RECORD";
    public static final String PURPOSE_EXTERNAL_REVIEW_OUTPUT = "EXTERNAL_REVIEW_OUTPUT";

    private static final Set<String> EDITABLE_SOURCE_EXTENSIONS = Set.of(
            "doc", "docx", "xls", "xlsx", "dwg", "sldprt", "sldasm", "slddrw");
    private static final Set<String> DRAWING_SOURCE_EXTENSIONS = Set.of("dwg", "sldprt", "sldasm", "slddrw");
    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private DccControlledFileUploadTypePolicy() {
    }

    public static String normalizePurpose(String purpose) {
        return StrUtil.trimToEmpty(purpose).toUpperCase(Locale.ROOT);
    }

    public static boolean isSupportedPurpose(String purpose) {
        String normalized = normalizePurpose(purpose);
        return PURPOSE_SOURCE.equals(normalized)
                || PURPOSE_DRAWING_PDF.equals(normalized)
                || PURPOSE_TRAINING_RECORD.equals(normalized)
                || PURPOSE_EXTERNAL_REVIEW_OUTPUT.equals(normalized);
    }

    public static boolean isSourcePurpose(String purpose) {
        return PURPOSE_SOURCE.equals(normalizePurpose(purpose));
    }

    public static boolean isDrawingPdfPurpose(String purpose) {
        return PURPOSE_DRAWING_PDF.equals(normalizePurpose(purpose));
    }

    public static boolean isAllowedEditableSourceName(String fileName) {
        String extension = extensionOf(fileName);
        return StrUtil.isNotBlank(extension) && EDITABLE_SOURCE_EXTENSIONS.contains(extension);
    }

    public static boolean isDrawingSourceName(String fileName) {
        String extension = extensionOf(fileName);
        return StrUtil.isNotBlank(extension) && DRAWING_SOURCE_EXTENSIONS.contains(extension);
    }

    public static boolean isRealPdfFile(String fileName, byte[] content) {
        return "pdf".equals(extensionOf(fileName)) && hasPdfSignature(content);
    }

    public static boolean hasPdfSignature(byte[] content) {
        if (content == null || content.length < PDF_SIGNATURE.length) {
            return false;
        }
        for (int i = 0; i < PDF_SIGNATURE.length; i++) {
            if (content[i] != PDF_SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }

    public static String allowedEditableSourceExtensionsText() {
        return "doc、docx、xls、xlsx、dwg、sldprt、sldasm、slddrw";
    }

    private static String extensionOf(String fileName) {
        String normalized = StrUtil.trimToEmpty(fileName);
        int index = normalized.lastIndexOf('.');
        if (index < 0 || index == normalized.length() - 1) {
            return "";
        }
        return normalized.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
