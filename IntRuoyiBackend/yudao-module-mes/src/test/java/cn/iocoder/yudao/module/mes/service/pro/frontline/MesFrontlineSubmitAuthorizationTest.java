package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlineSubmitAuthorizationTest {

    @Mock
    private MesFrontlineDeviceAccountContextService contextService;
    @Mock
    private MesFrontlineTemplateResolver templateResolver;
    @Mock
    private cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;

    private MesFrontlineSubmitAuthorizationServiceImpl submitAuthorizationService;

    @BeforeEach
    void setUp() {
        submitAuthorizationService = new MesFrontlineSubmitAuthorizationServiceImpl(
                contextService, templateResolver, activeOrderMapper, processSnapshotMapper);
    }

    @Test
    void shouldAuthorizeProcessWithoutConfiguredDeviceWhenFormalProcessAlsoHasNoDevice() {
        MesFrontlineSubmitIdentityCommand command = new MesFrontlineSubmitIdentityCommand(
                9001L, 10001L, 10001L, null, 301L, 101L, 1001L, 201L, "TPL-201-E1001");
        when(contextService.requireAuthorizedProcess(9001L, 101L, 1001L, 201L)).thenReturn(
                new MesFrontlineRouteProcessCandidate(101L, "R-101", "Route 101",
                        1001L, 201L, "P-201", "Manual inspection", 10,
                        null, null, null, 301L, "WS-301", "Workstation 301"));
        when(contextService.requireTeamEmployee(9001L, 101L, 1001L, 201L, 10001L)).thenReturn(
                new MesFrontlineEmployeeCandidate(10001L, "E1001", "Alice"));
        when(templateResolver.resolve(new MesFrontlineTemplateRequest(
                9001L, 10001L, 101L, 1001L, 201L))).thenReturn(
                new MesFrontlineTemplateDescriptor("TPL-201-E1001", "BATCH_RECORD",
                        1001L, 201L, 10001L));

        assertDoesNotThrow(() -> submitAuthorizationService.authorize(command));
    }

    @Test
    void shouldAuthorizeWhenOnlyDeviceIdDiffersButProcessAndWorkstationMatch() {
        MesFrontlineSubmitIdentityCommand command = new MesFrontlineSubmitIdentityCommand(
                9001L, 10001L, 10001L, 980009L, 301L, 101L, 1001L, 201L, "TPL-201-E1001");
        when(contextService.requireAuthorizedProcess(9001L, 101L, 1001L, 201L)).thenReturn(
                new MesFrontlineRouteProcessCandidate(101L, "R-101", "Route 101",
                        1001L, 201L, "P-201", "Manual inspection", 10,
                        41L, "A03190", "Formal machinery", 301L, "WS-301", "Workstation 301"));
        when(contextService.requireTeamEmployee(9001L, 101L, 1001L, 201L, 10001L)).thenReturn(
                new MesFrontlineEmployeeCandidate(10001L, "E1001", "Alice"));
        when(templateResolver.resolve(new MesFrontlineTemplateRequest(
                9001L, 10001L, 101L, 1001L, 201L))).thenReturn(
                new MesFrontlineTemplateDescriptor("TPL-201-E1001", "BATCH_RECORD",
                        1001L, 201L, 10001L));

        assertDoesNotThrow(() -> submitAuthorizationService.authorize(command));
    }

    @Test
    void shouldAuthorizeWhenSubmittedDeviceAndWorkstationDifferFromAuthorizedCandidate() {
        MesFrontlineSubmitIdentityCommand command = new MesFrontlineSubmitIdentityCommand(
                9001L, 10001L, 10001L, 980009L, 980010L, 101L, 1001L, 201L, "TPL-201-E1001");
        when(contextService.requireAuthorizedProcess(9001L, 101L, 1001L, 201L)).thenReturn(
                new MesFrontlineRouteProcessCandidate(101L, "R-101", "Route 101",
                        1001L, 201L, "P-201", "Manual inspection", 10,
                        41L, "A03190", "Formal machinery", 301L, "WS-301", "Workstation 301"));
        when(contextService.requireTeamEmployee(9001L, 101L, 1001L, 201L, 10001L)).thenReturn(
                new MesFrontlineEmployeeCandidate(10001L, "E1001", "Alice"));
        when(templateResolver.resolve(new MesFrontlineTemplateRequest(
                9001L, 10001L, 101L, 1001L, 201L))).thenReturn(
                new MesFrontlineTemplateDescriptor("TPL-201-E1001", "BATCH_RECORD",
                        1001L, 201L, 10001L));

        assertDoesNotThrow(() -> submitAuthorizationService.authorize(command));
    }

    @Test
    void shouldRejectSignatureEmployeeDifferentFromActualEmployee() {
        MesFrontlineSubmitIdentityCommand command = new MesFrontlineSubmitIdentityCommand(
                9001L, 10001L, 10002L, 501L, 301L, 101L, 1001L, 201L, "TPL-201-E1001");

        assertThrows(ServiceException.class, () -> submitAuthorizationService.authorize(command));
    }

    @Test
    void shouldRejectSubmitForProcessOutsideDeviceAccountBoundRoutes() {
        MesFrontlineSubmitIdentityCommand command = new MesFrontlineSubmitIdentityCommand(
                9001L, 10001L, 10001L, 501L, 301L, 199L, 1991L, 299L, "TPL-299-E1001");
        when(contextService.requireAuthorizedProcess(9001L, 199L, 1991L, 299L))
                .thenThrow(exception0(1_040_760_002, "route process not authorized"));

        assertThrows(ServiceException.class, () -> submitAuthorizationService.authorize(command));
    }

    @Test
    void shouldRejectSubmitWhenActualEmployeeIsNotBoundToCurrentProcess() {
        MesFrontlineSubmitIdentityCommand command = new MesFrontlineSubmitIdentityCommand(
                9001L, 20001L, 20001L, 501L, 301L, 101L, 1001L, 201L, "TPL-201-E2001");
        when(contextService.requireAuthorizedProcess(9001L, 101L, 1001L, 201L)).thenReturn(
                new MesFrontlineRouteProcessCandidate(101L, "R-101", "Route 101",
                        1001L, 201L, "P-201", "Granulation", 10,
                        501L, "D-501", "Device 501", 301L, "WS-301", "Workstation 301"));
        when(contextService.requireTeamEmployee(9001L, 101L, 1001L, 201L, 20001L))
                .thenThrow(exception0(1_040_760_005, "actual employee not bound"));

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

}
