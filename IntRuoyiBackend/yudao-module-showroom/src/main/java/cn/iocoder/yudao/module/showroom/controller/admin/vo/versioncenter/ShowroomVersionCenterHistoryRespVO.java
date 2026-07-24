package cn.iocoder.yudao.module.showroom.controller.admin.vo.versioncenter;

import java.util.List;

public record ShowroomVersionCenterHistoryRespVO(String targetType,
                                                 Long targetId,
                                                 Long currentContentRevisionId,
                                                 Long currentPublicRevisionId,
                                                 String currentReleaseId,
                                                 List<HistoryItemRespVO> items) {

    public record HistoryItemRespVO(Long revisionId,
                                    Integer revisionNo,
                                    String publishedAt,
                                    Long publishedBy,
                                    Long copiedFromRevisionId,
                                    boolean currentContent,
                                    boolean currentPublic,
                                    boolean selectable,
                                    String previewSummaryImageUrl,
                                    List<String> diffSummary,
                                    List<ShowroomVersionCenterDetailRespVO.BlockerRespVO> blockers) {
        public HistoryItemRespVO(Long revisionId, Integer revisionNo, String publishedAt, Long publishedBy,
                                 Long copiedFromRevisionId, boolean currentContent, boolean currentPublic,
                                 boolean selectable, String previewSummaryImageUrl, List<String> diffSummary) {
            this(revisionId, revisionNo, publishedAt, publishedBy, copiedFromRevisionId, currentContent,
                    currentPublic, selectable, previewSummaryImageUrl, diffSummary, List.of());
        }
    }
}
