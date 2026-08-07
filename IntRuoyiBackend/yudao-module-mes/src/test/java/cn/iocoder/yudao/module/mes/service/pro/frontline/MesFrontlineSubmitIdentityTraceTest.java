package cn.iocoder.yudao.module.mes.service.pro.frontline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlineSubmitIdentityTraceTest {

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
    void shouldReturnCompleteIdentityTraceForSubmit() {
        MesFrontlineSubmitIdentityCommand command = new MesFrontlineSubmitIdentityCommand(
                9001L, 10001L, 10001L, 501L, 301L, 101L, 1001L, 201L, "TPL-201-E1001");
        when(contextService.requireAuthorizedProcess(9001L, 101L, 1001L, 201L)).thenReturn(
                new MesFrontlineRouteProcessCandidate(101L, "R-101", "Route 101",
                        1001L, 201L, "P-201", "Granulation", 10,
                        501L, "D-501", "Device 501", 301L, "WS-301", "Workstation 301"));
        when(contextService.requireTeamEmployee(9001L, 101L, 1001L, 201L, 10001L)).thenReturn(
                new MesFrontlineEmployeeCandidate(10001L, "E1001", "Alice"));
        when(templateResolver.resolve(new MesFrontlineTemplateRequest(9001L, 10001L, 101L, 1001L, 201L))).thenReturn(
                new MesFrontlineTemplateDescriptor("TPL-201-E1001", "BATCH_RECORD",
                        1001L, 201L, 10001L));

        MesFrontlineSubmitIdentityTrace trace = submitAuthorizationService.authorize(command);

        assertEquals(9001L, trace.loginUserId());
        assertEquals(10001L, trace.actualEmployeeId());
        assertEquals(10001L, trace.signatureEmployeeId());
        assertEquals(501L, trace.deviceId());
        assertEquals(301L, trace.workstationId());
        assertEquals(101L, trace.routeId());
        assertEquals(1001L, trace.routeProcessId());
        assertEquals(201L, trace.processId());
        assertEquals("TPL-201-E1001", trace.templateNo());
    }

}
