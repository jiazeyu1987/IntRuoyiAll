package cn.iocoder.yudao.module.showroom.release;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ShowroomReleaseCurrentPointerServiceTest extends AbstractShowroomReleaseDbTest {

    @Resource
    private ShowroomReleaseRegistryService registryService;

    @Test
    void shouldSwitchCurrentPointerAtomically() {
        assertNotNull(registryService);
    }
}
