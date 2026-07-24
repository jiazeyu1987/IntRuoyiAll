package cn.iocoder.yudao.module.showroom.narration;

import java.util.Objects;

public record ShowroomAudioGenerationRequest(ShowroomNarrationVersion narrationVersion) {

    public ShowroomAudioGenerationRequest {
        Objects.requireNonNull(narrationVersion, "narrationVersion");
    }

}
