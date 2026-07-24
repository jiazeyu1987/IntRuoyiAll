package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowroomLegacyWebsiteConfigConditionalRequestTest extends AbstractShowroomReleaseDbTest {

    @org.springframework.beans.factory.annotation.Autowired
    private ShowroomLegacyWebsiteConfigProjector legacyProjector;

    @Test
    void shouldReturnNotModifiedAndProjectReleaseOnlyUrls() throws Exception {
        publishReleaseFixture();
        HttpHeaders firstHeaders = new HttpHeaders();
        var ok = legacyProjector.getCurrentResponse(defaultReleaseScope(), firstHeaders);
        assertEquals(HttpStatus.OK, ok.getStatusCode());
        String etag = ok.getHeaders().getETag();
        Map<String, Object> wrapper = JsonUtils.parseObject(ok.getBody(), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) wrapper.get("data");
        @SuppressWarnings("unchecked")
        Map<String, Object> company = (Map<String, Object>) data.get("company");
        assertTrue(String.valueOf(company.get("homeImageUrl")).startsWith("/showroom/sites/"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> showrooms = (List<Map<String, Object>>) data.get("showrooms");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) showrooms.getFirst().get("products");
        assertTrue(String.valueOf(products.getFirst().get("previewImageUrl")).startsWith("/showroom/sites/"));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.IF_NONE_MATCH, etag);
        var notModified = legacyProjector.getCurrentResponse(defaultReleaseScope(), headers);
        assertEquals(HttpStatus.NOT_MODIFIED, notModified.getStatusCode());
    }
}
