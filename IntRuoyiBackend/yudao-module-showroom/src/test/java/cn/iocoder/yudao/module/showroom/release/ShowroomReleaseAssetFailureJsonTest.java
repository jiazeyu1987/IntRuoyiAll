package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShowroomReleaseAssetFailureJsonTest extends AbstractShowroomReleaseDbTest {

    @Test
    void shouldReturnJsonErrorWhenAssetBytesAreBroken() throws Exception {
        ShowroomMaterializedRelease release = publishReleaseFixture();
        ShowroomMaterializedRelease.MaterializedAsset asset = release.assets().getFirst();
        var assetDo = releaseAssetMapper.selectByScopeAssetIdAndContentHash(DEFAULT_TENANT_ID, DEFAULT_SITE_KEY,
                DEFAULT_STAGE, asset.assetId(), asset.contentHash());
        assetDo.setBinaryContent(new byte[0]);
        releaseAssetMapper.updateById(assetDo);

        var response = scopedAssetController.getAsset(DEFAULT_SITE_KEY, DEFAULT_STAGE,
                asset.assetId(), asset.contentHash(), new HttpHeaders());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = JsonUtils.parseObject(String.valueOf(response.getBody()), Map.class);
        Map<String, Object> error = castMap(body.get("error"));
        assertEquals("SHOWROOM_ASSET_BROKEN", error.get("code"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
