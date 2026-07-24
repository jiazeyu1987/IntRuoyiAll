package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseTombstoneDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseAssetMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseAssetRefMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseDocumentMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseTombstoneMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class ShowroomReleasePurgeService {

    private final ShowroomReleaseMapper releaseMapper;
    private final ShowroomReleaseDocumentMapper documentMapper;
    private final ShowroomReleaseAssetMapper assetMapper;
    private final ShowroomReleaseAssetRefMapper assetRefMapper;
    private final ShowroomReleaseTombstoneMapper tombstoneMapper;

    public ShowroomReleasePurgeService(ShowroomReleaseMapper releaseMapper,
                                       ShowroomReleaseDocumentMapper documentMapper,
                                       ShowroomReleaseAssetMapper assetMapper,
                                       ShowroomReleaseAssetRefMapper assetRefMapper,
                                       ShowroomReleaseTombstoneMapper tombstoneMapper) {
        this.releaseMapper = releaseMapper;
        this.documentMapper = documentMapper;
        this.assetMapper = assetMapper;
        this.assetRefMapper = assetRefMapper;
        this.tombstoneMapper = tombstoneMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void purgeExpiredReleases() {
        throw ShowroomPublicReleaseScopeResolver.siteSelectorRequired();
    }

    @Transactional(rollbackFor = Exception.class)
    public void purgeExpiredReleases(ShowroomReleaseScope scope) {
        List<cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseDO> releases =
                releaseMapper.selectPublishedByScopeOrderByPublishedAtDesc(scope.tenantId(), scope.siteKey(),
                        scope.stage());
        if (releases.size() <= ShowroomReleaseConstants.RETAIN_COUNT) {
            return;
        }
        List<cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseDO> toPurge =
                releases.subList(ShowroomReleaseConstants.RETAIN_COUNT, releases.size());
        for (var release : toPurge) {
            LocalDateTime purgedAt = LocalDateTime.now(ZoneOffset.UTC);
            insertTombstone(scope, ShowroomReleaseConstants.RESOURCE_TYPE_RELEASE, release.getReleaseId(),
                    "retention-window", purgedAt);
            for (var document : documentMapper.selectListByReleaseScope(scope.tenantId(), scope.siteKey(),
                    scope.stage(), release.getReleaseId())) {
                insertTombstone(scope, ShowroomReleaseConstants.RESOURCE_TYPE_DOCUMENT,
                        release.getReleaseId() + ":" + document.getDocumentId(), "retention-window", purgedAt);
                documentMapper.deleteById(document.getId());
            }
            for (var ref : assetRefMapper.selectListByReleaseScope(scope.tenantId(), scope.siteKey(), scope.stage(),
                    release.getReleaseId())) {
                assetRefMapper.deleteById(ref.getId());
                if (assetRefMapper.countRetainedByAssetScope(scope.tenantId(), scope.siteKey(), scope.stage(),
                        ref.getAssetId(), ref.getContentHash()) == 0) {
                    insertTombstone(scope, ShowroomReleaseConstants.RESOURCE_TYPE_ASSET,
                            ref.getAssetId() + ":" + ref.getContentHash(), "retention-window", purgedAt);
                    var asset = assetMapper.selectByScopeAssetIdAndContentHash(scope.tenantId(), scope.siteKey(),
                            scope.stage(), ref.getAssetId(), ref.getContentHash());
                    if (asset != null) {
                        assetMapper.deleteById(asset.getId());
                    }
                }
            }
            release.setStatus(ShowroomReleaseConstants.RELEASE_STATUS_PURGED);
            releaseMapper.updateById(release);
        }
    }

    private void insertTombstone(ShowroomReleaseScope scope, String resourceType, String resourceKey, String reason,
                                 LocalDateTime purgedAt) {
        ShowroomReleaseTombstoneDO existing = tombstoneMapper.selectAnyByScopedResource(scope.tenantId(),
                scope.siteKey(), scope.stage(), resourceType, resourceKey);
        ShowroomReleaseTombstoneDO candidate = buildTombstone(scope, resourceType, resourceKey, reason, purgedAt);
        if (existing != null) {
            if (Boolean.TRUE.equals(existing.getDeleted())) {
                tombstoneMapper.reviveTombstoneById(existing.getId(), candidate);
            }
            return;
        }

        try {
            tombstoneMapper.insert(candidate);
        } catch (DataIntegrityViolationException exception) {
            ShowroomReleaseTombstoneDO conflicted = tombstoneMapper.selectAnyByScopedResource(scope.tenantId(),
                    scope.siteKey(), scope.stage(), resourceType, resourceKey);
            if (conflicted == null) {
                throw exception;
            }
            if (Boolean.TRUE.equals(conflicted.getDeleted())) {
                tombstoneMapper.reviveTombstoneById(conflicted.getId(), candidate);
            }
        }
    }

    private ShowroomReleaseTombstoneDO buildTombstone(ShowroomReleaseScope scope, String resourceType,
                                                     String resourceKey, String reason, LocalDateTime purgedAt) {
        return ShowroomReleaseTombstoneDO.builder()
                .tenantId(scope.tenantId())
                .siteKey(scope.siteKey())
                .stage(scope.stage())
                .resourceType(resourceType)
                .resourceKey(resourceKey)
                .purgedAt(purgedAt)
                .reason(reason)
                .build();
    }
}
