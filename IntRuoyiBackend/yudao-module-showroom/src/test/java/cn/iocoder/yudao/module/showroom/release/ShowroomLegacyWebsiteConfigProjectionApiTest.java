package cn.iocoder.yudao.module.showroom.release;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowroomLegacyWebsiteConfigProjectionApiTest extends AbstractShowroomReleaseDbTest {

    @Resource
    private ShowroomLegacyWebsiteConfigProjector legacyProjector;

    @Test
    void shouldProjectWebsiteConfigOnlyFromActiveRelease() throws Exception {
        publishReleaseFixture();

        var payload = legacyProjector.projectCurrentPayload(defaultReleaseScope()).getCheckedData();

        assertEquals("盈泰医疗", payload.company().name());
        assertTrue(payload.company().homeImageUrl().startsWith("/showroom/sites/"));
        assertEquals(1, payload.showrooms().size());
        assertEquals(1, payload.showrooms().getFirst().products().size());
        assertTrue(payload.showrooms().getFirst().products().getFirst().previewImageUrl().startsWith("/showroom/sites/"));
    }
}
