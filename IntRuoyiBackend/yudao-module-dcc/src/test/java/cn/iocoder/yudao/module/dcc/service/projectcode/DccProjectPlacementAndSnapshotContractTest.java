package cn.iocoder.yudao.module.dcc.service.projectcode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DccProjectPlacementAndSnapshotContractTest {
    @Test void applicationSnapshotsMustUseFormalBpmRoundMapping() {
        assertDoesNotThrow(() -> Class.forName("cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectApplicationSnapshotService")
                .getMethod("submit", Long.class, Long.class, String.class, Long.class, String.class,
                        cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes.class));
    }
    @Test void folderPlacementMustHaveDistinctPhysicalAndProjectIdentities() {
        assertDoesNotThrow(() -> Class.forName("cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccProjectFilePlacementService")
                .getMethod("bind", Long.class, Long.class, Long.class, Long.class, Long.class, String.class));
    }
}
