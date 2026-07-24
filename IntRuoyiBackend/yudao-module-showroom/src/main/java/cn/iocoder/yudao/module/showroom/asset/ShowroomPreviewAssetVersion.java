package cn.iocoder.yudao.module.showroom.asset;

import java.time.Instant;
import java.util.Objects;

public record ShowroomPreviewAssetVersion(Long id,
                                          ShowroomPreviewAssetKey key,
                                          Long sourceRevisionId,
                                          int versionNo,
                                          ShowroomPreviewAssetFiles files,
                                          ShowroomPreviewAssetStatus status,
                                          boolean generatedByAi,
                                          boolean runtimeGenerationRequested,
                                          Instant generatedAt,
                                          Instant publishedAt,
                                          boolean live) {

    public ShowroomPreviewAssetVersion {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(sourceRevisionId, "sourceRevisionId");
        Objects.requireNonNull(files, "files");
        Objects.requireNonNull(status, "status");
    }

    public ShowroomPreviewAssetVersion withStatus(ShowroomPreviewAssetStatus newStatus) {
        return new ShowroomPreviewAssetVersion(id, key, sourceRevisionId, versionNo, files, newStatus,
                generatedByAi, runtimeGenerationRequested, generatedAt, publishedAt, live);
    }

    public ShowroomPreviewAssetVersion withPublication(Instant newPublishedAt, boolean newLive) {
        return new ShowroomPreviewAssetVersion(id, key, sourceRevisionId, versionNo, files,
                ShowroomPreviewAssetStatus.PUBLISHED, generatedByAi, runtimeGenerationRequested, generatedAt,
                newPublishedAt, newLive);
    }

    public ShowroomPreviewAssetVersion withLive(boolean newLive) {
        return new ShowroomPreviewAssetVersion(id, key, sourceRevisionId, versionNo, files, status,
                generatedByAi, runtimeGenerationRequested, generatedAt, publishedAt, newLive);
    }

}
