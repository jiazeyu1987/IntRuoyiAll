package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceMigrationDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceMigrationMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_MIGRATION_CONFLICT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileSourceMigrationCommitServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileSourceMigrationMapper migrationMapper;
    @Mock
    private DccControlledFileSourceOwnershipService ownershipService;
    @Mock
    private DccControlledFileSourceGlobalClaimService globalClaimService;
    @InjectMocks
    private DccControlledFileSourceMigrationCommitService service;

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(31L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void commitIsolatedSource_updatesSourceClaimsOwnershipAndCompletesEvidenceAtomically() {
        DccControlledFileDO candidate = file(901L, 700L);
        DccControlledFileSourceMigrationDO migration = migration(50L, 901L, 700L);
        DccControlledFilePreparedSource prepared =
                new DccControlledFilePreparedSource(1700L, 700L, "verified-sha", true);
        when(controlledFileMapper.selectByIdAndTenantIncludingDeleted(31L, 901L)).thenReturn(candidate);
        when(controlledFileMapper.updateSourceFileIdIncludingDeleted(31L, 901L, 700L, 1700L, 120L))
                .thenReturn(1);
        when(migrationMapper.updateById(migration)).thenReturn(1);

        service.commitIsolatedSource(candidate, migration, prepared, 120L);

        verify(globalClaimService).claim(31L, 1700L, 901L, "verified-sha", 120L, null, null);
        verify(ownershipService).claimSubmissionSource(901L, prepared, 120L, "HISTORICAL_MIGRATION");
        ArgumentCaptor<DccControlledFileSourceMigrationDO> captor =
                ArgumentCaptor.forClass(DccControlledFileSourceMigrationDO.class);
        verify(migrationMapper).updateById(captor.capture());
        assertEquals("COMPLETED", captor.getValue().getMigrationStatus());
        assertEquals(1700L, captor.getValue().getIsolatedSourceFileId());
        assertEquals("verified-sha", captor.getValue().getSourceSha256());
        assertEquals(120L, captor.getValue().getMigratedBy());
    }

    @Test
    void commitIsolatedSource_sourceDriftFailsWithoutClaimOrUpdate() {
        DccControlledFileDO candidate = file(901L, 700L);
        DccControlledFileSourceMigrationDO migration = migration(50L, 901L, 700L);
        DccControlledFilePreparedSource prepared =
                new DccControlledFilePreparedSource(1700L, 700L, "verified-sha", true);
        when(controlledFileMapper.selectByIdAndTenantIncludingDeleted(31L, 901L))
                .thenReturn(file(901L, 799L));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.commitIsolatedSource(candidate, migration, prepared, 120L));

        assertEquals(CONTROLLED_FILE_SOURCE_MIGRATION_CONFLICT.getCode(), ex.getCode());
        verify(controlledFileMapper, never()).updateSourceFileIdIncludingDeleted(
                31L, 901L, 700L, 1700L, 120L);
        verify(ownershipService, never()).claimSubmissionSource(901L, prepared, 120L, "HISTORICAL_MIGRATION");
        verify(globalClaimService, never()).claim(31L, 1700L, 901L, "verified-sha", 120L, null, null);
    }

    @Test
    void commitIsolatedSource_zeroRowMigrationEvidenceWriteFailsClosed() {
        DccControlledFileDO candidate = file(901L, 700L);
        DccControlledFileSourceMigrationDO migration = migration(50L, 901L, 700L);
        DccControlledFilePreparedSource prepared =
                new DccControlledFilePreparedSource(1700L, 700L, "verified-sha", true);
        when(controlledFileMapper.selectByIdAndTenantIncludingDeleted(31L, 901L)).thenReturn(candidate);
        when(controlledFileMapper.updateSourceFileIdIncludingDeleted(31L, 901L, 700L, 1700L, 120L))
                .thenReturn(1);
        when(migrationMapper.updateById(migration)).thenReturn(0);

        assertThrows(IllegalStateException.class,
                () -> service.commitIsolatedSource(candidate, migration, prepared, 120L));
    }

    private DccControlledFileDO file(Long id, Long sourceFileId) {
        return DccControlledFileDO.builder().id(id).tenantId(31L).sourceFileId(sourceFileId).build();
    }

    private DccControlledFileSourceMigrationDO migration(Long id, Long fileId, Long legacySourceId) {
        return DccControlledFileSourceMigrationDO.builder().id(id).tenantId(31L)
                .controlledFileId(fileId).legacySourceFileId(legacySourceId)
                .migrationStatus("COPY_VERIFIED").build();
    }
}
