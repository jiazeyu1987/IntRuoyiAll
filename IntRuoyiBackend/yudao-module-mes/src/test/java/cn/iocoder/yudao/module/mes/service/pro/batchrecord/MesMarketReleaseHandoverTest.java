package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesMarketReleaseHandoverMapper.Row;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification.MesReleaseTaskNotificationService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.*;
import cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi;
import cn.iocoder.yudao.module.system.api.permission.dto.ApprovalAuthorityRevisionDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness=Strictness.LENIENT)
class MesMarketReleaseHandoverTest {
    @Mock MesMarketReleaseHandoverMapper handovers;
    @Mock MesProEdhrBatchExecutionMapper batches;
    @Mock MesProEdhrBatchExecutionOriginMapper origins;
    @Mock MesProEdhrReleaseTransactionMapper transactions;
    @Mock MesProEdhrWorkTaskMapper tasks;
    @Mock MesProEdhrWorkTaskService taskService;
    @Mock MesProductionReleaseRequiredCandidateResolver candidates;
    @Mock AdminUserApi users;
    @Mock ApprovalAuthorityApi authority;
    @Mock GxpAuditService ledger;
    @Mock MesProEdhrOperationAuditService operationAudit;
    @Mock MesReleaseTaskNotificationService notifications;
    @Mock MesProEdhrBatchExecutionVisibilityService visibility;
    MesMarketReleaseAccess access;
    MesMarketReleaseHandoverService service;
    MockedStatic<SecurityFrameworkUtils> security;
    List<Row> history=new ArrayList<>();
    long actor=100;
    MesProEdhrBatchExecutionDO batch;
    MesProEdhrReleaseTransactionDO transaction;
    MesProEdhrWorkTaskDO task;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        security=mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUserId).thenAnswer(call -> actor);
        security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("原办理人");
        access=new MesMarketReleaseAccess(handovers,mock(MesProBatchRecordExecutionMapper.class),ledger);
        service=new MesMarketReleaseHandoverService(handovers,access,visibility,batches,origins,transactions,tasks,taskService,candidates,users,authority,ledger,operationAudit,notifications);
        batch=new MesProEdhrBatchExecutionDO().setId(50L).setTenantId(1L).setWorkOrderId(55L).setRouteId(5L).setStatus(10);
        transaction=new MesProEdhrReleaseTransactionDO().setTenantId(1L).setId(60L).setBatchExecutionId(50L).setReleaseStatus("PENDING_APPROVAL").setVersion(1);
        task=new MesProEdhrWorkTaskDO().setId(70L).setBatchExecutionId(50L).setBusinessScopeId(60L)
                .setBusinessScopeType("RELEASE_TRANSACTION").setTaskType("RELEASE_APPROVE").setStatus("TODO")
                .setAssigneeUserId(100L).setCandidateUserSnapshot("100,200,300");
        when(batches.selectById(50L)).thenReturn(batch);
        when(batches.selectByTenantIdAndIdForUpdate(1L,50L)).thenReturn(batch);
        when(transactions.selectCurrentByBatchExecutionId(50L)).thenReturn(transaction);
        when(transactions.selectByIdForUpdate(60L)).thenReturn(transaction);
        when(transactions.update(any(MesProEdhrReleaseTransactionDO.class),any(Wrapper.class))).thenAnswer(call -> { transaction.setVersion(((MesProEdhrReleaseTransactionDO)call.getArgument(0)).getVersion()); return 1; });
        when(tasks.selectList(any(Wrapper.class))).thenAnswer(call -> List.of(task));
        when(tasks.selectByIdAndTenantForUpdate(70L,1L)).thenAnswer(call -> task);
        when(tasks.selectById(70L)).thenAnswer(call -> task);
        when(tasks.updateById(any(MesProEdhrWorkTaskDO.class))).thenAnswer(call -> {
            MesProEdhrWorkTaskDO update=call.getArgument(0);
            task.setAssigneeUserId(update.getAssigneeUserId()).setCandidateUserSnapshot(update.getCandidateUserSnapshot()); return 1;
        });
        when(handovers.history(50L)).thenAnswer(call -> List.copyOf(history));
        when(handovers.command(anyString())).thenAnswer(call -> history.stream().filter(row -> row.getCommandKey().equals(call.getArgument(0))).findFirst().orElse(null));
        when(handovers.insert(any(Row.class))).thenAnswer(call -> {Row row=call.getArgument(0);row.setId((long)history.size()+1);history.add(0,row);return 1;});
        when(handovers.updateById(any(Row.class))).thenReturn(1);
        when(authority.lockTenantApprovalAuthority(1L)).thenReturn(new ApprovalAuthorityRevisionDTO(1L,1L));
        when(candidates.resolveRequiredCandidates(1L,MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE))
                .thenReturn(new MesProductionReleaseRoleCandidates(8L,MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE,List.of(100L,200L,300L),"formal"));
        var people=List.of(user(100),user(200),user(300));
        when(users.getUserList(any())).thenReturn(people);
        when(users.getUser(anyLong())).thenAnswer(call -> people.stream().filter(u -> u.getId().equals(call.getArgument(0))).findFirst().orElse(null));
        var origin=new MesProEdhrBatchExecutionOriginDO().setBatchExecutionId(50L).setActiveOrderId(500L).setWorkOrderId(55L);
        origin.setTenantId(1L);
        when(origins.selectListByBatchExecutionId(50L)).thenReturn(List.of(origin));
    }
    @AfterEach void clear() { security.close();TenantContextHolder.clear(); }
    private AdminUserRespDTO user(long id) {var user=new AdminUserRespDTO();user.setId(id);user.setNickname("人员"+id);user.setStatus(0);return user;}

    @Test void handoverAndRecallAreExclusiveAndAudited() {
        service.handover(50L,200L,"交接测试","NONE","handover-key-1");
        assertFalse(access.get(50L,100L).writable());assertTrue(access.get(50L,100L).visible());
        assertTrue(access.get(50L,200L).writable());assertEquals("200",task.getCandidateUserSnapshot());
        assertTrue(service.context(50L).canRecall());
        actor=200;assertFalse(service.context(50L).canHandover());
        assertThrows(ServiceException.class,()->service.handover(50L,300L,"转交","1:ACTIVE","handover-key-2"));
        actor=100;
        service.recall(50L,"回收测试","1:ACTIVE","recall-key-1");
        assertTrue(access.get(50L,100L).writable());assertFalse(access.get(50L,200L).visible());
        assertEquals("100",task.getCandidateUserSnapshot());
        assertEquals(3,transaction.getVersion(),"handover and recall must invalidate earlier signature versions");
        actor=200;assertThrows(ServiceException.class,()->access.requireReadable(50L));
        assertThrows(ServiceException.class,()->access.requireWritable(50L));
        verify(operationAudit).recordInCallerTransaction(argThat(c -> c.getOperationType().equals("MARKET_RELEASE_HANDOVER") && c.getMetadataJson().contains("500")));
        verify(operationAudit).recordInCallerTransaction(argThat(c -> c.getOperationType().equals("MARKET_RELEASE_RECALL")));
        verify(notifications).scheduleHandover(any(),eq(100L),eq(200L),eq(1L),eq(false),eq("交接测试"),anyString());
        verify(notifications).scheduleHandover(any(),eq(100L),eq(200L),eq(1L),eq(true),eq("回收测试"),anyString());
        verify(authority,times(2)).incrementLockedAuthorityRevision(1L,1L);
    }
    @Test void duplicateRequestsDoNotCreateExtraTasksNotificationsOrHistory() {
        service.handover(50L,200L,"交接测试","NONE","handover-key-1");
        service.handover(50L,200L,"交接测试","NONE","handover-key-1");
        assertEquals(1,history.size());verify(tasks,times(1)).updateById(any(MesProEdhrWorkTaskDO.class));
        verify(notifications,times(1)).scheduleHandover(any(),anyLong(),anyLong(),anyLong(),anyBoolean(),anyString(),anyString());
        assertThrows(ServiceException.class,()->service.handover(50L,300L,"交接测试","NONE","handover-key-1"));
    }
    @Test void finishedReleaseStaleVersionForeignTenantAndUnqualifiedRecipientAreRejected() {
        assertThrows(ServiceException.class,()->service.handover(50L,200L,"交接测试","stale","handover-key-1"));
        assertThrows(ServiceException.class,()->service.handover(50L,400L,"交接测试","NONE","handover-key-2"));
        batch.setTenantId(2L);
        assertThrows(ServiceException.class,()->service.handover(50L,200L,"交接测试","NONE","handover-key-3"));
        batch.setTenantId(1L);
        service.handover(50L,200L,"交接测试","NONE","handover-key-4");
        transaction.setReleaseStatus("RELEASED");
        assertThrows(ServiceException.class,()->service.recall(50L,"回收测试","1:ACTIVE","recall-key-1"));
        assertNull(history.get(0).getRecalledAt());
    }
    @Test void recalledRecipientsStayHiddenAcrossLaterHandoverCycles() {
        service.handover(50L,200L,"交接测试","NONE","handover-key-1");
        service.recall(50L,"回收测试","1:ACTIVE","recall-key-1");
        service.handover(50L,300L,"重新交接","1:RECALLED","handover-key-2");
        assertFalse(access.get(50L,200L).visible());assertTrue(access.get(50L,300L).writable());
        assertFalse(access.get(50L,100L).writable());assertEquals(2,history.size());
    }
    @Test void previewReadsAuthorityWithoutWriteLocksWhileSignatureStillLocks() {
        org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(true);
        org.springframework.transaction.support.TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
        try {
            access.requireWritable(50L);
            verify(handovers).history(50L);
            verify(handovers,never()).historyForUpdate(anyLong());
            verify(ledger,never()).acquireLedgerLock();
            org.springframework.transaction.support.TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
            when(handovers.historyForUpdate(50L)).thenReturn(List.of());
            access.requireWritable(50L);
            verify(handovers).historyForUpdate(50L);
            verify(ledger).acquireLedgerLock();
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
            org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }
}
