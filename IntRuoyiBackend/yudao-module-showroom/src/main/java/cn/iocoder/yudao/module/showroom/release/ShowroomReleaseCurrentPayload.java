package cn.iocoder.yudao.module.showroom.release;

record ShowroomReleaseCurrentPayload(
        String releaseId,
        int schemaVersion,
        String manifestHash,
        String publishedAt,
        String rootDocumentId,
        int documentCount,
        int assetCount,
        long installBytes) {
}
