package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizerTest {

    @Test
    void canonicalizesObjectOrderWhitespaceAndEquivalentNumbersWithoutChangingJsonTypes() {
        assertEquals(
                "{\"number\":1000,\"text\":\"1\",\"zero\":0}",
                MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.canonicalize(
                        " { \"zero\" : -0.0, \"number\" : 1e3, \"text\" : \"1\" } "));
        assertEquals(
                MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.canonicalize("{\"n\":1000}"),
                MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.canonicalize("{\"n\":1000.0}"));
    }

    @Test
    void preservesArrayOrderAndDistinguishesAdjacentHighPrecisionNumbers() {
        assertNotEquals(
                MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.canonicalize("[1,2]"),
                MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.canonicalize("[2,1]"));
        assertNotEquals(
                MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.canonicalize(
                        "{\"n\":1.0000000000000000000001}"),
                MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.canonicalize(
                        "{\"n\":1.0000000000000000000002}"));
    }

    @Test
    void rejectsInvalidOrBlankJson() {
        assertThrows(IllegalArgumentException.class,
                () -> MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.canonicalize(" "));
        assertThrows(IllegalArgumentException.class,
                () -> MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.canonicalize("{broken"));
    }
}
