package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Actual revision mapper insert against formal NOT NULL constraints in isolated H2.
 * Event/task reads and field-diff writes are boundary doubles; not a MySQL or full-transaction claim. */
class MesPqcRevisionTaskIdentityTest {
    private JdbcTemplate jdbc;
    private MesProProcessPoolEventMapper events;
    private MesProProcessPoolEventRevisionDiffMapper diffs;
    private MesPqcInspectionTaskMapper tasks;
    private MesProcessPoolEventRevisionServiceImpl service;
    private MesProProcessPoolEventDO event;
    private MesPqcInspectionTaskDO task;

    @BeforeEach
    void prepare() throws Exception {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:pqc_revision_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        String ddl = Files.readString(Path.of("../sql/mysql/20260730_mes_process_pool_event_revision.sql"));
        ddl = ddl.substring(ddl.indexOf("CREATE TABLE"), ddl.indexOf(";", ddl.indexOf("CREATE TABLE")) + 1);
        ddl = ddl.replace("b'0'", "0").replace("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci", "");
        jdbc.execute(ddl);
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        var factory = new MybatisSqlSessionFactoryBean();
        var global = new com.baomidou.mybatisplus.core.config.GlobalConfig();
        global.setDbConfig(new com.baomidou.mybatisplus.core.config.GlobalConfig.DbConfig()
                .setIdType(com.baomidou.mybatisplus.annotation.IdType.AUTO));
        global.setMetaObjectHandler(new cn.iocoder.yudao.framework.mybatis.core.handler.DefaultDBFieldHandler());
        com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils.setGlobalConfig(configuration, global);
        configuration.addMapper(MesProProcessPoolEventRevisionMapper.class);
        factory.setGlobalConfig(global);
        factory.setDataSource(dataSource);
        factory.setConfiguration(configuration);
        var session = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
        events = mock(MesProProcessPoolEventMapper.class);
        diffs = mock(MesProProcessPoolEventRevisionDiffMapper.class);
        tasks = mock(MesPqcInspectionTaskMapper.class);
        service = new MesProcessPoolEventRevisionServiceImpl(events,
                session.getMapper(MesProProcessPoolEventRevisionMapper.class), diffs,
                mock(MesProcessPoolFifoAllocationService.class), mock(MesProcessPoolSubmissionReviewMapper.class),
                mock(MesProBatchRecordExecutionSignatureService.class));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "nonconformanceReviewService", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.class));
        ReflectionTestUtils.setField(service, "pqcTaskMapper", tasks);
        event = MesProcessPoolEventRevisionServiceTest.event()
                .setEventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                .setQaProcessId(6101L).setRouteProcessId(null).setProcessId(null)
                .setFeedbackSourceType("MES_PQC_INSPECTION_TASK").setFeedbackSourceId(9101L)
                .setRecordbookSourceType("MES_PQC_INSPECTION_TASK").setRecordbookSourceId(9101L);
        event.setTenantId(1L);
        task = new MesPqcInspectionTaskDO().setId(9101L).setWorkOrderId(event.getWorkOrderId())
                .setRouteId(event.getRouteId()).setQaProcessId(6101L)
                .setRouteProcessId(5101L).setProcessId(6201L).setSubmittedEventId(event.getId());
        task.setTenantId(1L);
        when(events.selectByIdForUpdate(event.getId())).thenReturn(event);
        when(tasks.selectByIdForUpdate(task.getId())).thenAnswer(call -> task);
    }

    @org.junit.jupiter.api.AfterEach
    void clearTenant() {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();
    }

    @Test
    void persistsQaCorrectionUsingProductionIdentityOfTheFormalTask() {
        Long id = service.updatePqcInspectionRecord(MesProcessPoolEventRevisionServiceTest.updateReq());
        var saved = jdbc.queryForMap("SELECT event_id,route_process_id,process_id FROM mes_pro_process_pool_event_revision WHERE id=?", id);
        assertEquals(1001L, ((Number) saved.get("event_id")).longValue());
        assertEquals(5101L, ((Number) saved.get("route_process_id")).longValue());
        assertEquals(6201L, ((Number) saved.get("process_id")).longValue());
        assertNull(event.getRouteProcessId());
        assertNull(event.getProcessId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "tenant", "order", "route", "qa", "event", "production", "source"})
    void rejectsInvalidTaskIdentityBeforeRevisionPersistence(String defect) {
        // Legacy production fields must never override a broken formal PQC task association.
        event.setRouteProcessId(5001L).setProcessId(6001L);
        switch (defect) {
            case "missing" -> task = null;
            case "tenant" -> task.setTenantId(2L);
            case "order" -> task.setWorkOrderId(3002L);
            case "route" -> task.setRouteId(4002L);
            case "qa" -> task.setQaProcessId(6102L);
            case "event" -> task.setSubmittedEventId(1002L);
            case "production" -> task.setRouteProcessId(null);
            case "source" -> event.setRecordbookSourceId(9102L);
            default -> throw new IllegalArgumentException(defect);
        }
        var error = assertThrows(ServiceException.class,
                () -> service.updatePqcInspectionRecord(MesProcessPoolEventRevisionServiceTest.updateReq()));
        assertEquals(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), error.getCode());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mes_pro_process_pool_event_revision", Integer.class));
        verifyNoInteractions(diffs);
        verify(events, never()).updateById(any(MesProProcessPoolEventDO.class));
    }
}
