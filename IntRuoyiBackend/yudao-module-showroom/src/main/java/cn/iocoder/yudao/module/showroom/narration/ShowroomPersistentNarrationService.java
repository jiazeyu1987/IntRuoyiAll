package cn.iocoder.yudao.module.showroom.narration;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.dal.dataobject.narration.ShowroomNarrationVersionDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.narration.ShowroomNarrationVersionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
public class ShowroomPersistentNarrationService implements ShowroomNarrationOperations {

    private final ShowroomNarrationVersionMapper narrationVersionMapper;
    private final Clock clock;
    private final ShowroomAudioGenerationAdapter audioGenerationAdapter;

    public ShowroomPersistentNarrationService(ShowroomNarrationVersionMapper narrationVersionMapper) {
        this(narrationVersionMapper, Clock.systemUTC(), null);
    }

    @Autowired
    public ShowroomPersistentNarrationService(ShowroomNarrationVersionMapper narrationVersionMapper,
                                              ObjectProvider<ShowroomAudioGenerationAdapter> audioGenerationAdapter) {
        this(narrationVersionMapper, Clock.systemUTC(), audioGenerationAdapter.getIfAvailable());
    }

    ShowroomPersistentNarrationService(ShowroomNarrationVersionMapper narrationVersionMapper, Clock clock,
                                       ShowroomAudioGenerationAdapter audioGenerationAdapter) {
        this.narrationVersionMapper = narrationVersionMapper;
        this.clock = clock;
        this.audioGenerationAdapter = audioGenerationAdapter;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomNarrationVersion draftScript(ShowroomNarrationDraftCommand command) {
        if (isBlank(command.scriptText())) {
            throw new ShowroomNarrationException("SHOWROOM_SCRIPT_MISSING",
                    "script text is required before narration draft creation");
        }
        ShowroomNarrationVersionDO latest = narrationVersionMapper.selectLatestByKey(command.targetType().name(),
                command.targetId(), command.audienceType().name(), command.language().name());
        int versionNo = latest == null ? 1 : latest.getVersionNo() + 1;
        ShowroomNarrationVersionDO version = ShowroomNarrationVersionDO.builder()
                .targetType(command.targetType().name())
                .targetId(command.targetId())
                .sourceRevisionId(command.sourceRevisionId())
                .audienceType(command.audienceType().name())
                .language(command.language().name())
                .versionNo(versionNo)
                .scriptText(command.scriptText())
                .generationStatus(ShowroomNarrationGenerationStatus.SCRIPT_GENERATED.name())
                .status(ShowroomNarrationStatus.DRAFT.name())
                .generatedByAi(command.generatedByAi())
                .generatedAt(command.generatedByAi() ? LocalDateTime.ofInstant(Instant.now(clock), ZoneOffset.UTC) : null)
                .build();
        version.setTenantId(TenantContextHolder.getRequiredTenantId());
        narrationVersionMapper.insert(version);
        return toDomain(version, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomNarrationVersion attachAudio(ShowroomNarrationAudioDraftCommand command) {
        if (command.audioDurationSeconds() <= 0) {
            throw new ShowroomNarrationException("SHOWROOM_AUDIO_GENERATION_FAILED",
                    "audio duration must be positive");
        }
        ShowroomNarrationVersionDO version = requireVersionDO(command.narrationVersionId());
        version.setAudioFileId(command.audioFileId());
        version.setAudioDurationSeconds(command.audioDurationSeconds());
        version.setVoice(command.voice());
        version.setGenerationStatus(ShowroomNarrationGenerationStatus.AUDIO_GENERATED.name());
        narrationVersionMapper.updateById(version);
        return toDomain(version, isLiveVersion(version));
    }

    @Override
    public ShowroomNarrationVersion generateAudio(Long narrationVersionId) {
        ShowroomNarrationVersion version = version(narrationVersionId);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomNarrationVersion submit(Long narrationVersionId) {
        ShowroomNarrationVersionDO version = requireVersionDO(narrationVersionId);
        requireStatus(version, ShowroomNarrationStatus.DRAFT);
        version.setStatus(ShowroomNarrationStatus.PENDING_SUPERVISOR_REVIEW.name());
        narrationVersionMapper.updateById(version);
        return toDomain(version, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomNarrationVersion supervisorApprove(Long narrationVersionId, Long supervisorUserId) {
        if (supervisorUserId == null) {
            throw new ShowroomNarrationException("SHOWROOM_DEPT_SUPERVISOR_MISSING",
                    "department supervisor is required for narration approval");
        }
        ShowroomNarrationVersionDO version = requireVersionDO(narrationVersionId);
        requireStatus(version, ShowroomNarrationStatus.PENDING_SUPERVISOR_REVIEW);
        version.setStatus(ShowroomNarrationStatus.PENDING_GAOXIN_APPROVAL.name());
        narrationVersionMapper.updateById(version);
        return toDomain(version, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomNarrationVersion gaoxinApprove(Long narrationVersionId, Long gaoxinApproverUserId) {
        if (gaoxinApproverUserId == null) {
            throw new ShowroomNarrationException("SHOWROOM_ROLE_BINDING_MISSING",
                    "Gaoxin approver is required for narration approval");
        }
        ShowroomNarrationVersionDO version = requireVersionDO(narrationVersionId);
        requireStatus(version, ShowroomNarrationStatus.PENDING_GAOXIN_APPROVAL);
        version.setStatus(ShowroomNarrationStatus.APPROVED.name());
        narrationVersionMapper.updateById(version);
        return toDomain(version, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomNarrationVersion publish(Long narrationVersionId) {
        ShowroomNarrationVersionDO version = requireVersionDO(narrationVersionId);
        requireStatus(version, ShowroomNarrationStatus.APPROVED);
        return publishPreparedVersion(version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomNarrationVersion publishDirectly(Long narrationVersionId) {
        ShowroomNarrationVersionDO version = requireVersionDO(narrationVersionId);
        return publishPreparedVersion(version);
    }

    private ShowroomNarrationVersion publishPreparedVersion(ShowroomNarrationVersionDO version) {
        if (isBlank(version.getScriptText())) {
            throw new ShowroomNarrationException("SHOWROOM_SCRIPT_MISSING",
                    "script text is required before narration publish");
        }
        if (version.getAudioFileId() == null) {
            throw new ShowroomNarrationException("SHOWROOM_AUDIO_GENERATION_FAILED",
                    "approved audio metadata is required before narration publish");
        }
        version.setStatus(ShowroomNarrationStatus.PUBLISHED.name());
        version.setPublishedAt(LocalDateTime.ofInstant(Instant.now(clock), ZoneOffset.UTC));
        narrationVersionMapper.updateById(version);
        return toDomain(version, true);
    }

    @Override
    public Optional<ShowroomNarrationVersion> live(ShowroomNarrationKey key) {
        ShowroomNarrationVersionDO version = narrationVersionMapper.selectLatestPublishedByKey(key.targetType().name(),
                key.targetId(), key.audienceType().name(), key.language().name());
        return version == null ? Optional.empty() : Optional.of(toDomain(version, true));
    }

    @Override
    public Optional<ShowroomNarrationVersion> latest(ShowroomNarrationKey key) {
        ShowroomNarrationVersionDO version = narrationVersionMapper.selectLatestByKey(key.targetType().name(),
                key.targetId(), key.audienceType().name(), key.language().name());
        return version == null ? Optional.empty() : Optional.of(toDomain(version, isLiveVersion(version)));
    }

    @Override
    public ShowroomNarrationVersion version(Long narrationVersionId) {
        ShowroomNarrationVersionDO version = requireVersionDO(narrationVersionId);
        return toDomain(version, isLiveVersion(version));
    }

    @Override
    public Optional<ShowroomNarrationVersion> latest(ShowroomNarrationKey key, Long sourceRevisionId) {
        ShowroomNarrationVersionDO version = narrationVersionMapper.selectLatestByKeyAndSourceRevision(
                key.targetType().name(), key.targetId(), key.audienceType().name(), key.language().name(),
                sourceRevisionId);
        return version == null ? Optional.empty() : Optional.of(toDomain(version, isLiveVersion(version)));
    }

    @Override
    public Optional<ShowroomNarrationVersion> latestPublished(ShowroomNarrationKey key, Long sourceRevisionId) {
        ShowroomNarrationVersionDO version = narrationVersionMapper.selectLatestPublishedByKeyAndSourceRevision(
                key.targetType().name(), key.targetId(), key.audienceType().name(), key.language().name(),
                sourceRevisionId);
        return version == null ? Optional.empty() : Optional.of(toDomain(version, isLiveVersion(version)));
    }

    private ShowroomNarrationVersionDO requireVersionDO(Long narrationVersionId) {
        ShowroomNarrationVersionDO version = narrationVersionMapper.selectById(narrationVersionId);
        if (version == null) {
            throw new ShowroomNarrationException("SHOWROOM_TARGET_NOT_FOUND",
                    "narration version does not exist: " + narrationVersionId);
        }
        if (!TenantContextHolder.getRequiredTenantId().equals(version.getTenantId())) {
            throw new ShowroomNarrationException("SHOWROOM_TARGET_NOT_FOUND",
                    "narration version does not exist: " + narrationVersionId);
        }
        return version;
    }

    private boolean isLiveVersion(ShowroomNarrationVersionDO version) {
        if (!ShowroomNarrationStatus.PUBLISHED.name().equals(version.getStatus())) {
            return false;
        }
        ShowroomNarrationVersionDO latest = narrationVersionMapper.selectLatestPublishedByKey(version.getTargetType(),
                version.getTargetId(), version.getAudienceType(), version.getLanguage());
        return latest != null && latest.getId().equals(version.getId());
    }

    private ShowroomNarrationVersion toDomain(ShowroomNarrationVersionDO version, boolean live) {
        return new ShowroomNarrationVersion(version.getId(), new ShowroomNarrationKey(
                ShowroomNarrationTargetType.valueOf(version.getTargetType()), version.getTargetId(),
                ShowroomNarrationAudienceType.valueOf(version.getAudienceType()),
                ShowroomNarrationLanguage.valueOf(version.getLanguage())), requireNonNull(version.getSourceRevisionId(),
                "sourceRevisionId"), version.getVersionNo(), version.getScriptText(), version.getAudioFileId(),
                version.getAudioDurationSeconds(), version.getVoice(),
                ShowroomNarrationGenerationStatus.valueOf(version.getGenerationStatus()),
                ShowroomNarrationStatus.valueOf(version.getStatus()), Boolean.TRUE.equals(version.getGeneratedByAi()),
                toInstant(version.getGeneratedAt()), toInstant(version.getPublishedAt()), live);
    }

    private static void requireStatus(ShowroomNarrationVersionDO version, ShowroomNarrationStatus expectedStatus) {
        if (!expectedStatus.name().equals(version.getStatus())) {
            throw new ShowroomNarrationException("SHOWROOM_NARRATION_STATUS_INVALID",
                    "expected " + expectedStatus + " but was " + version.getStatus());
        }
    }

    private static Long requireNonNull(Long value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException(fieldName + " must not be null");
        }
        return value;
    }

    private static Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
