package cn.iocoder.yudao.module.mes.service.audit;

import cn.iocoder.yudao.framework.common.biz.system.logger.dto.OperateLogCreateReqDTO;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.system.service.logger.OperateLogService;
import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesVersionOperateLogServiceTest {
    private final OperateLogService logs = mock(OperateLogService.class);
    private final MesVersionOperateLogService service = new MesVersionOperateLogService();

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "operateLogService", logs);
    }

    @AfterEach
    void cleanup() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void routePublicationPreservesActualPublisherAndBothVersions() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mes/pro/route-version/publish");
        request.addHeader("User-Agent", "audit-test");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        service.route("发布", route(1L, "V1"), route(2L, "V2"), 19L);
        var log = captured();
        assertEquals("工艺流程", log.getType());
        assertEquals("发布", log.getSubType());
        assertEquals(19L, log.getUserId());
        assertEquals(9L, log.getBizId());
        assertTrue(log.getAction().contains("原版本：V1；目标版本：V2"));
        assertEquals("POST", log.getRequestMethod());
        assertEquals("/mes/pro/route-version/publish", log.getRequestUrl());
        assertEquals(1L, JSON.parseObject(log.getExtra()).getLong("beforeVersionId"));
        assertEquals(2L, JSON.parseObject(log.getExtra()).getLong("versionId"));
    }

    @Test
    void qaUpgradeUsesLoggedInUserAndProjectIdentity() {
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(31L);
            service.qa("升版", new MesQaInspectionRegulationDO().setId(7L).setDccProjectCodeId(8L)
                            .setRegulationCode("QA-01").setRegulationName("检验规程").setOwnerModule("MES_QA"),
                    new MesQaInspectionRegulationVersionDO().setId(4L).setVersionNo("A/0"),
                    new MesQaInspectionRegulationVersionDO().setId(5L).setVersionNo("A/1").setLifecycleStatus("DRAFT"));
        }
        var log = captured();
        assertEquals("QA检验规程", log.getType());
        assertEquals(31L, log.getUserId());
        assertEquals(7L, log.getBizId());
        assertTrue(log.getAction().contains("A/0；目标版本：A/1"));
        assertEquals(8L, JSON.parseObject(log.getExtra()).getLong("dccProjectCodeId"));
        assertEquals("DRAFT", JSON.parseObject(log.getExtra()).getString("lifecycleStatus"));
    }

    @Test
    void missingActorCannotProduceAnonymousSuccessLog() {
        assertThrows(IllegalStateException.class, () -> service.route("创建", null, route(1L, "V1"), null));
        verifyNoInteractions(logs);
    }

    @Test
    void auditStorageFailurePropagatesToBusinessTransaction() {
        doThrow(new IllegalStateException("storage failure")).when(logs).createOperateLog(any());
        var failure = assertThrows(IllegalStateException.class,
                () -> service.route("发布", route(1L, "V1"), route(2L, "V2"), 19L));
        assertEquals("storage failure", failure.getMessage());
    }

    @Test
    void versionAndLogRollBackTogetherAndCannotWriteOutsideTransaction() {
        var dataSource = new org.springframework.jdbc.datasource.DriverManagerDataSource(
                "jdbc:h2:mem:version_operate_log;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE version_event (kind VARCHAR(20))");
        var manager = new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource);
        var interceptor = new org.springframework.transaction.interceptor.TransactionInterceptor();
        interceptor.setTransactionManager(manager);
        interceptor.setTransactionAttributeSource(
                new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource());
        var factory = new org.springframework.aop.framework.ProxyFactory(service);
        factory.addAdvice(interceptor);
        var transactionalService = (MesVersionOperateLogService) factory.getProxy();
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,
                () -> transactionalService.route("发布", null, route(2L, "V2"), 19L));
        verifyNoInteractions(logs);

        doAnswer(call -> {
            jdbc.update("INSERT INTO version_event VALUES ('log')");
            throw new IllegalStateException("audit write failed");
        }).when(logs).createOperateLog(any());
        var transaction = new org.springframework.transaction.support.TransactionTemplate(manager);
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            jdbc.update("INSERT INTO version_event VALUES ('version')");
            transactionalService.route("发布", null, route(2L, "V2"), 19L);
        }));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM version_event", Integer.class));
        jdbc.execute("DROP TABLE version_event");
    }

    private OperateLogCreateReqDTO captured() {
        var captor = ArgumentCaptor.forClass(OperateLogCreateReqDTO.class);
        verify(logs).createOperateLog(captor.capture());
        return captor.getValue();
    }

    private MesProRouteVersionDO route(Long id, String version) {
        return MesProRouteVersionDO.builder().id(id).routeId(9L).versionNo(version)
                .routeSnapshotJson("{\"routeCode\":\"RT-1\",\"routeName\":\"工艺一\"}")
                .lifecycleStatus("ACTIVE").build();
    }
}
