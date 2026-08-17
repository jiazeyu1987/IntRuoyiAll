package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAuditDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotEntrustedDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAuditMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateFileMapper;
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
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentRegistrationProjectionService;
import jakarta.annotation.Resource;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_APPROVAL_DATE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_DATE_ORDER_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_DRAFT_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FIRST_OBTAINED_DATE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PRODUCT_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PRODUCTION_RELATION_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECT_CODE_DISABLED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECT_CODE_PRODUCT_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECT_CODE_TENANT_MISMATCH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
@Import({
        DccRegistrationCertificateCommandService.class,
        DccRegistrationCertificateCommandTransactionService.class,
        DccRegistrationCertificateDraftRepository.class,
        DccRegistrationCertificatePrerequisiteValidator.class,
        DccRegistrationCertificateTerminalAuditService.class,
        DccRegistrationCertificateFailureAuditService.class,
        DccRegistrationCertificateFormalizationService.class,
        DccRegistrationCertificateCommandMutex.class,
        DccRegistrationCertificateCommandServiceTest.DbTestConfiguration.class
})
class DccRegistrationCertificateCommandServiceTest extends BaseDbUnitTest {

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

    @Resource
    private DccRegistrationCertificateCommandService dbCommandService;
    @Resource
    private DccRegistrationCertificateCommandTransactionService dbTransactionService;
    @Resource
    private DccRegistrationCertificateFailureAuditService dbFailureAuditService;
    @Resource
    private DccRegistrationCertificateTerminalAuditService dbTerminalAuditService;
    @Resource
    private DccRegistrationCertificateMapper dbCertificateMapper;
    @Resource
    private DccRegistrationCertificateVersionMapper dbVersionMapper;
    @Resource
    private DccRegistrationCertificateSnapshotMapper dbSnapshotMapper;
    @Resource
    private DccRegistrationCertificateSnapshotEntrustedMapper dbEntrustedMapper;
    @Resource
    private DccRegistrationCertificateFileMapper dbFileMapper;
    @Resource
    private DccRegistrationCertificateAuditMapper dbAuditMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private MdmCompanyScopeApi dbCompanyScopeApi;
    @MockitoBean
    private MdmEnterpriseApi dbEnterpriseApi;
    @MockitoBean
    private MdmProductApi dbProductApi;
    @MockitoBean
    private DccProjectCodeService dbProjectCodeService;
    @MockitoBean
    private ControlledContentRegistrationProjectionService dbProjectionService;

    private DccRegistrationCertificateCommandService service;

    @BeforeEach
    void setUp() {
        reset(dbCompanyScopeApi, dbEnterpriseApi, dbProductApi, dbProjectCodeService, dbProjectionService);
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
    void createDraft_sameKeyAndWhitespaceEquivalentPayloadReplaysTheSameCertificate() {
        DccRegistrationCertificateDraftData draft = validDraft();
        DccRegistrationCertificateCommandMetadata first = captureMetadata(draft);
        reset(transactionService);
        when(terminalAuditService.find(1L, "create-1")).thenReturn(successAudit(first, 1001L));

        DccRegistrationCertificateDraftData padded = new DccRegistrationCertificateDraftData(
                draft.ownerCompanyId(), draft.productMasterId(), draft.projectCodeId(),
                draft.firstObtainedDate(), "  " + draft.certificateNo() + "  ",
                draft.approvalDate(), draft.effectiveDate(), draft.expiryDate(),
                "  " + draft.classification() + "  ",
                "  " + draft.registrantName() + "  ",
                "  " + draft.modelSpecification() + "  ",
                "  " + draft.structureComposition() + "  ",
                "  " + draft.intendedUse() + "  ",
                "  " + draft.technicalRequirements() + "  ",
                "  " + draft.residenceAddress() + "  ",
                "  " + draft.productionAddress() + "  ",
                draft.entrustedProduction(), draft.selfProduction(), draft.entrustedEnterpriseIds());

        Long replayed = assertDoesNotThrow(
                () -> service.createDraft(1L, 99L, "create-1", "trace-2", padded));
        assertEquals(1001L, replayed);
        verify(transactionService, never()).createDraft(any(), any(), any());
        verify(failureAuditService, never()).recordFailure(any(), any(), any(), any());
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
    void sameKeySamePayloadFailureReplayDoesNotInvokeDependenciesAgain() {
        DccRegistrationCertificateDraftData draft = validDraft();
        DccRegistrationCertificateCommandMetadata first = captureMetadata(draft);
        reset(transactionService);
        DccRegistrationCertificateAuditDO failure = DccRegistrationCertificateAuditDO.builder()
                .tenantId(1L)
                .eventKey(first.idempotencyKey())
                .eventType("DRAFT_CREATE_FAILED")
                .actorId(first.actorId())
                .result("FAILURE")
                .resultCode(String.valueOf(REGISTRATION_CERTIFICATE_PRODUCT_INVALID.getCode()))
                .detailJson(JsonUtils.toJsonString(new DccRegistrationCertificateAuditDetail(
                        first.commandKind(), first.actorId(), first.payloadHash(), null,
                        REGISTRATION_CERTIFICATE_PRODUCT_INVALID.getCode(),
                        REGISTRATION_CERTIFICATE_PRODUCT_INVALID.getMsg())))
                .build();
        when(terminalAuditService.find(1L, "create-1")).thenReturn(failure);

        ServiceException replay = assertThrows(ServiceException.class,
                () -> service.createDraft(1L, 99L, "create-1", "trace-replay", draft));

        assertEquals(REGISTRATION_CERTIFICATE_PRODUCT_INVALID.getCode(), replay.getCode());
        verify(transactionService, never()).createDraft(any(), any(), any());
        verify(failureAuditService, never()).recordFailure(any(), any(), any(), any());
    }

    @Test
    void formalizeProjectionFailureRollsBackAllSuccessFactsAndPersistsRequiresNewFailureAudit() {
        configureDbValidDependencies();
        DraftFixture fixture = seedDraft(LocalDate.of(2026, 8, 1));
        IllegalStateException projectionFailure = new IllegalStateException("projection drift");
        when(dbProjectionService.registerActive(
                any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(projectionFailure);

        ServiceException error = assertThrows(ServiceException.class,
                () -> dbCommandService.formalize(1L, 99L, "formalize-rollback", "trace-formalize",
                        fixture.certificateId(), 1, 1, fixture.fileId()));

        assertSame(projectionFailure, error.getCause());
        assertDraftBusinessFacts(fixture);
        DccRegistrationCertificateAuditDO audit =
                dbAuditMapper.selectByTenantIdAndEventKey(1L, "formalize-rollback");
        assertNotNull(audit);
        assertEquals("FAILURE", audit.getResult());
        assertEquals(fixture.certificateId(), audit.getCertificateId());
        assertEquals(0, count("SELECT COUNT(*) FROM dcc_registration_certificate_audit "
                + "WHERE tenant_id = 1 AND event_key = ? AND result = 'SUCCESS'", "formalize-rollback"));
    }

    @Test
    void independentServicesUseDatabaseUniqueKeyToReplaySamePayloadWinner() throws Exception {
        configureDbValidDependenciesWithProductBarrier();
        DccRegistrationCertificateCommandService first = independentDbCommandService();
        DccRegistrationCertificateCommandService second = independentDbCommandService();
        DccRegistrationCertificateDraftData draft = validDraft();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Long> firstResult = executor.submit(
                    () -> first.createDraft(1L, 99L, "db-race-same", "trace-a", draft));
            Future<Long> secondResult = executor.submit(
                    () -> second.createDraft(1L, 99L, "db-race-same", "trace-b", draft));

            assertEquals(firstResult.get(10, TimeUnit.SECONDS), secondResult.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate WHERE tenant_id = 1"));
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_audit "
                + "WHERE tenant_id = 1 AND event_key = ? AND result = 'SUCCESS'", "db-race-same"));
        assertEquals(0, count("SELECT COUNT(*) FROM dcc_registration_certificate_audit "
                + "WHERE tenant_id = 1 AND event_key = ? AND result = 'FAILURE'", "db-race-same"));
    }

    @Test
    void independentServicesUseDatabaseUniqueKeyToRejectDifferentPayloadWithoutFailureAudit() throws Exception {
        configureDbValidDependenciesWithProductBarrier();
        DccRegistrationCertificateCommandService first = independentDbCommandService();
        DccRegistrationCertificateCommandService second = independentDbCommandService();
        DccRegistrationCertificateDraftData firstDraft = validDraft();
        DccRegistrationCertificateDraftData secondDraft = copyDraft(
                firstDraft, firstDraft.firstObtainedDate(), firstDraft.approvalDate(),
                firstDraft.effectiveDate(), firstDraft.expiryDate(), null, "CERT-002",
                true, false, List.of(30L));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Object firstResult;
        Object secondResult;
        try {
            Future<Long> firstFuture = executor.submit(
                    () -> first.createDraft(1L, 99L, "db-race-different", "trace-a", firstDraft));
            Future<Long> secondFuture = executor.submit(
                    () -> second.createDraft(1L, 99L, "db-race-different", "trace-b", secondDraft));
            firstResult = futureOutcome(firstFuture);
            secondResult = futureOutcome(secondFuture);
        } finally {
            executor.shutdownNow();
        }

        assertTrue(firstResult instanceof Long || secondResult instanceof Long);
        Object loser = firstResult instanceof Long ? secondResult : firstResult;
        assertTrue(loser instanceof ServiceException);
        assertEquals(REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT.getCode(),
                ((ServiceException) loser).getCode());
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate WHERE tenant_id = 1"));
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_audit "
                + "WHERE tenant_id = 1 AND event_key = ? AND result = 'SUCCESS'", "db-race-different"));
        assertEquals(0, count("SELECT COUNT(*) FROM dcc_registration_certificate_audit "
                + "WHERE tenant_id = 1 AND event_key = ? AND result = 'FAILURE'", "db-race-different"));
    }

    @Test
    void infrastructureFailuresFromAllFormalPrerequisitesRemainVisible() {
        for (String dependency : List.of("company", "owner", "product", "project")) {
            reset(dbCompanyScopeApi, dbEnterpriseApi, dbProductApi, dbProjectCodeService);
            configureDbValidDependencies();
            IllegalStateException outage = new IllegalStateException(dependency + " unavailable");
            DccRegistrationCertificateDraftData draft = validDraft();
            if ("company".equals(dependency)) {
                doThrow(outage).when(dbCompanyScopeApi).validateUserCompanyAccess(99L, 10L);
            } else if ("owner".equals(dependency)) {
                when(dbEnterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any())).thenThrow(outage);
            } else if ("product".equals(dependency)) {
                when(dbProductApi.getEnabledDccProduct(20L)).thenThrow(outage);
            } else {
                draft = copyDraft(draft, draft.firstObtainedDate(), draft.approvalDate(), draft.effectiveDate(),
                        draft.expiryDate(), 40L, draft.certificateNo(), true, false, List.of(30L));
                when(dbProjectCodeService.getProjectCode(99L, 40L)).thenThrow(outage);
            }
            DccRegistrationCertificateDraftData request = draft;

            IllegalStateException visible = assertThrows(IllegalStateException.class,
                    () -> dbCommandService.createDraft(1L, 99L, "infra-" + dependency,
                            "trace-" + dependency, request));

            assertSame(outage, visible);
            assertEquals(0, count("SELECT COUNT(*) FROM dcc_registration_certificate WHERE tenant_id = 1"));
            assertNull(dbAuditMapper.selectByTenantIdAndEventKey(1L, "infra-" + dependency));
        }
    }

    @Test
    void createUpdateAndDeleteFailuresLeaveRealDatabaseBusinessFactsUnchanged() {
        configureDbValidDependencies();
        when(dbProductApi.getEnabledDccProduct(20L)).thenReturn(null);
        ServiceException createFailure = assertThrows(ServiceException.class,
                () -> dbCommandService.createDraft(1L, 99L, "create-no-partial", "trace-create", validDraft()));
        assertEquals(REGISTRATION_CERTIFICATE_PRODUCT_INVALID.getCode(), createFailure.getCode());
        assertEquals(0, count("SELECT COUNT(*) FROM dcc_registration_certificate WHERE tenant_id = 1"));
        assertEquals("FAILURE", dbAuditMapper.selectByTenantIdAndEventKey(
                1L, "create-no-partial").getResult());

        reset(dbProductApi);
        configureDbValidDependencies();
        DraftFixture fixture = seedDraft(LocalDate.of(2026, 9, 1));
        DccRegistrationCertificateDraftData update = copyDraft(
                validDraft(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 9, 1), LocalDate.of(2031, 9, 1), 40L,
                "CERT-CHANGED", true, false, List.of(30L));
        when(dbProjectCodeService.getProjectCode(99L, 40L))
                .thenReturn(projectCode(1L, 20L, DccProjectCodeStatusConstants.DISABLE));
        ServiceException updateFailure = assertThrows(ServiceException.class,
                () -> dbCommandService.updateDraft(1L, 99L, "update-no-partial", "trace-update",
                        fixture.certificateId(), 1, 1, update));
        assertEquals(REGISTRATION_CERTIFICATE_PROJECT_CODE_DISABLED.getCode(), updateFailure.getCode());
        assertDraftBusinessFacts(fixture);
        assertEquals("CERT-001", dbVersionMapper.selectById(fixture.versionId()).getCertificateNo());
        assertEquals("FAILURE", dbAuditMapper.selectByTenantIdAndEventKey(
                1L, "update-no-partial").getResult());

        ServiceException deleteFailure = assertThrows(ServiceException.class,
                () -> dbCommandService.deleteDraft(1L, 99L, "delete-no-partial", "trace-delete",
                        fixture.certificateId(), 2, 1));
        assertNotNull(deleteFailure);
        assertDraftBusinessFacts(fixture);
        assertEquals("FAILURE", dbAuditMapper.selectByTenantIdAndEventKey(
                1L, "delete-no-partial").getResult());
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
    void validatorKeepsCompanyInfrastructureFailureVisibleAndRejectsInvalidProductionRelation() {
        DccRegistrationCertificatePrerequisiteValidator validator = validValidator();
        IllegalStateException companyOutage = new IllegalStateException("company scope unavailable");
        doThrow(companyOutage)
                .when(companyScopeApi).validateUserCompanyAccess(99L, 10L);

        IllegalStateException visible = assertThrows(IllegalStateException.class,
                () -> validator.validate(1L, 99L, validDraft()));
        assertSame(companyOutage, visible);

        reset(companyScopeApi);
        DccRegistrationCertificateDraftData neitherProductionMode = copyDraft(
                validDraft(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 9, 1), LocalDate.of(2031, 9, 1), null, "CERT-001",
                false, false, List.of());
        ServiceException invalidRelation = assertThrows(ServiceException.class,
                () -> validator.validate(1L, 99L, neitherProductionMode));
        assertEquals(REGISTRATION_CERTIFICATE_PRODUCTION_RELATION_INVALID.getCode(), invalidRelation.getCode());
    }

    private DccRegistrationCertificateCommandService independentDbCommandService() {
        return new DccRegistrationCertificateCommandService(
                dbTransactionService, dbFailureAuditService, dbTerminalAuditService,
                new DccRegistrationCertificateCommandMutex());
    }

    private void configureDbValidDependencies() {
        when(dbEnterpriseApi.getEnabledEnterprises(eq(List.of(10L)), any()))
                .thenReturn(List.of(MdmEnterpriseRespDTO.builder()
                        .id(10L).tenantId(1L).name("Owner Company").build()));
        when(dbEnterpriseApi.getEnabledEnterprises(eq(List.of(30L)), any()))
                .thenReturn(List.of(MdmEnterpriseRespDTO.builder()
                        .id(30L).tenantId(1L).name("Factory A").build()));
        when(dbProductApi.getEnabledDccProduct(20L)).thenReturn(
                MdmProductRespDTO.builder().id(20L).nameCn("Product A").build());
    }

    private void configureDbValidDependenciesWithProductBarrier() {
        configureDbValidDependencies();
        CountDownLatch productCalls = new CountDownLatch(2);
        when(dbProductApi.getEnabledDccProduct(20L)).thenAnswer(invocation -> {
            productCalls.countDown();
            if (!productCalls.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("both command instances did not enter validation");
            }
            return MdmProductRespDTO.builder().id(20L).nameCn("Product A").build();
        });
    }

    private DraftFixture seedDraft(LocalDate effectiveDate) {
        DccRegistrationCertificateDO certificate = DccRegistrationCertificateDO.builder()
                .ownerCompanyId(10L)
                .productMasterId(20L)
                .firstObtainedDate(LocalDate.of(2026, 1, 1))
                .status("DRAFT")
                .rowVersion(1)
                .build();
        certificate.setTenantId(1L);
        assertEquals(1, dbCertificateMapper.insert(certificate));

        DccRegistrationCertificateVersionDO version = DccRegistrationCertificateVersionDO.builder()
                .certificateId(certificate.getId())
                .versionNo(1)
                .versionType("INITIAL_CERTIFICATE")
                .certificateNo("CERT-001")
                .approvalDate(LocalDate.of(2026, 2, 1))
                .effectiveDate(effectiveDate)
                .expiryDate(LocalDate.of(2031, 9, 1))
                .classification("II")
                .categoryChanged(false)
                .status("DRAFT")
                .build();
        version.setTenantId(1L);
        assertEquals(1, dbVersionMapper.insert(version));

        DccRegistrationCertificateSnapshotDO snapshot = DccRegistrationCertificateSnapshotDO.builder()
                .versionId(version.getId())
                .revisionNo(1)
                .productName("Product A")
                .registrantName("Registrant")
                .modelSpecification("Model")
                .structureComposition("Structure")
                .intendedUse("Use")
                .technicalRequirements("Requirements")
                .residenceAddress("Residence")
                .productionAddress("Production")
                .entrustedProduction(true)
                .selfProduction(false)
                .entrustedEnterprisesJson("[{\"enterpriseId\":30,\"enterpriseName\":\"Factory A\"}]")
                .effectiveAt(effectiveDate.atStartOfDay())
                .build();
        snapshot.setTenantId(1L);
        assertEquals(1, dbSnapshotMapper.insert(snapshot));

        DccRegistrationCertificateSnapshotEntrustedDO entrusted =
                DccRegistrationCertificateSnapshotEntrustedDO.builder()
                        .snapshotId(snapshot.getId())
                        .enterpriseId(30L)
                        .enterpriseNameSnapshot("Factory A")
                        .sortOrder(1)
                        .build();
        entrusted.setTenantId(1L);
        assertEquals(1, dbEntrustedMapper.insert(entrusted));

        DccRegistrationCertificateFileDO file = DccRegistrationCertificateFileDO.builder()
                .ownerType("VERSION")
                .ownerId(version.getId())
                .fileKind("REGISTRATION_CERTIFICATE")
                .infraFileId(7001L)
                .originalName("certificate.pdf")
                .mimeType("application/pdf")
                .fileSize(128L)
                .sha256("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .status("STAGED")
                .build();
        file.setTenantId(1L);
        assertEquals(1, dbFileMapper.insert(file));
        return new DraftFixture(certificate.getId(), version.getId(), snapshot.getId(), file.getId());
    }

    private void assertDraftBusinessFacts(DraftFixture fixture) {
        DccRegistrationCertificateDO certificate = dbCertificateMapper.selectById(fixture.certificateId());
        assertNotNull(certificate);
        assertEquals("DRAFT", certificate.getStatus());
        assertEquals(1, certificate.getRowVersion());
        assertNull(certificate.getCurrentVersionId());
        assertNull(certificate.getPendingVersionId());
        assertNull(certificate.getCurrentSnapshotId());

        DccRegistrationCertificateVersionDO version = dbVersionMapper.selectById(fixture.versionId());
        assertNotNull(version);
        assertEquals("DRAFT", version.getStatus());
        assertNull(version.getFormalizedAt());
        assertNull(version.getFormalizedBy());
        assertNotNull(dbSnapshotMapper.selectById(fixture.snapshotId()));
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_snapshot_entrusted "
                + "WHERE tenant_id = 1 AND snapshot_id = ?", fixture.snapshotId()));

        DccRegistrationCertificateFileDO file = dbFileMapper.selectById(fixture.fileId());
        assertNotNull(file);
        assertEquals("STAGED", file.getStatus());
        assertNull(file.getBoundAt());
        assertNull(file.getBoundBy());
    }

    private int count(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private static Object futureOutcome(Future<Long> future) throws Exception {
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException exception) {
            return exception.getCause();
        }
    }

    private record DraftFixture(Long certificateId, Long versionId, Long snapshotId, Long fileId) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DbTestConfiguration {

        @Bean
        DccRegistrationCertificateBusinessClock registrationCertificateBusinessClock() {
            return new DccRegistrationCertificateBusinessClock(
                    Clock.fixed(Instant.parse("2026-08-17T01:00:00Z"), ZoneId.of("Asia/Shanghai")));
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
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
