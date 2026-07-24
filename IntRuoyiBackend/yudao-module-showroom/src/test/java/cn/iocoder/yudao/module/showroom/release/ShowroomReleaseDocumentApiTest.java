package cn.iocoder.yudao.module.showroom.release;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowroomReleaseDocumentApiTest extends AbstractShowroomReleaseDbTest {

    @Test
    void shouldExposeWebsiteIndexAndProductDetailDocuments() throws Exception {
        ShowroomMaterializedRelease release = publishReleaseFixture();
        String productDetailDocumentId = "product-detail-" + release.sourceSnapshot().productsById().keySet().iterator().next();

        var websiteIndex = scopedReleaseController.getDocument(DEFAULT_SITE_KEY, DEFAULT_STAGE, release.releaseId(),
                ShowroomReleaseConstants.DOCUMENT_ID_WEBSITE_INDEX, new HttpHeaders());
        var productDetail = scopedReleaseController.getDocument(DEFAULT_SITE_KEY, DEFAULT_STAGE,
                release.releaseId(), productDetailDocumentId, new HttpHeaders());

        assertEquals(HttpStatus.OK, websiteIndex.getStatusCode());
        assertEquals(HttpStatus.OK, productDetail.getStatusCode());
        assertTrue(String.valueOf(websiteIndex.getHeaders().getCacheControl()).contains("immutable"));
        assertTrue(String.valueOf(productDetail.getHeaders().getCacheControl()).contains("immutable"));
    }

    @Test
    void legacyDocumentPathShouldRequireSiteSelector() throws Exception {
        ShowroomMaterializedRelease release = publishReleaseFixture();

        var response = releaseController.getDocument(release.releaseId(),
                ShowroomReleaseConstants.DOCUMENT_ID_WEBSITE_INDEX, new HttpHeaders());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("SHOWROOM_SITE_SELECTOR_REQUIRED"));
    }
}
