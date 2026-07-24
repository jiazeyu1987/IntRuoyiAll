package cn.iocoder.yudao.module.showroom.release;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShowroomReleasePurgeServiceTest extends AbstractShowroomReleaseDbTest {

    @Test
    void shouldPurgeOnlyOldestReleaseOutsideRetentionWindow() throws Exception {
        seedPublishedFixture();
        ShowroomMaterializedRelease release1 = publishReleaseFixture(Instant.parse("2026-05-23T10:15:00Z"));
        mockFile(103L, 11L, "showroom/product/preview.png", "image/png", "product-preview-v2");
        ShowroomMaterializedRelease release2 = publishReleaseFixture(Instant.parse("2026-05-23T10:16:00Z"));
        ShowroomMaterializedRelease release3 = publishReleaseFixture(Instant.parse("2026-05-23T10:17:00Z"));
        ShowroomMaterializedRelease release4 = publishReleaseFixture(Instant.parse("2026-05-23T10:18:00Z"));

        assertEquals(3, releaseMapper.selectPublishedByScopeOrderByPublishedAtDesc(DEFAULT_TENANT_ID,
                DEFAULT_SITE_KEY, DEFAULT_STAGE).size());
        assertNotNull(tombstoneMapper.selectByScopedResource(DEFAULT_TENANT_ID, DEFAULT_SITE_KEY, DEFAULT_STAGE,
                "release", release1.releaseId()));
        assertNotNull(tombstoneMapper.selectByScopedResource(DEFAULT_TENANT_ID, DEFAULT_SITE_KEY, DEFAULT_STAGE,
                "document",
                release1.releaseId() + ":" + ShowroomReleaseConstants.DOCUMENT_ID_WEBSITE_INDEX));
        assertNull(tombstoneMapper.selectByScopedResource(DEFAULT_TENANT_ID, DEFAULT_SITE_KEY, DEFAULT_STAGE,
                "release", release2.releaseId()));
        assertNull(tombstoneMapper.selectByScopedResource(DEFAULT_TENANT_ID, DEFAULT_SITE_KEY, DEFAULT_STAGE,
                "release", release3.releaseId()));
        assertNull(tombstoneMapper.selectByScopedResource(DEFAULT_TENANT_ID, DEFAULT_SITE_KEY, DEFAULT_STAGE,
                "release", release4.releaseId()));
        assertEquals(release4.releaseId(),
                releasePointerMapper.selectByPointerScope(DEFAULT_TENANT_ID, DEFAULT_SITE_KEY, DEFAULT_STAGE,
                        ShowroomReleaseConstants.POINTER_KEY).getReleaseId());
        assertEquals(9, releaseAssetRefMapper.selectListByReleaseId(release2.releaseId()).size());
    }

    @Test
    void shouldClearAssetTombstoneWhenPurgedAssetIsReusedByNewRelease() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        bindDefaultSiteStage();
        ShowroomMaterializedRelease release1 = publisherService.publishRelease(900L, fixture.publishedAt(),
                DEFAULT_SITE_KEY, DEFAULT_STAGE);
        ShowroomMaterializedRelease.MaterializedAsset reusedPreview = release1.assets().stream()
                .filter(asset -> asset.assetId().equals("product-" + fixture.productId() + "-preview"))
                .findFirst()
                .orElseThrow();

        mockFile(103L, 11L, "showroom/product/产品封面图.png", "image/png", "product-preview-v2");
        publisherService.publishRelease(900L, fixture.publishedAt().plusSeconds(60), DEFAULT_SITE_KEY,
                DEFAULT_STAGE);
        publisherService.publishRelease(900L, fixture.publishedAt().plusSeconds(120), DEFAULT_SITE_KEY,
                DEFAULT_STAGE);
        publisherService.publishRelease(900L, fixture.publishedAt().plusSeconds(180), DEFAULT_SITE_KEY,
                DEFAULT_STAGE);

        String resourceKey = reusedPreview.assetId() + ":" + reusedPreview.contentHash();
        assertNotNull(tombstoneMapper.selectByScopedResource(DEFAULT_TENANT_ID, DEFAULT_SITE_KEY, DEFAULT_STAGE,
                ShowroomReleaseConstants.RESOURCE_TYPE_ASSET, resourceKey));
        ShowroomReleaseApiException purged = assertThrows(ShowroomReleaseApiException.class,
                () -> manifestQueryService.queryAsset(defaultReleaseScope(), reusedPreview.assetId(),
                        reusedPreview.contentHash()));
        assertEquals(HttpStatus.GONE, purged.getStatus());

        mockFile(103L, 11L, "showroom/product/产品封面图.png", "image/png", "product-cover");
        ShowroomMaterializedRelease release5 = publisherService.publishRelease(900L,
                fixture.publishedAt().plusSeconds(240), DEFAULT_SITE_KEY, DEFAULT_STAGE);

        assertEquals(release5.releaseId(),
                releasePointerMapper.selectByPointerScope(DEFAULT_TENANT_ID, DEFAULT_SITE_KEY, DEFAULT_STAGE,
                        ShowroomReleaseConstants.POINTER_KEY).getReleaseId());
        assertNull(tombstoneMapper.selectByScopedResource(DEFAULT_TENANT_ID, DEFAULT_SITE_KEY, DEFAULT_STAGE,
                ShowroomReleaseConstants.RESOURCE_TYPE_ASSET, resourceKey));
        ShowroomReleaseManifestQueryService.AssetView asset = assertDoesNotThrow(
                () -> manifestQueryService.queryAsset(defaultReleaseScope(), reusedPreview.assetId(),
                        reusedPreview.contentHash()));
        assertEquals(reusedPreview.mimeType(), asset.contentType());
        assertEquals(reusedPreview.contentHash(), ShowroomReleaseHashSupport.sha256Hex(asset.body()));
    }

    @Test
    void shouldReuseSoftDeletedAssetTombstoneWhenAssetIsPurgedAgain() throws Exception {
        PublishedFixture fixture = seedPublishedFixture();
        bindDefaultSiteStage();
        ShowroomMaterializedRelease release1 = publisherService.publishRelease(900L, fixture.publishedAt(),
                DEFAULT_SITE_KEY, DEFAULT_STAGE);
        ShowroomMaterializedRelease.MaterializedAsset reusedPreview = release1.assets().stream()
                .filter(asset -> asset.assetId().equals("product-" + fixture.productId() + "-preview"))
                .findFirst()
                .orElseThrow();

        mockFile(103L, 11L, "showroom/product/产品封面图.png", "image/png", "product-preview-v2");
        publisherService.publishRelease(900L, fixture.publishedAt().plusSeconds(60), DEFAULT_SITE_KEY,
                DEFAULT_STAGE);
        publisherService.publishRelease(900L, fixture.publishedAt().plusSeconds(120), DEFAULT_SITE_KEY,
                DEFAULT_STAGE);
        publisherService.publishRelease(900L, fixture.publishedAt().plusSeconds(180), DEFAULT_SITE_KEY,
                DEFAULT_STAGE);

        String resourceKey = reusedPreview.assetId() + ":" + reusedPreview.contentHash();
        assertNotNull(tombstoneMapper.selectByScopedResource(DEFAULT_TENANT_ID, DEFAULT_SITE_KEY, DEFAULT_STAGE,
                ShowroomReleaseConstants.RESOURCE_TYPE_ASSET, resourceKey));

        mockFile(103L, 11L, "showroom/product/产品封面图.png", "image/png", "product-cover");
        publisherService.publishRelease(900L, fixture.publishedAt().plusSeconds(240), DEFAULT_SITE_KEY,
                DEFAULT_STAGE);
        assertNull(tombstoneMapper.selectByScopedResource(DEFAULT_TENANT_ID, DEFAULT_SITE_KEY, DEFAULT_STAGE,
                ShowroomReleaseConstants.RESOURCE_TYPE_ASSET, resourceKey));

        mockFile(103L, 11L, "showroom/product/产品封面图.png", "image/png", "product-preview-v2");
        publisherService.publishRelease(900L, fixture.publishedAt().plusSeconds(300), DEFAULT_SITE_KEY,
                DEFAULT_STAGE);
        publisherService.publishRelease(900L, fixture.publishedAt().plusSeconds(360), DEFAULT_SITE_KEY,
                DEFAULT_STAGE);
        assertDoesNotThrow(() -> publisherService.publishRelease(900L, fixture.publishedAt().plusSeconds(420),
                DEFAULT_SITE_KEY, DEFAULT_STAGE));
        assertNotNull(tombstoneMapper.selectByScopedResource(DEFAULT_TENANT_ID, DEFAULT_SITE_KEY, DEFAULT_STAGE,
                ShowroomReleaseConstants.RESOURCE_TYPE_ASSET, resourceKey));
    }
}
