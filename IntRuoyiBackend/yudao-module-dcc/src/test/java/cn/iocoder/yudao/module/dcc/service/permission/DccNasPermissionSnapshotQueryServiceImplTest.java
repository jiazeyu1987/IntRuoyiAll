package cn.iocoder.yudao.module.dcc.service.permission;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNasTransferTaskDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclAceDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclDirectorySnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclIdentityMappingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNasTransferTaskMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclAceMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclDirectorySnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclIdentityMappingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclSnapshotMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccNasPermissionSnapshotQueryServiceImplTest extends BaseMockitoUnitTest {

    private static final Long TASK_ID = 10L;
    private static final Long SNAPSHOT_ID = 7001L;
    private static final long RESTORABLE_ACCESS_MASK = 2_032_127L;
    private static final long WINDOWS_READ_EXECUTE_MASK = 1_179_817L;
    private static final long WINDOWS_MODIFY_MASK = 1_245_695L;
    private static final int SNAPSHOT_NOT_READY_CODE = 1_080_000_076;

    @Mock
    private DccNasAclSnapshotMapper snapshotMapper;
    @Mock
    private DccNasAclDirectorySnapshotMapper directorySnapshotMapper;
    @Mock
    private DccNasAclAceMapper aceMapper;
    @Mock
    private DccNasAclIdentityMappingMapper identityMappingMapper;
    @Mock
    private DccControlledFileNasTransferTaskMapper transferTaskMapper;

    @InjectMocks
    private DccNasPermissionSnapshotQueryServiceImpl queryService;

    private DccNasPermissionSnapshotQueryService serviceContract;

    @BeforeEach
    void setUpContract() {
        serviceContract = queryService;
    }

    @Test
    void getSummaryAndItems_deriveCapturedHappyPathFromMapperData() {
        LocalDateTime capturedAt = LocalDateTime.of(2026, 5, 26, 21, 30);
        mockSnapshot(capturedSnapshot(capturedAt),
                List.of(successDirectorySnapshot(7301L, 7101L, 902634L, "Quality/SOP")),
                List.of(allowAce(7201L, 7101L, "S-1-5-21-1000-2000-3000-1101", "hash-1101",
                        RESTORABLE_ACCESS_MASK)),
                List.of(mappedIdentity("hash-1101")));

        DccNasPermissionSnapshotQueryService.SummaryResult summary = serviceContract.getSummary(TASK_ID);
        PageResult<DccNasPermissionSnapshotQueryService.ItemResult> items =
                serviceContract.getItems(TASK_ID, 1, 100, null);

        assertEquals(TASK_ID, summary.taskId());
        assertEquals("CAPTURED", summary.snapshotStatus());
        assertEquals(List.of("Quality/SOP"), summary.selectedNasPaths());
        assertEquals(1L, summary.directorySnapshotCount());
        assertEquals(1L, summary.aceCount());
        assertEquals(0L, summary.unsupportedAceCount());
        assertEquals(0L, summary.unmappedPrincipalCount());
        assertEquals(0L, summary.blockerCount());
        assertEquals(capturedAt, summary.capturedAt());
        assertTrue(summary.restoreSupported());
        assertEquals(1L, items.getTotal());
        assertEquals("CAPTURED", items.getList().get(0).snapshotStatus());
        assertEquals(1L, items.getList().get(0).aceCount());
        assertTrue(items.getList().get(0).blockers().isEmpty());
    }

    @Test
    void getSummaryAndItems_reportCollectFailureBlockerFromDirectorySnapshot() {
        when(snapshotMapper.selectOne(anySnapshotWrapper())).thenReturn(capturedSnapshot(null));
        when(directorySnapshotMapper.selectList(anyDirectorySnapshotWrapper()))
                .thenReturn(List.of(failedDirectorySnapshot(7301L, "Quality/Failed")));

        DccNasPermissionSnapshotQueryService.SummaryResult summary = serviceContract.getSummary(TASK_ID);
        PageResult<DccNasPermissionSnapshotQueryService.ItemResult> items =
                serviceContract.getItems(TASK_ID, 1, 100, "FAILED");

        assertEquals(1L, summary.directorySnapshotCount());
        assertEquals(0L, summary.aceCount());
        assertEquals(1L, summary.blockerCount());
        assertFalse(summary.restoreSupported());
        assertEquals(1L, items.getTotal());
        DccNasPermissionSnapshotQueryService.ItemResult item = items.getList().get(0);
        assertEquals("FAILED", item.snapshotStatus());
        assertEquals("DCC_NAS_ACL_COLLECT_FAILED", item.blockers().get(0).code());
        verify(aceMapper, never()).selectList(anyAceWrapper());
        verify(identityMappingMapper, never()).selectList(anyMappingWrapper());
    }

    @Test
    void getSummaryAndItems_reportDenyAndUnsupportedMaskBlockersFromAceData() {
        mockSnapshot(capturedSnapshot(null),
                List.of(
                        successDirectorySnapshot(7301L, 7101L, 902634L, "Quality/Deny"),
                        successDirectorySnapshot(7302L, 7102L, 902635L, "Quality/SpecialMask")),
                List.of(
                        denyAce(7201L, 7101L, "S-1-5-21-1000-2000-3000-1101", "hash-1101",
                        RESTORABLE_ACCESS_MASK),
                        allowAce(7202L, 7102L, "S-1-5-21-1000-2000-3000-1102", "hash-1102", 0L)),
                List.of(mappedIdentity("hash-1101"), mappedIdentity("hash-1102")));

        DccNasPermissionSnapshotQueryService.SummaryResult summary = serviceContract.getSummary(TASK_ID);
        PageResult<DccNasPermissionSnapshotQueryService.ItemResult> items =
                serviceContract.getItems(TASK_ID, 1, 100, "BLOCKED");

        assertEquals(2L, summary.unsupportedAceCount());
        assertEquals(0L, summary.unmappedPrincipalCount());
        assertEquals(2L, summary.blockerCount());
        assertFalse(summary.restoreSupported());
        assertEquals(2L, items.getTotal());
        assertEquals("DCC_NAS_ACL_DENY_UNSUPPORTED", items.getList().get(0).blockers().get(0).code());
        assertEquals("DCC_NAS_ACL_SPECIAL_MASK_UNSUPPORTED", items.getList().get(1).blockers().get(0).code());
    }

    @Test
    void getSummaryAndItems_treatWindowsReadExecuteAndModifyMasksAsRestorable() {
        mockSnapshot(capturedSnapshot(null),
                List.of(successDirectorySnapshot(7301L, 7101L, 902634L, "Quality/Readable")),
                List.of(
                        allowAce(7201L, 7101L, "S-1-5-21-1000-2000-3000-1101", "hash-1101",
                                WINDOWS_READ_EXECUTE_MASK),
                        allowAce(7202L, 7101L, "S-1-5-21-1000-2000-3000-1102", "hash-1102",
                                WINDOWS_MODIFY_MASK)),
                List.of(mappedIdentity("hash-1101"), mappedIdentity("hash-1102")));

        DccNasPermissionSnapshotQueryService.SummaryResult summary = serviceContract.getSummary(TASK_ID);
        PageResult<DccNasPermissionSnapshotQueryService.ItemResult> items =
                serviceContract.getItems(TASK_ID, 1, 100, null);

        assertEquals(0L, summary.unsupportedAceCount());
        assertEquals(0L, summary.blockerCount());
        assertTrue(summary.restoreSupported());
        assertEquals(1L, items.getTotal());
        assertEquals("CAPTURED", items.getList().get(0).snapshotStatus());
        assertEquals(0, items.getList().get(0).blockers().size());
    }

    @Test
    void getSummaryAndItems_treatNormalizedAllowTypeAsRestorable() {
        mockSnapshot(capturedSnapshot(null),
                List.of(successDirectorySnapshot(7301L, 7101L, 902634L, "Quality/Readable")),
                List.of(ace(7201L, 7101L, "ALLOW", "S-1-5-21-1000-2000-3000-1101",
                        "hash-1101", WINDOWS_READ_EXECUTE_MASK)),
                List.of(mappedIdentity("hash-1101")));

        DccNasPermissionSnapshotQueryService.SummaryResult summary = serviceContract.getSummary(TASK_ID);
        PageResult<DccNasPermissionSnapshotQueryService.ItemResult> items =
                serviceContract.getItems(TASK_ID, 1, 100, null);

        assertEquals(0L, summary.unsupportedAceCount());
        assertEquals(0L, summary.blockerCount());
        assertTrue(summary.restoreSupported());
        assertEquals("CAPTURED", items.getList().get(0).snapshotStatus());
    }

    @Test
    void getSummaryAndItems_reportNormalizedDenyTypeAsDenyBlocker() {
        mockSnapshot(capturedSnapshot(null),
                List.of(successDirectorySnapshot(7301L, 7101L, 902634L, "Quality/Deny")),
                List.of(ace(7201L, 7101L, "DENY", "S-1-5-21-1000-2000-3000-1101",
                        "hash-1101", RESTORABLE_ACCESS_MASK)),
                List.of(mappedIdentity("hash-1101")));

        DccNasPermissionSnapshotQueryService.SummaryResult summary = serviceContract.getSummary(TASK_ID);
        PageResult<DccNasPermissionSnapshotQueryService.ItemResult> items =
                serviceContract.getItems(TASK_ID, 1, 100, "BLOCKED");

        assertEquals(1L, summary.unsupportedAceCount());
        assertEquals(1L, summary.blockerCount());
        assertFalse(summary.restoreSupported());
        assertEquals("DCC_NAS_ACL_DENY_UNSUPPORTED", items.getList().get(0).blockers().get(0).code());
    }

    @Test
    void getSummaryAndItems_reportUnmappedPrincipalBlockerFromIdentityMappingData() {
        mockSnapshot(capturedSnapshot(null),
                List.of(successDirectorySnapshot(7301L, 7101L, 902634L, "Quality/Unmapped")),
                List.of(allowAce(7201L, 7101L, "S-1-5-21-1000-2000-3000-1101", "hash-unmapped",
                        RESTORABLE_ACCESS_MASK)),
                List.of());

        DccNasPermissionSnapshotQueryService.SummaryResult summary = serviceContract.getSummary(TASK_ID);
        PageResult<DccNasPermissionSnapshotQueryService.ItemResult> items =
                serviceContract.getItems(TASK_ID, 1, 100, "BLOCKED");

        assertEquals(1L, summary.unmappedPrincipalCount());
        assertEquals(1L, summary.blockerCount());
        assertFalse(summary.restoreSupported());
        assertEquals(1L, items.getTotal());
        assertEquals("DCC_NAS_PRINCIPAL_UNMAPPED", items.getList().get(0).blockers().get(0).code());
        assertEquals("S-1-5-21-1000-2000-3000-1101", items.getList().get(0).blockers().get(0).principal());
    }

    @Test
    void getItems_appliesPaginationAndStatusFilterAfterDerivingItemStatuses() {
        mockSnapshot(capturedSnapshot(null),
                List.of(
                        successDirectorySnapshot(7301L, 7101L, 902634L, "Quality/Captured"),
                        successDirectorySnapshot(7302L, 7102L, 902635L, "Quality/Blocked"),
                        failedDirectorySnapshot(7303L, "Quality/Failed")),
                List.of(
                        allowAce(7201L, 7101L, "S-1-5-21-1000-2000-3000-1101", "hash-1101",
                                RESTORABLE_ACCESS_MASK),
                        denyAce(7202L, 7102L, "S-1-5-21-1000-2000-3000-1102", "hash-1102",
                                RESTORABLE_ACCESS_MASK)),
                List.of(mappedIdentity("hash-1101"), mappedIdentity("hash-1102")));

        PageResult<DccNasPermissionSnapshotQueryService.ItemResult> secondPage =
                serviceContract.getItems(TASK_ID, 2, 1, null);
        PageResult<DccNasPermissionSnapshotQueryService.ItemResult> blocked =
                serviceContract.getItems(TASK_ID, 1, 10, "BLOCKED");

        assertEquals(3L, secondPage.getTotal());
        assertEquals(1, secondPage.getList().size());
        assertEquals("BLOCKED", secondPage.getList().get(0).snapshotStatus());
        assertEquals(1L, blocked.getTotal());
        assertEquals("Quality/Blocked", blocked.getList().get(0).nasPath());
    }

    @Test
    void getSummary_returnsNotCollectedStatusWhenTransferTaskHasNoSnapshotYet() {
        when(snapshotMapper.selectOne(anySnapshotWrapper())).thenReturn(null);
        when(transferTaskMapper.selectById(TASK_ID)).thenReturn(DccControlledFileNasTransferTaskDO.builder()
                .id(TASK_ID)
                .selectedNasPathsJson("[\"3.DMR\"]")
                .status("RUNNING")
                .build());

        DccNasPermissionSnapshotQueryService.SummaryResult summary = serviceContract.getSummary(TASK_ID);

        assertEquals(TASK_ID, summary.taskId());
        assertEquals("NOT_COLLECTED", summary.snapshotStatus());
        assertEquals(List.of("3.DMR"), summary.selectedNasPaths());
        assertEquals(0L, summary.directorySnapshotCount());
        assertEquals(0L, summary.aceCount());
        assertEquals(0L, summary.unsupportedAceCount());
        assertEquals(0L, summary.unmappedPrincipalCount());
        assertEquals(0L, summary.blockerCount());
        assertFalse(summary.restoreSupported());
        verify(directorySnapshotMapper, never()).selectList(anyDirectorySnapshotWrapper());
    }

    @Test
    void getSummary_failsFastWhenTransferTaskAndSnapshotAreBothMissing() {
        when(snapshotMapper.selectOne(anySnapshotWrapper())).thenReturn(null);
        when(transferTaskMapper.selectById(TASK_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> serviceContract.getSummary(TASK_ID));

        assertEquals(SNAPSHOT_NOT_READY_CODE, ex.getCode());
        verify(directorySnapshotMapper, never()).selectList(anyDirectorySnapshotWrapper());
    }

    private void mockSnapshot(DccNasAclSnapshotDO snapshot,
                              List<DccNasAclDirectorySnapshotDO> directorySnapshots,
                              List<DccNasAclAceDO> aces,
                              List<DccNasAclIdentityMappingDO> mappings) {
        when(snapshotMapper.selectOne(anySnapshotWrapper())).thenReturn(snapshot);
        when(directorySnapshotMapper.selectList(anyDirectorySnapshotWrapper())).thenReturn(directorySnapshots);
        when(aceMapper.selectList(anyAceWrapper())).thenReturn(aces);
        when(identityMappingMapper.selectList(anyMappingWrapper())).thenReturn(mappings);
    }

    private static DccNasAclSnapshotDO capturedSnapshot(LocalDateTime capturedAt) {
        return DccNasAclSnapshotDO.builder()
                .id(SNAPSHOT_ID)
                .transferTaskId(TASK_ID)
                .status("CAPTURED")
                .rootPathsJson("[\"Quality/SOP\"]")
                .completedAt(capturedAt)
                .build();
    }

    private static DccNasAclDirectorySnapshotDO successDirectorySnapshot(Long id,
                                                                        Long descriptorId,
                                                                        Long directoryId,
                                                                        String nasPath) {
        return DccNasAclDirectorySnapshotDO.builder()
                .id(id)
                .snapshotId(SNAPSHOT_ID)
                .transferTaskId(TASK_ID)
                .transferTaskItemId(100L + id)
                .dccDirectoryId(directoryId)
                .nasPath(nasPath)
                .descriptorId(descriptorId)
                .collectStatus("SUCCESS")
                .build();
    }

    private static DccNasAclDirectorySnapshotDO failedDirectorySnapshot(Long id, String nasPath) {
        return DccNasAclDirectorySnapshotDO.builder()
                .id(id)
                .snapshotId(SNAPSHOT_ID)
                .transferTaskId(TASK_ID)
                .transferTaskItemId(100L + id)
                .nasPath(nasPath)
                .collectStatus("FAILED")
                .failureCode("DCC_NAS_ACL_COLLECT_FAILED")
                .failureMessage("NAS ACL snapshot collection failed")
                .build();
    }

    private static DccNasAclAceDO allowAce(Long id,
                                           Long descriptorId,
                                           String sid,
                                           String sidHash,
                                           Long accessMask) {
        return ace(id, descriptorId, "ACCESS_ALLOWED_ACE_TYPE", sid, sidHash, accessMask);
    }

    private static DccNasAclAceDO denyAce(Long id,
                                          Long descriptorId,
                                          String sid,
                                          String sidHash,
                                          Long accessMask) {
        return ace(id, descriptorId, "ACCESS_DENIED_ACE_TYPE", sid, sidHash, accessMask);
    }

    private static DccNasAclAceDO ace(Long id,
                                      Long descriptorId,
                                      String aceType,
                                      String sid,
                                      String sidHash,
                                      Long accessMask) {
        return DccNasAclAceDO.builder()
                .id(id)
                .descriptorId(descriptorId)
                .aceIndex(id.intValue())
                .aceType(aceType)
                .accessMask(accessMask)
                .trusteeSid(sid)
                .trusteeSidHash(sidHash)
                .build();
    }

    private static DccNasAclIdentityMappingDO mappedIdentity(String sidHash) {
        return DccNasAclIdentityMappingDO.builder()
                .sidHash(sidHash)
                .mappingStatus("MAPPED")
                .dccSubjectType("USER")
                .dccSubjectId(901L)
                .build();
    }

    private static Wrapper<DccNasAclSnapshotDO> anySnapshotWrapper() {
        return any();
    }

    private static Wrapper<DccNasAclDirectorySnapshotDO> anyDirectorySnapshotWrapper() {
        return any();
    }

    private static Wrapper<DccNasAclAceDO> anyAceWrapper() {
        return any();
    }

    private static Wrapper<DccNasAclIdentityMappingDO> anyMappingWrapper() {
        return any();
    }
}
