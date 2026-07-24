package cn.iocoder.yudao.module.showroom.narration;

import java.time.Instant;
import java.util.Objects;

public record ShowroomNarrationVersion(Long id,
                                       ShowroomNarrationKey key,
                                       Long sourceRevisionId,
                                       int versionNo,
                                       String scriptText,
                                       Long audioFileId,
                                       Integer audioDurationSeconds,
                                       String voice,
                                       ShowroomNarrationGenerationStatus generationStatus,
                                       ShowroomNarrationStatus status,
                                       boolean generatedByAi,
                                       Instant generatedAt,
                                       Instant publishedAt,
                                       boolean live) {

    public ShowroomNarrationVersion {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(sourceRevisionId, "sourceRevisionId");
        Objects.requireNonNull(generationStatus, "generationStatus");
        Objects.requireNonNull(status, "status");
    }

    public ShowroomNarrationVersion withAudio(Long newAudioFileId, Integer newAudioDurationSeconds,
                                              ShowroomNarrationGenerationStatus newGenerationStatus) {
        return withAudio(newAudioFileId, newAudioDurationSeconds, null, newGenerationStatus);
    }

    public ShowroomNarrationVersion withAudio(Long newAudioFileId, Integer newAudioDurationSeconds,
                                              String newVoice,
                                              ShowroomNarrationGenerationStatus newGenerationStatus) {
        return new ShowroomNarrationVersion(id, key, sourceRevisionId, versionNo, scriptText, newAudioFileId,
                newAudioDurationSeconds, newVoice, newGenerationStatus, status, generatedByAi, generatedAt, publishedAt, live);
    }

    public ShowroomNarrationVersion withStatus(ShowroomNarrationStatus newStatus) {
        return new ShowroomNarrationVersion(id, key, sourceRevisionId, versionNo, scriptText, audioFileId,
                audioDurationSeconds, voice, generationStatus, newStatus, generatedByAi, generatedAt, publishedAt, live);
    }

    public ShowroomNarrationVersion withPublication(Instant newPublishedAt, boolean newLive) {
        return new ShowroomNarrationVersion(id, key, sourceRevisionId, versionNo, scriptText, audioFileId,
                audioDurationSeconds, voice, generationStatus, ShowroomNarrationStatus.PUBLISHED, generatedByAi,
                generatedAt, newPublishedAt, newLive);
    }

    public ShowroomNarrationVersion withLive(boolean newLive) {
        return new ShowroomNarrationVersion(id, key, sourceRevisionId, versionNo, scriptText, audioFileId,
                audioDurationSeconds, voice, generationStatus, status, generatedByAi, generatedAt, publishedAt, newLive);
    }

}
