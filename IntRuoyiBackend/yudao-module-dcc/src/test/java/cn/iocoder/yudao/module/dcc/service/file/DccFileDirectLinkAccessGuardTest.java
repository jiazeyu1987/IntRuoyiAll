package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAccessAuditService;
import cn.iocoder.yudao.module.dcc.service.audit.DccDirectLinkDeniedLogCreateCommand;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.access.FileDirectLinkAccessContext;
import cn.iocoder.yudao.module.infra.service.file.access.FileDirectLinkAccessGuard;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccFileDirectLinkAccessGuardTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileQueryService controlledFileQueryService;
    @Mock
    private DccControlledFileAccessAuditService accessAuditService;

    @InjectMocks
    private DccFileDirectLinkAccessGuard guard;

    @Test
    void assertAllowed_whenScopeControlled_blocksByBusinessReferenceNotPathPrefix() {
        FileDO file = FileDO.builder()
                .id(700L)
                .configId(10L)
                .path("ordinary-looking/path/spec.pdf")
                .build();
        when(controlledFileQueryService.identifyControlledFileScope(700L))
                .thenReturn(new DccControlledFileScope(700L, List.of(
                        new DccControlledFileArtifactReference(900L, 122L, DccControlledFileArtifactRole.PUBLISHED))));
        FileDirectLinkAccessContext context = new FileDirectLinkAccessContext("10.0.0.7",
                "Playwright-E2E", "REQ-DIRECT-001");

        assertThrows(FileDirectLinkAccessGuard.ControlledFileDirectLinkBlockedException.class,
                () -> guard.assertAllowed(file, context));
        verify(controlledFileQueryService).identifyControlledFileScope(700L);
        verify(accessAuditService).recordDirectLinkDeniedLog(argThat(command ->
                command.controlledFileId().equals(900L)
                        && command.tenantId().equals(122L)
                        && command.infraFileId().equals(700L)
                        && "PUBLISHED".equals(command.artifactRole())
                        && "DIRECT_LINK".equals(command.actionType())
                        && "INFRA_DIRECT_LINK".equals(command.purpose())
                        && "DENIED".equals(command.result())
                        && "DCC_DIRECT_LINK_BLOCKED".equals(command.failureCode())
                        && "10.0.0.7".equals(command.sourceIp())
                        && "Playwright-E2E".equals(command.userAgent())
                        && "REQ-DIRECT-001".equals(command.requestId())));
    }

    @Test
    void assertAllowed_whenScopeNotControlled_allowsOrdinaryFile() {
        FileDO file = FileDO.builder()
                .id(701L)
                .configId(10L)
                .path("INT/RE/not-controlled-by-business-reference.pdf")
                .build();
        when(controlledFileQueryService.identifyControlledFileScope(701L))
                .thenReturn(new DccControlledFileScope(701L, List.of()));

        guard.assertAllowed(file, new FileDirectLinkAccessContext("10.0.0.8", "Playwright-E2E",
                "REQ-DIRECT-002"));

        verify(controlledFileQueryService).identifyControlledFileScope(701L);
        verify(accessAuditService, never()).recordDirectLinkDeniedLog(
                org.mockito.ArgumentMatchers.any(DccDirectLinkDeniedLogCreateCommand.class));
    }
}
