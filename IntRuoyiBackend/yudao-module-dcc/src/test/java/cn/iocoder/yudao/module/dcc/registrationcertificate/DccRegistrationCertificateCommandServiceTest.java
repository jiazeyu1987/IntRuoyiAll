package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAuditDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotEntrustedMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateAuditDetail;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateCommandContext;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateCommandMetadata;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateCommandMutex;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateCommandService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateCommandTransactionService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateDraftData;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateDraftRepository;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateDraftState;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateFailureAuditService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificatePrerequisiteValidator;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateResolvedDraft;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateTerminalAuditService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.formalization.DccRegistrationCertificateFormalizationService;
import cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectCodeService;
import cn.iocoder.yudao.module.mdm.api.companyscope.MdmCompanyScopeApi;
import cn.iocoder.yudao.module.mdm.api.enterprise.MdmEnterpriseApi;
import cn.iocoder.yudao.module.mdm.api.enterprise.dto.MdmEnterpriseRespDTO;
import cn.iocoder.yudao.module.mdm.api.product.MdmProductApi;
import cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_APPROVAL_DATE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_DATE_ORDER_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_DRAFT_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FIRST_OBTAINED_DATE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PRODUCT_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PRODUCTION_RELATION_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECT_CODE_DISABLED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECT_CODE_PRODUCT_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECT_CODE_TENANT_MISMATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DccRegistrationCertificateCommandServiceTest {

    @Mock
    private DccRegistrationCertificateCommandTransactionService transactionService;
    @Mock
    private DccRegistrationCertificateFailureAuditService failureAuditService;
    @Mock
    private DccRegistrationCertificateTerminalAuditService terminalAuditService;
    @Mock
    private MdmCompanyScopeApi companyScopeApi;
    @Mock
    private MdmEnterpriseApi enterpriseApi;
    @Mock
    private MdmProductApi productApi;
    @Mock
    private DccProjectCodeService projectCodeService;

    private DccRegistrationCertificateCommandService service;

    @BeforeEach
    void setUp() {
        service = new DccRegistrationCertificateCommandService(
                transactionService, failureAuditService, terminalAuditService,
                new DccRegistrationCertificateCommandMutex());
    }

    @Test
    void createDraft_sameKeyAndPayloadReplaysTheSameCertificateWithoutDependencies() {
        DccRegistrationCertificateDraftData draft = validDraft();
        when(terminalAuditService.find(1L, "create-1")).thenReturn(null);
        when(transactionService.createDraft(any(), any(), eq(draft))).thenReturn(1001L);

        assertEquals(1001L, service.createDraft(1L, 99L, "create-1", "trace-1", draft));
        ArgumentCaptor<DccRegistrationCertificateCommandMetadata> metadata =
                ArgumentCaptor.forClass(DccRegistrationCertificateCommandMetadata.class);
        verify(transactionService).createDraft(metadata.capture(), any(), eq(draft));

        reset(transactionService);
        when(terminalAuditService.find(1L, "create-1")).thenReturn(successAudit(metadata.getValue(), 1001L));
        assertEquals(1001L, service.createDraft(1L, 99L, "create-1", "trace-2", draft));
        verify(transactionService, never()).createDraft(any(), any(), any());
    }

    @Test
    void sameKeyWithDifferentActorFailsBeforeDependencies() {
        DccRegistrationCertificateDraftData draft = validDraft();
        DccRegistrationCertificateCommandMetadata first = captureMetadata(draft);
        reset(transactionService);
        when(terminalAuditService.find(1L, "create-1")).thenReturn(successAudit(first, 1001L));

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.createDraft(1L, 100L, "create-1", "trace-2", draft));

        assertEquals(REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT.getCode(), error.getCode());
        verify(transactionService, never()).createDraft(any(), any(), any());
    }

    @Test
    void sameKeyWithDifferentPayloadOrCommandFailsBeforeDependencies() {
        DccRegistrationCertificateDraftData draft = validDraft();
        DccRegistrationCertificateCommandMetadata first = captureMetadata(draft);
        reset(transactionService);
        when(terminalAuditService.find(1L, "create-1")).thenReturn(successAudit(first, 1001L));

        ServiceException payloadConflict = assertThrows(ServiceException.class,
                () -> service.createDraft(1L, 99L, "create-1", "trace-2",
                        copyDraft(draft, draft.firstObtainedDate(), draft.approvalDate(),
                                draft.effectiveDate(), draft.expiryDate(), null, "CERT-002",
                                draft.entrustedProduction(), draft.selfProduction(),
                                draft.entrustedEnterpriseIds())));
        ServiceException commandConflict = assertThrows(ServiceException.class,
                () -> service.formalize(1L, 99L, "create-1", "trace-3", 1001L, 1, 1, 5001L));

        assertEquals(REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT.getCode(), payloadConflict.getCode());
        assertEquals(REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT.getCode(), commandConflict.getCode());
        verify(transactionService, never()).createDraft(any(), any(), any());
        verify(transactionService, never()).formalize(any(), any(), any(), any(), any(), any());
    }

    @Test
    void concurrentSameKeyArbitratesOnceAndReplaysWithoutCallingDependenciesAgain() throws Exception {
        DccRegistrationCertificateDraftData draft = validDraft();
        AtomicReference<DccRegistrationCertificateCommandMetadata> committed = new AtomicReference<>();
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        when(terminalAuditService.find(1L, "concurrent-1")).thenAnswer(invocation -> {
            DccRegistrationCertificateCommandMetadata metadata = committed.get();
            return metadata == null ? null : successAudit(metadata, 1001L);
        });
        when(transactionService.createDraft(any(), any(), eq(draft))).thenAnswer(invocation -> {
            committed.set(invocation.getArgument(0));
            firstEntered.countDown();
            if (!releaseFirst.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("concurrent command was not released");
            }
            return 1001L;
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Long> first = executor.submit(
                    () -> service.createDraft(1L, 99L, "concurrent-1", "trace-1", draft));
            assertTrue(firstEntered.await(5, TimeUnit.SECONDS));
            Future<Long> replay = executor.submit(
                    () -> service.createDraft(1L, 99L, "concurrent-1", "trace-2", draft));
            releaseFirst.countDown();

            assertEquals(1001L, first.get(5, TimeUnit.SECONDS));
            assertEquals(1001L, replay.get(5, TimeUnit.SECONDS));
            verify(transactionService, times(1)).createDraft(any(), any(), eq(draft));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void failedCommandPersistsOneTerminalFailureAndRethrowsTheStableError() {
        DccRegistrationCertificateDraftData draft = validDraft();
        when(terminalAuditService.find(1L, "create-failure")).thenReturn(null);
        ServiceException expected = new ServiceException(REGISTRATION_CERTIFICATE_PRODUCT_INVALID);
        when(transactionService.createDraft(any(), any(), eq(draft))).thenThrow(expected);

        ServiceException actual = assertThrows(ServiceException.class,
                () -> service.createDraft(1L, 99L, "create-failure", "trace-failure", draft));

        assertSame(expected, actual);
        verify(failureAuditService).recordFailure(any(), any(),
                eq(REGISTRATION_CERTIFICATE_PRODUCT_INVALID.getCode()),
                eq(REGISTRATION_CERTIFICATE_PRODUCT_INVALID.getMsg()));
    }

    @Test
    void failureAuditPersistenceFailureIsVisibleAndDoesNotBecomeSuccess() {
        DccRegistrationCertificateDraftData draft = validDraft();
        when(terminalAuditService.find(1L, "audit-failure")).thenReturn(null);
        when(transactionService.createDraft(any(), any(), eq(draft)))
                .thenThrow(new ServiceException(REGISTRATION_CERTIFICATE_PRODUCT_INVALID));
        IllegalStateException auditFailure = new IllegalStateException("audit unavailable");
        doThrow(auditFailure).when(failureAuditService)
                .recordFailure(any(), any(), any(), any());

        IllegalStateException actual = assertThrows(IllegalStateException.class,
                () -> service.createDraft(1L, 99L, "audit-failure", "trace-failure", draft));

        assertSame(auditFailure, actual);
        assertEquals(1, actual.getSuppressed().length);
        assertTrue(actual.getSuppressed()[0] instanceof ServiceException);
    }

    @Test
    void transactionWorkerDeclaresRequiredRollbackBoundaryForEveryWriteCommand() {
        for (String method : List.of("createDraft", "updateDraft", "deleteDraft", "formalize")) {
            Transactional annotation = java.util.Arrays.stream(
                            DccRegistrationCertificateCommandTransactionService.class.getDeclaredMethods())
                    .filter(candidate -> candidate.getName().equals(method))
                    .findFirst().orElseThrow().getAnnotation(Transactional.class);
            assertNotNull(annotation, method + " must join a REQUIRED transaction");
            assertEquals(org.springframework.transaction.annotation.Propagation.REQUIRED, annotation.propagation());
            assertEquals(Exception.class, annotation.rollbackFor()[0]);
        }
    }

    @Test
    void updateDraft_refreshesTrustedAuditIdentityAfterChangingTheDraftOwnerCompany() {
        DccRegistrationCertificateMapper certificateMapper = mock(DccRegistrationCertificateMapper.class);
        DccRegistrationCertificateVersionMapper versionMapper = mock(DccRegistrationCertificateVersionMapper.class);
        DccRegistrationCertificateSnapshotMapper snapshotMapper = mock(DccRegistrationCertificateSnapshotMapper.class);
        DccRegistrationCertificateSnapshotEntrustedMapper entrustedMapper =
                mock(DccRegistrationCertificateSnapshotEntrustedMapper.class);
        DccRegistrationCertificateDraftRepository repository = mock(DccRegistrationCertificateDraftRepository.class);
        DccRegistrationCertificatePrerequisiteValidator validator =
                mock(DccRegistrationCertificatePrerequisiteValidator.class);
        DccRegistrationCertificateFormalizationService formalization =
                mock(DccRegistrationCertificateFormalizationService.class);
        DccRegistrationCertificateTerminalAuditService audit =
                mock(DccRegistrationCertificateTerminalAuditService.class);
        DccRegistrationCertificateCommandTransactionService worker =
                new DccRegistrationCertificateCommandTransactionService(
                        certificateMapper, versionMapper, snapshotMapper, entrustedMapper,
                        repository, validator, formalization, audit);
        DccRegistrationCertificateDraftData updated = copyDraft(
                validDraft(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 9, 1), LocalDate.of(2031, 9, 1), null, "CERT-001",
                true, false, List.of(30L), 11L);
        DccRegistrationCertificateDraftState state = draftState(10L, "DRAFT");
        DccRegistrationCertificateCommandContext context =
                new DccRegistrationCertificateCommandContext(11L, 1001L);
        DccRegistrationCertificateCommandMetadata metadata =
                new DccRegistrationCertificateCommandMetadata(1L, 99L, "update-1", "trace-1",
                        "DRAFT_UPDATE", "hash");
        when(repository.load(1L, 1001L, 1, 1, context)).thenAnswer(invocation -> {
            context.resolveTrustedIdentity(10L, 1001L);
            return state;
        });
        when(validator.validate(1L, 99L, updated)).thenReturn(mock(DccRegistrationCertificateResolvedDraft.class));

        assertEquals(1001L, worker.updateDraft(metadata, context, 1001L, 1, 1, updated));

        assertEquals(11L, context.ownerCompanyId());
        assertEquals(1001L, context.certificateId());
        verify(audit).recordSuccess(metadata, context, 2001L, 3001L, null);
    }

    @Test
    void draftLoadRejectsFormalRowsBeforeReadingOrMutatingChildren() {
        DccRegistrationCertificateMapper certificateMapper = mock(DccRegistrationCertificateMapper.class);
        DccRegistrationCertificateVersionMapper versionMapper = mock(DccRegistrationCertificateVersionMapper.class);
        DccRegistrationCertificateSnapshotMapper snapshotMapper = mock(DccRegistrationCertificateSnapshotMapper.class);
        DccRegistrationCertificateSnapshotEntrustedMapper entrustedMapper =
                mock(DccRegistrationCertificateSnapshotEntrustedMapper.class);
        DccRegistrationCertificateDraftRepository repository = new DccRegistrationCertificateDraftRepository(
                certificateMapper, versionMapper, snapshotMapper, entrustedMapper);
        DccRegistrationCertificateDO formal = draftState(10L, "ACTIVE").certificate();
        when(certificateMapper.selectById(1001L)).thenReturn(formal);
        DccRegistrationCertificateCommandContext context =
                new DccRegistrationCertificateCommandContext(null, 1001L);

        ServiceException error = assertThrows(ServiceException.class,
                () -> repository.load(1L, 1001L, 1, 1, context));

        assertEquals(REGISTRATION_CERTIFICATE_DRAFT_NOT_EXISTS.getCode(), error.getCode());
        assertNull(context.ownerCompanyId());
        assertNull(context.certificateId());
        verify(versionMapper, never()).selectOne(any());
        verify(snapshotMapper, never()).selectListByVersionId(any());
    }

    @Test
    void validatorUsesFixedBusinessDateForAllD008Boundaries() {
        DccRegistrationCertificatePrerequisiteValidator validator = validatorAt("2026-08-17T01:00:00Z");
        DccRegistrationCertificateDraftData base = validDraft();

        ServiceException order = assertThrows(ServiceException.class, () -> validator.validate(1L, 99L,
                copyDraft(base, LocalDate.of(2026, 2, 2), LocalDate.of(2026, 2, 1),
                        LocalDate.of(2026, 9, 1), LocalDate.of(2031, 9, 1), null, "CERT-001",
                        true, false, List.of(30L))));
        ServiceException firstFuture = assertThrows(ServiceException.class, () -> validator.validate(1L, 99L,
                copyDraft(base, LocalDate.of(2026, 8, 18), LocalDate.of(2026, 8, 18),
                        LocalDate.of(2026, 8, 19), LocalDate.of(2031, 8, 19), null, "CERT-001",
                        true, false, List.of(30L))));
        ServiceException approvalFuture = assertThrows(ServiceException.class, () -> validator.validate(1L, 99L,
                copyDraft(base, LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 18),
                        LocalDate.of(2026, 8, 19), LocalDate.of(2031, 8, 19), null, "CERT-001",
                        true, false, List.of(30L))));

        assertEquals(REGISTRATION_CERTIFICATE_DATE_ORDER_INVALID.getCode(), order.getCode());
        assertEquals(REGISTRATION_CERTIFICATE_FIRST_OBTAINED_DATE_INVALID.getCode(), firstFuture.getCode());
        assertEquals(REGISTRATION_CERTIFICATE_APPROVAL_DATE_INVALID.getCode(), approvalFuture.getCode());
    }

    @Test
    void businessClockRejectsAnyZoneOtherThanAsiaShanghai() {
        assertThrows(IllegalArgumentException.class,
                () -> new DccRegistrationCertificateBusinessClock(
                        Clock.fixed(Instant.parse("2026-08-17T01:00:00Z"), ZoneId.of("UTC"))));
    }

    @Test
    void validatorAllowsNoProjectCodeWithoutGrantingAProjectVisibilityLookup() {
        DccRegistrationCertificatePrerequisiteValidator validator = validValidator();

        DccRegistrationCertificateResolvedDraft resolved = validator.validate(1L, 99L, validDraft());

        assertEquals("Product A", resolved.productName());
        verify(projectCodeService, never()).getProjectCode(any(), any());
    }

    @Test
    void validatorRejectsDisabledCrossTenantAndProductReboundProjectCodes() {
        DccRegistrationCertificatePrerequisiteValidator validator = validValidator();
        DccRegistrationCertificateDraftData draft = copyDraft(
                validDraft(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 9, 1), LocalDate.of(2031, 9, 1), 40L, "CERT-001",
                true, false, List.of(30L));
        DccProjectCodeDO code = projectCode(1L, 20L, DccProjectCodeStatusConstants.DISABLE);
        when(projectCodeService.getProjectCode(99L, 40L)).thenReturn(code);
        ServiceException disabled = assertThrows(ServiceException.class, () -> validator.validate(1L, 99L, draft));

        code.setStatus(DccProjectCodeStatusConstants.ENABLE);
        code.setTenantId(2L);
        ServiceException crossTenant = assertThrows(ServiceException.class,
                () -> validator.validate(1L, 99L, draft));

        code.setTenantId(1L);
        code.setProductMasterId(21L);
        ServiceException rebound = assertThrows(ServiceException.class,
                () -> validator.validate(1L, 99L, draft));

        assertEquals(REGISTRATION_CERTIFICATE_PROJECT_CODE_DISABLED.getCode(), disabled.getCode());
        assertEquals(REGISTRATION_CERTIFICATE_PROJECT_CODE_TENANT_MISMATCH.getCode(), crossTenant.getCode());
        assertEquals(REGISTRATION_CERTIFICATE_PROJECT_CODE_PRODUCT_MISMATCH.getCode(), rebound.getCode());
        verify(projectCodeService, times(3)).getProjectCode(99L, 40L);
    }

    @Test
    void validatorFailsClosedForCompanyScopeAndInvalidProductionRelation() {
        DccRegistrationCertificatePrerequisiteValidator validator = validValidator();
        doThrow(new IllegalStateException("company denied"))
                .when(companyScopeApi).validateUserCompanyAccess(99L, 10L);

        ServiceException companyDenied = assertThrows(ServiceException.class,
                () -> validator.validate(1L, 99L, validDraft()));
        assertEquals(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED.getCode(), companyDenied.getCode());

        reset(companyScopeApi);
        DccRegistrationCertificateDraftData neitherProductionMode = copyDraft(
                validDraft(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 9, 1), LocalDate.of(2031, 9, 1), null, "CERT-001",
                false, false, List.of());
        ServiceException invalidRelation = assertThrows(ServiceException.class,
                () -> validator.validate(1L, 99L, neitherProductionMode));
        assertEquals(REGISTRATION_CERTIFICATE_PRODUCTION_RELATION_INVALID.getCode(), invalidRelation.getCode());
    }

    private DccRegistrationCertificateCommandMetadata captureMetadata(DccRegistrationCertificateDraftData draft) {
        when(terminalAuditService.find(1L, "create-1")).thenReturn(null);
        when(transactionService.createDraft(any(), any(), eq(draft))).thenReturn(1001L);
        service.createDraft(1L, 99L, "create-1", "trace-1", draft);
        ArgumentCaptor<DccRegistrationCertificateCommandMetadata> metadata =
                ArgumentCaptor.forClass(DccRegistrationCertificateCommandMetadata.class);
        verify(transactionService).createDraft(metadata.capture(), any(), eq(draft));
        return metadata.getValue();
    }

    private DccRegistrationCertificatePrerequisiteValidator validatorAt(String instant) {
        return new DccRegistrationCertificatePrerequisiteValidator(
                companyScopeApi, enterpriseApi, productApi, projectCodeService,
                new DccRegistrationCertificateBusinessClock(
                        Clock.fixed(Instant.parse(instant), ZoneId.of("Asia/Shanghai"))));
    }

    private DccRegistrationCertificatePrerequisiteValidator validValidator() {
        lenient().when(enterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(MdmEnterpriseRespDTO.builder()
                        .id(10L).tenantId(1L).name("Owner Company").build()));
        lenient().when(enterpriseApi.getEnabledEnterprises(eq(List.of(30L)), any()))
                .thenReturn(List.of(MdmEnterpriseRespDTO.builder()
                        .id(30L).tenantId(1L).name("Factory A").build()));
        lenient().when(productApi.getEnabledDccProduct(20L)).thenReturn(
                MdmProductRespDTO.builder().id(20L).nameCn("Product A").build());
        return validatorAt("2026-08-17T01:00:00Z");
    }

    private static DccProjectCodeDO projectCode(Long tenantId, Long productMasterId, String status) {
        DccProjectCodeDO projectCode = DccProjectCodeDO.builder()
                .id(40L).productMasterId(productMasterId).status(status).build();
        projectCode.setTenantId(tenantId);
        return projectCode;
    }

    private static DccRegistrationCertificateDraftState draftState(Long ownerCompanyId, String status) {
        DccRegistrationCertificateDO certificate = DccRegistrationCertificateDO.builder()
                .id(1001L).ownerCompanyId(ownerCompanyId).productMasterId(20L)
                .status(status).rowVersion(1).build();
        certificate.setTenantId(1L);
        DccRegistrationCertificateVersionDO version = DccRegistrationCertificateVersionDO.builder()
                .id(2001L).certificateId(1001L).versionNo(1).status("DRAFT").build();
        version.setTenantId(1L);
        DccRegistrationCertificateSnapshotDO snapshot = DccRegistrationCertificateSnapshotDO.builder()
                .id(3001L).versionId(2001L).revisionNo(1).build();
        snapshot.setTenantId(1L);
        return new DccRegistrationCertificateDraftState(certificate, version, snapshot, List.of());
    }

    private static DccRegistrationCertificateAuditDO successAudit(
            DccRegistrationCertificateCommandMetadata metadata, Long certificateId) {
        return DccRegistrationCertificateAuditDO.builder()
                .tenantId(metadata.tenantId())
                .ownerCompanyId(10L)
                .certificateId(certificateId)
                .eventKey(metadata.idempotencyKey())
                .eventType(metadata.commandKind() + "_SUCCEEDED")
                .actorId(metadata.actorId())
                .result("SUCCESS")
                .resultCode("OK")
                .requestTraceId("trace-1")
                .detailJson(JsonUtils.toJsonString(new DccRegistrationCertificateAuditDetail(
                        metadata.commandKind(), metadata.actorId(), metadata.payloadHash(),
                        certificateId, null, null)))
                .build();
    }

    static DccRegistrationCertificateDraftData validDraft() {
        return new DccRegistrationCertificateDraftData(
                10L, 20L, null,
                LocalDate.of(2026, 1, 1), "CERT-001",
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 9, 1), LocalDate.of(2031, 9, 1),
                "II", "Registrant", "Model", "Structure", "Use", "Requirements",
                "Residence", "Production", true, false, List.of(30L));
    }

    private static DccRegistrationCertificateDraftData copyDraft(
            DccRegistrationCertificateDraftData source,
            LocalDate firstObtainedDate, LocalDate approvalDate, LocalDate effectiveDate, LocalDate expiryDate,
            Long projectCodeId, String certificateNo, Boolean entrustedProduction, Boolean selfProduction,
            List<Long> entrustedEnterpriseIds) {
        return copyDraft(source, firstObtainedDate, approvalDate, effectiveDate, expiryDate,
                projectCodeId, certificateNo, entrustedProduction, selfProduction,
                entrustedEnterpriseIds, source.ownerCompanyId());
    }

    private static DccRegistrationCertificateDraftData copyDraft(
            DccRegistrationCertificateDraftData source,
            LocalDate firstObtainedDate, LocalDate approvalDate, LocalDate effectiveDate, LocalDate expiryDate,
            Long projectCodeId, String certificateNo, Boolean entrustedProduction, Boolean selfProduction,
            List<Long> entrustedEnterpriseIds, Long ownerCompanyId) {
        return new DccRegistrationCertificateDraftData(
                ownerCompanyId, source.productMasterId(), projectCodeId, firstObtainedDate,
                certificateNo, approvalDate, effectiveDate, expiryDate, source.classification(),
                source.registrantName(), source.modelSpecification(), source.structureComposition(),
                source.intendedUse(), source.technicalRequirements(), source.residenceAddress(),
                source.productionAddress(), entrustedProduction, selfProduction, entrustedEnterpriseIds);
    }
}
