package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowroomReleaseManifestApiTest extends AbstractShowroomReleaseDbTest {

    @Test
    void shouldExposeFullManifestContract() throws Exception {
        ShowroomMaterializedRelease release = publishReleaseFixture();

        var response = scopedReleaseController.getManifest(DEFAULT_SITE_KEY, DEFAULT_STAGE,
                release.releaseId(), new HttpHeaders());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("\"" + release.manifestHash() + "\"", response.getHeaders().getETag());
        assertTrue(String.valueOf(response.getHeaders().getCacheControl()).contains("immutable"));
        Map<String, Object> body = JsonUtils.parseObject(response.getBody(), Map.class);
        assertEquals(release.releaseId(), body.get("releaseId"));
        assertEquals(ShowroomReleaseConstants.SCHEMA_VERSION, body.get("schemaVersion"));
        assertEquals(ShowroomReleaseConstants.DOCUMENT_ID_WEBSITE_INDEX, body.get("rootDocumentId"));
        assertEquals(release.manifestHash(), body.get("manifestHash"));
        assertEquals(2, castList(body.get("documents")).size());
        assertEquals(9, castList(body.get("assets")).size());
    }

    @Test
    void legacyManifestPathShouldRequireSiteSelector() throws Exception {
        ShowroomMaterializedRelease release = publishReleaseFixture();

        var response = releaseController.getManifest(release.releaseId(), new HttpHeaders());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("SHOWROOM_SITE_SELECTOR_REQUIRED"));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object value) {
        return (List<Map<String, Object>>) value;
    }
}
