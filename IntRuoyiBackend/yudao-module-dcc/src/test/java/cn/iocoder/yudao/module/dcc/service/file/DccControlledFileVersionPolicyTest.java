package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccControlledFileVersionPolicyTest {

    @Test
    void defaultPolicyTreatsFirstSlashSegmentAsMajorIdentity() {
        DccControlledFileVersionPolicy policy = DccControlledFileVersionPolicy.defaultPolicy();

        assertEquals("A", policy.requireStored(file("A/1")).majorIdentity());
        assertFalse(policy.isMajorVersionChange(file("A/2"), file("A/1")));
        assertTrue(policy.isMajorVersionChange(file("B/1"), file("A/2")));
        assertEquals("A/2", policy.nextMinor(file("A/1"), List.of(file("A/1"))).display());
        assertEquals("B/1", policy.nextMajor(file("A/9"), List.of(file("A/1"), file("A/9"))).display());
    }

    @Test
    void configurablePolicyTreatsFirstTwoSlashSegmentsAsMajorIdentity() {
        DccControlledFileVersionPolicyProperties properties = new DccControlledFileVersionPolicyProperties();
        properties.setMajorIdentitySegmentCount(2);
        DccControlledFileVersionPolicy policy = new DccControlledFileVersionPolicy(properties);

        assertEquals("A/1", policy.requireStored(file("A/1/1")).majorIdentity());
        assertEquals("B/1", policy.requireStored(file("B/1/1")).majorIdentity());
        assertFalse(policy.isMajorVersionChange(file("A/1/2"), file("A/1/1")));
        assertTrue(policy.isMajorVersionChange(file("B/1/1"), file("A/1/2")));
        assertTrue(policy.isMajorVersionChange(file("A/2/1"), file("A/1/2")));
        assertEquals("A/1/3", policy.nextMinor(file("A/1/1"), List.of(file("A/1/1"), file("A/1/2"))).display());
        assertEquals("B/1/1", policy.nextMajor(file("A/1/9"), List.of(file("A/1/9"))).display());
    }

    @Test
    void initialVersionRequiresFirstMinorIterationForConfiguredPolicy() {
        DccControlledFileVersionPolicyProperties properties = new DccControlledFileVersionPolicyProperties();
        properties.setMajorIdentitySegmentCount(2);
        DccControlledFileVersionPolicy policy = new DccControlledFileVersionPolicy(properties);

        assertEquals("A/1/1", policy.initialForNewFile(null));
        assertEquals("B/1/1", policy.initialForNewFile("b/1/1"));
        assertThrows(IllegalArgumentException.class, () -> policy.initialForNewFile("A/1/2"));
        assertThrows(IllegalArgumentException.class, () -> policy.initialForNewFile("A/1"));
    }

    private DccControlledFileDO file(String versionNo) {
        DccControlledFileVersionPolicy.VersionNumber parsed = DccControlledFileVersionPolicy.defaultPolicy().parse(versionNo);
        String revisionCode = parsed == null ? null : parsed.majorIdentity();
        Integer iterationNo = parsed == null ? null : parsed.iterationNo();
        return DccControlledFileDO.builder()
                .id((long) versionNo.hashCode())
                .masterId(10L)
                .versionNo(versionNo)
                .revisionCode(revisionCode)
                .iterationNo(iterationNo)
                .build();
    }
}
