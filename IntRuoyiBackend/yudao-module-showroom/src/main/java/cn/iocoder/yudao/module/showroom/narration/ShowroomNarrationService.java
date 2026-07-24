package cn.iocoder.yudao.module.showroom.narration;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ShowroomNarrationService implements ShowroomNarrationOperations {

    private final Clock clock;
    private final ShowroomAudioGenerationAdapter audioGenerationAdapter;
    private final Map<Long, ShowroomNarrationVersion> versionsById = new LinkedHashMap<>();
    private final Map<ShowroomNarrationKey, Integer> latestVersionNoByKey = new HashMap<>();
    private final Map<ShowroomNarrationKey, Long> liveVersionIdByKey = new HashMap<>();
    private long nextId = 1L;

    public ShowroomNarrationService(Clock clock, ShowroomAudioGenerationAdapter audioGenerationAdapter) {
        this.clock = clock;
        this.audioGenerationAdapter = audioGenerationAdapter;
    }

    public static ShowroomNarrationService withoutAudioAdapter(Clock clock) {
        return new ShowroomNarrationService(clock, null);
    }

    public synchronized ShowroomNarrationVersion draftScript(ShowroomNarrationDraftCommand command) {
        if (isBlank(command.scriptText())) {
            throw new ShowroomNarrationException("SHOWROOM_SCRIPT_MISSING",
                    "script text is required before narration draft creation");
        }
        ShowroomNarrationKey key = command.key();
        int versionNo = latestVersionNoByKey.merge(key, 1, Integer::sum);
        ShowroomNarrationVersion version = new ShowroomNarrationVersion(nextId++, key, command.sourceRevisionId(),
                versionNo, command.scriptText(), null, null, null, ShowroomNarrationGenerationStatus.SCRIPT_GENERATED,
                ShowroomNarrationStatus.DRAFT, command.generatedByAi(),
                command.generatedByAi() ? Instant.now(clock) : null, null, false);
        versionsById.put(version.id(), version);
        return version;
    }

    public synchronized ShowroomNarrationVersion attachAudio(ShowroomNarrationAudioDraftCommand command) {
        if (command.audioDurationSeconds() <= 0) {
            throw new ShowroomNarrationException("SHOWROOM_AUDIO_GENERATION_FAILED",
                    "audio duration must be positive");
        }
        ShowroomNarrationVersion version = requireVersion(command.narrationVersionId());
        ShowroomNarrationVersion updated = version.withAudio(command.audioFileId(), command.audioDurationSeconds(),
                command.voice(), ShowroomNarrationGenerationStatus.AUDIO_GENERATED);
        versionsById.put(updated.id(), updated);
        return updated;
    }

    public synchronized ShowroomNarrationVersion generateAudio(Long narrationVersionId) {
        ShowroomNarrationVersion version = requireVersion(narrationVersionId);
        if (audioGenerationAdapter == null) {
            throw new ShowroomNarrationException("SHOWROOM_AUDIO_GENERATION_FAILED",
                    "audio generation adapter contract is missing");
        }
        ShowroomGeneratedAudio generatedAudio = audioGenerationAdapter.generate(new ShowroomAudioGenerationRequest(version));
        if (generatedAudio == null) {
            throw new ShowroomNarrationException("SHOWROOM_AUDIO_GENERATION_FAILED",
                    "audio generation adapter returned no audio metadata");
        }
        return attachAudio(new ShowroomNarrationAudioDraftCommand(version.id(), generatedAudio.audioFileId(),
                generatedAudio.audioDurationSeconds(), generatedAudio.voice()));
    }

    public synchronized ShowroomNarrationVersion submit(Long narrationVersionId) {
        ShowroomNarrationVersion version = requireVersion(narrationVersionId);
        requireStatus(version, ShowroomNarrationStatus.DRAFT);
        return replace(version.withStatus(ShowroomNarrationStatus.PENDING_SUPERVISOR_REVIEW));
    }

    public synchronized ShowroomNarrationVersion supervisorApprove(Long narrationVersionId, Long supervisorUserId) {
        if (supervisorUserId == null) {
            throw new ShowroomNarrationException("SHOWROOM_DEPT_SUPERVISOR_MISSING",
                    "department supervisor is required for narration approval");
        }
        ShowroomNarrationVersion version = requireVersion(narrationVersionId);
        requireStatus(version, ShowroomNarrationStatus.PENDING_SUPERVISOR_REVIEW);
        return replace(version.withStatus(ShowroomNarrationStatus.PENDING_GAOXIN_APPROVAL));
    }

    public synchronized ShowroomNarrationVersion gaoxinApprove(Long narrationVersionId, Long gaoxinApproverUserId) {
        if (gaoxinApproverUserId == null) {
            throw new ShowroomNarrationException("SHOWROOM_ROLE_BINDING_MISSING",
                    "Gaoxin approver is required for narration approval");
        }
        ShowroomNarrationVersion version = requireVersion(narrationVersionId);
        requireStatus(version, ShowroomNarrationStatus.PENDING_GAOXIN_APPROVAL);
        return replace(version.withStatus(ShowroomNarrationStatus.APPROVED));
    }

    public synchronized ShowroomNarrationVersion publish(Long narrationVersionId) {
        ShowroomNarrationVersion version = requireVersion(narrationVersionId);
        if (version.status() != ShowroomNarrationStatus.APPROVED) {
            throw new ShowroomNarrationException("SHOWROOM_NARRATION_NOT_APPROVED",
                    "narration script and audio must be approved before publish");
        }
        return publishPreparedVersion(version);
    }

    public synchronized ShowroomNarrationVersion publishDirectly(Long narrationVersionId) {
        ShowroomNarrationVersion version = requireVersion(narrationVersionId);
        return publishPreparedVersion(version);
    }

    private ShowroomNarrationVersion publishPreparedVersion(ShowroomNarrationVersion version) {
        if (isBlank(version.scriptText())) {
            throw new ShowroomNarrationException("SHOWROOM_SCRIPT_MISSING",
                    "script text is required before narration publish");
        }
        if (version.audioFileId() == null) {
            throw new ShowroomNarrationException("SHOWROOM_AUDIO_GENERATION_FAILED",
                    "approved audio metadata is required before narration publish");
        }
        Long currentLiveVersionId = liveVersionIdByKey.get(version.key());
        if (currentLiveVersionId != null && !currentLiveVersionId.equals(version.id())) {
            ShowroomNarrationVersion currentLive = requireVersion(currentLiveVersionId);
            replace(currentLive.withLive(false));
        }
        ShowroomNarrationVersion published = replace(version.withPublication(Instant.now(clock), true));
        liveVersionIdByKey.put(published.key(), published.id());
        return published;
    }

    @Override
    public synchronized Optional<ShowroomNarrationVersion> live(ShowroomNarrationKey key) {
        Long liveVersionId = liveVersionIdByKey.get(key);
        if (liveVersionId == null) {
            return Optional.empty();
        }
        ShowroomNarrationVersion liveVersion = versionsById.get(liveVersionId);
        return liveVersion == null || !liveVersion.live() ? Optional.empty() : Optional.of(liveVersion);
    }

    @Override
    public synchronized Optional<ShowroomNarrationVersion> latest(ShowroomNarrationKey key) {
        return versionsById.values().stream()
                .filter(version -> version.key().equals(key))
                .max(java.util.Comparator.comparingInt(ShowroomNarrationVersion::versionNo)
                        .thenComparingLong(ShowroomNarrationVersion::id));
    }

    @Override
    public synchronized Optional<ShowroomNarrationVersion> latest(ShowroomNarrationKey key, Long sourceRevisionId) {
        return versionsById.values().stream()
                .filter(version -> version.key().equals(key))
                .filter(version -> java.util.Objects.equals(version.sourceRevisionId(), sourceRevisionId))
                .max(java.util.Comparator.comparingInt(ShowroomNarrationVersion::versionNo)
                        .thenComparingLong(ShowroomNarrationVersion::id));
    }

    @Override
    public synchronized Optional<ShowroomNarrationVersion> latestPublished(ShowroomNarrationKey key, Long sourceRevisionId) {
        return versionsById.values().stream()
                .filter(version -> version.key().equals(key))
                .filter(version -> java.util.Objects.equals(version.sourceRevisionId(), sourceRevisionId))
                .filter(version -> version.status() == ShowroomNarrationStatus.PUBLISHED)
                .max(java.util.Comparator.comparingInt(ShowroomNarrationVersion::versionNo)
                        .thenComparingLong(ShowroomNarrationVersion::id));
    }

    @Override
    public synchronized ShowroomNarrationVersion version(Long narrationVersionId) {
        return requireVersion(narrationVersionId);
    }

    private ShowroomNarrationVersion replace(ShowroomNarrationVersion version) {
        versionsById.put(version.id(), version);
        return version;
    }

    private ShowroomNarrationVersion requireVersion(Long narrationVersionId) {
        ShowroomNarrationVersion version = versionsById.get(narrationVersionId);
        if (version == null) {
            throw new ShowroomNarrationException("SHOWROOM_TARGET_NOT_FOUND",
                    "narration version does not exist: " + narrationVersionId);
        }
        return version;
    }

    private static void requireStatus(ShowroomNarrationVersion version, ShowroomNarrationStatus expectedStatus) {
        if (version.status() != expectedStatus) {
            throw new ShowroomNarrationException("SHOWROOM_NARRATION_STATUS_INVALID",
                    "expected " + expectedStatus + " but was " + version.status());
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
