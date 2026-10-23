package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCheckinReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitIterationReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes;
import org.flowable.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_CHECKIN_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;

/** Read-only IC16 boundary verification; does not prepare or invent a LEGACY source. */
class DccW08PreparedSourceBoundaryTest {
    private static final String UNVERIFIED_ID = "unverified-definition-id";
    private static final String UNVERIFIED_DIGEST = "a".repeat(64);
    private BpmProcessDefinitionService definitions;
    private DccApplicationExecutionContractService contracts;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        TenantContextHolder.setIgnore(false);
        definitions = mock(BpmProcessDefinitionService.class);
        contracts = new DccApplicationExecutionContractService();
        ReflectionTestUtils.setField(contracts, "definitions", definitions);
    }

    @AfterEach void clear() {
        TransactionSynchronizationManager.clear();
        TenantContextHolder.clear();
    }

    @ParameterizedTest
    @ValueSource(strings = {"A/1", "A/9", "B/1"})
    void iterationNumberAndClientLegacyClaimCannotPrepareANewBody(String version) {
        var source = source(21L, version, 1L);
        var request = a21ExplicitCheckinRequest();
        request.setChangeDescription("task-owned negative boundary inspection");
        request.setNeedTraining(false);
        var nextVersion = DccControlledFileVersionPolicy.defaultPolicy()
                .parseStored(DccControlledFileDO.builder().versionNo(version + "-1").build());
        DccControlledFileDO next = ReflectionTestUtils.invokeMethod(new DccControlledFileQueryServiceImpl(),
                "copyForCheckin", source, 99L, nextVersion,
                new DccControlledFilePreparedSource(200L, 100L, "new-body-hash", true),
                "source-hash", null, null, request);
        assertNotNull(next);
        assertEquals(source.getId(), next.getPredecessorControlledFileId());
        var denied = assertThrows(IllegalStateException.class, () -> contracts.requirePrepared(next,
                DccApplicationExecutionContractService.LEGACY, UNVERIFIED_ID, UNVERIFIED_DIGEST,
                DccControlledFileProcessDefinitionKeys.UPLOAD));
        assertEquals("EXECUTION_CONTRACT_PREPARED_IDENTITY_REQUIRED", denied.getMessage());
        assertNull(next.getPreparedWorkflowContract());
        assertNull(next.getPreparedProcessDefinitionId());
        assertNull(next.getPreparedDefinitionDigest());
        verifyNoInteractions(definitions);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7})
    void projectingPartialSourceProofDoesNotMakeItAValidatedPreparation(int missingMask) {
        var selected = declaredSource();
        if ((missingMask & 1) != 0) selected.setPreparedWorkflowContract(null);
        if ((missingMask & 2) != 0) selected.setPreparedProcessDefinitionId(null);
        if ((missingMask & 4) != 0) selected.setPreparedDefinitionDigest(null);
        var revision = new DccControlledFileRevisionServiceImpl();
        var names = mock(DccControlledFileNameClaimService.class);
        when(names.requireSourceName(selected)).thenReturn(selected.getSourceOriginalFileName());
        ReflectionTestUtils.setField(revision, "nameClaimService", names);
        DccControlledFileDO projected = ReflectionTestUtils.invokeMethod(revision, "sourceIdentityProjection", selected);
        assertNotNull(projected);
        var denied = assertThrows(IllegalStateException.class, () -> contracts.requirePrepared(projected,
                projected.getPreparedWorkflowContract(), projected.getPreparedProcessDefinitionId(),
                projected.getPreparedDefinitionDigest(), DccControlledFileProcessDefinitionKeys.UPLOAD));
        assertEquals("EXECUTION_CONTRACT_PREPARED_IDENTITY_REQUIRED", denied.getMessage());
        verifyNoInteractions(definitions);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "tenant", "key", "id", "deployment"})
    void aCompleteDeclarationNeedsTheExactRealDefinitionAndNeverSelectsLatest(String invalid) {
        var selected = declaredSource();
        if (!"missing".equals(invalid)) {
            var definition = mock(ProcessDefinition.class);
            when(definition.getId()).thenReturn("id".equals(invalid) ? "different-id" : UNVERIFIED_ID);
            when(definition.getTenantId()).thenReturn("tenant".equals(invalid) ? "2" : "1");
            when(definition.getKey()).thenReturn("key".equals(invalid) ? "different-key" : DccControlledFileProcessDefinitionKeys.UPLOAD);
            when(definition.getDeploymentId()).thenReturn("deployment".equals(invalid) ? null : "offline-boundary-deployment");
            when(definitions.getProcessDefinition(UNVERIFIED_ID)).thenReturn(definition);
        }
        var denied = assertThrows(IllegalStateException.class, () -> contracts.requirePrepared(selected,
                selected.getPreparedWorkflowContract(), selected.getPreparedProcessDefinitionId(),
                selected.getPreparedDefinitionDigest(), DccControlledFileProcessDefinitionKeys.UPLOAD));
        assertEquals("EXECUTION_CONTRACT_DEFINITION_IDENTITY_INVALID", denied.getMessage());
        verify(definitions).getProcessDefinition(UNVERIFIED_ID);
        verifyNoMoreInteractions(definitions);
    }

    @ParameterizedTest
    @ValueSource(strings = {"contract", "id", "digest"})
    void changedExpectedProofCannotReplaceTheSavedSourceByRequest(String changed) {
        var selected = declaredSource();
        var denied = assertThrows(IllegalArgumentException.class, () -> contracts.requirePrepared(selected,
                "contract".equals(changed) ? DccApplicationExecutionContractService.LEGACY : selected.getPreparedWorkflowContract(),
                "id".equals(changed) ? "changed-id" : selected.getPreparedProcessDefinitionId(),
                "digest".equals(changed) ? "b".repeat(64) : selected.getPreparedDefinitionDigest(),
                DccControlledFileProcessDefinitionKeys.UPLOAD));
        assertEquals("EXECUTION_CONTRACT_EXPECTATION_STALE", denied.getMessage());
        verifyNoInteractions(definitions);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void candidateCreationWithoutATransactionStopsBeforeAnySourceOrTargetWrite(boolean initial) {
        TransactionSynchronizationManager.setActualTransactionActive(false);
        var revision = new DccControlledFileRevisionServiceImpl();
        var files = mock(DccControlledFileMapper.class);
        var storage = mock(DccControlledFileSourceOwnershipService.class);
        ReflectionTestUtils.setField(revision, "fileMapper", files);
        ReflectionTestUtils.setField(revision, "sourceOwnershipService", storage);
        var request = request(initial);
        var denied = assertThrows(IllegalStateException.class, () -> {
            if (initial) revision.createInitialCandidate(99L, 21L, request);
            else revision.createRevision(99L, 20L, 21L, request);
        });
        assertEquals("formal revision requires an active transaction", denied.getMessage());
        verifyNoInteractions(files, storage, definitions);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void aForeignTenantSelectedSourceStopsBeforePlacementCopyOrCandidateInsert(boolean initial) {
        // This flag only enables the method's early identity guard; it is not a database transaction proof.
        TransactionSynchronizationManager.setActualTransactionActive(true);
        var revision = new DccControlledFileRevisionServiceImpl();
        var files = mock(DccControlledFileMapper.class);
        var storage = mock(DccControlledFileSourceOwnershipService.class);
        var placement = mock(DccPublicUploadPlacementService.class);
        ReflectionTestUtils.setField(revision, "fileMapper", files);
        ReflectionTestUtils.setField(revision, "sourceOwnershipService", storage);
        ReflectionTestUtils.setField(revision, "publicUploadPlacementService", placement);
        when(files.selectById(21L)).thenReturn(source(21L, "A/1-1", 2L));
        if (!initial) when(files.selectById(20L)).thenReturn(source(20L, "A/1", 1L));
        var denied = assertThrows(ServiceException.class, () -> {
            if (initial) revision.createInitialCandidate(99L, 21L, request(true));
            else revision.createRevision(99L, 20L, 21L, request(false));
        });
        assertEquals(initial ? CONTROLLED_FILE_NOT_EXISTS.getCode() : CONTROLLED_FILE_CHECKIN_NOT_ALLOWED.getCode(), denied.getCode());
        verify(files, never()).insert(any(DccControlledFileDO.class));
        verifyNoInteractions(storage, placement, definitions);
    }

    private DccControlledFileDO declaredSource() {
        var source = source(21L, "A/1-1", 1L);
        // A PUBLIC declaration is deliberately unverified; no successful prepared source is fabricated.
        source.setPreparedWorkflowContract(DccApplicationExecutionContractService.PUBLIC);
        source.setPreparedProcessDefinitionId(UNVERIFIED_ID);
        source.setPreparedDefinitionDigest(UNVERIFIED_DIGEST);
        return source;
    }

    private DccControlledFileDO source(Long id, String version, Long tenant) {
        return DccControlledFileDO.builder().id(id).tenantId(tenant).masterId(10L)
                .dccProjectCodeId(5L).categoryId(2L).directoryId(3L).fileTypeTaxonomyId(6L)
                .fileNumber("W08-IC16-NEGATIVE").fileName("source.docx").sourceOriginalFileName("source.docx")
                .versionNo(version).changeType("NEW").status("WORKING").requesterId(99L)
                .sourceFileId(100L).sourceSha256("source-hash").build();
    }

    private DccControlledFileSubmitIterationReqVO request(boolean initial) {
        var request = new DccControlledFileSubmitIterationReqVO();
        request.setIdempotencyKey("w08-ic16-negative-only");
        request.setRevisionChangeType(initial ? "INITIAL" : "PARTIAL");
        request.setChangeDescription("task-owned negative boundary inspection");
        request.setNeedTraining(false);
        request.setEffectiveMode(DccApplicationEffectiveSelection.PRESET_DATE);
        request.setEffectiveDate(LocalDate.of(2099, 12, 20));
        request.setSelectedSignoffDepartmentIds(List.of(101L));
        request.setSelectedDistributionDepartmentIds(List.of(201L));
        request.setProjectAttributes(new DccProjectAttributes(List.of("CE"), null, "Y", "N", "N", null));
        return request;
    }

    /** A21 fixture: no selected attachment is an explicit read/ready empty list. */
    private static DccControlledFileCheckinReqVO a21ExplicitCheckinRequest() {
        var input = new DccControlledFileCheckinReqVO();
        input.setNeedTraining(false);
        input.setAttachmentUploadTickets(java.util.List.of());
        return input;
    }
}
