package cn.iocoder.yudao.module.showroom.content.model;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ShowroomProductAttachmentPolicy {

    public static final String ASSET_TYPE_IMAGE = "image";
    public static final String ASSET_TYPE_VIDEO = "video";
    public static final String ASSET_TYPE_TEXT = "text";
    public static final int MAX_ATTACHMENTS_PER_PRODUCT = 20;
    public static final long MAX_VIDEO_BYTES = 500L * 1024L * 1024L;
    public static final long MAX_NON_VIDEO_BYTES = 50L * 1024L * 1024L;

    private static final Map<String, Set<String>> EXTENSIONS_BY_TYPE = Map.of(
            ASSET_TYPE_IMAGE, Set.of("jpg", "jpeg", "png", "webp", "gif"),
            ASSET_TYPE_VIDEO, Set.of("mp4", "webm", "mov", "avi", "m4v"),
            ASSET_TYPE_TEXT, Set.of("txt", "md", "pdf", "doc", "docx")
    );

    private ShowroomProductAttachmentPolicy() {
    }

    public static List<ShowroomProductAttachment> normalizedCopy(List<ShowroomProductAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        if (attachments.size() > MAX_ATTACHMENTS_PER_PRODUCT) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_ATTACHMENT_LIMIT_EXCEEDED: product attachments must not exceed "
                    + MAX_ATTACHMENTS_PER_PRODUCT);
        }
        return attachments.stream()
                .map(ShowroomProductAttachmentPolicy::normalizeAttachment)
                .sorted(Comparator.comparingInt(ShowroomProductAttachment::displayOrder)
                        .thenComparing(ShowroomProductAttachment::originalName))
                .toList();
    }

    public static String validateUpload(String assetType, String originalName, String mimeType, long fileSize) {
        String normalizedAssetType = normalizeAssetType(assetType);
        validateFileShape(normalizedAssetType, originalName, mimeType, fileSize);
        return normalizedAssetType;
    }

    private static ShowroomProductAttachment normalizeAttachment(ShowroomProductAttachment attachment) {
        if (attachment == null) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_ATTACHMENT_INVALID: attachment is required");
        }
        String assetType = normalizeAssetType(attachment.assetType());
        if (attachment.fileId() == null) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_ATTACHMENT_INVALID: attachment fileId is required");
        }
        String originalName = requireText(attachment.originalName(), "originalName");
        String mimeType = requireText(attachment.mimeType(), "mimeType");
        Long fileSize = attachment.fileSize();
        if (fileSize == null) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_ATTACHMENT_INVALID: attachment fileSize is required");
        }
        validateFileShape(assetType, originalName, mimeType, fileSize);
        return new ShowroomProductAttachment(attachment.id(), attachment.productId(), attachment.productRevisionId(),
                assetType, attachment.fileId(), originalName, mimeType, fileSize, attachment.displayOrder());
    }

    private static String normalizeAssetType(String assetType) {
        String normalized = requireText(assetType, "assetType").toLowerCase(Locale.ROOT);
        if (!EXTENSIONS_BY_TYPE.containsKey(normalized)) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_ATTACHMENT_INVALID: unsupported attachment assetType "
                    + assetType);
        }
        return normalized;
    }

    private static void validateFileShape(String assetType, String originalName, String mimeType, long fileSize) {
        requireText(originalName, "originalName");
        requireText(mimeType, "mimeType");
        if (fileSize <= 0) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_ATTACHMENT_INVALID: attachment file is empty");
        }
        long maxBytes = ASSET_TYPE_VIDEO.equals(assetType) ? MAX_VIDEO_BYTES : MAX_NON_VIDEO_BYTES;
        if (fileSize > maxBytes) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_ATTACHMENT_SIZE_EXCEEDED: " + assetType
                    + " attachment exceeds " + maxBytes + " bytes");
        }
        String extension = extensionOf(originalName);
        if (!EXTENSIONS_BY_TYPE.get(assetType).contains(extension)) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_ATTACHMENT_INVALID: unsupported " + assetType
                    + " attachment extension " + extension);
        }
    }

    private static String extensionOf(String originalName) {
        int lastDot = originalName.lastIndexOf('.');
        if (lastDot < 0 || lastDot == originalName.length() - 1) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_ATTACHMENT_INVALID: attachment extension is required");
        }
        return originalName.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_ATTACHMENT_INVALID: attachment " + field
                    + " is required");
        }
        return value.trim();
    }
}
