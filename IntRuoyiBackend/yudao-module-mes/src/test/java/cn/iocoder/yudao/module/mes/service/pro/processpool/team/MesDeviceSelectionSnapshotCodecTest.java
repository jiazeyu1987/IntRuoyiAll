package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamProcessDeviceDO;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineDeviceSelectionGroup;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MesDeviceSelectionSnapshotCodecTest {

    @Test
    void shouldFreezeSingleAndMultipleGroups() {
        String json = MesDeviceSelectionSnapshotCodec.canonicalize(List.of(
                binding(10L, 101L, "wash", "MULTIPLE"),
                binding(10L, 102L, "wash", "MULTIPLE"),
                binding(10L, 201L, "dry", "SINGLE")
        ), 10L);

        List<MesFrontlineDeviceSelectionGroup> groups = MesDeviceSelectionSnapshotCodec.parse(json);
        assertEquals(List.of("dry", "wash"), groups.stream()
                .map(MesFrontlineDeviceSelectionGroup::deviceGroupKey).toList());
        assertEquals(List.of(101L, 102L), groups.get(1).deviceIds());
        assertEquals("MULTIPLE", groups.get(1).selectionMode());
    }

    @Test
    void shouldRejectConflictingModesInOneGroup() {
        assertThrows(IllegalStateException.class, () -> MesDeviceSelectionSnapshotCodec.canonicalize(List.of(
                binding(10L, 101L, "wash", "SINGLE"),
                binding(10L, 102L, "wash", "MULTIPLE")
        ), 10L));
    }

    private static MesProcessPoolTeamProcessDeviceDO binding(Long processId, Long deviceId,
                                                              String groupKey, String mode) {
        return MesProcessPoolTeamProcessDeviceDO.builder()
                .processId(processId)
                .deviceId(deviceId)
                .deviceGroupKey(groupKey)
                .selectionMode(mode)
                .enabled(true)
                .build();
    }
}
