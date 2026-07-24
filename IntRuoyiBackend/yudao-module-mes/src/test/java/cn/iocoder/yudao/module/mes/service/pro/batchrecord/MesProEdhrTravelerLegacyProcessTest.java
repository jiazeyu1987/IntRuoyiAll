package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerActivateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerGenerateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrTravelerEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrTravelerInstanceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrTravelerTemplateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrTravelerEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrTravelerInstanceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrTravelerTemplateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.sn.MesWmSnMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrTravelerLegacyProcessTest {

    private static final Long TEMPLATE_ID = 10L;
    private static final Long BATCH_EXECUTION_ID = 30L;
    private static final Long ROUTE_ID = 20L;
    private static final Long HISTORICAL_ROUTE_PROCESS_ID = 99L;
    private static final Long CURRENT_ROUTE_PROCESS_ID = 100L;
    private static final Long HISTORICAL_PROCESS_ID = 1999L;
    private static final Long CURRENT_PROCESS_ID = 2000L;

    private MesProEdhrTravelerServiceImpl service;

    @Mock
    private MesProEdhrTravelerTemplateMapper templateMapper;
    @Mock
    private MesProEdhrTravelerInstanceMapper instanceMapper;
    @Mock
    private MesProEdhrTravelerEventMapper eventMapper;
    @Mock
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock
    private MesProRouteProcessService routeProcessService;
    @Mock
    private MesProProcessMapper processMapper;
    @Mock
    private MesWmSnMapper snMapper;

    @BeforeEach
    void setUp() {
        service = new MesProEdhrTravelerServiceImpl();
        ReflectionTestUtils.setField(service, "templateMapper", templateMapper);
        ReflectionTestUtils.setField(service, "instanceMapper", instanceMapper);
        ReflectionTestUtils.setField(service, "eventMapper", eventMapper);
        ReflectionTestUtils.setField(service, "batchExecutionMapper", batchExecutionMapper);
        ReflectionTestUtils.setField(service, "routeProcessService", routeProcessService);
        ReflectionTestUtils.setField(service, "processMapper", processMapper);
        ReflectionTestUtils.setField(service, "snMapper", snMapper);
    }

    @Test
    void generate_shouldKeepFrozenHistoricalRouteProcessAndTemplateProcessIdentity() {
        when(templateMapper.selectById(TEMPLATE_ID)).thenReturn(activeTemplate(HISTORICAL_PROCESS_ID));
        when(batchExecutionMapper.selectById(BATCH_EXECUTION_ID)).thenReturn(batchExecution());
        when(routeProcessService.resolveFrozenRouteProcess(HISTORICAL_ROUTE_PROCESS_ID, ROUTE_ID, null))
                .thenReturn(frozenRouteProcess());
        when(processMapper.selectById(HISTORICAL_PROCESS_ID)).thenReturn(historicalProcess());
        when(instanceMapper.selectByBusinessKeyHash(anyString())).thenReturn(null);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(501L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");

            service.generate(new MesProEdhrTravelerGenerateReqVO()
                    .setTemplateId(TEMPLATE_ID)
                    .setBatchExecutionId(BATCH_EXECUTION_ID)
                    .setRouteProcessId(HISTORICAL_ROUTE_PROCESS_ID)
                    .setRequestId("REQ-LEGACY"));
        }

        ArgumentCaptor<MesProEdhrTravelerInstanceDO> travelerCaptor =
                ArgumentCaptor.forClass(MesProEdhrTravelerInstanceDO.class);
        verify(instanceMapper).insert(travelerCaptor.capture());
        assertEquals(HISTORICAL_ROUTE_PROCESS_ID, travelerCaptor.getValue().getRouteProcessId());
        assertEquals(HISTORICAL_PROCESS_ID, travelerCaptor.getValue().getProcessId());
        verify(routeProcessService, never()).resolveCurrentRouteProcess(HISTORICAL_ROUTE_PROCESS_ID, ROUTE_ID, null);

        ArgumentCaptor<MesProEdhrTravelerEventDO> eventCaptor =
                ArgumentCaptor.forClass(MesProEdhrTravelerEventDO.class);
        verify(eventMapper).insert(eventCaptor.capture());
        assertEquals("SUCCESS", eventCaptor.getValue().getResultStatus());
    }

    @Test
    void activateTemplate_shouldRejectActiveTemplateWithHistoricalSameProcessIdentity() {
        MesProEdhrTravelerTemplateDO template = draftTemplate(CURRENT_PROCESS_ID);
        MesProEdhrTravelerTemplateDO historicalActive = activeTemplate(HISTORICAL_PROCESS_ID).setId(9L);
        when(templateMapper.selectById(TEMPLATE_ID)).thenReturn(template);
        when(routeProcessService.resolveCurrentRouteProcess(null, ROUTE_ID, CURRENT_PROCESS_ID))
                .thenReturn(currentRouteProcess());
        when(templateMapper.selectActiveTemplatesByProductAndRoute("P-001", ROUTE_ID))
                .thenReturn(List.of(historicalActive));
        when(routeProcessService.getProcessIdentityMap(List.of(CURRENT_PROCESS_ID)))
                .thenReturn(Map.of(HISTORICAL_PROCESS_ID, CURRENT_PROCESS_ID, CURRENT_PROCESS_ID, CURRENT_PROCESS_ID));

        assertThrows(ServiceException.class, () -> service.activateTemplate(
                new MesProEdhrTravelerActivateReqVO().setId(TEMPLATE_ID)));
    }

    private MesProEdhrTravelerTemplateDO activeTemplate(Long applicableProcessId) {
        return draftTemplate(applicableProcessId).setStatus("ACTIVE");
    }

    private MesProEdhrTravelerTemplateDO draftTemplate(Long applicableProcessId) {
        return new MesProEdhrTravelerTemplateDO()
                .setId(TEMPLATE_ID)
                .setTemplateCode("TL-001")
                .setTemplateName("Traveler")
                .setTemplateVersion("V1")
                .setStatus("DRAFT")
                .setApplicableProductCode("P-001")
                .setApplicableRouteId(ROUTE_ID)
                .setApplicableProcessId(applicableProcessId);
    }

    private MesProEdhrBatchExecutionDO batchExecution() {
        return new MesProEdhrBatchExecutionDO()
                .setId(BATCH_EXECUTION_ID)
                .setBatchExecutionCode("BE-001")
                .setWorkOrderId(700L)
                .setWorkOrderCode("MO-001")
                .setBatchCode("B-001")
                .setProductId(800L)
                .setProductCode("P-001")
                .setProductName("Product")
                .setRouteId(ROUTE_ID)
                .setRouteCode("R-001")
                .setRouteName("Route");
    }

    private MesProRouteProcessDO currentRouteProcess() {
        return MesProRouteProcessDO.builder()
                .id(CURRENT_ROUTE_PROCESS_ID)
                .routeId(ROUTE_ID)
                .processId(CURRENT_PROCESS_ID)
                .sort(1)
                .build();
    }

    private MesProRouteProcessDO frozenRouteProcess() {
        return MesProRouteProcessDO.builder()
                .id(HISTORICAL_ROUTE_PROCESS_ID)
                .routeId(ROUTE_ID)
                .processId(HISTORICAL_PROCESS_ID)
                .sort(1)
                .build();
    }

    private MesProProcessDO currentProcess() {
        return MesProProcessDO.builder()
                .id(CURRENT_PROCESS_ID)
                .code("P-A")
                .name("Process A")
                .build();
    }

    private MesProProcessDO historicalProcess() {
        return MesProProcessDO.builder()
                .id(HISTORICAL_PROCESS_ID)
                .code("P-H")
                .name("Historical Process")
                .build();
    }
}
