package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Actual mapper persistence and FIFO writer, isolated H2; not an InnoDB isolation claim. */
class MesReportAllocationCyclePersistenceTest {
    @Test void oldFortySurvivesNewCycleTwentyThirtyTenZeroAndTwenty() throws Exception {
        var ds=new DriverManagerDataSource("jdbc:h2:mem:fifo_cycles_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_UPPER=false", "sa", "");
        var oldBeans=org.springframework.test.util.ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class,"beanFactory");
        var compatible=com.baomidou.mybatisplus.extension.spi.CompatibleHelper.getCompatibleSet();
        var oldContext=org.springframework.test.util.ReflectionTestUtils.getField(compatible.getClass(),"applicationContext");
        try(var keep=ds.getConnection();var context=new org.springframework.context.support.GenericApplicationContext()) {
            var jdbc=new JdbcTemplate(ds);TenantContextHolder.setTenantId(1L);
            for(var row:List.of(MesProcessPoolFifoAllocationLineDO.class,MesProProcessPoolQuantityFragmentDO.class,
                    MesProcessPoolReportAllocationDO.class,MesProWorkOrderDO.class)) createTable(jdbc,row);
            var config=new MybatisConfiguration();config.setMapUnderscoreToCamelCase(true);

            var factory=new MybatisSqlSessionFactoryBean();factory.setDataSource(ds);factory.setConfiguration(config);factory.setApplicationContext(context);
            factory.setGlobalConfig(new com.baomidou.mybatisplus.core.config.GlobalConfig().setMetaObjectHandler(new cn.iocoder.yudao.framework.mybatis.core.handler.DefaultDBFieldHandler()));
            var session=new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
            List.of(MesProcessPoolFifoAllocationLineMapper.class,MesProProcessPoolQuantityFragmentMapper.class,
                    MesProcessPoolReportAllocationMapper.class,MesProWorkOrderMapper.class).forEach(config::addMapper);
            context.getBeanFactory().registerSingleton("dataSource",ds);
            context.getBeanFactory().registerSingleton("sqlSessionFactory",session.getSqlSessionFactory());
            context.getBeanFactory().registerSingleton("fifoMapper",session.getMapper(MesProcessPoolFifoAllocationLineMapper.class));
            context.refresh();new cn.hutool.extra.spring.SpringUtil().postProcessBeanFactory(context.getBeanFactory());
            var lines=session.getMapper(MesProcessPoolFifoAllocationLineMapper.class);
            var fragments=session.getMapper(MesProProcessPoolQuantityFragmentMapper.class);
            var allocations=session.getMapper(MesProcessPoolReportAllocationMapper.class);
            var work=session.getMapper(MesProWorkOrderMapper.class);
            work.insert(MesProWorkOrderDO.builder().id(9001L).code("same-work").build());
            var event=MesProProcessPoolEventDO.builder().id(1001L).poolId(2001L).routeProcessId(5001L).processId(6001L).build();
            var fragment=MesProProcessPoolQuantityFragmentDO.builder().id(6101L).eventId(1001L).productionSubmitEventId(1001L)
                    .poolId(2001L).routeProcessId(5001L).processId(6001L).sourceQuantityType("OUTPUT")
                    .totalQuantity(new BigDecimal("100")).allocatedQuantity(BigDecimal.ZERO).availableQuantity(new BigDecimal("100")).build();
            fragments.insert(fragment);
            var service=new MesReportAllocationQuantityFragmentService(fragments,lines,work);
            var old=allocation(7101L,8101L,1,40);allocations.insert(old);
            service.rebuildForVersion(event,1,List.of(old));
            String oldRow=JsonUtils.toJsonString(lines.selectListBySourceEventIdForUpdate(1001L).get(0));
            String oldAllocation=JsonUtils.toJsonString(allocations.selectById(7101L));
            int version=1; Long previous=null;
            for(int amount:List.of(20,30,10,0,20)) {
                version++;
                if(previous!=null) assertEquals(1,allocations.supersedeCurrentRows(List.of(previous),version));
                List<MesProcessPoolReportAllocationDO> next=new ArrayList<>();
                if(amount>0) {var a=allocation(7100L+version,8102L,version,amount);allocations.insert(a);next.add(a);previous=a.getId();}
                else previous=null;
                service.rebuildPreservingAllocations(event,version,next,List.of(allocations.selectById(7101L)));
                var persisted=lines.selectListBySourceEventIdForUpdate(1001L);
                var retained=persisted.stream().filter(l->Objects.equals(l.getTargetActiveOrderId(),8101L)).toList();
                assertEquals(1,retained.size());assertEquals(oldRow,JsonUtils.toJsonString(retained.get(0)));
                assertEquals(oldAllocation,JsonUtils.toJsonString(allocations.selectById(7101L)));
                var current=persisted.stream().filter(l->Objects.equals(l.getTargetActiveOrderId(),8102L)).toList();
                assertEquals(amount==0?0:1,current.size());
                if(amount>0) {assertEquals(previous,current.get(0).getReportAllocationId());assertEquals(version,current.get(0).getReportAllocationVersion());}
                assertEquals(0,new BigDecimal(40+amount).compareTo(fragments.selectById(6101L).getAllocatedQuantity()));
                assertEquals(0,new BigDecimal(60-amount).compareTo(fragments.selectById(6101L).getAvailableQuantity()));
                assertEquals(amount==0?1:2,allocations.selectListByEventId(1001L).size());
            }
        } finally {TenantContextHolder.clear();
            org.springframework.test.util.ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class,"beanFactory",oldBeans);
            compatible.setContext(oldContext);
        }
    }
    @Test void sharedGuardReadsEachCyclesPersistedFrozenSnapshot() throws Exception {
        var ds=new DriverManagerDataSource("jdbc:h2:mem:shared_cycles_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_UPPER=false","sa","");
        try(var keep=ds.getConnection()) {
            var jdbc=new JdbcTemplate(ds);TenantContextHolder.setTenantId(1L);
            for(var row:List.of(MesProcessPoolReportAllocationDO.class,MesProcessPoolActiveOrderDO.class,MesProcessPoolActiveOrderProcessSnapshotDO.class))createTable(jdbc,row);
            var config=new MybatisConfiguration();config.setMapUnderscoreToCamelCase(true);
            var factory=new MybatisSqlSessionFactoryBean();factory.setDataSource(ds);factory.setConfiguration(config);
            var sessions=new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
            List.of(MesProcessPoolReportAllocationMapper.class,MesProcessPoolActiveOrderMapper.class,MesProcessPoolActiveOrderProcessSnapshotMapper.class).forEach(config::addMapper);
            var orders=sessions.getMapper(MesProcessPoolActiveOrderMapper.class);var snapshots=sessions.getMapper(MesProcessPoolActiveOrderProcessSnapshotMapper.class);
            for(long id:List.of(8101L,8102L)) {
                var order=MesProcessPoolActiveOrderDO.builder().id(id).workOrderId(id+100).routeId(id+200).routeVersionId(id+300)
                        .activeStatus("ACTIVE").businessStatus("ACTIVE").build();order.setTenantId(1L);orders.insert(order);
                var snapshot=MesProcessPoolActiveOrderProcessSnapshotDO.builder().id(id).activeOrderId(id).workOrderId(id+100)
                        .routeId(id+200).routeVersionId(id+300).routeProcessId(id+400).processId(6001L).build();snapshot.setTenantId(1L);snapshots.insert(snapshot);
            }
            var allocation=allocation(7101L,8102L,1,20).setWorkOrderId(8202L).setRouteProcessId(8502L);allocation.setTenantId(1L);
            sessions.getMapper(MesProcessPoolReportAllocationMapper.class).insert(allocation);
            var event=MesProProcessPoolEventDO.builder().id(1001L).workOrderId(8201L).routeId(8301L).routeProcessId(8501L).processId(6001L).rawPayload("{\"activeOrderId\":8101}").build();event.setTenantId(1L);
            var guard=new cn.iocoder.yudao.module.mes.service.pro.processpool.MesSharedProductionReportCorrectionGuard();
            org.springframework.test.util.ReflectionTestUtils.setField(guard,"orders",orders);org.springframework.test.util.ReflectionTestUtils.setField(guard,"snapshots",snapshots);
            org.springframework.test.util.ReflectionTestUtils.setField(guard,"allocations",sessions.getMapper(MesProcessPoolReportAllocationMapper.class));
            org.springframework.test.util.ReflectionTestUtils.setField(guard,"receipts",org.mockito.Mockito.mock(MesProcessPoolActiveOrderCompletionReceiptMapper.class));
            org.springframework.test.util.ReflectionTestUtils.setField(guard,"releases",org.mockito.Mockito.mock(MesReportAllocationReleaseStateService.class));
            org.springframework.test.util.ReflectionTestUtils.setField(guard,"freezes",org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.class));
            assertDoesNotThrow(()->guard.assertEditable(event));
            for(String mismatch:List.of("active_order_id=999","work_order_id=999","route_id=999","route_version_id=999","route_process_id=999","process_id=999","tenant_id=2")) {
                jdbc.update("UPDATE mes_pro_process_pool_active_order_process_snapshot SET "+mismatch+" WHERE id=8102");
                assertThrows(IllegalStateException.class,()->guard.assertEditable(event),mismatch);
                jdbc.update("UPDATE mes_pro_process_pool_active_order_process_snapshot SET active_order_id=8102,work_order_id=8202,route_id=8302,route_version_id=8402,route_process_id=8502,process_id=6001,tenant_id=1 WHERE id=8102");
            }
        } finally {TenantContextHolder.clear();}
    }

    private static MesProcessPoolReportAllocationDO allocation(Long id,Long cycle,int version,int quantity) {
        return MesProcessPoolReportAllocationDO.builder().id(id).eventId(1001L).activeOrderId(cycle).workOrderId(9001L)
                .routeProcessId(5001L).processId(6001L).createdVersion(version).allocatedQuantity(new BigDecimal(quantity)).lifecycleStatus("CURRENT").build();
    }
    private static void createTable(JdbcTemplate jdbc,Class<?> row) {
        List<String> columns=new ArrayList<>();
        for(Class<?> type=row;type!=Object.class;type=type.getSuperclass()) for(var field:type.getDeclaredFields()) {
            var mapping=field.getAnnotation(TableField.class);
            if(Modifier.isStatic(field.getModifiers()) || (mapping!=null&&!mapping.exist()))continue;
            String name=mapping!=null&&!mapping.value().isBlank()?mapping.value():field.getName().replaceAll("([a-z0-9])([A-Z])","$1_$2").toLowerCase(Locale.ROOT);
            String sqlType=field.getType()==Long.class?"BIGINT":field.getType()==Integer.class?"INT":field.getType()==Boolean.class?"BOOLEAN DEFAULT FALSE":field.getType()==BigDecimal.class?"DECIMAL(24,8)":field.getType()==LocalDateTime.class?"TIMESTAMP":"VARCHAR(10000)";
            columns.add("`"+name+"` "+(name.equals("id")?"BIGINT AUTO_INCREMENT PRIMARY KEY":sqlType));
        }
        jdbc.execute("CREATE TABLE "+row.getAnnotation(TableName.class).value()+"("+String.join(",",columns)+")");
    }
}
