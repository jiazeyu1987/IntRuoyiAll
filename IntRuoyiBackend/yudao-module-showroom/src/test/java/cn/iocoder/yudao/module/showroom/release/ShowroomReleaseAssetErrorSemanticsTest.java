package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseTombstoneDO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShowroomReleaseAssetErrorSemanticsTest extends AbstractShowroomReleaseDbTest {

    @Test
    void shouldReturnExplicitNotFoundAndGoneForAssets() throws Exception {
        ShowroomMaterializedRelease release = publishReleaseFixture();
        ShowroomMaterializedRelease.MaterializedAsset asset = release.assets().getFirst();

        var missing = scopedAssetController.getAsset(DEFAULT_SITE_KEY, DEFAULT_STAGE,
                "missing-asset", "deadbeef", new HttpHeaders());
        assertEquals(HttpStatus.NOT_FOUND, missing.getStatusCode());
        assertEquals("SHOWROOM_ASSET_NOT_FOUND", castErrorCode(missing.getBody()));

        tombstoneMapper.insert(ShowroomReleaseTombstoneDO.builder()
                .tenantId(DEFAULT_TENANT_ID)
                .siteKey(DEFAULT_SITE_KEY)
                .stage(DEFAULT_STAGE)
                .resourceType("asset")
                .resourceKey(asset.assetId() + ":" + asset.contentHash())
                .purgedAt(LocalDateTime.now(ZoneOffset.UTC))
                .reason("manual-test")
                .build());

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.IF_NONE_MATCH, "\"" + asset.contentHash() + "\"");
        var gone = scopedAssetController.getAsset(DEFAULT_SITE_KEY, DEFAULT_STAGE,
                asset.assetId(), asset.contentHash(), headers);
        assertEquals(HttpStatus.GONE, gone.getStatusCode());
        assertEquals("SHOWROOM_ASSET_PURGED", castErrorCode(gone.getBody()));
    }

    private static String castErrorCode(Object bodyValue) {
        Map<String, Object> body = JsonUtils.parseObject(String.valueOf(bodyValue), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) body.get("error");
        return String.valueOf(error.get("code"));
    }
}
