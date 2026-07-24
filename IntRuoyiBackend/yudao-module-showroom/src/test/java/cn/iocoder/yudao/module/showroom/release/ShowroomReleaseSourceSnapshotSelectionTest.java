package cn.iocoder.yudao.module.showroom.release;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ShowroomReleaseSourceSnapshotSelectionTest extends AbstractShowroomReleaseDbTest {

    @Resource
    private ShowroomReleaseAssembler releaseAssembler;

    @Test
    void shouldFreezeSourceSnapshotBeforeMaterialization() throws Exception {
        seedPublishedFixture();

        var snapshot = releaseAssembler.resolveSourceSnapshot();

        assertNotNull(snapshot);
        assertEquals(2, snapshot.previewAssetVersionIds().size());
    }
}
