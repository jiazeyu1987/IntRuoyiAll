package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.module.showroom.controller.admin.vo.versioncenter.ShowroomVersionCenterDetailRespVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.versioncenter.ShowroomVersionCenterHistoryRespVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.versioncenter.ShowroomVersionCenterRepublishRespVO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShowroomVersionCenterAssembler {

    public ShowroomVersionCenterHistoryRespVO history(String targetType, Long targetId, Long currentContentRevisionId,
                                                      Long currentPublicRevisionId, String currentReleaseId,
                                                      List<ShowroomVersionCenterHistoryRespVO.HistoryItemRespVO> items) {
        return new ShowroomVersionCenterHistoryRespVO(targetType, targetId, currentContentRevisionId,
                currentPublicRevisionId, currentReleaseId, List.copyOf(items));
    }

    public ShowroomVersionCenterDetailRespVO detail(ShowroomVersionCenterDetailRespVO.TargetSummaryRespVO targetSummary,
                                                    ShowroomVersionCenterDetailRespVO.SnapshotRespVO selectedVersion,
                                                    ShowroomVersionCenterDetailRespVO.SnapshotRespVO currentContentVersion,
                                                    ShowroomVersionCenterDetailRespVO.SnapshotRespVO currentPublicVersion,
                                                    ShowroomVersionCenterDetailRespVO.ReleaseSummaryRespVO currentRelease,
                                                    List<ShowroomVersionCenterDetailRespVO.FieldDiffRespVO> fieldDiffs,
                                                    ShowroomVersionCenterDetailRespVO.PermissionRespVO permissions,
                                                    ShowroomVersionCenterDetailRespVO.RepublishReadinessRespVO readiness) {
        return new ShowroomVersionCenterDetailRespVO(targetSummary, selectedVersion, currentContentVersion,
                currentPublicVersion, currentRelease, List.copyOf(fieldDiffs), permissions, readiness);
    }

    public ShowroomVersionCenterDetailRespVO.BlockerRespVO blocker(ShowroomVersionBundleService.ShowroomVersionBlocker blocker) {
        return new ShowroomVersionCenterDetailRespVO.BlockerRespVO(blocker.blockerCode(), blocker.message(),
                blocker.affectedRevisionIds(), blocker.scope(), blocker.targetType(), blocker.targetId(),
                blocker.language(), blocker.missingFields(), blocker.fileId(), blocker.assetId(),
                blocker.contentHash(), blocker.backendErrorCode());
    }

    public ShowroomVersionCenterRepublishRespVO republish(String targetType, Long targetId, Long sourceRevisionId,
                                                          Long newRevisionId, Integer newRevisionNo,
                                                          ShowroomMaterializedRelease release) {
        return new ShowroomVersionCenterRepublishRespVO(targetType, targetId, sourceRevisionId, newRevisionId,
                newRevisionNo, release.releaseId(), release.manifestHash(), release.publishedAt().toString());
    }
}
