package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteFlowBatchRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteFlowBatchRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteFlowConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteFlowFormBindingSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteFlowProcessConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteFlowProcessConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteStartProductionLeaderProductionLineRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProRouteFlowConfigTypeEnum;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionGateService;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.math.BigDecimal;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_TYPE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_BATCH_REPORT_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_FILLER_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_PUBLISHED_VERSION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_GLOBAL_FORM_GROUP_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_GLOBAL_FORM_GROUP_INCOMPLETE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_GLOBAL_FORM_GROUP_INCONSISTENT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteFlowConfigServiceImplTest {

    @InjectMocks
    private MesProRouteFlowConfigServiceImpl service;

    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProRouteProcessService routeProcessService;
    @Mock
    private MesProProcessMapper processMapper;
    @Mock
    private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Mock
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Mock
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Mock
    private MesProBatchRecordReportMapper batchRecordReportMapper;
    @Mock
    private FormTemplateVersionMapper formTemplateVersionMapper;
    @Mock
    private MesProEdhrPermissionGateService permissionGateService;
    @Mock
    private MesProRouteCandidateConfigService routeCandidateConfigService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private RoleApi roleApi;

    @BeforeEach
    void stubActiveRouteVersion() {
        lenient().when(routeVersionMapper.selectActiveByRouteId(10L)).thenReturn(null);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void permissionGateService_shouldBeSpringInjected() throws NoSuchFieldException {
        Field field = MesProRouteFlowConfigServiceImpl.class.getDeclaredField("permissionGateService");

        assertTrue(field.isAnnotationPresent(Resource.class));
    }

    @Test
    void routeFlowProcessQueryMethods_shouldNotBeResourceInjectionMethods() throws NoSuchMethodException {
        Method method = MesProRouteFlowConfigServiceImpl.class.getDeclaredMethod(
                "getRouteFlowProcessConfigList", Long.class, String.class);

        assertFalse(method.isAnnotationPresent(Resource.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void parseCandidateRouteProcesses_shouldUseFormalWorkstationBindingField() throws Exception {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(9901L)
                .routeId(10L)
                .build();
        JSONObject flowGraph = JSON.parseObject("""
                {
                  "nodes": [
                    {
                      "routeProcessId": 100,
                      "processId": 1000,
                      "routeProcessWorkstationId": 980010,
                      "workstationId": 922757,
                      "sort": 1,
                      "keyFlag": true,
                      "checkFlag": false
                    }
                  ]
                }
                """);
        Method method = MesProRouteFlowConfigServiceImpl.class.getDeclaredMethod(
                "parseCandidateRouteProcesses", MesProRouteVersionDO.class, JSONObject.class);
        method.setAccessible(true);

        List<MesProRouteProcessDO> result =
                (List<MesProRouteProcessDO>) method.invoke(service, candidate, flowGraph);

        assertEquals(1, result.size());
        assertEquals(980010L, result.get(0).getWorkstationId());
    }

    @Test
    void getRouteStartProductionLeaderProductionLines_shouldUseCurrentRouteAsResponsibleScope() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("RT-10").name("压力泵路线").build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(100L).routeId(10L).processId(1000L).sort(1).build();
        MesProProcessDO process = MesProProcessDO.builder().id(1000L).code("P1000").name("粗洗").build();
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(routeProcess));
        doReturn(List.of(process)).when(processMapper).selectBatchIds(anyCollection());

        List<MesProRouteStartProductionLeaderProductionLineRespVO> result =
                service.getRouteStartProductionLeaderProductionLines(10L, null);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getProductionLineId());
        assertEquals("RT-10", result.get(0).getProductionLineCode());
        assertEquals("压力泵路线", result.get(0).getProductionLineName());
        assertEquals(List.of(100L), result.get(0).getRouteProcessIds());
        assertEquals(List.of("粗洗"), result.get(0).getProcessNames());
    }

    @Test
    void getRouteFlowProcessConfigList_shouldUseHistoricalConfigForCurrentRouteProcess() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteProcessDO currentRouteProcess = MesProRouteProcessDO.builder()
                .id(100L).routeId(10L).processId(1000L).sort(1).build();
        MesProRouteFlowConfigDO flowConfig = MesProRouteFlowConfigDO.builder()
                .id(800L).routeId(10L).useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                .enabled(Boolean.TRUE).build();
        MesProRouteFlowProcessConfigDO historicalConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(901L)
                .routeFlowConfigId(800L)
                .routeId(10L)
                .routeProcessId(99L)
                .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                .enabled(Boolean.TRUE)
                .productionQuantityFactor(BigDecimal.ONE)
                .build();
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(currentRouteProcess));
        doReturn(List.of(MesProProcessDO.builder().id(1000L).code("P1000").name("工序").build()))
                .when(processMapper).selectBatchIds(anyCollection());
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(flowConfig);
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(List.of(historicalConfig));
        when(routeProcessService.resolveCurrentRouteProcess(99L, 10L, null))
                .thenReturn(currentRouteProcess);

        List<MesProRouteFlowProcessConfigRespVO> result =
                service.getRouteFlowProcessConfigList(10L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType());

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getRouteProcessId());
        assertEquals(Boolean.TRUE, result.get(0).getEnabled());
    }

    @Test
    void getRouteFlowProcessConfigList_shouldReadActiveVersionSnapshotWhenRouteVersionIdIsActive() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteVersionDO publishedVersion = MesProRouteVersionDO.builder()
                .id(100L)
                .routeId(10L)
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .routeSnapshotJson("""
                        {
                          "routeId": 10,
                          "configSnapshots": {
                            "flowGraph": {
                              "nodes": [
                                {"routeProcessId": 100, "processId": 1000, "sort": 1, "keyFlag": true, "checkFlag": false}
                              ]
                            },
                            "batchUseConfigs": [
                              {"routeProcessId": 100, "enabled": true, "productionQuantityFactor": 1}
                            ]
                          }
                        }
                        """)
                .build();
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(100L)).thenReturn(publishedVersion);
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(MesProRouteProcessDO.builder()
                .id(100L).routeId(10L).processId(1000L).sort(1).build()));
        doReturn(List.of(MesProProcessDO.builder().id(1000L).code("P1000").name("生效工序").build()))
                .when(processMapper).selectBatchIds(anyCollection());

        List<MesProRouteFlowProcessConfigRespVO> result =
                service.getRouteFlowProcessConfigList(
                        10L, MesProRouteFlowConfigTypeEnum.BATCH.getType(), 100L);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getRouteProcessId());
        assertEquals("P1000", result.get(0).getProcessCode());
        assertEquals("生效工序", result.get(0).getProcessName());
        verify(routeFlowProcessBatchRecordMapper, never()).selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType());
        verify(routeCandidateConfigService, never()).saveConfigSnapshot(any(), any(), any());
    }

    @Test
    void getRouteFlowProcessConfigList_shouldProjectActiveVersionSnapshotBindingsOntoCurrentRouteProcesses() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteVersionDO activeVersion = MesProRouteVersionDO.builder()
                .id(310L)
                .routeId(10L)
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .routeSnapshotJson("""
                        {
                          "routeId": 10,
                          "configSnapshots": {
                            "flowGraph": {
                              "nodes": [
                                {"routeProcessId": 3100, "processId": 1000, "sort": 1, "keyFlag": true, "checkFlag": false}
                              ]
                            },
                            "batchUseConfigs": [
                              {
                                "routeProcessId": 3100,
                                "enabled": true,
                                "productionQuantityFactor": 1,
                                "formBindings": [
                                  {
                                    "formBindingKey": "FB-ACTIVE-SNAPSHOT",
                                    "formTemplateId": 2001,
                                    "formTemplateName": "已发布生产记录表",
                                    "lastPublishedTemplateVersionId": 3001,
                                    "lastPublishedTemplateVersionNo": "V1",
                                    "candidateSourceType": "USERS",
                                    "candidateSourceIds": [9001],
                                    "candidateSourceNames": ["张三"],
                                    "reportSort": 1
                                  }
                                ]
                              }
                            ]
                          }
                        }
                        """)
                .build();
        MesProRouteProcessDO currentRouteProcess = MesProRouteProcessDO.builder()
                .id(2000L).routeId(10L).processId(1000L).sort(1).build();
        MesProRouteFlowProcessConfigDO currentConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(901L)
                .routeId(10L)
                .routeProcessId(2000L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .enabled(Boolean.TRUE)
                .build();
        MesProRouteFlowProcessBatchRecordDO oldCurrentBinding = MesProRouteFlowProcessBatchRecordDO.builder()
                .routeFlowProcessConfigId(901L)
                .routeId(10L)
                .routeProcessId(2000L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .formBindingKey("FB-LIVE-OLD")
                .formTemplateId(2002L)
                .formTemplateNameSnapshot("实时旧绑定")
                .lastPublishedTemplateVersionId(3002L)
                .lastPublishedTemplateVersionNo("V1")
                .recordCategory("BATCH_RECORD")
                .validationProfile("CONTROLLED_BATCH")
                .candidateSourceType("USERS")
                .candidateSourceIds("9002")
                .candidateSourceNames("[\"李四\"]")
                .reportSort(1)
                .build();
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectActiveByRouteId(10L)).thenReturn(activeVersion);
        when(routeVersionMapper.selectById(310L)).thenReturn(activeVersion);
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(currentRouteProcess));
        doReturn(List.of(MesProProcessDO.builder().id(1000L).code("P1000").name("已发布快照工序").build()))
                .when(processMapper).selectBatchIds(anyCollection());
        lenient().when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(List.of(currentConfig));
        lenient().when(routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(List.of(oldCurrentBinding));

        List<MesProRouteFlowProcessConfigRespVO> omittedVersionResult =
                service.getRouteFlowProcessConfigList(10L, MesProRouteFlowConfigTypeEnum.BATCH.getType());
        List<MesProRouteFlowProcessConfigRespVO> explicitActiveVersionResult =
                service.getRouteFlowProcessConfigList(10L, MesProRouteFlowConfigTypeEnum.BATCH.getType(), 310L);

        for (List<MesProRouteFlowProcessConfigRespVO> result : List.of(
                omittedVersionResult, explicitActiveVersionResult)) {
            assertEquals(1, result.size());
            assertEquals(2000L, result.get(0).getRouteProcessId());
            assertEquals("P1000", result.get(0).getProcessCode());
            assertEquals("已发布快照工序", result.get(0).getProcessName());
            assertEquals(1, result.get(0).getFormBindings().size());
            assertEquals("FB-ACTIVE-SNAPSHOT", result.get(0).getFormBindings().get(0).getFormBindingKey());
        }
        verify(routeFlowProcessBatchRecordMapper, never()).selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType());
    }

    @Test
    void getRouteFlowProcessConfigList_shouldReadCurrentSettingsWhenNoActiveVersionExists() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteProcessDO currentRouteProcess = MesProRouteProcessDO.builder()
                .id(2000L).routeId(10L).processId(1000L).sort(1).build();
        MesProRouteFlowConfigDO flowConfig = MesProRouteFlowConfigDO.builder()
                .id(800L)
                .routeId(10L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .enabled(Boolean.TRUE)
                .build();
        MesProRouteFlowProcessConfigDO currentConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(901L)
                .routeFlowConfigId(800L)
                .routeId(10L)
                .routeProcessId(2000L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .enabled(Boolean.TRUE)
                .productionQuantityFactor(BigDecimal.ONE)
                .build();
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(currentRouteProcess));
        doReturn(List.of(MesProProcessDO.builder().id(1000L).code("P1000").name("当前工序").build()))
                .when(processMapper).selectBatchIds(anyCollection());
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(flowConfig);
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(List.of(currentConfig));
        when(routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(List.of());

        List<MesProRouteFlowProcessConfigRespVO> result =
                service.getRouteFlowProcessConfigList(10L, MesProRouteFlowConfigTypeEnum.BATCH.getType());

        assertEquals(1, result.size());
        assertEquals(2000L, result.get(0).getRouteProcessId());
        assertEquals("P1000", result.get(0).getProcessCode());
        assertEquals("当前工序", result.get(0).getProcessName());
        assertTrue(result.get(0).getFormBindings().isEmpty());
    }

    @Test
    void getRouteFlowProcessConfigList_shouldReadDraftCandidateUseConfigSnapshot() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(1002L)).thenReturn(MesProRouteVersionDO.builder()
                .id(1002L)
                .routeId(10L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 10,
                          "configSnapshots": {
                            "flowGraph": {
                              "nodes": [
                                {"routeProcessId": 100, "processId": 1000, "sort": 1, "keyFlag": true, "checkFlag": false}
                              ]
                            },
                            "scheduleUseConfigs": [
                              {"routeProcessId": 100, "enabled": true, "productionQuantityFactor": 2.5, "remark": "候选排产配置"}
                            ]
                          }
                        }
                        """)
                .build());
        doReturn(List.of(MesProProcessDO.builder().id(1000L).code("P1000").name("候选工序").build()))
                .when(processMapper).selectBatchIds(anyCollection());

        List<MesProRouteFlowProcessConfigRespVO> result =
                service.getRouteFlowProcessConfigList(
                        10L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType(), 1002L);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getRouteProcessId());
        assertEquals("P1000", result.get(0).getProcessCode());
        assertEquals("候选工序", result.get(0).getProcessName());
        assertEquals(new BigDecimal("2.500000"), result.get(0).getProductionQuantityFactor());
        assertEquals("候选排产配置", result.get(0).getRemark());
        verify(routeProcessMapper, never()).selectListByRouteId(10L);
        verify(routeFlowProcessConfigMapper, never()).selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType());
    }

    @Test
    void getRouteFlowProcessConfigList_shouldReadPendingApprovalCandidateUseConfigSnapshot() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(160L)).thenReturn(MesProRouteVersionDO.builder()
                .id(160L)
                .routeId(10L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL)
                .routeSnapshotJson("""
                        {
                          "routeId": 10,
                          "configSnapshots": {
                            "flowGraph": {
                              "nodes": [
                                {"routeProcessId": 100, "processId": 1000, "sort": 1, "keyFlag": true, "checkFlag": false}
                              ]
                            },
                            "scheduleUseConfigs": [
                              {"routeProcessId": 100, "enabled": true, "productionQuantityFactor": 1.25, "remark": "审批中候选排产配置"}
                            ]
                          }
                        }
                        """)
                .build());
        doReturn(List.of(MesProProcessDO.builder().id(1000L).code("P1000").name("审批中候选工序").build()))
                .when(processMapper).selectBatchIds(anyCollection());

        List<MesProRouteFlowProcessConfigRespVO> result =
                service.getRouteFlowProcessConfigList(
                        10L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType(), 160L);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getRouteProcessId());
        assertEquals("P1000", result.get(0).getProcessCode());
        assertEquals("审批中候选工序", result.get(0).getProcessName());
        assertEquals(new BigDecimal("1.250000"), result.get(0).getProductionQuantityFactor());
        assertEquals("审批中候选排产配置", result.get(0).getRemark());
        verify(routeProcessMapper, never()).selectListByRouteId(10L);
        verify(routeFlowProcessConfigMapper, never()).selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType());
        verify(routeCandidateConfigService, never()).saveConfigSnapshot(any(), any(), any());
    }

    @Test
    void getRouteFlowProcessConfigList_shouldPreservePublishedSnapshotFormBindingFillerFields() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(1002L)).thenReturn(MesProRouteVersionDO.builder()
                .id(1002L)
                .routeId(10L)
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .routeSnapshotJson("""
                        {
                          "routeId": 10,
                          "configSnapshots": {
                            "flowGraph": {
                              "nodes": [
                                {"routeProcessId": 100, "processId": 1000, "sort": 1, "keyFlag": true, "checkFlag": false}
                              ]
                            },
                            "batchUseConfigs": [
                              {
                                "routeProcessId": 100,
                                "enabled": true,
                                "formBindings": [
                                  {
                                    "formBindingKey": "FB-A",
                                    "formTemplateId": 2001,
                                    "formTemplateName": "生产记录表",
                                    "lastPublishedTemplateVersionId": 3001,
                                    "lastPublishedTemplateVersionNo": "V1",
                                    "instanceScope": "BATCH_SHARED",
                                    "sharedFormKey": "shared-a",
                                    "fillableScopeJson": "{\\"ranges\\":[{\\"sourceTableIndex\\":0,\\"startRow\\":0,\\"endRow\\":1}]}",
                                    "candidateSourceType": "USER",
                                    "candidateSourceIds": [9001],
                                    "candidateSourceNames": ["张三"],
                                    "reportSort": 1
                                  }
                                ]
                              }
                            ]
                          }
                        }
                        """)
                .build());
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(MesProRouteProcessDO.builder()
                .id(100L).routeId(10L).processId(1000L).sort(1).build()));
        doReturn(List.of(MesProProcessDO.builder().id(1000L).code("P1000").name("候选工序").build()))
                .when(processMapper).selectBatchIds(anyCollection());

        List<MesProRouteFlowProcessConfigRespVO> result =
                service.getRouteFlowProcessConfigList(
                        10L, MesProRouteFlowConfigTypeEnum.BATCH.getType(), 1002L);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getFormBindings().size());
        var binding = result.get(0).getFormBindings().get(0);
        assertEquals("FB-A", binding.getFormBindingKey());
        assertEquals(2001L, binding.getFormTemplateId());
        assertEquals("生产记录表", binding.getFormTemplateName());
        assertEquals("BATCH_SHARED", binding.getInstanceScope());
        assertEquals("shared-a", binding.getSharedFormKey());
        assertEquals("{\"ranges\":[{\"sourceTableIndex\":0,\"startRow\":0,\"endRow\":1}]}",
                binding.getFillableScopeJson());
        assertEquals("USERS", binding.getCandidateSourceType());
        assertEquals(List.of(9001L), binding.getCandidateSourceIds());
        assertEquals(List.of("张三"), binding.getCandidateSourceNames());
    }

    @Test
    void getRouteFlowProcessConfigList_shouldReadCurrentProcessSettingFormBindingsForDraftVersion() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteFlowConfigDO flowConfig = MesProRouteFlowConfigDO.builder()
                .id(800L)
                .routeId(10L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .enabled(Boolean.TRUE)
                .build();
        MesProRouteFlowProcessConfigDO processConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(901L)
                .routeFlowConfigId(800L)
                .routeId(10L)
                .routeProcessId(100L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .enabled(Boolean.TRUE)
                .productionQuantityFactor(BigDecimal.ONE)
                .build();
        MesProRouteFlowProcessBatchRecordDO currentBinding = MesProRouteFlowProcessBatchRecordDO.builder()
                .id(9901L)
                .routeFlowProcessConfigId(901L)
                .routeId(10L)
                .routeProcessId(100L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .formSlotType("LOSS_REPORT")
                .formBindingKey("FB-LIVE")
                .formTemplateId(2002L)
                .formTemplateNameSnapshot("工序设置当前生产记录")
                .lastPublishedTemplateVersionId(3002L)
                .lastPublishedTemplateVersionNo("V2")
                .instanceScope("PROCESS")
                .recordCategory("BATCH_RECORD")
                .validationProfile("CONTROLLED_BATCH")
                .candidateSourceType("USERS")
                .candidateSourceIds("9002")
                .candidateSourceNames("[\"李四\"]")
                .reportSort(1)
                .build();
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(1002L)).thenReturn(MesProRouteVersionDO.builder()
                .id(1002L)
                .routeId(10L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 10,
                          "configSnapshots": {
                            "flowGraph": {
                              "nodes": [
                                {"routeProcessId": 100, "processId": 1000, "sort": 1, "keyFlag": true, "checkFlag": false}
                              ]
                            },
                            "batchUseConfigs": [
                              {"routeProcessId": 100, "enabled": true, "productionQuantityFactor": 1}
                            ]
                          }
                        }
                        """)
                .build());
        doReturn(List.of(MesProProcessDO.builder().id(1000L).code("P1000").name("清洗工序").build()))
                .when(processMapper).selectBatchIds(anyCollection());
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(MesProRouteProcessDO.builder()
                .id(100L).routeId(10L).processId(1000L).sort(1).build()));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(flowConfig);
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(List.of(processConfig));
        when(routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(List.of(currentBinding));

        List<MesProRouteFlowProcessConfigRespVO> result =
                service.getRouteFlowProcessConfigList(
                        10L, MesProRouteFlowConfigTypeEnum.BATCH.getType(), 1002L);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getFormBindings().size());
        var binding = result.get(0).getFormBindings().get(0);
        assertEquals("FB-LIVE", binding.getFormBindingKey());
        assertEquals("LOSS_REPORT", binding.getFormSlotType());
        assertEquals(2002L, binding.getFormTemplateId());
        assertEquals("工序设置当前生产记录", binding.getFormTemplateName());
        assertEquals("PROCESS", binding.getInstanceScope());
        assertEquals("USERS", binding.getCandidateSourceType());
        assertEquals(List.of(9002L), binding.getCandidateSourceIds());
        assertEquals(List.of("李四"), binding.getCandidateSourceNames());
    }

    @Test
    void getRouteFlowProcessConfigList_shouldReadSavedDraftBatchSnapshotBeforeCurrentBindings() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteFlowConfigDO flowConfig = MesProRouteFlowConfigDO.builder()
                .id(800L)
                .routeId(10L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .enabled(Boolean.TRUE)
                .build();
        MesProRouteFlowProcessConfigDO processConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(901L)
                .routeFlowConfigId(800L)
                .routeId(10L)
                .routeProcessId(100L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .enabled(Boolean.TRUE)
                .productionQuantityFactor(BigDecimal.ONE)
                .build();
        MesProRouteFlowProcessBatchRecordDO currentBinding = MesProRouteFlowProcessBatchRecordDO.builder()
                .id(9901L)
                .routeFlowProcessConfigId(901L)
                .routeId(10L)
                .routeProcessId(100L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .formSlotType("LOSS_REPORT")
                .formBindingKey("FB-LIVE")
                .formTemplateId(2002L)
                .formTemplateNameSnapshot("当前工序设置")
                .lastPublishedTemplateVersionId(3002L)
                .lastPublishedTemplateVersionNo("V2")
                .instanceScope("PROCESS")
                .recordCategory("BATCH_RECORD")
                .validationProfile("CONTROLLED_BATCH")
                .candidateSourceType("USERS")
                .candidateSourceIds("9002")
                .candidateSourceNames("[\"李四\"]")
                .reportSort(1)
                .build();
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(1002L)).thenReturn(MesProRouteVersionDO.builder()
                .id(1002L)
                .routeId(10L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 10,
                          "configSnapshots": {
                            "flowGraph": {
                              "nodes": [
                                {"routeProcessId": 100, "processId": 1000, "sort": 1, "keyFlag": true, "checkFlag": false}
                              ]
                            },
                            "batchUseConfigs": [
                              {
                                "routeProcessId": 100,
                                "enabled": true,
                                "productionQuantityFactor": 1,
                                "batchRecordBindingSnapshotExplicit": true,
                                "formBindings": [
                                  {
                                    "formBindingKey": "FB-DRAFT-SAVED",
                                    "formTemplateId": 2001,
                                    "formTemplateName": "草稿已保存损耗单",
                                    "lastPublishedTemplateVersionId": 3001,
                                    "lastPublishedTemplateVersionNo": "V1",
                                    "formSlotType": "LOSS_REPORT",
                                    "instanceScope": "BATCH_SHARED",
                                    "sharedFormKey": "LOSS_REPORT_2001",
                                    "candidateSourceType": "USERS",
                                    "candidateSourceIds": [9001],
                                    "candidateSourceNames": ["张三"],
                                    "reportSort": 1
                                  }
                                ]
                              }
                            ]
                          }
                        }
                        """)
                .build());
        doReturn(List.of(MesProProcessDO.builder().id(1000L).code("P1000").name("清洗工序").build()))
                .when(processMapper).selectBatchIds(anyCollection());
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(MesProRouteProcessDO.builder()
                .id(100L).routeId(10L).processId(1000L).sort(1).build()));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(flowConfig);
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(List.of(processConfig));
        when(routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(List.of(currentBinding));

        List<MesProRouteFlowProcessConfigRespVO> result =
                service.getRouteFlowProcessConfigList(
                        10L, MesProRouteFlowConfigTypeEnum.BATCH.getType(), 1002L);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getFormBindings().size());
        var binding = result.get(0).getFormBindings().get(0);
        assertEquals("FB-DRAFT-SAVED", binding.getFormBindingKey());
        assertEquals(2001L, binding.getFormTemplateId());
        assertEquals("草稿已保存损耗单", binding.getFormTemplateName());
        assertEquals("BATCH_SHARED", binding.getInstanceScope());
        assertEquals("LOSS_REPORT_2001", binding.getSharedFormKey());
        assertEquals(List.of(9001L), binding.getCandidateSourceIds());
        assertEquals(List.of("张三"), binding.getCandidateSourceNames());
    }

    @Test
    void getRouteFlowProcessConfigList_shouldReadFormalBatchReportsPerRouteProcessFromDraftSnapshot() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(1002L)).thenReturn(MesProRouteVersionDO.builder()
                .id(1002L)
                .routeId(10L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 10,
                          "configSnapshots": {
                            "flowGraph": {
                              "nodes": [
                                {"routeProcessId": 100, "processId": 1000, "sort": 1, "keyFlag": true, "checkFlag": false},
                                {"routeProcessId": 101, "processId": 1000, "sort": 2, "keyFlag": false, "checkFlag": false}
                              ]
                            },
                            "batchUseConfigs": [
                              {
                                "routeProcessId": 100,
                                "enabled": true,
                                "batchRecordBindingSnapshotExplicit": true,
                                "batchRecordReports": [
                                  {"batchRecordReportId": "REPORT-A", "formSlotType": "MAIN", "reportSort": 1}
                                ],
                                "formBindings": [
                                  {
                                    "formBindingKey": "FORM-SLOT-A",
                                    "formTemplateId": 2001,
                                    "formTemplateName": "补充动态表单",
                                    "formSlotType": "LOSS_REPORT",
                                    "reportSort": 2
                                  }
                                ]
                              },
                              {
                                "routeProcessId": 101,
                                "enabled": true,
                                "batchRecordBindingSnapshotExplicit": true,
                                "batchRecordReports": [
                                  {"batchRecordReportId": "REPORT-B", "formSlotType": "MAIN", "reportSort": 1}
                                ]
                              }
                            ]
                          }
                        }
                        """)
                .build());
        doReturn(List.of(MesProProcessDO.builder().id(1000L).code("P1000").name("重复基础工序").build()))
                .when(processMapper).selectBatchIds(anyCollection());
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(List.of());
        when(routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(List.of());
        when(batchRecordReportMapper.selectListByReportIds(Set.of("REPORT-A", "REPORT-B")))
                .thenReturn(List.of(report("REPORT-A"), report("REPORT-B")));

        List<MesProRouteFlowProcessConfigRespVO> result =
                service.getRouteFlowProcessConfigList(
                        10L, MesProRouteFlowConfigTypeEnum.BATCH.getType(), 1002L);

        assertEquals(2, result.size());
        assertEquals(100L, result.get(0).getRouteProcessId());
        assertEquals("REPORT-A-name", result.get(0).getBatchRecordReports().get(0).getBatchRecordReportName());
        assertEquals("FORM-SLOT-A", result.get(0).getFormBindings().get(0).getFormBindingKey());
        assertEquals(101L, result.get(1).getRouteProcessId());
        assertEquals("REPORT-B-name", result.get(1).getBatchRecordReports().get(0).getBatchRecordReportName());
        assertTrue(result.get(1).getFormBindings().isEmpty());
    }

    @Test
    void getRouteFlowProcessConfigList_shouldMapCurrentProcessSettingBindingsByProcessIdentityForDraftVersion() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteFlowConfigDO flowConfig = MesProRouteFlowConfigDO.builder()
                .id(800L)
                .routeId(10L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .enabled(Boolean.TRUE)
                .build();
        MesProRouteProcessDO currentRouteProcess = MesProRouteProcessDO.builder()
                .id(200L).routeId(10L).processId(2000L).sort(1)
                .keyFlag(Boolean.TRUE).checkFlag(Boolean.FALSE).build();
        MesProRouteFlowProcessConfigDO currentProcessConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(902L)
                .routeFlowConfigId(800L)
                .routeId(10L)
                .routeProcessId(200L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .enabled(Boolean.TRUE)
                .productionQuantityFactor(BigDecimal.ONE)
                .build();
        MesProRouteFlowProcessBatchRecordDO currentBinding = MesProRouteFlowProcessBatchRecordDO.builder()
                .id(9902L)
                .routeFlowProcessConfigId(902L)
                .routeId(10L)
                .routeProcessId(200L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .formBindingKey("FB-LIVE-DRIFT")
                .formTemplateId(2003L)
                .formTemplateNameSnapshot("当前清洗工序生产记录")
                .instanceScope("PROCESS")
                .recordCategory("BATCH_RECORD")
                .validationProfile("CONTROLLED_BATCH")
                .reportSort(1)
                .build();
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(1002L)).thenReturn(MesProRouteVersionDO.builder()
                .id(1002L)
                .routeId(10L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 10,
                          "configSnapshots": {
                            "flowGraph": {
                              "nodes": [
                                {"routeProcessId": 100, "processId": 1000, "sort": 1, "keyFlag": true, "checkFlag": false}
                              ]
                            },
                            "batchUseConfigs": [
                              {"routeProcessId": 100, "enabled": true, "productionQuantityFactor": 1}
                            ]
                          }
                        }
                        """)
                .build());
        doReturn(List.of(MesProProcessDO.builder().id(1000L).code("P-CLEAN").name("清洗工序").build()))
                .when(processMapper).selectBatchIds(argThat(ids -> ids != null && ids.contains(1000L)));
        doReturn(List.of(MesProProcessDO.builder().id(2000L).code("P-CLEAN").name("清洗工序").build()))
                .when(processMapper).selectBatchIds(argThat(ids -> ids != null && ids.contains(2000L)));
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(currentRouteProcess));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(flowConfig);
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(List.of(currentProcessConfig));
        when(routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(List.of(currentBinding));

        List<MesProRouteFlowProcessConfigRespVO> result =
                service.getRouteFlowProcessConfigList(
                        10L, MesProRouteFlowConfigTypeEnum.BATCH.getType(), 1002L);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getRouteProcessId());
        assertEquals(1, result.get(0).getFormBindings().size());
        var binding = result.get(0).getFormBindings().get(0);
        assertEquals("FB-LIVE-DRIFT", binding.getFormBindingKey());
        assertEquals(2003L, binding.getFormTemplateId());
        assertEquals("当前清洗工序生产记录", binding.getFormTemplateName());
    }

    @Test
    void getRouteFlowProcessConfigList_shouldReadCurrentProcessSettingFormBindingsForPendingApprovalVersion() {
        assertReviewCandidateVersionReadsCurrentProcessSettingFormBinding(
                MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL, 160L);
    }

    @Test
    void getRouteFlowProcessConfigList_shouldReadCurrentProcessSettingFormBindingsForReadyToPublishVersion() {
        assertReviewCandidateVersionReadsCurrentProcessSettingFormBinding(
                MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH, 170L);
    }

    @Test
    void getRouteFlowProcessConfigList_shouldReadSnapshotFormBindingsForSupersededVersion() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(180L)).thenReturn(MesProRouteVersionDO.builder()
                .id(180L)
                .routeId(10L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_SUPERSEDED)
                .routeSnapshotJson("""
                        {
                          "routeId": 10,
                          "configSnapshots": {
                            "flowGraph": {
                              "nodes": [
                                {"routeProcessId": 100, "processId": 1000, "sort": 1, "keyFlag": true, "checkFlag": false}
                              ]
                            },
                            "batchUseConfigs": [
                              {
                                "routeProcessId": 100,
                                "enabled": true,
                                "productionQuantityFactor": 1,
                                "formBindings": [
                                  {
                                    "formBindingKey": "FB-SUPERSEDED-SNAPSHOT",
                                    "formTemplateId": 2001,
                                    "formTemplateName": "已发布历史生产记录",
                                    "instanceScope": "PROCESS",
                                    "recordCategory": "BATCH_RECORD",
                                    "validationProfile": "CONTROLLED_BATCH",
                                    "candidateSourceType": "USERS",
                                    "candidateSourceIds": [9001],
                                    "candidateSourceNames": ["张三"],
                                    "reportSort": 1
                                  }
                                ]
                              }
                            ]
                          }
                        }
                        """)
                .build());
        doReturn(List.of(MesProProcessDO.builder().id(1000L).code("P1000").name("清洗工序").build()))
                .when(processMapper).selectBatchIds(anyCollection());

        List<MesProRouteFlowProcessConfigRespVO> result =
                service.getRouteFlowProcessConfigList(
                        10L, MesProRouteFlowConfigTypeEnum.BATCH.getType(), 180L);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getFormBindings().size());
        var binding = result.get(0).getFormBindings().get(0);
        assertEquals("FB-SUPERSEDED-SNAPSHOT", binding.getFormBindingKey());
        assertEquals(2001L, binding.getFormTemplateId());
        assertEquals("已发布历史生产记录", binding.getFormTemplateName());
        verify(routeFlowProcessBatchRecordMapper, never()).selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType());
    }

    @ParameterizedTest
    @ValueSource(strings = {"REJECTED", "CANCELLED"})
    void getRouteFlowProcessConfigList_shouldReadSnapshotForClosedCandidateVersion(String lifecycleStatus) {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(181L)).thenReturn(MesProRouteVersionDO.builder()
                .id(181L)
                .routeId(10L)
                .active(Boolean.FALSE)
                .lifecycleStatus(lifecycleStatus)
                .routeSnapshotJson("""
                        {
                          "routeId": 10,
                          "configSnapshots": {
                            "flowGraph": {
                              "nodes": [
                                {"routeProcessId": 100, "processId": 1000, "sort": 1, "keyFlag": true, "checkFlag": false}
                              ]
                            },
                            "batchUseConfigs": [
                              {
                                "routeProcessId": 100,
                                "enabled": true,
                                "productionQuantityFactor": 1,
                                "formBindings": [
                                  {
                                    "formBindingKey": "FB-CLOSED-SNAPSHOT",
                                    "formTemplateId": 2002,
                                    "formTemplateName": "关闭候选生产记录",
                                    "instanceScope": "PROCESS",
                                    "recordCategory": "BATCH_RECORD",
                                    "validationProfile": "CONTROLLED_BATCH",
                                    "candidateSourceType": "USERS",
                                    "candidateSourceIds": [9002],
                                    "candidateSourceNames": ["李四"],
                                    "reportSort": 1
                                  }
                                ]
                              }
                            ]
                          }
                        }
                        """)
                .build());
        doReturn(List.of(MesProProcessDO.builder().id(1000L).code("P1000").name("清洗工序").build()))
                .when(processMapper).selectBatchIds(anyCollection());

        List<MesProRouteFlowProcessConfigRespVO> result =
                service.getRouteFlowProcessConfigList(
                        10L, MesProRouteFlowConfigTypeEnum.BATCH.getType(), 181L);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getFormBindings().size());
        var binding = result.get(0).getFormBindings().get(0);
        assertEquals("FB-CLOSED-SNAPSHOT", binding.getFormBindingKey());
        assertEquals(2002L, binding.getFormTemplateId());
        assertEquals("关闭候选生产记录", binding.getFormTemplateName());
        verify(routeFlowProcessBatchRecordMapper, never()).selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType());
    }

    private void assertReviewCandidateVersionReadsCurrentProcessSettingFormBinding(String lifecycleStatus, Long routeVersionId) {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteFlowConfigDO flowConfig = MesProRouteFlowConfigDO.builder()
                .id(800L)
                .routeId(10L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .enabled(Boolean.TRUE)
                .build();
        MesProRouteFlowProcessConfigDO processConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(901L)
                .routeFlowConfigId(800L)
                .routeId(10L)
                .routeProcessId(100L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .enabled(Boolean.TRUE)
                .productionQuantityFactor(BigDecimal.ONE)
                .build();
        MesProRouteFlowProcessBatchRecordDO currentBinding = MesProRouteFlowProcessBatchRecordDO.builder()
                .id(9901L)
                .routeFlowProcessConfigId(901L)
                .routeId(10L)
                .routeProcessId(100L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .formSlotType("MAIN")
                .formBindingKey("FB-LIVE")
                .formTemplateId(2002L)
                .formTemplateNameSnapshot("工序设置当前生产记录")
                .lastPublishedTemplateVersionId(3002L)
                .lastPublishedTemplateVersionNo("V2")
                .instanceScope("PROCESS")
                .recordCategory("BATCH_RECORD")
                .validationProfile("CONTROLLED_BATCH")
                .candidateSourceType("USERS")
                .candidateSourceIds("9002")
                .candidateSourceNames("[\"李四\"]")
                .reportSort(1)
                .build();
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(routeVersionId)).thenReturn(MesProRouteVersionDO.builder()
                .id(routeVersionId)
                .routeId(10L)
                .active(Boolean.FALSE)
                .lifecycleStatus(lifecycleStatus)
                .routeSnapshotJson("""
                        {
                          "routeId": 10,
                          "configSnapshots": {
                            "flowGraph": {
                              "nodes": [
                                {"routeProcessId": 100, "processId": 1000, "sort": 1, "keyFlag": true, "checkFlag": false}
                              ]
                            },
                            "batchUseConfigs": [
                              {
                                "routeProcessId": 100,
                                "enabled": true,
                                "productionQuantityFactor": 1,
                                "formBindings": [
                                  {
                                    "formBindingKey": "FB-SNAPSHOT",
                                    "formTemplateId": 2001,
                                    "formTemplateName": "提交时生产记录",
                                    "lastPublishedTemplateVersionId": 3001,
                                    "lastPublishedTemplateVersionNo": "V1",
                                    "instanceScope": "PROCESS",
                                    "recordCategory": "BATCH_RECORD",
                                    "validationProfile": "CONTROLLED_BATCH",
                                    "candidateSourceType": "USERS",
                                    "candidateSourceIds": [9001],
                                    "candidateSourceNames": ["张三"],
                                    "reportSort": 1
                                  }
                                ]
                              }
                            ]
                          }
                        }
                        """)
                .build());
        doReturn(List.of(MesProProcessDO.builder().id(1000L).code("P1000").name("清洗工序").build()))
                .when(processMapper).selectBatchIds(anyCollection());
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(MesProRouteProcessDO.builder()
                .id(100L).routeId(10L).processId(1000L).sort(1).build()));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(flowConfig);
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(List.of(processConfig));
        when(routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(List.of(currentBinding));

        List<MesProRouteFlowProcessConfigRespVO> result =
                service.getRouteFlowProcessConfigList(
                        10L, MesProRouteFlowConfigTypeEnum.BATCH.getType(), routeVersionId);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getFormBindings().size());
        var binding = result.get(0).getFormBindings().get(0);
        assertEquals("FB-LIVE", binding.getFormBindingKey());
        assertEquals("MAIN", binding.getFormSlotType());
        assertEquals(2002L, binding.getFormTemplateId());
        assertEquals("工序设置当前生产记录", binding.getFormTemplateName());
        assertEquals(List.of(9002L), binding.getCandidateSourceIds());
        verify(routeFlowProcessBatchRecordMapper).selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType());
    }

    @Test
    void saveRouteFlowConfig_shouldWriteDraftCandidateScheduleUseSnapshotWithoutMutatingActiveBindings() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteProcessDO currentRouteProcess = MesProRouteProcessDO.builder()
                .id(100L).routeId(10L).processId(1000L).sort(1).build();
        MesProRouteFlowProcessConfigSaveReqVO processConfig = new MesProRouteFlowProcessConfigSaveReqVO();
        processConfig.setRouteProcessId(100L);
        processConfig.setEnabled(Boolean.TRUE);
        MesProRouteFlowConfigSaveReqVO reqVO = new MesProRouteFlowConfigSaveReqVO();
        reqVO.setRouteId(10L);
        reqVO.setRouteVersionId(1002L);
        reqVO.setUseType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType());
        reqVO.setProcessConfigs(List.of(processConfig));
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(1002L)).thenReturn(draftRouteVersion(1002L));

        service.saveRouteFlowConfig(reqVO);

        verify(routeCandidateConfigService).saveConfigSnapshot(eq(1002L), eq("scheduleUseConfigs"),
                argThat(snapshot -> snapshot instanceof List<?>
                        && snapshot.toString().contains("routeProcessId=100")));
        verify(routeFlowConfigMapper, never()).insert(any(MesProRouteFlowConfigDO.class));
        verify(routeFlowConfigMapper, never()).updateById(any(MesProRouteFlowConfigDO.class));
        verify(routeFlowProcessConfigMapper, never()).insert(any(MesProRouteFlowProcessConfigDO.class));
        verify(routeFlowProcessConfigMapper, never()).updateById(any(MesProRouteFlowProcessConfigDO.class));
    }

    @Test
    void saveRouteFlowConfig_shouldAcceptDraftCandidateScheduleProcessIdsFromFlowGraphSnapshot() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteProcessDO activeRouteProcess = MesProRouteProcessDO.builder()
                .id(100L).routeId(10L).processId(1000L).sort(1).build();
        MesProRouteFlowProcessConfigSaveReqVO processConfig = new MesProRouteFlowProcessConfigSaveReqVO();
        processConfig.setRouteProcessId(200L);
        processConfig.setEnabled(Boolean.TRUE);
        processConfig.setProductionQuantityFactor(new BigDecimal("2.000000"));
        MesProRouteFlowConfigSaveReqVO reqVO = new MesProRouteFlowConfigSaveReqVO();
        reqVO.setRouteId(10L);
        reqVO.setRouteVersionId(1002L);
        reqVO.setUseType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType());
        reqVO.setProcessConfigs(List.of(processConfig));
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(1002L)).thenReturn(MesProRouteVersionDO.builder()
                .id(1002L)
                .routeId(10L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 10,
                          "configSnapshots": {
                            "flowGraph": {
                              "nodes": [
                                {"routeProcessId": 200, "processId": 1000, "sort": 1, "keyFlag": true, "checkFlag": false}
                              ]
                            },
                            "scheduleUseConfigs": []
                          }
                        }
                        """)
                .build());
        service.saveRouteFlowConfig(reqVO);

        verify(routeCandidateConfigService).saveConfigSnapshot(eq(1002L), eq("scheduleUseConfigs"),
                argThat(snapshot -> snapshot instanceof List<?>
                        && snapshot.toString().contains("routeProcessId=200")
                        && snapshot.toString().contains("productionQuantityFactor=2.000000")));
        verify(routeFlowProcessConfigMapper, never()).insert(any(MesProRouteFlowProcessConfigDO.class));
        verify(routeFlowProcessConfigMapper, never()).updateById(any(MesProRouteFlowProcessConfigDO.class));
    }

    @Test
    void saveRouteFlowConfig_shouldRejectNonDraftRouteVersion() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteFlowConfigSaveReqVO reqVO = new MesProRouteFlowConfigSaveReqVO();
        reqVO.setRouteId(10L);
        reqVO.setRouteVersionId(99L);
        reqVO.setUseType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType());
        MesProRouteFlowProcessConfigSaveReqVO processConfig = new MesProRouteFlowProcessConfigSaveReqVO();
        processConfig.setRouteProcessId(100L);
        processConfig.setEnabled(Boolean.TRUE);
        reqVO.setProcessConfigs(List.of(processConfig));
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(99L)).thenReturn(activeRouteVersion(99L));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveRouteFlowConfig(reqVO));

        assertEquals(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE.getCode(), ex.getCode());
        verify(routeFlowProcessConfigMapper, never()).insert(any(MesProRouteFlowProcessConfigDO.class));
        verify(routeFlowProcessConfigMapper, never()).updateById(any(MesProRouteFlowProcessConfigDO.class));
    }

    @Test
    void saveRouteFlowConfig_shouldRejectCancelledRouteVersion() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteFlowConfigSaveReqVO reqVO = new MesProRouteFlowConfigSaveReqVO();
        reqVO.setRouteId(10L);
        reqVO.setRouteVersionId(99L);
        reqVO.setUseType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType());
        MesProRouteFlowProcessConfigSaveReqVO processConfig = new MesProRouteFlowProcessConfigSaveReqVO();
        processConfig.setRouteProcessId(100L);
        processConfig.setEnabled(Boolean.TRUE);
        reqVO.setProcessConfigs(List.of(processConfig));
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(99L)).thenReturn(MesProRouteVersionDO.builder()
                .id(99L)
                .routeId(10L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_CANCELLED)
                .build());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveRouteFlowConfig(reqVO));

        assertEquals(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE.getCode(), ex.getCode());
        verify(routeCandidateConfigService, never()).saveConfigSnapshot(any(), any(), any());
        verify(routeFlowProcessConfigMapper, never()).insert(any(MesProRouteFlowProcessConfigDO.class));
        verify(routeFlowProcessConfigMapper, never()).updateById(any(MesProRouteFlowProcessConfigDO.class));
    }

    @Test
    void getRouteFlowProcessConfigList_shouldReturnDefaultEnabledRowWhenProcessUseConfigMissing() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(100L).routeId(10L).processId(1000L).sort(1).batchRecordReportId("BASE-REPORT").build();
        MesProProcessDO process = MesProProcessDO.builder().id(1000L).code("B010").name("吹球囊成型").build();
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(routeProcess));
        doReturn(List.of(process)).when(processMapper).selectBatchIds(anyCollection());
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(10L, MesProRouteFlowConfigTypeEnum.BATCH.getType()))
                .thenReturn(List.of());
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(10L, MesProRouteFlowConfigTypeEnum.BATCH.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder().id(800L).routeId(10L).useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                        .enabled(Boolean.TRUE).build());
        when(routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(10L, MesProRouteFlowConfigTypeEnum.BATCH.getType()))
                .thenReturn(List.of());

        List<MesProRouteFlowProcessConfigRespVO> result =
                service.getRouteFlowProcessConfigList(10L, MesProRouteFlowConfigTypeEnum.BATCH.getType());

        assertEquals(1, result.size());
        MesProRouteFlowProcessConfigRespVO row = result.get(0);
        assertEquals(100L, row.getRouteProcessId());
        assertEquals("BASE-REPORT", row.getBaseBatchRecordReportId());
        assertEquals(Boolean.TRUE, row.getRouteConfigEnabled());
        assertTrue(Boolean.TRUE.equals(row.getEnabled()));
        assertEquals(null, row.getExecutionMode());
        assertEquals(new BigDecimal("1.000000"), row.getProductionQuantityFactor());
        assertEquals(List.of(), row.getBatchRecordReports());
    }

    @Test
    void getRouteFlowProcessConfigList_shouldReturnDefaultRowWhenAnyCurrentRouteProcessLacksUseConfig() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteProcessDO firstProcess = MesProRouteProcessDO.builder()
                .id(100L).routeId(10L).processId(1000L).sort(1).batchRecordReportId("BASE-REPORT-1").build();
        MesProRouteProcessDO secondProcess = MesProRouteProcessDO.builder()
                .id(101L).routeId(10L).processId(1001L).sort(2).batchRecordReportId("BASE-REPORT-2").build();
        MesProProcessDO processA = MesProProcessDO.builder().id(1000L).code("B010").name("吹球囊成型").build();
        MesProProcessDO processB = MesProProcessDO.builder().id(1001L).code("B020").name("焊接").build();
        MesProRouteFlowProcessConfigDO existingConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(901L)
                .routeFlowConfigId(800L)
                .routeId(10L)
                .routeProcessId(100L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .enabled(Boolean.FALSE)
                .executionMode("PARALLEL")
                .productionQuantityFactor(new BigDecimal("3.000000"))
                .remark("隐藏首道工序")
                .build();
        MesProRouteFlowProcessConfigDO staleConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(902L)
                .routeFlowConfigId(999L)
                .routeId(10L)
                .routeProcessId(999L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .enabled(Boolean.FALSE)
                .batchRecordReportId("STALE")
                .build();
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(firstProcess, secondProcess));
        doReturn(List.of(processA, processB)).when(processMapper).selectBatchIds(anyCollection());
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(10L, MesProRouteFlowConfigTypeEnum.BATCH.getType()))
                .thenReturn(List.of(existingConfig, staleConfig));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(10L, MesProRouteFlowConfigTypeEnum.BATCH.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder()
                        .id(800L)
                        .routeId(10L)
                        .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                        .enabled(Boolean.TRUE)
                        .build());
        when(routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(10L, MesProRouteFlowConfigTypeEnum.BATCH.getType()))
                .thenReturn(List.of(MesProRouteFlowProcessBatchRecordDO.builder()
                        .routeFlowProcessConfigId(901L)
                        .routeId(10L)
                        .routeProcessId(100L)
                        .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                        .formBindingKey("FB-ACTIVE-1")
                        .formTemplateId(2001L)
                        .formTemplateNameSnapshot("生产记录表")
                        .lastPublishedTemplateVersionId(3001L)
                        .lastPublishedTemplateVersionNo("V1")
                        .recordCategory("BATCH_RECORD")
                        .validationProfile("CONTROLLED_BATCH")
                        .candidateSourceType("USERS")
                        .candidateSourceIds("9001")
                        .candidateSourceNames("[\"张三\"]")
                        .reportSort(1)
                        .build()));

        List<MesProRouteFlowProcessConfigRespVO> result =
                service.getRouteFlowProcessConfigList(10L, MesProRouteFlowConfigTypeEnum.BATCH.getType());

        assertEquals(2, result.size());
        assertEquals(100L, result.get(0).getRouteProcessId());
        assertEquals(Boolean.TRUE, result.get(0).getEnabled());
        assertEquals(null, result.get(0).getExecutionMode());
        assertEquals(new BigDecimal("3.000000"), result.get(0).getProductionQuantityFactor());
        assertEquals(1, result.get(0).getFormBindings().size());
        assertEquals("FB-ACTIVE-1", result.get(0).getFormBindings().get(0).getFormBindingKey());
        assertEquals(2001L, result.get(0).getFormBindings().get(0).getFormTemplateId());
        assertEquals(List.of(9001L), result.get(0).getFormBindings().get(0).getCandidateSourceIds());
        assertEquals(101L, result.get(1).getRouteProcessId());
        assertEquals("BASE-REPORT-2", result.get(1).getBaseBatchRecordReportId());
        assertTrue(Boolean.TRUE.equals(result.get(1).getEnabled()));
        assertEquals(null, result.get(1).getExecutionMode());
        assertEquals(new BigDecimal("1.000000"), result.get(1).getProductionQuantityFactor());
        assertEquals(List.of(), result.get(1).getFormBindings());
    }

    @Test
    void getRouteFlowProcessConfigList_shouldReturnLegacyBatchRecordReportsForProcessDetailDisplay() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("RT000006").name("球囊扩张压力泵").build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(100L).routeId(10L).processId(1000L).sort(1).build();
        MesProProcessDO process = MesProProcessDO.builder().id(1000L).code("P-CLEAN").name("清洗工序").build();
        MesProRouteFlowConfigDO flowConfig = MesProRouteFlowConfigDO.builder()
                .id(800L).routeId(10L).useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .enabled(Boolean.TRUE).build();
        MesProRouteFlowProcessConfigDO processConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(901L)
                .routeFlowConfigId(800L)
                .routeId(10L)
                .routeProcessId(100L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .enabled(Boolean.TRUE)
                .build();
        MesProRouteFlowProcessBatchRecordDO legacyReportBinding =
                MesProRouteFlowProcessBatchRecordDO.builder()
                        .routeFlowProcessConfigId(901L)
                        .routeId(10L)
                        .routeProcessId(100L)
                        .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                        .batchRecordReportId("REPORT-CLEAN")
                        .formSlotType("MAIN")
                        .recordCategory("BATCH_RECORD")
                        .validationProfile("CONTROLLED_BATCH")
                        .reportSort(1)
                        .build();
        MesProBatchRecordReportDO report = new MesProBatchRecordReportDO();
        report.setReportId("REPORT-CLEAN");
        report.setReportCode("BR-CLEAN");
        report.setReportName("清洗工序生产记录");
        report.setFormSlotType("MAIN");
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(routeProcess));
        doReturn(List.of(process)).when(processMapper).selectBatchIds(anyCollection());
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(flowConfig);
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(List.of(processConfig));
        when(routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(List.of(legacyReportBinding));
        when(batchRecordReportMapper.selectListByReportIds(Set.of("REPORT-CLEAN"))).thenReturn(List.of(report));

        List<MesProRouteFlowProcessConfigRespVO> result =
                service.getRouteFlowProcessConfigList(10L, MesProRouteFlowConfigTypeEnum.BATCH.getType());

        assertEquals(1, result.size());
        assertEquals(List.of(), result.get(0).getFormBindings());
        assertEquals(1, result.get(0).getBatchRecordReports().size());
        MesProRouteFlowBatchRecordRespVO batchRecordReport = result.get(0).getBatchRecordReports().get(0);
        assertEquals("REPORT-CLEAN", batchRecordReport.getBatchRecordReportId());
        assertEquals("BR-CLEAN", batchRecordReport.getBatchRecordReportCode());
        assertEquals("清洗工序生产记录", batchRecordReport.getBatchRecordReportName());
        assertEquals("MAIN", batchRecordReport.getFormSlotType());
    }

    @Test
    void getRouteFlowProcessConfigList_shouldIgnoreConfigOwnedByDifferentFlow() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(100L).routeId(10L).processId(1000L).sort(1).build();
        MesProProcessDO process = MesProProcessDO.builder().id(1000L).code("B010").name("吹球囊成型").build();
        MesProRouteFlowConfigDO currentFlow = MesProRouteFlowConfigDO.builder()
                .id(800L).routeId(10L).useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .enabled(Boolean.TRUE).build();
        MesProRouteFlowProcessConfigDO wrongOwnerConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(901L).routeFlowConfigId(999L).routeId(10L).routeProcessId(100L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType()).enabled(Boolean.TRUE).build();
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(routeProcess));
        doReturn(List.of(process)).when(processMapper).selectBatchIds(anyCollection());
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(10L, MesProRouteFlowConfigTypeEnum.BATCH.getType()))
                .thenReturn(currentFlow);
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(10L, MesProRouteFlowConfigTypeEnum.BATCH.getType()))
                .thenReturn(List.of(wrongOwnerConfig));
        when(routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(10L, MesProRouteFlowConfigTypeEnum.BATCH.getType()))
                .thenReturn(List.of());

        List<MesProRouteFlowProcessConfigRespVO> result =
                service.getRouteFlowProcessConfigList(10L, MesProRouteFlowConfigTypeEnum.BATCH.getType());

        assertEquals(1, result.size());
        assertTrue(Boolean.TRUE.equals(result.get(0).getEnabled()));
    }

    @Test
    void saveRouteFlowConfig_shouldRejectZeroProductionQuantityFactor() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(100L).routeId(10L).processId(1000L).sort(1).build();
        MesProRouteFlowConfigSaveReqVO reqVO = new MesProRouteFlowConfigSaveReqVO();
        reqVO.setRouteId(10L);
        reqVO.setRouteVersionId(1002L);
        reqVO.setUseType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType());
        MesProRouteFlowProcessConfigSaveReqVO processConfig = new MesProRouteFlowProcessConfigSaveReqVO();
        processConfig.setRouteProcessId(100L);
        processConfig.setEnabled(Boolean.TRUE);
        processConfig.setProductionQuantityFactor(BigDecimal.ZERO);
        reqVO.setProcessConfigs(List.of(processConfig));

        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(1002L)).thenReturn(draftRouteVersion(1002L));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveRouteFlowConfig(reqVO));

        assertEquals(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_PRODUCTION_QUANTITY_FACTOR_INVALID.getCode(),
                ex.getCode());
        verify(routeFlowProcessConfigMapper, never()).insert(any(MesProRouteFlowProcessConfigDO.class));
        verify(routeFlowProcessConfigMapper, never()).updateById(any(MesProRouteFlowProcessConfigDO.class));
    }

    @Test
    void saveScheduleRouteFlowConfig_shouldNotSyncBatchRouteFlowEnabledState() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteProcessDO firstProcess = MesProRouteProcessDO.builder()
                .id(100L).routeId(10L).processId(1000L).sort(1).build();
        MesProRouteProcessDO secondProcess = MesProRouteProcessDO.builder()
                .id(101L).routeId(10L).processId(1001L).sort(2).build();
        MesProRouteFlowConfigSaveReqVO reqVO = new MesProRouteFlowConfigSaveReqVO();
        reqVO.setRouteId(10L);
        reqVO.setRouteVersionId(1002L);
        reqVO.setUseType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType());
        reqVO.setConfigVersion("ROUTE-1-SCHEDULE-V1");
        MesProRouteFlowProcessConfigSaveReqVO firstConfig = new MesProRouteFlowProcessConfigSaveReqVO();
        firstConfig.setRouteProcessId(100L);
        firstConfig.setEnabled(Boolean.FALSE);
        MesProRouteFlowProcessConfigSaveReqVO secondConfig = new MesProRouteFlowProcessConfigSaveReqVO();
        secondConfig.setRouteProcessId(101L);
        secondConfig.setEnabled(Boolean.TRUE);
        reqVO.setProcessConfigs(List.of(firstConfig, secondConfig));

        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(1002L)).thenReturn(draftRouteVersion(1002L, firstProcess, secondProcess));

        service.saveRouteFlowConfig(reqVO);

        verify(routeCandidateConfigService).saveConfigSnapshot(eq(1002L), eq("scheduleUseConfigs"),
                argThat(snapshot -> snapshot instanceof List<?>
                        && snapshot.toString().contains("routeProcessId=100")
                        && snapshot.toString().contains("routeProcessId=101")));
        verify(routeFlowConfigMapper, never()).insert(any(MesProRouteFlowConfigDO.class));
        verify(routeFlowConfigMapper, never()).updateById(any(MesProRouteFlowConfigDO.class));
        verify(routeFlowProcessConfigMapper, never()).insert(any(MesProRouteFlowProcessConfigDO.class));
        verify(routeFlowProcessConfigMapper, never()).updateById(any(MesProRouteFlowProcessConfigDO.class));
    }

    @Test
    void saveRouteFlowConfig_shouldRejectDuplicateFormTemplateForOneProcess() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(100L).routeId(10L).processId(1000L).sort(1).build();
        stubTenantAndTemplate(2001L, 3001L, "生产记录表");
        MesProRouteFlowConfigSaveReqVO reqVO = new MesProRouteFlowConfigSaveReqVO();
        reqVO.setRouteId(10L);
        reqVO.setRouteVersionId(1002L);
        reqVO.setUseType(MesProRouteFlowConfigTypeEnum.BATCH.getType());
        reqVO.setConfigVersion("ROUTE-1-BATCH-V2");
        MesProRouteFlowProcessConfigSaveReqVO processConfig = new MesProRouteFlowProcessConfigSaveReqVO();
        processConfig.setRouteProcessId(100L);
        processConfig.setEnabled(Boolean.TRUE);
        processConfig.setExecutionMode("SEQUENTIAL");
        processConfig.setFormBindings(List.of(
                formBinding("FB-A", 2001L, "USER", List.of(9001L), List.of("张三"), 1),
                formBinding("FB-B", 2001L, "ROLE", List.of(8001L), List.of("生产角色"), 2)));
        reqVO.setProcessConfigs(List.of(processConfig));
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(1002L)).thenReturn(draftRouteVersion(1002L));

        ServiceException exception = assertThrows(ServiceException.class, () -> service.saveRouteFlowConfig(reqVO));

        assertEquals(PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_DUPLICATE.getCode(), exception.getCode());
        verify(routeFlowProcessBatchRecordMapper, never()).insert(any(MesProRouteFlowProcessBatchRecordDO.class));
    }

    @Test
    void saveRouteFlowConfig_shouldRejectFormTemplateWithoutPublishedVersion() {
        TenantContextHolder.setTenantId(122L);
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(100L).routeId(10L).processId(1000L).sort(1).build();
        MesProRouteFlowConfigSaveReqVO reqVO = new MesProRouteFlowConfigSaveReqVO();
        reqVO.setRouteId(10L);
        reqVO.setRouteVersionId(1002L);
        reqVO.setUseType(MesProRouteFlowConfigTypeEnum.BATCH.getType());
        MesProRouteFlowProcessConfigSaveReqVO processConfig = new MesProRouteFlowProcessConfigSaveReqVO();
        processConfig.setRouteProcessId(100L);
        processConfig.setEnabled(Boolean.TRUE);
        processConfig.setFormBindings(List.of(
                formBinding("FB-UNPUBLISHED", 2003L, "USER", List.of(9001L), List.of("张三"), 1)));
        reqVO.setProcessConfigs(List.of(processConfig));
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(1002L)).thenReturn(draftRouteVersion(1002L));
        when(formTemplateVersionMapper.selectLatestPublishedByTemplateId(122L, 2003L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.saveRouteFlowConfig(reqVO));

        assertEquals(PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_PUBLISHED_VERSION_NOT_EXISTS.getCode(), exception.getCode());
        verify(routeFlowProcessBatchRecordMapper, never()).insert(any(MesProRouteFlowProcessBatchRecordDO.class));
    }

    @Test
    void saveRouteFlowConfig_shouldRejectSelectedFormWithoutFiller() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        stubTenantAndTemplate(2002L, 3002L, "过程记录表");
        MesProRouteFlowConfigSaveReqVO reqVO = new MesProRouteFlowConfigSaveReqVO();
        reqVO.setRouteId(10L);
        reqVO.setRouteVersionId(1002L);
        reqVO.setUseType(MesProRouteFlowConfigTypeEnum.BATCH.getType());
        MesProRouteFlowProcessConfigSaveReqVO processConfig = new MesProRouteFlowProcessConfigSaveReqVO();
        processConfig.setRouteProcessId(100L);
        processConfig.setEnabled(Boolean.TRUE);
        processConfig.setFormBindings(List.of(new MesProRouteFlowFormBindingSaveReqVO()
                .setFormBindingKey("FB-MISSING-FILLER")
                .setFormTemplateId(2002L)
                .setReportSort(1)));
        reqVO.setProcessConfigs(List.of(processConfig));
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(1002L)).thenReturn(draftRouteVersion(1002L));

        ServiceException exception = assertThrows(ServiceException.class, () -> service.saveRouteFlowConfig(reqVO));

        assertEquals(PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_FILLER_REQUIRED.getCode(), exception.getCode());
        verify(routeCandidateConfigService, never()).saveConfigSnapshot(any(), any(), any());
        verify(adminUserApi, never()).validateUserList(anyCollection());
        verify(roleApi, never()).validRoleList(anyCollection());
        verify(routeFlowProcessBatchRecordMapper, never()).insert(any(MesProRouteFlowProcessBatchRecordDO.class));
    }

    @Test
    void getRouteFlowProcessConfigList_shouldReturnInternalRecordMetadata() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(100L).routeId(10L).processId(1000L).sort(1).batchRecordReportId("BASE-REPORT").build();
        MesProProcessDO process = MesProProcessDO.builder().id(1000L).code("B010").name("吹球囊成型").build();
        MesProRouteFlowProcessConfigDO existingConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(901L)
                .routeFlowConfigId(800L)
                .routeId(10L)
                .routeProcessId(100L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .enabled(Boolean.TRUE)
                .executionMode("SEQUENTIAL")
                .build();
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(routeProcess));
        doReturn(List.of(process)).when(processMapper).selectBatchIds(anyCollection());
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(10L, MesProRouteFlowConfigTypeEnum.BATCH.getType()))
                .thenReturn(List.of(existingConfig));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(10L, MesProRouteFlowConfigTypeEnum.BATCH.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder()
                        .id(800L).routeId(10L).useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                        .enabled(Boolean.TRUE).build());
        when(routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(10L, MesProRouteFlowConfigTypeEnum.BATCH.getType()))
                .thenReturn(List.of(MesProRouteFlowProcessBatchRecordDO.builder()
                        .routeFlowProcessConfigId(901L)
                        .routeId(10L)
                        .routeProcessId(100L)
                        .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                        .formBindingKey("FB-INTERNAL-1")
                        .formTemplateId(2002L)
                        .formTemplateNameSnapshot("内部追溯表")
                        .lastPublishedTemplateVersionId(3002L)
                        .lastPublishedTemplateVersionNo("V1")
                        .recordCategory("INTERNAL_RECORD")
                        .validationProfile("INTERNAL_TRACE")
                        .permissionScopeId(5001L)
                        .candidateSourceType("ROLE")
                        .candidateSourceIds("8001")
                        .candidateSourceNames("[\"生产角色\"]")
                        .reportSort(1)
                        .build()));

        List<MesProRouteFlowProcessConfigRespVO> list =
                service.getRouteFlowProcessConfigList(10L, MesProRouteFlowConfigTypeEnum.BATCH.getType());

        assertEquals(1, list.size());
        assertEquals("FB-INTERNAL-1", list.get(0).getFormBindings().get(0).getFormBindingKey());
        assertEquals("INTERNAL_RECORD", list.get(0).getFormBindings().get(0).getRecordCategory());
        assertEquals("INTERNAL_TRACE", list.get(0).getFormBindings().get(0).getValidationProfile());
        assertEquals(Boolean.FALSE, list.get(0).getFormBindings().get(0).getRecordbookEnabled());
        assertEquals(5001L, list.get(0).getFormBindings().get(0).getPermissionScopeId());
        assertEquals("ROLE", list.get(0).getFormBindings().get(0).getCandidateSourceType());
        assertEquals(List.of(8001L), list.get(0).getFormBindings().get(0).getCandidateSourceIds());
    }

    @Test
    void getRouteFlowProcessConfigList_shouldDefaultRecordbookEnabledForBatchRecordBinding() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(100L).routeId(10L).processId(1000L).sort(1).batchRecordReportId("BASE-REPORT").build();
        MesProProcessDO process = MesProProcessDO.builder().id(1000L).code("B010").name("吹球囊成型").build();
        MesProRouteFlowProcessConfigDO existingConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(901L)
                .routeFlowConfigId(800L)
                .routeId(10L)
                .routeProcessId(100L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .enabled(Boolean.TRUE)
                .executionMode("SEQUENTIAL")
                .build();
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(routeProcess));
        doReturn(List.of(process)).when(processMapper).selectBatchIds(anyCollection());
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(10L, MesProRouteFlowConfigTypeEnum.BATCH.getType()))
                .thenReturn(List.of(existingConfig));
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(10L, MesProRouteFlowConfigTypeEnum.BATCH.getType()))
                .thenReturn(MesProRouteFlowConfigDO.builder()
                        .id(800L).routeId(10L).useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                        .enabled(Boolean.TRUE).build());
        when(routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(10L, MesProRouteFlowConfigTypeEnum.BATCH.getType()))
                .thenReturn(List.of(MesProRouteFlowProcessBatchRecordDO.builder()
                        .routeFlowProcessConfigId(901L)
                        .routeId(10L)
                        .routeProcessId(100L)
                        .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                        .batchRecordReportId("BATCH-RECORD-1")
                        .formSlotType("MAIN")
                        .recordCategory("BATCH_RECORD")
                        .validationProfile("CONTROLLED_BATCH")
                        .reportSort(1)
                        .build()));
        when(batchRecordReportMapper.selectListByReportIds(anyCollection()))
                .thenReturn(List.of(report("BATCH-RECORD-1")));

        List<MesProRouteFlowProcessConfigRespVO> list =
                service.getRouteFlowProcessConfigList(10L, MesProRouteFlowConfigTypeEnum.BATCH.getType());

        assertEquals(Boolean.TRUE, list.get(0).getBatchRecordReports().get(0).getRecordbookEnabled());
    }

    @Test
    void getRouteFlowProcessConfigList_shouldIgnoreBindingOwnedByReplacedProcessConfig() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(100L).routeId(10L).processId(1000L).sort(1).build();
        MesProProcessDO process = MesProProcessDO.builder().id(1000L).code("B010").name("吹球囊成型").build();
        MesProRouteFlowProcessConfigDO currentConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(902L)
                .routeId(10L)
                .routeProcessId(100L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .enabled(Boolean.TRUE)
                .executionMode("SEQUENTIAL")
                .build();
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeProcessMapper.selectListByRouteId(10L)).thenReturn(List.of(routeProcess));
        doReturn(List.of(process)).when(processMapper).selectBatchIds(anyCollection());
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType()))
                .thenReturn(List.of(currentConfig));
        when(routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(
                10L, MesProRouteFlowConfigTypeEnum.BATCH.getType()))
                .thenReturn(List.of(MesProRouteFlowProcessBatchRecordDO.builder()
                        .routeFlowProcessConfigId(901L)
                        .routeId(10L)
                        .routeProcessId(100L)
                        .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                        .batchRecordReportId("STALE-REPORT")
                        .formSlotType("MAIN")
                        .recordCategory("BATCH_RECORD")
                        .validationProfile("CONTROLLED_BATCH")
                        .reportSort(1)
                        .build()));

        List<MesProRouteFlowProcessConfigRespVO> list =
                service.getRouteFlowProcessConfigList(10L, MesProRouteFlowConfigTypeEnum.BATCH.getType());

        assertEquals(1, list.size());
        assertTrue(list.get(0).getBatchRecordReports().isEmpty());
    }

    @Test
    void getRouteFlowProcessConfigList_shouldRejectInvalidUseType() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.getRouteFlowProcessConfigList(10L, "UNKNOWN"));
        assertEquals(PRO_ROUTE_FLOW_TYPE_INVALID.getCode(), ex.getCode());
    }

    private MesProBatchRecordReportDO report(String reportId) {
        MesProBatchRecordReportDO report = new MesProBatchRecordReportDO();
        report.setReportId(reportId);
        report.setReportCode(reportId);
        report.setReportName(reportId + "-name");
        report.setFormSlotType("MAIN");
        return report;
    }

    private MesProRouteFlowBatchRecordSaveReqVO mainBatchRecord(String reportId, int reportSort) {
        return new MesProRouteFlowBatchRecordSaveReqVO()
                .setBatchRecordReportId(reportId)
                .setFormSlotType("MAIN")
                .setRecordCategory("BATCH_RECORD")
                .setValidationProfile("CONTROLLED_BATCH")
                .setReportSort(reportSort);
    }

    @Test
    void validateGlobalFormBindingGroups_shouldAcceptCompleteConsistentGroupAndIgnoreServerFields() {
        MesProRouteFlowFormBindingSaveReqVO first = globalFormBinding("FB-100", "GFB-1", 2001L);
        first.setLastPublishedTemplateVersionId(3001L).setSlotConfigSnapshotHash("HASH-A");
        MesProRouteFlowFormBindingSaveReqVO second = globalFormBinding("FB-200", "GFB-1", 2001L);
        second.setLastPublishedTemplateVersionId(3002L).setSlotConfigSnapshotHash("HASH-B");

        service.validateGlobalFormBindingGroups(
                List.of(processConfig(100L, first), processConfig(200L, second)),
                Set.of(100L, 200L));
    }

    @Test
    void validateGlobalFormBindingGroups_shouldRejectIncompleteGroup() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.validateGlobalFormBindingGroups(
                        List.of(processConfig(100L, globalFormBinding("FB-100", "GFB-1", 2001L))),
                        Set.of(100L, 200L)));

        assertEquals(PRO_ROUTE_FLOW_CONFIG_GLOBAL_FORM_GROUP_INCOMPLETE.getCode(), exception.getCode());
    }

    @Test
    void validateGlobalFormBindingGroups_shouldRejectDuplicateMember() {
        MesProRouteFlowProcessConfigSaveReqVO processConfig = new MesProRouteFlowProcessConfigSaveReqVO()
                .setRouteProcessId(100L)
                .setFormBindings(List.of(
                        globalFormBinding("FB-100-A", "GFB-1", 2001L),
                        globalFormBinding("FB-100-B", "GFB-1", 2001L)));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.validateGlobalFormBindingGroups(List.of(processConfig), Set.of(100L)));

        assertEquals(PRO_ROUTE_FLOW_CONFIG_GLOBAL_FORM_GROUP_DUPLICATE.getCode(), exception.getCode());
    }

    @Test
    void validateGlobalFormBindingGroups_shouldRejectEditableConfigMismatch() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.validateGlobalFormBindingGroups(
                        List.of(
                                processConfig(100L, globalFormBinding("FB-100", "GFB-1", 2001L)),
                                processConfig(200L, globalFormBinding("FB-200", "GFB-1", 2002L))),
                        Set.of(100L, 200L)));

        assertEquals(PRO_ROUTE_FLOW_CONFIG_GLOBAL_FORM_GROUP_INCONSISTENT.getCode(), exception.getCode());
    }

    private MesProRouteFlowProcessConfigSaveReqVO processConfig(
            Long routeProcessId,
            MesProRouteFlowFormBindingSaveReqVO... bindings) {
        return new MesProRouteFlowProcessConfigSaveReqVO()
                .setRouteProcessId(routeProcessId)
                .setFormBindings(List.of(bindings));
    }

    private MesProRouteFlowFormBindingSaveReqVO globalFormBinding(
            String formBindingKey,
            String globalSyncKey,
            Long templateId) {
        return new MesProRouteFlowFormBindingSaveReqVO()
                .setFormBindingKey(formBindingKey)
                .setGlobalSyncKey(globalSyncKey)
                .setFormTemplateId(templateId)
                .setFormSlotType("PROCESS_INSPECTION")
                .setInstanceScope("PROCESS")
                .setRecordCategory("INTERNAL_RECORD")
                .setValidationProfile("INTERNAL_TRACE")
                .setRecordbookEnabled(Boolean.FALSE)
                .setRequiredPolicy("REQUIRED")
                .setOwnerRoleKey("QUALITY")
                .setArchiveVisibility("FINAL_DHR")
                .setCandidateSourceType("ROLE")
                .setCandidateSourceIds(List.of(8001L))
                .setCandidateSourceNames(List.of("质量角色"))
                .setReportSort(1)
                .setRemark("全局配置");
    }

    private void stubTenantAndTemplate(Long templateId, Long versionId, String templateName) {
        TenantContextHolder.setTenantId(122L);
        lenient().when(formTemplateVersionMapper.selectLatestPublishedByTemplateId(122L, templateId))
                .thenReturn(FormTemplateVersionDO.builder()
                        .id(versionId)
                        .templateId(templateId)
                        .tenantId(122L)
                        .templateName(templateName)
                        .versionNo("V1")
                        .status("PUBLISHED")
                        .build());
    }

    private MesProRouteFlowFormBindingSaveReqVO formBinding(String formBindingKey,
                                                            Long templateId,
                                                            String candidateSourceType,
                                                            List<Long> candidateSourceIds,
                                                            List<String> candidateSourceNames,
                                                            int reportSort) {
        return new MesProRouteFlowFormBindingSaveReqVO()
                .setFormBindingKey(formBindingKey)
                .setFormTemplateId(templateId)
                .setFormTemplateName("前端模板名称快照")
                .setInstanceScope("PROCESS")
                .setRequiredPolicy("REQUIRED")
                .setCandidateSourceType(candidateSourceType)
                .setCandidateSourceIds(candidateSourceIds)
                .setCandidateSourceNames(candidateSourceNames)
                .setReportSort(reportSort);
    }

    @Test
    void saveRouteFlowConfig_shouldUseDraftCandidateSnapshotRouteProcesses() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        stubTenantAndTemplate(2001L, 3001L, "生产记录表");
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(1002L)).thenReturn(MesProRouteVersionDO.builder()
                .id(1002L)
                .routeId(10L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 10,
                          "configSnapshots": {
                            "flowGraph": {
                              "nodes": [
                                {"routeProcessId": 34501, "processId": 1000, "sort": 1, "keyFlag": true, "checkFlag": false}
                              ]
                            }
                          }
                        }
                        """)
                .build());

        MesProRouteFlowConfigSaveReqVO reqVO = new MesProRouteFlowConfigSaveReqVO();
        reqVO.setRouteId(10L);
        reqVO.setRouteVersionId(1002L);
        reqVO.setUseType(MesProRouteFlowConfigTypeEnum.BATCH.getType());
        reqVO.setProcessConfigs(List.of(new MesProRouteFlowProcessConfigSaveReqVO()
                .setRouteProcessId(34501L)
                .setFormBindings(List.of(formBinding("FB-CANDIDATE", 2001L, "USER",
                        List.of(9001L), List.of("张三"), 1)))));

        service.saveRouteFlowConfig(reqVO);

        verify(routeCandidateConfigService).saveConfigSnapshot(eq(1002L), eq("batchUseConfigs"),
                argThat(snapshot -> snapshot instanceof List<?>
                        && snapshot.toString().contains("routeProcessId=34501")
                        && snapshot.toString().contains("FB-CANDIDATE")));
        verify(routeProcessMapper, never()).selectListByRouteId(10L);
        verify(adminUserApi).validateUserList(List.of(9001L));
    }

    @Test
    void saveRouteFlowConfig_shouldRoundTripDraftBatchRecordReportsWithoutConvertingToFormBindings() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteVersionDO routeVersion = draftRouteVersion(1002L);
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(1002L)).thenReturn(routeVersion);
        doReturn(List.of(MesProProcessDO.builder().id(1000L).code("P1000").name("粗洗工序").build()))
                .when(processMapper).selectBatchIds(anyCollection());

        MesProRouteFlowConfigSaveReqVO reqVO = new MesProRouteFlowConfigSaveReqVO();
        reqVO.setRouteId(10L);
        reqVO.setRouteVersionId(1002L);
        reqVO.setUseType(MesProRouteFlowConfigTypeEnum.BATCH.getType());
        reqVO.setProcessConfigs(List.of(new MesProRouteFlowProcessConfigSaveReqVO()
                .setRouteProcessId(100L)
                .setEnabled(Boolean.TRUE)
                .setBatchRecordReports(List.of(mainBatchRecord("REPORT-CLEAN", 1)))
                .setFormBindings(List.of())));

        service.saveRouteFlowConfig(reqVO);

        ArgumentCaptor<Object> snapshotCaptor = ArgumentCaptor.forClass(Object.class);
        verify(routeCandidateConfigService).saveConfigSnapshot(
                eq(1002L), eq("batchUseConfigs"), snapshotCaptor.capture());
        @SuppressWarnings("unchecked")
        List<MesProRouteFlowProcessConfigSaveReqVO> savedSnapshot =
                (List<MesProRouteFlowProcessConfigSaveReqVO>) snapshotCaptor.getValue();
        assertEquals(1, savedSnapshot.get(0).getBatchRecordReports().size());
        assertEquals("REPORT-CLEAN",
                savedSnapshot.get(0).getBatchRecordReports().get(0).getBatchRecordReportId());
        assertTrue(savedSnapshot.get(0).getFormBindings().isEmpty());

        routeVersion.setRouteSnapshotJson("""
                {
                  "routeId": 10,
                  "configSnapshots": {
                    "flowGraph": {
                      "nodes": [
                        {"routeProcessId": 100, "processId": 1000, "sort": 1, "keyFlag": true, "checkFlag": false}
                      ]
                    },
                    "batchUseConfigs": %s
                  }
                }
                """.formatted(JSON.toJSONString(savedSnapshot)));

        List<MesProRouteFlowProcessConfigRespVO> result =
                service.getRouteFlowProcessConfigList(
                        10L, MesProRouteFlowConfigTypeEnum.BATCH.getType(), 1002L);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getBatchRecordReports().size());
        assertEquals("REPORT-CLEAN", result.get(0).getBatchRecordReports().get(0).getBatchRecordReportId());
        assertTrue(result.get(0).getFormBindings().isEmpty());
    }

    @Test
    void saveRouteFlowConfig_shouldMergeDraftCandidateBatchUseSnapshotByRouteProcessId() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteProcessDO firstProcess = MesProRouteProcessDO.builder()
                .id(100L).routeId(10L).processId(1000L).sort(1).build();
        MesProRouteProcessDO secondProcess = MesProRouteProcessDO.builder()
                .id(101L).routeId(10L).processId(1001L).sort(2).build();
        stubTenantAndTemplate(2001L, 3001L, "既有生产记录表");
        stubTenantAndTemplate(2002L, 3002L, "新增角色记录表");
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(1002L)).thenReturn(MesProRouteVersionDO.builder()
                .id(1002L)
                .routeId(10L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 10,
                          "configSnapshots": {
                            "flowGraph": {
                              "nodes": [
                                {"routeProcessId": 100, "processId": 1000, "sort": 1, "keyFlag": true, "checkFlag": false},
                                {"routeProcessId": 101, "processId": 1001, "sort": 2, "keyFlag": false, "checkFlag": false}
                              ]
                            },
                            "batchUseConfigs": [
                              {
                                "routeProcessId": 100,
                                "enabled": true,
                                "formBindings": [
                                  {
                                    "formBindingKey": "FB-A",
                                    "formTemplateId": 2001,
                                    "formTemplateName": "既有生产记录表",
                                    "lastPublishedTemplateVersionId": 3001,
                                    "lastPublishedTemplateVersionNo": "V1",
                                    "instanceScope": "BATCH_SHARED",
                                    "sharedFormKey": "shared-a",
                                    "fillableScopeJson": "{\\"ranges\\":[{\\"sourceTableIndex\\":0,\\"startRow\\":0,\\"endRow\\":1}]}",
                                    "candidateSourceType": "USERS",
                                    "candidateSourceIds": [9001],
                                    "candidateSourceNames": ["张三"],
                                    "reportSort": 1
                                  }
                                ]
                              }
                            ]
                          }
                        }
                        """)
                .build());

        MesProRouteFlowConfigSaveReqVO reqVO = new MesProRouteFlowConfigSaveReqVO();
        reqVO.setRouteId(10L);
        reqVO.setRouteVersionId(1002L);
        reqVO.setUseType(MesProRouteFlowConfigTypeEnum.BATCH.getType());
        reqVO.setProcessConfigs(List.of(new MesProRouteFlowProcessConfigSaveReqVO()
                .setRouteProcessId(101L)
                .setFormBindings(List.of(formBinding("FB-B", 2002L, "ROLE",
                        List.of(8001L), List.of("生产角色"), 1)))));

        service.saveRouteFlowConfig(reqVO);

        verify(routeCandidateConfigService).saveConfigSnapshot(eq(1002L), eq("batchUseConfigs"),
                argThat(snapshot -> snapshot instanceof List<?>
                        && snapshot.toString().contains("routeProcessId=100")
                        && snapshot.toString().contains("FB-A")
                        && snapshot.toString().contains("BATCH_SHARED")
                        && snapshot.toString().contains("shared-a")
                        && snapshot.toString().contains("candidateSourceIds=[9001]")
                        && snapshot.toString().contains("routeProcessId=101")
                        && snapshot.toString().contains("FB-B")
                        && snapshot.toString().contains("candidateSourceType=ROLE")
                        && snapshot.toString().contains("candidateSourceIds=[8001]")));
        verify(adminUserApi).validateUserList(List.of(9001L));
        verify(roleApi).validRoleList(List.of(8001L));
    }

    @Test
    void saveRouteFlowConfig_shouldRejectPartialGlobalGroupTamperingWithoutSavingSnapshot() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        stubTenantAndTemplate(2001L, 3001L, "全局过程检验表");
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(1002L)).thenReturn(MesProRouteVersionDO.builder()
                .id(1002L)
                .routeId(10L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 10,
                          "configSnapshots": {
                            "flowGraph": {
                              "nodes": [
                                {"routeProcessId": 100, "processId": 1000, "sort": 1, "keyFlag": false, "checkFlag": false},
                                {"routeProcessId": 101, "processId": 1001, "sort": 2, "keyFlag": false, "checkFlag": false}
                              ]
                            },
                            "batchUseConfigs": [
                              {
                                "routeProcessId": 100,
                                "enabled": true,
                                "formBindings": [{
                                  "formBindingKey": "FB-100",
                                  "globalSyncKey": "GFB-1",
                                  "formTemplateId": 2001,
                                  "formSlotType": "PROCESS_INSPECTION",
                                  "instanceScope": "PROCESS",
                                  "recordCategory": "INTERNAL_RECORD",
                                  "validationProfile": "INTERNAL_TRACE",
                                  "recordbookEnabled": false,
                                  "requiredPolicy": "REQUIRED",
                                  "ownerRoleKey": "QUALITY",
                                  "archiveVisibility": "FINAL_DHR",
                                  "candidateSourceType": "ROLE",
                                  "candidateSourceIds": [8001],
                                  "candidateSourceNames": ["质量角色"],
                                  "reportSort": 1,
                                  "remark": "全局配置"
                                }]
                              },
                              {
                                "routeProcessId": 101,
                                "enabled": true,
                                "formBindings": [{
                                  "formBindingKey": "FB-101",
                                  "globalSyncKey": "GFB-1",
                                  "formTemplateId": 2001,
                                  "formSlotType": "PROCESS_INSPECTION",
                                  "instanceScope": "PROCESS",
                                  "recordCategory": "INTERNAL_RECORD",
                                  "validationProfile": "INTERNAL_TRACE",
                                  "recordbookEnabled": false,
                                  "requiredPolicy": "REQUIRED",
                                  "ownerRoleKey": "QUALITY",
                                  "archiveVisibility": "FINAL_DHR",
                                  "candidateSourceType": "ROLE",
                                  "candidateSourceIds": [8001],
                                  "candidateSourceNames": ["质量角色"],
                                  "reportSort": 1,
                                  "remark": "全局配置"
                                }]
                              }
                            ]
                          }
                        }
                        """)
                .build());

        MesProRouteFlowConfigSaveReqVO reqVO = new MesProRouteFlowConfigSaveReqVO()
                .setRouteId(10L)
                .setRouteVersionId(1002L)
                .setUseType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .setProcessConfigs(List.of(processConfig(101L,
                        globalFormBinding("FB-101", "GFB-1", 2001L).setRemark("局部篡改"))));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.saveRouteFlowConfig(reqVO));

        assertEquals(PRO_ROUTE_FLOW_CONFIG_GLOBAL_FORM_GROUP_INCOMPLETE.getCode(), exception.getCode());
        verify(routeCandidateConfigService, never()).saveConfigSnapshot(any(), any(), any());
    }

    @Test
    void saveRouteFlowConfig_shouldWriteDraftCandidateBatchUseSnapshotWithoutMutatingActiveBindings() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(100L).routeId(10L).processId(1000L).sort(1).build();
        stubTenantAndTemplate(2001L, 3001L, "生产记录表");
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(1002L)).thenReturn(draftRouteVersion(1002L));

        MesProRouteFlowConfigSaveReqVO reqVO = new MesProRouteFlowConfigSaveReqVO();
        reqVO.setRouteId(10L);
        reqVO.setRouteVersionId(1002L);
        reqVO.setUseType(MesProRouteFlowConfigTypeEnum.BATCH.getType());
        reqVO.setConfigVersion("BATCH-CANDIDATE");
        reqVO.setProcessConfigs(List.of(new MesProRouteFlowProcessConfigSaveReqVO()
                .setRouteProcessId(100L)
                .setExecutionMode("SEQUENTIAL")
                .setFormBindings(List.of(formBinding("FB-USER", 2001L, "USER",
                        List.of(9001L), List.of("张三"), 1)
                        .setRecordbookEnabled(Boolean.FALSE)))));

        service.saveRouteFlowConfig(reqVO);

        ArgumentCaptor<Object> snapshotCaptor = ArgumentCaptor.forClass(Object.class);
        verify(routeCandidateConfigService).saveConfigSnapshot(
                eq(1002L), eq("batchUseConfigs"), snapshotCaptor.capture());
        @SuppressWarnings("unchecked")
        List<MesProRouteFlowProcessConfigSaveReqVO> snapshot =
                (List<MesProRouteFlowProcessConfigSaveReqVO>) snapshotCaptor.getValue();
        MesProRouteFlowFormBindingSaveReqVO savedBinding = snapshot.get(0).getFormBindings().get(0);
        assertEquals("FB-USER", savedBinding.getFormBindingKey());
        assertEquals(Boolean.FALSE, savedBinding.getRecordbookEnabled());
        assertEquals("USERS", savedBinding.getCandidateSourceType());
        assertEquals(List.of(9001L), savedBinding.getCandidateSourceIds());
        assertTrue(savedBinding.getRecordCategorySnapshotHash().matches("[0-9a-f]{64}"));
        assertTrue(savedBinding.getSlotConfigSnapshotHash().matches("[0-9a-f]{64}"));
        assertFalse(savedBinding.getRecordCategorySnapshotHash()
                .equals(savedBinding.getSlotConfigSnapshotHash()));
        verify(adminUserApi).validateUserList(List.of(9001L));
        verify(routeFlowConfigMapper, never()).insert(any(MesProRouteFlowConfigDO.class));
        verify(routeFlowConfigMapper, never()).updateById(any(MesProRouteFlowConfigDO.class));
        verify(routeFlowProcessConfigMapper, never()).insert(any(MesProRouteFlowProcessConfigDO.class));
        verify(routeFlowProcessConfigMapper, never()).updateById(any(MesProRouteFlowProcessConfigDO.class));
        verify(routeFlowProcessBatchRecordMapper, never()).insert(any(MesProRouteFlowProcessBatchRecordDO.class));
    }

    @Test
    void saveRouteFlowConfig_shouldPreserveFormalBatchRecordReportsInDraftCandidateSnapshot() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(1002L)).thenReturn(draftRouteVersion(1002L));

        MesProRouteFlowConfigSaveReqVO reqVO = new MesProRouteFlowConfigSaveReqVO();
        reqVO.setRouteId(10L);
        reqVO.setRouteVersionId(1002L);
        reqVO.setUseType(MesProRouteFlowConfigTypeEnum.BATCH.getType());
        reqVO.setProcessConfigs(List.of(new MesProRouteFlowProcessConfigSaveReqVO()
                .setRouteProcessId(100L)
                .setEnabled(Boolean.TRUE)
                .setBatchRecordReports(List.of(mainBatchRecord("REPORT-CLEAN", 1)))));

        service.saveRouteFlowConfig(reqVO);

        verify(routeCandidateConfigService).saveConfigSnapshot(eq(1002L), eq("batchUseConfigs"),
                argThat(snapshot -> {
                    if (!(snapshot instanceof List<?> rows) || rows.size() != 1) {
                        return false;
                    }
                    if (!(rows.get(0) instanceof MesProRouteFlowProcessConfigSaveReqVO savedConfig)) {
                        return false;
                    }
                    return Boolean.TRUE.equals(savedConfig.getBatchRecordBindingSnapshotExplicit())
                            && savedConfig.getBatchRecordReports() != null
                            && savedConfig.getBatchRecordReports().size() == 1
                            && "REPORT-CLEAN".equals(savedConfig.getBatchRecordReports().get(0)
                            .getBatchRecordReportId())
                            && "MAIN".equals(savedConfig.getBatchRecordReports().get(0).getFormSlotType());
                }));
    }

    @Test
    void saveRouteFlowConfig_shouldDefaultBatchExecutionModeWhenSavingFormOnlyCandidateSnapshot() {
        MesProRouteDO route = MesProRouteDO.builder().id(10L).code("ROUTE-1").name("Route1").build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(100L).routeId(10L).processId(1000L).sort(1).build();
        stubTenantAndTemplate(2001L, 3001L, "生产记录表");
        when(routeMapper.selectById(10L)).thenReturn(route);
        when(routeVersionMapper.selectById(1002L)).thenReturn(draftRouteVersion(1002L));

        MesProRouteFlowConfigSaveReqVO reqVO = new MesProRouteFlowConfigSaveReqVO();
        reqVO.setRouteId(10L);
        reqVO.setRouteVersionId(1002L);
        reqVO.setUseType(MesProRouteFlowConfigTypeEnum.BATCH.getType());
        reqVO.setProcessConfigs(List.of(new MesProRouteFlowProcessConfigSaveReqVO()
                .setRouteProcessId(100L)
                .setFormBindings(List.of(formBinding("FB-USER", 2001L, "USERS",
                        List.of(9001L), List.of("张三"), 1)))));

        service.saveRouteFlowConfig(reqVO);

        verify(routeCandidateConfigService).saveConfigSnapshot(eq(1002L), eq("batchUseConfigs"),
                argThat(snapshot -> snapshot instanceof List<?>
                        && snapshot.toString().contains("FB-USER")
                        && snapshot.toString().contains("executionMode=SEQUENTIAL")));
    }

    private MesProRouteVersionDO activeRouteVersion(Long versionId) {
        return MesProRouteVersionDO.builder()
                .id(versionId)
                .routeId(10L)
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .build();
    }

    private MesProRouteVersionDO draftRouteVersion(Long versionId, MesProRouteProcessDO... routeProcesses) {
        List<MesProRouteProcessDO> snapshotProcesses = routeProcesses.length == 0
                ? List.of(MesProRouteProcessDO.builder()
                        .id(100L).routeId(10L).processId(1000L).sort(1)
                        .keyFlag(Boolean.TRUE).checkFlag(Boolean.FALSE).build())
                : List.of(routeProcesses);
        return MesProRouteVersionDO.builder()
                .id(versionId)
                .routeId(10L)
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson(routeSnapshotJson(snapshotProcesses))
                .build();
    }

    private String routeSnapshotJson(List<MesProRouteProcessDO> routeProcesses) {
        StringBuilder nodes = new StringBuilder();
        for (int i = 0; i < routeProcesses.size(); i++) {
            MesProRouteProcessDO routeProcess = routeProcesses.get(i);
            if (i > 0) {
                nodes.append(",");
            }
            nodes.append("{\"routeProcessId\":").append(routeProcess.getId())
                    .append(",\"processId\":").append(routeProcess.getProcessId())
                    .append(",\"sort\":").append(routeProcess.getSort())
                    .append(",\"keyFlag\":").append(Boolean.TRUE.equals(routeProcess.getKeyFlag()))
                    .append(",\"checkFlag\":").append(Boolean.TRUE.equals(routeProcess.getCheckFlag()))
                    .append("}");
        }
        return """
                {
                  "routeId": 10,
                  "configSnapshots": {
                    "flowGraph": {
                      "nodes": [%s]
                    }
                  }
                }
                """.formatted(nodes);
    }

}
