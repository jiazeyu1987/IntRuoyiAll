package cn.iocoder.yudao.module.mes.service.pro.testdata;

import static cn.iocoder.yudao.module.mes.service.pro.testdata.MesLocalTestRunContracts.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductCreateRequestDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectProductCreateRequestMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.*;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.unitmeasure.MesMdUnitMeasureDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.*;
import cn.iocoder.yudao.module.mes.dal.mysql.md.unitmeasure.MesMdUnitMeasureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.testdata.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.*;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import com.baomidou.mybatisplus.core.*;
import com.baomidou.mybatisplus.core.metadata.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;
import org.apache.ibatis.mapping.Environment;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * H2 roundtrip of actual typed mappers, including decimal/time defaults and transaction rollback.
 */
class MesLocalTestRunPreparationServiceTest {
  private JdbcTemplate jdbc;
  private SqlSessionTemplate sql;
  private TransactionTemplate tx;
  private MesLocalTestRunPreparationService service;
  private MesLocalTestRunManifestMapper manifests;
  private MesProWorkOrderMapper orders;
  private MesLocalTestRunDefinitionMapper definitions;
  private MesFixedTestOrderModeGuard mode;
  private MesLocalTestRunSourceGuard guard;
  private final Map<Class<?>, Object> beans = new HashMap<>();

  @BeforeEach
  void setup() throws Exception {
    TenantContextHolder.setTenantId(1L);
    JdbcDataSource ds = new JdbcDataSource();
    ds.setURL(
        "jdbc:h2:mem:localrun"
            + UUID.randomUUID()
            + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    jdbc = new JdbcTemplate(ds);
    tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
    MybatisConfiguration config = new MybatisConfiguration();
    config.setMapUnderscoreToCamelCase(true);
    config.setEnvironment(new Environment("test", new SpringManagedTransactionFactory(), ds));
    Class<?>[] mapperTypes = {
      MesLocalTestRunManifestMapper.class,
      MesMdItemMapper.class,
      MesMdUnitMeasureMapper.class,
      MesMdProductBomMapper.class,
      MesProWorkOrderMapper.class,
      MesProWorkOrderBomMapper.class,
      MesKingdeeProductionMaterialListMapper.class,
      ErpKingdeeProductionPickListMapper.class,
      ErpKingdeeProductionPickListItemMapper.class,
      ErpKingdeeProductionReplenishmentListMapper.class,
      ErpKingdeeProductionReplenishmentListItemMapper.class
    };
    for (Class<?> c : mapperTypes) config.addMapper(c);
    var factory = new MybatisSqlSessionFactoryBuilder().build(config);
    sql = new SqlSessionTemplate(factory);
    for (Class<?> c : mapperTypes) beans.put(c, sql.getMapper(c));
    for (TableInfo t : TableInfoHelper.getTableInfos()) {
      if (!Set.of(
              "mes_local_test_run_manifest",
              "mes_md_item",
              "mes_md_unit_measure",
              "mes_md_product_bom",
              "mes_pro_work_order",
              "mes_pro_work_order_bom",
              "mes_kingdee_production_material_list",
              "erp_kingdee_production_pick_list",
              "erp_kingdee_production_pick_list_item",
              "erp_kingdee_production_replenishment_list",
              "erp_kingdee_production_replenishment_list_item")
          .contains(t.getTableName())) continue;
      List<String> cols = new ArrayList<>();
      cols.add(t.getKeyColumn() + " BIGINT PRIMARY KEY");
      for (TableFieldInfo f : t.getFieldList())
        cols.add(f.getColumn() + " " + columnType(t.getTableName(), f));
      if (t.getFieldList().stream().noneMatch(f -> f.getColumn().equals("tenant_id")))
        cols.add("tenant_id BIGINT DEFAULT 1");
      jdbc.execute("CREATE TABLE " + t.getTableName() + " (" + String.join(",", cols) + ")");
    }
    jdbc.execute("CREATE TABLE system_tenant(id BIGINT PRIMARY KEY,deleted BOOLEAN DEFAULT FALSE)");
    jdbc.update("INSERT INTO system_tenant(id) VALUES(1)");
    jdbc.execute(
        "CREATE UNIQUE INDEX local_run_slot ON mes_local_test_run_manifest(tenant_id,run_id,slot)");
    jdbc.execute("CREATE UNIQUE INDEX local_order ON mes_local_test_run_manifest(work_order_id)");
    mode = mock(MesFixedTestOrderModeGuard.class);
    beans.put(MesFixedTestOrderModeGuard.class, mode);
    definitions = mock(MesLocalTestRunDefinitionMapper.class);
    beans.put(MesLocalTestRunDefinitionMapper.class, definitions);
    var access = mock(DccProjectAccessService.class);
    beans.put(DccProjectAccessService.class, access);
    when(access.listReadableProjectIds(9L)).thenReturn(List.of(2L));
    var requestMapper = mock(DccProjectProductCreateRequestMapper.class);
    beans.put(DccProjectProductCreateRequestMapper.class, requestMapper);
    var q = new DccProjectProductCreateRequestDO();
    q.setId(1L);
    q.setTenantId(1L);
    q.setApplicantUserId(9L);
    when(requestMapper.selectById(1L)).thenReturn(q);
    beans.put(DccProjectProductActorSupport.class, mock(DccProjectProductActorSupport.class));
    beans.put(DccProjectReviewerRoleService.class, mock(DccProjectReviewerRoleService.class));
    beans.put(PermissionApi.class, mock(PermissionApi.class));
    guard = construct(MesLocalTestRunSourceGuard.class);
    beans.put(MesLocalTestRunSourceGuard.class, guard);
    service = construct(MesLocalTestRunPreparationService.class);
    manifests = get(MesLocalTestRunManifestMapper.class);
    orders = get(MesProWorkOrderMapper.class);
    when(definitions.selectApproved(1L))
        .thenReturn(List.of(new ApprovedProduct(1L, 2L, 3L, 4L, 10L, "PP-A", "本轮产品", "P-A")));
    when(definitions.countExactActiveRoute(1L, 2L, 10L)).thenReturn(1);
    get(MesMdUnitMeasureMapper.class)
        .insert(MesMdUnitMeasureDO.builder().id(20L).code("PCS").name("件").status(0).build());
    get(MesMdItemMapper.class)
        .insert(
            MesMdItemDO.builder()
                .id(10L)
                .code("PP-A")
                .name("本轮产品")
                .unitMeasureId(20L)
                .status(0)
                .build());
    get(MesMdItemMapper.class)
        .insert(
            MesMdItemDO.builder()
                .id(11L)
                .code("MAT-A")
                .name("本轮物料")
                .unitMeasureId(20L)
                .status(0)
                .build());
    get(MesMdProductBomMapper.class)
        .insert(
            MesMdProductBomDO.builder()
                .id(30L)
                .itemId(10L)
                .bomItemId(11L)
                .quantity(new BigDecimal("1.25"))
                .build());
  }

  @AfterEach
  void clear() {
    TenantContextHolder.clear();
  }

  @SuppressWarnings("unchecked")
  private <T> T get(Class<T> c) {
    return (T) beans.get(c);
  }

  @SuppressWarnings("unchecked")
  private <T> T construct(Class<T> c) throws Exception {
    var constructor = c.getConstructors()[0];
    return (T)
        constructor.newInstance(
            Arrays.stream(constructor.getParameterTypes()).map(beans::get).toArray());
  }

  private String columnType(String table, TableFieldInfo f) {
    Class<?> c = f.getPropertyType();
    String n = f.getColumn();
    if (c == Long.class) return "BIGINT";
    if (c == Integer.class) return "INT";
    if (c == Boolean.class || c == boolean.class) return "BOOLEAN DEFAULT FALSE";
    if (c == BigDecimal.class)
      return table.equals("mes_pro_work_order") ? "DECIMAL(14,2)" : "DECIMAL(18,6)";
    if (c == java.time.LocalDateTime.class) return "TIMESTAMP(0) DEFAULT CURRENT_TIMESTAMP";
    if (c == java.time.LocalDate.class) return "DATE";
    return "VARCHAR("
        + (Set.of("batch_code", "order_source_code", "code").contains(n) ? 64 : 4096)
        + ")";
  }

  private Request input(String run) {
    var r = new Request();
    r.setSource("LOCAL_TEST_RUN");
    r.setRunId(run);
    r.setRequestId(1L);
    r.setProjectId(2L);
    r.setCatalogId(3L);
    r.setRelationId(4L);
    r.setMesProductId(10L);
    List<OrderInput> list = new ArrayList<>();
    for (String slot : List.of("A", "B")) {
      var o = new OrderInput();
      o.setSlot(slot);
      o.setQuantity(new BigDecimal("2.50"));
      o.setBatchCode(run + "-" + slot);
      o.setIncludeReplenishment(slot.equals("B"));
      list.add(o);
    }
    r.setOrders(list);
    return r;
  }

  @Test
  void preparesExactSourcesAndReplaysActualCompletedStatusWithoutLiveConfiguration() {
    // The real BOM form omits status, and the create service/database preserve null.
    assertNull(get(MesMdProductBomMapper.class).selectById(30L).getStatus());
    Request r = input("ROUND_TRIP");
    assertTrue(service.preflight(r, 9L).ready());
    var first = tx.execute(s -> service.prepare(r, 9L));
    assertEquals(2, first.orders().size());
    for (var prepared : first.orders()) {
      assertTrue(guard.validate(orders.selectById(prepared.workOrderId()), false));
      assertEquals(
          0,
          new BigDecimal("3.125").compareTo(
              jdbc.queryForObject(
                  "SELECT quantity FROM mes_pro_work_order_bom WHERE work_order_id=?",
                  BigDecimal.class, prepared.workOrderId())));
    }
    var a = manifests.selectByWorkOrderId(first.orders().get(0).workOrderId());
    assertFalse(a.getIncludeReplenishment());
    assertNull(a.getReplenishmentHeaderId());
    assertEquals("[]", a.getReplenishmentItemIdsJson());
    assertTrue(guard.validate(orders.selectById(a.getWorkOrderId()), false));
    jdbc.update(
        "UPDATE mes_pro_work_order SET status=2,quantity_produced=quantity WHERE id=?",
        a.getWorkOrderId());
    when(definitions.selectApproved(1L)).thenReturn(List.of());
    when(definitions.countExactActiveRoute(1L, 2L, 10L)).thenReturn(0);
    var replay = tx.execute(s -> service.prepare(r, 9L));
    assertEquals(a.getWorkOrderId(), replay.orders().get(0).workOrderId());
    assertEquals(2, replay.orders().get(0).status());
    assertTrue(replay.orders().stream().allMatch(OrderResult::reused));
    assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_work_order", Integer.class));
  }

  @Test
  void mixedNullAndZeroStatusBomPreservesEveryComponentInBothOrders() {
    get(MesMdItemMapper.class)
        .insert(
            MesMdItemDO.builder()
                .id(12L)
                .code("MAT-B")
                .name("本轮物料B")
                .unitMeasureId(20L)
                .status(0)
                .build());
    get(MesMdProductBomMapper.class)
        .insert(
            MesMdProductBomDO.builder()
                .id(31L)
                .itemId(10L)
                .bomItemId(12L)
                .quantity(new BigDecimal("2"))
                .status(0)
                .build());
    Request r = input("MIXED_BOM");
    assertTrue(service.preflight(r, 9L).ready());
    var result = tx.execute(s -> service.prepare(r, 9L));
    assertEquals(2, result.orders().size());
    for (var prepared : result.orders()) {
      assertTrue(guard.validate(orders.selectById(prepared.workOrderId()), false));
      assertEquals(
          2,
          jdbc.queryForObject(
              "SELECT COUNT(*) FROM mes_pro_work_order_bom WHERE work_order_id=?",
              Integer.class, prepared.workOrderId()));
      assertEquals(
          0,
          new BigDecimal("3.125").compareTo(
              jdbc.queryForObject(
                  "SELECT quantity FROM mes_pro_work_order_bom WHERE work_order_id=? AND item_id=11",
                  BigDecimal.class, prepared.workOrderId())));
      assertEquals(
          0,
          new BigDecimal("5").compareTo(
              jdbc.queryForObject(
                  "SELECT quantity FROM mes_pro_work_order_bom WHERE work_order_id=? AND item_id=12",
                  BigDecimal.class, prepared.workOrderId())));
    }
  }

  @Test
  void newRunUsesNewObjectsAndSavedRunStillRequiresProjectAndRequestAccess() {
    var first = tx.execute(s -> service.prepare(input("FIRST_RUN"), 9L));
    var second = tx.execute(s -> service.prepare(input("SECOND_RUN"), 9L));
    assertNotEquals(first.orders().get(0).workOrderId(), second.orders().get(0).workOrderId());
    assertEquals(
        4, jdbc.queryForObject("SELECT COUNT(*) FROM mes_local_test_run_manifest", Integer.class));
    doThrow(new IllegalStateException("project forbidden"))
        .when(get(DccProjectAccessService.class))
        .assertProjectViewerOrAbove(9L, 2L);
    assertThrows(IllegalStateException.class, () -> service.preflight(input("FIRST_RUN"), 9L));
    reset(get(DccProjectAccessService.class));
    when(get(DccProjectProductCreateRequestMapper.class).selectById(1L)).thenReturn(null);
    assertThrows(RuntimeException.class, () -> service.preflight(input("FIRST_RUN"), 9L));
  }

  @Test
  void changedInputAndTamperedSourceAreRejectedWithoutOverwriting() {
    var r = input("CONFLICT");
    var first = tx.execute(s -> service.prepare(r, 9L));
    r.getOrders().get(0).setQuantity(BigDecimal.ONE);
    assertFalse(service.preflight(r, 9L).ready());
    assertThrows(RuntimeException.class, () -> tx.execute(s -> service.prepare(r, 9L)));
    r.getOrders().get(0).setQuantity(new BigDecimal("2.5"));
    jdbc.update("UPDATE erp_kingdee_production_pick_list_item SET actual_quantity=99");
    assertThrows(RuntimeException.class, () -> service.preflight(r, 9L));
    assertEquals(
        2, jdbc.queryForObject("SELECT COUNT(*) FROM mes_local_test_run_manifest", Integer.class));
  }

  @Test
  void secondSlotFailureRollsBackWholeBatch() {
    jdbc.execute(
        "ALTER TABLE mes_pro_work_order ADD CONSTRAINT reject_second CHECK(code NOT LIKE '%-B')");
    assertThrows(
        RuntimeException.class, () -> tx.execute(s -> service.prepare(input("ROLLBACK"), 9L)));
    for (String table :
        List.of(
            "mes_pro_work_order",
            "mes_pro_work_order_bom",
            "mes_local_test_run_manifest",
            "mes_kingdee_production_material_list",
            "erp_kingdee_production_pick_list",
            "erp_kingdee_production_pick_list_item",
            "erp_kingdee_production_replenishment_list"))
      assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class), table);
  }

  @Test
  void missingExactProductAndUnsafePrecisionReturnNoWrites() {
    var r = input("MISSING");
    r.setMesProductId(null);
    assertFalse(service.preflight(r, 9L).ready());
    r.setMesProductId(10L);
    r.getOrders().get(0).setQuantity(new BigDecimal("0.001"));
    assertThrows(RuntimeException.class, () -> service.preflight(r, 9L));
    r.getOrders().get(0).setQuantity(new BigDecimal("1000000000000"));
    assertThrows(RuntimeException.class, () -> service.preflight(r, 9L));
    assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_work_order", Integer.class));
  }

  @Test
  void ambiguousApprovedProductCannotBeChosenThroughEitherEntryPoint() {
    when(definitions.selectApproved(1L))
        .thenReturn(
            List.of(
                new ApprovedProduct(1L, 2L, 3L, 4L, 10L, "PP-A", "本轮产品", "P-A"),
                new ApprovedProduct(1L, 2L, 3L, 4L, 12L, "PP-A", "本轮产品", "P-A")));
    assertThrows(RuntimeException.class, () -> service.approvedProducts(9L));
    var r = input("AMBIGUOUS");
    var preflight = service.preflight(r, 9L);
    assertFalse(preflight.ready());
    assertTrue(preflight.missing().stream().anyMatch(c -> c.code().equals("APPROVED_PRODUCT")));
    assertThrows(RuntimeException.class, () -> tx.execute(s -> service.prepare(r, 9L)));
    assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_work_order", Integer.class));
    assertEquals(
        0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_local_test_run_manifest", Integer.class));
  }

  @Test
  void tenantAndDisabledTestModeCannotPrepare() {
    var r = input("DISABLED");
    doThrow(new IllegalStateException("test mode disabled")).when(mode).requireEnabled(9L);
    assertThrows(IllegalStateException.class, () -> tx.execute(s -> service.prepare(r, 9L)));
    reset(mode);
    TenantContextHolder.setTenantId(2L);
    assertThrows(RuntimeException.class, () -> tx.execute(s -> service.prepare(r, 9L)));
  }
}
