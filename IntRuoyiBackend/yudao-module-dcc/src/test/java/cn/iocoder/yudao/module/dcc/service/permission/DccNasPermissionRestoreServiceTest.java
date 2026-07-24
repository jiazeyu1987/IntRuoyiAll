package cn.iocoder.yudao.module.dcc.service.permission;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccDirectoryAccessRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclAceDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclDirectorySnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclIdentityMappingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclRestoreLogDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclRestorePlanDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclRestorePlanItemDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccDirectoryAccessRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclAceMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclDirectorySnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclIdentityMappingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclRestoreLogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclRestorePlanItemMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclRestorePlanMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclSnapshotMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccNasPermissionRestoreServiceTest extends BaseMockitoUnitTest {

    private static final Long TASK_ID = 10L;
    private static final Long SNAPSHOT_ID = 7001L;
    private static final Long DIRECTORY_SNAPSHOT_ID = 7301L;
    private static final Long DESCRIPTOR_ID = 7101L;
    private static final Long DIRECTORY_ID = 902634L;
    private static final Long SUBJECT_ID = 901L;
    private static final Long OPERATOR_USER_ID = 99L;
    private static final String NAS_PATH = "3.DMR/01.图纸";
    private static final String MAPPED_SID = "S-1-5-21-1000-2000-3000-1101";
    private static final String MAPPED_SID_HASH = "hash-mapped";
    private static final String UNMAPPED_SID = "S-1-5-21-1000-2000-3000-2202";
    private static final long RESTORABLE_ACCESS_MASK = 2032127L;
    private static final long WINDOWS_READ_EXECUTE_MASK = 1179817L;
    private static final long WINDOWS_MODIFY_MASK = 1245695L;
    private static final int SNAPSHOT_NOT_READY_CODE = 1_080_000_076;
    private static final int RESTORE_BLOCKED_CODE = 1_080_000_077;
    private static final int PLAN_STALE_CODE = 1_080_000_078;
    private static final int UNSUPPORTED_MODE_CODE = 1_080_000_079;
    private static final int IDEMPOTENCY_CONFLICT_CODE = 1_080_000_080;

    @Mock
    private DccNasAclSnapshotMapper snapshotMapper;
    @Mock
    private DccNasAclDirectorySnapshotMapper directorySnapshotMapper;
    @Mock
    private DccNasAclAceMapper aceMapper;
    @Mock
    private DccNasAclIdentityMappingMapper identityMappingMapper;
    @Mock
    private DccNasAclRestorePlanMapper restorePlanMapper;
    @Mock
    private DccNasAclRestorePlanItemMapper restorePlanItemMapper;
    @Mock
    private DccNasAclRestoreLogMapper restoreLogMapper;
    @Mock
    private DccDirectoryAccessRuleMapper directoryAccessRuleMapper;

    @InjectMocks
    private DccNasPermissionRestoreServiceImpl restoreService;

    private DccNasPermissionRestoreService serviceContract;

    @BeforeEach
    void setUpContract() {
        serviceContract = restoreService;
    }

    @Test
    void preview_readOnlyBuildsReplaceDirectoryRulesPlanFromMappedAllowAce() {
        mockSnapshot(List.of(allowAce(MAPPED_SID, MAPPED_SID_HASH, RESTORABLE_ACCESS_MASK)),
                List.of(mappedIdentity(MAPPED_SID, MAPPED_SID_HASH)));

        DccNasPermissionRestoreService.PreviewResult result = serviceContract.preview(TASK_ID);

        assertTrue(result.canRestore());
        assertEquals(TASK_ID, result.taskId());
        assertNotNull(result.planHash());
        assertTrue(result.planHash().startsWith("sha256:"));
        assertEquals("REPLACE_DIRECTORY_RULES", result.restoreMode());
        assertEquals(1L, result.directoryCount());
        assertEquals(1L, result.ruleCount());
        assertEquals(0, result.blockers().size());
        assertEquals(1, result.sampleRules().size());
        DccNasPermissionRestoreService.RestoreRulePreview sampleRule = result.sampleRules().get(0);
        assertEquals(DIRECTORY_ID, sampleRule.directoryId());
        assertEquals(NAS_PATH, sampleRule.nasPath());
        assertEquals("USER", sampleRule.subjectType());
        assertEquals(SUBJECT_ID, sampleRule.subjectId());
        assertTrue(sampleRule.canQuery());
        assertTrue(sampleRule.canPreview());
        assertTrue(sampleRule.canDownload());
        assertNoWriteCalls(restorePlanMapper, restorePlanItemMapper, restoreLogMapper, directoryAccessRuleMapper);
    }

    @Test
    void preview_mapsWindowsReadExecuteAndModifyMasksToReadableDccRules() {
        mockSnapshot(List.of(
                        allowAce(7201L, DESCRIPTOR_ID, "S-1-5-21-1000-2000-3000-1101",
                                "hash-read-execute", WINDOWS_READ_EXECUTE_MASK),
                        allowAce(7202L, DESCRIPTOR_ID, "S-1-5-21-1000-2000-3000-1102",
                                "hash-modify", WINDOWS_MODIFY_MASK)),
                List.of(
                        mappedIdentity(7401L, "S-1-5-21-1000-2000-3000-1101", "hash-read-execute", 901L),
                        mappedIdentity(7402L, "S-1-5-21-1000-2000-3000-1102", "hash-modify", 902L)));

        DccNasPermissionRestoreService.PreviewResult result = serviceContract.preview(TASK_ID);

        assertTrue(result.canRestore());
        assertEquals(2L, result.ruleCount());
        assertEquals(0, result.blockers().size());
        assertEquals(2, result.sampleRules().size());
        for (DccNasPermissionRestoreService.RestoreRulePreview sampleRule : result.sampleRules()) {
            assertTrue(sampleRule.canQuery());
            assertTrue(sampleRule.canPreview());
            assertTrue(sampleRule.canDownload());
        }
        assertNoWriteCalls(restorePlanMapper, restorePlanItemMapper, restoreLogMapper, directoryAccessRuleMapper);
    }

    @Test
    void preview_mapsNormalizedAllowAceTypeToReadableDccRule() {
        mockSnapshot(List.of(ace("ALLOW", MAPPED_SID, MAPPED_SID_HASH, WINDOWS_READ_EXECUTE_MASK)),
                List.of(mappedIdentity(MAPPED_SID, MAPPED_SID_HASH)));

        DccNasPermissionRestoreService.PreviewResult result = serviceContract.preview(TASK_ID);

        assertTrue(result.canRestore());
        assertEquals(1L, result.ruleCount());
        assertEquals(0, result.blockers().size());
        assertNoWriteCalls(restorePlanMapper, restorePlanItemMapper, restoreLogMapper, directoryAccessRuleMapper);
    }

    @Test
    void preview_usesBatchLookupsForIdentityMappingsAndRuntimeDirectoryRules() {
        mockSnapshot(List.of(
                        successDirectorySnapshot(7301L, 7101L, 902634L, "3.DMR/01.图纸"),
                        successDirectorySnapshot(7302L, 7102L, 902635L, "3.DMR/02.工艺"),
                        successDirectorySnapshot(7303L, 7103L, 902636L, "3.DMR/03.质量")),
                List.of(
                        allowAce(7201L, 7101L, "S-1-5-21-1000-2000-3000-1101", "hash-1101", RESTORABLE_ACCESS_MASK),
                        allowAce(7202L, 7102L, "S-1-5-21-1000-2000-3000-1102", "hash-1102", RESTORABLE_ACCESS_MASK),
                        allowAce(7203L, 7103L, "S-1-5-21-1000-2000-3000-1103", "hash-1103", RESTORABLE_ACCESS_MASK)),
                List.of(
                        mappedIdentity(7401L, "S-1-5-21-1000-2000-3000-1101", "hash-1101", 901L),
                        mappedIdentity(7402L, "S-1-5-21-1000-2000-3000-1102", "hash-1102", 902L),
                        mappedIdentity(7403L, "S-1-5-21-1000-2000-3000-1103", "hash-1103", 903L)),
                List.of(runtimeRule(7601L, 902634L, 901L)));
        clearInvocations(identityMappingMapper, directoryAccessRuleMapper);

        DccNasPermissionRestoreService.PreviewResult result = serviceContract.preview(TASK_ID);

        assertTrue(result.canRestore());
        assertEquals(3L, result.directoryCount());
        assertEquals(3L, result.ruleCount());
        verify(identityMappingMapper, times(1)).selectList(anyMappingWrapper());
        verify(identityMappingMapper, never()).selectOne(anyMappingWrapper());
        verify(directoryAccessRuleMapper, times(1)).selectList(anyDirectoryAccessRuleWrapper());
        verify(directoryAccessRuleMapper, never()).selectListByDirectoryId(any());
        assertNoWriteCalls(restorePlanMapper, restorePlanItemMapper, restoreLogMapper, directoryAccessRuleMapper);
    }

    @Test
    void preview_planHashChangesWhenRuntimeDirectoryRuleChangeReasonChanges() {
        mockSnapshot(List.of(allowAce(MAPPED_SID, MAPPED_SID_HASH, RESTORABLE_ACCESS_MASK)),
                List.of(mappedIdentity(MAPPED_SID, MAPPED_SID_HASH)),
                List.of(runtimeRule(7601L, DIRECTORY_ID, SUBJECT_ID, "原始目录授权原因")));

        DccNasPermissionRestoreService.PreviewResult firstPreview = serviceContract.preview(TASK_ID);
        when(directoryAccessRuleMapper.selectList(anyDirectoryAccessRuleWrapper()))
                .thenReturn(List.of(runtimeRule(7601L, DIRECTORY_ID, SUBJECT_ID, "预览后调整目录授权原因")));

        DccNasPermissionRestoreService.PreviewResult secondPreview = serviceContract.preview(TASK_ID);

        assertTrue(firstPreview.canRestore());
        assertTrue(secondPreview.canRestore());
        assertNotEquals(firstPreview.planHash(), secondPreview.planHash());
        assertNoWriteCalls(restorePlanMapper, restorePlanItemMapper, restoreLogMapper, directoryAccessRuleMapper);
    }

    @Test
    void preview_blocksWhenTrusteeSidHasNoActiveMappedIdentity() {
        mockSnapshot(List.of(allowAce(UNMAPPED_SID, "hash-unmapped", RESTORABLE_ACCESS_MASK)), List.of());

        DccNasPermissionRestoreService.PreviewResult result = serviceContract.preview(TASK_ID);

        assertFalse(result.canRestore());
        assertEquals("REPLACE_DIRECTORY_RULES", result.restoreMode());
        assertEquals(1L, result.directoryCount());
        assertEquals(0L, result.ruleCount());
        assertEquals(1, result.blockers().size());
        DccNasPermissionRestoreService.RestoreBlocker blocker = result.blockers().get(0);
        assertEquals("DCC_NAS_PRINCIPAL_UNMAPPED", blocker.code());
        assertTrue(blocker.message().contains(UNMAPPED_SID));
        assertEquals(DIRECTORY_SNAPSHOT_ID, blocker.directorySnapshotId());
        assertEquals(NAS_PATH, blocker.nasPath());
        assertEquals(UNMAPPED_SID, blocker.trusteeSid());
        assertNoWriteCalls(restorePlanMapper, restorePlanItemMapper, restoreLogMapper, directoryAccessRuleMapper);
    }

    @Test
    void preview_blocksDenyAceAsUnrecoverableSemantic() {
        mockSnapshot(List.of(denyAce(MAPPED_SID, MAPPED_SID_HASH, RESTORABLE_ACCESS_MASK)),
                List.of(mappedIdentity(MAPPED_SID, MAPPED_SID_HASH)));

        DccNasPermissionRestoreService.PreviewResult result = serviceContract.preview(TASK_ID);

        assertFalse(result.canRestore());
        assertEquals(1L, result.directoryCount());
        assertEquals(0L, result.ruleCount());
        assertTrue(result.blockers().stream().anyMatch(blocker ->
                "DCC_NAS_ACL_DENY_UNSUPPORTED".equals(blocker.code())));
        assertTrue(result.blockers().stream().anyMatch(blocker ->
                blocker.message().contains("DENY") || blocker.message().contains("不可恢复")));
        assertNoWriteCalls(restorePlanMapper, restorePlanItemMapper, restoreLogMapper, directoryAccessRuleMapper);
    }

    @Test
    void preview_blocksNormalizedDenyAceAsUnrecoverableSemantic() {
        mockSnapshot(List.of(ace("DENY", MAPPED_SID, MAPPED_SID_HASH, RESTORABLE_ACCESS_MASK)),
                List.of(mappedIdentity(MAPPED_SID, MAPPED_SID_HASH)));

        DccNasPermissionRestoreService.PreviewResult result = serviceContract.preview(TASK_ID);

        assertFalse(result.canRestore());
        assertEquals(1L, result.directoryCount());
        assertEquals(0L, result.ruleCount());
        assertTrue(result.blockers().stream().anyMatch(blocker ->
                "DCC_NAS_ACL_DENY_UNSUPPORTED".equals(blocker.code())));
        assertNoWriteCalls(restorePlanMapper, restorePlanItemMapper, restoreLogMapper, directoryAccessRuleMapper);
    }

    @Test
    void preview_blocksUnsupportedAccessMaskAsUnrecoverableSemantic() {
        mockSnapshot(List.of(allowAce(MAPPED_SID, MAPPED_SID_HASH, 0L)),
                List.of(mappedIdentity(MAPPED_SID, MAPPED_SID_HASH)));

        DccNasPermissionRestoreService.PreviewResult result = serviceContract.preview(TASK_ID);

        assertFalse(result.canRestore());
        assertEquals(1L, result.directoryCount());
        assertEquals(0L, result.ruleCount());
        assertTrue(result.blockers().stream().anyMatch(blocker ->
                "DCC_NAS_ACL_SPECIAL_MASK_UNSUPPORTED".equals(blocker.code())));
        assertTrue(result.blockers().stream().anyMatch(blocker ->
                blocker.message().contains("accessMask") || blocker.message().contains("不可恢复")));
        assertNoWriteCalls(restorePlanMapper, restorePlanItemMapper, restoreLogMapper, directoryAccessRuleMapper);
    }

    @Test
    void preview_throwsServiceExceptionWhenSnapshotIsNotReady() {
        when(snapshotMapper.selectOne(anySnapshotWrapper())).thenReturn(null);

        assertServiceExceptionCode(() -> serviceContract.preview(TASK_ID), SNAPSHOT_NOT_READY_CODE);
        assertNoWriteCalls(restorePlanMapper, restorePlanItemMapper, restoreLogMapper, directoryAccessRuleMapper);
    }

    @Test
    void apply_requiresMatchingLatestPlanHashAndCreatesAuditableWaitingRestoreTaskOnly() {
        mockSnapshot(List.of(allowAce(MAPPED_SID, MAPPED_SID_HASH, RESTORABLE_ACCESS_MASK)),
                List.of(mappedIdentity(MAPPED_SID, MAPPED_SID_HASH)));
        doAnswer(invocation -> {
            DccNasAclRestorePlanDO plan = invocation.getArgument(0);
            plan.setId(8001L);
            return 1;
        }).when(restorePlanMapper).insert(any(DccNasAclRestorePlanDO.class));
        doAnswer(invocation -> {
            DccNasAclRestorePlanItemDO item = invocation.getArgument(0);
            item.setId(8101L);
            return 1;
        }).when(restorePlanItemMapper).insert(any(DccNasAclRestorePlanItemDO.class));
        DccNasPermissionRestoreService.PreviewResult preview = serviceContract.preview(TASK_ID);
        clearInvocations(identityMappingMapper, directoryAccessRuleMapper);

        DccNasPermissionRestoreService.ApplyResult result = serviceContract.apply(
                new DccNasPermissionRestoreService.ApplyRestoreCommand(
                        TASK_ID,
                        "restore-key-1",
                        preview.planHash(),
                        "REPLACE_DIRECTORY_RULES",
                        "按 NAS 转移权限快照恢复",
                        OPERATOR_USER_ID));

        assertEquals(8001L, result.restoreId());
        assertEquals(TASK_ID, result.taskId());
        assertEquals("WAITING", result.status());
        assertEquals(1L, result.directoryCount());
        assertEquals(1L, result.ruleCount());
        assertEquals(0L, result.completedDirectoryCount());
        assertEquals(0L, result.failedDirectoryCount());

        ArgumentCaptor<DccNasAclRestorePlanDO> planCaptor =
                ArgumentCaptor.forClass(DccNasAclRestorePlanDO.class);
        ArgumentCaptor<DccNasAclRestorePlanItemDO> itemCaptor =
                ArgumentCaptor.forClass(DccNasAclRestorePlanItemDO.class);
        org.mockito.Mockito.verify(restorePlanMapper).insert(planCaptor.capture());
        org.mockito.Mockito.verify(restorePlanItemMapper, atLeastOnce()).insert(itemCaptor.capture());

        DccNasAclRestorePlanDO plan = planCaptor.getValue();
        assertEquals(SNAPSHOT_ID, plan.getSnapshotId());
        assertEquals(TASK_ID, plan.getTransferTaskId());
        assertEquals("DCC_PERMISSION_RULES", plan.getTargetModel());
        assertEquals("READY", plan.getStatus());
        assertEquals(OPERATOR_USER_ID, plan.getCreatedByUserId());
        assertNotNull(plan.getPlanKey());
        assertTrue(plan.getValidationSummaryJson().contains(preview.planHash()));
        assertTrue(plan.getValidationSummaryJson().contains("\"completedDirectoryCount\":0"));
        assertTrue(plan.getValidationSummaryJson().contains("\"failedDirectoryCount\":0"));

        DccNasAclRestorePlanItemDO item = itemCaptor.getValue();
        assertEquals(DIRECTORY_SNAPSHOT_ID, item.getDirectorySnapshotId());
        assertEquals(DIRECTORY_ID, item.getDccDirectoryId());
        assertEquals(DESCRIPTOR_ID, item.getSourceDescriptorId());
        assertEquals("WAITING", item.getStatus());
        assertTrue(item.getPlannedOperationsJson().contains(String.valueOf(SUBJECT_ID)));
        assertTrue(item.getPlannedOperationsJson().contains("canDownload"));

        assertNoWriteCalls(restoreLogMapper, directoryAccessRuleMapper);
        verify(identityMappingMapper, times(1)).selectList(anyMappingWrapper());
        verify(identityMappingMapper, never()).selectOne(anyMappingWrapper());
        verify(directoryAccessRuleMapper, times(1)).selectList(anyDirectoryAccessRuleWrapper());
        verify(directoryAccessRuleMapper, never()).selectListByDirectoryId(any());

        clearInvocations(restorePlanMapper, restorePlanItemMapper, restoreLogMapper, directoryAccessRuleMapper);
        assertServiceExceptionCode(() -> serviceContract.apply(new DccNasPermissionRestoreService.ApplyRestoreCommand(
                TASK_ID,
                "restore-key-stale",
                "sha256:stale",
                "REPLACE_DIRECTORY_RULES",
                "按 NAS 转移权限快照恢复",
                OPERATOR_USER_ID)), PLAN_STALE_CODE);
        assertNoWriteCalls(restorePlanMapper, restorePlanItemMapper, restoreLogMapper, directoryAccessRuleMapper);
    }

    @Test
    void getStatus_failsFastWhenAuditCountsAreMissingFromValidationSummary() {
        DccNasAclRestorePlanDO plan = DccNasAclRestorePlanDO.builder()
                .id(8001L)
                .transferTaskId(TASK_ID)
                .status("EXECUTING")
                .validationSummaryJson("{\"ruleCount\":3}")
                .build();
        when(restorePlanMapper.selectById(8001L)).thenReturn(plan);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> serviceContract.getStatus(TASK_ID, 8001L));

        assertTrue(ex.getMessage().contains("directoryCount"));
        verify(restorePlanItemMapper, never()).selectList(anyPlanItemWrapper());
    }

    @Test
    void getStatus_returnsPollableProgressCountsFromCurrentItemsForExecutingPlan() {
        DccNasAclRestorePlanDO plan = DccNasAclRestorePlanDO.builder()
                .id(8001L)
                .transferTaskId(TASK_ID)
                .status("EXECUTING")
                .validationSummaryJson("{\"directoryCount\":4,\"ruleCount\":7,"
                        + "\"completedDirectoryCount\":0,\"failedDirectoryCount\":0}")
                .build();
        when(restorePlanMapper.selectById(8001L)).thenReturn(plan);
        when(restorePlanItemMapper.selectList(anyPlanItemWrapper())).thenReturn(List.of(
                DccNasAclRestorePlanItemDO.builder().id(8101L).planId(8001L).status("VERIFIED").build(),
                DccNasAclRestorePlanItemDO.builder().id(8102L).planId(8001L).status("FAILED").build(),
                DccNasAclRestorePlanItemDO.builder().id(8103L).planId(8001L).status("WAITING").build(),
                DccNasAclRestorePlanItemDO.builder().id(8104L).planId(8001L).status("VERIFIED").build()));

        DccNasPermissionRestoreService.RestoreStatusResult result = serviceContract.getStatus(TASK_ID, 8001L);

        assertEquals(8001L, result.restoreId());
        assertEquals("EXECUTING", result.status());
        assertEquals(4L, result.directoryCount());
        assertEquals(7L, result.ruleCount());
        assertEquals(2L, result.completedDirectoryCount());
        assertEquals(1L, result.failedDirectoryCount());
    }

    @Test
    void apply_throwsServiceExceptionForUnsupportedModeAndBlockedRestore() {
        assertServiceExceptionCode(() -> serviceContract.apply(new DccNasPermissionRestoreService.ApplyRestoreCommand(
                TASK_ID,
                "restore-key-unsupported",
                "sha256:any",
                "MERGE_DIRECTORY_RULES",
                "按 NAS 转移权限快照恢复",
                OPERATOR_USER_ID)), UNSUPPORTED_MODE_CODE);
        assertNoWriteCalls(restorePlanMapper, restorePlanItemMapper, restoreLogMapper, directoryAccessRuleMapper);

        mockSnapshot(List.of(denyAce(MAPPED_SID, MAPPED_SID_HASH, RESTORABLE_ACCESS_MASK)),
                List.of(mappedIdentity(MAPPED_SID, MAPPED_SID_HASH)));
        DccNasPermissionRestoreService.PreviewResult preview = serviceContract.preview(TASK_ID);
        clearInvocations(restorePlanMapper, restorePlanItemMapper, restoreLogMapper, directoryAccessRuleMapper);

        assertServiceExceptionCode(() -> serviceContract.apply(new DccNasPermissionRestoreService.ApplyRestoreCommand(
                TASK_ID,
                "restore-key-blocked",
                preview.planHash(),
                "REPLACE_DIRECTORY_RULES",
                "按 NAS 转移权限快照恢复",
                OPERATOR_USER_ID)), RESTORE_BLOCKED_CODE);
        assertNoWriteCalls(restorePlanMapper, restorePlanItemMapper, restoreLogMapper, directoryAccessRuleMapper);
    }

    @Test
    void apply_isIdempotentForSameRequestAndRejectsDifferentReplayWithoutWrites() {
        mockSnapshot(List.of(allowAce(MAPPED_SID, MAPPED_SID_HASH, RESTORABLE_ACCESS_MASK)),
                List.of(mappedIdentity(MAPPED_SID, MAPPED_SID_HASH)));
        AtomicReference<DccNasAclRestorePlanDO> insertedPlan = new AtomicReference<>();
        doAnswer(invocation -> {
            DccNasAclRestorePlanDO plan = invocation.getArgument(0);
            plan.setId(8001L);
            insertedPlan.set(plan);
            return 1;
        }).when(restorePlanMapper).insert(any(DccNasAclRestorePlanDO.class));
        doAnswer(invocation -> {
            DccNasAclRestorePlanItemDO item = invocation.getArgument(0);
            item.setId(8101L);
            return 1;
        }).when(restorePlanItemMapper).insert(any(DccNasAclRestorePlanItemDO.class));
        DccNasPermissionRestoreService.PreviewResult preview = serviceContract.preview(TASK_ID);
        DccNasPermissionRestoreService.ApplyRestoreCommand command =
                new DccNasPermissionRestoreService.ApplyRestoreCommand(
                        TASK_ID,
                        "restore-key-idempotent",
                        preview.planHash(),
                        "REPLACE_DIRECTORY_RULES",
                        "按 NAS 转移权限快照恢复",
                        OPERATOR_USER_ID);
        DccNasPermissionRestoreService.ApplyResult firstResult = serviceContract.apply(command);
        assertNotNull(insertedPlan.get());

        when(restorePlanMapper.selectOne(anyPlanWrapper())).thenReturn(insertedPlan.get());
        clearInvocations(snapshotMapper, directorySnapshotMapper, aceMapper, identityMappingMapper,
                restorePlanMapper, restorePlanItemMapper, restoreLogMapper, directoryAccessRuleMapper);

        DccNasPermissionRestoreService.ApplyResult replayResult = serviceContract.apply(command);

        assertEquals(firstResult, replayResult);
        verify(restorePlanMapper, never()).insert(any(DccNasAclRestorePlanDO.class));
        verify(restorePlanItemMapper, never()).insert(any(DccNasAclRestorePlanItemDO.class));
        assertNoWriteCalls(restorePlanMapper, restorePlanItemMapper, restoreLogMapper, directoryAccessRuleMapper);

        clearInvocations(snapshotMapper, directorySnapshotMapper, aceMapper, identityMappingMapper,
                restorePlanMapper, restorePlanItemMapper, restoreLogMapper, directoryAccessRuleMapper);
        assertServiceExceptionCode(() -> serviceContract.apply(new DccNasPermissionRestoreService.ApplyRestoreCommand(
                TASK_ID,
                "restore-key-idempotent",
                "sha256:different-plan",
                "REPLACE_DIRECTORY_RULES",
                "按 NAS 转移权限快照恢复",
                OPERATOR_USER_ID)), IDEMPOTENCY_CONFLICT_CODE);
        verify(restorePlanMapper, never()).insert(any(DccNasAclRestorePlanDO.class));
        verify(restorePlanItemMapper, never()).insert(any(DccNasAclRestorePlanItemDO.class));
        assertNoWriteCalls(restorePlanMapper, restorePlanItemMapper, restoreLogMapper, directoryAccessRuleMapper);

        clearInvocations(snapshotMapper, directorySnapshotMapper, aceMapper, identityMappingMapper,
                restorePlanMapper, restorePlanItemMapper, restoreLogMapper, directoryAccessRuleMapper);
        assertServiceExceptionCode(() -> serviceContract.apply(new DccNasPermissionRestoreService.ApplyRestoreCommand(
                TASK_ID,
                "restore-key-idempotent",
                preview.planHash(),
                "REPLACE_DIRECTORY_RULES",
                "改用另一条恢复原因",
                OPERATOR_USER_ID)), IDEMPOTENCY_CONFLICT_CODE);
        verify(restorePlanMapper, never()).insert(any(DccNasAclRestorePlanDO.class));
        verify(restorePlanItemMapper, never()).insert(any(DccNasAclRestorePlanItemDO.class));
        assertNoWriteCalls(restorePlanMapper, restorePlanItemMapper, restoreLogMapper, directoryAccessRuleMapper);
    }

    private void mockSnapshot(List<DccNasAclAceDO> aces,
                              List<DccNasAclIdentityMappingDO> mappings) {
        mockSnapshot(List.of(successDirectorySnapshot()), aces, mappings, List.of());
    }

    private void mockSnapshot(List<DccNasAclAceDO> aces,
                              List<DccNasAclIdentityMappingDO> mappings,
                              List<DccDirectoryAccessRuleDO> runtimeRules) {
        mockSnapshot(List.of(successDirectorySnapshot()), aces, mappings, runtimeRules);
    }

    private void mockSnapshot(List<DccNasAclDirectorySnapshotDO> directorySnapshots,
                              List<DccNasAclAceDO> aces,
                              List<DccNasAclIdentityMappingDO> mappings,
                              List<DccDirectoryAccessRuleDO> runtimeRules) {
        DccNasAclSnapshotDO snapshot = capturedSnapshot();
        when(snapshotMapper.selectOne(anySnapshotWrapper())).thenReturn(snapshot);
        when(snapshotMapper.selectById(SNAPSHOT_ID)).thenReturn(snapshot);
        when(directorySnapshotMapper.selectList(anyDirectorySnapshotWrapper()))
                .thenReturn(directorySnapshots);
        when(aceMapper.selectList(anyAceWrapper())).thenReturn(aces);
        when(identityMappingMapper.selectList(anyMappingWrapper())).thenReturn(mappings);
        lenient().when(identityMappingMapper.selectOne(anyMappingWrapper()))
                .thenReturn(mappings.isEmpty() ? null : mappings.get(0));
        when(directoryAccessRuleMapper.selectList(anyDirectoryAccessRuleWrapper())).thenReturn(runtimeRules);
        lenient().when(directoryAccessRuleMapper.selectListByDirectoryId(any())).thenReturn(List.of());
    }

    private static DccNasAclSnapshotDO capturedSnapshot() {
        return DccNasAclSnapshotDO.builder()
                .id(SNAPSHOT_ID)
                .transferTaskId(TASK_ID)
                .snapshotKey("NAS_ACL_SNAPSHOT:" + TASK_ID)
                .status("CAPTURED")
                .normalizationVersion("NAS_ACL_V1")
                .totalDirectoryCount(1L)
                .snapshottedDirectoryCount(1L)
                .failedDirectoryCount(0L)
                .build();
    }

    private static DccNasAclDirectorySnapshotDO successDirectorySnapshot() {
        return successDirectorySnapshot(DIRECTORY_SNAPSHOT_ID, DESCRIPTOR_ID, DIRECTORY_ID, NAS_PATH);
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
                .pathHash("path-hash-" + id)
                .itemName(nasPath.substring(nasPath.lastIndexOf('/') + 1))
                .descriptorId(descriptorId)
                .collectStatus("SUCCESS")
                .build();
    }

    private static DccNasAclAceDO allowAce(String sid, String sidHash, Long accessMask) {
        return ace("ACCESS_ALLOWED_ACE_TYPE", sid, sidHash, accessMask);
    }

    private static DccNasAclAceDO denyAce(String sid, String sidHash, Long accessMask) {
        return ace("ACCESS_DENIED_ACE_TYPE", sid, sidHash, accessMask);
    }

    private static DccNasAclAceDO ace(String aceType, String sid, String sidHash, Long accessMask) {
        return allowOrDenyAce(7201L, DESCRIPTOR_ID, aceType, sid, sidHash, accessMask);
    }

    private static DccNasAclAceDO allowAce(Long id, Long descriptorId, String sid, String sidHash, Long accessMask) {
        return allowOrDenyAce(id, descriptorId, "ACCESS_ALLOWED_ACE_TYPE", sid, sidHash, accessMask);
    }

    private static DccNasAclAceDO allowOrDenyAce(Long id,
                                                 Long descriptorId,
                                                 String aceType,
                                                 String sid,
                                                 String sidHash,
                                                 Long accessMask) {
        return DccNasAclAceDO.builder()
                .id(id)
                .descriptorId(descriptorId)
                .aceIndex(0)
                .aceType(aceType)
                .accessMask(accessMask)
                .trusteeSid(sid)
                .trusteeSidHash(sidHash)
                .inherited(false)
                .rawAceJson("{\"aceType\":\"" + aceType + "\",\"accessMask\":" + accessMask + "}")
                .build();
    }

    private static DccNasAclIdentityMappingDO mappedIdentity(String sid, String sidHash) {
        return mappedIdentity(7401L, sid, sidHash, SUBJECT_ID);
    }

    private static DccNasAclIdentityMappingDO mappedIdentity(Long id, String sid, String sidHash, Long subjectId) {
        return DccNasAclIdentityMappingDO.builder()
                .id(id)
                .sid(sid)
                .sidHash(sidHash)
                .mappingStatus("MAPPED")
                .dccSubjectType("USER")
                .dccSubjectId(subjectId)
                .mappingMethod("MANUAL")
                .build();
    }

    private static DccDirectoryAccessRuleDO runtimeRule(Long id, Long directoryId, Long subjectId) {
        return runtimeRule(id, directoryId, subjectId, null);
    }

    private static DccDirectoryAccessRuleDO runtimeRule(Long id,
                                                        Long directoryId,
                                                        Long subjectId,
                                                        String changeReason) {
        return DccDirectoryAccessRuleDO.builder()
                .id(id)
                .directoryId(directoryId)
                .subjectType("USER")
                .subjectId(subjectId)
                .canQuery(true)
                .canPreview(true)
                .canDownload(true)
                .active(true)
                .changeReason(changeReason)
                .build();
    }

    private static Wrapper<DccNasAclSnapshotDO> anySnapshotWrapper() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static Wrapper<DccNasAclDirectorySnapshotDO> anyDirectorySnapshotWrapper() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static Wrapper<DccNasAclAceDO> anyAceWrapper() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static Wrapper<DccNasAclIdentityMappingDO> anyMappingWrapper() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static Wrapper<DccNasAclRestorePlanDO> anyPlanWrapper() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static Wrapper<DccNasAclRestorePlanItemDO> anyPlanItemWrapper() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static Wrapper<DccDirectoryAccessRuleDO> anyDirectoryAccessRuleWrapper() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static void assertServiceExceptionCode(org.junit.jupiter.api.function.Executable executable,
                                                   int expectedCode) {
        ServiceException exception = assertThrows(ServiceException.class, executable);
        assertEquals(expectedCode, exception.getCode());
    }

    private static void assertNoWriteCalls(Object... mocks) {
        for (Object mock : mocks) {
            List<String> writeCalls = mockingDetails(mock).getInvocations().stream()
                    .map(invocation -> invocation.getMethod().getName())
                    .filter(DccNasPermissionRestoreServiceTest::isWriteMethod)
                    .toList();
            String mockName = mockingDetails(mock).getMockCreationSettings().getTypeToMock().getSimpleName();
            assertTrue(writeCalls.isEmpty(), "Expected no write calls on " + mockName + " but got " + writeCalls);
        }
    }

    private static boolean isWriteMethod(String methodName) {
        return methodName.startsWith("insert")
                || methodName.startsWith("update")
                || methodName.startsWith("delete");
    }
}
