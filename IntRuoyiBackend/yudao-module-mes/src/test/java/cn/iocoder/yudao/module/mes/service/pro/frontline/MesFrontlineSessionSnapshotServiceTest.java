package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlineSessionSnapshotServiceTest {

    @Mock
    private MesFrontlineSessionSnapshotStore snapshotStore;

    private MesFrontlineSessionSnapshotServiceImpl service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        service = new MesFrontlineSessionSnapshotServiceImpl(snapshotStore);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldIssueAndLoadImmutableSnapshotForCurrentTenantAndLoginUser() {
        MesFrontlineSessionSnapshotReference reference = service.issue(content());
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotStore).save(eq(1L), eq(reference.snapshotId()), jsonCaptor.capture(),
                eq(MesFrontlineSessionSnapshotServiceImpl.SNAPSHOT_TTL));
        assertTrue(reference.snapshotHash().length() == 64);
        when(snapshotStore.get(1L, reference.snapshotId())).thenReturn(jsonCaptor.getValue());

        MesFrontlineSessionSnapshot loaded = service.require(reference.snapshotId(),
                reference.snapshotHash(), 9001L);

        assertEquals(101L, loaded.content().routeId());
        assertEquals(10001L, loaded.content().employeeSwitchSnapshots().get(0).actualEmployeeId());
    }

    @Test
    void shouldRejectExpiredSnapshot() {
        when(snapshotStore.get(1L, "expired")).thenReturn(null);

        assertThrows(ServiceException.class, () -> service.require("expired", "hash", 9001L));
    }

    @Test
    void shouldRejectHashOrLoginUserMismatch() {
        MesFrontlineSessionSnapshotReference reference = service.issue(content());
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotStore).save(eq(1L), eq(reference.snapshotId()), jsonCaptor.capture(),
                eq(MesFrontlineSessionSnapshotServiceImpl.SNAPSHOT_TTL));
        when(snapshotStore.get(1L, reference.snapshotId())).thenReturn(jsonCaptor.getValue());

        assertThrows(ServiceException.class, () -> service.require(reference.snapshotId(), "tampered", 9001L));
        assertThrows(ServiceException.class, () -> service.require(reference.snapshotId(),
                reference.snapshotHash(), 9002L));
    }

    private static MesFrontlineSessionSnapshotContent content() {
        MesFrontlineEmployeeSwitchResult employee = new MesFrontlineEmployeeSwitchResult(
                9001L, 10001L, 101L, 1001L, 201L, false,
                new MesFrontlineTemplateDescriptor("TPL-001", "PRODUCTION", 1001L, 201L, 10001L));
        return new MesFrontlineSessionSnapshotContent(1L, 9001L, 101L, 1001L, 201L, 301L,
                List.of(employee), List.of(), List.of(), List.of(),
                new MesFrontlineProductionSubmitContext(null, null, null, null,
                        101L, 1001L, 201L, 301L, null, 9001L, null, null, null));
    }

}
