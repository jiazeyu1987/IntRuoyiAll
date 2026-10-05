package cn.iocoder.yudao.module.mes.service.pro.handoff;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.*;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApiImpl;
import cn.iocoder.yudao.module.system.dal.mysql.notify.NotifyMessageMapper;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.iocoder.yudao.module.system.service.notify.*;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification.MesReleaseTaskNotificationPlatformSender;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
/** Real handoff mapper, H2 transaction and Spring afterCommit/REQUIRES_NEW proxies; real platform idempotent sender/message SQL; audit policy is an isolated boundary. */
class MesActiveOrderHandoffDeliveryReliabilityTest {
 JdbcTemplate jdbc;DataSourceTransactionManager manager;MesActiveOrderHandoffDeliveryMapper mapper;MesActiveOrderHandoffTaskMapper tasks;
 MesActiveOrderHandoffDeliveryService service;MesActiveOrderHandoffDeliveryTransactionService attempts;MesActiveOrderHandoffAudit audit;NotifyMessageSendApi sender;MesActiveOrderHandoffTaskDO task;
 @BeforeEach void fixture()throws Exception{
 var ds=new DriverManagerDataSource("jdbc:h2:mem:handoff_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1","sa","");jdbc=new JdbcTemplate(ds);manager=new DataSourceTransactionManager(ds);
 jdbc.execute("CREATE TABLE mes_active_order_handoff_delivery(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,handoff_task_id BIGINT,user_id BIGINT,business_key VARCHAR(255),template_params_json CLOB,status VARCHAR(16),attempt_count INT,row_version INT,last_attempt_at TIMESTAMP,sent_at TIMESTAMP,system_message_id BIGINT,last_error_summary VARCHAR(512),creator VARCHAR(64),updater VARCHAR(64),create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,deleted BOOLEAN DEFAULT FALSE,UNIQUE(tenant_id,handoff_task_id,user_id),UNIQUE(tenant_id,business_key))");
 jdbc.execute("CREATE TABLE system_notify_message(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT NOT NULL,user_id BIGINT NOT NULL,user_type INT NOT NULL,business_key VARCHAR(255),template_id BIGINT,template_code VARCHAR(128),template_type INT,template_nickname VARCHAR(255),template_content VARCHAR(12000),template_params VARCHAR(12000),read_status BOOLEAN DEFAULT FALSE,read_time TIMESTAMP,creator VARCHAR(64),updater VARCHAR(64),create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,deleted BOOLEAN DEFAULT FALSE,UNIQUE(tenant_id,business_key))");
 var config=new MybatisConfiguration();config.setMapUnderscoreToCamelCase(true);var factory=new MybatisSqlSessionFactoryBean();factory.setDataSource(ds);factory.setConfiguration(config);factory.setGlobalConfig(new com.baomidou.mybatisplus.core.config.GlobalConfig().setDbConfig(new com.baomidou.mybatisplus.core.config.GlobalConfig.DbConfig().setIdType(com.baomidou.mybatisplus.annotation.IdType.AUTO)).setMetaObjectHandler(new cn.iocoder.yudao.framework.mybatis.core.handler.DefaultDBFieldHandler()));com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils.setGlobalConfig(config,new com.baomidou.mybatisplus.core.config.GlobalConfig().setDbConfig(new com.baomidou.mybatisplus.core.config.GlobalConfig.DbConfig().setIdType(com.baomidou.mybatisplus.annotation.IdType.AUTO)).setMetaObjectHandler(new cn.iocoder.yudao.framework.mybatis.core.handler.DefaultDBFieldHandler()));config.addMapper(MesActiveOrderHandoffDeliveryMapper.class);config.addMapper(NotifyMessageMapper.class);var sf=Objects.requireNonNull(factory.getObject());var sessions=new SqlSessionTemplate(sf);mapper=sessions.getMapper(MesActiveOrderHandoffDeliveryMapper.class);
 tasks=mock(MesActiveOrderHandoffTaskMapper.class);audit=mock(MesActiveOrderHandoffAudit.class);
 var templates=mock(NotifyTemplateService.class);var template=new NotifyTemplateDO().setId(9001L).setCode(MesActiveOrderHandoffContract.TEMPLATE).setStatus(0).setType(1).setNickname("交接通知").setContent("{reason}").setParams(List.of("activeOrderId","taskName","actionUrl","reason","handoffTaskId","handoffType"));
 when(templates.getNotifyTemplateByCodeFromCache(MesActiveOrderHandoffContract.TEMPLATE)).thenReturn(template);when(templates.formatNotifyTemplateContent(anyString(),anyMap())).thenAnswer(i->i.<Map<String,Object>>getArgument(1).get("reason").toString());
 var messages=new NotifyMessageServiceImpl();ReflectionTestUtils.setField(messages,"notifyMessageMapper",sessions.getMapper(NotifyMessageMapper.class));
 var sendTarget=new NotifySendServiceImpl();ReflectionTestUtils.setField(sendTarget,"notifyTemplateService",templates);ReflectionTestUtils.setField(sendTarget,"notifyMessageService",messages);
 var api=new NotifyMessageSendApiImpl();ReflectionTestUtils.setField(api,"notifySendService",tx(sendTarget));sender=spy(api);
 var platformTarget=new MesReleaseTaskNotificationPlatformSender();ReflectionTestUtils.setField(platformTarget,"notifyMessageSendApi",sender);var platformSender=(MesReleaseTaskNotificationPlatformSender)tx(platformTarget);
 var kernel=new MesActiveOrderHandoffDeliveryTransactionService();ReflectionTestUtils.setField(kernel,"mapper",mapper);ReflectionTestUtils.setField(kernel,"audit",audit);attempts=(MesActiveOrderHandoffDeliveryTransactionService)tx(kernel);
 var target=new MesActiveOrderHandoffDeliveryService();ReflectionTestUtils.setField(target,"mapper",mapper);ReflectionTestUtils.setField(target,"tasks",tasks);ReflectionTestUtils.setField(target,"transactions",attempts);ReflectionTestUtils.setField(target,"platformSender",platformSender);ReflectionTestUtils.setField(target,"audit",audit);service=(MesActiveOrderHandoffDeliveryService)tx(target);
 TenantContextHolder.setTenantId(1L);task=MesActiveOrderHandoffContractTest.review();when(tasks.selectById(task.getId())).thenAnswer(i->task);
 }
 Object tx(Object target){var p=new ProxyFactory(target);p.setProxyTargetClass(true);p.addAdvice(new TransactionInterceptor(manager,new AnnotationTransactionAttributeSource()));return p.getProxy();}
 // Closing the JdbcTemplate connection then retires this isolated database. SHUTDOWN closes
 // the connection before Spring can read SQL warnings and masks otherwise valid assertions.
 @AfterEach void clear(){TenantContextHolder.clear();jdbc.execute("SET DB_CLOSE_DELAY 0");}
 void commit(){new TransactionTemplate(manager).executeWithoutResult(s->service.schedule(task));}
 MesActiveOrderHandoffDeliveryDO row(){return mapper.forTask(1L,task.getId()).get(0);}
 @Test void rollbackRemovesIntentAndNeverCallsPlatform(){assertThrows(IllegalStateException.class,()->new TransactionTemplate(manager).executeWithoutResult(s->{service.schedule(task);throw new IllegalStateException("business failed");}));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM mes_active_order_handoff_delivery",Integer.class));verifyNoInteractions(sender);assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM system_notify_message",Integer.class));}
 @Test void afterBusinessCommitRealPlatformMessageIsIndependentlyCommitted(){commit();assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM system_notify_message",Integer.class));Long message=jdbc.queryForObject("SELECT id FROM system_notify_message",Long.class);assertEquals(message,row().getSystemMessageId());assertEquals("SENT",row().getStatus());}
 @Test void sameTaskScheduleReplayCreatesZeroDuplicateRows(){commit();commit();assertEquals(1,mapper.forTask(1L,task.getId()).size());assertEquals("SENT",row().getStatus());assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM system_notify_message",Integer.class));verify(sender,times(1)).sendSingleMessageIdempotentlyToAdmin(any());}
 @Test void failureIsDurableVisibleAndOwnReasonedRetryRecovers(){doThrow(new IllegalStateException("platform unavailable")).when(sender).sendSingleMessageIdempotentlyToAdmin(any());commit();var failed=row();assertEquals("FAILED",failed.getStatus());assertEquals(1,failed.getAttemptCount());assertEquals("IllegalStateException",failed.getLastErrorSummary());reset(sender);assertThrows(RuntimeException.class,()->service.retry(failed.getId(),failed.getRowVersion(),343L,""));assertThrows(RuntimeException.class,()->service.retry(failed.getId(),failed.getRowVersion(),344L,"原收件人重试"));service.retry(failed.getId(),failed.getRowVersion(),343L,"原收件人重试");assertEquals("SENT",row().getStatus());assertEquals(2,row().getAttemptCount());}
 @Test void platformSuccessButSentAuditFailureCanRecoverWithoutDuplicateMessage(){var once=new AtomicBoolean(true);doAnswer(i->{if("delivery-sent".equals(i.getArgument(0))&&once.getAndSet(false))throw new IllegalStateException("ACK audit rejected");return null;}).when(audit).append(anyString(),anyString(),anyString(),anyLong(),anyInt(),any(),any(),anyString());commit();var failed=row();assertEquals("FAILED",failed.getStatus());assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM system_notify_message",Integer.class));service.retry(failed.getId(),failed.getRowVersion(),343L,"恢复真实发送回执");assertEquals("SENT",row().getStatus());assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM system_notify_message",Integer.class));verify(sender,times(2)).sendSingleMessageIdempotentlyToAdmin(any());}
 @Test void oldAttemptAckCannotOverwriteNewSent(){doThrow(new IllegalStateException()).when(sender).sendSingleMessageIdempotentlyToAdmin(any());commit();var failed=row();var first=attempts.recordAttempt(failed.getId(),failed.getRowVersion(),"尝试一");var second=attempts.recordAttempt(failed.getId(),first.getRowVersion(),"尝试二");attempts.recordSent(second.getId(),second.getRowVersion(),903L,"正式成功");assertThrows(RuntimeException.class,()->attempts.recordFailed(first.getId(),first.getRowVersion(),"old failure","旧回执"));assertEquals("SENT",row().getStatus());assertEquals(903L,row().getSystemMessageId());}
 @Test void retiredCycleCannotRetryPendingNotification(){doThrow(new IllegalStateException()).when(sender).sendSingleMessageIdempotentlyToAdmin(any());commit();var failed=row();task.setStatus("CANCELED");assertThrows(RuntimeException.class,()->service.retry(failed.getId(),failed.getRowVersion(),343L,"旧周期重试"));assertEquals("FAILED",row().getStatus());}
}
