package cn.iocoder.yudao.module.showroom.release;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class ShowroomReleaseGoneSemanticsTest extends AbstractShowroomReleaseDbTest {

    @Test
    void shouldPreferGoneOverNotModifiedForPurgedReleaseDocumentAndAsset() throws Exception {
        seedPublishedFixture();
        ShowroomMaterializedRelease release1 = publishReleaseFixture(Instant.parse("2026-05-23T10:15:00Z"));
        ShowroomMaterializedRelease.MaterializedAsset purgedAsset = release1.assets().stream()
                .filter(asset -> asset.assetId().contains("product-") && asset.assetId().contains("-preview"))
                .findFirst()
                .orElseThrow();
        HttpHeaders manifestHeaders = new HttpHeaders();
        manifestHeaders.add(HttpHeaders.IF_NONE_MATCH, "\"" + release1.manifestHash() + "\"");
        String detailDocumentId = release1.documents().stream()
                .filter(document -> ShowroomReleaseConstants.DOCUMENT_KIND_PRODUCT_DETAIL.equals(document.kind()))
                .findFirst()
                .orElseThrow()
                .documentId();
        String detailHash = release1.documents().stream()
                .filter(document -> detailDocumentId.equals(document.documentId()))
                .findFirst()
                .orElseThrow()
                .contentHash();
        HttpHeaders documentHeaders = new HttpHeaders();
        documentHeaders.add(HttpHeaders.IF_NONE_MATCH, "\"" + detailHash + "\"");
        HttpHeaders assetHeaders = new HttpHeaders();
        assetHeaders.add(HttpHeaders.IF_NONE_MATCH, "\"" + purgedAsset.contentHash() + "\"");

        mockFile(103L, 11L, "showroom/product/产品封面图.png", "image/png", "product-preview-v2");
        publishReleaseFixture(Instant.parse("2026-05-23T10:16:00Z"));
        mockFile(103L, 11L, "showroom/product/产品封面图.png", "image/png", "product-preview-v3");
        publishReleaseFixture(Instant.parse("2026-05-23T10:17:00Z"));
        mockFile(103L, 11L, "showroom/product/产品封面图.png", "image/png", "product-preview-v4");
        publishReleaseFixture(Instant.parse("2026-05-23T10:18:00Z"));

        if (tombstoneMapper.selectByScopedResource(DEFAULT_TENANT_ID, DEFAULT_SITE_KEY, DEFAULT_STAGE,
                "release", release1.releaseId()) == null) {
            fail("missing release tombstone for " + release1.releaseId());
        }
        if (tombstoneMapper.selectByScopedResource(DEFAULT_TENANT_ID, DEFAULT_SITE_KEY, DEFAULT_STAGE, "asset",
                purgedAsset.assetId() + ":" + purgedAsset.contentHash()) == null) {
            fail("missing asset tombstone for " + purgedAsset.assetId() + ":" + purgedAsset.contentHash());
        }

        var manifestResponse = scopedReleaseController.getManifest(DEFAULT_SITE_KEY, DEFAULT_STAGE,
                release1.releaseId(), manifestHeaders);
        var documentResponse = scopedReleaseController.getDocument(DEFAULT_SITE_KEY, DEFAULT_STAGE,
                release1.releaseId(), detailDocumentId, documentHeaders);
        var assetResponse = scopedAssetController.getAsset(DEFAULT_SITE_KEY, DEFAULT_STAGE,
                purgedAsset.assetId(), purgedAsset.contentHash(), assetHeaders);

        assertEquals(HttpStatus.GONE, manifestResponse.getStatusCode());
        assertEquals(HttpStatus.GONE, documentResponse.getStatusCode());
        assertEquals(HttpStatus.GONE, assetResponse.getStatusCode());
    }
}
