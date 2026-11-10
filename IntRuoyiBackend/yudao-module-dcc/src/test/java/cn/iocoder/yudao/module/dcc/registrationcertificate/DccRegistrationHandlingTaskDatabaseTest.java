package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.handling.DccRegistrationHandlingTaskService;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Actual migration tables and JDBC writes in transactions; only account, permission and notify ports are replaced. */
class DccRegistrationHandlingTaskDatabaseTest {
    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private DccRegistrationHandlingTaskService service;
    private NotifyMessageSendApi messages;
    private final Map<Long,AdminUserRespDTO> accounts=new HashMap<>();
    private final Set<String> grants=new HashSet<>();
    private final LocalDateTime now=LocalDateTime.of(2026,10,30,10,0);

    @BeforeEach void setup() throws Exception {
        TenantContextHolder.setTenantId(1L);
        var dataSource=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");
        jdbc=new JdbcTemplate(dataSource);transactions=new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        Path migration=Path.of(".").toAbsolutePath();
        while(migration!=null&&!Files.exists(migration.resolve("sql/mysql/20261030_dcc_registration_handling_tasks.sql")))migration=migration.getParent();
        assertNotNull(migration,"real handling-task migration must be available");
        String schema=Files.readString(migration.resolve("sql/mysql/20261030_dcc_registration_handling_tasks.sql"));
        for(String statement:schema.substring(0,schema.indexOf("INSERT INTO system_notify_template")).replaceAll("(?m)^--.*$", "").split(";"))if(!statement.isBlank())jdbc.execute(statement);
        jdbc.execute("CREATE TABLE dcc_project_code(id BIGINT PRIMARY KEY,tenant_id BIGINT,project_code VARCHAR(32),project_name VARCHAR(128),status VARCHAR(16),deleted INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE dcc_registration_certificate_access_request(id BIGINT PRIMARY KEY,tenant_id BIGINT,requester_user_id BIGINT,certificate_id BIGINT,project_code_id BIGINT,status VARCHAR(16),request_type VARCHAR(40),deleted INT DEFAULT 0)");
        var users=mock(AdminUserApi.class);var permissions=mock(PermissionApi.class);messages=mock(NotifyMessageSendApi.class);
        when(users.getUser(anyLong())).thenAnswer(i->accounts.get(i.getArgument(0)));
        when(permissions.hasAnyPermissions(anyLong(),any(String[].class))).thenAnswer(i->{
            Long actor=i.getArgument(0);String[] values=(String[])i.getRawArguments()[1];
            return Arrays.stream(values).anyMatch(permission->grants.contains(actor+":"+permission));
        });
        when(messages.sendSingleMessageIdempotentlyToAdmin(any())).thenReturn(1001L);
        var clock=new DccRegistrationCertificateBusinessClock(Clock.fixed(now.atZone(DccRegistrationCertificateBusinessClock.BUSINESS_ZONE).toInstant(),DccRegistrationCertificateBusinessClock.BUSINESS_ZONE));
        service=new DccRegistrationHandlingTaskService(jdbc,users,permissions,messages,clock);
        for(long id:List.of(9L,10L,20L,30L))accounts.put(id,new AdminUserRespDTO().setId(id).setTenantId(1L).setStatus(0).setNickname("账号"+id));
        grant(9L,"config:update");for(long id:List.of(10L,20L)){grant(id,"upload:create");grant(id,"renewal:upload");}
        jdbc.update("INSERT INTO dcc_project_code VALUES(100,1,'PRJ-100','真实项目','ENABLE',0)");
    }
    @AfterEach void cleanup(){TenantContextHolder.clear();}
    private void grant(Long actor,String suffix){grants.add(actor+":dcc:registration-certificate:"+suffix);}
    private <T>T tx(Supplier<T> action){return transactions.execute(status->action.get());}
    private void configured(){tx(()->service.configure(9L,10L,5,8));}
    private Long projectTask(){configured();return tx(()->service.createProjectTask(100L));}
    private void request(Long id,Long actor,String status){jdbc.update("INSERT INTO dcc_registration_certificate_access_request VALUES(?,1,?,500,100,?,'UPLOAD_CERTIFICATE',0)",id,actor,status);}
    private void bind(Long id,Long actor){tx(()->{service.bindRequest(1L,actor,100L,500L,id,"UPLOAD");return null;});}
    private String status(Long task){return jdbc.queryForObject("SELECT status FROM dcc_registration_handling_task WHERE id=?",String.class,task);}

    @Test void missingConfigurationFailsAccuratelyAndDoesNotGenerateTask(){
        var error=assertThrows(ServiceException.class,()->tx(()->service.createProjectTask(100L)));
        assertTrue(error.getMessage().contains("注册证负责人"));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_registration_handling_task",Integer.class));
        verifyNoInteractions(messages);
    }
    @Test void configurationRequiresManagerActiveSameTenantOwnerAndBothHandlingPermissions(){
        assertThrows(ServiceException.class,()->tx(()->service.configure(30L,10L,5,8)));
        accounts.get(10L).setTenantId(2L);assertThrows(ServiceException.class,()->tx(()->service.configure(9L,10L,5,8)));
        accounts.get(10L).setTenantId(1L).setStatus(1);assertThrows(ServiceException.class,()->tx(()->service.configure(9L,10L,5,8)));
        accounts.get(10L).setStatus(0);grants.remove("10:dcc:registration-certificate:renewal:upload");
        assertThrows(ServiceException.class,()->tx(()->service.configure(9L,10L,5,8)));
        assertNull(service.getConfig());
    }
    @Test void projectTaskIsIdempotentAndUsesExplicitOwnerDeadlineAndNotification(){
        Long id=projectTask();assertEquals(id,tx(()->service.createProjectTask(100L)));
        var task=(DccRegistrationHandlingTaskService.Task)service.detail(10L,id).get("task");
        assertEquals(10L,task.responsibleUserId());assertEquals(now.plusDays(5),task.dueAt());assertEquals("OPEN",task.status());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_registration_handling_task",Integer.class));
        verify(messages,times(1)).sendSingleMessageIdempotentlyToAdmin(argThat(req->req.getUserId().equals(10L)&&req.getTemplateParams().get("handlingTaskId").equals(id.toString())));
        assertThrows(ServiceException.class,()->service.detail(30L,id));
        TenantContextHolder.setTenantId(2L);assertThrows(ServiceException.class,()->service.detail(10L,id));
    }
    @Test void notifyFailureRollsBackTaskAndEvent(){
        configured();when(messages.sendSingleMessageIdempotentlyToAdmin(any())).thenReturn(null);
        assertThrows(ServiceException.class,()->tx(()->service.createProjectTask(100L)));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_registration_handling_task",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_registration_handling_event",Integer.class));
    }
    @Test void onlyOriginalResponsibleCanDelegateAndPersistAuthorizerActualHandlerAndResult(){
        Long task=projectTask();assertThrows(ServiceException.class,()->tx(()->{service.delegate(20L,task,20L,now.plusDays(2),"擅自授权");return null;}));
        tx(()->{service.delegate(10L,task,20L,now.plusDays(2),"原负责人授权");return null;});
        assertFalse(service.authorizesProjectUpload(10L,100L));assertTrue(service.authorizesProjectUpload(20L,100L));
        assertThrows(ServiceException.class,()->tx(()->{service.delegate(20L,task,30L,now.plusDays(2),"二次转交");return null;}));
        request(901L,20L,"SUBMITTED");bind(901L,20L);bind(901L,20L);assertEquals("SUBMITTED",status(task));
        assertThrows(ServiceException.class,()->tx(()->{service.recordDecision(1L,9L,901L,true,"伪造审批结果");return null;}));
        jdbc.update("UPDATE dcc_registration_certificate_access_request SET status='APPROVED' WHERE id=901");
        tx(()->{service.recordDecision(1L,9L,901L,true,"已正式入库");return null;});
        tx(()->{service.recordDecision(1L,9L,901L,true,"重复回调");return null;});assertEquals("COMPLETED",status(task));
        var row=jdbc.queryForMap("SELECT responsible_user_id,assigned_user_id,actual_handler_id,result FROM dcc_registration_handling_task WHERE id=?",task);
        assertEquals(10L,((Number)row.get("responsible_user_id")).longValue());assertEquals(20L,((Number)row.get("actual_handler_id")).longValue());assertEquals("APPROVED",row.get("result"));
        assertEquals(10L,jdbc.queryForObject("SELECT actor_id FROM dcc_registration_handling_event WHERE task_id=? AND event_type='DELEGATED'",Long.class,task));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_registration_handling_attempt",Integer.class));
    }
    @Test void expiredDelegationCrossTenantAndAnotherRequesterCannotBind(){
        Long task=projectTask();tx(()->{service.delegate(10L,task,20L,now.plusDays(1),"有效授权");return null;});
        jdbc.update("UPDATE dcc_registration_handling_task SET due_at=? WHERE id=?",now.minusSeconds(1),task);
        request(901L,20L,"SUBMITTED");assertThrows(ServiceException.class,()->bind(901L,20L));
        tx(()->{service.delegate(10L,task,10L,now.plusDays(1),"本人收回");return null;});
        assertThrows(ServiceException.class,()->bind(901L,10L));
        assertThrows(ServiceException.class,()->tx(()->{service.bindRequest(2L,10L,100L,500L,901L,"UPLOAD");return null;}));
        assertEquals("OPEN",status(task));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_registration_handling_attempt",Integer.class));
    }
    @Test void rejectionAndWithdrawalReturnToSameTaskAndHistoricalCallbacksCannotChangeNewAttempt(){
        Long task=projectTask();request(901L,10L,"SUBMITTED");bind(901L,10L);
        jdbc.update("UPDATE dcc_registration_certificate_access_request SET status='REJECTED' WHERE id=901");
        tx(()->{service.recordDecision(1L,9L,901L,false,"资料退回");return null;});assertEquals("OPEN",status(task));
        assertEquals("901",service.detail(10L,task).get("restartRequestId"));
        request(902L,10L,"SUBMITTED");bind(902L,10L);
        tx(()->{service.recordDecision(1L,9L,901L,false,"重复旧回调");return null;});assertEquals("SUBMITTED",status(task));
        jdbc.update("UPDATE dcc_registration_certificate_access_request SET status='WITHDRAWN' WHERE id=902");
        assertThrows(ServiceException.class,()->tx(()->{service.recordWithdrawal(1L,20L,902L,"其他人撤回");return null;}));
        tx(()->{service.recordWithdrawal(1L,10L,902L,"申请人撤回");return null;});assertEquals("OPEN",status(task));
        assertEquals(List.of("REJECTED","WITHDRAWN"),jdbc.queryForList("SELECT result FROM dcc_registration_handling_attempt ORDER BY request_id",String.class));
    }
    @Test void expiredScanUsesActualCurrentFactsAndIsIdempotentPerVersion(){
        configured();jdbc.execute("CREATE TABLE dcc_registration_certificate(id BIGINT PRIMARY KEY,tenant_id BIGINT,project_code_id BIGINT,current_version_id BIGINT,current_snapshot_id BIGINT,pending_version_id BIGINT,status VARCHAR(16),deleted INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE dcc_registration_certificate_version(id BIGINT PRIMARY KEY,tenant_id BIGINT,certificate_id BIGINT,status VARCHAR(16),deleted INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE dcc_registration_certificate_snapshot(id BIGINT PRIMARY KEY,tenant_id BIGINT,version_id BIGINT,product_name VARCHAR(100),facts_json VARCHAR(4000),deleted INT DEFAULT 0)");
        for(long id=1;id<=4;id++){
            jdbc.update("INSERT INTO dcc_registration_certificate VALUES(?,1,100,?,?,NULL,'ACTIVE',0)",id,100+id,200+id);
            jdbc.update("INSERT INTO dcc_registration_certificate_version VALUES(?,1,?,'CURRENT',0)",100+id,id);
            String facts=id==1?"{\"certificateType\":\"DOMESTIC\",\"certificateStatus\":\"VALID\",\"expiryDate\":\"2026-10-29\"}":id==2?"{\"certificateType\":\"DOMESTIC\",\"certificateStatus\":\"VALID\",\"expiryDate\":\"2026-10-30\"}":id==3?"{\"certificateType\":\"FDA\",\"certificateStatus\":\"EXPIRED\"}":"{\"certificateType\":\"CE\",\"certificateStatus\":\"REVOKED\",\"expiryDate\":\"2020-01-01\"}";
            jdbc.update("INSERT INTO dcc_registration_certificate_snapshot VALUES(?,1,?,'测试产品',?,0)",200+id,100+id,facts);
        }
        for(long id=5;id<=7;id++){
            jdbc.update("INSERT INTO dcc_registration_certificate VALUES(?,1,100,?,?,NULL,'ACTIVE',0)",id,100+id,200+id);
            jdbc.update("INSERT INTO dcc_registration_certificate_version VALUES(?,1,?,'CURRENT',0)",100+id,id==7?999:id);
            jdbc.update("INSERT INTO dcc_registration_certificate_snapshot VALUES(?,?,?,'错误归属快照',?,0)",200+id,id==5?2:1,id==6?999:100+id,
                    "{\"certificateType\":\"DOMESTIC\",\"certificateStatus\":\"VALID\",\"expiryDate\":\"2026-10-29\"}");
        }
        assertEquals(1,tx(()->service.createExpiredTasks(1L,now.toLocalDate())).intValue());assertEquals(0,tx(()->service.createExpiredTasks(1L,now.toLocalDate())).intValue());
        var row=jdbc.queryForMap("SELECT task_kind,certificate_id,due_at FROM dcc_registration_handling_task");
        assertEquals("RENEWAL",row.get("task_kind"));assertEquals(1L,((Number)row.get("certificate_id")).longValue());
        assertEquals(now.plusDays(8),jdbc.queryForObject("SELECT due_at FROM dcc_registration_handling_task",LocalDateTime.class));
    }
}
