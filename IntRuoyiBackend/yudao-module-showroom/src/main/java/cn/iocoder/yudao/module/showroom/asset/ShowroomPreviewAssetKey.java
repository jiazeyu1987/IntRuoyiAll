package cn.iocoder.yudao.module.showroom.asset;

import java.util.Objects;

public record ShowroomPreviewAssetKey(ShowroomPreviewAssetTargetType targetType, Long targetId) {

    public ShowroomPreviewAssetKey {
        Objects.requireNonNull(targetType, "targetType");
        Objects.requireNonNull(targetId, "targetId");
    }

}
