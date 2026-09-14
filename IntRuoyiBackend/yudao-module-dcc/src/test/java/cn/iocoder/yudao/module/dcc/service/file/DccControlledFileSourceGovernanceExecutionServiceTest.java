package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceBatchDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceItemDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceMigrationDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceOwnershipDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceGovernanceItemMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceMigrationMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceOwnershipMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_GOVERNANCE_MANIFEST_INVALID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileSourceGovernanceExecutionServiceTest extends BaseMockitoUnitTest {

    private static final String HASH = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileSourceGovernanceItemMapper itemMapper;
    @Mock
    private DccControlledFileSourceMigrationMapper migrationMapper;
    @Mock
    private DccControlledFileSourceOwnershipMapper ownershipMapper;
    @Mock
    private DccControlledFileSourceOwnershipService ownershipService;
    @Mock
    private DccControlledFileSourceMigrationCommitService commitService;
    @Mock
    private DccControlledFileSourceGovernanceManifestService manifestService;
    @Mock
    private FileMapper fileMapper;
    @InjectMocks
    private DccControlledFileSourceGovernanceExecutionService service;

    @BeforeEach
    void stubSuccessfulEvidenceWrites() {
        org.mockito.Mockito.lenient().when(itemMapper.updateById(
                any(DccControlledFileSourceGovernanceItemDO.class))).thenReturn(1);
        org.mockito.Mockito.lenient().when(migrationMapper.insert(
                any(DccControlledFileSourceMigrationDO.class))).thenReturn(1);
        org.mockito.Mockito.lenient().when(migrationMapper.updateById(
                any(DccControlledFileSourceMigrationDO.class))).thenReturn(1);
    }

    @Test
    void executeItem_rejectsUnconfirmedManifestBeforeAnyWrite() {
        DccControlledFileSourceGovernanceBatchDO batch = batch();
        DccControlledFileSourceGovernanceItemDO item = item("CLAIM_SOURCE");
        org.mockito.Mockito.doThrow(new ServiceException(CONTROLLED_FILE_SOURCE_GOVERNANCE_MANIFEST_INVALID.getCode(), "invalid"))
                .when(manifestService).requireConfirmed(batch, "manifest", "request");

        assertThrows(ServiceException.class,
                () -> service.executeItem(batch, item, Set.of(31L), "manifest", "request", 120L));

        verify(itemMapper, never()).updateById(any(DccControlledFileSourceGovernanceItemDO.class));
        verify(commitService, never()).commitExistingSource(any(), any(), any(), eq(120L), any(), any());
    }

    @Test
    void executeItem_sourceDriftMarksBlockedWithoutChangingPointer() {
        DccControlledFileSourceGovernanceBatchDO batch = batch();
        DccControlledFileSourceGovernanceItemDO item = item("CLAIM_SOURCE");
        DccControlledFileDO candidate = file(901L, 799L);
        when(manifestService.isCompleted(item)).thenReturn(false);
        when(controlledFileMapper.selectByIdAndTenantIncludingDeleted(31L, 901L)).thenReturn(candidate);
        manifestService.requireConfirmed(batch, "manifest", "request");

        DccControlledFileSourceGovernanceExecutionResult result =
                service.executeItem(batch, item, Set.of(31L), "manifest", "request", 120L);

        assertEquals("BLOCKED", result.status());
        assertEquals("SNAPSHOT_DRIFTED", result.reasonCode());
        ArgumentCaptor<DccControlledFileSourceGovernanceItemDO> captor =
                ArgumentCaptor.forClass(DccControlledFileSourceGovernanceItemDO.class);
        verify(itemMapper).updateById(captor.capture());
        assertEquals("BLOCKED", captor.getValue().getItemStatus());
        verify(commitService, never()).commitExistingSource(any(), any(), any(), eq(120L), any(), any());
    }

    @Test
    void executeItem_validUnsharedSourceClaimsAndCompletesItem() {
        DccControlledFileSourceGovernanceBatchDO batch = batch();
        DccControlledFileSourceGovernanceItemDO item = item("CLAIM_SOURCE");
        DccControlledFileDO candidate = file(901L, 700L);
        when(manifestService.isCompleted(item)).thenReturn(false);
        when(controlledFileMapper.selectByIdAndTenantIncludingDeleted(31L, 901L)).thenReturn(candidate);
        when(controlledFileMapper.selectGlobalEffectiveSourceReferences(700L, 901L))
                .thenReturn(List.of(new DccControlledFileMapper.GlobalSourceReference()));
        DccControlledFileMapper.GlobalSourceReference reference =
                new DccControlledFileMapper.GlobalSourceReference();
        reference.setTenantId(31L);
        reference.setControlledFileId(901L);
        reference.setSourceFileId(700L);
        when(controlledFileMapper.selectGlobalEffectiveSourceReferences(700L, 901L))
                .thenReturn(List.of(reference));
        when(fileMapper.selectById(700L)).thenReturn(file());
        when(ownershipMapper.selectByControlledFileId(31L, 901L)).thenReturn(null);
        when(ownershipService.inspectSource(700L))
                .thenReturn(new DccControlledFilePreparedSource(700L, 700L, HASH, false));
        when(migrationMapper.selectByControlledFileId(31L, 901L)).thenReturn(null);
        manifestService.requireConfirmed(batch, "manifest", "request");

        DccControlledFileSourceGovernanceExecutionResult result =
                service.executeItem(batch, item, Set.of(31L), "manifest", "request", 120L);

        assertEquals("COMPLETED", result.status());
        verify(commitService).commitExistingSource(eq(candidate), any(DccControlledFileSourceMigrationDO.class),
                eq(new DccControlledFilePreparedSource(700L, 700L, HASH, false)), eq(120L), eq(55L), eq(66L));
        verify(itemMapper).updateById(any(DccControlledFileSourceGovernanceItemDO.class));
    }

    @Test
    void executeItem_sharedSourceCreatesCopyAndCompletesItem() {
        DccControlledFileSourceGovernanceBatchDO batch = batch();
        DccControlledFileSourceGovernanceItemDO item = item("COPY_SHARED_SOURCE");
        DccControlledFileDO candidate = file(901L, 700L);
        DccControlledFileMapper.GlobalSourceReference first = reference(31L, 901L, 700L);
        DccControlledFileMapper.GlobalSourceReference second = reference(31L, 902L, 700L);
        when(manifestService.isCompleted(item)).thenReturn(false);
        when(controlledFileMapper.selectByIdAndTenantIncludingDeleted(31L, 901L)).thenReturn(candidate);
        when(controlledFileMapper.selectGlobalEffectiveSourceReferences(700L, 901L))
                .thenReturn(List.of(first, second));
        when(fileMapper.selectById(700L)).thenReturn(file());
        when(ownershipMapper.selectByControlledFileId(31L, 901L)).thenReturn(null);
        when(ownershipService.inspectSource(700L))
                .thenReturn(new DccControlledFilePreparedSource(700L, 700L, HASH, false));
        when(ownershipService.createVerifiedCopy(700L))
                .thenReturn(new DccControlledFilePreparedSource(1700L, 700L, HASH, true));
        when(migrationMapper.selectByControlledFileId(31L, 901L)).thenReturn(null);
        manifestService.requireConfirmed(batch, "manifest", "request");

        DccControlledFileSourceGovernanceExecutionResult result =
                service.executeItem(batch, item, Set.of(31L), "manifest", "request", 120L);

        assertEquals("COMPLETED", result.status());
        verify(commitService).commitIsolatedSource(eq(candidate), any(DccControlledFileSourceMigrationDO.class),
                eq(new DccControlledFilePreparedSource(1700L, 700L, HASH, true)), eq(120L), eq(55L), eq(66L));
        verify(itemMapper).updateById(any(DccControlledFileSourceGovernanceItemDO.class));
    }

    @Test
    void executeItem_sourceHashSnapshotDriftBlocksBeforeCopy() {
        DccControlledFileSourceGovernanceBatchDO batch = batch();
        DccControlledFileSourceGovernanceItemDO item = item("CLAIM_SOURCE");
        item.setSnapshotSourceSha256("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        DccControlledFileDO candidate = file(901L, 700L);
        DccControlledFileMapper.GlobalSourceReference reference = reference(31L, 901L, 700L);
        when(manifestService.isCompleted(item)).thenReturn(false);
        when(controlledFileMapper.selectByIdAndTenantIncludingDeleted(31L, 901L)).thenReturn(candidate);
        when(controlledFileMapper.selectGlobalEffectiveSourceReferences(700L, 901L))
                .thenReturn(List.of(reference));
        when(fileMapper.selectById(700L)).thenReturn(file());
        when(ownershipService.inspectSource(700L))
                .thenReturn(new DccControlledFilePreparedSource(700L, 700L, HASH, false));
        manifestService.requireConfirmed(batch, "manifest", "request");

        DccControlledFileSourceGovernanceExecutionResult result =
                service.executeItem(batch, item, Set.of(31L), "manifest", "request", 120L);

        assertEquals("BLOCKED", result.status());
        assertEquals("SNAPSHOT_DRIFTED", result.reasonCode());
        verify(ownershipService, never()).createVerifiedCopy(700L);
        verify(commitService, never()).commitExistingSource(any(), any(), any(), eq(120L), any(), any());
    }

    @Test
    void executeItem_copyVerifiedMigrationReusesPersistedCopy() {
        DccControlledFileSourceGovernanceBatchDO batch = batch();
        DccControlledFileSourceGovernanceItemDO item = item("COPY_SHARED_SOURCE");
        DccControlledFileDO candidate = file(901L, 700L);
        DccControlledFileMapper.GlobalSourceReference first = reference(31L, 901L, 700L);
        DccControlledFileMapper.GlobalSourceReference second = reference(31L, 902L, 700L);
        DccControlledFileSourceMigrationDO migration = DccControlledFileSourceMigrationDO.builder()
                .id(77L).tenantId(31L).controlledFileId(901L).legacySourceFileId(700L)
                .isolatedSourceFileId(1700L).sourceSha256(HASH).migrationStatus("COPY_VERIFIED").build();
        when(manifestService.isCompleted(item)).thenReturn(false);
        when(controlledFileMapper.selectByIdAndTenantIncludingDeleted(31L, 901L)).thenReturn(candidate);
        when(controlledFileMapper.selectGlobalEffectiveSourceReferences(700L, 901L))
                .thenReturn(List.of(first, second));
        when(fileMapper.selectById(700L)).thenReturn(file());
        when(migrationMapper.selectByControlledFileId(31L, 901L)).thenReturn(migration);
        when(ownershipService.inspectSource(700L))
                .thenReturn(new DccControlledFilePreparedSource(700L, 700L, HASH, false));
        when(ownershipService.inspectSource(1700L))
                .thenReturn(new DccControlledFilePreparedSource(1700L, 1700L, HASH, false));
        manifestService.requireConfirmed(batch, "manifest", "request");

        DccControlledFileSourceGovernanceExecutionResult result =
                service.executeItem(batch, item, Set.of(31L), "manifest", "request", 120L);

        assertEquals("COMPLETED", result.status());
        verify(ownershipService, never()).createVerifiedCopy(700L);
        verify(commitService).commitIsolatedSource(eq(candidate), eq(migration),
                eq(new DccControlledFilePreparedSource(1700L, 700L, HASH, true)), eq(120L), eq(55L), eq(66L));
    }

    @Test
    void executeItem_rejectsPersistedCopyWithoutCopyVerifiedEvidence() {
        DccControlledFileSourceGovernanceBatchDO batch = batch();
        DccControlledFileSourceGovernanceItemDO item = item("COPY_SHARED_SOURCE");
        DccControlledFileDO candidate = file(901L, 700L);
        DccControlledFileSourceMigrationDO migration = DccControlledFileSourceMigrationDO.builder()
                .id(77L).tenantId(31L).controlledFileId(901L).legacySourceFileId(700L)
                .isolatedSourceFileId(1700L).sourceSha256(HASH).migrationStatus("FAILED").build();
        when(manifestService.isCompleted(item)).thenReturn(false);
        when(controlledFileMapper.selectByIdAndTenantIncludingDeleted(31L, 901L)).thenReturn(candidate);
        when(controlledFileMapper.selectGlobalEffectiveSourceReferences(700L, 901L)).thenReturn(List.of(
                reference(31L, 901L, 700L), reference(31L, 902L, 700L)));
        when(fileMapper.selectById(700L)).thenReturn(file());
        when(ownershipService.inspectSource(700L))
                .thenReturn(new DccControlledFilePreparedSource(700L, 700L, HASH, false));
        when(migrationMapper.selectByControlledFileId(31L, 901L)).thenReturn(migration);

        DccControlledFileSourceGovernanceExecutionResult result =
                service.executeItem(batch, item, Set.of(31L), "manifest", "request", 120L);

        assertEquals("BLOCKED", result.status());
        assertEquals("MIGRATION_COPY_EVIDENCE_INVALID", result.reasonCode());
        verify(ownershipService, never()).inspectSource(1700L);
        verify(commitService, never()).commitIsolatedSource(any(), any(), any(), any(), any(), any());
    }

    @Test
    void executeItem_sharedSourceWithExistingOwnershipRequiresManualReview() {
        DccControlledFileSourceGovernanceBatchDO batch = batch();
        DccControlledFileSourceGovernanceItemDO item = item("COPY_SHARED_SOURCE");
        DccControlledFileDO candidate = file(901L, 700L);
        DccControlledFileSourceOwnershipDO ownership = DccControlledFileSourceOwnershipDO.builder()
                .id(88L).tenantId(31L).controlledFileId(901L).sourceFileId(700L)
                .sourceSha256(HASH).build();
        when(manifestService.isCompleted(item)).thenReturn(false);
        when(controlledFileMapper.selectByIdAndTenantIncludingDeleted(31L, 901L)).thenReturn(candidate);
        when(controlledFileMapper.selectGlobalEffectiveSourceReferences(700L, 901L)).thenReturn(List.of(
                reference(31L, 901L, 700L), reference(31L, 902L, 700L)));
        when(fileMapper.selectById(700L)).thenReturn(file());
        when(ownershipMapper.selectByControlledFileId(31L, 901L)).thenReturn(ownership);
        when(ownershipService.inspectSource(700L))
                .thenReturn(new DccControlledFilePreparedSource(700L, 700L, HASH, false));

        DccControlledFileSourceGovernanceExecutionResult result =
                service.executeItem(batch, item, Set.of(31L), "manifest", "request", 120L);

        assertEquals("BLOCKED", result.status());
        assertEquals("SOURCE_OWNERSHIP_SHARED_REQUIRES_MANUAL_REVIEW", result.reasonCode());
        verify(ownershipService, never()).createVerifiedCopy(700L);
        verify(commitService, never()).commitIsolatedSource(any(), any(), any(), any(), any(), any());
    }

    @Test
    void executeSharedGroup_requiresAllThreeItemsToCompleteAndCleansNewCopiesOnFailure() {
        DccControlledFileSourceGovernanceBatchDO batch = batch();
        List<DccControlledFileSourceGovernanceItemDO> items = List.of(
                item("COPY_SHARED_SOURCE"), item("COPY_SHARED_SOURCE"), item("COPY_SHARED_SOURCE"));
        for (int index = 0; index < items.size(); index++) {
            items.get(index).setId(66L + index);
            items.get(index).setControlledFileId(901L + index);
            items.get(index).setSharedGroupKey("source:700");
        }
        when(controlledFileMapper.selectGlobalEffectiveSourceReferences(700L, 901L)).thenReturn(List.of(
                reference(31L, 901L, 700L), reference(31L, 902L, 700L), reference(31L, 903L, 700L)));
        when(controlledFileMapper.selectByIdAndTenantIncludingDeleted(eq(31L), any()))
                .thenAnswer(invocation -> file(invocation.getArgument(1), 700L));
        when(fileMapper.selectById(700L)).thenReturn(file());
        when(ownershipService.inspectSource(700L))
                .thenReturn(new DccControlledFilePreparedSource(700L, 700L, HASH, false));
        when(ownershipService.createVerifiedCopy(700L)).thenReturn(
                new DccControlledFilePreparedSource(1701L, 700L, HASH, true),
                new DccControlledFilePreparedSource(1702L, 700L, HASH, true));
        when(migrationMapper.selectByControlledFileId(eq(31L), any())).thenReturn(null);
        doAnswer(invocation -> {
            DccControlledFileDO candidate = invocation.getArgument(0);
            if (candidate.getId().equals(902L)) {
                throw new IllegalStateException("group commit failed");
            }
            return null;
        }).when(commitService).commitIsolatedSource(any(), any(), any(), eq(120L), eq(55L), any());

        assertThrows(IllegalStateException.class,
                () -> service.executeSharedGroup(batch, items, Set.of(31L), "manifest", "request", 120L));
        verify(ownershipService).cleanupFailedCopy(eq(1701L), any(RuntimeException.class));
        verify(ownershipService).cleanupFailedCopy(eq(1702L), any(RuntimeException.class));
    }

    @Test
    void executeSharedGroup_completesThreeItemsWithDistinctCopies() {
        DccControlledFileSourceGovernanceBatchDO batch = batch();
        batch.setSnapshotMaxControlledFileId(903L);
        List<DccControlledFileSourceGovernanceItemDO> items = List.of(
                item("COPY_SHARED_SOURCE"), item("COPY_SHARED_SOURCE"), item("COPY_SHARED_SOURCE"));
        for (int index = 0; index < items.size(); index++) {
            items.get(index).setId(66L + index);
            items.get(index).setControlledFileId(901L + index);
            items.get(index).setSharedGroupKey("source:700");
        }
        when(controlledFileMapper.selectByIdAndTenantIncludingDeleted(eq(31L), any()))
                .thenAnswer(invocation -> file(invocation.getArgument(1), 700L));
        when(controlledFileMapper.selectGlobalEffectiveSourceReferences(700L, 903L)).thenReturn(List.of(
                reference(31L, 901L, 700L), reference(31L, 902L, 700L), reference(31L, 903L, 700L)));
        when(fileMapper.selectById(700L)).thenReturn(file());
        when(ownershipService.inspectSource(700L))
                .thenReturn(new DccControlledFilePreparedSource(700L, 700L, HASH, false));
        when(ownershipService.createVerifiedCopy(700L)).thenReturn(
                new DccControlledFilePreparedSource(1701L, 700L, HASH, true),
                new DccControlledFilePreparedSource(1702L, 700L, HASH, true),
                new DccControlledFilePreparedSource(1703L, 700L, HASH, true));

        List<DccControlledFileSourceGovernanceExecutionResult> results = service.executeSharedGroup(
                batch, items, Set.of(31L), "manifest", "request", 120L);

        assertEquals(3, results.size());
        assertEquals(Set.of(1701L, 1702L, 1703L), items.stream()
                .map(DccControlledFileSourceGovernanceItemDO::getIsolatedSourceFileId)
                .collect(java.util.stream.Collectors.toSet()));
        verify(ownershipService, org.mockito.Mockito.times(3)).createVerifiedCopy(700L);
        verify(commitService, org.mockito.Mockito.times(3)).commitIsolatedSource(any(), any(), any(),
                eq(120L), eq(55L), any());
        verify(ownershipService, never()).cleanupFailedCopy(any(), any());
    }

    @Test
    void executeSharedGroup_attemptsEveryCopyCleanupWhenOneCleanupFails() {
        DccControlledFileSourceGovernanceBatchDO batch = batch();
        List<DccControlledFileSourceGovernanceItemDO> items = List.of(
                item("COPY_SHARED_SOURCE"), item("COPY_SHARED_SOURCE"));
        for (int index = 0; index < items.size(); index++) {
            items.get(index).setId(66L + index);
            items.get(index).setControlledFileId(901L + index);
            items.get(index).setSharedGroupKey("source:700");
        }
        when(controlledFileMapper.selectGlobalEffectiveSourceReferences(700L, 901L)).thenReturn(List.of(
                reference(31L, 901L, 700L), reference(31L, 902L, 700L)));
        when(controlledFileMapper.selectByIdAndTenantIncludingDeleted(eq(31L), any()))
                .thenAnswer(invocation -> file(invocation.getArgument(1), 700L));
        when(fileMapper.selectById(700L)).thenReturn(file());
        when(ownershipService.inspectSource(700L))
                .thenReturn(new DccControlledFilePreparedSource(700L, 700L, HASH, false));
        when(ownershipService.createVerifiedCopy(700L)).thenReturn(
                new DccControlledFilePreparedSource(1701L, 700L, HASH, true),
                new DccControlledFilePreparedSource(1702L, 700L, HASH, true));
        doAnswer(invocation -> {
            DccControlledFileDO candidate = invocation.getArgument(0);
            if (candidate.getId().equals(902L)) {
                throw new IllegalStateException("group commit failed");
            }
            return null;
        }).when(commitService).commitIsolatedSource(any(), any(), any(), eq(120L), eq(55L), any());
        doThrow(new IllegalStateException("first cleanup failed")).when(ownershipService)
                .cleanupFailedCopy(eq(1701L), any(RuntimeException.class));

        assertThrows(IllegalStateException.class,
                () -> service.executeSharedGroup(batch, items, Set.of(31L), "manifest", "request", 120L));

        verify(ownershipService).cleanupFailedCopy(eq(1701L), any(RuntimeException.class));
        verify(ownershipService).cleanupFailedCopy(eq(1702L), any(RuntimeException.class));
    }

    @Test
    void executeSharedGroup_rejectsEmptyGroupBeforeReturningSuccess() {
        assertThrows(IllegalArgumentException.class,
                () -> service.executeSharedGroup(batch(), List.of(), Set.of(31L),
                        "manifest", "request", 120L));
    }

    @Test
    void executeSharedGroup_preservesBusinessBlockerForOuterAudit() {
        DccControlledFileSourceGovernanceBatchDO batch = batch();
        List<DccControlledFileSourceGovernanceItemDO> items = List.of(
                item("COPY_SHARED_SOURCE"), item("COPY_SHARED_SOURCE"));
        items.get(0).setSharedGroupKey("source:700");
        items.get(1).setId(67L);
        items.get(1).setControlledFileId(902L);
        items.get(1).setSharedGroupKey("source:700");
        when(controlledFileMapper.selectGlobalEffectiveSourceReferences(700L, 901L)).thenReturn(List.of(
                reference(31L, 901L, 700L), reference(31L, 902L, 700L)));
        when(controlledFileMapper.selectByIdAndTenantIncludingDeleted(31L, 901L))
                .thenReturn(file(901L, 799L));

        DccControlledFileSourceGovernanceGroupBlockedException ex = assertThrows(
                DccControlledFileSourceGovernanceGroupBlockedException.class,
                () -> service.executeSharedGroup(batch, items, Set.of(31L),
                        "manifest", "request", 120L));

        assertEquals("SNAPSHOT_DRIFTED", ex.reasonCode());
    }

    @Test
    void executeItem_finalEvidenceWriteFailureCleansCreatedCopyAndPropagates() {
        DccControlledFileSourceGovernanceBatchDO batch = batch();
        DccControlledFileSourceGovernanceItemDO item = item("COPY_SHARED_SOURCE");
        DccControlledFileDO candidate = file(901L, 700L);
        when(manifestService.isCompleted(item)).thenReturn(false);
        when(controlledFileMapper.selectByIdAndTenantIncludingDeleted(31L, 901L)).thenReturn(candidate);
        when(controlledFileMapper.selectGlobalEffectiveSourceReferences(700L, 901L))
                .thenReturn(List.of(reference(31L, 901L, 700L), reference(31L, 902L, 700L)));
        when(fileMapper.selectById(700L)).thenReturn(file());
        when(ownershipService.inspectSource(700L))
                .thenReturn(new DccControlledFilePreparedSource(700L, 700L, HASH, false));
        when(ownershipService.createVerifiedCopy(700L))
                .thenReturn(new DccControlledFilePreparedSource(1700L, 700L, HASH, true));
        when(migrationMapper.selectByControlledFileId(31L, 901L)).thenReturn(null);
        doThrow(new IllegalStateException("item evidence write failed"))
                .when(itemMapper).updateById(any(DccControlledFileSourceGovernanceItemDO.class));

        assertThrows(IllegalStateException.class,
                () -> service.executeItem(batch, item, Set.of(31L), "manifest", "request", 120L));

        verify(ownershipService).cleanupFailedCopy(eq(1700L), any(RuntimeException.class));
    }

    @Test
    void executeItem_zeroRowFinalEvidenceWriteFailsInsteadOfReturningCompleted() {
        DccControlledFileSourceGovernanceBatchDO batch = batch();
        DccControlledFileSourceGovernanceItemDO item = item("CLAIM_SOURCE");
        DccControlledFileDO candidate = file(901L, 700L);
        when(manifestService.isCompleted(item)).thenReturn(false);
        when(controlledFileMapper.selectByIdAndTenantIncludingDeleted(31L, 901L)).thenReturn(candidate);
        when(controlledFileMapper.selectGlobalEffectiveSourceReferences(700L, 901L))
                .thenReturn(List.of(reference(31L, 901L, 700L)));
        when(fileMapper.selectById(700L)).thenReturn(file());
        when(ownershipService.inspectSource(700L))
                .thenReturn(new DccControlledFilePreparedSource(700L, 700L, HASH, false));
        when(migrationMapper.selectByControlledFileId(31L, 901L)).thenReturn(null);
        when(migrationMapper.insert(any(DccControlledFileSourceMigrationDO.class))).thenReturn(1);
        when(itemMapper.updateById(any(DccControlledFileSourceGovernanceItemDO.class))).thenReturn(0);

        assertThrows(IllegalStateException.class,
                () -> service.executeItem(batch, item, Set.of(31L), "manifest", "request", 120L));
    }

    private DccControlledFileSourceGovernanceBatchDO batch() {
        return DccControlledFileSourceGovernanceBatchDO.builder().id(55L)
                .snapshotMaxControlledFileId(901L).batchStatus("CONFIRMED")
                .ruleVersion(DccControlledFileSourceGovernanceManifestService.CURRENT_RULE_VERSION)
                .schemaVersion(DccControlledFileSourceGovernanceManifestService.CURRENT_SCHEMA_VERSION)
                .manifestSha256("manifest").requestSha256("request").build();
    }

    private DccControlledFileSourceGovernanceItemDO item(String action) {
        return DccControlledFileSourceGovernanceItemDO.builder().id(66L).batchId(55L).tenantId(31L)
                .controlledFileId(901L).legacySourceFileId(700L).snapshotSourceFileId(700L)
                .snapshotSourceSha256(HASH).governanceAction(action).itemStatus("READY").build();
    }

    private DccControlledFileDO file(Long id, Long sourceFileId) {
        return DccControlledFileDO.builder().id(id).tenantId(31L).sourceFileId(sourceFileId).build();
    }

    private FileDO file() {
        FileDO file = FileDO.builder().id(700L).configId(1L).path("dcc/source.docx").build();
        file.setDeleted(false);
        return file;
    }

    private DccControlledFileMapper.GlobalSourceReference reference(Long tenantId, Long controlledFileId,
                                                                     Long sourceFileId) {
        DccControlledFileMapper.GlobalSourceReference reference =
                new DccControlledFileMapper.GlobalSourceReference();
        reference.setTenantId(tenantId);
        reference.setControlledFileId(controlledFileId);
        reference.setSourceFileId(sourceFileId);
        return reference;
    }
}
