package cn.iocoder.yudao.module.showroom.asset;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ShowroomPreviewAssetService implements ShowroomPreviewAssetOperations {

    private final Clock clock;
    private final Map<Long, ShowroomPreviewAssetVersion> versionsById = new LinkedHashMap<>();
    private final Map<ShowroomPreviewAssetKey, Integer> latestVersionNoByKey = new HashMap<>();
    private final Map<ShowroomPreviewAssetKey, Long> liveVersionIdByKey = new HashMap<>();
    private long nextId = 1L;

    public ShowroomPreviewAssetService(Clock clock) {
        this.clock = clock;
    }

    @Override
    public synchronized ShowroomPreviewAssetVersion bindStaticPreviewAssets(ShowroomPreviewAssetDraftCommand command) {
        if (!command.files().hasAllDeviceFiles()) {
            throw new ShowroomPreviewAssetException("SHOWROOM_PREVIEW_STATIC_ASSET_MISSING",
                    "desktop, mobile, and pad static preview file references are all required");
        }
        ShowroomPreviewAssetKey key = command.key();
        int versionNo = latestVersionNoByKey.merge(key, 1, Integer::sum);
        ShowroomPreviewAssetVersion version = new ShowroomPreviewAssetVersion(nextId++, key,
                command.sourceRevisionId(), versionNo, command.files(), ShowroomPreviewAssetStatus.DRAFT,
                false, false, null, null, false);
        versionsById.put(version.id(), version);
        return version;
    }

    @Override
    public synchronized ShowroomPreviewAssetVersion submit(Long previewAssetVersionId) {
        ShowroomPreviewAssetVersion version = requireVersion(previewAssetVersionId);
        requireStatus(version, ShowroomPreviewAssetStatus.DRAFT);
        return replace(version.withStatus(ShowroomPreviewAssetStatus.PENDING_SUPERVISOR_REVIEW));
    }

    @Override
    public synchronized ShowroomPreviewAssetVersion supervisorApprove(Long previewAssetVersionId,
                                                                      Long supervisorUserId) {
        if (supervisorUserId == null) {
            throw new ShowroomPreviewAssetException("SHOWROOM_DEPT_SUPERVISOR_MISSING",
                    "department supervisor is required for preview asset approval");
        }
        ShowroomPreviewAssetVersion version = requireVersion(previewAssetVersionId);
        requireStatus(version, ShowroomPreviewAssetStatus.PENDING_SUPERVISOR_REVIEW);
        return replace(version.withStatus(ShowroomPreviewAssetStatus.PENDING_GAOXIN_APPROVAL));
    }

    @Override
    public synchronized ShowroomPreviewAssetVersion gaoxinApprove(Long previewAssetVersionId,
                                                                 Long gaoxinApproverUserId) {
        if (gaoxinApproverUserId == null) {
            throw new ShowroomPreviewAssetException("SHOWROOM_ROLE_BINDING_MISSING",
                    "Gaoxin approver is required for preview asset approval");
        }
        ShowroomPreviewAssetVersion version = requireVersion(previewAssetVersionId);
        requireStatus(version, ShowroomPreviewAssetStatus.PENDING_GAOXIN_APPROVAL);
        return replace(version.withStatus(ShowroomPreviewAssetStatus.APPROVED));
    }

    @Override
    public synchronized ShowroomPreviewAssetVersion publish(Long previewAssetVersionId) {
        ShowroomPreviewAssetVersion version = requireVersion(previewAssetVersionId);
        if (version.status() != ShowroomPreviewAssetStatus.APPROVED) {
            throw new ShowroomPreviewAssetException("SHOWROOM_PREVIEW_ASSET_NOT_APPROVED",
                    "preview asset must be approved before publish");
        }
        return publishPreparedVersion(version);
    }

    @Override
    public synchronized ShowroomPreviewAssetVersion publishDirectly(Long previewAssetVersionId) {
        ShowroomPreviewAssetVersion version = requireVersion(previewAssetVersionId);
        return publishPreparedVersion(version);
    }

    private ShowroomPreviewAssetVersion publishPreparedVersion(ShowroomPreviewAssetVersion version) {
        Long currentLiveVersionId = liveVersionIdByKey.get(version.key());
        if (currentLiveVersionId != null && !currentLiveVersionId.equals(version.id())) {
            ShowroomPreviewAssetVersion currentLive = requireVersion(currentLiveVersionId);
            replace(currentLive.withLive(false));
        }
        ShowroomPreviewAssetVersion published = replace(version.withPublication(Instant.now(clock), true));
        liveVersionIdByKey.put(published.key(), published.id());
        return published;
    }

    @Override
    public synchronized Optional<ShowroomPreviewAssetVersion> live(ShowroomPreviewAssetKey key) {
        Long liveVersionId = liveVersionIdByKey.get(key);
        if (liveVersionId == null) {
            return Optional.empty();
        }
        ShowroomPreviewAssetVersion liveVersion = versionsById.get(liveVersionId);
        return liveVersion == null || !liveVersion.live() ? Optional.empty() : Optional.of(liveVersion);
    }

    @Override
    public synchronized Optional<Long> liveImageFileId(ShowroomPreviewAssetKey key) {
        return live(key).map(version -> version.files().desktopFileId());
    }

    @Override
    public synchronized ShowroomPreviewAssetVersion version(Long previewAssetVersionId) {
        return requireVersion(previewAssetVersionId);
    }

    private ShowroomPreviewAssetVersion replace(ShowroomPreviewAssetVersion version) {
        versionsById.put(version.id(), version);
        return version;
    }

    private ShowroomPreviewAssetVersion requireVersion(Long previewAssetVersionId) {
        ShowroomPreviewAssetVersion version = versionsById.get(previewAssetVersionId);
        if (version == null) {
            throw new ShowroomPreviewAssetException("SHOWROOM_TARGET_NOT_FOUND",
                    "preview asset version does not exist: " + previewAssetVersionId);
        }
        return version;
    }

    private static void requireStatus(ShowroomPreviewAssetVersion version, ShowroomPreviewAssetStatus expectedStatus) {
        if (version.status() != expectedStatus) {
            throw new ShowroomPreviewAssetException("SHOWROOM_PREVIEW_ASSET_STATUS_INVALID",
                    "expected " + expectedStatus + " but was " + version.status());
        }
    }

}
