package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceBatchDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceItemDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceGovernanceBatchMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceGovernanceItemMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceOwnershipMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileSourceGovernancePreparationServiceTest extends BaseMockitoUnitTest {

    private static final String HASH = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileSourceGovernanceBatchMapper batchMapper;
    @Mock
    private DccControlledFileSourceGovernanceItemMapper itemMapper;
    @Mock
    private DccControlledFileSourceOwnershipMapper ownershipMapper;
    @Mock
    private DccControlledFileSourceOwnershipService ownershipService;
    @Mock
    private FileMapper fileMapper;
    @Spy
    private DccControlledFileSourceGovernanceManifestHasher manifestHasher =
            new DccControlledFileSourceGovernanceManifestHasher();
    @InjectMocks
    private DccControlledFileSourceGovernancePreparationService service;

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(31L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void prepareBatchFreezesReadyItemAndManifestDigests() {
        DccControlledFileDO candidate = controlledFile(901L, 700L);
        DccControlledFileMapper.GlobalSourceReference reference = reference(31L, 901L, 700L);
        when(controlledFileMapper.selectGlobalMaxControlledFileId()).thenReturn(999L);
        when(controlledFileMapper.selectEffectiveSourceGovernanceCandidates(31L, 999L, 100))
                .thenReturn(List.of(candidate));
        when(controlledFileMapper.selectGlobalEffectiveSourceReferences(700L, 999L))
                .thenReturn(List.of(reference));
        when(controlledFileMapper.selectByIdAndTenantIncludingDeleted(31L, 901L)).thenReturn(candidate);
        when(fileMapper.selectById(700L)).thenReturn(sourceFile());
        when(ownershipService.inspectSource(700L))
                .thenReturn(new DccControlledFilePreparedSource(700L, 700L, HASH, false));
        when(batchMapper.insert(any(DccControlledFileSourceGovernanceBatchDO.class))).thenAnswer(invocation -> {
            ((DccControlledFileSourceGovernanceBatchDO) invocation.getArgument(0)).setId(55L);
            return 1;
        });

        DccControlledFileSourceGovernancePreparationResult result = service.prepareBatch("task-1", 100);

        assertEquals("PREPARED", result.batchStatus());
        assertEquals(1, result.totalCount());
        assertEquals(1, result.readyCount());
        assertEquals(0, result.blockedCount());
        assertTrue(result.manifestSha256().matches("[0-9a-f]{64}"));
        assertTrue(result.requestSha256().matches("[0-9a-f]{64}"));
        ArgumentCaptor<DccControlledFileSourceGovernanceItemDO> itemCaptor =
                ArgumentCaptor.forClass(DccControlledFileSourceGovernanceItemDO.class);
        verify(itemMapper).insert(itemCaptor.capture());
        assertEquals("CLAIM_SOURCE", itemCaptor.getValue().getGovernanceAction());
        assertEquals("READY", itemCaptor.getValue().getItemStatus());
        assertEquals(HASH, itemCaptor.getValue().getSnapshotSourceSha256());
        assertNotNull(itemCaptor.getValue().getSnapshotLocationHash());
        assertNotNull(itemCaptor.getValue().getSnapshotHistoryEvidenceHash());
    }

    @Test
    void prepareBatchMarksCrossTenantSharedSourceBlocked() {
        DccControlledFileDO candidate = controlledFile(901L, 700L);
        when(controlledFileMapper.selectGlobalMaxControlledFileId()).thenReturn(999L);
        when(controlledFileMapper.selectEffectiveSourceGovernanceCandidates(31L, 999L, 100))
                .thenReturn(List.of(candidate));
        when(controlledFileMapper.selectGlobalEffectiveSourceReferences(700L, 999L)).thenReturn(List.of(
                reference(31L, 901L, 700L), reference(32L, 902L, 700L)));
        when(controlledFileMapper.selectByIdAndTenantIncludingDeleted(31L, 901L)).thenReturn(candidate);
        when(fileMapper.selectById(700L)).thenReturn(sourceFile());
        when(ownershipService.inspectSource(700L))
                .thenReturn(new DccControlledFilePreparedSource(700L, 700L, HASH, false));
        when(batchMapper.insert(any(DccControlledFileSourceGovernanceBatchDO.class))).thenAnswer(invocation -> {
            ((DccControlledFileSourceGovernanceBatchDO) invocation.getArgument(0)).setId(55L);
            return 1;
        });

        DccControlledFileSourceGovernancePreparationResult result = service.prepareBatch("task-2", 100);

        assertEquals(0, result.readyCount());
        assertEquals(1, result.blockedCount());
        ArgumentCaptor<DccControlledFileSourceGovernanceItemDO> itemCaptor =
                ArgumentCaptor.forClass(DccControlledFileSourceGovernanceItemDO.class);
        verify(itemMapper).insert(itemCaptor.capture());
        assertEquals("SOURCE_GLOBAL_REFERENCE_OUT_OF_SCOPE", itemCaptor.getValue().getBlockerReasonCode());
        assertEquals("NO_ACTION", itemCaptor.getValue().getGovernanceAction());
    }

    private DccControlledFileDO controlledFile(Long id, Long sourceFileId) {
        return DccControlledFileDO.builder().id(id).tenantId(31L).sourceFileId(sourceFileId)
                .originalFileId(sourceFileId).versionNo("V1.0").status("ACTIVE").build();
    }

    private FileDO sourceFile() {
        FileDO file = FileDO.builder().id(700L).configId(8L).path("dcc/source.docx")
                .name("source.docx").type("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .size(10L).build();
        file.setDeleted(false);
        return file;
    }

    private DccControlledFileMapper.GlobalSourceReference reference(Long tenantId, Long controlledFileId,
                                                                     Long sourceFileId) {
        DccControlledFileMapper.GlobalSourceReference reference = new DccControlledFileMapper.GlobalSourceReference();
        reference.setTenantId(tenantId);
        reference.setControlledFileId(controlledFileId);
        reference.setSourceFileId(sourceFileId);
        return reference;
    }
}
