package cn.iocoder.yudao.module.showroom.release;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShowroomReleaseManifestConditionalRequestTest extends AbstractShowroomReleaseDbTest {

    @Test
    void shouldReturnNotModifiedWhenManifestValidatorsMatch() throws Exception {
        ShowroomMaterializedRelease release = publishReleaseFixture();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.IF_NONE_MATCH, "\"" + release.manifestHash() + "\"");

        var response = scopedReleaseController.getManifest(DEFAULT_SITE_KEY, DEFAULT_STAGE,
                release.releaseId(), headers);

        assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
    }
}
