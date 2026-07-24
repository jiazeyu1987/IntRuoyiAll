package cn.iocoder.yudao.module.dcc.service.permission;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccDirectoryAccessRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclRestoreLogDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclRestorePlanDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclRestorePlanItemDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccDirectoryAccessRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclRestoreLogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclRestorePlanItemMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclRestorePlanMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccNasPermissionRestoreExecutionServiceTest extends BaseMockitoUnitTest {

    private static final Long RESTORE_ID = 8001L;
    private static final Long SNAPSHOT_ID = 7001L;
    private static final Long TASK_ID = 10L;
    private static final Long DIRECTORY_SNAPSHOT_ID = 7301L;
    private static final Long TRANSFER_TASK_ITEM_ID = 1001L;
    private static final Long DIRECTORY_ID = 902634L;
    private static final Long DESCRIPTOR_ID = 7101L;
    private static final Long OPERATOR_USER_ID = 99L;

    @Mock
    private DccNasAclRestorePlanMapper restorePlanMapper;
    @Mock
    private DccNasAclRestorePlanItemMapper restorePlanItemMapper;
    @Mock
    private DccNasAclRestoreLogMapper restoreLogMapper;
    @Mock
    private DccDirectoryAccessRuleMapper directoryAccessRuleMapper;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private TransactionStatus transactionStatus;

    @InjectMocks
    private DccNasPermissionRestoreExecutionServiceImpl restoreExecutionService;

    private DccNasPermissionRestoreExecutionService serviceContract;

    @BeforeEach
    void setUpContract() {
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(transactionStatus);
        });
        lenient().when(restorePlanMapper.refreshExecutingPlanLease(eq(RESTORE_ID),
                any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1);
        serviceContract = restoreExecutionService;
    }

    @Test
    void processWaitingRestorePlans_replacesDirectoryRulesAndWritesValidateApplyVerifyLogs() {
        DccDirectoryAccessRuleDO currentRule = runtimeRule(7601L, "USER", 201L,
                true, false, false, true, "existing DCC rule");
        DccDirectoryAccessRuleDO targetRule = runtimeRule(null, "USER", 901L,
                true, true, true, true, "restore from NAS ACL snapshot");
        String expectedCurrentHash = directoryRulesHash(List.of(currentRule));
        String expectedAfterHash = directoryRulesHash(List.of(targetRule));
        DccNasAclRestorePlanDO readyPlan = readyRestorePlan();
        DccNasAclRestorePlanItemDO waitingItem = waitingRestoreItem(expectedCurrentHash,
                expectedAfterHash, List.of(targetRule));

        when(restorePlanMapper.selectList(anyPlanWrapper())).thenReturn(List.of(readyPlan));
        mockClaimReadyPlanSucceeded();
        when(restorePlanItemMapper.selectList(anyItemWrapper())).thenReturn(List.of(waitingItem));
        when(directoryAccessRuleMapper.selectList(anyDirectoryAccessRuleWrapper()))
                .thenReturn(List.of(currentRule), List.of(targetRule));

        serviceContract.processWaitingRestorePlans();

        verifyReadyPlanClaimed();
        assertDirectoryRulesDeletedBeforeInserted(directoryAccessRuleMapper);

        ArgumentCaptor<DccDirectoryAccessRuleDO> insertedRuleCaptor =
                ArgumentCaptor.forClass(DccDirectoryAccessRuleDO.class);
        verify(directoryAccessRuleMapper, atLeastOnce()).insert(insertedRuleCaptor.capture());
        List<DccDirectoryAccessRuleDO> insertedRules = insertedRuleCaptor.getAllValues();
        assertEquals(1, insertedRules.size());
        DccDirectoryAccessRuleDO insertedRule = insertedRules.get(0);
        assertEquals(DIRECTORY_ID, insertedRule.getDirectoryId());
        assertEquals("USER", insertedRule.getSubjectType());
        assertEquals(901L, insertedRule.getSubjectId());
        assertTrue(insertedRule.getCanQuery());
        assertTrue(insertedRule.getCanPreview());
        assertTrue(insertedRule.getCanDownload());
        assertTrue(insertedRule.getActive());
        assertEquals("restore from NAS ACL snapshot", insertedRule.getChangeReason());

        ArgumentCaptor<DccNasAclRestorePlanItemDO> itemCaptor =
                ArgumentCaptor.forClass(DccNasAclRestorePlanItemDO.class);
        verify(restorePlanItemMapper, atLeastOnce()).updateById(itemCaptor.capture());
        DccNasAclRestorePlanItemDO verifiedItem = lastCaptured(itemCaptor.getAllValues());
        assertEquals(waitingItem.getId(), verifiedItem.getId());
        assertEquals("VERIFIED", verifiedItem.getStatus());
        assertEquals(expectedAfterHash, verifiedItem.getExpectedAfterHash());
        assertEquals(expectedAfterHash, verifiedItem.getActualAfterHash());
        assertNotNull(verifiedItem.getVerifiedAt());
        assertFalse(containsText(verifiedItem.getBlockReason()));

        ArgumentCaptor<DccNasAclRestorePlanDO> planCaptor =
                ArgumentCaptor.forClass(DccNasAclRestorePlanDO.class);
        verify(restorePlanMapper, atLeastOnce()).updateById(planCaptor.capture());
        DccNasAclRestorePlanDO completedPlan = lastCaptured(planCaptor.getAllValues());
        assertEquals(RESTORE_ID, completedPlan.getId());
        assertEquals("COMPLETED", completedPlan.getStatus());
        assertNotNull(completedPlan.getStartedAt());
        assertNotNull(completedPlan.getCompletedAt());
        assertTrue(completedPlan.getValidationSummaryJson().contains("\"completedDirectoryCount\":1"));
        assertTrue(completedPlan.getValidationSummaryJson().contains("\"failedDirectoryCount\":0"));

        ArgumentCaptor<DccNasAclRestoreLogDO> logCaptor =
                ArgumentCaptor.forClass(DccNasAclRestoreLogDO.class);
        verify(restoreLogMapper, times(3)).insert(logCaptor.capture());
        List<DccNasAclRestoreLogDO> logs = logCaptor.getAllValues();
        assertLog(logs.get(0), "VALIDATE", "SUCCEEDED", expectedCurrentHash, expectedAfterHash, null);
        assertLog(logs.get(1), "APPLY", "SUCCEEDED", expectedCurrentHash, expectedAfterHash, null);
        assertLog(logs.get(2), "VERIFY", "SUCCEEDED", expectedAfterHash, expectedAfterHash, null);
        assertTrue(logs.stream().allMatch(log -> RESTORE_ID.equals(log.getPlanId())));
        assertTrue(logs.stream().allMatch(log -> waitingItem.getId().equals(log.getPlanItemId())));
        assertTrue(logs.stream().allMatch(log -> OPERATOR_USER_ID.equals(log.getOperatorUserId())));
        verify(transactionTemplate, times(1)).execute(any());
    }

    @Test
    void processWaitingRestorePlans_blocksRestoreWhenCurrentDirectoryRuleHashChangedAfterPreview() {
        DccDirectoryAccessRuleDO currentRuleAfterPreview = runtimeRule(7602L, "USER", 201L,
                true, true, false, true, "manual change after preview");
        DccDirectoryAccessRuleDO previewCurrentRule = runtimeRule(7601L, "USER", 201L,
                true, false, false, true, "existing DCC rule");
        DccDirectoryAccessRuleDO targetRule = runtimeRule(null, "USER", 901L,
                true, true, true, true, "restore from NAS ACL snapshot");
        String staleExpectedCurrentHash = directoryRulesHash(List.of(previewCurrentRule));
        String actualCurrentHash = directoryRulesHash(List.of(currentRuleAfterPreview));
        String expectedAfterHash = directoryRulesHash(List.of(targetRule));
        DccNasAclRestorePlanItemDO waitingItem = waitingRestoreItem(staleExpectedCurrentHash,
                expectedAfterHash, List.of(targetRule));

        when(restorePlanMapper.selectList(anyPlanWrapper())).thenReturn(List.of(readyRestorePlan()));
        mockClaimReadyPlanSucceeded();
        when(restorePlanItemMapper.selectList(anyItemWrapper())).thenReturn(List.of(waitingItem));
        when(directoryAccessRuleMapper.selectList(anyDirectoryAccessRuleWrapper()))
                .thenReturn(List.of(currentRuleAfterPreview));

        serviceContract.processWaitingRestorePlans();

        verifyReadyPlanClaimed();
        assertNoWriteCalls(directoryAccessRuleMapper);

        ArgumentCaptor<DccNasAclRestorePlanItemDO> itemCaptor =
                ArgumentCaptor.forClass(DccNasAclRestorePlanItemDO.class);
        verify(restorePlanItemMapper, atLeastOnce()).updateById(itemCaptor.capture());
        DccNasAclRestorePlanItemDO blockedItem = lastCaptured(itemCaptor.getAllValues());
        assertEquals(waitingItem.getId(), blockedItem.getId());
        assertTrue(List.of("FAILED", "BLOCKED").contains(blockedItem.getStatus()));
        assertEquals(expectedAfterHash, blockedItem.getExpectedAfterHash());
        assertEquals(actualCurrentHash, blockedItem.getActualAfterHash());
        assertTrue(blockedItem.getBlockReason().contains("DCC_NAS_ACL_RESTORE_CURRENT_HASH_MISMATCH"));

        ArgumentCaptor<DccNasAclRestorePlanDO> planCaptor =
                ArgumentCaptor.forClass(DccNasAclRestorePlanDO.class);
        verify(restorePlanMapper, atLeastOnce()).updateById(planCaptor.capture());
        DccNasAclRestorePlanDO failedPlan = lastCaptured(planCaptor.getAllValues());
        assertEquals(RESTORE_ID, failedPlan.getId());
        assertEquals("FAILED", failedPlan.getStatus());
        assertEquals("DCC_NAS_ACL_RESTORE_CURRENT_HASH_MISMATCH", failedPlan.getFailureCode());
        assertTrue(failedPlan.getFailureMessage().contains(DIRECTORY_ID.toString()));
        assertTrue(failedPlan.getValidationSummaryJson().contains("\"completedDirectoryCount\":0"));
        assertTrue(failedPlan.getValidationSummaryJson().contains("\"failedDirectoryCount\":1"));

        ArgumentCaptor<DccNasAclRestoreLogDO> logCaptor =
                ArgumentCaptor.forClass(DccNasAclRestoreLogDO.class);
        verify(restoreLogMapper, times(1)).insert(logCaptor.capture());
        DccNasAclRestoreLogDO validateLog = logCaptor.getValue();
        assertEquals("VALIDATE", validateLog.getActionType());
        assertEquals("FAILED", validateLog.getStatus());
        assertEquals(actualCurrentHash, validateLog.getBeforeHash());
        assertEquals(expectedAfterHash, validateLog.getExpectedAfterHash());
        assertNull(validateLog.getActualAfterHash());
        assertEquals("DCC_NAS_ACL_RESTORE_CURRENT_HASH_MISMATCH", validateLog.getErrorCode());
        assertTrue(validateLog.getErrorMessage().contains(staleExpectedCurrentHash));
        assertTrue(validateLog.getErrorMessage().contains(DIRECTORY_ID.toString()));
        verify(transactionTemplate, times(1)).execute(any());
    }

    @Test
    void processWaitingRestorePlans_executesEachWaitingItemInShortTransaction() {
        Long secondDirectorySnapshotId = 7302L;
        Long secondTransferTaskItemId = 1002L;
        Long secondDirectoryId = 902635L;
        DccDirectoryAccessRuleDO currentRule = runtimeRule(7601L, DIRECTORY_ID, "USER", 201L,
                true, false, false, true, "existing DCC rule");
        DccDirectoryAccessRuleDO targetRule = runtimeRule(null, DIRECTORY_ID, "USER", 901L,
                true, true, true, true, "restore from NAS ACL snapshot");
        DccDirectoryAccessRuleDO secondCurrentRule = runtimeRule(7602L, secondDirectoryId, "USER", 202L,
                true, false, false, true, "existing DCC rule 2");
        DccDirectoryAccessRuleDO secondTargetRule = runtimeRule(null, secondDirectoryId, "USER", 902L,
                true, true, true, true, "restore from NAS ACL snapshot 2");
        DccNasAclRestorePlanItemDO firstItem = waitingRestoreItem(
                8101L,
                DIRECTORY_SNAPSHOT_ID,
                TRANSFER_TASK_ITEM_ID,
                DIRECTORY_ID,
                directoryRulesHash(List.of(currentRule)),
                directoryRulesHash(List.of(targetRule)),
                List.of(targetRule));
        DccNasAclRestorePlanItemDO secondItem = waitingRestoreItem(
                8102L,
                secondDirectorySnapshotId,
                secondTransferTaskItemId,
                secondDirectoryId,
                directoryRulesHash(List.of(secondCurrentRule)),
                directoryRulesHash(List.of(secondTargetRule)),
                List.of(secondTargetRule));

        when(restorePlanMapper.selectList(anyPlanWrapper())).thenReturn(List.of(readyRestorePlan(
                "{\"planHash\":\"sha256:preview\",\"directoryCount\":2,"
                        + "\"ruleCount\":2,\"completedDirectoryCount\":0,\"failedDirectoryCount\":0}")));
        mockClaimReadyPlanSucceeded();
        when(restorePlanItemMapper.selectList(anyItemWrapper())).thenReturn(List.of(firstItem, secondItem));
        when(directoryAccessRuleMapper.selectList(anyDirectoryAccessRuleWrapper()))
                .thenReturn(List.of(currentRule), List.of(targetRule),
                        List.of(secondCurrentRule), List.of(secondTargetRule));

        serviceContract.processWaitingRestorePlans();

        verifyReadyPlanClaimed();
        verify(transactionTemplate, times(2)).execute(any());
    }

    @Test
    void processWaitingRestorePlans_refreshesExecutingLeaseBeforeEachExecutableItem() {
        Long secondDirectorySnapshotId = 7302L;
        Long secondTransferTaskItemId = 1002L;
        Long secondDirectoryId = 902635L;
        DccDirectoryAccessRuleDO currentRule = runtimeRule(7601L, DIRECTORY_ID, "USER", 201L,
                true, false, false, true, "existing DCC rule");
        DccDirectoryAccessRuleDO targetRule = runtimeRule(null, DIRECTORY_ID, "USER", 901L,
                true, true, true, true, "restore from NAS ACL snapshot");
        DccDirectoryAccessRuleDO secondCurrentRule = runtimeRule(7602L, secondDirectoryId, "USER", 202L,
                true, false, false, true, "existing DCC rule 2");
        DccDirectoryAccessRuleDO secondTargetRule = runtimeRule(null, secondDirectoryId, "USER", 902L,
                true, true, true, true, "restore from NAS ACL snapshot 2");
        DccNasAclRestorePlanItemDO firstItem = waitingRestoreItem(
                8101L,
                DIRECTORY_SNAPSHOT_ID,
                TRANSFER_TASK_ITEM_ID,
                DIRECTORY_ID,
                directoryRulesHash(List.of(currentRule)),
                directoryRulesHash(List.of(targetRule)),
                List.of(targetRule));
        DccNasAclRestorePlanItemDO secondItem = waitingRestoreItem(
                8102L,
                secondDirectorySnapshotId,
                secondTransferTaskItemId,
                secondDirectoryId,
                directoryRulesHash(List.of(secondCurrentRule)),
                directoryRulesHash(List.of(secondTargetRule)),
                List.of(secondTargetRule));

        when(restorePlanMapper.selectList(anyPlanWrapper())).thenReturn(List.of(readyRestorePlan(
                "{\"planHash\":\"sha256:preview\",\"directoryCount\":2,"
                        + "\"ruleCount\":2,\"completedDirectoryCount\":0,\"failedDirectoryCount\":0}")));
        mockClaimReadyPlanSucceeded();
        when(restorePlanItemMapper.selectList(anyItemWrapper())).thenReturn(List.of(firstItem, secondItem));
        when(directoryAccessRuleMapper.selectList(anyDirectoryAccessRuleWrapper()))
                .thenReturn(List.of(currentRule), List.of(targetRule),
                        List.of(secondCurrentRule), List.of(secondTargetRule));

        serviceContract.processWaitingRestorePlans();

        ArgumentCaptor<LocalDateTime> currentLeaseCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> refreshedLeaseCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(restorePlanMapper, times(2)).refreshExecutingPlanLease(eq(RESTORE_ID),
                currentLeaseCaptor.capture(), refreshedLeaseCaptor.capture());
        List<LocalDateTime> currentLeases = currentLeaseCaptor.getAllValues();
        List<LocalDateTime> refreshedLeases = refreshedLeaseCaptor.getAllValues();
        ArgumentCaptor<LocalDateTime> claimedLeaseCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(restorePlanMapper, atLeastOnce()).claimReadyPlan(eq(RESTORE_ID), claimedLeaseCaptor.capture());
        LocalDateTime claimedLease = lastCaptured(claimedLeaseCaptor.getAllValues());
        assertSecondPrecision(claimedLease);
        assertEquals(claimedLease, currentLeases.get(0));
        assertEquals(refreshedLeases.get(0), currentLeases.get(1));
        currentLeases.forEach(DccNasPermissionRestoreExecutionServiceTest::assertSecondPrecision);
        refreshedLeases.forEach(DccNasPermissionRestoreExecutionServiceTest::assertSecondPrecision);
        assertTrue(currentLeases.stream().allMatch(Objects::nonNull));
        assertTrue(refreshedLeases.stream().allMatch(Objects::nonNull));
        verify(transactionTemplate, times(2)).execute(any());
    }

    @Test
    void processWaitingRestorePlans_stopsBeforeNextItemWhenLeaseRefreshFails() {
        Long secondDirectoryId = 902635L;
        DccDirectoryAccessRuleDO currentRule = runtimeRule(7601L, DIRECTORY_ID, "USER", 201L,
                true, false, false, true, "existing DCC rule");
        DccDirectoryAccessRuleDO targetRule = runtimeRule(null, DIRECTORY_ID, "USER", 901L,
                true, true, true, true, "restore from NAS ACL snapshot");
        DccDirectoryAccessRuleDO secondCurrentRule = runtimeRule(7602L, secondDirectoryId, "USER", 202L,
                true, false, false, true, "existing DCC rule 2");
        DccDirectoryAccessRuleDO secondTargetRule = runtimeRule(null, secondDirectoryId, "USER", 902L,
                true, true, true, true, "restore from NAS ACL snapshot 2");
        DccNasAclRestorePlanItemDO firstItem = waitingRestoreItem(
                8101L,
                DIRECTORY_SNAPSHOT_ID,
                TRANSFER_TASK_ITEM_ID,
                DIRECTORY_ID,
                directoryRulesHash(List.of(currentRule)),
                directoryRulesHash(List.of(targetRule)),
                List.of(targetRule));
        DccNasAclRestorePlanItemDO secondItem = waitingRestoreItem(
                8102L,
                7302L,
                1002L,
                secondDirectoryId,
                directoryRulesHash(List.of(secondCurrentRule)),
                directoryRulesHash(List.of(secondTargetRule)),
                List.of(secondTargetRule));

        when(restorePlanMapper.selectList(anyPlanWrapper())).thenReturn(List.of(readyRestorePlan(
                "{\"planHash\":\"sha256:preview\",\"directoryCount\":2,"
                        + "\"ruleCount\":2,\"completedDirectoryCount\":0,\"failedDirectoryCount\":0}")));
        mockClaimReadyPlanSucceeded();
        when(restorePlanMapper.refreshExecutingPlanLease(eq(RESTORE_ID),
                any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1, 0);
        when(restorePlanItemMapper.selectList(anyItemWrapper())).thenReturn(List.of(firstItem, secondItem));
        when(directoryAccessRuleMapper.selectList(anyDirectoryAccessRuleWrapper()))
                .thenReturn(List.of(currentRule), List.of(targetRule));

        serviceContract.processWaitingRestorePlans();

        verify(restorePlanMapper, times(2)).refreshExecutingPlanLease(eq(RESTORE_ID),
                any(LocalDateTime.class), any(LocalDateTime.class));
        verify(transactionTemplate, times(1)).execute(any());
        verify(restoreLogMapper, times(3)).insert(any(DccNasAclRestoreLogDO.class));
        verify(restorePlanMapper, never()).updateById(any(DccNasAclRestorePlanDO.class));
    }

    @Test
    void processWaitingRestorePlans_completesPlanWithPreviouslyVerifiedItemCountWhenResuming() {
        Long verifiedDirectoryId = 902633L;
        DccDirectoryAccessRuleDO currentRule = runtimeRule(7601L, DIRECTORY_ID, "USER", 201L,
                true, false, false, true, "existing DCC rule");
        DccDirectoryAccessRuleDO targetRule = runtimeRule(null, DIRECTORY_ID, "USER", 901L,
                true, true, true, true, "restore from NAS ACL snapshot");
        DccNasAclRestorePlanItemDO verifiedItem = verifiedRestoreItem(8100L, verifiedDirectoryId);
        DccNasAclRestorePlanItemDO waitingItem = waitingRestoreItem(
                8101L,
                DIRECTORY_SNAPSHOT_ID,
                TRANSFER_TASK_ITEM_ID,
                DIRECTORY_ID,
                directoryRulesHash(List.of(currentRule)),
                directoryRulesHash(List.of(targetRule)),
                List.of(targetRule));
        DccNasAclRestorePlanDO resumedPlan = readyRestorePlan("{\"planHash\":\"sha256:preview\","
                + "\"directoryCount\":2,\"ruleCount\":2,\"completedDirectoryCount\":1,"
                + "\"failedDirectoryCount\":0}");

        when(restorePlanMapper.selectList(anyPlanWrapper())).thenReturn(List.of(resumedPlan));
        mockClaimReadyPlanSucceeded();
        when(restorePlanItemMapper.selectList(anyItemWrapper()))
                .thenReturn(List.of(verifiedItem, waitingItem), List.of(verifiedItem, waitingItem));
        when(directoryAccessRuleMapper.selectList(anyDirectoryAccessRuleWrapper()))
                .thenReturn(List.of(currentRule), List.of(targetRule));

        serviceContract.processWaitingRestorePlans();

        verifyReadyPlanClaimed();
        ArgumentCaptor<DccNasAclRestorePlanDO> planCaptor =
                ArgumentCaptor.forClass(DccNasAclRestorePlanDO.class);
        verify(restorePlanMapper, atLeastOnce()).updateById(planCaptor.capture());
        DccNasAclRestorePlanDO completedPlan = lastCaptured(planCaptor.getAllValues());
        assertEquals(RESTORE_ID, completedPlan.getId());
        assertEquals("COMPLETED", completedPlan.getStatus());
        assertTrue(completedPlan.getValidationSummaryJson().contains("\"completedDirectoryCount\":2"));
        assertTrue(completedPlan.getValidationSummaryJson().contains("\"failedDirectoryCount\":0"));
        verify(transactionTemplate, times(1)).execute(any());
    }

    @Test
    void processWaitingRestorePlans_skipsReadyPlanWhenAtomicClaimFails() {
        when(restorePlanMapper.selectList(anyPlanWrapper())).thenReturn(List.of(readyRestorePlan()));
        mockClaimReadyPlanFailed();

        serviceContract.processWaitingRestorePlans();

        verifyReadyPlanClaimed();
        verify(restorePlanItemMapper, never()).selectList(anyItemWrapper());
        verify(transactionTemplate, never()).execute(any());
        assertNoWriteCalls(directoryAccessRuleMapper);
        assertNoWriteCalls(restoreLogMapper);
        verify(restorePlanMapper, never()).updateById(any(DccNasAclRestorePlanDO.class));
    }

    @Test
    void processWaitingRestorePlans_recoversStaleExecutingPlanAfterClaimBeforeItemProcessing() {
        LocalDateTime staleStartedAt = LocalDateTime.now().minusHours(2);
        DccDirectoryAccessRuleDO currentRule = runtimeRule(7601L, "USER", 201L,
                true, false, false, true, "existing DCC rule");
        DccDirectoryAccessRuleDO targetRule = runtimeRule(null, "USER", 901L,
                true, true, true, true, "restore from NAS ACL snapshot");
        DccNasAclRestorePlanDO executingPlan = executingRestorePlan(staleStartedAt,
                "{\"planHash\":\"sha256:preview\",\"directoryCount\":1,"
                        + "\"ruleCount\":1,\"completedDirectoryCount\":0,\"failedDirectoryCount\":0}");
        DccNasAclRestorePlanItemDO waitingItem = waitingRestoreItem(directoryRulesHash(List.of(currentRule)),
                directoryRulesHash(List.of(targetRule)), List.of(targetRule));

        when(restorePlanMapper.selectList(anyPlanWrapper())).thenReturn(List.of(executingPlan));
        mockReclaimExecutingPlanSucceeded(staleStartedAt);
        when(restorePlanItemMapper.selectList(anyItemWrapper())).thenReturn(List.of(waitingItem));
        when(directoryAccessRuleMapper.selectList(anyDirectoryAccessRuleWrapper()))
                .thenReturn(List.of(currentRule), List.of(targetRule));

        serviceContract.processWaitingRestorePlans();

        verifyExecutingPlanReclaimed(staleStartedAt);
        verify(restorePlanMapper, never()).claimReadyPlan(eq(RESTORE_ID), any(LocalDateTime.class));
        verify(transactionTemplate, times(1)).execute(any());

        ArgumentCaptor<DccNasAclRestorePlanDO> planCaptor =
                ArgumentCaptor.forClass(DccNasAclRestorePlanDO.class);
        verify(restorePlanMapper, atLeastOnce()).updateById(planCaptor.capture());
        DccNasAclRestorePlanDO completedPlan = lastCaptured(planCaptor.getAllValues());
        assertEquals("COMPLETED", completedPlan.getStatus());
        assertTrue(completedPlan.getValidationSummaryJson().contains("\"completedDirectoryCount\":1"));
    }

    @Test
    void processWaitingRestorePlans_usesSecondPrecisionLeaseForStaleReclaimAndFirstRefresh() {
        LocalDateTime staleStartedAt = LocalDateTime.now().minusHours(2);
        DccDirectoryAccessRuleDO currentRule = runtimeRule(7601L, "USER", 201L,
                true, false, false, true, "existing DCC rule");
        DccDirectoryAccessRuleDO targetRule = runtimeRule(null, "USER", 901L,
                true, true, true, true, "restore from NAS ACL snapshot");
        DccNasAclRestorePlanDO executingPlan = executingRestorePlan(staleStartedAt,
                "{\"planHash\":\"sha256:preview\",\"directoryCount\":1,"
                        + "\"ruleCount\":1,\"completedDirectoryCount\":0,\"failedDirectoryCount\":0}");
        DccNasAclRestorePlanItemDO waitingItem = waitingRestoreItem(directoryRulesHash(List.of(currentRule)),
                directoryRulesHash(List.of(targetRule)), List.of(targetRule));

        when(restorePlanMapper.selectList(anyPlanWrapper())).thenReturn(List.of(executingPlan));
        when(restorePlanMapper.reclaimExecutingPlan(eq(RESTORE_ID),
                any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1);
        when(restorePlanItemMapper.selectList(anyItemWrapper())).thenReturn(List.of(waitingItem));
        when(directoryAccessRuleMapper.selectList(anyDirectoryAccessRuleWrapper()))
                .thenReturn(List.of(currentRule), List.of(targetRule));

        serviceContract.processWaitingRestorePlans();

        ArgumentCaptor<LocalDateTime> currentReclaimLeaseCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> reclaimedLeaseCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(restorePlanMapper, atLeastOnce()).reclaimExecutingPlan(eq(RESTORE_ID),
                currentReclaimLeaseCaptor.capture(), reclaimedLeaseCaptor.capture());
        LocalDateTime currentReclaimLease = lastCaptured(currentReclaimLeaseCaptor.getAllValues());
        LocalDateTime reclaimedLease = lastCaptured(reclaimedLeaseCaptor.getAllValues());
        assertSecondPrecision(currentReclaimLease);
        assertSecondPrecision(reclaimedLease);

        ArgumentCaptor<LocalDateTime> refreshCurrentLeaseCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> refreshedLeaseCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(restorePlanMapper, atLeastOnce()).refreshExecutingPlanLease(eq(RESTORE_ID),
                refreshCurrentLeaseCaptor.capture(), refreshedLeaseCaptor.capture());
        LocalDateTime firstRefreshCurrentLease = refreshCurrentLeaseCaptor.getAllValues().get(0);
        LocalDateTime firstRefreshedLease = refreshedLeaseCaptor.getAllValues().get(0);
        assertEquals(reclaimedLease, firstRefreshCurrentLease);
        assertSecondPrecision(firstRefreshCurrentLease);
        assertSecondPrecision(firstRefreshedLease);
    }

    @Test
    void processWaitingRestorePlans_resumesStaleExecutingPlanWithVerifiedAndAppliedItems() {
        LocalDateTime staleStartedAt = LocalDateTime.now().minusHours(2);
        Long verifiedDirectoryId = 902633L;
        DccDirectoryAccessRuleDO targetRule = runtimeRule(null, DIRECTORY_ID, "USER", 901L,
                true, true, true, true, "restore from NAS ACL snapshot");
        String expectedAfterHash = directoryRulesHash(List.of(targetRule));
        DccNasAclRestorePlanDO executingPlan = executingRestorePlan(staleStartedAt,
                "{\"planHash\":\"sha256:preview\",\"directoryCount\":2,"
                        + "\"ruleCount\":2,\"completedDirectoryCount\":1,\"failedDirectoryCount\":0}");
        DccNasAclRestorePlanItemDO verifiedItem = verifiedRestoreItem(8100L, verifiedDirectoryId);
        DccNasAclRestorePlanItemDO appliedItem = appliedRestoreItem(8101L, DIRECTORY_ID, expectedAfterHash,
                List.of(targetRule));

        when(restorePlanMapper.selectList(anyPlanWrapper())).thenReturn(List.of(executingPlan));
        mockReclaimExecutingPlanSucceeded(staleStartedAt);
        when(restorePlanItemMapper.selectList(anyItemWrapper()))
                .thenReturn(List.of(verifiedItem, appliedItem), List.of(verifiedItem, appliedItem));
        when(directoryAccessRuleMapper.selectList(anyDirectoryAccessRuleWrapper())).thenReturn(List.of(targetRule));

        serviceContract.processWaitingRestorePlans();

        verifyExecutingPlanReclaimed(staleStartedAt);
        verify(transactionTemplate, times(1)).execute(any());
        assertNoWriteCalls(directoryAccessRuleMapper);

        ArgumentCaptor<DccNasAclRestorePlanItemDO> itemCaptor =
                ArgumentCaptor.forClass(DccNasAclRestorePlanItemDO.class);
        verify(restorePlanItemMapper, atLeastOnce()).updateById(itemCaptor.capture());
        DccNasAclRestorePlanItemDO verifiedAppliedItem = lastCaptured(itemCaptor.getAllValues());
        assertEquals(appliedItem.getId(), verifiedAppliedItem.getId());
        assertEquals("VERIFIED", verifiedAppliedItem.getStatus());
        assertEquals(expectedAfterHash, verifiedAppliedItem.getActualAfterHash());

        ArgumentCaptor<DccNasAclRestorePlanDO> planCaptor =
                ArgumentCaptor.forClass(DccNasAclRestorePlanDO.class);
        verify(restorePlanMapper, atLeastOnce()).updateById(planCaptor.capture());
        DccNasAclRestorePlanDO completedPlan = lastCaptured(planCaptor.getAllValues());
        assertEquals("COMPLETED", completedPlan.getStatus());
        assertTrue(completedPlan.getValidationSummaryJson().contains("\"completedDirectoryCount\":2"));
        assertTrue(completedPlan.getValidationSummaryJson().contains("\"failedDirectoryCount\":0"));
    }

    @Test
    void processWaitingRestorePlans_failsClaimedPlanWhenPlanItemsAreMissing() {
        when(restorePlanMapper.selectList(anyPlanWrapper())).thenReturn(List.of(readyRestorePlan()));
        mockClaimReadyPlanSucceeded();
        when(restorePlanItemMapper.selectList(anyItemWrapper())).thenReturn(List.of());

        serviceContract.processWaitingRestorePlans();

        verifyReadyPlanClaimed();
        verify(transactionTemplate, never()).execute(any());
        assertNoWriteCalls(directoryAccessRuleMapper);
        assertNoWriteCalls(restoreLogMapper);

        ArgumentCaptor<DccNasAclRestorePlanDO> planCaptor =
                ArgumentCaptor.forClass(DccNasAclRestorePlanDO.class);
        verify(restorePlanMapper, atLeastOnce()).updateById(planCaptor.capture());
        DccNasAclRestorePlanDO failedPlan = lastCaptured(planCaptor.getAllValues());
        assertEquals("FAILED", failedPlan.getStatus());
        assertEquals("DCC_NAS_ACL_RESTORE_PLAN_ITEM_PREREQUISITE_INVALID", failedPlan.getFailureCode());
        assertTrue(failedPlan.getFailureMessage().contains("restore plan items required"));
    }

    @Test
    void processWaitingRestorePlans_failsWhenDirectoryCountDoesNotMatchItemCount() {
        DccNasAclRestorePlanDO inconsistentPlan = readyRestorePlan("{\"planHash\":\"sha256:preview\","
                + "\"directoryCount\":2,\"ruleCount\":1,\"completedDirectoryCount\":1,"
                + "\"failedDirectoryCount\":0}");
        DccNasAclRestorePlanItemDO verifiedItem = verifiedRestoreItem(8100L, DIRECTORY_ID);

        when(restorePlanMapper.selectList(anyPlanWrapper())).thenReturn(List.of(inconsistentPlan));
        mockClaimReadyPlanSucceeded();
        when(restorePlanItemMapper.selectList(anyItemWrapper())).thenReturn(List.of(verifiedItem));

        serviceContract.processWaitingRestorePlans();

        verify(transactionTemplate, never()).execute(any());

        ArgumentCaptor<DccNasAclRestorePlanDO> planCaptor =
                ArgumentCaptor.forClass(DccNasAclRestorePlanDO.class);
        verify(restorePlanMapper, atLeastOnce()).updateById(planCaptor.capture());
        DccNasAclRestorePlanDO failedPlan = lastCaptured(planCaptor.getAllValues());
        assertEquals("FAILED", failedPlan.getStatus());
        assertEquals("DCC_NAS_ACL_RESTORE_PLAN_ITEM_PREREQUISITE_INVALID", failedPlan.getFailureCode());
        assertTrue(failedPlan.getFailureMessage().contains("directoryCount"));
    }

    @Test
    void processWaitingRestorePlans_failsIncompletePlanWithoutEligibleItems() {
        DccNasAclRestorePlanItemDO unknownStatusItem = restoreItemWithOperations(8101L, DIRECTORY_ID,
                "APPLYING", "sha256:after", validOperationsJson(DIRECTORY_ID));

        when(restorePlanMapper.selectList(anyPlanWrapper())).thenReturn(List.of(readyRestorePlan()));
        mockClaimReadyPlanSucceeded();
        when(restorePlanItemMapper.selectList(anyItemWrapper())).thenReturn(List.of(unknownStatusItem));

        serviceContract.processWaitingRestorePlans();

        verify(transactionTemplate, never()).execute(any());
        assertNoWriteCalls(directoryAccessRuleMapper);

        ArgumentCaptor<DccNasAclRestorePlanItemDO> itemCaptor =
                ArgumentCaptor.forClass(DccNasAclRestorePlanItemDO.class);
        verify(restorePlanItemMapper, atLeastOnce()).updateById(itemCaptor.capture());
        DccNasAclRestorePlanItemDO failedItem = lastCaptured(itemCaptor.getAllValues());
        assertEquals(unknownStatusItem.getId(), failedItem.getId());
        assertEquals("FAILED", failedItem.getStatus());
        assertTrue(failedItem.getBlockReason().contains("unsupported restore item status"));

        ArgumentCaptor<DccNasAclRestorePlanDO> planCaptor =
                ArgumentCaptor.forClass(DccNasAclRestorePlanDO.class);
        verify(restorePlanMapper, atLeastOnce()).updateById(planCaptor.capture());
        DccNasAclRestorePlanDO failedPlan = lastCaptured(planCaptor.getAllValues());
        assertEquals("FAILED", failedPlan.getStatus());
        assertEquals("DCC_NAS_ACL_RESTORE_PLAN_ITEM_PREREQUISITE_INVALID", failedPlan.getFailureCode());
        assertTrue(failedPlan.getValidationSummaryJson().contains("\"completedDirectoryCount\":0"));
        assertTrue(failedPlan.getValidationSummaryJson().contains("\"failedDirectoryCount\":1"));
    }

    @Test
    void processWaitingRestorePlans_convertsMissingPlannedOperationsJsonToAuditedFailure() {
        DccNasAclRestorePlanItemDO waitingItem = restoreItemWithOperations(8101L, DIRECTORY_ID,
                "WAITING", "sha256:after", null);

        assertWaitingItemValidationFailure(waitingItem, "plannedOperationsJson");
    }

    @Test
    void processWaitingRestorePlans_convertsUnsupportedRestoreModeToAuditedFailure() {
        String operationsJson = "{\"restoreMode\":\"APPEND_DIRECTORY_RULES\",\"directoryId\":" + DIRECTORY_ID
                + ",\"expectedCurrentRuleHash\":\"sha256:current\",\"expectedAfterHash\":\"sha256:after\","
                + "\"replaceDirectoryRules\":[]}";
        DccNasAclRestorePlanItemDO waitingItem = restoreItemWithOperations(8101L, DIRECTORY_ID,
                "WAITING", "sha256:after", operationsJson);

        assertWaitingItemValidationFailure(waitingItem, "unsupported restoreMode");
    }

    @Test
    void processWaitingRestorePlans_convertsInvalidReplaceDirectoryRulesToAuditedFailure() {
        String operationsJson = "{\"restoreMode\":\"REPLACE_DIRECTORY_RULES\",\"directoryId\":" + DIRECTORY_ID
                + ",\"expectedCurrentRuleHash\":\"sha256:current\",\"expectedAfterHash\":\"sha256:after\","
                + "\"replaceDirectoryRules\":[{\"directoryId\":" + (DIRECTORY_ID + 1)
                + ",\"subjectType\":\"USER\",\"subjectId\":901,\"canQuery\":true,"
                + "\"canPreview\":true,\"canDownload\":true,\"active\":true}]}";
        DccNasAclRestorePlanItemDO waitingItem = restoreItemWithOperations(8101L, DIRECTORY_ID,
                "WAITING", "sha256:after", operationsJson);

        assertWaitingItemValidationFailure(waitingItem, "directoryId mismatch");
    }

    @Test
    void processWaitingRestorePlans_convertsTransactionFailureToAuditedFailure() {
        DccDirectoryAccessRuleDO targetRule = runtimeRule(null, "USER", 901L,
                true, true, true, true, "restore from NAS ACL snapshot");
        DccNasAclRestorePlanItemDO waitingItem = waitingRestoreItem("sha256:current",
                directoryRulesHash(List.of(targetRule)), List.of(targetRule));

        when(restorePlanMapper.selectList(anyPlanWrapper())).thenReturn(List.of(readyRestorePlan()));
        mockClaimReadyPlanSucceeded();
        when(restorePlanItemMapper.selectList(anyItemWrapper())).thenReturn(List.of(waitingItem));
        doThrow(new IllegalStateException("transaction unavailable")).when(transactionTemplate).execute(any());

        serviceContract.processWaitingRestorePlans();

        assertNoWriteCalls(directoryAccessRuleMapper);

        ArgumentCaptor<DccNasAclRestorePlanItemDO> itemCaptor =
                ArgumentCaptor.forClass(DccNasAclRestorePlanItemDO.class);
        verify(restorePlanItemMapper, atLeastOnce()).updateById(itemCaptor.capture());
        DccNasAclRestorePlanItemDO failedItem = lastCaptured(itemCaptor.getAllValues());
        assertEquals(waitingItem.getId(), failedItem.getId());
        assertEquals("FAILED", failedItem.getStatus());
        assertTrue(failedItem.getBlockReason().contains("transaction unavailable"));

        ArgumentCaptor<DccNasAclRestoreLogDO> logCaptor =
                ArgumentCaptor.forClass(DccNasAclRestoreLogDO.class);
        verify(restoreLogMapper, times(1)).insert(logCaptor.capture());
        assertEquals("VALIDATE", logCaptor.getValue().getActionType());
        assertEquals("FAILED", logCaptor.getValue().getStatus());

        ArgumentCaptor<DccNasAclRestorePlanDO> planCaptor =
                ArgumentCaptor.forClass(DccNasAclRestorePlanDO.class);
        verify(restorePlanMapper, atLeastOnce()).updateById(planCaptor.capture());
        DccNasAclRestorePlanDO failedPlan = lastCaptured(planCaptor.getAllValues());
        assertEquals("FAILED", failedPlan.getStatus());
        assertEquals("DCC_NAS_ACL_RESTORE_ITEM_PROCESSING_FAILED", failedPlan.getFailureCode());
    }

    private static DccNasAclRestorePlanDO readyRestorePlan() {
        return readyRestorePlan("{\"planHash\":\"sha256:preview\",\"directoryCount\":1,"
                + "\"ruleCount\":1,\"completedDirectoryCount\":0,\"failedDirectoryCount\":0}");
    }

    private static DccNasAclRestorePlanDO readyRestorePlan(String validationSummaryJson) {
        return DccNasAclRestorePlanDO.builder()
                .id(RESTORE_ID)
                .snapshotId(SNAPSHOT_ID)
                .transferTaskId(TASK_ID)
                .planKey("restore-plan-key")
                .targetModel("DCC_PERMISSION_RULES")
                .status("READY")
                .semanticPolicyVersion("DCC_NAS_ACL_TO_DIRECTORY_RULES_V1")
                .identityMappingVersion("DCC_NAS_IDENTITY_MAPPING_V1")
                .validationSummaryJson(validationSummaryJson)
                .createdByUserId(OPERATOR_USER_ID)
                .build();
    }

    private static DccNasAclRestorePlanDO executingRestorePlan(LocalDateTime startedAt,
                                                               String validationSummaryJson) {
        DccNasAclRestorePlanDO plan = readyRestorePlan(validationSummaryJson);
        plan.setStatus("EXECUTING");
        plan.setStartedAt(startedAt);
        return plan;
    }

    private static DccNasAclRestorePlanItemDO verifiedRestoreItem(Long itemId, Long directoryId) {
        return DccNasAclRestorePlanItemDO.builder()
                .id(itemId)
                .planId(RESTORE_ID)
                .directorySnapshotId(7300L)
                .transferTaskItemId(1000L)
                .dccDirectoryId(directoryId)
                .dccCategoryId(900250L)
                .sourceDescriptorId(DESCRIPTOR_ID)
                .plannedOperationsHash("sha256:verified-item")
                .plannedOperationsJson("{\"restoreMode\":\"REPLACE_DIRECTORY_RULES\",\"directoryId\":"
                        + directoryId + "}")
                .status("VERIFIED")
                .expectedAfterHash("sha256:verified-after")
                .actualAfterHash("sha256:verified-after")
                .verifiedAt(LocalDateTime.of(2026, 5, 27, 5, 0))
                .build();
    }

    private static DccNasAclRestorePlanItemDO appliedRestoreItem(Long itemId,
                                                                 Long directoryId,
                                                                 String expectedAfterHash,
                                                                 List<DccDirectoryAccessRuleDO> targetRules) {
        DccNasAclRestorePlanItemDO item = waitingRestoreItem(itemId, DIRECTORY_SNAPSHOT_ID,
                TRANSFER_TASK_ITEM_ID, directoryId, "sha256:already-applied-before",
                expectedAfterHash, targetRules);
        item.setStatus("APPLIED");
        item.setActualAfterHash(expectedAfterHash);
        return item;
    }

    private static DccNasAclRestorePlanItemDO waitingRestoreItem(String expectedCurrentRuleHash,
                                                                 String expectedAfterHash,
                                                                 List<DccDirectoryAccessRuleDO> targetRules) {
        return waitingRestoreItem(8101L, DIRECTORY_SNAPSHOT_ID, TRANSFER_TASK_ITEM_ID, DIRECTORY_ID,
                expectedCurrentRuleHash, expectedAfterHash, targetRules);
    }

    private static DccNasAclRestorePlanItemDO waitingRestoreItem(Long itemId,
                                                                 Long directorySnapshotId,
                                                                 Long transferTaskItemId,
                                                                 Long directoryId,
                                                                 String expectedCurrentRuleHash,
                                                                 String expectedAfterHash,
                                                                 List<DccDirectoryAccessRuleDO> targetRules) {
        String plannedOperationsJson = plannedOperationsJson(directoryId,
                expectedCurrentRuleHash, expectedAfterHash, targetRules);
        return DccNasAclRestorePlanItemDO.builder()
                .id(itemId)
                .planId(RESTORE_ID)
                .directorySnapshotId(directorySnapshotId)
                .transferTaskItemId(transferTaskItemId)
                .dccDirectoryId(directoryId)
                .dccCategoryId(900250L)
                .sourceDescriptorId(DESCRIPTOR_ID)
                .plannedOperationsHash(sha256(plannedOperationsJson))
                .plannedOperationsJson(plannedOperationsJson)
                .status("WAITING")
                .expectedAfterHash(expectedAfterHash)
                .build();
    }

    private static DccNasAclRestorePlanItemDO restoreItemWithOperations(Long itemId,
                                                                        Long directoryId,
                                                                        String status,
                                                                        String expectedAfterHash,
                                                                        String plannedOperationsJson) {
        return DccNasAclRestorePlanItemDO.builder()
                .id(itemId)
                .planId(RESTORE_ID)
                .directorySnapshotId(DIRECTORY_SNAPSHOT_ID)
                .transferTaskItemId(TRANSFER_TASK_ITEM_ID)
                .dccDirectoryId(directoryId)
                .dccCategoryId(900250L)
                .sourceDescriptorId(DESCRIPTOR_ID)
                .plannedOperationsHash(plannedOperationsJson == null ? null : sha256(plannedOperationsJson))
                .plannedOperationsJson(plannedOperationsJson)
                .status(status)
                .expectedAfterHash(expectedAfterHash)
                .build();
    }

    private static String validOperationsJson(Long directoryId) {
        DccDirectoryAccessRuleDO targetRule = runtimeRule(null, directoryId, "USER", 901L,
                true, true, true, true, "restore from NAS ACL snapshot");
        return plannedOperationsJson(directoryId, "sha256:current", directoryRulesHash(List.of(targetRule)),
                List.of(targetRule));
    }

    private static String plannedOperationsJson(Long directoryId,
                                                String expectedCurrentRuleHash,
                                                String expectedAfterHash,
                                                List<DccDirectoryAccessRuleDO> targetRules) {
        Map<String, Object> operations = new LinkedHashMap<>();
        operations.put("restoreMode", "REPLACE_DIRECTORY_RULES");
        operations.put("directoryId", directoryId);
        operations.put("expectedCurrentRuleHash", expectedCurrentRuleHash);
        operations.put("expectedAfterHash", expectedAfterHash);
        operations.put("replaceDirectoryRules", targetRules.stream()
                .sorted(ruleComparator())
                .map(DccNasPermissionRestoreExecutionServiceTest::rulePayload)
                .toList());
        return JsonUtils.toJsonString(operations);
    }

    private static DccDirectoryAccessRuleDO runtimeRule(Long id,
                                                        String subjectType,
                                                        Long subjectId,
                                                        Boolean canQuery,
                                                        Boolean canPreview,
                                                        Boolean canDownload,
                                                        Boolean active,
                                                        String changeReason) {
        return runtimeRule(id, DIRECTORY_ID, subjectType, subjectId, canQuery, canPreview, canDownload,
                active, changeReason);
    }

    private static DccDirectoryAccessRuleDO runtimeRule(Long id,
                                                        Long directoryId,
                                                        String subjectType,
                                                        Long subjectId,
                                                        Boolean canQuery,
                                                        Boolean canPreview,
                                                        Boolean canDownload,
                                                        Boolean active,
                                                        String changeReason) {
        return DccDirectoryAccessRuleDO.builder()
                .id(id)
                .directoryId(directoryId)
                .subjectType(subjectType)
                .subjectId(subjectId)
                .canQuery(canQuery)
                .canPreview(canPreview)
                .canDownload(canDownload)
                .active(active)
                .changeReason(changeReason)
                .build();
    }

    private static String directoryRulesHash(List<DccDirectoryAccessRuleDO> rules) {
        List<Map<String, Object>> canonicalRules = rules.stream()
                .sorted(ruleComparator())
                .map(DccNasPermissionRestoreExecutionServiceTest::rulePayload)
                .toList();
        return "sha256:" + sha256(JsonUtils.toJsonString(canonicalRules));
    }

    private static Map<String, Object> rulePayload(DccDirectoryAccessRuleDO rule) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("directoryId", rule.getDirectoryId());
        payload.put("subjectType", rule.getSubjectType());
        payload.put("subjectId", rule.getSubjectId());
        payload.put("canQuery", rule.getCanQuery());
        payload.put("canPreview", rule.getCanPreview());
        payload.put("canDownload", rule.getCanDownload());
        payload.put("active", rule.getActive());
        payload.put("changeReason", rule.getChangeReason());
        return payload;
    }

    private void assertWaitingItemValidationFailure(DccNasAclRestorePlanItemDO waitingItem,
                                                    String expectedMessageFragment) {
        when(restorePlanMapper.selectList(anyPlanWrapper())).thenReturn(List.of(readyRestorePlan()));
        mockClaimReadyPlanSucceeded();
        when(restorePlanItemMapper.selectList(anyItemWrapper())).thenReturn(List.of(waitingItem));

        serviceContract.processWaitingRestorePlans();

        verify(transactionTemplate, times(1)).execute(any());
        assertNoWriteCalls(directoryAccessRuleMapper);

        ArgumentCaptor<DccNasAclRestorePlanItemDO> itemCaptor =
                ArgumentCaptor.forClass(DccNasAclRestorePlanItemDO.class);
        verify(restorePlanItemMapper, atLeastOnce()).updateById(itemCaptor.capture());
        DccNasAclRestorePlanItemDO failedItem = lastCaptured(itemCaptor.getAllValues());
        assertEquals(waitingItem.getId(), failedItem.getId());
        assertEquals("FAILED", failedItem.getStatus());
        assertTrue(failedItem.getBlockReason().contains("DCC_NAS_ACL_RESTORE_ITEM_PROCESSING_FAILED"));
        assertTrue(failedItem.getBlockReason().contains(expectedMessageFragment));

        ArgumentCaptor<DccNasAclRestoreLogDO> logCaptor =
                ArgumentCaptor.forClass(DccNasAclRestoreLogDO.class);
        verify(restoreLogMapper, times(1)).insert(logCaptor.capture());
        DccNasAclRestoreLogDO log = logCaptor.getValue();
        assertEquals("VALIDATE", log.getActionType());
        assertEquals("FAILED", log.getStatus());
        assertEquals("DCC_NAS_ACL_RESTORE_ITEM_PROCESSING_FAILED", log.getErrorCode());
        assertTrue(log.getErrorMessage().contains(expectedMessageFragment));

        ArgumentCaptor<DccNasAclRestorePlanDO> planCaptor =
                ArgumentCaptor.forClass(DccNasAclRestorePlanDO.class);
        verify(restorePlanMapper, atLeastOnce()).updateById(planCaptor.capture());
        DccNasAclRestorePlanDO failedPlan = lastCaptured(planCaptor.getAllValues());
        assertEquals("FAILED", failedPlan.getStatus());
        assertEquals("DCC_NAS_ACL_RESTORE_ITEM_PROCESSING_FAILED", failedPlan.getFailureCode());
        assertTrue(failedPlan.getFailureMessage().contains(expectedMessageFragment));
    }

    private static Comparator<DccDirectoryAccessRuleDO> ruleComparator() {
        return Comparator.comparing(DccDirectoryAccessRuleDO::getDirectoryId, Comparator.nullsLast(Long::compareTo))
                .thenComparing(DccDirectoryAccessRuleDO::getSubjectType, Comparator.nullsLast(String::compareTo))
                .thenComparing(DccDirectoryAccessRuleDO::getSubjectId, Comparator.nullsLast(Long::compareTo))
                .thenComparing(DccDirectoryAccessRuleDO::getCanQuery, Comparator.nullsLast(Boolean::compareTo))
                .thenComparing(DccDirectoryAccessRuleDO::getCanPreview, Comparator.nullsLast(Boolean::compareTo))
                .thenComparing(DccDirectoryAccessRuleDO::getCanDownload, Comparator.nullsLast(Boolean::compareTo))
                .thenComparing(DccDirectoryAccessRuleDO::getActive, Comparator.nullsLast(Boolean::compareTo))
                .thenComparing(DccDirectoryAccessRuleDO::getChangeReason, Comparator.nullsLast(String::compareTo));
    }

    private static void assertLog(DccNasAclRestoreLogDO log,
                                  String actionType,
                                  String status,
                                  String beforeHash,
                                  String expectedAfterHash,
                                  String errorCode) {
        assertEquals(actionType, log.getActionType());
        assertEquals(status, log.getStatus());
        assertEquals(beforeHash, log.getBeforeHash());
        assertEquals(expectedAfterHash, log.getExpectedAfterHash());
        assertEquals(errorCode, log.getErrorCode());
        assertNotNull(log.getStartedAt());
        assertNotNull(log.getCompletedAt());
    }

    private static <T> T lastCaptured(List<T> values) {
        assertFalse(values.isEmpty());
        return values.get(values.size() - 1);
    }

    private static void assertSecondPrecision(LocalDateTime value) {
        assertNotNull(value);
        assertEquals(0, value.getNano());
    }

    private static LocalDateTime toSecondPrecision(LocalDateTime value) {
        return value.withNano(0);
    }

    private static boolean containsText(String value) {
        return value != null && !value.isBlank();
    }

    private static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private static Wrapper<DccNasAclRestorePlanDO> anyPlanWrapper() {
        return any();
    }

    private static Wrapper<DccNasAclRestorePlanItemDO> anyItemWrapper() {
        return any();
    }

    private static Wrapper<DccDirectoryAccessRuleDO> anyDirectoryAccessRuleWrapper() {
        return any();
    }

    private void mockClaimReadyPlanSucceeded() {
        when(restorePlanMapper.claimReadyPlan(eq(RESTORE_ID), any(LocalDateTime.class))).thenReturn(1);
    }

    private void mockClaimReadyPlanFailed() {
        when(restorePlanMapper.claimReadyPlan(eq(RESTORE_ID), any(LocalDateTime.class))).thenReturn(0);
    }

    private void mockReclaimExecutingPlanSucceeded(LocalDateTime currentStartedAt) {
        when(restorePlanMapper.reclaimExecutingPlan(eq(RESTORE_ID), eq(toSecondPrecision(currentStartedAt)),
                any(LocalDateTime.class)))
                .thenReturn(1);
    }

    private void verifyReadyPlanClaimed() {
        verify(restorePlanMapper, atLeastOnce()).claimReadyPlan(eq(RESTORE_ID), any(LocalDateTime.class));
    }

    private void verifyExecutingPlanReclaimed(LocalDateTime currentStartedAt) {
        verify(restorePlanMapper, atLeastOnce())
                .reclaimExecutingPlan(eq(RESTORE_ID), eq(toSecondPrecision(currentStartedAt)),
                        any(LocalDateTime.class));
    }

    private static void assertNoWriteCalls(Object mock) {
        List<String> writeCalls = mockingDetails(mock).getInvocations().stream()
                .map(invocation -> invocation.getMethod().getName())
                .filter(DccNasPermissionRestoreExecutionServiceTest::isWriteMethod)
                .toList();
        String mockName = mockingDetails(mock).getMockCreationSettings().getTypeToMock().getSimpleName();
        assertTrue(writeCalls.isEmpty(), "Expected no write calls on " + mockName + " but got " + writeCalls);
    }

    private static void assertDirectoryRulesDeletedBeforeInserted(Object mock) {
        List<String> writeCalls = mockingDetails(mock).getInvocations().stream()
                .map(invocation -> invocation.getMethod().getName())
                .filter(DccNasPermissionRestoreExecutionServiceTest::isWriteMethod)
                .toList();
        int firstDeleteIndex = firstWriteMethodIndex(writeCalls, "delete");
        int firstInsertIndex = firstWriteMethodIndex(writeCalls, "insert");
        assertTrue(firstDeleteIndex >= 0, "Expected old directory rules to be deleted/replaced but got " + writeCalls);
        assertTrue(firstInsertIndex >= 0, "Expected target directory rules to be inserted but got " + writeCalls);
        assertTrue(firstDeleteIndex < firstInsertIndex,
                "Expected old directory rules to be deleted before target rules are inserted but got " + writeCalls);
    }

    private static int firstWriteMethodIndex(List<String> writeCalls, String methodPrefix) {
        for (int index = 0; index < writeCalls.size(); index++) {
            if (writeCalls.get(index).startsWith(methodPrefix)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean isWriteMethod(String methodName) {
        return methodName.startsWith("insert")
                || methodName.startsWith("update")
                || methodName.startsWith("delete");
    }

}
