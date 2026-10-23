package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.service.file.listener.DccControlledFileActivationJob;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccControlledFileActivationJobTest {

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void handlerOptsIntoStrictTenantFailurePropagation() throws Exception {
        var annotation = DccControlledFileActivationJob.class.getMethod("execute", String.class).getAnnotation(TenantJob.class);
        assertNotNull(annotation);
        assertTrue(annotation.failOnError());
    }

    @Test
    void aggregatesMultipleFailuresAndContinuesAfterEachBadFile() {
        TenantContextHolder.setTenantId(1L);
        var lifecycle = mock(DccControlledFileLifecycleService.class);
        when(lifecycle.dueVersions()).thenReturn(List.of(file(41L), file(42L), file(43L)));
        var first = new IllegalStateException("first activation failure");
        var second = new IllegalArgumentException("second activation failure");
        when(lifecycle.activateDue(41L)).thenThrow(first);
        when(lifecycle.activateDue(42L)).thenThrow(second);
        when(lifecycle.activateDue(43L)).thenReturn(true);

        var failure = assertThrows(IllegalStateException.class,
                () -> new DccControlledFileActivationJob(lifecycle).execute(null));

        assertTrue(failure.getMessage().contains("41"));
        assertTrue(failure.getMessage().contains("42"));
        assertTrue(failure.getMessage().contains("1"));
        assertNull(failure.getCause());
        assertArrayEquals(new Throwable[]{first, second}, failure.getSuppressed());
        verify(lifecycle).dueVersions();
        verify(lifecycle).activateDue(41L);
        verify(lifecycle).activateDue(42L);
        verify(lifecycle).activateDue(43L);
        verifyNoMoreInteractions(lifecycle);
    }

    @Test
    void falseActivationIsNotCountedAndSuccessResultRemainsUnchanged() {
        var lifecycle = mock(DccControlledFileLifecycleService.class);
        when(lifecycle.dueVersions()).thenReturn(List.of(file(51L), file(52L)));
        when(lifecycle.activateDue(51L)).thenReturn(false);
        when(lifecycle.activateDue(52L)).thenReturn(true);

        assertEquals("实际生效版本数：1", new DccControlledFileActivationJob(lifecycle).execute(null));
    }

    @Test
    void oneBadAndTwoGoodFilesAreEachAttemptedOnce() {
        TenantContextHolder.setTenantId(1L);
        var lifecycle = mock(DccControlledFileLifecycleService.class);
        when(lifecycle.dueVersions()).thenReturn(List.of(file(61L), file(62L), file(63L)));
        var original = new IllegalStateException("bad file");
        when(lifecycle.activateDue(61L)).thenThrow(original);
        when(lifecycle.activateDue(62L)).thenReturn(true);
        when(lifecycle.activateDue(63L)).thenReturn(true);
        var failure = assertThrows(IllegalStateException.class,
                () -> new DccControlledFileActivationJob(lifecycle).execute(null));
        assertEquals("DCC activation batch failed: activatedCount=2 failedFileIds=[61]", failure.getMessage());
        assertArrayEquals(new Throwable[]{original}, failure.getSuppressed());
        var order = inOrder(lifecycle);
        order.verify(lifecycle).dueVersions();
        order.verify(lifecycle).activateDue(61L);
        order.verify(lifecycle).activateDue(62L);
        order.verify(lifecycle).activateDue(63L);
        verifyNoMoreInteractions(lifecycle);
    }

    @Test
    void errorEscapesImmediatelyWithoutAttemptingLaterRows() {
        var lifecycle = mock(DccControlledFileLifecycleService.class);
        when(lifecycle.dueVersions()).thenReturn(List.of(file(71L), file(72L)));
        var original = new AssertionError("fatal activation error");
        when(lifecycle.activateDue(71L)).thenThrow(original);
        assertSame(original, assertThrows(AssertionError.class,
                () -> new DccControlledFileActivationJob(lifecycle).execute(null)));
        verify(lifecycle).dueVersions();
        verify(lifecycle).activateDue(71L);
        verifyNoMoreInteractions(lifecycle);
    }

    @Test
    void dueQueryFailurePropagatesUnchangedWithoutActivationCalls() {
        var lifecycle = mock(DccControlledFileLifecycleService.class);
        var queryFailure = new IllegalStateException("due query unavailable");
        when(lifecycle.dueVersions()).thenThrow(queryFailure);

        assertSame(queryFailure, assertThrows(IllegalStateException.class,
                () -> new DccControlledFileActivationJob(lifecycle).execute(null)));
        verify(lifecycle).dueVersions();
        verifyNoMoreInteractions(lifecycle);
    }

    private DccControlledFileDO file(Long id) {
        return DccControlledFileDO.builder().id(id).tenantId(1L).processDefinitionKey("dcc-controlled-file-revision").build();
    }
}
