package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MesEdhrMaterialAssociationIdentityTest {
    @Test void submissionIdentityBindsTenantReleaseAndExactKey() {
        String original = MesEdhrSourceCreationCodec.submissionMaterialKeyDigest(1, 2, "request");
        assertEquals(original, MesEdhrSourceCreationCodec.submissionMaterialKeyDigest(1, 2, "request"));
        assertNotEquals(original, MesEdhrSourceCreationCodec.submissionMaterialKeyDigest(2, 2, "request"));
        assertNotEquals(original, MesEdhrSourceCreationCodec.submissionMaterialKeyDigest(1, 3, "request"));
        assertNotEquals(original, MesEdhrSourceCreationCodec.submissionMaterialKeyDigest(1, 2, "request "));
    }
    @Test void handoffIdentitySeparatesActualUpstreamBranchesAndVersions() {
        String pqc = MesEdhrSourceCreationCodec.managedHandoffAuditKey(1, 2, "MANAGED_PQC_APPROVE", 3, 0, "request");
        assertTrue(pqc.matches("MH1:[0-9a-f]{64}"));
        assertNotEquals(pqc, MesEdhrSourceCreationCodec.managedHandoffAuditKey(1, 2, "MANAGED_REPORT_COMPLETE", 3, 0, "request"));
        assertNotEquals(pqc, MesEdhrSourceCreationCodec.managedHandoffAuditKey(1, 2, "MANAGED_PQC_APPROVE", 3, 1, "request"));
        assertThrows(IllegalArgumentException.class, () -> MesEdhrSourceCreationCodec.managedHandoffAuditKey(1, 2, "HTTP", 3, 0, "request"));
        assertThrows(IllegalArgumentException.class, () -> MesEdhrSourceCreationCodec.submissionMaterialKeyDigest(0, 2, "request"));
    }
}