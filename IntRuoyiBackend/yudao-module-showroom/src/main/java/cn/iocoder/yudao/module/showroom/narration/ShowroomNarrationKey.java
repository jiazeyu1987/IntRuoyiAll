package cn.iocoder.yudao.module.showroom.narration;

import java.util.Objects;

public record ShowroomNarrationKey(ShowroomNarrationTargetType targetType,
                                   Long targetId,
                                   ShowroomNarrationAudienceType audienceType,
                                   ShowroomNarrationLanguage language) {

    public ShowroomNarrationKey {
        Objects.requireNonNull(targetType, "targetType");
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(audienceType, "audienceType");
        Objects.requireNonNull(language, "language");
    }

}
