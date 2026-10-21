package cn.iocoder.yudao.module.system.service.profileworkbench;
import cn.iocoder.yudao.module.system.api.workbench.*;
import cn.iocoder.yudao.module.system.dal.mysql.profileworkbench.*;
import cn.iocoder.yudao.module.system.dal.dataobject.profileworkbench.ProfileWorkbenchTaskVisibilityDO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.aop.support.AopUtils;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.*;
class WorkbenchVisibilityTransactionTest {
 @Configuration @EnableTransactionManagement @Import({WorkbenchVisibilityContextInitializer.class,WorkbenchVisibilityWriteTransaction.class})
 static class Config {
  @Bean DataSource dataSource(){return new DriverManagerDataSource("jdbc:h2:mem:workbench-s3;DB_CLOSE_DELAY=-1","sa","");}
  @Bean PlatformTransactionManager transactionManager(DataSource ds){return new DataSourceTransactionManager(ds);}
  @Bean JdbcTemplate jdbc(DataSource ds){return new JdbcTemplate(ds);}
  @Bean WorkbenchVisibilityVersionMapper versions(JdbcTemplate db){
   var m=mock(WorkbenchVisibilityVersionMapper.class);
   when(m.initialize(anyLong(),anyLong())).thenAnswer(i->{assertTrue(TransactionSynchronizationManager.isActualTransactionActive());assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());db.update("MERGE INTO context(tenant_id,user_id,version) KEY(tenant_id,user_id) VALUES(?,?,COALESCE((SELECT version FROM context WHERE tenant_id=? AND user_id=?),1))",i.getArgument(0),i.getArgument(1),i.getArgument(0),i.getArgument(1));return 1;});
   when(m.shared(anyLong(),anyLong())).thenAnswer(i->version(db,i.getArgument(0),i.getArgument(1)));
   when(m.exclusive(anyLong(),anyLong())).thenAnswer(i->{assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());return version(db,i.getArgument(0),i.getArgument(1));});
   when(m.advance(anyLong(),anyLong(),anyLong(),anyLong())).thenAnswer(i->db.update("UPDATE context SET version=? WHERE tenant_id=? AND user_id=? AND version=?",i.getArgument(3),i.getArgument(0),i.getArgument(1),i.getArgument(2)));
   return m;
  }
  static Long version(JdbcTemplate db,long tenant,long user){var v=db.queryForList("SELECT version FROM context WHERE tenant_id=? AND user_id=?",Long.class,tenant,user);return v.isEmpty()?null:v.get(0);}
  @Bean ProfileWorkbenchTaskVisibilityMapper hidden(JdbcTemplate db){
   var m=mock(ProfileWorkbenchTaskVisibilityMapper.class);
   when(m.lockPersonal(anyLong(),anyLong(),anyString())).thenAnswer(i->db.queryForObject("SELECT COUNT(*) FROM hidden WHERE tenant_id=? AND user_id=? AND task_key=?",Long.class,i.getArgument(0),i.getArgument(1),i.getArgument(2))==0?null:new ProfileWorkbenchTaskVisibilityDO());
   when(m.insert(any(ProfileWorkbenchTaskVisibilityDO.class))).thenAnswer(i->{ProfileWorkbenchTaskVisibilityDO row=i.getArgument(0);return db.update("INSERT INTO hidden VALUES(?,?,?,?)",row.getTenantId(),row.getUserId(),row.getTaskKey(),row.getDetail());});
   when(m.deletePersonal(anyLong(),anyLong(),anyString())).thenAnswer(i->db.update("DELETE FROM hidden WHERE tenant_id=? AND user_id=? AND task_key=?",i.getArgument(0),i.getArgument(1),i.getArgument(2)));return m;
  }
 }
 AnnotationConfigApplicationContext context;JdbcTemplate db;WorkbenchVisibilityContextInitializer init;WorkbenchVisibilityWriteTransaction writer;
 @BeforeEach void setup(){context=new AnnotationConfigApplicationContext(Config.class);db=context.getBean(JdbcTemplate.class);db.execute("CREATE TABLE IF NOT EXISTS context(tenant_id BIGINT,user_id BIGINT,version BIGINT,PRIMARY KEY(tenant_id,user_id))");db.execute("CREATE TABLE IF NOT EXISTS hidden(tenant_id BIGINT,user_id BIGINT,task_key VARCHAR(160),detail VARCHAR(200),PRIMARY KEY(tenant_id,user_id,task_key))");db.update("DELETE FROM hidden");db.update("DELETE FROM context");init=context.getBean(WorkbenchVisibilityContextInitializer.class);writer=context.getBean(WorkbenchVisibilityWriteTransaction.class);}
 @AfterEach void close(){context.close();}
 TaskSummary row(){var r=new TaskSummary();r.setTaskKey("SIX:key-é");r.setBusinessId("key-é");r.setDetail("服务器展示");r.setSource("来源");r.setTaskType("类别");return r;}
 @Test void initializerCommitsOrdinaryWriteAndActualProxyWriteIsVersionedAndIdempotent(){
  assertTrue(AopUtils.isAopProxy(init));assertTrue(AopUtils.isAopProxy(writer));assertEquals(1,init.initialize(2,3));assertFalse(TransactionSynchronizationManager.isActualTransactionActive());assertEquals(1,init.initialize(2,3));
  var result=writer.write(2,3,1,"SIX:key-é",true,()->{assertTrue(TransactionSynchronizationManager.isActualTransactionActive());assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());return row();});assertTrue(result.changed());assertEquals("2",result.visibilityVersion());
  assertFalse(writer.write(2,3,2,"SIX:key-é",true,this::row).changed());assertEquals(2,Config.version(db,2,3));
  assertTrue(writer.write(2,3,2,"SIX:key-é",false,this::row).changed());assertFalse(writer.write(2,3,3,"SIX:key-é",false,this::row).changed());assertEquals(3,Config.version(db,2,3));
 }
 @Test void missingStaleAndIneligibleNeverWriteAndPersonalContextIsIsolated(){
  assertEquals(VISIBILITY_CONTEXT_MISSING.code(),assertThrows(ServiceException.class,()->writer.write(2,3,1,"SIX:key-é",true,this::row)).getCode());init.initialize(2,3);init.initialize(2,4);
  assertEquals(VISIBILITY_VERSION_CONFLICT.code(),assertThrows(ServiceException.class,()->writer.write(2,3,5,"SIX:key-é",true,this::row)).getCode());
  assertEquals(TASK_NOT_ELIGIBLE.code(),assertThrows(ServiceException.class,()->writer.write(2,3,1,"SIX:key-é",true,()->null)).getCode());writer.write(2,3,1,"SIX:key-é",true,this::row);assertEquals(1,Config.version(db,2,4));
 }
 @Test void failureAfterHiddenInsertActuallyRollsBackAndNowaitIsExactNotMessageMatching(){
  init.initialize(2,3);var versions=context.getBean(WorkbenchVisibilityVersionMapper.class);doReturn(0).when(versions).advance(2,3,1,2);
  assertEquals(VISIBILITY_WRITE_FAILED.code(),assertThrows(ServiceException.class,()->writer.write(2,3,1,"SIX:key-é",true,this::row)).getCode());assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM hidden",Integer.class));assertEquals(1,Config.version(db,2,3));
  assertEquals(VISIBILITY_WRITE_CONFLICT.code(),assertThrows(ServiceException.class,()->writer.write(2,3,1,"SIX:key-é",true,()->{throw new IllegalStateException(new SQLException("locked","HY000",3572));})).getCode());
  var unknown=new IllegalStateException(new SQLException("NOWAIT locked","HY000",1234));assertSame(unknown,assertThrows(IllegalStateException.class,()->writer.write(2,3,1,"SIX:key-é",true,()->{throw unknown;})));
 }
}
