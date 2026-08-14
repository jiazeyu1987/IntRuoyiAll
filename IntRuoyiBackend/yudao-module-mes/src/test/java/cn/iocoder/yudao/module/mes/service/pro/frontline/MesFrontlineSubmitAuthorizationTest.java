package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlineSubmitAuthorizationTest {

    @Mock
    private MesFrontlineDeviceAccountContextService contextService;
    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    @Mock
    private MesFrontlineSessionSnapshotService sessionSnapshotService;

    private MesFrontlineSubmitAuthorizationServiceImpl submitAuthorizationService;

    @BeforeEach
    void setUp() {
        submitAuthorizationService = new MesFrontlineSubmitAuthorizationServiceImpl(
                contextService, activeOrderMapper, processSnapshotMapper, sessionSnapshotService);
    }

    @Test
    void shouldAuthorizeIdentityFromServerIssuedSnapshot() {
        MesFrontlineSubmitIdentityCommand command = command(10001L, 10001L, 501L,
                101L, 1001L, 201L, "TPL-201-E1001");
        givenSnapshot(List.of(employee(10001L, "TPL-201-E1001")), List.of(device(501L)));

        assertDoesNotThrow(() -> submitAuthorizationService.authorize(command));
    }

    @Test
    void shouldAuthorizeProcessWithoutSelectedDeviceWhenSnapshotHasNoDevice() {
        MesFrontlineSubmitIdentityCommand command = command(10001L, 10001L, null,
                101L, 1001L, 201L, "TPL-201-E1001");
        givenSnapshot(List.of(employee(10001L, "TPL-201-E1001")), List.of());

        assertDoesNotThrow(() -> submitAuthorizationService.authorize(command));
    }

    @Test
    void shouldRejectSignatureEmployeeDifferentFromActualEmployee() {
        MesFrontlineSubmitIdentityCommand command = command(10001L, 10002L, 501L,
                101L, 1001L, 201L, "TPL-201-E1001");

        assertThrows(ServiceException.class, () -> submitAuthorizationService.authorize(command));
    }

    @Test
    void shouldRejectProcessOutsideSnapshot() {
        MesFrontlineSubmitIdentityCommand command = command(10001L, 10001L, 501L,
                199L, 1991L, 299L, "TPL-299-E1001");
        givenSnapshot(List.of(employee(10001L, "TPL-201-E1001")), List.of(device(501L)));

        assertThrows(ServiceException.class, () -> submitAuthorizationService.authorize(command));
    }

    @Test
    void shouldRejectEmployeeOutsideSnapshot() {
        MesFrontlineSubmitIdentityCommand command = command(20001L, 20001L, 501L,
                101L, 1001L, 201L, "TPL-201-E2001");
        givenSnapshot(List.of(employee(10001L, "TPL-201-E1001")), List.of(device(501L)));

        assertThrows(ServiceException.class, () -> submitAuthorizationService.authorize(command));
    }

    @Test
    void shouldRejectTemplateDifferentFromEmployeeSnapshot() {
        MesFrontlineSubmitIdentityCommand command = command(10001L, 10001L, 501L,
                101L, 1001L, 201L, "TPL-CHANGED");
        givenSnapshot(List.of(employee(10001L, "TPL-201-E1001")), List.of(device(501L)));

        assertThrows(ServiceException.class, () -> submitAuthorizationService.authorize(command));
    }

    @Test
    void shouldRejectDeviceOutsideSnapshot() {
        MesFrontlineSubmitIdentityCommand command = command(10001L, 10001L, 999L,
                101L, 1001L, 201L, "TPL-201-E1001");
        givenSnapshot(List.of(employee(10001L, "TPL-201-E1001")), List.of(device(501L)));

        assertThrows(ServiceException.class, () -> submitAuthorizationService.authorize(command));
    }

    @Test
    void shouldAuthorizeSelectedActiveOrderForResponsibleLeaderAndProcessSnapshot() {
        when(contextService.resolveResponsibleLeaderUserId(9001L)).thenReturn(3001L);
        when(activeOrderMapper.selectActiveByLeaderAndWorkOrderForUpdate(3001L, 41L)).thenReturn(
                MesProcessPoolActiveOrderDO.builder().id(81L).workOrderId(41L).routeId(21L).build());
        when(processSnapshotMapper.selectByActiveOrderAndProcess(81L, 71L, 31L)).thenReturn(
                MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                        .activeOrderId(81L).workOrderId(41L).routeId(21L)
                        .routeProcessId(71L).processId(31L).build());

        assertDoesNotThrow(() -> submitAuthorizationService.authorizeActiveOrder(
                9001L, 41L, 21L, 71L, 31L));
    }

    @Test
    void shouldRejectSelectedActiveOrderThatIsNoLongerActiveForResponsibleLeader() {
        when(contextService.resolveResponsibleLeaderUserId(9001L)).thenReturn(3001L);
        when(activeOrderMapper.selectActiveByLeaderAndWorkOrderForUpdate(3001L, 41L)).thenReturn(null);

        assertThrows(ServiceException.class, () -> submitAuthorizationService.authorizeActiveOrder(
                9001L, 41L, 21L, 71L, 31L));
    }

    private void givenSnapshot(List<MesFrontlineEmployeeSwitchResult> employees,
                               List<MesFrontlineTeamDeviceOption> devices) {
        MesFrontlineSessionSnapshotContent content = new MesFrontlineSessionSnapshotContent(
                1L, 9001L, 101L, 1001L, 201L, 301L, employees, devices, List.of(),
                new MesFrontlineProductionSubmitContext(null, null, null, null,
                        101L, 1001L, 201L, 301L, null, 9001L, null, null, null));
        when(sessionSnapshotService.require("snapshot-001", "hash-001", 9001L))
                .thenReturn(new MesFrontlineSessionSnapshot("snapshot-001", "hash-001", content));
    }

    private static MesFrontlineSubmitIdentityCommand command(Long actualEmployeeId, Long signatureEmployeeId,
                                                             Long deviceId, Long routeId, Long routeProcessId,
                                                             Long processId, String templateNo) {
        return new MesFrontlineSubmitIdentityCommand(9001L, actualEmployeeId, signatureEmployeeId,
                deviceId, 301L, routeId, routeProcessId, processId, templateNo, "snapshot-001", "hash-001");
    }

    private static MesFrontlineEmployeeSwitchResult employee(Long actualEmployeeId, String templateNo) {
        return new MesFrontlineEmployeeSwitchResult(9001L, actualEmployeeId, 101L, 1001L, 201L, false,
                new MesFrontlineTemplateDescriptor(templateNo, "BATCH_RECORD", 1001L, 201L, actualEmployeeId));
    }

    private static MesFrontlineTeamDeviceOption device(Long deviceId) {
        return new MesFrontlineTeamDeviceOption(deviceId, "D-" + deviceId, "Device " + deviceId,
                "ENABLED", List.of());
    }

}
