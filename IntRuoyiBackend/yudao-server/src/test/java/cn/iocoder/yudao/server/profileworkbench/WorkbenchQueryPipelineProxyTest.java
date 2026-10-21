package cn.iocoder.yudao.server.profileworkbench;
import cn.iocoder.yudao.module.system.api.workbench.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApiImpl;
import cn.iocoder.yudao.module.system.service.permission.*;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.*;
import cn.iocoder.yudao.module.system.dal.mysql.permission.*;
import cn.iocoder.yudao.module.system.dal.mysql.profileworkbench.*;
import cn.iocoder.yudao.module.system.dal.dataobject.profileworkbench.ProfileWorkbenchTaskVisibilityDO;
import cn.iocoder.yudao.module.system.service.profileworkbench.*;
import cn.iocoder.yudao.module.dcc.service.file.DccDistributionWorkbenchSource;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.hutool.extra.spring.SpringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.aop.support.AopUtils;
import org.springframework.test.util.ReflectionTestUtils;
import javax.sql.DataSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class WorkbenchQueryPipelineProxyTest {
 static final List<String> nodes=new ArrayList<>();
 static String mode(){return !TransactionSynchronizationManager.isActualTransactionActive()?"OUTSIDE":(TransactionSynchronizationManager.isCurrentTransactionReadOnly()?"RR_READ":"WRITE");}
 static void read(){assertTrue(TransactionSynchronizationManager.isActualTransactionActive());assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());assertEquals(java.sql.Connection.TRANSACTION_REPEATABLE_READ,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());}
 @Configuration @EnableTransactionManagement @Import({ProfileWorkbenchTodoQueryService.class,ProfileWorkbenchTodoReadTransaction.class,WorkbenchVisibilityContextInitializer.class,WorkbenchVisibilityWriteTransaction.class})
 static class Config {
  @Bean DataSource ds(){return new DriverManagerDataSource("jdbc:h2:mem:query-pipeline-s3;DB_CLOSE_DELAY=-1","sa","");}
  @Bean PlatformTransactionManager transactionManager(DataSource ds){return new DataSourceTransactionManager(ds);}
  @Bean JdbcTemplate db(DataSource ds){return new JdbcTemplate(ds);}
  @Bean WorkbenchVisibilityVersionMapper versions(JdbcTemplate db){
   var mapper=mock(WorkbenchVisibilityVersionMapper.class);
   when(mapper.initialize(anyLong(),anyLong())).thenAnswer(i->{nodes.add("initialize:"+mode());assertEquals("WRITE",mode());db.update("INSERT INTO personal VALUES(2,3,1)");return 1;});
   when(mapper.shared(anyLong(),anyLong())).thenAnswer(i->{nodes.add("shared:"+mode());var values=db.queryForList("SELECT version FROM personal WHERE tenant=2 AND user_id=3",Long.class);return values.isEmpty()?null:values.get(0);});
   when(mapper.exclusive(anyLong(),anyLong())).thenAnswer(i->{nodes.add("exclusive:"+mode());assertEquals("WRITE",mode());return db.queryForObject("SELECT version FROM personal WHERE tenant=2 AND user_id=3",Long.class);});
   when(mapper.advance(anyLong(),anyLong(),anyLong(),anyLong())).thenAnswer(i->{nodes.add("advance:"+mode());return db.update("UPDATE personal SET version=? WHERE version=?",new Object[]{i.getArgument(3),i.getArgument(2)});});return mapper;
  }
  @Bean ProfileWorkbenchTaskVisibilityMapper hidden(JdbcTemplate db){var mapper=mock(ProfileWorkbenchTaskVisibilityMapper.class);when(mapper.lockPersonal(anyLong(),anyLong(),anyString())).thenAnswer(i->{nodes.add("hidden:"+mode());return null;});when(mapper.insert(any(ProfileWorkbenchTaskVisibilityDO.class))).thenAnswer(i->db.update("INSERT INTO hidden VALUES(?)",((ProfileWorkbenchTaskVisibilityDO)i.getArgument(0)).getTaskKey()));return mapper;}
  @Bean ObjectMapper mapper(){return new ObjectMapper().findAndRegisterModules();}
  @Bean WorkbenchTokenStore store(){return new WorkbenchTokenStore(){final Map<String,String> values=new HashMap<>();public boolean create(String token,String value,java.time.Duration ttl){nodes.add("token:"+mode());WorkbenchQueryPipelineProxyTest.read();return values.putIfAbsent(token,value)==null;}public String read(String token){return values.get(token);}};}
  @Bean WorkbenchQueryTokens tokens(WorkbenchTokenStore store,ObjectMapper mapper){return new WorkbenchQueryTokens(store,mapper);}
  @Bean SourceAdapter fixture(){return new SourceAdapter(){
   DccDistributionWorkbenchSource auth;
   public SourceDescriptor descriptor(){return new SourceDescriptor("SIX","fixture",1,0,"来源","类别","policy","six.open",1);}
   public RowOrderContract rowOrderContract(){return RowOrderContract.UTF8_BINARY_DESC;}public SourceIdentityContract identityContract(){return SourceIdentityContract.SQL_BINARY_UNIQUE_TASK_KEY;}
   public boolean isAuthorized(long user,long tenant){return auth.isAuthorized(user,tenant);}
   public long count(QueryContext q){nodes.add("count:"+mode());read();return 0;}
   public TaskChunk readChunk(QueryContext q,ChunkCursor c,int limit){throw new AssertionError();}
   public TaskSummary resolveVisibilityIdentity(QueryContext q,String key){nodes.add("qualify:"+mode());assertEquals("WRITE",mode());var r=new TaskSummary();r.setTaskKey("SIX:"+key);r.setBusinessId(key);r.setTaskType("类别");r.setSource("来源");r.setDetail("服务器");return r;}
  };}
  @Bean WorkbenchSourceRegistry registry(SourceAdapter fixture){return new WorkbenchSourceRegistry(List.of(fixture),new WorkbenchSourceRegistry.Manifest(1,"revision",List.of(new WorkbenchSourceRegistry.Module("fixture",true,List.of("SIX")))));}
 }
 AnnotationConfigApplicationContext context;JdbcTemplate db;ProfileWorkbenchTodoQueryService service;PermissionServiceImpl permission;
 @BeforeEach void setup(){
  nodes.clear();context=new AnnotationConfigApplicationContext(Config.class);db=context.getBean(JdbcTemplate.class);db.execute("CREATE TABLE IF NOT EXISTS personal(tenant BIGINT,user_id BIGINT,version BIGINT)");db.execute("CREATE TABLE IF NOT EXISTS hidden(task_key VARCHAR(160))");db.execute("CREATE TABLE IF NOT EXISTS permission_use(event VARCHAR(16))");for(String table:List.of("personal","hidden","permission_use"))db.update("DELETE FROM "+table);service=context.getBean(ProfileWorkbenchTodoQueryService.class);TenantContextHolder.setTenantId(2L);
  // Actual module -> PermissionApiImpl -> PermissionServiceImpl -> TemporaryRoleGrantServiceImpl USE path;
  // only cache/mapper boundaries are controlled, audit INSERT really participates in current JDBC transaction.
  var role=new RoleDO().setId(9L).setStatus(0);var menu=new MenuDO().setId(7L).setStatus(0);var grant=new TemporaryRoleGrantDO().setId(1L).setUserId(3L).setRoleId(9L);grant.setTenantId(2L);
  var grantMapper=mock(TemporaryRoleGrantMapper.class);when(grantMapper.selectActiveListByUserId(eq(3L),any())).thenReturn(List.of(grant));var menus=mock(MenuMapper.class);when(menus.selectListByPermission(anyString())).thenReturn(List.of(menu));var roleMenus=mock(RoleMenuMapper.class);when(roleMenus.selectListByMenuId(anyCollection())).thenReturn(List.of(new RoleMenuDO().setRoleId(9L).setMenuId(7L)));
  var audits=mock(TemporaryRoleGrantAuditMapper.class);when(audits.insert(any(TemporaryRoleGrantAuditDO.class))).thenAnswer(i->{TemporaryRoleGrantAuditDO audit=i.getArgument(0);assertEquals("USE",audit.getEventType());assertNotEquals("RR_READ",mode());nodes.add("USE:"+mode());return db.update("INSERT INTO permission_use VALUES('USE')");});
  var temporary=new TemporaryRoleGrantServiceImpl();ReflectionTestUtils.setField(temporary,"temporaryRoleGrantMapper",grantMapper);ReflectionTestUtils.setField(temporary,"menuMapper",menus);ReflectionTestUtils.setField(temporary,"roleMenuMapper",roleMenus);ReflectionTestUtils.setField(temporary,"temporaryRoleGrantAuditMapper",audits);
  var roleService=mock(RoleService.class);when(roleService.getRoleListFromCache(anyCollection())).thenAnswer(i->((Collection<?>)i.getArgument(0)).isEmpty()?List.of():List.of(role));var menuService=mock(MenuService.class);when(menuService.getMenuIdListByPermissionFromCache(anyString())).thenReturn(List.of(7L));
  permission=spy(new PermissionServiceImpl());ReflectionTestUtils.setField(permission,"systemEntitlementService",mock(SystemEntitlementService.class));ReflectionTestUtils.setField(permission,"temporaryRoleGrantService",temporary);ReflectionTestUtils.setField(permission,"roleService",roleService);ReflectionTestUtils.setField(permission,"menuService",menuService);doReturn(Set.of()).when(permission).getUserRoleIdListByUserIdFromCache(3L);doReturn(Set.of(9L)).when(permission).getMenuRoleIdListByMenuIdFromCache(7L);
  var api=new PermissionApiImpl();ReflectionTestUtils.setField(api,"permissionService",permission);var source=new DccDistributionWorkbenchSource();ReflectionTestUtils.setField(source,"permissionApi",api);ReflectionTestUtils.setField(context.getBean(SourceAdapter.class),"auth",source);
 }
 @AfterEach void close(){context.close();TenantContextHolder.clear();}
 @Test void actualInitializerReaderWriterProxiesKeepFormalUseOutsideReadonlyAndCommitBeforeReader(){
  assertTrue(AopUtils.isAopProxy(context.getBean(WorkbenchVisibilityContextInitializer.class)));assertTrue(AopUtils.isAopProxy(context.getBean(ProfileWorkbenchTodoReadTransaction.class)));assertTrue(AopUtils.isAopProxy(context.getBean(WorkbenchVisibilityWriteTransaction.class)));
  try(var security=mockStatic(SecurityFrameworkUtils.class);var spring=mockStatic(SpringUtil.class)){
   security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(3L);spring.when(()->SpringUtil.getBean(PermissionServiceImpl.class)).thenReturn(permission);
   var response=service.context(new WorkbenchContextRequest("revision",List.of("SIX"),"epoch",new WorkbenchQuerySpec("visible",null,null,null)));
   assertEquals(List.of("USE:OUTSIDE","initialize:WRITE","shared:WRITE","shared:RR_READ","token:RR_READ"),nodes);assertFalse(TransactionSynchronizationManager.isActualTransactionActive());assertEquals(1,db.queryForObject("SELECT version FROM personal",Long.class));
   nodes.clear();service.page(response.queryVersion(),"epoch",1,10);assertEquals("USE:OUTSIDE",nodes.get(0));assertEquals("shared:RR_READ",nodes.get(1));assertEquals(3,nodes.stream().filter(n->n.equals("count:RR_READ")).count());assertFalse(nodes.stream().anyMatch(n->n.startsWith("initialize")));
   nodes.clear();service.visibility(new WorkbenchVisibilityRequest(response.queryVersion(),"epoch","SIX:opaque-key","hide"));assertEquals(List.of("USE:OUTSIDE","exclusive:WRITE","USE:WRITE","qualify:WRITE","hidden:WRITE","advance:WRITE"),nodes);assertEquals(2,db.queryForObject("SELECT version FROM personal",Long.class));assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM hidden",Long.class));
  }
 }
 @Test void initializationFailureNeverEntersReadonlyReaderAndMissingContextNeverInventsZero(){
  try(var security=mockStatic(SecurityFrameworkUtils.class);var spring=mockStatic(SpringUtil.class)){
   security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(3L);spring.when(()->SpringUtil.getBean(PermissionServiceImpl.class)).thenReturn(permission);doThrow(new IllegalStateException("initialization failed")).when(context.getBean(WorkbenchVisibilityVersionMapper.class)).initialize(2,3);
   assertThrows(IllegalStateException.class,()->service.context(new WorkbenchContextRequest("revision",List.of("SIX"),"epoch",new WorkbenchQuerySpec("visible",null,null,null))));assertFalse(nodes.stream().anyMatch(n->n.contains("RR_READ")));assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM personal",Integer.class));
  }
 }
}
