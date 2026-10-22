package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowIdempotency;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MesPqcProductionReleaseUdiPolicyTest {

    @Test
    void rejectsBlankUdiDocumentNumberAfterTrim() {
        assertThrows(IllegalArgumentException.class,
                () -> MesPqcProductionReleaseUdiPolicy.requireNormalized("   "));
    }

    @Test
    void accepts128CharactersAndRejects129Characters() {
        String accepted = "A".repeat(128);
        assertEquals(accepted, MesPqcProductionReleaseUdiPolicy.requireNormalized(" " + accepted + " "));
        assertThrows(IllegalArgumentException.class,
                () -> MesPqcProductionReleaseUdiPolicy.requireNormalized("A".repeat(129)));
    }

    @Test
    void writesWhenOrderHasNoValueAndAllowsSameValueReplay() {
        var order = new cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO();
        assertEquals(MesPqcProductionReleaseUdiPolicy.WriteDecision.WRITE,
                MesPqcProductionReleaseUdiPolicy.checkCompatibility(order, "UDI-A"));
        order.setUdiControlDocumentNo("UDI-A");
        assertEquals(MesPqcProductionReleaseUdiPolicy.WriteDecision.IDEMPOTENT,
                MesPqcProductionReleaseUdiPolicy.checkCompatibility(order, "UDI-A"));
    }

    @Test
    void rejectsDifferentValueWithoutOverwritingExistingOrderValue() {
        var order = new cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO()
                .setUdiControlDocumentNo("UDI-A");
        assertThrows(IllegalStateException.class,
                () -> MesPqcProductionReleaseUdiPolicy.checkCompatibility(order, "UDI-B"));
        assertEquals("UDI-A", order.getUdiControlDocumentNo());
    }

    @Test
    void bindsUdiDocumentNumberToApprovalPayloadHash() {
        String hashA = MesReleaseFlowIdempotency.payloadHash(
                "APPROVE", "100", "200", "1", "7", "", "UDI-A");
        String hashB = MesReleaseFlowIdempotency.payloadHash(
                "APPROVE", "100", "200", "1", "7", "", "UDI-B");
        assertNotEquals(hashA, hashB);
    }
}
