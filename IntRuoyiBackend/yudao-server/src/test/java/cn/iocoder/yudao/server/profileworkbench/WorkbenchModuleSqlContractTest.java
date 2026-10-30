package cn.iocoder.yudao.server.profileworkbench;
import cn.iocoder.yudao.module.system.api.workbench.*;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class WorkbenchModuleSqlContractTest {
 Configuration configuration;
 static final String DCC="cn.iocoder.yudao.module.dcc.dal.mysql.file.DccWorkbenchTodoMapper.";
 static final String MES="cn.iocoder.yudao.module.mes.dal.mysql.pro.MesWorkbenchTodoMapper.";
 static final String SHOW="cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomWorkbenchTodoMapper.";
 @BeforeEach void parseCurrentSourceXml()throws Exception{
  configuration=new Configuration();configuration.setMapUnderscoreToCamelCase(true);
  for(String file:List.of("yudao-module-system/src/main/resources/mapper/profileworkbench/WorkbenchQuerySql.xml","yudao-module-dcc/src/main/resources/mapper/profileworkbench/DccWorkbenchTodoMapper.xml","yudao-module-mes/src/main/resources/mapper/profileworkbench/MesWorkbenchTodoMapper.xml","yudao-module-showroom/src/main/resources/mapper/profileworkbench/ShowroomWorkbenchTodoMapper.xml")){
   Path path=Path.of("..").resolve(file);try(var in=Files.newInputStream(path)){new XMLMapperBuilder(in,configuration,file,configuration.getSqlFragments()).parse();}
  }
 }
 QueryContext query(){return new WorkbenchQuerySpec("visible",null,new WorkbenchQuerySpec.QuickFilter("detail","contains","é"),new WorkbenchQuerySpec.Sort("detail","desc")).context(2,3,1);}
 String sql(String statement,Map<String,Object> parameters){return configuration.getMappedStatement(statement).getBoundSql(parameters).getSql().replaceAll("\\s+"," ");}
 @Test void allFiveCountsRejectBinaryDuplicateBaseBeforeFiltersAndChunksShareFixedOrder(){
  var count=List.of(DCC+"countDistribution",DCC+"countTraining",MES+"countEdhr",SHOW+"countAssignment");
  var chunk=List.of(DCC+"chunkDistribution",DCC+"chunkTraining",MES+"chunkEdhr",SHOW+"chunkAssignment");
  for(int i=0;i<5;i++){
   var q=query();String c=sql(count.get(i),Map.of("q",q));assertTrue(c.contains("COUNT(DISTINCT CAST(CONVERT(u.task_key USING utf8mb4) AS BINARY))"));assertTrue(c.contains("THEN COUNT(*) ELSE NULL END"));assertTrue(c.indexOf("COUNT(DISTINCT")<c.indexOf("system_profile_workbench_task_visibility"));assertFalse(c.contains(" IN (?)"));
   String all=sql(count.get(i),Map.of("q",q.partition(null)));
   assertTrue(all.contains("COUNT(DISTINCT CAST(CONVERT(r.task_key USING utf8mb4) AS BINARY))"));assertTrue(all.contains("THEN COUNT(*) ELSE NULL END"));
   assertFalse(all.contains("SELECT CASE WHEN (SELECT"));assertFalse(c.contains("AS due_group"));assertFalse(all.contains("AS due_group"));
   var args=new HashMap<String,Object>();args.put("q",q);args.put("fetchLimit",101);args.put("after",new ChunkCursor("FIXTURE","类别","来源","é","待处理",null,null,"row-😀"));String s=sql(chunk.get(i),args);
   assertTrue(s.contains("AS DATETIME(6)"));assertTrue(s.contains("CONVERT(r.row_order_key USING utf8mb4) AS BINARY) DESC"));assertTrue(s.contains("r.due_group ASC,r.due_sort ASC,r.created_sort DESC"));assertTrue(s.contains("NOT EXISTS"));assertTrue(s.contains("CONVERT(v.task_key USING utf8mb4)"));assertTrue(s.contains("LIMIT ?"));
   args.put("fetchLimit",1000);assertThrows(RuntimeException.class,()->sql(chunk.get(0),args));
  }
 }
 @Test void distributionPageAndVisibilityIdentityIncludeFormalNavigationIdentity(){
  var args=new HashMap<String,Object>();args.put("q",query());args.put("fetchLimit",101);args.put("after",null);args.put("businessId","7");
  for(String statement:List.of("chunkDistribution","identityDistribution")){
   String distribution=sql(DCC+statement,args);
   assertTrue(distribution.contains("f.version_no AS version_no"));
   assertTrue(distribution.contains("f.process_instance_id AS approval_process_instance_id"));
  }
 }
 @Test void directModuleInvalidQueryCannotUseSilentOtherwise(){
  var badSort=query();badSort.setSortKey("invalid");assertThrows(RuntimeException.class,()->sql(DCC+"countTraining",Map.of("q",badSort)));
  var q=query();q.setVisibilityFilter(new VisibilityFilter(9,3,1,"visible"));var invalid=q;assertThrows(RuntimeException.class,()->sql(MES+"countEdhr",Map.of("q",invalid)));
 }
 @Test void currentQualificationUsesLockedMesStatusAndWorkOrderSingleTableAndTrainingNowait(){
  var args=new HashMap<String,Object>();args.put("q",query().partition(null));args.put("id",7L);args.put("lockedBatchStatus",30L);
  String mes=sql(MES+"identityEdhr",args);assertTrue(mes.contains("NOT IN (40,50,60)"));assertTrue(mes.matches(".*task_type\\s*=\\s*'ARCHIVE'.*"));assertTrue(mes.contains("dossier_order"));assertTrue(mes.contains("dossier_void.change_type"));assertTrue(mes.contains("FOR UPDATE"));assertTrue(mes.contains("LOCATE(CONCAT(',',"));
  assertTrue(sql(DCC+"identityTraining",args).endsWith("FOR UPDATE NOWAIT"));
  String invalid=sql(SHOW+"countInvalidNotifications",args);assertTrue(invalid.contains("n.id IS NULL"));assertTrue(invalid.contains("a.status = 'OPEN'"));
 }

 @Test void qaHandoffUsesDistinctIdentityOnlyForCurrentActionableCandidateAndFrozenTasksStayVisible(){
  var args=new HashMap<String,Object>();args.put("q",query().partition(null));args.put("fetchLimit",101);args.put("after",null);
  for(String statement:List.of("countEdhr","chunkEdhr")) {
   String value=sql(MES+statement,args);
   assertTrue(value.contains("UNION ALL"));assertTrue(value.contains("CONCAT('EDHR_WORK_TASK:handoff:',h.id)"));
   assertTrue(value.contains("h.task_type IN ('QA_DECISION_HANDOFF','QA_REVIEW')"));assertTrue(value.contains("h.status='TODO' AND h.notification_only=0"));
   assertTrue(value.contains("FIND_IN_SET(?,h.candidate_user_snapshot)>0"));assertTrue(value.contains("ao.active_status='ACTIVE'"));
   assertTrue(value.contains("frozen.status=15"));assertTrue(value.contains("('FILL','REVIEW','APPROVE','REWORK','ARCHIVE','CLOSE')"));
   assertTrue(value.contains("THEN '批次冻结，暂不可处理'"));
  }
  args.put("id",7L);String identity=sql(MES+"identityHandoff",args);
  assertTrue(identity.endsWith("FOR UPDATE"));assertTrue(identity.contains("AND h.id=?"));
 }
 @Test void edhrWorkbenchIncludesOnlyRegisteredExecutionAndApprovalHandoffs(){
  var args=new HashMap<String,Object>();args.put("q",query().partition(null));args.put("fetchLimit",101);args.put("after",null);
  String count=sql(MES+"countEdhr",args);
  String chunk=sql(MES+"chunkEdhr",args);
  for(String statement:List.of(count,chunk)){
   assertTrue(statement.contains("UNION ALL"));
   assertTrue(statement.contains("'QA_DECISION_HANDOFF','QA_REVIEW'"));
   assertTrue(statement.contains("mes_pro_edhr_work_task.task_type"));
   String ordinaryWhere=statement.substring(statement.indexOf("WHERE mes_pro_edhr_work_task.tenant_id"),
    statement.indexOf("UNION ALL"));
   assertFalse(java.util.regex.Pattern.compile("(?i)AND\\s+mes_pro_edhr_work_task\\.task_type\\s+IN\\s*\\(").matcher(ordinaryWhere).find(),
    "new assigned execution and approval tasks must not be silently omitted by a static task-type whitelist");
   assertTrue(statement.contains("'RECORD_CHANGE_APPROVE'"), "record-change approval must remain visible for batch status 30/50");
  }
 }

 @Test void tenantParserPreservesAllFilteredCountAndChunkParametersAndVisibility() {
  var tenant = new com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor(
   new com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler() {
    @Override public net.sf.jsqlparser.expression.Expression getTenantId() {
     return new net.sf.jsqlparser.expression.LongValue(2);
    }
   });
  var counts=List.of(DCC+"countDistribution",DCC+"countTraining",MES+"countEdhr",SHOW+"countAssignment");
  var chunks=List.of(DCC+"chunkDistribution",DCC+"chunkTraining",MES+"chunkEdhr",SHOW+"chunkAssignment");
  for(String visibility:List.of("visible","hidden","all")) for(int variant=0;variant<9;variant++) {
   var q=new QueryContext();q.setTenantId(2L);q.setUserId(3L);q.setVisibility("all".equals(visibility)?null:visibility);q.freezeVisibility(1);
   if(variant==0 || variant>=3 && variant<=5)q.setTaskType("批记录");
   if(variant==1 || variant==2 || variant==3 || variant==4 || variant>=7) {
    q.setFilterField(variant>=7?"statusLabel":variant%2==0?"source":"detail");
    q.setFilterOperator(variant%2==0?"contains":"eq");q.setFilterValue("é");
   }
   if(variant>=3 && variant<=5){q.setSortKey(variant==3?"detail":"statusLabel");q.setSortOrder(variant%2==0?"asc":"desc");}
   var args=new HashMap<String,Object>();args.put("q",q);args.put("fetchLimit",101);
   args.put("after",variant==2 || variant==4 || variant==5?new ChunkCursor("FIXTURE","批记录","来源","é","待处理",null,null,"row-😀"):null);
   for(int source=0;source<counts.size();source++) for(String statement:List.of(counts.get(source),chunks.get(source))) {
    var bound=configuration.getMappedStatement(statement).getBoundSql(args);
    // Do not normalize whitespace before parsing: that previously hid the runtime truncation.
    String parsed=tenant.parserSingle(bound.getSql(),null);
    String context=statement+" / "+visibility+" / "+variant;
    assertEquals(bound.getParameterMappings().size(),parsed.chars().filter(c->c=='?').count(),context);
    assertEquals(!"all".equals(visibility),parsed.contains("system_profile_workbench_task_visibility"),context);
    if(!"all".equals(visibility)) {
     assertTrue(parsed.contains("v.user_id = ?"),context);
     assertTrue(parsed.contains("CONVERT( v.task_key USING utf8mb4 )"),context);
     assertEquals("visible".equals(visibility),parsed.contains("NOT EXISTS (SELECT 1 FROM system_profile_workbench_task_visibility"),context);
    }
    if(statement.contains("chunk"))assertTrue(parsed.endsWith("LIMIT ?"),context);
   }
  }
 }
}
