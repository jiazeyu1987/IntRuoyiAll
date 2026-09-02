package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAuditDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAuditMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.audit.DccRegistrationCertificateReadAuditCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.audit.DccRegistrationCertificateReadAuditService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_AUDIT_EVENT_CONFLICT;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import({DccRegistrationCertificateReadAuditService.class, DccRegistrationCertificateBusinessClock.class})
class DccRegistrationCertificateAuditServiceTest extends BaseDbUnitTest {

    @Resource
    private DccRegistrationCertificateReadAuditService readAuditService;
    @Resource
    private DccRegistrationCertificateAuditMapper auditMapper;

    @Test
    void readAuditRequiresTraceBeforeCreatingAnyEventKey() {
        assertThrows(IllegalArgumentException.class,
                () -> readAuditService.record(successCommand(null, 1001L)));
    }

    @Test
    void readAuditPersistsTrustedSuccessAndRejectsDuplicateEventKey() {
        readAuditService.record(successCommand("REQ-DETAIL-001", 1001L));

        DccRegistrationCertificateAuditDO stored = auditMapper.selectByTenantIdAndEventKey(
                1L, "REQ-DETAIL-001:DETAIL:CERTIFICATE:1001:SUCCESS");
        assertNotNull(stored);
        assertEquals(10L, stored.getOwnerCompanyId());
        assertEquals(1001L, stored.getCertificateId());
        assertEquals(2001L, stored.getVersionId());
        assertEquals(3001L, stored.getSnapshotId());
        assertEquals("DETAIL_SUCCEEDED", stored.getEventType());
        assertEquals("SUCCESS", stored.getResult());
        assertEquals("OK", stored.getResultCode());
        assertEquals("REQ-DETAIL-001", stored.getRequestTraceId());
        assertEquals("99", stored.getCreator());

        ServiceException duplicate = assertThrows(ServiceException.class,
                () -> readAuditService.record(successCommand("REQ-DETAIL-001", 1001L)));
        assertEquals(REGISTRATION_CERTIFICATE_AUDIT_EVENT_CONFLICT.getCode(), duplicate.getCode());
    }

    @Test
    void repeatableListReadKeepsSingleAuditEventForDuplicatePageSuccess() {
        readAuditService.recordRepeatableListRead(successCommand("REQ-PAGE-001", 1001L, "PAGE"));

        assertDoesNotThrow(() -> readAuditService.recordRepeatableListRead(
                successCommand("REQ-PAGE-001", 1001L, "PAGE")));

        DccRegistrationCertificateAuditDO stored = auditMapper.selectByTenantIdAndEventKey(
                1L, "REQ-PAGE-001:PAGE:CERTIFICATE:1001:SUCCESS");
        assertNotNull(stored);
        assertEquals("PAGE_SUCCEEDED", stored.getEventType());
        assertEquals(1, auditMapper.selectListByCertificateId(1001L).size());
    }

    @Test
    void readAuditPersistsFailureWithRequestedIdentityOnly() {
        readAuditService.record(DccRegistrationCertificateReadAuditCommand.builder()
                .tenantId(1L)
                .requestedCertificateId(9001L)
                .operation("DETAIL")
                .actorId(99L)
                .result("FAILURE")
                .resultCode("108000208")
                .requestTraceId("REQ-HIDDEN-001")
                .detailJson("{\"reason\":\"hidden\"}")
                .build());

        DccRegistrationCertificateAuditDO stored = auditMapper.selectByTenantIdAndEventKey(
                1L, "REQ-HIDDEN-001:DETAIL:REQUESTED:9001:FAILURE");
        assertNotNull(stored);
        assertNull(stored.getOwnerCompanyId());
        assertNull(stored.getCertificateId());
        assertEquals(9001L, stored.getRequestedCertificateId());
        assertEquals("DETAIL_FAILED", stored.getEventType());
        assertEquals("FAILURE", stored.getResult());
        assertEquals("108000208", stored.getResultCode());
    }

    private static DccRegistrationCertificateReadAuditCommand successCommand(String traceId, Long certificateId) {
        return successCommand(traceId, certificateId, "DETAIL");
    }

    private static DccRegistrationCertificateReadAuditCommand successCommand(
            String traceId, Long certificateId, String operation) {
        return DccRegistrationCertificateReadAuditCommand.builder()
                .tenantId(1L)
                .ownerCompanyId(10L)
                .certificateId(certificateId)
                .versionId(2001L)
                .snapshotId(3001L)
                .operation(operation)
                .actorId(99L)
                .result("SUCCESS")
                .resultCode("OK")
                .requestTraceId(traceId)
                .detailJson("{\"source\":\"query\"}")
                .build();
    }
}
