package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccWorkingDraftVerifiedSourceIdentityTest {
    private final DccWorkingApplicationDraftInitializer initializer = new DccWorkingApplicationDraftInitializer();
    private final DccControlledFileNameClaimService names = mock(DccControlledFileNameClaimService.class);
    private DccControlledFileDO source(String name) {
        return DccControlledFileDO.builder().id(21L).tenantId(1L).masterId(10L).dccProjectCodeId(5L)
                .sourceFileId(100L).sourceSha256("a".repeat(64)).versionNo("A/1-1")
                .sourceOriginalFileName(name).status("WORKING").requesterId(99L).build();
    }
    private DccControlledFileDO resolve(DccControlledFileDO source) {
        return ReflectionTestUtils.invokeMethod(initializer, "verifiedSourceIdentity", source);
    }
    @Test void verifiedLegacyNameCreatesOnlyAnImmutableIdentityProjection() {
        ReflectionTestUtils.setField(initializer, "verifiedSourceNames", names);
        var original = source(null); String before = original.toString();
        when(names.requireSourceName(original)).thenReturn("已核验原件.pdf");
        var projected = resolve(original);
        assertNotSame(original, projected); assertEquals(before, original.toString());
        assertEquals("已核验原件.pdf", projected.getSourceOriginalFileName());
        assertEquals(original.getId(), projected.getId()); assertEquals(original.getTenantId(), projected.getTenantId());
        assertEquals(original.getMasterId(), projected.getMasterId()); assertEquals(original.getSourceFileId(), projected.getSourceFileId());
        assertEquals(original.getVersionNo(), projected.getVersionNo()); assertEquals(original.getSourceSha256(), projected.getSourceSha256());
        verify(names).requireSourceName(original); verifyNoMoreInteractions(names);
    }
    @Test void modernOriginalIdentityDoesNotInvokeLegacyEvidenceResolution() {
        ReflectionTestUtils.setField(initializer, "verifiedSourceNames", names);
        var original = source("真实现行原件.pdf");
        assertSame(original, resolve(original)); verifyNoInteractions(names);
    }
    @Test void missingOrFailedFormalProofCannotBeSuppressed() {
        ReflectionTestUtils.setField(initializer, "verifiedSourceNames", names);
        var original = source(null); String before = original.toString();
        when(names.requireSourceName(original)).thenThrow(new IllegalStateException("selected historical source has no complete verified identity"));
        assertThrows(IllegalStateException.class, () -> resolve(original)); assertEquals(before, original.toString());
    }
    @Test void blankResolvedNameCannotSatisfyAnIdentity() {
        ReflectionTestUtils.setField(initializer, "verifiedSourceNames", names);
        var original = source(null);
        when(names.requireSourceName(original)).thenReturn("  ");
        assertThrows(IllegalStateException.class, () -> resolve(original)); assertNull(original.getSourceOriginalFileName());
    }
}
