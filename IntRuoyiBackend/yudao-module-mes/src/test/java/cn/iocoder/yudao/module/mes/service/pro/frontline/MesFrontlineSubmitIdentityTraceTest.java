package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlineSubmitIdentityTraceTest {

    @Mock
    private MesFrontlineDeviceAccountContextService contextService;
    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesFrontlineSessionSnapshotService sessionSnapshotService;

    private MesFrontlineSubmitAuthorizationServiceImpl submitAuthorizationService;

    @BeforeEach
    void setUp() {
        submitAuthorizationService = new MesFrontlineSubmitAuthorizationServiceImpl(
                contextService, activeOrderMapper, sessionSnapshotService);
    }

    @Test
    void shouldReturnCompleteIdentityTraceForSubmit() {
        MesFrontlineSubmitIdentityCommand command = new MesFrontlineSubmitIdentityCommand(
                9001L, 10001L, 10001L, 501L, 301L, 101L, 1001L, 201L,
                "TPL-201-E1001", "snapshot-001", "hash-001");
        MesFrontlineEmployeeSwitchResult employee = new MesFrontlineEmployeeSwitchResult(
                9001L, 10001L, 101L, 1001L, 201L, false,
                new MesFrontlineTemplateDescriptor("TPL-201-E1001", "BATCH_RECORD", 1001L, 201L, 10001L));
        MesFrontlineSessionSnapshotContent content = new MesFrontlineSessionSnapshotContent(
                1L, 9001L, 101L, 1001L, 201L, 301L, List.of(employee),
                List.of(new MesFrontlineTeamDeviceOption(501L, "D-501", "Device 501", "ENABLED", List.of())),
                List.of(), new MesFrontlineProductionSubmitContext(null, null, null, null,
                101L, 1001L, 201L, 301L, null, 9001L, null, null, null));
        MesFrontlineSessionSnapshot snapshot = new MesFrontlineSessionSnapshot("snapshot-001", "hash-001", content);
        when(sessionSnapshotService.require("snapshot-001", "hash-001", 9001L)).thenReturn(snapshot);

        MesFrontlineSubmitIdentityTrace trace = submitAuthorizationService.authorize(command);

        assertEquals(9001L, trace.loginUserId());
        assertEquals(10001L, trace.actualEmployeeId());
        assertEquals(501L, trace.deviceId());
        assertEquals(101L, trace.routeId());
        assertEquals("TPL-201-E1001", trace.templateNo());
        assertEquals("snapshot-001", trace.frontlineSessionSnapshotId());
        assertEquals("hash-001", trace.frontlineSessionSnapshotHash());
        assertSame(snapshot, trace.sessionSnapshot());
    }

}
