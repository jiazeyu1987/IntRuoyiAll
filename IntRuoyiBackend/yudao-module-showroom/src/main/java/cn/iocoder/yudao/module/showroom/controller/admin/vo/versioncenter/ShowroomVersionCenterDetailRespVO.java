package cn.iocoder.yudao.module.showroom.controller.admin.vo.versioncenter;

import java.util.List;

public record ShowroomVersionCenterDetailRespVO(TargetSummaryRespVO targetSummary,
                                                SnapshotRespVO selectedVersion,
                                                SnapshotRespVO currentContentVersion,
                                                SnapshotRespVO currentPublicVersion,
                                                ReleaseSummaryRespVO currentRelease,
                                                List<FieldDiffRespVO> fieldDiffs,
                                                PermissionRespVO permissions,
                                                RepublishReadinessRespVO republishReadiness) {

    public record TargetSummaryRespVO(String targetType,
                                      Long targetId,
                                      String title,
                                      String titleEn,
                                      Long currentContentRevisionId,
                                      Long currentPublicRevisionId) {
    }

    public record SnapshotRespVO(Long revisionId,
                                 Integer revisionNo,
                                 String publishedAt,
                                 Long publishedBy,
                                 Long copiedFromRevisionId,
                                 boolean currentContent,
                                 boolean currentPublic,
                                 String title,
                                 String titleEn,
                                 String companyType,
                                 List<FieldValueRespVO> fields,
                                 ImageRespVO image,
                                 List<NarrationRespVO> narrations) {
    }

    public record FieldValueRespVO(String fieldCode,
                                   String label,
                                   String labelEn,
                                   Integer order,
                                   String valueZh,
                                   String valueEn) {
    }

    public record ImageRespVO(ContentImageRespVO contentImage,
                              ReleasePreviewAssetRespVO releasePreviewAsset) {
    }

    public record ContentImageRespVO(String source,
                                     String url,
                                     String alt,
                                     Long versionId,
                                     Long fileId,
                                     Long sourceRevisionId) {
    }

    public record ReleasePreviewAssetRespVO(String source,
                                            String url,
                                            String alt,
                                            Long versionId,
                                            Long fileId,
                                            Long sourceRevisionId) {
    }

    public record NarrationRespVO(String language,
                                  Long versionId,
                                  String scriptText,
                                  String audioUrl,
                                  Integer duration,
                                  String voice) {
    }

    public record FieldDiffRespVO(String fieldCode,
                                  String label,
                                  String labelEn,
                                  Integer order,
                                  String selectedValueZh,
                                  String selectedValueEn,
                                  String currentContentValueZh,
                                  String currentContentValueEn,
                                  boolean changed) {
    }

    public record ReleaseSummaryRespVO(String releaseId,
                                       String manifestHash,
                                       String publishedAt,
                                       Long companyRevisionId,
                                       Boolean productInCurrentRelease,
                                       Long productCurrentReleaseRevisionId) {
    }

    public record PermissionRespVO(boolean canRepublish,
                                   String republishDisabledReason) {
    }

    public record RepublishReadinessRespVO(boolean ready,
                                           List<BlockerRespVO> blockers) {
    }

    public record BlockerRespVO(String blockerCode,
                                String message,
                                List<Long> affectedRevisionIds,
                                String scope,
                                String targetType,
                                Long targetId,
                                String language,
                                List<String> missingFields,
                                Long fileId,
                                String assetId,
                                String contentHash,
                                String backendErrorCode) {
        public BlockerRespVO(String blockerCode, String message, List<Long> affectedRevisionIds, String scope) {
            this(blockerCode, message, affectedRevisionIds, scope, null, null, null, List.of(), null, null, null,
                    blockerCode);
        }
    }
}
