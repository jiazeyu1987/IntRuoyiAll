package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesTeamLeaderDataCleanupMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderDataCleanupRetentionTest {
    @Mock private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock private MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;
    @Mock private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock private MesTeamLeaderDataCleanupMapper dataCleanupMapper;
    @Mock private MesProcessPoolTeamMaintenanceAuditMapper auditMapper;
    @Mock private GxpAuditService gxpAuditService;
    @Spy @InjectMocks private MesTeamLeaderActiveOrderServiceImpl service;
    private MesTeamLeaderDataCleanupPreview currentPreview;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "dataCleanupMapper", dataCleanupMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "gxpAuditService", gxpAuditService);
        currentPreview = preview();
        doAnswer(call -> currentPreview).when(service).previewDataCleanup(3001L);
        when(dataCleanupMapper.selectAllBatchExecutionIdsForUpdate(1L)).thenReturn(List.of(71L));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @ParameterizedTest
    @ValueSource(ints = {30, 40, 50, 60})
    void rejectsTerminalBatchBeforeAnyDeletion(int status) {
        lenient().when(batchExecutionMapper.selectByIdForUpdate(71L)).thenReturn(batch(status));
        assertProtected();
    }

    @Test
    void rejectsClosingSignatureEvenIfStatusIsNotTerminal() {
        lenient().when(batchExecutionMapper.selectByIdForUpdate(71L))
                .thenReturn(batch(20).setCloseSignatureId(801L));
        assertProtected();
    }

    @Test
    void rejectsRejectionSignatureEvenIfStatusIsNotTerminal() {
        lenient().when(batchExecutionMapper.selectByIdForUpdate(71L))
                .thenReturn(batch(20).setRejectSignatureId(802L));
        assertProtected();
    }

    @Test
    void rejectsMissingBatchInsteadOfDeletingItsEvidence() {
        assertProtected();
    }

    @Test
    void rejectsCrossTenantBatch() {
        lenient().when(batchExecutionMapper.selectByIdForUpdate(71L))
                .thenReturn(batch(10).setTenantId(2L));
        assertProtected();
    }

    @Test
    void rejectsUnknownBatchStatus() {
        lenient().when(batchExecutionMapper.selectByIdForUpdate(71L)).thenReturn(batch(null));
        assertProtected();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 10, 15, 20, 25})
    void rejectsNonTerminalBatchBecauseItsEvidenceAlsoRequiresRetention(int status) {
        lenient().when(batchExecutionMapper.selectByIdForUpdate(71L)).thenReturn(batch(status));
        assertProtected();
    }

    @Test
    void rejectsProductionEventWithoutBatch() {
        var emptyBatchPreview = preview();
        emptyBatchPreview.setBatchExecutionIds(List.of());
        currentPreview = emptyBatchPreview;
        when(dataCleanupMapper.selectAllBatchExecutionIdsForUpdate(1L)).thenReturn(List.of());
        when(dataCleanupMapper.selectAllCleanupEventIdsForUpdate(1L)).thenReturn(List.of(91L));
        assertProtected();
    }

    @Test
    void rejectsReleaseApplicationWithoutBatchOrEvent() {
        var scope = preview();
        scope.setBatchExecutionIds(List.of());
        scope.setReleaseApplicationCount(1);
        currentPreview = scope;
        when(dataCleanupMapper.selectAllBatchExecutionIdsForUpdate(1L)).thenReturn(List.of());
        when(releaseApplicationMapper.selectListByActiveOrderIdsForUpdate(List.of())).thenReturn(List.of(
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team
                        .MesProcessPoolActiveOrderReleaseApplicationDO.builder().id(81L).build()));
        assertProtected();
    }

    @Test
    void permitsEmptyScopeWithoutEvidence() {
        var scope = preview();
        scope.setBatchExecutionIds(List.of());
        scope.setBatchExecutionCount(0);
        currentPreview = scope;
        when(dataCleanupMapper.selectAllBatchExecutionIdsForUpdate(1L)).thenReturn(List.of());
        assertDoesNotThrow(() -> service.executeDataCleanup(3001L, scope));
        assertTrue(mockingDetails(dataCleanupMapper).getInvocations().stream()
                .noneMatch(call -> call.getMethod().getName().startsWith("delete")));
    }

    @Test
    void rejectsBatchCommittedAfterPreviewWithoutDeletingAnything() {
        currentPreview.setBatchExecutionIds(List.of());
        ServiceException error = assertThrows(ServiceException.class,
                () -> service.executeDataCleanup(3001L, currentPreview));
        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_DATA_CLEANUP_SCOPE_CHANGED.getCode(), error.getCode());
        var order = inOrder(gxpAuditService, dataCleanupMapper);
        order.verify(gxpAuditService).acquireLedgerLock();
        order.verify(dataCleanupMapper).selectAllCleanupEventIdsForUpdate(1L);
        order.verify(dataCleanupMapper).selectAllBatchExecutionIdsForUpdate(1L);
        assertTrue(mockingDetails(dataCleanupMapper).getInvocations().stream()
                .noneMatch(call -> call.getMethod().getName().startsWith("delete")
                        || call.getMethod().getName().startsWith("softRemove")));
        verifyNoInteractions(auditMapper);
    }

    private void assertProtected() {
        ServiceException error = assertThrows(ServiceException.class,
                () -> service.executeDataCleanup(3001L, preview()));
        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_DATA_CLEANUP_BLOCKED.getCode(), error.getCode());
        assertTrue(mockingDetails(dataCleanupMapper).getInvocations().stream()
                .noneMatch(call -> call.getMethod().getName().startsWith("delete")
                        || call.getMethod().getName().startsWith("softRemove")), "No destructive statement may run");
        verifyNoInteractions(auditMapper);
    }

    private MesProEdhrBatchExecutionDO batch(Integer status) {
        return MesProEdhrBatchExecutionDO.builder().id(71L).tenantId(1L).status(status).build();
    }

    private MesTeamLeaderDataCleanupPreview preview() {
        return MesTeamLeaderDataCleanupPreview.builder().leaderUserId(3001L)
                .orderIds(List.of()).orderVersions(List.of()).workOrderIds(List.of())
                .batchExecutionIds(List.of(71L)).activeOrderCount(0).reportEventCount(0)
                .batchExecutionCount(1).releaseApplicationCount(0).releaseTransactionCount(0).build();
    }
}
