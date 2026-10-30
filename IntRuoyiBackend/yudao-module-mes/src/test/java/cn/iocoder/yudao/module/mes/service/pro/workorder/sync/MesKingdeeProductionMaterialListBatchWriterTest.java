package cn.iocoder.yudao.module.mes.service.pro.workorder.sync;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncRunMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncWatermarkMapper;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncRunStatusEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionMaterialList;
import cn.iocoder.yudao.module.erp.service.sync.runtime.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.*;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Real MyBatis batch SQL and Spring physical transactions against an isolated H2 database. */
class MesKingdeeProductionMaterialListBatchWriterTest {
    private JdbcTemplate jdbc;
    private GenericApplicationContext application;
    private DataSourceTransactionManager transactions;
    private MesKingdeeProductionMaterialListBatchWriter writer;
    private ErpKingdeeSyncRuntimeTransactionService runtime;
    private MesProWorkOrderMapper orders;
    private MesProWorkOrderBomMapper boms;
    private MesMdItemMapper items;
    private MesKingdeeProductionMaterialListMapper materials;

    @BeforeEach
    void setup() throws Exception {
        // Initialize the actual application parser-cache policy even when this test runs on its own.
        Class.forName("cn.iocoder.yudao.framework.mybatis.config.YudaoMybatisAutoConfiguration");
        TenantContextHolder.setTenantId(165L);
        var source = new DriverManagerDataSource("jdbc:h2:mem:pml_" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
        // Keep one anchor connection alive; closing the context destroys only this test database.
        application = new GenericApplicationContext();
        var anchor = source.getConnection();
        application.getDefaultListableBeanFactory().registerDisposableBean("anchor", anchor::close);
        application.registerBean(DataSource.class, () -> source);
        jdbc = new JdbcTemplate(source);
        loadSchema("20260613_mes_kingdee_production_material_list.sql");
        loadSchema("20260612_erp_kingdee_sync_runtime.sql");

        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(org.apache.ibatis.logging.nologging.NoLoggingImpl.class);
        var global = new GlobalConfig();
        global.setDbConfig(new GlobalConfig.DbConfig().setIdType(IdType.AUTO));
        global.setMetaObjectHandler(new cn.iocoder.yudao.framework.mybatis.core.handler.DefaultDBFieldHandler());
        var plugins = new MybatisPlusInterceptor();
        plugins.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override public Expression getTenantId() {
                return new LongValue(TenantContextHolder.getRequiredTenantId());
            }
        }));
        var bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(source);
        bean.setConfiguration(configuration);
        bean.setGlobalConfig(global);
        bean.setTransactionFactory(new SpringManagedTransactionFactory());
        bean.setPlugins(plugins);
        SqlSessionFactory factory = Objects.requireNonNull(bean.getObject());
        factory.getConfiguration().addMapper(MesKingdeeProductionMaterialListMapper.class);
        factory.getConfiguration().addMapper(ErpKingdeeSyncRunMapper.class);
        factory.getConfiguration().addMapper(ErpKingdeeSyncWatermarkMapper.class);
        application.registerBean(SqlSessionFactory.class, () -> factory);
        application.refresh();
        var sessions = new SqlSessionTemplate(factory);
        materials = sessions.getMapper(MesKingdeeProductionMaterialListMapper.class);
        transactions = new DataSourceTransactionManager(source);
        runtime = proxy(new ErpKingdeeSyncRuntimeTransactionService(
                sessions.getMapper(ErpKingdeeSyncWatermarkMapper.class), sessions.getMapper(ErpKingdeeSyncRunMapper.class)));
        orders = mock(MesProWorkOrderMapper.class);
        boms = mock(MesProWorkOrderBomMapper.class);
        items = mock(MesMdItemMapper.class);
        writer = proxy(new MesKingdeeProductionMaterialListBatchWriter(materials, orders, boms, items, runtime));
        jdbc.update("INSERT INTO erp_kingdee_sync_run (id,sync_type,trigger_type,status,started_at,tenant_id) VALUES (91,?,'FULL',?,CURRENT_TIMESTAMP,165)",
                ErpKingdeeSyncTypeEnum.PRODUCTION_MATERIAL_LIST.getType(), ErpKingdeeSyncRunStatusEnum.RUNNING.getStatus());
    }

    @AfterEach
    void close() {
        TenantContextHolder.clear();
        if (application != null) application.close();
    }

    @Test
    void writesThousandRowsThenRerunSkipsWithoutDuplicatesOrPerRowLookups() {
        List<ErpKingdeeProductionMaterialList> page = IntStream.range(0, 1000).mapToObj(this::row).toList();
        assertEquals(1000, writer.writePage(91L, page).getCreatedCount());
        assertEquals(1000, writer.writePage(91L, page).getSkippedCount());
        assertEquals(1000, count());
        assertEquals(1000, progress("created_count"));
        assertEquals(1000, progress("skipped_count"));
        verify(orders, times(2)).selectListByCodes(any());
        verify(items).selectListByCodes(any());
        verify(items, never()).selectByCode(any());
        verify(orders, never()).selectByIdForUpdate(any());
    }

    @Test
    void laterBatchFailureRollsBackItsFlushedRowsButRetainsEarlierPageAndProgress() {
        writer.writePage(91L, List.of(row(1)));
        var invalidPage = new ArrayList<>(IntStream.range(2, 205).mapToObj(this::row).toList());
        invalidPage.add(ErpKingdeeProductionMaterialList.builder().formId("PRD_PPBOM").billNo("invalid")
                .productCode("P").productionOrderNo("MO").productionOrderLineNo(1).childMaterialCode("C")
                .requiredQuantity(BigDecimal.ONE).build()); // required child name missing, after first JDBC flush
        assertThrows(RuntimeException.class, () -> writer.writePage(91L, invalidPage));
        assertEquals(1, count());
        assertEquals(1, progress("created_count"));
        runtime.fail(new ErpKingdeeSyncExecution(91L, ErpKingdeeSyncContext.builder()
                .windowEnd(LocalDateTime.now()).build()), new IllegalStateException("page failed"));
        assertEquals(1, progress("created_count"));
        assertEquals(ErpKingdeeSyncRunStatusEnum.FAILED.getStatus(), progress("status"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM erp_kingdee_sync_watermark", Integer.class));
    }

    @Test
    void missingOrOtherTenantProgressRecordRollsBackBusinessRows() {
        assertThrows(IllegalStateException.class, () -> writer.writePage(999L, List.of(row(1))));
        assertEquals(0, count());
        TenantUtils.execute(1L, () -> assertThrows(IllegalStateException.class,
                () -> writer.writePage(91L, List.of(row(1)))));
        assertEquals(0, count());
        assertEquals(0, progress("created_count"));
    }

    @Test
    void pageCommitsIndependentlyOfAnyCallerTransaction() {
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
            writer.writePage(91L, List.of(row(1)));
            throw new IllegalStateException("caller failed after commit");
        }));
        assertEquals(1, count());
        assertEquals(1, progress("created_count"));
    }

    @Test
    void duplicateRowsWithinPageAreInsertedOnceAndExistingColumnsAreBackfilled() {
        var source = row(1);
        assertEquals(1, writer.writePage(91L, List.of(source, source)).getCreatedCount());
        jdbc.update("UPDATE mes_kingdee_production_material_list SET drawing_number=NULL,demand_time=NULL");
        assertEquals(1, writer.writePage(91L, List.of(source)).getUpdatedCount());
        var saved = materials.selectBySourceLine("B1", "MO", 1, "C");
        assertEquals("drawing", saved.getDrawingNumber());
        assertEquals(source.getDemandTime(), saved.getDemandTime());
        assertEquals(1, count());
        assertEquals(1, progress("updated_count"));
    }

    @Test
    void routesAndBatchesEachTenantAndRestoresProgressTenant() {
        var first = order(501L, 1L, 20L);
        var second = order(502L, 122L, 120L);
        when(orders.selectListByCodes(any())).thenReturn(List.of(first, second));
        when(orders.selectListByIdsForUpdate(any())).thenAnswer(call ->
                List.of(TenantContextHolder.getTenantId() == 1L ? first : second));
        when(items.selectListByCodes(any())).thenAnswer(call -> List.of(new MesMdItemDO().setId(
                TenantContextHolder.getTenantId() == 1L ? 30L : 130L).setCode("C")));
        when(boms.selectListByWorkOrderIdsAndItemIds(any(), any())).thenAnswer(call ->
                TenantContextHolder.getTenantId() == 1L
                        ? List.of(new MesProWorkOrderBomDO().setId(601L).setWorkOrderId(501L).setItemId(30L))
                        : List.of(new MesProWorkOrderBomDO().setId(602L).setWorkOrderId(502L).setItemId(130L)));
        assertEquals(2, writer.writePage(91L, List.of(row(1))).getCreatedCount());
        assertEquals(165L, TenantContextHolder.getTenantId());
        assertEquals(2, progress("created_count"));
        assertEquals(0, materials.selectCount());
        TenantUtils.execute(1L, () -> {
            var saved = materials.selectBySourceLine("B1", "MO", 1, "C");
            assertEquals(501L, saved.getWorkOrderId());
            assertEquals(601L, saved.getWorkOrderBomId());
            assertEquals(20L, saved.getProductId());
            assertEquals(30L, saved.getChildMaterialId());
        });
        TenantUtils.execute(122L, () -> {
            var saved = materials.selectBySourceLine("B1", "MO", 1, "C");
            assertEquals(502L, saved.getWorkOrderId());
            assertEquals(602L, saved.getWorkOrderBomId());
            assertEquals(120L, saved.getProductId());
            assertEquals(130L, saved.getChildMaterialId());
        });
    }

    @Test
    void changedWorkOrderAbortsWholePage() {
        when(orders.selectListByCodes(any())).thenReturn(List.of(order(501L, 1L, 20L)));
        assertThrows(IllegalStateException.class, () -> writer.writePage(91L, List.of(row(1))));
        assertEquals(165L, TenantContextHolder.getTenantId());
        assertEquals(0, count());
        assertEquals(0, progress("created_count"));
    }

    @Test
    void progressCannotBeCommittedOutsidePageTransaction() {
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class, () ->
                runtime.recordProductionMaterialListProgress(91L, ErpKingdeeSyncRunResult.success(null, 1, 0, 0, 0)));
        assertEquals(0, progress("created_count"));
    }

    private int count() { return jdbc.queryForObject("SELECT COUNT(*) FROM mes_kingdee_production_material_list", Integer.class); }
    private int progress(String column) { return jdbc.queryForObject("SELECT " + column + " FROM erp_kingdee_sync_run WHERE id=91", Integer.class); }
    @SuppressWarnings("unchecked")
    private <T> T proxy(T target) {
        var factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        return (T) factory.getProxy();
    }
    private void loadSchema(String name) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("IntRuoyiBackend/sql/mysql").resolve(name))) root = root.getParent();
        assertNotNull(root, "repository SQL migration must be available");
        String sql = Files.readString(root.resolve("IntRuoyiBackend/sql/mysql").resolve(name))
                .replaceAll("(?m)^--.*", "")
                .replaceAll("(?i)CHARACTER SET(?:\\s*=)?\\s*utf8mb4", "")
                .replaceAll("(?i)COLLATE(?:\\s*=)?\\s*utf8mb4_unicode_ci", "")
                .replaceAll("(?i)USING BTREE", "")
                .replace("b'0'", "0")
                .replaceAll("(?i)ENGINE\\s*=\\s*InnoDB", "")
                .replaceAll("(?i)DEFAULT CHARSET=utf8mb4", "");
        for (String statement : sql.split(";")) if (!statement.isBlank()) jdbc.execute(statement);
    }
    private MesProWorkOrderDO order(Long id, Long tenant, Long product) {
        var result = new MesProWorkOrderDO().setId(id).setCode("MO").setProductId(product);
        result.setTenantId(tenant);
        return result;
    }
    private ErpKingdeeProductionMaterialList row(int id) {
        return ErpKingdeeProductionMaterialList.builder().formId("PRD_PPBOM").billNo("B" + id)
                .entryId(String.valueOf(id)).productCode("P").productionOrderNo("MO").productionOrderLineNo(1)
                .childMaterialCode("C").childMaterialName("child").requiredQuantity(BigDecimal.ONE)
                .drawingNumber("drawing").demandTime(LocalDateTime.of(2026, 10, 29, 0, 0)).rawPayload("[]").build();
    }
}
