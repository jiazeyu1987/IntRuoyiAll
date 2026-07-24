package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseAssetDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseAssetRefDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseDocumentDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleasePointerDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseAssetMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseAssetRefMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseDocumentMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleasePointerMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseTombstoneMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ShowroomReleaseManifestQueryService {

    private final ShowroomReleasePointerMapper pointerMapper;
    private final ShowroomReleaseMapper releaseMapper;
    private final ShowroomReleaseDocumentMapper documentMapper;
    private final ShowroomReleaseAssetMapper assetMapper;
    private final ShowroomReleaseAssetRefMapper assetRefMapper;
    private final ShowroomReleaseTombstoneMapper tombstoneMapper;
    private final ShowroomPublicReleaseScopeResolver scopeResolver;

    ShowroomReleaseManifestQueryService(ShowroomReleasePointerMapper pointerMapper,
                                        ShowroomReleaseMapper releaseMapper,
                                        ShowroomReleaseDocumentMapper documentMapper,
                                        ShowroomReleaseAssetMapper assetMapper,
                                        ShowroomReleaseAssetRefMapper assetRefMapper,
                                        ShowroomReleaseTombstoneMapper tombstoneMapper) {
        this(pointerMapper, releaseMapper, documentMapper, assetMapper, assetRefMapper, tombstoneMapper, null);
    }

    @Autowired
    public ShowroomReleaseManifestQueryService(ShowroomReleasePointerMapper pointerMapper,
                                               ShowroomReleaseMapper releaseMapper,
                                               ShowroomReleaseDocumentMapper documentMapper,
                                               ShowroomReleaseAssetMapper assetMapper,
                                               ShowroomReleaseAssetRefMapper assetRefMapper,
                                               ShowroomReleaseTombstoneMapper tombstoneMapper,
                                               ShowroomPublicReleaseScopeResolver scopeResolver) {
        this.pointerMapper = pointerMapper;
        this.releaseMapper = releaseMapper;
        this.documentMapper = documentMapper;
        this.assetMapper = assetMapper;
        this.assetRefMapper = assetRefMapper;
        this.tombstoneMapper = tombstoneMapper;
        this.scopeResolver = scopeResolver;
    }

    public ShowroomReleaseCurrentView queryCurrent() {
        throw ShowroomPublicReleaseScopeResolver.siteSelectorRequired();
    }

    public ResponseEntity<String> getCurrentResponse(HttpHeaders headers) {
        return ShowroomReleaseHttpSupport.error(ShowroomPublicReleaseScopeResolver.siteSelectorRequired());
    }

    public ResponseEntity<String> getCurrentResponse(String siteKey, String stage, HttpHeaders headers) {
        try {
            ShowroomReleaseCurrentView current = queryCurrent(siteKey, stage);
            CacheControl cacheControl = CacheControl.noStore();
            return ShowroomReleaseHttpSupport.jsonOk(JsonUtils.toJsonString(current.payload()), current.etag(),
                    current.publishedAt(), cacheControl);
        } catch (ShowroomReleaseApiException exception) {
            return ShowroomReleaseHttpSupport.error(exception);
        }
    }

    public ShowroomReleaseCurrentView queryCurrent(String siteKey, String stage) {
        ShowroomReleaseScope scope = requireScopeResolver().resolve(siteKey, stage);
        return requireScopeResolver().executeInTenant(scope, () -> queryCurrent(scope));
    }

    ShowroomReleaseCurrentView queryCurrent(ShowroomReleaseScope scope) {
        ShowroomReleasePointerDO pointer = pointerMapper.selectByPointerScope(scope.tenantId(), scope.siteKey(),
                scope.stage(), ShowroomReleaseConstants.POINTER_KEY);
        if (pointer == null) {
            throw new ShowroomReleaseApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "SHOWROOM_RELEASE_UNAVAILABLE", "Current release is unavailable.", false,
                    Map.of("siteKey", scope.siteKey(), "stage", scope.stage()));
        }
        ShowroomReleaseDO release = releaseMapper.selectByReleaseScope(scope.tenantId(), scope.siteKey(),
                scope.stage(), pointer.getReleaseId());
        if (release == null || documentMapper.selectByReleaseScopeAndDocumentId(scope.tenantId(), scope.siteKey(),
                scope.stage(), pointer.getReleaseId(), release.getRootDocumentId()) == null) {
            throw new ShowroomReleaseApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "SHOWROOM_RELEASE_BROKEN", "Current release metadata is unreadable.", false,
                    Map.of("releaseId", pointer.getReleaseId(), "siteKey", scope.siteKey(), "stage", scope.stage()));
        }
        ShowroomReleaseCurrentPayload payload = new ShowroomReleaseCurrentPayload(
                release.getReleaseId(),
                release.getSchemaVersion(),
                release.getManifestHash(),
                DateTimeFormatter.ISO_INSTANT.format(release.getPublishedAt().toInstant(ZoneOffset.UTC)),
                release.getRootDocumentId(),
                release.getDocumentCount(),
                release.getAssetCount(),
                release.getInstallBytes()
        );
        return new ShowroomReleaseCurrentView(payload,
                "\"" + scope.siteKey() + ":" + scope.stage() + ":" + release.getReleaseId() + ":"
                        + release.getManifestHash() + "\"",
                release.getPublishedAt().toInstant(ZoneOffset.UTC));
    }

    public String queryManifestJson(String releaseId) {
        throw ShowroomPublicReleaseScopeResolver.siteSelectorRequired();
    }

    public String queryManifestJson(ShowroomReleaseScope scope, String releaseId) {
        return queryManifestView(scope, releaseId).bodyJson();
    }

    public ResponseEntity<String> getManifestResponse(String releaseId, HttpHeaders headers) {
        return ShowroomReleaseHttpSupport.error(ShowroomPublicReleaseScopeResolver.siteSelectorRequired());
    }

    public ResponseEntity<String> getManifestResponse(String siteKey, String stage, String releaseId,
                                                      HttpHeaders headers) {
        try {
            ShowroomPublicReleaseScopeResolver resolver = requireScopeResolver();
            ShowroomReleaseScope scope = resolver.resolve(siteKey, stage);
            return resolver.executeInTenant(scope, () -> {
                ManifestView manifest = queryManifestView(scope, releaseId);
                CacheControl cacheControl = CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable();
                if (ShowroomReleaseHttpSupport.matchesEtag(headers, manifest.etag())
                        || ShowroomReleaseHttpSupport.matchesLastModified(headers, manifest.lastModified())) {
                    return ShowroomReleaseHttpSupport.notModified(manifest.etag(), manifest.lastModified(),
                            cacheControl);
                }
                return ShowroomReleaseHttpSupport.jsonOk(manifest.bodyJson(), manifest.etag(),
                        manifest.lastModified(), cacheControl);
            });
        } catch (ShowroomReleaseApiException exception) {
            return ShowroomReleaseHttpSupport.error(exception);
        }
    }

    public ResponseEntity<String> getDocumentResponse(String releaseId, String documentId, HttpHeaders headers) {
        return ShowroomReleaseHttpSupport.error(ShowroomPublicReleaseScopeResolver.siteSelectorRequired());
    }

    public ResponseEntity<String> getDocumentResponse(String siteKey, String stage, String releaseId,
                                                      String documentId, HttpHeaders headers) {
        try {
            ShowroomPublicReleaseScopeResolver resolver = requireScopeResolver();
            ShowroomReleaseScope scope = resolver.resolve(siteKey, stage);
            return resolver.executeInTenant(scope, () -> {
                DocumentView document = queryDocumentView(scope, releaseId, documentId);
                CacheControl cacheControl = CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable();
                if (ShowroomReleaseHttpSupport.matchesEtag(headers, document.etag())
                        || ShowroomReleaseHttpSupport.matchesLastModified(headers, document.lastModified())) {
                    return ShowroomReleaseHttpSupport.notModified(document.etag(), document.lastModified(),
                            cacheControl);
                }
                return ShowroomReleaseHttpSupport.jsonOk(document.bodyJson(), document.etag(),
                        document.lastModified(), cacheControl);
            });
        } catch (ShowroomReleaseApiException exception) {
            return ShowroomReleaseHttpSupport.error(exception);
        }
    }

    public ResponseEntity<?> getAssetResponse(String assetId, String contentHash, HttpHeaders headers) {
        return ShowroomReleaseHttpSupport.error(ShowroomPublicReleaseScopeResolver.siteSelectorRequired());
    }

    public ResponseEntity<?> getAssetResponse(String siteKey, String stage, String assetId, String contentHash,
                                              HttpHeaders headers) {
        try {
            ShowroomPublicReleaseScopeResolver resolver = requireScopeResolver();
            ShowroomReleaseScope scope = resolver.resolve(siteKey, stage);
            return resolver.executeInTenant(scope, () -> {
                AssetView asset = queryAsset(scope, assetId, contentHash);
                if (ShowroomReleaseHttpSupport.matchesEtag(headers, asset.etag())
                        || ShowroomReleaseHttpSupport.matchesLastModified(headers, asset.lastModified())) {
                    return ShowroomReleaseHttpSupport.notModified(asset.etag(), asset.lastModified(),
                            CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());
                }
                return ShowroomReleaseHttpSupport.binaryOk(asset.body(), asset.contentType(), asset.etag(),
                        asset.lastModified());
            });
        } catch (ShowroomReleaseApiException exception) {
            return ShowroomReleaseHttpSupport.error(exception);
        }
    }

    public ManifestView queryManifestView(String releaseId) {
        throw ShowroomPublicReleaseScopeResolver.siteSelectorRequired();
    }

    public ManifestView queryManifestView(ShowroomReleaseScope scope, String releaseId) {
        if (tombstoneMapper.selectByScopedResource(scope.tenantId(), scope.siteKey(), scope.stage(),
                ShowroomReleaseConstants.RESOURCE_TYPE_RELEASE, releaseId) != null) {
            throw new ShowroomReleaseApiException(HttpStatus.GONE, "SHOWROOM_RELEASE_PURGED",
                    "Release " + releaseId + " has been purged.", false,
                    Map.of("releaseId", releaseId, "siteKey", scope.siteKey(), "stage", scope.stage()));
        }
        ShowroomReleaseDO release = releaseMapper.selectByReleaseScope(scope.tenantId(), scope.siteKey(),
                scope.stage(), releaseId);
        if (release == null) {
            throw new ShowroomReleaseApiException(HttpStatus.NOT_FOUND, "SHOWROOM_RELEASE_NOT_FOUND",
                    "Release " + releaseId + " does not exist.", false,
                    Map.of("releaseId", releaseId, "siteKey", scope.siteKey(), "stage", scope.stage()));
        }
        if (documentMapper.selectByReleaseScopeAndDocumentId(scope.tenantId(), scope.siteKey(), scope.stage(),
                releaseId, release.getRootDocumentId()) == null) {
            throw new ShowroomReleaseApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SHOWROOM_RELEASE_BROKEN",
                    "Release " + releaseId + " manifest is broken.", false,
                    Map.of("releaseId", releaseId, "siteKey", scope.siteKey(), "stage", scope.stage()));
        }
        return buildManifestView(release, documentMapper.selectListByReleaseScope(scope.tenantId(), scope.siteKey(),
                        scope.stage(), releaseId),
                assetRefMapper.selectListByReleaseScope(scope.tenantId(), scope.siteKey(), scope.stage(), releaseId),
                scope);
    }

    public String queryDocumentJson(String releaseId, String documentId) {
        throw ShowroomPublicReleaseScopeResolver.siteSelectorRequired();
    }

    public String queryDocumentJson(ShowroomReleaseScope scope, String releaseId, String documentId) {
        return queryDocumentView(scope, releaseId, documentId).bodyJson();
    }

    public DocumentView queryDocumentView(String releaseId, String documentId) {
        throw ShowroomPublicReleaseScopeResolver.siteSelectorRequired();
    }

    public DocumentView queryDocumentView(ShowroomReleaseScope scope, String releaseId, String documentId) {
        if (tombstoneMapper.selectByScopedResource(scope.tenantId(), scope.siteKey(), scope.stage(),
                ShowroomReleaseConstants.RESOURCE_TYPE_RELEASE, releaseId) != null) {
            throw new ShowroomReleaseApiException(HttpStatus.GONE, "SHOWROOM_RELEASE_PURGED",
                    "Release " + releaseId + " has been purged.", false,
                    Map.of("releaseId", releaseId, "siteKey", scope.siteKey(), "stage", scope.stage()));
        }
        ShowroomReleaseDO release = releaseMapper.selectByReleaseScope(scope.tenantId(), scope.siteKey(),
                scope.stage(), releaseId);
        if (release == null) {
            throw new ShowroomReleaseApiException(HttpStatus.NOT_FOUND, "SHOWROOM_RELEASE_NOT_FOUND",
                    "Release " + releaseId + " does not exist.", false,
                    Map.of("releaseId", releaseId, "siteKey", scope.siteKey(), "stage", scope.stage()));
        }
        ShowroomReleaseDocumentDO document = documentMapper.selectByReleaseScopeAndDocumentId(scope.tenantId(),
                scope.siteKey(), scope.stage(), releaseId, documentId);
        return buildDocumentView(releaseId, documentId, document, Map.of("releaseId", releaseId,
                "documentId", documentId, "siteKey", scope.siteKey(), "stage", scope.stage()));
    }

    public AssetView queryAsset(String assetId, String contentHash) {
        throw ShowroomPublicReleaseScopeResolver.siteSelectorRequired();
    }

    public AssetView queryAsset(ShowroomReleaseScope scope, String assetId, String contentHash) {
        String resourceKey = assetId + ":" + contentHash;
        Map<String, Object> details = Map.of("assetId", assetId, "contentHash", contentHash,
                "siteKey", scope.siteKey(), "stage", scope.stage(), "tenantId", scope.tenantId());
        if (tombstoneMapper.selectByScopedResource(scope.tenantId(), scope.siteKey(), scope.stage(),
                ShowroomReleaseConstants.RESOURCE_TYPE_ASSET, resourceKey) != null) {
            throw new ShowroomReleaseApiException(HttpStatus.GONE, "SHOWROOM_ASSET_PURGED",
                    "Asset has been purged.", false, details);
        }
        ShowroomReleaseAssetDO asset = assetMapper.selectByScopeAssetIdAndContentHash(scope.tenantId(),
                scope.siteKey(), scope.stage(), assetId, contentHash);
        if (asset == null) {
            throw new ShowroomReleaseApiException(HttpStatus.NOT_FOUND, "SHOWROOM_ASSET_NOT_FOUND",
                    "Asset does not exist.", false, details);
        }
        if (asset.getBinaryContent() == null || asset.getBinaryContent().length == 0
                || !ShowroomReleaseHashSupport.sha256Hex(asset.getBinaryContent()).equals(contentHash)) {
            throw new ShowroomReleaseApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SHOWROOM_ASSET_BROKEN",
                    "Asset is unreadable.", false, details);
        }
        return new AssetView(asset.getBinaryContent(), asset.getMimeType(),
                "\"" + asset.getContentHash() + "\"", asset.getMaterializedAt().toInstant(ZoneOffset.UTC));
    }

    private ManifestView buildManifestView(ShowroomReleaseDO release, List<ShowroomReleaseDocumentDO> documents,
                                           List<ShowroomReleaseAssetRefDO> assetRefs, ShowroomReleaseScope scope) {
        List<Map<String, Object>> documentItems = documents.stream()
                .map(document -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("documentId", document.getDocumentId());
                    map.put("kind", document.getKind());
                    if (ShowroomReleaseConstants.DOCUMENT_KIND_AWARD_DETAIL.equals(document.getKind())
                            && document.getProductId() != null) {
                        map.put("awardId", String.valueOf(document.getProductId()));
                    } else if (document.getProductId() != null) {
                        map.put("productId", String.valueOf(document.getProductId()));
                    }
                    map.put("contentHash", document.getContentHash());
                    map.put("bytes", document.getBytes());
                    return map;
                })
                .toList();
        Map<String, List<String>> referencedBy = new LinkedHashMap<>();
        Map<String, ShowroomReleaseAssetDO> assetsByKey = new LinkedHashMap<>();
        for (ShowroomReleaseAssetRefDO ref : assetRefs) {
            String key = assetKey(ref.getAssetId(), ref.getContentHash());
            List<String> documentIds = referencedBy.computeIfAbsent(key, ignored -> new ArrayList<>());
            if (!documentIds.contains(ref.getDocumentId())) {
                documentIds.add(ref.getDocumentId());
            }
            if (!assetsByKey.containsKey(key)) {
                ShowroomReleaseAssetDO asset = selectManifestAsset(scope, ref);
                if (asset == null) {
                    Map<String, Object> details = new LinkedHashMap<>();
                    details.put("releaseId", release.getReleaseId());
                    details.put("assetId", ref.getAssetId());
                    details.put("contentHash", ref.getContentHash());
                    if (scope != null) {
                        details.put("siteKey", scope.siteKey());
                        details.put("stage", scope.stage());
                    }
                    throw new ShowroomReleaseApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "SHOWROOM_RELEASE_BROKEN", "Release " + release.getReleaseId()
                            + " manifest asset is missing.", false, details);
                }
                assetsByKey.put(key, asset);
            }
        }
        List<Map<String, Object>> assetItems = assetsByKey.entrySet().stream()
                .map(entry -> manifestAssetMap(entry.getValue(), referencedBy.getOrDefault(entry.getKey(), List.of())))
                .toList();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("releaseId", release.getReleaseId());
        payload.put("schemaVersion", release.getSchemaVersion());
        payload.put("publishedAt", DateTimeFormatter.ISO_INSTANT.format(release.getPublishedAt().toInstant(ZoneOffset.UTC)));
        payload.put("rootDocumentId", release.getRootDocumentId());
        payload.put("documents", documentItems);
        payload.put("assets", assetItems);
        payload.put("manifestHash", release.getManifestHash());
        return new ManifestView(JsonUtils.toJsonString(payload), "\"" + release.getManifestHash() + "\"",
                release.getPublishedAt().toInstant(ZoneOffset.UTC));
    }

    private ShowroomReleaseAssetDO selectManifestAsset(ShowroomReleaseScope scope, ShowroomReleaseAssetRefDO ref) {
        if (scope == null) {
            throw ShowroomPublicReleaseScopeResolver.siteSelectorRequired();
        }
        return assetMapper.selectManifestAssetByScopeAssetIdAndContentHash(scope.tenantId(), scope.siteKey(),
                scope.stage(), ref.getAssetId(), ref.getContentHash());
    }

    private DocumentView buildDocumentView(String releaseId, String documentId, ShowroomReleaseDocumentDO document,
                                           Map<String, Object> details) {
        if (document == null) {
            throw new ShowroomReleaseApiException(HttpStatus.NOT_FOUND, "SHOWROOM_DOCUMENT_NOT_FOUND",
                    "Document " + documentId + " does not exist.", false, details);
        }
        if (document.getPayloadJson() == null || document.getPayloadJson().isBlank()) {
            throw new ShowroomReleaseApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SHOWROOM_RELEASE_BROKEN",
                    "Document " + documentId + " is unreadable.", false, details);
        }
        return new DocumentView(document.getPayloadJson(), "\"" + document.getContentHash() + "\"",
                document.getMaterializedAt().toInstant(ZoneOffset.UTC));
    }

    private Map<String, Object> manifestAssetMap(ShowroomReleaseAssetDO asset, List<String> referencedBy) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("assetId", asset.getAssetId());
        map.put("contentHash", asset.getContentHash());
        map.put("assetType", asset.getAssetType());
        map.put("mimeType", asset.getMimeType());
        map.put("bytes", asset.getBytes());
        map.put("referencedBy", referencedBy);
        return map;
    }

    private ShowroomPublicReleaseScopeResolver requireScopeResolver() {
        if (scopeResolver == null) {
            throw new ShowroomReleaseApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "SHOWROOM_PUBLIC_SITE_BINDING_UNAVAILABLE",
                    "Public site binding mapper is required for scoped release lookup.", false, Map.of());
        }
        return scopeResolver;
    }

    private static String assetKey(String assetId, String contentHash) {
        return assetId + "\n" + contentHash;
    }

    record AssetView(byte[] body, String contentType, String etag, Instant lastModified) {
    }

    record ManifestView(String bodyJson, String etag, Instant lastModified) {
    }

    record DocumentView(String bodyJson, String etag, Instant lastModified) {
    }
}
