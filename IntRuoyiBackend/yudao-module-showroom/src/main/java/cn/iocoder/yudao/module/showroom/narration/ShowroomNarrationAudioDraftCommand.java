package cn.iocoder.yudao.module.showroom.narration;

import java.util.Objects;

public record ShowroomNarrationAudioDraftCommand(Long narrationVersionId,
                                                 Long audioFileId,
                                                 int audioDurationSeconds,
                                                 String voice) {

    public ShowroomNarrationAudioDraftCommand {
        Objects.requireNonNull(narrationVersionId, "narrationVersionId");
        Objects.requireNonNull(audioFileId, "audioFileId");
    }

    public ShowroomNarrationAudioDraftCommand(Long narrationVersionId,
                                              Long audioFileId,
                                              int audioDurationSeconds) {
        this(narrationVersionId, audioFileId, audioDurationSeconds, null);
    }

}
