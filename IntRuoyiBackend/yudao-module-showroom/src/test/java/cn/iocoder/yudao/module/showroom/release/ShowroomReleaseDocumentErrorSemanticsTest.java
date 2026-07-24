package cn.iocoder.yudao.module.showroom.release;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShowroomReleaseDocumentErrorSemanticsTest extends AbstractShowroomReleaseDbTest {

    @Test
    void shouldReturnExplicitNotFoundAndBrokenErrorsForDocuments() throws Exception {
        ShowroomMaterializedRelease release = publishReleaseFixture();
        Long productId = release.sourceSnapshot().productsById().keySet().iterator().next();
        String documentId = "product-detail-" + productId;

        ShowroomReleaseApiException missing = assertThrows(ShowroomReleaseApiException.class,
                () -> manifestQueryService.queryDocumentJson(defaultReleaseScope(), release.releaseId(),
                        "product-detail-404"));
        assertEquals(HttpStatus.NOT_FOUND, missing.getStatus());
        assertEquals("SHOWROOM_DOCUMENT_NOT_FOUND", missing.getCode());

        var document = releaseDocumentMapper.selectByReleaseIdAndDocumentId(release.releaseId(), documentId);
        document.setPayloadJson("");
        releaseDocumentMapper.updateById(document);

        ShowroomReleaseApiException broken = assertThrows(ShowroomReleaseApiException.class,
                () -> manifestQueryService.queryDocumentJson(defaultReleaseScope(), release.releaseId(), documentId));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, broken.getStatus());
        assertEquals("SHOWROOM_RELEASE_BROKEN", broken.getCode());
    }
}
