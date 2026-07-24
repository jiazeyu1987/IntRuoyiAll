package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseAssetDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseAssetRefDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseDocumentDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseAssetMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseAssetRefMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseDocumentMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleasePointerMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseTombstoneMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShowroomReleaseManifestQueryServiceTest {

    @Test
    void queryManifestShouldLoadAssetMetadataWithoutBinaryContent() {
        ShowroomReleasePointerMapper pointerMapper = mock(ShowroomReleasePointerMapper.class);
        ShowroomReleaseMapper releaseMapper = mock(ShowroomReleaseMapper.class);
        ShowroomReleaseDocumentMapper documentMapper = mock(ShowroomReleaseDocumentMapper.class);
        ShowroomReleaseAssetMapper assetMapper = mock(ShowroomReleaseAssetMapper.class);
        ShowroomReleaseAssetRefMapper assetRefMapper = mock(ShowroomReleaseAssetRefMapper.class);
        ShowroomReleaseTombstoneMapper tombstoneMapper = mock(ShowroomReleaseTombstoneMapper.class);
        ShowroomReleaseManifestQueryService service = new ShowroomReleaseManifestQueryService(
                pointerMapper, releaseMapper, documentMapper, assetMapper, assetRefMapper, tombstoneMapper);
        ShowroomReleaseScope scope = new ShowroomReleaseScope(1L, "yingtai-showroom", "TEST");

        LocalDateTime publishedAt = LocalDateTime.parse("2026-05-25T12:00:00");
        ShowroomReleaseDO release = ShowroomReleaseDO.builder()
                .releaseId("release-oom")
                .tenantId(scope.tenantId())
                .siteKey(scope.siteKey())
                .stage(scope.stage())
                .schemaVersion(ShowroomReleaseConstants.SCHEMA_VERSION)
                .manifestHash("manifest-hash")
                .rootDocumentId("website-index")
                .publishedAt(publishedAt)
                .status(ShowroomReleaseConstants.RELEASE_STATUS_PUBLISHED)
                .build();
        ShowroomReleaseDocumentDO rootDocument = ShowroomReleaseDocumentDO.builder()
                .releaseId("release-oom")
                .tenantId(scope.tenantId())
                .siteKey(scope.siteKey())
                .stage(scope.stage())
                .documentId("website-index")
                .kind(ShowroomReleaseConstants.DOCUMENT_KIND_WEBSITE_INDEX)
                .contentHash("doc-hash")
                .bytes(32L)
                .materializedAt(publishedAt)
                .payloadJson("{}")
                .build();
        ShowroomReleaseDocumentDO productDocument = ShowroomReleaseDocumentDO.builder()
                .releaseId("release-oom")
                .tenantId(scope.tenantId())
                .siteKey(scope.siteKey())
                .stage(scope.stage())
                .documentId("product-1")
                .kind(ShowroomReleaseConstants.DOCUMENT_KIND_PRODUCT_DETAIL)
                .productId(1L)
                .contentHash("product-hash")
                .bytes(48L)
                .materializedAt(publishedAt)
                .payloadJson("{}")
                .build();
        ShowroomReleaseAssetRefDO rootRef = ShowroomReleaseAssetRefDO.builder()
                .releaseId("release-oom")
                .tenantId(scope.tenantId())
                .siteKey(scope.siteKey())
                .stage(scope.stage())
                .documentId("website-index")
                .assetId("hero-image")
                .contentHash("asset-hash")
                .build();
        ShowroomReleaseAssetRefDO productRef = ShowroomReleaseAssetRefDO.builder()
                .releaseId("release-oom")
                .tenantId(scope.tenantId())
                .siteKey(scope.siteKey())
                .stage(scope.stage())
                .documentId("product-1")
                .assetId("hero-image")
                .contentHash("asset-hash")
                .build();
        ShowroomReleaseAssetDO assetMetadata = ShowroomReleaseAssetDO.builder()
                .tenantId(scope.tenantId())
                .siteKey(scope.siteKey())
                .stage(scope.stage())
                .assetId("hero-image")
                .assetType(ShowroomReleaseConstants.ASSET_TYPE_IMAGE)
                .contentHash("asset-hash")
                .mimeType("image/png")
                .bytes(669_736_727L)
                .materializedAt(publishedAt)
                .status(ShowroomReleaseConstants.RELEASE_STATUS_PUBLISHED)
                .binaryContent(null)
                .build();

        when(tombstoneMapper.selectByScopedResource(scope.tenantId(), scope.siteKey(), scope.stage(),
                ShowroomReleaseConstants.RESOURCE_TYPE_RELEASE, "release-oom"))
                .thenReturn(null);
        when(releaseMapper.selectByReleaseScope(scope.tenantId(), scope.siteKey(), scope.stage(),
                "release-oom")).thenReturn(release);
        when(documentMapper.selectByReleaseScopeAndDocumentId(scope.tenantId(), scope.siteKey(), scope.stage(),
                "release-oom", "website-index")).thenReturn(rootDocument);
        when(documentMapper.selectListByReleaseScope(scope.tenantId(), scope.siteKey(), scope.stage(),
                "release-oom")).thenReturn(List.of(rootDocument, productDocument));
        when(assetRefMapper.selectListByReleaseScope(scope.tenantId(), scope.siteKey(), scope.stage(),
                "release-oom")).thenReturn(List.of(rootRef, productRef));
        when(assetMapper.selectManifestAssetByScopeAssetIdAndContentHash(scope.tenantId(), scope.siteKey(),
                scope.stage(), "hero-image", "asset-hash"))
                .thenReturn(assetMetadata);

        ShowroomReleaseManifestQueryService.ManifestView manifest = service.queryManifestView(scope, "release-oom");

        assertTrue(manifest.bodyJson().contains("\"assetId\":\"hero-image\""));
        assertTrue(manifest.bodyJson().contains("\"referencedBy\":[\"website-index\",\"product-1\"]"));
        verify(assetMapper, times(1)).selectManifestAssetByScopeAssetIdAndContentHash(scope.tenantId(),
                scope.siteKey(), scope.stage(), "hero-image", "asset-hash");
        verify(assetMapper, never()).selectByAssetIdAndContentHash(anyString(), anyString());
    }
}
