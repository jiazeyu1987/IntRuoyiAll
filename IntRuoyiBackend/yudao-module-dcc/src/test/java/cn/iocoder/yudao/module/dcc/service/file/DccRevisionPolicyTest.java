package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DccRevisionPolicyTest {
    private final DccControlledFileVersionPolicy policy = DccControlledFileVersionPolicy.defaultPolicy();

    @ParameterizedTest
    @CsvSource({"A/1,A/2", "A/8,A/9", "A/9,B/1", "Y/9,Z/1"})
    void partialRollover(String from, String to) {
        assertEquals(to, policy.nextMinor(file(from), List.of(file(from))).display());
    }

    @Test void parsesWorkingIterationWithoutChangingFormalIdentity() {
        var version = policy.parse("A/1-2");
        assertNotNull(version);
        assertEquals("A/1-2", version.display());
        assertEquals("A", version.majorIdentity());
        assertEquals(1, version.iterationNo());
    }

    @Test void replacementOfA3StartsB1() {
        assertEquals("B/1", policy.nextMajor(file("A/3"), List.of(file("A/3"))).display());
    }

    @Test void zRuleMustBeExplicitlyBlocked() {
        assertThrows(IllegalArgumentException.class, () -> policy.nextMajor(file("Z/3"), List.of()));
        assertThrows(IllegalArgumentException.class, () -> policy.nextMinor(file("Z/9"), List.of()));
    }

    @ParameterizedTest
    @CsvSource({"A/0", "A/10", "A/1-0", "A/1-01", "A/1-2147483648"})
    void invalidNumbersAreRejected(String value) { assertNull(policy.parse(value)); }

    @Test void malformedStoredVersionIsNotRebuiltFromOtherFields() {
        assertNull(policy.parseStored(DccControlledFileDO.builder().versionNo("bad").revisionCode("A").iterationNo(1).build()));
    }

    @Test void legacyFacadeCannotInventAfterZOrSkipNineRollover() {
        assertEquals("B/1", DccWindchillVersionNumber.parse("A/9").nextIteration().display());
        assertThrows(IllegalArgumentException.class, () -> DccWindchillVersionNumber.incrementRevision("Z"));
        assertNull(DccWindchillVersionNumber.parseStoredInitial("V1.0"));
    }

    @Test void historyOrderingIncludesWorkingNumbersNumerically() {
        var ten = DccControlledFileVersion.parse("A/1-10");
        var two = DccControlledFileVersion.parse("A/1-2");
        assertNotNull(ten); assertNotNull(two);
        assertTrue(ten.compareTo(two) > 0);
        assertTrue(two.compareTo(DccControlledFileVersion.parse("A/1")) > 0);
        assertTrue(DccControlledFileVersion.parse("A/2").compareTo(ten) > 0);
    }

    @ParameterizedTest @CsvSource({"AA/1", "A/1/1", "A/1/1-2"})
    void unconfirmedGrammarCannotEnterNewWritePolicy(String raw) {
        assertNull(policy.parse(raw));
        assertNull(DccWindchillVersionNumber.parse(raw));
    }
    @Test void partialRolloverIsNotInferredAsReplacementInReplayMatching() {
        assertTrue(policy.matchesVersionChange(file("A/9"),file("B/1"),false));
        assertFalse(policy.matchesVersionChange(file("A/9"),file("B/2"),false));
        assertFalse(policy.matchesVersionChange(file("A/1"),file("A/1-2"),false));
        assertTrue(policy.matchesVersionChange(file("A/3"),file("B/1"),true));
    }
    @Test void historicalOverflowNeverCrashesComparison() {
        assertNull(DccControlledFileVersion.parse("A/2147483648"));
        assertNull(DccControlledFileVersion.parse("A/1-2147483648"));
    }

    private DccControlledFileDO file(String version) {
        return DccControlledFileDO.builder().versionNo(version).build();
    }
}
