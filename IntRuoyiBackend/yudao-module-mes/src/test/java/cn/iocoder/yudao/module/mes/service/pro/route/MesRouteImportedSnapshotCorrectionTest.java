package cn.iocoder.yudao.module.mes.service.pro.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MesRouteImportedSnapshotCorrectionTest {
    private static String snapshot(String key, long inner) {
        return "{\"routeId\":9,\"configSnapshots\":{\"flowGraph\":{\"nodes\":[{\"routeProcessId\":111,\"processId\":501,\"sort\":1}]},\"scheduleConfigs\":{\""
            + key + "\":{\"routeProcessId\":" + inner + ",\"remark\":null,\"capacityMode\":\"MACHINE\"}}}}";
    }
    @Test void correctionChangesOnlyTheInvalidOuterIndex() throws Exception {
        ObjectMapper json = new ObjectMapper();
        String original = snapshot("11",111);
        var before = json.readTree(original);
        var after = json.readTree(MesRouteImportedSnapshotCorrection.correctScheduleIndexes(9L,original,1));
        var configs = after.path("configSnapshots").path("scheduleConfigs");
        assertFalse(configs.has("11"));
        assertEquals(before.path("configSnapshots").path("scheduleConfigs").path("11"),configs.path("111"));
        assertEquals(before.path("configSnapshots").path("flowGraph"),after.path("configSnapshots").path("flowGraph"));
        assertTrue(new MesProRouteSnapshotCanonicalizer().validate(9L,after.toString()).ready());
    }
    @Test void wrongCountUnknownNodeAndAlreadyValidSourceAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> MesRouteImportedSnapshotCorrection.correctScheduleIndexes(9L,snapshot("11",111),2));
        assertThrows(IllegalArgumentException.class, () -> MesRouteImportedSnapshotCorrection.correctScheduleIndexes(9L,snapshot("11",112),1));
        assertThrows(IllegalArgumentException.class, () -> MesRouteImportedSnapshotCorrection.correctScheduleIndexes(9L,snapshot("111",111),1));
    }
    @Test void scopeAndAuditCannotBeExpandedByCommandInput() throws Exception {
        ObjectMapper json = new ObjectMapper();
        assertThrows(IllegalArgumentException.class, () -> MesRouteImportedSnapshotCorrection.plan(json.readTree("[]")));
        assertThrows(IllegalArgumentException.class, () -> MesRouteImportedSnapshotCorrectionCommand.auditReason("bad","/private/plan.json"));
        assertThrows(IllegalArgumentException.class, () -> MesRouteImportedSnapshotCorrectionCommand.auditReason("a".repeat(64),"/private/../plan.json"));
        assertTrue(MesRouteImportedSnapshotCorrectionCommand.auditReason("a".repeat(64),"/private/plan.json").contains("original JSON and frozen identity retained"));
    }
}
