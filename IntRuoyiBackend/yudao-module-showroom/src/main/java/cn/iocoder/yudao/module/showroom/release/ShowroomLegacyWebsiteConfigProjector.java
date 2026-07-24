package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.showroom.controller.display.ShowroomDisplayController;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseLegacyProjectionDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleasePointerDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseDocumentMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseLegacyProjectionMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleasePointerMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseTombstoneMapper;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;

@Service
public class ShowroomLegacyWebsiteConfigProjector {

    private final ShowroomReleasePointerMapper pointerMapper;
    private final ShowroomReleaseLegacyProjectionMapper legacyProjectionMapper;
    private final ShowroomReleaseDocumentMapper documentMapper;
    private final ShowroomReleaseTombstoneMapper tombstoneMapper;

    public ShowroomLegacyWebsiteConfigProjector(ShowroomReleasePointerMapper pointerMapper,
                                                ShowroomReleaseLegacyProjectionMapper legacyProjectionMapper,
                                                ShowroomReleaseDocumentMapper documentMapper,
                                                ShowroomReleaseTombstoneMapper tombstoneMapper) {
        this.pointerMapper = pointerMapper;
        this.legacyProjectionMapper = legacyProjectionMapper;
        this.documentMapper = documentMapper;
        this.tombstoneMapper = tombstoneMapper;
    }

    public LegacyProjectionView queryCurrentLegacyProjection() {
        throw ShowroomPublicReleaseScopeResolver.siteSelectorRequired();
    }

    public LegacyProjectionView queryCurrentLegacyProjection(ShowroomReleaseScope scope) {
        ShowroomReleasePointerDO pointer = pointerMapper.selectByPointerScope(scope.tenantId(), scope.siteKey(),
                scope.stage(), ShowroomReleaseConstants.POINTER_KEY);
        if (pointer == null) {
            throw new ShowroomReleaseApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "SHOWROOM_RELEASE_UNAVAILABLE", "Current release is unavailable.", false,
                    Map.of("siteKey", scope.siteKey(), "stage", scope.stage()));
        }
        if (tombstoneMapper.selectByScopedResource(scope.tenantId(), scope.siteKey(), scope.stage(),
                ShowroomReleaseConstants.RESOURCE_TYPE_RELEASE, pointer.getReleaseId()) != null) {
            throw new ShowroomReleaseApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "SHOWROOM_RELEASE_BROKEN", "Current release is broken.", false,
                    Map.of("releaseId", pointer.getReleaseId(), "siteKey", scope.siteKey(), "stage", scope.stage()));
        }
        if (documentMapper.selectByReleaseScopeAndDocumentId(scope.tenantId(), scope.siteKey(), scope.stage(),
                pointer.getReleaseId(),
                ShowroomReleaseConstants.DOCUMENT_ID_WEBSITE_INDEX) == null) {
            throw new ShowroomReleaseApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "SHOWROOM_RELEASE_BROKEN", "Current release root document is unreadable.", false,
                    Map.of("releaseId", pointer.getReleaseId(), "siteKey", scope.siteKey(), "stage", scope.stage()));
        }
        ShowroomReleaseLegacyProjectionDO projection = legacyProjectionMapper.selectByReleaseScope(scope.tenantId(),
                scope.siteKey(), scope.stage(), pointer.getReleaseId());
        if (projection == null || !ShowroomReleaseConstants.LEGACY_STATUS_READY.equals(projection.getStatus())) {
            throw new ShowroomReleaseApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "SHOWROOM_RELEASE_BROKEN", "Legacy website-config projection is broken.", false,
                    Map.of("releaseId", pointer.getReleaseId(), "siteKey", scope.siteKey(), "stage", scope.stage()));
        }
        return new LegacyProjectionView(projection.getPayloadJson(),
                "\"" + scope.siteKey() + ":" + scope.stage() + ":" + pointer.getReleaseId() + ":"
                        + pointer.getManifestHash() + ":legacy-website-config\"",
                projection.getPublishedAt().toInstant(java.time.ZoneOffset.UTC));
    }

    public CommonResult<ShowroomDisplayController.WebsiteConfigPayload> projectCurrentPayload() {
        LegacyProjectionView view = queryCurrentLegacyProjection();
        return projectPayload(view);
    }

    public CommonResult<ShowroomDisplayController.WebsiteConfigPayload> projectCurrentPayload(
            ShowroomReleaseScope scope) {
        LegacyProjectionView view = queryCurrentLegacyProjection(scope);
        return projectPayload(view);
    }

    private CommonResult<ShowroomDisplayController.WebsiteConfigPayload> projectPayload(LegacyProjectionView view) {
        Map<String, Object> wrapper = JsonUtils.parseObject(view.payloadJson(), Map.class);
        Map<String, Object> data = castMap(wrapper.get("data"));
        return CommonResult.success(JsonUtils.parseObject(JsonUtils.toJsonString(data),
                ShowroomDisplayController.WebsiteConfigPayload.class));
    }

    public ResponseEntity<String> getCurrentResponse(HttpHeaders headers) {
        LegacyProjectionView view = queryCurrentLegacyProjection();
        return responseFromView(headers, view);
    }

    public ResponseEntity<String> getCurrentResponse(ShowroomReleaseScope scope, HttpHeaders headers) {
        LegacyProjectionView view = queryCurrentLegacyProjection(scope);
        return responseFromView(headers, view);
    }

    private ResponseEntity<String> responseFromView(HttpHeaders headers, LegacyProjectionView view) {
        CacheControl cacheControl = CacheControl.maxAge(15, TimeUnit.SECONDS).cachePublic().mustRevalidate();
        if (ShowroomReleaseHttpSupport.matchesEtag(headers, view.etag())
                || ShowroomReleaseHttpSupport.matchesLastModified(headers, view.publishedAt())) {
            return ShowroomReleaseHttpSupport.notModified(view.etag(), view.publishedAt(), cacheControl);
        }
        return ShowroomReleaseHttpSupport.jsonOk(view.payloadJson(), view.etag(), view.publishedAt(), cacheControl);
    }

    record LegacyProjectionView(String payloadJson, String etag, Instant publishedAt) {
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return value == null ? Map.of() : (Map<String, Object>) value;
    }
}
