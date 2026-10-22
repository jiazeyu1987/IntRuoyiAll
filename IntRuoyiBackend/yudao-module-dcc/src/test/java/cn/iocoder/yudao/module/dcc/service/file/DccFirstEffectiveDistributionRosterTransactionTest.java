package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.datapermission.core.aop.DataPermissionContextHolder;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccDistributionDispatchAttemptMapper;
import cn.iocoder.yudao.module.system.api.notify.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApiImpl;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.DeptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.dal.mysql.notify.NotifyMessageMapper;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.dept.DeptServiceImpl;
import cn.iocoder.yudao.module.system.service.user.AdminUserServiceImpl;
import cn.iocoder.yudao.module.system.service.notify.*;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Production source + real directory Mappers, tenant interceptor, lifecycle, batch and platform message writes.
 * Inherited lifecycle cases keep their original explicit policy-unready port; only new cases connect the production bean. */
@Import({NotifyMessageSendApiImpl.class,NotifySendServiceImpl.class,NotifyMessageServiceImpl.class})
class DccFirstEffectiveDistributionRosterTransactionTest extends DccLifecycleDistributionOutboxTransactionTest {
    @DynamicPropertySource static void directorySchema(DynamicPropertyRegistry registry) {
        var root=Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while(root!=null&&!java.nio.file.Files.isDirectory(root.resolve("IntRuoyiBackend")))root=root.getParent();
        if(root==null)throw new IllegalStateException("actual repository schema root missing");
        var backend=root.resolve("IntRuoyiBackend");
        registry.add("spring.sql.init.schema-locations",()->String.join(",","classpath:/sql/create_tables.sql",backend.resolve("yudao-module-system/src/test/resources/sql/create_tables.sql").toUri().toString(),"classpath:/sql/dcc_distribution_outbox.sql"));
        registry.add("mybatis-plus.global-config.db-config.id-type",()->"AUTO");
    }
    @Resource private NotifyMessageSendApiImpl realSend;
    @Resource private NotifyMessageServiceImpl realMessages;
    @Resource private NotifyMessageMapper platformRows;
    @Resource private DccDistributionDispatchAttemptMapper attempts;
    @Resource private DeptMapper departmentRows;
    @Resource private AdminUserMapper userRows;
    @Resource private MybatisPlusInterceptor interceptors;
    @MockitoBean private NotifyTemplateService templates;
    private List<InnerInterceptor> originalInterceptors;
    private DeptServiceImpl directoryDepartments;
    private AdminUserApiImpl directoryUsers;
    private DccFirstEffectiveDistributionRosterSource source;
    private NotifyMessageSendApi send;
    private GenericApplicationContext sourceContext;

    @BeforeEach void productionFixture()throws Exception {
        try(var connection=dataSource.getConnection()){assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));}
        originalInterceptors=new ArrayList<>(interceptors.getInterceptors());
        var directoryTenant=new TenantLineInnerInterceptor(new TenantDatabaseInterceptor(new TenantProperties()));
        var configured=new ArrayList<InnerInterceptor>();configured.add(directoryTenant);configured.addAll(originalInterceptors);interceptors.setInterceptors(configured);
        for(String ddl:new ClassPathResource("sql/dcc_d_platform_message_fixture.sql").getContentAsString(StandardCharsets.UTF_8).split(";"))if(!ddl.isBlank())jdbc.execute(ddl);
        jdbc.update("DELETE FROM system_notify_message");jdbc.update("DELETE FROM system_users WHERE username LIKE 'ic32-%'");jdbc.update("DELETE FROM system_dept WHERE id IN (31,32,33)");
        jdbc.update("INSERT INTO system_dept(id,name,parent_id,status,tenant_id) VALUES(31,'ic32-direct',0,0,1),(32,'ic32-child',31,0,1),(33,'ic32-other',0,0,1)");
        directoryUser(99L,31L,1L,0);directoryUser(100L,31L,1L,0);directoryUser(101L,31L,1L,1);directoryUser(102L,32L,1L,0);directoryUser(103L,31L,2L,0);
        directoryDepartments=spy(new DeptServiceImpl());wire(directoryDepartments,"deptMapper",departmentRows);
        var users=new AdminUserServiceImpl();wire(users,"userMapper",userRows);
        directoryUsers=spy(new AdminUserApiImpl());wire(directoryUsers,"userService",users);
        source=spy(new DccFirstEffectiveDistributionRosterSource(directoryDepartments,directoryUsers));
        for(String op:List.of("dcc.distribution.batch.prepare","dcc.distribution.attempt.start","dcc.distribution.attempt.finish"))policy(op);
        var template=NotifyTemplateDO.builder().id(310L).code("dcc_distribution").type(2).nickname("DCC系统").status(0).content("{title} {version}").params(List.of("title","version")).build();
        when(templates.getNotifyTemplateByCodeFromCache("dcc_distribution")).thenReturn(template);
        when(templates.formatNotifyTemplateContent(anyString(),anyMap())).thenAnswer(c->new NotifyTemplateServiceImpl().formatNotifyTemplateContent(c.getArgument(0),c.getArgument(1)));
        send=spy(realSend);
    }
    @AfterEach void productionCleanup(){
        if(sourceContext!=null)sourceContext.close();
        if(jdbc!=null){jdbc.update("DELETE FROM system_notify_message");jdbc.update("DELETE FROM system_users WHERE username LIKE 'ic32-%'");jdbc.update("DELETE FROM system_dept WHERE id IN (31,32,33)");}
        if(originalInterceptors!=null)interceptors.setInterceptors(originalInterceptors);
    }
    private void directoryUser(Long id,Long department,Long tenant,int status){jdbc.update("INSERT INTO system_users(id,username,canonical_username,nickname,dept_id,status,tenant_id) VALUES(?,?,?,?,?,?,?)",id,"ic32-"+id,"ic32-"+id,"IC32 "+id,department,status,tenant);}
    private void connectProductionSource(){
        sourceContext=new GenericApplicationContext();sourceContext.registerBean(DeptService.class,()->directoryDepartments);sourceContext.registerBean(AdminUserApi.class,()->directoryUsers);
        sourceContext.registerBean(DccFirstEffectiveDistributionRosterSource.class,()->source);sourceContext.refresh();
        assertNotNull(DccFirstEffectiveDistributionRosterSource.class.getAnnotation(org.springframework.stereotype.Service.class));
        assertEquals(1,sourceContext.getBeansOfType(DccDistributionRosterSource.class).size());
        var attemptService=new DccDistributionDispatchAttemptService();wire(attemptService,"batchMapper",batchRows,"attemptMapper",attempts,"messageJobMapper",jobs,"gxpAuditService",ledger);
        var delivery=new DccControlledFileMessageDeliveryService();wire(delivery,"messageJobMapper",jobs,"notifyMessageSendApi",send,"notifyMessageService",realMessages,
                "controlledFileMapper",files,"distributionMapper",scopes,"distributionRecipientMapper",recipients,"transactionManager",transactionManager,"dispatchAttemptService",attemptService);
        var distribution=new DccWorkflowDistributionService();wire(distribution,"controlledFileMapper",files,"distributionMapper",scopes,"recipientMapper",recipients,"messageJobMapper",jobs,"messageDeliveryService",delivery);
        wire(batches,"rosterSources",sourceContext.getBeanProvider(DccDistributionRosterSource.class),"distributionService",distribution,"messageDeliveryService",delivery);
        // New Batch dependency is absent during RED; wire only once the typed guard has been implemented.
        if(Arrays.stream(DccDistributionBatchService.class.getDeclaredFields()).anyMatch(field->field.getName().equals("adminUserApi")))wire(batches,"adminUserApi",directoryUsers);
    }
    private DccDistributionBatchDO batch(){return batchRows.selectList().get(0);}
    private void failSecondNotificationOnce(){var failOnce=new java.util.concurrent.atomic.AtomicBoolean();doAnswer(call->{var request=call.getArgument(0,cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO.class);
        if(request.getUserId().equals(100L)&&failOnce.compareAndSet(false,true))throw new IllegalStateException("IC32 actual notification fault");return call.callRealMethod();}).when(send).sendSingleMessageIdempotentlyToAdmin(any());}
    private List<?> obligations(){return jdbc.queryForList("SELECT * FROM dcc_controlled_file_distribution_recipient ORDER BY id");}
    private List<?> messageKeys(){return jdbc.queryForList("SELECT id,recipient_user_id,business_type,business_id,distribution_batch_id FROM dcc_controlled_file_message_job ORDER BY id");}
    private void expectSameFrozenFacts(DccDistributionBatchDO original,List<?> oldObligations,List<?> oldKeys){var current=batch();assertEquals(original.getSnapshotPayloadHash(),current.getSnapshotPayloadHash());assertEquals(original.getSnapshotAt(),current.getSnapshotAt());assertEquals(original.getSnapshotPolicyVersion(),current.getSnapshotPolicyVersion());assertEquals(oldObligations,obligations());assertEquals(oldKeys,messageKeys());}

    @Test void productionBeanReadsChangedMembersOnlyAfterRealActivationCommit(){
        connectProductionSource();jdbc.update("UPDATE system_users SET status=1 WHERE id=99");directoryUser(104L,31L,1L,0);
        var before=LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        tx.executeWithoutResult(s->{controlLocked();verifyNoInteractions(source);assertTrue(platformRows.selectList().isEmpty());assertEquals("PENDING",batch().getStatus());});
        var current=batch();assertEquals("SENT",current.getStatus());assertEquals(List.of(100L,104L),recipients.selectList().stream().map(DccControlledFileDistributionRecipientDO::getUserId).sorted().toList());
        assertEquals(2,platformRows.selectList().size());assertEquals("FIRST_EFFECTIVE_DISPATCH_V1",current.getSnapshotPolicyVersion());assertFalse(current.getSnapshotAt().isBefore(before));assertTrue(current.getSnapshotAt().isAfter(now));
        assertEquals("B/1",current.getVersionNo());assertEquals("round-1",current.getApprovalProcessInstanceId());assertEquals("[\"31\"]",current.getDepartmentIdsJson());verify(source).resolve(any());
        assertEquals("ACTIVE",files.selectById(20L).getStatus());assertEquals(20L,masters.selectById(3L).getCurrentActiveControlledFileId());
    }
    @Test void failedFrozenSendRetriesOriginalIdsDespiteCurrentDepartmentMembershipChange(){
        connectProductionSource();failSecondNotificationOnce();control();var original=batch();assertEquals("FAILED",original.getStatus());assertEquals(1,platformRows.selectList().size());
        var oldObligations=obligations();var oldKeys=messageKeys();var firstPlatform=jdbc.queryForList("SELECT * FROM system_notify_message ORDER BY id");
        jdbc.update("UPDATE system_users SET dept_id=33 WHERE id=100");directoryUser(104L,31L,1L,0);
        assertEquals("SENT",batches.processAutomaticBatch(original.getId()).status());expectSameFrozenFacts(original,oldObligations,oldKeys);verify(source,times(1)).resolve(any());
        assertEquals(2,platformRows.selectList().size());assertEquals(firstPlatform.get(0),jdbc.queryForList("SELECT * FROM system_notify_message ORDER BY id").get(0));verify(directoryUsers,times(1)).getUserListByDeptIds(any());
    }
    @ParameterizedTest @ValueSource(strings={"DISABLED","MISSING","FOREIGN"})
    void invalidFrozenAccountBlocksEveryRetryBeforeAnyNotificationAndCanRecoverWithSameRoster(String fault){
        connectProductionSource();failSecondNotificationOnce();control();var original=batch();var oldObligations=obligations();var oldKeys=messageKeys();
        // Invalidate the already-successful user too: every original user must be checked before retrying user 100.
        if(fault.equals("DISABLED"))jdbc.update("UPDATE system_users SET status=1 WHERE id=99");
        else if(fault.equals("MISSING"))jdbc.update("DELETE FROM system_users WHERE id=99");else jdbc.update("UPDATE system_users SET tenant_id=2 WHERE id=99");
        var sentBefore=jdbc.queryForList("SELECT * FROM system_notify_message ORDER BY id");var attemptsBefore=attempts.selectList().size();
        var failure=assertThrows(RuntimeException.class,()->batches.processAutomaticBatch(original.getId()));assertTrue(failure.getMessage().contains("99"));
        assertEquals("FAILED",batch().getStatus());assertEquals("ROSTER_FROZEN_ACCOUNTS_INVALID",batch().getErrorCode());assertTrue(batch().getErrorMessage().contains("99"));assertEquals(sentBefore,jdbc.queryForList("SELECT * FROM system_notify_message ORDER BY id"));assertEquals(attemptsBefore,attempts.selectList().size());expectSameFrozenFacts(original,oldObligations,oldKeys);
        if(fault.equals("MISSING"))directoryUser(99L,31L,1L,0);else jdbc.update("UPDATE system_users SET status=0,tenant_id=1 WHERE id=99");
        directoryUser(104L,31L,1L,0);assertEquals("SENT",batches.processAutomaticBatch(original.getId()).status());assertEquals(2,platformRows.selectList().size());expectSameFrozenFacts(original,oldObligations,oldKeys);verify(source,times(1)).resolve(any());verify(directoryUsers,times(1)).getUserListByDeptIds(any());
    }
    @Test void allInvalidFrozenIdsAreReportedTogetherBeforeDispatch(){
        connectProductionSource();failSecondNotificationOnce();control();jdbc.update("UPDATE system_users SET status=1 WHERE id IN (99,100)");
        var failure=assertThrows(RuntimeException.class,()->batches.processAutomaticBatch(batch().getId()));assertTrue(failure.getMessage().contains("99"));assertTrue(failure.getMessage().contains("100"));assertEquals(1,platformRows.selectList().size());
    }
    @Test void sentHistoryReplayDoesNotRequerySourceOrRevalidateChangedAccount(){
        connectProductionSource();control();var original=batch();var before=jdbc.queryForList("SELECT * FROM system_notify_message ORDER BY id");
        doThrow(new AssertionError("SENT must not requery source")).when(source).resolve(any());doThrow(new AssertionError("SENT must not revalidate history")).when(directoryUsers).getUserList(any());
        jdbc.update("UPDATE system_users SET status=1 WHERE id=99");assertEquals("SENT",batches.processAutomaticBatch(original.getId()).status());assertEquals(before,jdbc.queryForList("SELECT * FROM system_notify_message ORDER BY id"));
    }
    @Test void directoryFailureAfterActivationPersistsAccurateFailureThenFirstSuccessfulReadFreezes(){
        connectProductionSource();doThrow(new IllegalStateException("IC32 system directory offline")).when(directoryUsers).getUserListByDeptIds(any());control();
        assertEquals("ACTIVE",files.selectById(20L).getStatus());assertEquals("ROSTER_SOURCE_FAILED",batch().getErrorCode());assertNull(batch().getSnapshotPayloadHash());assertTrue(platformRows.selectList().isEmpty());assertTrue(recipients.selectList().isEmpty());assertNull(DataPermissionContextHolder.get());
        doCallRealMethod().when(directoryUsers).getUserListByDeptIds(any());jdbc.update("UPDATE system_users SET status=1 WHERE id=99");directoryUser(104L,31L,1L,0);
        assertEquals("SENT",batches.processAutomaticBatch(batch().getId()).status());assertEquals(List.of(100L,104L),recipients.selectList().stream().map(DccControlledFileDistributionRecipientDO::getUserId).sorted().toList());verify(source,times(2)).resolve(any());
    }
    @ParameterizedTest @ValueSource(strings={"EMPTY","DISABLED_DEPARTMENT"})
    void explicitDepartmentFailureNeverSendsOrDropsTheSavedDepartment(String fault){
        connectProductionSource();if(fault.equals("EMPTY"))jdbc.update("UPDATE system_users SET status=1 WHERE dept_id=31 AND tenant_id=1");else jdbc.update("UPDATE system_dept SET status=1 WHERE id=31");control();
        assertEquals(fault.equals("EMPTY")?"ROSTER_DEPARTMENT_EMPTY":"ROSTER_DEPARTMENT_UNAVAILABLE",batch().getErrorCode());assertTrue(batch().getErrorMessage().contains("31"));assertEquals("FAILED",batch().getStatus());assertNull(batch().getSnapshotPayloadHash());assertEquals("[\"31\"]",batch().getDepartmentIdsJson());assertEquals("ACTIVE",files.selectById(20L).getStatus());assertTrue(platformRows.selectList().isEmpty());assertTrue(recipients.selectList().isEmpty());
    }
    @Test void futureEffectiveAndUncontrolledArtifactNeverQueryProductionDirectory(){
        connectProductionSource();jdbc.update("UPDATE dcc_controlled_file SET effective_date=? WHERE id=20",now.toLocalDate().plusDays(2));control();assertTrue(batchRows.selectList().isEmpty());verifyNoInteractions(source);assertTrue(platformRows.selectList().isEmpty());
    }
    @Test void missingControlledArtifactCannotActivateOrResolveProductionRoster(){
        connectProductionSource();jdbc.update("UPDATE dcc_controlled_file SET published_file_id=NULL,stamped_file_id=NULL WHERE id=20");assertThrows(RuntimeException.class,this::control);assertRollback();verifyNoInteractions(source);assertTrue(platformRows.selectList().isEmpty());
    }
    @Test void frozenAccountDirectoryFailureCannotDispatchOrReplaceFrozenEvidence(){
        connectProductionSource();failSecondNotificationOnce();control();var original=batch();var oldObligations=obligations();var oldKeys=messageKeys();var attemptsBefore=attempts.selectList().size();
        doAnswer(call->{assertNotNull(DataPermissionContextHolder.get());assertFalse(DataPermissionContextHolder.get().enable());assertFalse(TenantContextHolder.isIgnore());throw new IllegalStateException("IC32 frozen account directory unavailable");}).when(directoryUsers).getUserList(any());
        var failure=assertThrows(IllegalStateException.class,()->batches.processAutomaticBatch(original.getId()));assertEquals("IC32 frozen account directory unavailable",failure.getMessage());assertEquals("FAILED",batch().getStatus());assertEquals(1,platformRows.selectList().size());assertEquals(attemptsBefore,attempts.selectList().size());expectSameFrozenFacts(original,oldObligations,oldKeys);assertNull(DataPermissionContextHolder.get());
    }
    @Test void nullFrozenAccountDirectoryResponseCannotSucceedOrDropObligations(){
        connectProductionSource();failSecondNotificationOnce();control();var original=batch();var oldObligations=obligations();var oldKeys=messageKeys();
        doReturn(null).when(directoryUsers).getUserList(any());assertThrows(IllegalStateException.class,()->batches.processAutomaticBatch(original.getId()));assertEquals("FAILED",batch().getStatus());assertEquals(1,platformRows.selectList().size());expectSameFrozenFacts(original,oldObligations,oldKeys);
    }
}
