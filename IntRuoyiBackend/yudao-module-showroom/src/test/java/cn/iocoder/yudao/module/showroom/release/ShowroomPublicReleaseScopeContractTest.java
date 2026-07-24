package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseAssetMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseAssetRefMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseDocumentMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleasePointerMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomReleaseTombstoneMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ShowroomPublicReleaseScopeContractTest {

    @Test
    void legacyCurrentWithoutSiteAndStageMustFailFast() {
        ShowroomReleaseManifestQueryService service = new ShowroomReleaseManifestQueryService(
                mock(ShowroomReleasePointerMapper.class),
                mock(ShowroomReleaseMapper.class),
                mock(ShowroomReleaseDocumentMapper.class),
                mock(ShowroomReleaseAssetMapper.class),
                mock(ShowroomReleaseAssetRefMapper.class),
                mock(ShowroomReleaseTombstoneMapper.class));

        var response = service.getCurrentResponse(new HttpHeaders());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("SHOWROOM_SITE_SELECTOR_REQUIRED"));
    }

    @Test
    void releasePointerMapperMustExposeSiteStageScopedCurrentSelector() {
        boolean hasScopedSelector = Arrays.stream(ShowroomReleasePointerMapper.class.getMethods())
                .anyMatch(method -> "selectByPointerScope".equals(method.getName())
                        && method.getParameterCount() == 4);

        assertTrue(hasScopedSelector, "release current pointer must be selected by tenantId + siteKey + stage + pointerKey");
    }
}
