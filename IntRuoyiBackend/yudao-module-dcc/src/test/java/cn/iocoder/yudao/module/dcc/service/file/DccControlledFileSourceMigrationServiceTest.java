package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceMigrationDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceOwnershipDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceMigrationMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceOwnershipMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileSourceMigrationServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileSourceMigrationMapper migrationMapper;
    @Mock
    private DccControlledFileSourceOwnershipMapper ownershipMapper;
    @Mock
    private DccControlledFileSourceOwnershipService ownershipService;
    @Mock
    private DccControlledFileSourceMigrationCommitService commitService;
    @InjectMocks
    private DccControlledFileSourceMigrationService service;

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(31L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void migrateBatch_sharedLegacySourceKeepsOneOwnerAndCopiesEveryOtherRecord() {
        DccControlledFileDO first = sourceReference(900L, 700L);
        DccControlledFileDO second = sourceReference(901L, 700L);
        when(controlledFileMapper.selectUnownedSourceReferences(31L, 10)).thenReturn(List.of(first, second));
        when(migrationMapper.selectByControlledFileId(31L, 900L)).thenReturn(null);
        when(migrationMapper.selectByControlledFileId(31L, 901L)).thenReturn(null);
        when(ownershipMapper.selectBySourceFileId(31L, 700L)).thenReturn(null,
                DccControlledFileSourceOwnershipDO.builder()
                        .tenantId(31L).controlledFileId(900L).sourceFileId(700L).build());
        when(ownershipService.inspectSource(700L))
                .thenReturn(new DccControlledFilePreparedSource(700L, 700L, "legacy-sha", false));
        when(ownershipService.createVerifiedCopy(700L))
                .thenReturn(new DccControlledFilePreparedSource(1700L, 700L, "legacy-sha", true));
        when(controlledFileMapper.countUnownedSourceReferences(31L)).thenReturn(0L);

        DccControlledFileSourceMigrationResult result = service.migrateBatch(120L, 10);

        assertEquals(2, result.processedCount());
        assertEquals(0L, result.remainingCount());
        ArgumentCaptor<DccControlledFileSourceMigrationDO> migrationCaptor =
                ArgumentCaptor.forClass(DccControlledFileSourceMigrationDO.class);
        verify(migrationMapper, org.mockito.Mockito.times(2)).insert(migrationCaptor.capture());
        assertEquals(List.of(900L, 901L), migrationCaptor.getAllValues().stream()
                .map(DccControlledFileSourceMigrationDO::getControlledFileId).toList());
        verify(commitService).commitExistingSource(first, migrationCaptor.getAllValues().get(0),
                new DccControlledFilePreparedSource(700L, 700L, "legacy-sha", false), 120L);
        verify(commitService).commitIsolatedSource(second, migrationCaptor.getAllValues().get(1),
                new DccControlledFilePreparedSource(1700L, 700L, "legacy-sha", true), 120L);
    }

    @Test
    void migrateBatch_copyVerifiedRetryReusesPersistedIsolatedSource() {
        DccControlledFileDO file = sourceReference(902L, 701L);
        DccControlledFileSourceMigrationDO migration = DccControlledFileSourceMigrationDO.builder()
                .id(50L).tenantId(31L).controlledFileId(902L).legacySourceFileId(701L)
                .isolatedSourceFileId(1701L).sourceSha256("verified-sha")
                .migrationStatus("COPY_VERIFIED").build();
        when(controlledFileMapper.selectUnownedSourceReferences(31L, 1)).thenReturn(List.of(file));
        when(migrationMapper.selectByControlledFileId(31L, 902L)).thenReturn(migration);
        when(controlledFileMapper.countUnownedSourceReferences(31L)).thenReturn(0L);

        DccControlledFileSourceMigrationResult result = service.migrateBatch(120L, 1);

        assertEquals(1, result.processedCount());
        verify(ownershipService, never()).createVerifiedCopy(any());
        verify(commitService).commitIsolatedSource(file, migration,
                new DccControlledFilePreparedSource(1701L, 701L, "verified-sha", true), 120L);
    }

    @Test
    void getReadiness_reportsSharedAndUnownedRecordsForCurrentTenant() {
        when(controlledFileMapper.countAllSourceReferences(31L)).thenReturn(8L);
        when(controlledFileMapper.countUnownedSourceReferences(31L)).thenReturn(3L);
        when(controlledFileMapper.countSharedSourceGroups(31L)).thenReturn(1L);
        when(controlledFileMapper.countSharedSourceRecords(31L)).thenReturn(2L);
        when(migrationMapper.countByStatus(31L, "FAILED")).thenReturn(1L);

        DccControlledFileSourceMigrationReadiness result = service.getReadiness();

        assertEquals(8L, result.totalSourceRecordCount());
        assertEquals(5L, result.ownedSourceRecordCount());
        assertEquals(3L, result.unownedSourceRecordCount());
        assertEquals(1L, result.sharedSourceGroupCount());
        assertEquals(2L, result.sharedSourceRecordCount());
        assertEquals(1L, result.failedMigrationCount());
    }

    private DccControlledFileDO sourceReference(Long controlledFileId, Long sourceFileId) {
        return DccControlledFileDO.builder().id(controlledFileId).tenantId(31L)
                .sourceFileId(sourceFileId).originalFileId(sourceFileId).build();
    }
}
