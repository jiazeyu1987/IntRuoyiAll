package cn.iocoder.yudao.module.showroom.release;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowroomReleaseAssetApiTest extends AbstractShowroomReleaseDbTest {

    @Test
    void shouldExposeImmutableAssetBinaryContract() throws Exception {
        ShowroomMaterializedRelease release = publishReleaseFixture();
        ShowroomMaterializedRelease.MaterializedAsset asset = release.assets().getFirst();

        var response = scopedAssetController.getAsset(DEFAULT_SITE_KEY, DEFAULT_STAGE,
                asset.assetId(), asset.contentHash(), new HttpHeaders());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(asset.mimeType(), response.getHeaders().getContentType().toString());
        assertEquals(asset.bytes(), response.getHeaders().getContentLength());
        assertEquals("\"" + asset.contentHash() + "\"", response.getHeaders().getETag());
        assertTrue(String.valueOf(response.getHeaders().getCacheControl()).contains("immutable"));
    }

    @Test
    void legacyAssetPathShouldRequireSiteSelector() throws Exception {
        ShowroomMaterializedRelease release = publishReleaseFixture();
        ShowroomMaterializedRelease.MaterializedAsset asset = release.assets().getFirst();

        var response = assetController.getAsset(asset.assetId(), asset.contentHash(), new HttpHeaders());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
