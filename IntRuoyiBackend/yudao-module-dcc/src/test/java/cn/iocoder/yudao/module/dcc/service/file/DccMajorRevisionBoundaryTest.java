package cn.iocoder.yudao.module.dcc.service.file;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class DccMajorRevisionBoundaryTest {

    @ParameterizedTest
    @CsvSource({"Z,AA", "AA,AB", "AZ,BA", "ZZ,AAA", "ZZZ,AAAA"})
    void confirmedAlphabeticRevisionContinuesWithoutNumericOverflow(String from, String to) {
        var parsed = DccWindchillVersionNumber.parse(from + "/2");
        assertNotNull(parsed);
        assertEquals(to + "/1", parsed.nextRevision().display());
        assertEquals(to + "/1", new DccWindchillVersionNumber(to, 1).display());
    }

    @Test void configuredAndHistoricalComparisonsAgreeAcrossLetterBoundaries() {
        var policy = DccControlledFileVersionPolicy.defaultPolicy();
        var ascending = java.util.List.of("Z/9", "AA/1", "AA/1-2", "AA/1-10", "AA/2", "AZ/9", "BA/1", "ZZ/9", "AAA/1");
        for (int i=1; i<ascending.size(); i++) {
            var previous = policy.parse(ascending.get(i-1));
            var current = policy.parse(ascending.get(i));
            assertNotNull(previous); assertNotNull(current);
            assertTrue(previous.compareTo(current)<0);
            assertTrue(DccControlledFileVersion.parse(ascending.get(i-1))
                    .compareTo(DccControlledFileVersion.parse(ascending.get(i)))<0);
        }
        assertEquals("AB/1",policy.nextMajor(file("Z/1"),java.util.List.of(file("AA/9"),file("Z/9"))).display());
        assertEquals("AA/1-11",policy.nextWorking(file("AA/1"),java.util.List.of(file("Z/1-99"),file("AA/1-10"))).display());
    }

    @Test void multisegmentConfigurationPreservesIdentityWhileIncrementingLetters() {
        var properties=new DccControlledFileVersionPolicyProperties(); properties.setMajorIdentitySegmentCount(2);
        var policy=new DccControlledFileVersionPolicy(properties);
        assertEquals("AA/1/1",policy.parse("Z/2/9").nextMajor().display());
        assertEquals("AA/2/10",policy.parse("AA/2/9").nextMinor().display());
        assertEquals("AA/2",policy.parse("AA/2/9").majorIdentity());
    }

    private cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO file(String version) {
        return cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO.builder().versionNo(version).build();
    }
}
