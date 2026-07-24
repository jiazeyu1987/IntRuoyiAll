package cn.iocoder.yudao.module.showroom.narration;

import java.util.Objects;

public record ShowroomNarrationDraftCommand(ShowroomNarrationTargetType targetType,
                                            Long targetId,
                                            Long sourceRevisionId,
                                            ShowroomNarrationAudienceType audienceType,
                                            ShowroomNarrationLanguage language,
                                            String scriptText,
                                            boolean generatedByAi) {

    public ShowroomNarrationDraftCommand {
        Objects.requireNonNull(targetType, "targetType");
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(sourceRevisionId, "sourceRevisionId");
        Objects.requireNonNull(audienceType, "audienceType");
        Objects.requireNonNull(language, "language");
    }

    public ShowroomNarrationKey key() {
        return new ShowroomNarrationKey(targetType, targetId, audienceType, language);
    }

}
