package cn.iocoder.yudao.module.mes.dal.mysql.pro.testdata;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/** Executes the production candidate and route SQL; service mocks cannot verify these joins. */
class MesLocalTestRunDefinitionMapperTest {
  private JdbcTemplate jdbc;
  private MesLocalTestRunDefinitionMapper mapper;

  @BeforeEach
  void setup() {
    var ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:definitions" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
    jdbc = new JdbcTemplate(ds);
    String[] tables = {
      "dcc_project_product_create_request(id BIGINT, tenant_id BIGINT, deleted INT, status VARCHAR(32), relation_id BIGINT, generated_project_code_id BIGINT, generated_product_catalog_id BIGINT, product_code VARCHAR(64), product_name VARCHAR(64))",
      "dcc_project_product_relation(id BIGINT, tenant_id BIGINT, deleted INT, request_id BIGINT, relation_status VARCHAR(32), project_code_id BIGINT, product_catalog_id BIGINT)",
      "dcc_project_code(id BIGINT, tenant_id BIGINT, deleted INT, status VARCHAR(32), project_code VARCHAR(64), product_master_id BIGINT)",
      "dcc_product_catalog(id BIGINT, tenant_id BIGINT, deleted INT, product_code VARCHAR(64), product VARCHAR(64))",
      "mdm_product(id BIGINT, tenant_id BIGINT, deleted INT, status VARCHAR(32))",
      "mes_md_item(id BIGINT, tenant_id BIGINT, deleted INT, product_master_id BIGINT, code VARCHAR(64), name VARCHAR(64), status INT)",
      "mes_pro_route_product(tenant_id BIGINT, deleted INT, route_id BIGINT, item_id BIGINT)",
      "mes_pro_route(id BIGINT, tenant_id BIGINT, deleted INT, status INT)",
      "mes_pro_route_version(id BIGINT, route_id BIGINT, tenant_id BIGINT, deleted INT, active INT, lifecycle_status VARCHAR(32))",
      "mes_pro_route_dcc_project_binding(route_id BIGINT, tenant_id BIGINT, deleted INT, dcc_project_code_id BIGINT)"
    };
    for (String table : tables) jdbc.execute("CREATE TABLE " + table);
    jdbc.update("INSERT INTO dcc_project_product_create_request VALUES(1,1,0,'COMPLETED',4,2,3,'PP-A','Approved A')");
    jdbc.update("INSERT INTO dcc_project_product_relation VALUES(4,1,0,1,'ACTIVE',2,3)");
    jdbc.update("INSERT INTO dcc_project_code VALUES(2,1,0,'ENABLE','PROJECT-A',99)");
    jdbc.update("INSERT INTO dcc_product_catalog VALUES(3,1,0,'PP-A','Approved A')");
    jdbc.update("INSERT INTO mdm_product VALUES(99,1,0,'ENABLE')");
    jdbc.update("INSERT INTO mes_md_item VALUES(10,1,0,99,'PP-A','Approved A',0)");
    jdbc.update("INSERT INTO mes_pro_route_product VALUES(1,0,50,10)");
    jdbc.update("INSERT INTO mes_pro_route VALUES(50,1,0,0)");
    jdbc.update("INSERT INTO mes_pro_route_version VALUES(51,50,1,0,1,'ACTIVE')");
    jdbc.update("INSERT INTO mes_pro_route_dcc_project_binding VALUES(50,1,0,2)");
    var config = new Configuration();
    config.setEnvironment(new Environment("test", new SpringManagedTransactionFactory(), ds));
    config.addMapper(MesLocalTestRunDefinitionMapper.class);
    mapper = new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(config))
        .getMapper(MesLocalTestRunDefinitionMapper.class);
  }

  @Test
  void selectsOnlyExactFormalIdentityAndTenant() {
    jdbc.update("INSERT INTO mes_md_item VALUES(11,2,0,99,'PP-A','Approved A',0)");
    jdbc.update("INSERT INTO mes_md_item VALUES(12,1,0,100,'PP-A','Approved A',0)");
    var rows = mapper.selectApproved(1L);
    assertEquals(1, rows.size());
    var row = rows.get(0);
    assertEquals(1L, row.requestId());
    assertEquals(2L, row.projectId());
    assertEquals(3L, row.catalogId());
    assertEquals(4L, row.relationId());
    assertEquals(10L, row.mesProductId());
    assertEquals("PP-A", row.productCode());
    assertEquals("Approved A", row.productName());
    assertEquals("PROJECT-A", row.projectCode());
    assertTrue(mapper.selectApproved(2L).isEmpty());
  }

  @Test
  void requiresExplicitRouteMappingRatherThanTextOrShowroomMaster() {
    jdbc.update("UPDATE dcc_project_code SET product_master_id=NULL");
    jdbc.update("UPDATE mes_md_item SET product_master_id=NULL");
    assertEquals(10L, mapper.selectApproved(1L).get(0).mesProductId());
    jdbc.update("UPDATE mes_pro_route_dcc_project_binding SET dcc_project_code_id=999");
    assertNull(mapper.selectApproved(1L).get(0).mesProductId());
    jdbc.update("UPDATE mes_pro_route_dcc_project_binding SET dcc_project_code_id=2,tenant_id=2");
    assertNull(mapper.selectApproved(1L).get(0).mesProductId());
  }

  @Test
  void excludesUnapprovedBrokenOrDeletedFormalLinks() {
    jdbc.update("UPDATE dcc_project_product_create_request SET status='REJECTED'");
    assertTrue(mapper.selectApproved(1L).isEmpty());
    jdbc.update("UPDATE dcc_project_product_create_request SET status='COMPLETED'");
    jdbc.update("UPDATE dcc_project_product_relation SET request_id=999");
    assertTrue(mapper.selectApproved(1L).isEmpty());
    jdbc.update("UPDATE dcc_project_product_relation SET request_id=1,deleted=1");
    assertTrue(mapper.selectApproved(1L).isEmpty());
  }

  @Test
  void routeRequiresExactTenantProjectItemAndActiveVersion() {
    assertEquals(1, mapper.countExactActiveRoute(1L, 2L, 10L));
    assertEquals(0, mapper.countExactActiveRoute(2L, 2L, 10L));
    assertEquals(0, mapper.countExactActiveRoute(1L, 3L, 10L));
    assertEquals(0, mapper.countExactActiveRoute(1L, 2L, 11L));
    jdbc.update("UPDATE mes_pro_route_version SET active=0");
    assertEquals(0, mapper.countExactActiveRoute(1L, 2L, 10L));
    assertEquals(10L, mapper.selectApproved(1L).get(0).mesProductId());
    jdbc.update("UPDATE mes_pro_route_version SET active=1,lifecycle_status='DRAFT'");
    assertEquals(0, mapper.countExactActiveRoute(1L, 2L, 10L));
  }

  @Test
  void rejectsAnotherRouteEvenWhenBoundToAnotherProject() {
    jdbc.update("INSERT INTO mes_pro_route VALUES(60,1,0,0)");
    jdbc.update("INSERT INTO mes_pro_route_product VALUES(1,0,60,10)");
    jdbc.update("INSERT INTO mes_pro_route_dcc_project_binding VALUES(60,1,0,3)");
    assertEquals(0, mapper.countExactActiveRoute(1L, 2L, 10L));
    jdbc.update("UPDATE mes_pro_route SET deleted=1 WHERE id=60");
    assertEquals(1, mapper.countExactActiveRoute(1L, 2L, 10L));
  }
}
