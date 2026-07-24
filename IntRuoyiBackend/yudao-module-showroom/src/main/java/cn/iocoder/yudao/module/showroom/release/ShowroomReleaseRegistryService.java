package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseDocumentDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseLegacyProjectionDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleasePointerDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseSourceSnapshotDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseAssetDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseAssetRefDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseAssetMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseAssetRefMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseDocumentMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseLegacyProjectionMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleasePointerMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseSourceSnapshotMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseTombstoneMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class ShowroomReleaseRegistryService {

    private final ShowroomReleaseMapper releaseMapper;
    private final ShowroomReleaseSourceSnapshotMapper snapshotMapper;
    private final ShowroomReleaseDocumentMapper documentMapper;
    private final ShowroomReleaseAssetMapper assetMapper;
    private final ShowroomReleaseAssetRefMapper assetRefMapper;
    private final ShowroomReleaseLegacyProjectionMapper legacyProjectionMapper;
    private final ShowroomReleasePointerMapper pointerMapper;
    private final ShowroomReleaseTombstoneMapper tombstoneMapper;

    public ShowroomReleaseRegistryService(ShowroomReleaseMapper releaseMapper,
                                          ShowroomReleaseSourceSnapshotMapper snapshotMapper,
                                          ShowroomReleaseDocumentMapper documentMapper,
                                          ShowroomReleaseAssetMapper assetMapper,
                                          ShowroomReleaseAssetRefMapper assetRefMapper,
                                          ShowroomReleaseLegacyProjectionMapper legacyProjectionMapper,
                                          ShowroomReleasePointerMapper pointerMapper,
                                          ShowroomReleaseTombstoneMapper tombstoneMapper) {
        this.releaseMapper = releaseMapper;
        this.snapshotMapper = snapshotMapper;
        this.documentMapper = documentMapper;
        this.assetMapper = assetMapper;
        this.assetRefMapper = assetRefMapper;
        this.legacyProjectionMapper = legacyProjectionMapper;
        this.pointerMapper = pointerMapper;
        this.tombstoneMapper = tombstoneMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void persistRelease(ShowroomMaterializedRelease release) {
        throw ShowroomPublicReleaseScopeResolver.siteSelectorRequired();
    }

    @Transactional(rollbackFor = Exception.class)
    public void persistRelease(ShowroomReleaseScope scope, ShowroomMaterializedRelease release) {
        releaseMapper.insert(ShowroomReleaseDO.builder()
                .releaseId(release.releaseId())
                .tenantId(scope.tenantId())
                .siteKey(scope.siteKey())
                .stage(scope.stage())
                .schemaVersion(ShowroomReleaseConstants.SCHEMA_VERSION)
                .manifestHash(release.manifestHash())
                .rootDocumentId(release.rootDocumentId())
                .documentCount(release.documentCount())
                .assetCount(release.assetCount())
                .installBytes(release.installBytes())
                .publishedAt(LocalDateTime.ofInstant(release.publishedAt(), ZoneOffset.UTC))
                .status(ShowroomReleaseConstants.RELEASE_STATUS_PUBLISHED)
                .build());
        snapshotMapper.insert(ShowroomReleaseSourceSnapshotDO.builder()
                .releaseId(release.releaseId())
                .tenantId(scope.tenantId())
                .siteKey(scope.siteKey())
                .stage(scope.stage())
                .companyRevisionId(release.sourceSnapshot().companyRevisionId())
                .hallSnapshotHash(release.sourceSnapshot().hallSnapshotHash())
                .hallProductMappingHash(release.sourceSnapshot().hallProductMappingHash())
                .productRevisionIdsJson(JsonUtils.toJsonString(release.sourceSnapshot().productRevisionIds()))
                .previewAssetVersionIdsJson(JsonUtils.toJsonString(release.sourceSnapshot().previewAssetVersionIds()))
                .narrationVersionIdsJson(JsonUtils.toJsonString(release.sourceSnapshot().narrationVersionIds()))
                .resolvedAt(LocalDateTime.ofInstant(release.sourceSnapshot().resolvedAt(), ZoneOffset.UTC))
                .build());
        for (ShowroomMaterializedRelease.MaterializedDocument document : release.documents()) {
            documentMapper.insert(ShowroomReleaseDocumentDO.builder()
                    .releaseId(release.releaseId())
                    .tenantId(scope.tenantId())
                    .siteKey(scope.siteKey())
                    .stage(scope.stage())
                    .documentId(document.documentId())
                    .kind(document.kind())
                    .productId(document.productId())
                    .contentHash(document.contentHash())
                    .bytes(document.bytes())
                    .materializedAt(LocalDateTime.ofInstant(document.materializedAt(), ZoneOffset.UTC))
                    .payloadJson(document.payloadJson())
                    .build());
        }
        for (ShowroomMaterializedRelease.MaterializedAsset asset : release.assets()) {
            upsertAsset(scope, asset);
        }
        for (ShowroomMaterializedRelease.MaterializedAssetRef ref : release.assetRefs()) {
            assetRefMapper.insert(ShowroomReleaseAssetRefDO.builder()
                    .releaseId(release.releaseId())
                    .tenantId(scope.tenantId())
                    .siteKey(scope.siteKey())
                    .stage(scope.stage())
                    .documentId(ref.documentId())
                    .assetId(ref.assetId())
                    .contentHash(ref.contentHash())
                    .usageCode(ref.usageCode())
                    .build());
        }
        legacyProjectionMapper.insert(ShowroomReleaseLegacyProjectionDO.builder()
                .releaseId(release.releaseId())
                .tenantId(scope.tenantId())
                .siteKey(scope.siteKey())
                .stage(scope.stage())
                .projectionHash(release.legacyProjectionHash())
                .publishedAt(LocalDateTime.ofInstant(release.publishedAt(), ZoneOffset.UTC))
                .rootDocumentId(release.rootDocumentId())
                .status(ShowroomReleaseConstants.LEGACY_STATUS_READY)
                .payloadJson(release.legacyProjectionJson())
                .build());
    }

    @Transactional(rollbackFor = Exception.class)
    public void switchCurrentRelease(String releaseId) {
        throw ShowroomPublicReleaseScopeResolver.siteSelectorRequired();
    }

    @Transactional(rollbackFor = Exception.class)
    public void switchCurrentRelease(ShowroomReleaseScope scope, String releaseId) {
        ShowroomReleaseDO release = releaseMapper.selectByReleaseScope(scope.tenantId(), scope.siteKey(),
                scope.stage(), releaseId);
        if (release == null) {
            throw new IllegalStateException("SHOWROOM_RELEASE_NOT_FOUND: " + releaseId);
        }
        if (legacyProjectionMapper.selectByReleaseScope(scope.tenantId(), scope.siteKey(), scope.stage(),
                releaseId) == null) {
            throw new IllegalStateException("SHOWROOM_RELEASE_BROKEN: missing legacy projection");
        }
        if (documentMapper.selectByReleaseScopeAndDocumentId(scope.tenantId(), scope.siteKey(), scope.stage(),
                releaseId, release.getRootDocumentId()) == null) {
            throw new IllegalStateException("SHOWROOM_RELEASE_BROKEN: missing root document");
        }
        ShowroomReleasePointerDO pointer = pointerMapper.selectByPointerScope(scope.tenantId(), scope.siteKey(),
                scope.stage(), ShowroomReleaseConstants.POINTER_KEY);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (pointer == null) {
            pointerMapper.insert(ShowroomReleasePointerDO.builder()
                    .tenantId(scope.tenantId())
                    .siteKey(scope.siteKey())
                    .stage(scope.stage())
                    .pointerKey(ShowroomReleaseConstants.POINTER_KEY)
                    .releaseId(releaseId)
                    .manifestHash(release.getManifestHash())
                    .updatedAt(now)
                    .build());
            return;
        }
        pointer.setReleaseId(releaseId);
        pointer.setManifestHash(release.getManifestHash());
        pointer.setUpdatedAt(now);
        pointerMapper.updateById(pointer);
    }

    private void upsertAsset(ShowroomReleaseScope scope, ShowroomMaterializedRelease.MaterializedAsset asset) {
        ShowroomReleaseAssetDO candidate = buildAssetDO(scope, asset);
        ShowroomReleaseAssetDO existing = assetMapper.selectAnyByScopeAssetIdAndContentHash(scope.tenantId(),
                scope.siteKey(), scope.stage(), asset.assetId(), asset.contentHash());
        if (existing != null) {
            if (Boolean.TRUE.equals(existing.getDeleted())) {
                assetMapper.reviveAssetById(existing.getId(), candidate);
            }
            deleteAssetTombstone(scope, asset);
            return;
        }
        try {
            assetMapper.insert(candidate);
            deleteAssetTombstone(scope, asset);
        } catch (DataIntegrityViolationException exception) {
            ShowroomReleaseAssetDO conflicted = assetMapper.selectAnyByScopeAssetIdAndContentHash(scope.tenantId(),
                    scope.siteKey(), scope.stage(), asset.assetId(), asset.contentHash());
            if (conflicted == null) {
                throw exception;
            }
            if (Boolean.TRUE.equals(conflicted.getDeleted())) {
                assetMapper.reviveAssetById(conflicted.getId(), candidate);
            }
            deleteAssetTombstone(scope, asset);
        }
    }

    private void deleteAssetTombstone(ShowroomReleaseScope scope, ShowroomMaterializedRelease.MaterializedAsset asset) {
        tombstoneMapper.deleteByScopedResource(scope.tenantId(), scope.siteKey(), scope.stage(),
                ShowroomReleaseConstants.RESOURCE_TYPE_ASSET,
                asset.assetId() + ":" + asset.contentHash());
    }

    private ShowroomReleaseAssetDO buildAssetDO(ShowroomReleaseScope scope,
                                               ShowroomMaterializedRelease.MaterializedAsset asset) {
        return ShowroomReleaseAssetDO.builder()
                .tenantId(scope.tenantId())
                .siteKey(scope.siteKey())
                .stage(scope.stage())
                .assetId(asset.assetId())
                .assetType(asset.assetType())
                .contentHash(asset.contentHash())
                .mimeType(asset.mimeType())
                .bytes(asset.bytes())
                .storageKey(asset.storageKey())
                .materializedAt(LocalDateTime.ofInstant(asset.materializedAt(), ZoneOffset.UTC))
                .status(ShowroomReleaseConstants.RELEASE_STATUS_PUBLISHED)
                .binaryContent(asset.binaryContent())
                .build();
    }
}
