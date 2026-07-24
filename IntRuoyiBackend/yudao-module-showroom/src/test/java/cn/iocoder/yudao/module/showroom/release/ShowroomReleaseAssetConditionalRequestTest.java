package cn.iocoder.yudao.module.showroom.release;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShowroomReleaseAssetConditionalRequestTest extends AbstractShowroomReleaseDbTest {

    @Test
    void shouldReturnNotModifiedWhenAssetValidatorsMatch() throws Exception {
        ShowroomMaterializedRelease release = publishReleaseFixture();
        ShowroomMaterializedRelease.MaterializedAsset asset = release.assets().getFirst();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.IF_NONE_MATCH, "\"" + asset.contentHash() + "\"");

        var response = scopedAssetController.getAsset(DEFAULT_SITE_KEY, DEFAULT_STAGE,
                asset.assetId(), asset.contentHash(), headers);

        assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
    }
}
