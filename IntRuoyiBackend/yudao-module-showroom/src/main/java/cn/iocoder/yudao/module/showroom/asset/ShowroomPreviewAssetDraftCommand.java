package cn.iocoder.yudao.module.showroom.asset;

import java.util.Objects;

public record ShowroomPreviewAssetDraftCommand(ShowroomPreviewAssetTargetType targetType,
                                               Long targetId,
                                               Long sourceRevisionId,
                                               ShowroomPreviewAssetFiles files) {

    public ShowroomPreviewAssetDraftCommand {
        Objects.requireNonNull(targetType, "targetType");
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(sourceRevisionId, "sourceRevisionId");
        Objects.requireNonNull(files, "files");
    }

    public ShowroomPreviewAssetKey key() {
        return new ShowroomPreviewAssetKey(targetType, targetId);
    }

}
