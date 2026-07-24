package cn.iocoder.yudao.module.showroom.release;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ShowroomReleasePublisherService {

    private final ShowroomReleaseAssembler assembler;
    private final ShowroomReleaseRegistryService registryService;
    private final ShowroomReleaseManifestQueryService manifestQueryService;
    private final ShowroomLegacyWebsiteConfigProjector legacyProjector;
    private final ShowroomReleasePurgeService purgeService;
    private final ShowroomPublicReleaseScopeResolver scopeResolver;

    public ShowroomReleasePublisherService(ShowroomReleaseAssembler assembler,
                                           ShowroomReleaseRegistryService registryService,
                                           ShowroomReleaseManifestQueryService manifestQueryService,
                                           ShowroomLegacyWebsiteConfigProjector legacyProjector,
                                           ShowroomReleasePurgeService purgeService,
                                           ShowroomPublicReleaseScopeResolver scopeResolver) {
        this.assembler = assembler;
        this.registryService = registryService;
        this.manifestQueryService = manifestQueryService;
        this.legacyProjector = legacyProjector;
        this.purgeService = purgeService;
        this.scopeResolver = scopeResolver;
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomMaterializedRelease publishRelease(Long operatorUserId, Instant publishedAt) {
        throw ShowroomPublicReleaseScopeResolver.siteSelectorRequired();
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomMaterializedRelease publishRelease(Long operatorUserId, Instant publishedAt,
                                                      String siteKey, String stage) {
        ShowroomReleaseScope scope = scopeResolver.resolvePublishScope(siteKey, stage);
        return scopeResolver.executeInTenant(scope, () -> {
            ShowroomReleaseSourceSnapshot snapshot = assembler.resolveSourceSnapshot();
            ShowroomMaterializedRelease release = assembler.materializeRelease(scope, snapshot, publishedAt);
            registryService.persistRelease(scope, release);
            registryService.switchCurrentRelease(scope, release.releaseId());
            selfVerify(scope, release.releaseId(), release.manifestHash(), release.rootDocumentId());
            purgeService.purgeExpiredReleases(scope);
            return release;
        });
    }

    private void selfVerify(ShowroomReleaseScope scope, String releaseId, String manifestHash, String rootDocumentId) {
        ShowroomReleaseCurrentView current = manifestQueryService.queryCurrent(scope);
        if (!releaseId.equals(current.payload().releaseId())) {
            throw new IllegalStateException("SHOWROOM_RELEASE_BROKEN: scoped current self verification failed");
        }
        String manifestJson = manifestQueryService.queryManifestJson(scope, releaseId);
        if (!ShowroomReleaseHashSupport.sha256Hex(manifestJson).equals(manifestHash)
                && !manifestJson.contains(manifestHash)) {
            throw new IllegalStateException("SHOWROOM_RELEASE_BROKEN: manifest self verification failed");
        }
        if (manifestQueryService.queryDocumentJson(scope, releaseId, rootDocumentId) == null) {
            throw new IllegalStateException("SHOWROOM_RELEASE_BROKEN: root document self verification failed");
        }
        legacyProjector.queryCurrentLegacyProjection(scope);
    }
}
