package cn.iocoder.yudao.module.mes.service.pro.batchrecord;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesMarketReleaseHandoverMapper;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class MesMarketReleaseReadScopeTest {
 @Test void batchAccessQueryParsesAndReturnsLatestOwnerAndPriorRecipient() throws Exception {
  var method=MesMarketReleaseHandoverMapper.class.getMethod("selectAccessByBatchIdsInternal",Collection.class,Long.class,Long.class);
  var script=String.join("\n",method.getAnnotation(org.apache.ibatis.annotations.Select.class).value());
  var configuration=new Configuration();
  var source=new XMLLanguageDriver().createSqlSource(configuration,script,Map.class);
  try(var connection=DriverManager.getConnection("jdbc:h2:mem:market_access;MODE=MySQL","sa","");var ddl=connection.createStatement()) {
   ddl.execute("CREATE TABLE mes_pro_edhr_market_release_handover(id BIGINT,tenant_id BIGINT,batch_execution_id BIGINT,original_user_id BIGINT,original_user_name VARCHAR,recipient_user_id BIGINT,recipient_user_name VARCHAR,recalled_at TIMESTAMP,deleted INT)");
   ddl.execute("INSERT INTO mes_pro_edhr_market_release_handover VALUES(1,1,50,100,'owner',200,'first','2026-10-27 10:00:00',0),(2,1,50,100,'owner',300,'current',NULL,0),(3,2,50,400,'other',200,'first',NULL,0)");
   for(Long actor:Arrays.asList(200L,300L,100L,null)) {
    Map<String,Object> params=new HashMap<>();params.put("batchIds",List.of(50L));params.put("actor",actor);params.put("tenantId",1L);
    var sql=source.getBoundSql(params);
    assertNotNull(net.sf.jsqlparser.parser.CCJSqlParserUtil.parse(sql.getSql()));
    var mapped=new MappedStatement.Builder(configuration,"batchAccess",source,SqlCommandType.SELECT).build();
    try(var statement=connection.prepareStatement(sql.getSql())) {
     new DefaultParameterHandler(mapped,params,sql).setParameters(statement);
     try(var rows=statement.executeQuery()) {
      assertTrue(rows.next());assertEquals(2L,rows.getLong("id"));assertEquals(300L,rows.getLong("recipient_user_id"));
      assertEquals(Objects.equals(actor,200L)||Objects.equals(actor,300L),rows.getBoolean("actor_recipient"));assertFalse(rows.next());
     }
    }
   }
  }
 }
 @Test void revokedUsersAreExcludedBeforePaginationWithoutCrossTenantDenial() throws Exception {
  TenantContextHolder.setTenantId(1L);
  try(var security=mockStatic(SecurityFrameworkUtils.class);
      var connection=DriverManager.getConnection("jdbc:h2:mem:market_handover;MODE=MySQL","sa","")) {
   var statement=connection.createStatement();
   statement.execute("CREATE TABLE sample(id BIGINT,batch_execution_id BIGINT)");
   statement.execute("INSERT INTO sample VALUES(1,50),(2,70),(3,NULL)");
   statement.execute("CREATE TABLE mes_pro_edhr_market_release_handover(id BIGINT,tenant_id BIGINT,batch_execution_id BIGINT,original_user_id BIGINT,recipient_user_id BIGINT,recalled_at TIMESTAMP,deleted INT)");
   statement.execute("INSERT INTO mes_pro_edhr_market_release_handover VALUES(1,1,50,100,200,'2026-10-27 10:00:00',0),(2,1,50,100,300,NULL,0),(3,2,70,100,200,'2026-10-27 11:00:00',0)");
   security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(200L);
   assertEquals(List.of(2L,3L),query(connection));
   security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(100L);
   assertEquals(List.of(1L,2L,3L),query(connection));
   security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(300L);
   assertEquals(List.of(1L,2L,3L),query(connection));
   statement.execute("UPDATE mes_pro_edhr_market_release_handover SET recalled_at='2026-10-27 12:00:00' WHERE id=2");
   assertEquals(List.of(2L,3L),query(connection));
  } finally {TenantContextHolder.clear();}
 }
 private List<Long> query(Connection connection) throws Exception {
  var wrapper=MesMarketReleaseHandoverMapper.readable(new LambdaQueryWrapperX<>(),"batch_execution_id");
  var configuration=new Configuration();
  var source=new XMLLanguageDriver().createSqlSource(configuration,"<script>SELECT id FROM sample WHERE ${ew.sqlSegment} ORDER BY id</script>",Map.class);
  var params=Map.of("ew",wrapper);var sql=source.getBoundSql(params);
  var mapped=new MappedStatement.Builder(configuration,"scope",source,SqlCommandType.SELECT).build();
  try(var statement=connection.prepareStatement(sql.getSql())) {
   new DefaultParameterHandler(mapped,params,sql).setParameters(statement);
   var rows=statement.executeQuery();List<Long> result=new ArrayList<>();
   while(rows.next())result.add(rows.getLong(1));return result;
  }
 }
}
