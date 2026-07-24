package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomPublicSiteBindingDO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowroomReleaseCurrentApiTest extends AbstractShowroomReleaseDbTest {

    @Test
    void shouldReturnServiceUnavailableErrorEnvelopeWhenCurrentReleaseIsMissing() {
        bindDefaultSiteStage();
        var response = manifestQueryService.getCurrentResponse(DEFAULT_SITE_KEY, DEFAULT_STAGE, new HttpHeaders());

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertTrue(response.getBody().contains("SHOWROOM_RELEASE_UNAVAILABLE"));
        assertTrue(response.getBody().contains("Current release is unavailable."));
    }

    @Test
    void shouldExposeCurrentReleaseProbeAfterPublish() throws Exception {
        var release = publishReleaseFixture();

        var response = manifestQueryService.getCurrentResponse(DEFAULT_SITE_KEY, DEFAULT_STAGE, new HttpHeaders());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertTrue(response.getBody().contains("\"releaseId\":\"" + release.releaseId() + "\""));
        assertTrue(response.getBody().contains("\"manifestHash\":\"" + release.manifestHash() + "\""));
    }

    @Test
    void shouldExposeScopedManifestDocumentAndAssetAfterPublish() throws Exception {
        var release = publishReleaseFixture();
        var asset = release.assets().getFirst();

        var manifest = scopedReleaseController.getManifest(DEFAULT_SITE_KEY, DEFAULT_STAGE,
                release.releaseId(), new HttpHeaders());
        assertEquals(HttpStatus.OK, manifest.getStatusCode());
        assertTrue(manifest.getBody().contains("\"releaseId\":\"" + release.releaseId() + "\""));
        assertTrue(manifest.getBody().contains("\"manifestHash\":\"" + release.manifestHash() + "\""));

        var document = scopedReleaseController.getDocument(DEFAULT_SITE_KEY, DEFAULT_STAGE,
                release.releaseId(), release.rootDocumentId(), new HttpHeaders());
        assertEquals(HttpStatus.OK, document.getStatusCode());
        assertNotNull(document.getBody());

        var assetResponse = scopedAssetController.getAsset(DEFAULT_SITE_KEY, DEFAULT_STAGE,
                asset.assetId(), asset.contentHash(), new HttpHeaders());
        assertEquals(HttpStatus.OK, assetResponse.getStatusCode());
        byte[] body = assertInstanceOf(byte[].class, assetResponse.getBody());
        assertArrayEquals(asset.binaryContent(), body);
    }

    @Test
    void shouldNotReadScopedReleaseResourcesFromDifferentStage() throws Exception {
        var release = publishReleaseFixture();
        var asset = release.assets().getFirst();
        bindSiteStage("PROD");

        var manifest = scopedReleaseController.getManifest(DEFAULT_SITE_KEY, "PROD",
                release.releaseId(), new HttpHeaders());
        assertEquals(HttpStatus.NOT_FOUND, manifest.getStatusCode());
        assertEquals("SHOWROOM_RELEASE_NOT_FOUND", errorCode(manifest.getBody()));

        var document = scopedReleaseController.getDocument(DEFAULT_SITE_KEY, "PROD",
                release.releaseId(), release.rootDocumentId(), new HttpHeaders());
        assertEquals(HttpStatus.NOT_FOUND, document.getStatusCode());
        assertEquals("SHOWROOM_RELEASE_NOT_FOUND", errorCode(document.getBody()));

        var assetResponse = scopedAssetController.getAsset(DEFAULT_SITE_KEY, "PROD",
                asset.assetId(), asset.contentHash(), new HttpHeaders());
        assertEquals(HttpStatus.NOT_FOUND, assetResponse.getStatusCode());
        assertEquals("SHOWROOM_ASSET_NOT_FOUND", errorCode(String.valueOf(assetResponse.getBody())));
    }

    private void bindSiteStage(String stage) {
        if (siteBindingMapper.selectEnabledBySiteStage(DEFAULT_SITE_KEY, stage) != null) {
            return;
        }
        siteBindingMapper.insert(ShowroomPublicSiteBindingDO.builder()
                .siteKey(DEFAULT_SITE_KEY)
                .stage(stage)
                .tenantId(DEFAULT_TENANT_ID)
                .displayName("Yingtai " + stage)
                .enabled(true)
                .build());
    }

    private static String errorCode(String bodyValue) {
        Map<String, Object> body = JsonUtils.parseObject(bodyValue, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) body.get("error");
        return String.valueOf(error.get("code"));
    }
}
