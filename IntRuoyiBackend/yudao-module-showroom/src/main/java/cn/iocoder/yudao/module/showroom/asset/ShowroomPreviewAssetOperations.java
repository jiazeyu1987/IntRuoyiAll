package cn.iocoder.yudao.module.showroom.asset;

import java.util.Optional;

public interface ShowroomPreviewAssetOperations {

    ShowroomPreviewAssetVersion bindStaticPreviewAssets(ShowroomPreviewAssetDraftCommand command);

    ShowroomPreviewAssetVersion submit(Long previewAssetVersionId);

    ShowroomPreviewAssetVersion supervisorApprove(Long previewAssetVersionId, Long supervisorUserId);

    ShowroomPreviewAssetVersion gaoxinApprove(Long previewAssetVersionId, Long gaoxinApproverUserId);

    ShowroomPreviewAssetVersion publish(Long previewAssetVersionId);

    ShowroomPreviewAssetVersion publishDirectly(Long previewAssetVersionId);

    Optional<ShowroomPreviewAssetVersion> live(ShowroomPreviewAssetKey key);

    Optional<Long> liveImageFileId(ShowroomPreviewAssetKey key);

    ShowroomPreviewAssetVersion version(Long previewAssetVersionId);

}
