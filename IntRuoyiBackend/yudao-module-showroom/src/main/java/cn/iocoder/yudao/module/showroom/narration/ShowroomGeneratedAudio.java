package cn.iocoder.yudao.module.showroom.narration;

import java.util.Objects;

public record ShowroomGeneratedAudio(Long audioFileId, int audioDurationSeconds, String voice) {

    public ShowroomGeneratedAudio {
        Objects.requireNonNull(audioFileId, "audioFileId");
    }

    public ShowroomGeneratedAudio(Long audioFileId, int audioDurationSeconds) {
        this(audioFileId, audioDurationSeconds, null);
    }

}
