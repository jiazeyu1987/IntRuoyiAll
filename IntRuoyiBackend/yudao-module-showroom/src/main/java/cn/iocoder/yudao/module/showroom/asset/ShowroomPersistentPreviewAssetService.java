package cn.iocoder.yudao.module.showroom.asset;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.dal.dataobject.asset.ShowroomPreviewAssetVersionDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.asset.ShowroomPreviewAssetVersionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
public class ShowroomPersistentPreviewAssetService implements ShowroomPreviewAssetOperations {

    private final ShowroomPreviewAssetVersionMapper previewAssetVersionMapper;
    private final Clock clock;

    @Autowired
    public ShowroomPersistentPreviewAssetService(ShowroomPreviewAssetVersionMapper previewAssetVersionMapper) {
        this(previewAssetVersionMapper, Clock.systemUTC());
    }

    ShowroomPersistentPreviewAssetService(ShowroomPreviewAssetVersionMapper previewAssetVersionMapper, Clock clock) {
        this.previewAssetVersionMapper = previewAssetVersionMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomPreviewAssetVersion bindStaticPreviewAssets(ShowroomPreviewAssetDraftCommand command) {
        if (!command.files().hasAllDeviceFiles()) {
            throw new ShowroomPreviewAssetException("SHOWROOM_PREVIEW_STATIC_ASSET_MISSING",
                    "desktop, mobile, and pad static preview file references are all required");
        }
        ShowroomPreviewAssetVersionDO latest = previewAssetVersionMapper.selectLatestByKey(command.targetType().name(),
                command.targetId());
        int versionNo = latest == null ? 1 : latest.getVersionNo() + 1;
        ShowroomPreviewAssetVersionDO version = ShowroomPreviewAssetVersionDO.builder()
                .targetType(command.targetType().name())
                .targetId(command.targetId())
                .sourceRevisionId(command.sourceRevisionId())
                .versionNo(versionNo)
                .imageFileId(command.files().desktopFileId())
                .status(ShowroomPreviewAssetStatus.DRAFT.name())
                .generatedByAi(Boolean.FALSE)
                .build();
        version.setTenantId(TenantContextHolder.getRequiredTenantId());
        previewAssetVersionMapper.insert(version);
        return toDomain(version, false, command.files());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomPreviewAssetVersion submit(Long previewAssetVersionId) {
        ShowroomPreviewAssetVersionDO version = requireVersionDO(previewAssetVersionId);
        requireStatus(version, ShowroomPreviewAssetStatus.DRAFT);
        version.setStatus(ShowroomPreviewAssetStatus.PENDING_SUPERVISOR_REVIEW.name());
        previewAssetVersionMapper.updateById(version);
        return toDomain(version, isLiveVersion(version), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomPreviewAssetVersion supervisorApprove(Long previewAssetVersionId, Long supervisorUserId) {
        if (supervisorUserId == null) {
            throw new ShowroomPreviewAssetException("SHOWROOM_DEPT_SUPERVISOR_MISSING",
                    "department supervisor is required for preview asset approval");
        }
        ShowroomPreviewAssetVersionDO version = requireVersionDO(previewAssetVersionId);
        requireStatus(version, ShowroomPreviewAssetStatus.PENDING_SUPERVISOR_REVIEW);
        version.setStatus(ShowroomPreviewAssetStatus.PENDING_GAOXIN_APPROVAL.name());
        previewAssetVersionMapper.updateById(version);
        return toDomain(version, false, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomPreviewAssetVersion gaoxinApprove(Long previewAssetVersionId, Long gaoxinApproverUserId) {
        if (gaoxinApproverUserId == null) {
            throw new ShowroomPreviewAssetException("SHOWROOM_ROLE_BINDING_MISSING",
                    "Gaoxin approver is required for preview asset approval");
        }
        ShowroomPreviewAssetVersionDO version = requireVersionDO(previewAssetVersionId);
        requireStatus(version, ShowroomPreviewAssetStatus.PENDING_GAOXIN_APPROVAL);
        version.setStatus(ShowroomPreviewAssetStatus.APPROVED.name());
        previewAssetVersionMapper.updateById(version);
        return toDomain(version, false, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomPreviewAssetVersion publish(Long previewAssetVersionId) {
        ShowroomPreviewAssetVersionDO version = requireVersionDO(previewAssetVersionId);
        if (!ShowroomPreviewAssetStatus.APPROVED.name().equals(version.getStatus())) {
            throw new ShowroomPreviewAssetException("SHOWROOM_PREVIEW_ASSET_NOT_APPROVED",
                    "preview asset must be approved before publish");
        }
        return publishPreparedVersion(version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShowroomPreviewAssetVersion publishDirectly(Long previewAssetVersionId) {
        ShowroomPreviewAssetVersionDO version = requireVersionDO(previewAssetVersionId);
        return publishPreparedVersion(version);
    }

    private ShowroomPreviewAssetVersion publishPreparedVersion(ShowroomPreviewAssetVersionDO version) {
        version.setStatus(ShowroomPreviewAssetStatus.PUBLISHED.name());
        version.setPublishedAt(LocalDateTime.ofInstant(Instant.now(clock), ZoneOffset.UTC));
        previewAssetVersionMapper.updateById(version);
        return toDomain(version, true, null);
    }

    @Override
    public Optional<ShowroomPreviewAssetVersion> live(ShowroomPreviewAssetKey key) {
        ShowroomPreviewAssetVersionDO version = previewAssetVersionMapper.selectLatestPublishedByKey(
                key.targetType().name(), key.targetId());
        return version == null ? Optional.empty() : Optional.of(toDomain(version, true, null));
    }

    @Override
    public Optional<Long> liveImageFileId(ShowroomPreviewAssetKey key) {
        ShowroomPreviewAssetVersionDO version = previewAssetVersionMapper.selectLatestPublishedByKey(
                key.targetType().name(), key.targetId());
        return Optional.ofNullable(version == null ? null : version.getImageFileId());
    }

    @Override
    public ShowroomPreviewAssetVersion version(Long previewAssetVersionId) {
        ShowroomPreviewAssetVersionDO version = requireVersionDO(previewAssetVersionId);
        return toDomain(version, isLiveVersion(version), null);
    }

    private ShowroomPreviewAssetVersionDO requireVersionDO(Long previewAssetVersionId) {
        ShowroomPreviewAssetVersionDO version = previewAssetVersionMapper.selectById(previewAssetVersionId);
        if (version == null) {
            throw new ShowroomPreviewAssetException("SHOWROOM_TARGET_NOT_FOUND",
                    "preview asset version does not exist: " + previewAssetVersionId);
        }
        if (!TenantContextHolder.getRequiredTenantId().equals(version.getTenantId())) {
            throw new ShowroomPreviewAssetException("SHOWROOM_TARGET_NOT_FOUND",
                    "preview asset version does not exist: " + previewAssetVersionId);
        }
        return version;
    }

    private boolean isLiveVersion(ShowroomPreviewAssetVersionDO version) {
        if (!ShowroomPreviewAssetStatus.PUBLISHED.name().equals(version.getStatus())) {
            return false;
        }
        ShowroomPreviewAssetVersionDO latest = previewAssetVersionMapper.selectLatestPublishedByKey(
                version.getTargetType(), version.getTargetId());
        return latest != null && latest.getId().equals(version.getId());
    }

    private ShowroomPreviewAssetVersion toDomain(ShowroomPreviewAssetVersionDO version, boolean live,
                                                 ShowroomPreviewAssetFiles draftFiles) {
        ShowroomPreviewAssetFiles files = draftFiles != null
                ? draftFiles
                : new ShowroomPreviewAssetFiles(version.getImageFileId(), null, null);
        return new ShowroomPreviewAssetVersion(version.getId(), new ShowroomPreviewAssetKey(
                ShowroomPreviewAssetTargetType.valueOf(version.getTargetType()), version.getTargetId()),
                version.getSourceRevisionId(), version.getVersionNo(), files,
                ShowroomPreviewAssetStatus.valueOf(version.getStatus()),
                Boolean.TRUE.equals(version.getGeneratedByAi()), false, toInstant(version.getGeneratedAt()),
                toInstant(version.getPublishedAt()), live);
    }

    private static void requireStatus(ShowroomPreviewAssetVersionDO version, ShowroomPreviewAssetStatus expectedStatus) {
        if (!expectedStatus.name().equals(version.getStatus())) {
            throw new ShowroomPreviewAssetException("SHOWROOM_PREVIEW_ASSET_STATUS_INVALID",
                    "expected " + expectedStatus + " but was " + version.getStatus());
        }
    }

    private static Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

}
