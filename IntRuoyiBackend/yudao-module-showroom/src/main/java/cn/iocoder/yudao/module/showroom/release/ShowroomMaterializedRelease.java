package cn.iocoder.yudao.module.showroom.release;

import java.time.Instant;
import java.util.List;

public record ShowroomMaterializedRelease(
        String releaseId,
        Instant publishedAt,
        String manifestHash,
        String manifestJson,
        String rootDocumentId,
        int documentCount,
        int assetCount,
        long installBytes,
        ShowroomReleaseSourceSnapshot sourceSnapshot,
        List<MaterializedDocument> documents,
        List<MaterializedAsset> assets,
        List<MaterializedAssetRef> assetRefs,
        String legacyProjectionJson,
        String legacyProjectionHash) {

    record MaterializedDocument(String documentId, String kind, Long productId, String contentHash,
                                long bytes, Instant materializedAt, String payloadJson) {
    }

    record MaterializedAsset(String assetId, String assetType, String contentHash, String mimeType,
                             long bytes, String storageKey, Instant materializedAt, byte[] binaryContent) {
    }

    record MaterializedAssetRef(String documentId, String assetId, String contentHash, String usageCode) {
    }
}
