package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlineSubmitAuthorizationTest {

    @Mock
    private MesFrontlineDeviceAccountContextService contextService;
    @Mock
    private MesFrontlineTemplateResolver templateResolver;

    private MesFrontlineSubmitAuthorizationServiceImpl submitAuthorizationService;

    @BeforeEach
    void setUp() {
        submitAuthorizationService = new MesFrontlineSubmitAuthorizationServiceImpl(contextService, templateResolver);
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

}
