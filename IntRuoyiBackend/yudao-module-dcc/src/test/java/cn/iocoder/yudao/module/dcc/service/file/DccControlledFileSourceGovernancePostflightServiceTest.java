package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceBatchDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceItemDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceOwnershipDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceGovernanceBatchMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceGovernanceItemMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceOwnershipMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class DccControlledFileSourceGovernancePostflightServiceTest extends BaseMockitoUnitTest {

    private static final String HASH = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Mock
    private DccControlledFileSourceGovernanceBatchMapper batchMapper;
    @Mock
    private DccControlledFileSourceGovernanceItemMapper itemMapper;
    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileSourceOwnershipMapper ownershipMapper;
    @Mock
    private DccControlledFileSourceOwnershipService ownershipService;
    @InjectMocks
    private DccControlledFileSourceGovernancePostflightService service;

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(31L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void inspectCompletedItemsReportsValidEvidenceAndHashDrift() {
        DccControlledFileSourceGovernanceBatchDO batch = DccControlledFileSourceGovernanceBatchDO.builder()
                .id(55L).taskKey("task-1").tenantScopeJson("[31]").build();
        DccControlledFileSourceGovernanceItemDO valid = item(66L, 700L);
        DccControlledFileSourceGovernanceItemDO drifted = item(67L, 701L);
        when(batchMapper.selectByTaskKey("task-1")).thenReturn(batch);
        when(itemMapper.selectByBatchAndTenant(55L, 31L)).thenReturn(List.of(valid, drifted));
        when(controlledFileMapper.selectByIdAndTenantIncludingDeleted(31L, 901L))
                .thenReturn(controlledFile(901L, 700L));
        when(controlledFileMapper.selectByIdAndTenantIncludingDeleted(31L, 902L))
                .thenReturn(controlledFile(902L, 701L));
        when(controlledFileMapper.selectGlobalEffectiveSourceReferences(700L, null))
                .thenReturn(List.of(reference(31L, 901L, 700L)));
        when(controlledFileMapper.selectGlobalEffectiveSourceReferences(701L, null))
                .thenReturn(List.of(reference(31L, 902L, 701L)));
        when(ownershipMapper.selectByControlledFileId(31L, 901L)).thenReturn(owner(700L, HASH));
        when(ownershipMapper.selectByControlledFileId(31L, 902L)).thenReturn(owner(701L, HASH));
        when(ownershipService.inspectSource(700L))
                .thenReturn(new DccControlledFilePreparedSource(700L, 700L, HASH, false));
        when(ownershipService.inspectSource(701L))
                .thenReturn(new DccControlledFilePreparedSource(701L, 701L, "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", false));

        DccControlledFileSourceGovernancePostflightReport report = service.inspectCompletedItems("task-1");

        assertEquals(2, report.checkedCount());
        assertEquals(1, report.validCount());
        assertEquals("SOURCE_HASH_MISMATCH", report.findings().get(0).reasonCode());
    }

    @Test
    void inspectCompletedItemsReportsSourceStillShared() {
        DccControlledFileSourceGovernanceBatchDO batch = DccControlledFileSourceGovernanceBatchDO.builder()
                .id(55L).taskKey("task-1").tenantScopeJson("[31]").snapshotMaxControlledFileId(901L).build();
        DccControlledFileSourceGovernanceItemDO item = item(66L, 700L);
        when(batchMapper.selectByTaskKey("task-1")).thenReturn(batch);
        when(itemMapper.selectByBatchAndTenant(55L, 31L)).thenReturn(List.of(item));
        when(controlledFileMapper.selectByIdAndTenantIncludingDeleted(31L, 901L))
                .thenReturn(controlledFile(901L, 700L));
        when(controlledFileMapper.selectGlobalEffectiveSourceReferences(700L, 901L))
                .thenReturn(List.of(reference(31L, 901L, 700L), reference(31L, 902L, 700L)));

        DccControlledFileSourceGovernancePostflightReport report = service.inspectCompletedItems("task-1");

        assertEquals(0, report.validCount());
        assertEquals("COMPLETED_SOURCE_STILL_SHARED", report.findings().get(0).reasonCode());
    }

    @Test
    void inspectCompletedItemsReportsHistoricalEvidenceDrift() {
        DccControlledFileSourceGovernanceBatchDO batch = DccControlledFileSourceGovernanceBatchDO.builder()
                .id(55L).taskKey("task-1").tenantScopeJson("[31]").snapshotMaxControlledFileId(901L).build();
        DccControlledFileSourceGovernanceItemDO item = item(66L, 700L);
        item.setSnapshotHistoryEvidenceHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        DccControlledFileDO controlledFile = controlledFile(901L, 700L);
        controlledFile.setOriginalFileId(700L);
        when(batchMapper.selectByTaskKey("task-1")).thenReturn(batch);
        when(itemMapper.selectByBatchAndTenant(55L, 31L)).thenReturn(List.of(item));
        when(controlledFileMapper.selectByIdAndTenantIncludingDeleted(31L, 901L)).thenReturn(controlledFile);

        DccControlledFileSourceGovernancePostflightReport report = service.inspectCompletedItems("task-1");

        assertEquals("HISTORICAL_EVIDENCE_DRIFTED", report.findings().get(0).reasonCode());
    }

    private DccControlledFileSourceGovernanceItemDO item(Long id, Long sourceId) {
        return DccControlledFileSourceGovernanceItemDO.builder().id(id).batchId(55L).tenantId(31L)
                .controlledFileId(id == 66L ? 901L : 902L).snapshotSourceFileId(sourceId)
                .isolatedSourceFileId(sourceId).sourceSha256(HASH).itemStatus("COMPLETED").build();
    }

    private DccControlledFileDO controlledFile(Long id, Long sourceId) {
        return DccControlledFileDO.builder().id(id).tenantId(31L).sourceFileId(sourceId).build();
    }

    private DccControlledFileSourceOwnershipDO owner(Long sourceId, String hash) {
        return DccControlledFileSourceOwnershipDO.builder().tenantId(31L).controlledFileId(901L)
                .sourceFileId(sourceId).sourceSha256(hash).build();
    }

    private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper.GlobalSourceReference reference(
            Long tenantId, Long controlledFileId, Long sourceFileId) {
        cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper.GlobalSourceReference reference =
                new cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper.GlobalSourceReference();
        reference.setTenantId(tenantId);
        reference.setControlledFileId(controlledFileId);
        reference.setSourceFileId(sourceFileId);
        return reference;
    }
}
