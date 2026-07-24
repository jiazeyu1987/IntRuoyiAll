package cn.iocoder.yudao.module.showroom.release;

public record ResolvedBinarySource(
        String assetId,
        String assetType,
        String mimeType,
        byte[] bytes,
        String sourceKey,
        Long previewAssetVersionId,
        Long narrationVersionId) {
}
