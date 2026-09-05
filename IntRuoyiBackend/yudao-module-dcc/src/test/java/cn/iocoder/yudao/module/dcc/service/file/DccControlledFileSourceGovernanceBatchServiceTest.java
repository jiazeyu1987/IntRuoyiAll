package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceBatchDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceItemDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceGovernanceBatchMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceGovernanceItemMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_GOVERNANCE_SCOPE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_GOVERNANCE_BATCH_SIZE_SPLITS_GROUP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class DccControlledFileSourceGovernanceBatchServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileSourceGovernanceBatchMapper batchMapper;
    @Mock
    private DccControlledFileSourceGovernanceItemMapper itemMapper;
    @Mock
    private DccControlledFileSourceGovernanceManifestService manifestService;
    @Mock
    private DccControlledFileSourceGovernanceExecutionService executionService;
    @InjectMocks
    private DccControlledFileSourceGovernanceBatchService service;

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(31L);
        org.mockito.Mockito.lenient().when(batchMapper.updateById(
                any(DccControlledFileSourceGovernanceBatchDO.class))).thenReturn(1);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void confirmBatch_requiresPreparedStatusAndRecordsConfirmation() {
        DccControlledFileSourceGovernanceBatchDO batch = batch("PREPARED");
        when(batchMapper.selectByTaskKey("task-1")).thenReturn(batch);
        when(batchMapper.updateById(batch)).thenReturn(1);

        DccControlledFileSourceGovernanceBatchDO result =
                service.confirmBatch("task-1", 120L, "manifest", "request");

        assertEquals("CONFIRMED", result.getBatchStatus());
        assertEquals(120L, result.getConfirmedBy());
        verify(batchMapper).updateById(batch);
    }

    @Test
    void confirmBatch_zeroRowAuditUpdateFailsClosed() {
        DccControlledFileSourceGovernanceBatchDO batch = batch("PREPARED");
        when(batchMapper.selectByTaskKey("task-1")).thenReturn(batch);
        when(batchMapper.updateById(batch)).thenReturn(0);

        assertThrows(IllegalStateException.class,
                () -> service.confirmBatch("task-1", 120L, "manifest", "request"));
    }

    @Test
    void executeConfirmedBatchProcessesOnlyReadyItemsAndRefreshesCounts() {
        DccControlledFileSourceGovernanceBatchDO batch = batch("CONFIRMED");
        DccControlledFileSourceGovernanceItemDO item = item(66L, "READY");
        when(batchMapper.selectByTaskKey("task-1")).thenReturn(batch);
        when(itemMapper.selectByBatchAndTenant(55L, 31L)).thenReturn(List.of(item));
        when(itemMapper.selectStatusCountsByBatchAndTenant(55L, 31L)).thenReturn(List.of(
                Map.of("itemStatus", "COMPLETED", "itemCount", 1)));
        when(executionService.executeItem(any(), any(), any(), any(), any(), any()))
                .thenReturn(new DccControlledFileSourceGovernanceExecutionResult(
                        901L, "COMPLETED", "CLAIM_SOURCE", null, null));

        DccControlledFileSourceGovernanceBatchExecutionResult result =
                service.executeConfirmedBatch("task-1", 100, "manifest", "request", 120L);

        assertEquals("COMPLETED", result.batchStatus());
        assertEquals(1, result.processedCount());
        assertEquals(1, result.completedCount());
        verify(executionService).executeItem(batch, item, java.util.Set.of(31L),
                "manifest", "request", 120L);
        verify(batchMapper).updateById(batch);
    }

    @Test
    void executeConfirmedBatchRejectsTenantOutsideFrozenScope() {
        DccControlledFileSourceGovernanceBatchDO batch = batch("CONFIRMED");
        batch.setTenantScopeJson("[32]");
        when(batchMapper.selectByTaskKey("task-1")).thenReturn(batch);
        doThrow(new ServiceException(CONTROLLED_FILE_SOURCE_GOVERNANCE_SCOPE_INVALID.getCode(), "out of scope"))
                .when(manifestService).requireTenantInScope(batch, 31L);

        assertThrows(ServiceException.class,
                () -> service.executeConfirmedBatch("task-1", 100, "manifest", "request", 120L));
        verify(itemMapper, org.mockito.Mockito.never()).selectByBatchAndTenant(55L, 31L);
    }

    @Test
    void executeConfirmedBatchProcessesSharedSourceAsOneCompleteGroup() {
        DccControlledFileSourceGovernanceBatchDO batch = batch("CONFIRMED");
        List<DccControlledFileSourceGovernanceItemDO> group = List.of(
                item(66L, "READY"), item(67L, "READY"), item(68L, "READY"));
        group.forEach(item -> {
            item.setGovernanceAction("COPY_SHARED_SOURCE");
            item.setSharedGroupKey("source:700");
            item.setSnapshotSourceFileId(700L);
        });
        when(batchMapper.selectByTaskKey("task-1")).thenReturn(batch);
        when(itemMapper.selectByBatchAndTenant(55L, 31L)).thenReturn(group);
        when(itemMapper.selectStatusCountsByBatchAndTenant(55L, 31L)).thenReturn(List.of(
                Map.of("itemStatus", "COMPLETED", "itemCount", 3)));

        DccControlledFileSourceGovernanceBatchExecutionResult result =
                service.executeConfirmedBatch("task-1", 3, "manifest", "request", 120L);

        assertEquals(3, result.processedCount());
        verify(executionService).executeSharedGroup(batch, group, java.util.Set.of(31L),
                "manifest", "request", 120L);
    }

    @Test
    void executeConfirmedBatchRejectsBatchSizeThatSplitsSharedGroup() {
        DccControlledFileSourceGovernanceBatchDO batch = batch("CONFIRMED");
        List<DccControlledFileSourceGovernanceItemDO> group = List.of(
                item(66L, "READY"), item(67L, "READY"), item(68L, "READY"));
        group.forEach(item -> {
            item.setGovernanceAction("COPY_SHARED_SOURCE");
            item.setSharedGroupKey("source:700");
            item.setSnapshotSourceFileId(700L);
        });
        when(batchMapper.selectByTaskKey("task-1")).thenReturn(batch);
        when(itemMapper.selectByBatchAndTenant(55L, 31L)).thenReturn(group);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.executeConfirmedBatch("task-1", 2, "manifest", "request", 120L));

        assertEquals(CONTROLLED_FILE_SOURCE_GOVERNANCE_BATCH_SIZE_SPLITS_GROUP.getCode(), ex.getCode());
    }

    @Test
    void executeConfirmedBatchDoesNotDowngradeIncompleteSharedGroupToSingleItem() {
        DccControlledFileSourceGovernanceBatchDO batch = batch("CONFIRMED");
        DccControlledFileSourceGovernanceItemDO item = item(66L, "COPY_SHARED_SOURCE");
        item.setGovernanceAction("COPY_SHARED_SOURCE");
        item.setItemStatus("READY");
        item.setSharedGroupKey("source:700");
        when(batchMapper.selectByTaskKey("task-1")).thenReturn(batch);
        when(itemMapper.selectByBatchAndTenant(55L, 31L)).thenReturn(List.of(item));
        when(itemMapper.selectStatusCountsByBatchAndTenant(55L, 31L)).thenReturn(List.of(
                Map.of("itemStatus", "BLOCKED", "itemCount", 1)));
        when(executionService.executeSharedGroup(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(new DccControlledFileSourceGovernanceExecutionResult(
                        901L, "BLOCKED", "COPY_SHARED_SOURCE", "SOURCE_GROUP_MANIFEST_INCOMPLETE", "incomplete")));

        service.executeConfirmedBatch("task-1", 100, "manifest", "request", 120L);

        verify(executionService).executeSharedGroup(batch, List.of(item), java.util.Set.of(31L),
                "manifest", "request", 120L);
    }

    @Test
    void executeConfirmedBatch_recordsSharedGroupFailureAfterGroupRollback() {
        DccControlledFileSourceGovernanceBatchDO batch = batch("CONFIRMED");
        List<DccControlledFileSourceGovernanceItemDO> group = List.of(
                item(66L, "READY"), item(67L, "READY"));
        group.forEach(item -> {
            item.setGovernanceAction("COPY_SHARED_SOURCE");
            item.setSharedGroupKey("source:700");
            item.setSnapshotSourceFileId(700L);
        });
        IllegalStateException failure = new IllegalStateException("object storage unavailable");
        when(batchMapper.selectByTaskKey("task-1")).thenReturn(batch);
        when(itemMapper.selectByBatchAndTenant(55L, 31L)).thenReturn(group);
        doThrow(failure).when(executionService).executeSharedGroup(
                batch, group, java.util.Set.of(31L), "manifest", "request", 120L);
        when(itemMapper.selectStatusCountsByBatchAndTenant(55L, 31L)).thenReturn(List.of(
                Map.of("itemStatus", "FAILED", "itemCount", 2)));

        assertThrows(IllegalStateException.class,
                () -> service.executeConfirmedBatch("task-1", 100, "manifest", "request", 120L));

        verify(executionService).recordGroupFailure(group, 120L, failure);
        verify(batchMapper).updateById(org.mockito.ArgumentMatchers.<DccControlledFileSourceGovernanceBatchDO>argThat(value ->
                "FAILED".equals(value.getBatchStatus()) && Long.valueOf(2L).equals(value.getFailedCount())));
    }

    @Test
    void executeConfirmedBatch_doesNotWrapIndependentGroupsInOneTransaction() throws Exception {
        assertNull(DccControlledFileSourceGovernanceBatchService.class
                .getMethod("executeConfirmedBatch", String.class, int.class, String.class,
                        String.class, Long.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class));
    }

    private DccControlledFileSourceGovernanceBatchDO batch(String status) {
        return DccControlledFileSourceGovernanceBatchDO.builder().id(55L).taskKey("task-1")
                .tenantScopeJson("[31]").snapshotMaxControlledFileId(901L).batchStatus(status)
                .ruleVersion(DccControlledFileSourceGovernanceManifestService.CURRENT_RULE_VERSION)
                .schemaVersion(DccControlledFileSourceGovernanceManifestService.CURRENT_SCHEMA_VERSION)
                .manifestSha256("manifest").requestSha256("request").build();
    }

    private DccControlledFileSourceGovernanceItemDO item(Long id, String status) {
        return DccControlledFileSourceGovernanceItemDO.builder().id(id).batchId(55L).tenantId(31L)
                .controlledFileId(901L).itemStatus(status).governanceAction("CLAIM_SOURCE").build();
    }
}
