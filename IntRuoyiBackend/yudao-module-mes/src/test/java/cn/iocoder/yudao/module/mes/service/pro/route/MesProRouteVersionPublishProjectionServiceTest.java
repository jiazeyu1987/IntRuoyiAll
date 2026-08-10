package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalPolicyDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalPolicyMapper;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicySlot;
import cn.iocoder.yudao.module.bpm.formcenter.runtime.FormCenterRuntimeService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDefectReasonDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowBoundaryEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowLayoutDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrProcessFormPermissionRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDefectReasonMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDeviceParameterRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowBoundaryEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowLayoutMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteVersionPublishProjectionServiceTest {

    @InjectMocks
    private MesProRouteVersionPublishProjectionServiceImpl service;

    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProRouteProcessFlowEdgeMapper flowEdgeMapper;
    @Mock
    private MesProRouteProcessFlowBoundaryEdgeMapper boundaryEdgeMapper;
    @Mock
    private MesProRouteProcessFlowLayoutMapper flowLayoutMapper;
    @Mock
    private MesProRouteProductMapper routeProductMapper;
    @Mock
    private MesProRouteProductBomMapper routeProductBomMapper;
    @Mock
    private MesProRouteScheduleConfigMapper routeScheduleConfigMapper;
    @Mock
    private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Mock
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Mock
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Mock
    private MesProProcessMapper processMapper;
    @Mock
    private MesMdItemMapper itemMapper;
    @Mock
    private FormCenterRuntimeService formCenterRuntimeService;
    @Mock
    private BusinessApprovalPolicyMapper businessApprovalPolicyMapper;
    @Mock
    private MesProEdhrProcessFormPermissionRuleMapper processFormPermissionRuleMapper;
    @Mock
    private MesProcessPoolDefectReasonMapper defectReasonMapper;
    @Mock
    private MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper;

    @BeforeEach
    void setUpIds() {
        AtomicLong routeProcessIds = new AtomicLong(3000L);
        lenient().when(defectReasonMapper.selectList(any())).thenReturn(List.of());
        lenient().when(parameterRuleMapper.selectList(any())).thenReturn(List.of());
        lenient().when(routeProcessMapper.insert(any(MesProRouteProcessDO.class))).thenAnswer(invocation -> {
            MesProRouteProcessDO row = invocation.getArgument(0);
            row.setId(routeProcessIds.incrementAndGet());
            return 1;
        });
        lenient().when(routeFlowConfigMapper.insert(any(MesProRouteFlowConfigDO.class))).thenAnswer(invocation -> {
            MesProRouteFlowConfigDO row = invocation.getArgument(0);
            row.setId(4001L);
            return 1;
        });
        lenient().when(routeFlowProcessConfigMapper.insert(any(MesProRouteFlowProcessConfigDO.class))).thenAnswer(invocation -> {
            MesProRouteFlowProcessConfigDO row = invocation.getArgument(0);
            row.setId(5001L);
            return 1;
        });
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void projectCandidate_shouldRejectSnapshotWithoutFrozenFlowNodes() {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(2302L)
                .routeId(9301L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 9301,
                          "routeCode": "RT-9301-V2",
                          "routeName": "缺少冻结工序节点的候选",
                          "status": 0,
                          "configSnapshots": {
                            "flowGraph": {
                              "graphVersion": 11,
                              "edges": []
                            },
                            "products": [],
                            "scheduleConfigs": [],
                            "scheduleUseConfigs": [],
                            "batchUseConfigs": []
                          }
                        }
                        """)
                .build();
        lenient().when(routeProcessMapper.selectListByRouteId(9301L)).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(1001L).processId(401L).sort(1).build()));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.projectCandidate(candidate));

        assertEquals("flowGraph nodes are required", thrown.getMessage());
        verify(routeMapper, never()).updateById(any(MesProRouteDO.class));
        verify(routeProcessMapper, never()).deleteByRouteId(9301L);
        verify(routeProcessMapper, never()).selectListByRouteId(9301L);
    }

    @Test
    void projectCandidate_shouldPreserveFrozenRouteProcessWorkstationBinding() {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(2402L)
                .routeId(9401L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 9401,
                          "routeCode": "RT-9401-V2",
                          "routeName": "正式工作站绑定路线",
                          "status": 0,
                          "configSnapshots": {
                            "flowGraph": {
                              "graphVersion": 12,
                              "nodes": [
                                {
                                  "routeProcessId": 928609,
                                  "processId": 922985,
                                  "routeProcessWorkstationId": 980010,
                                  "workstationId": 922757,
                                  "sort": 1,
                                  "keyFlag": false,
                                  "checkFlag": false
                                }
                              ],
                              "edges": []
                            },
                            "products": [],
                            "scheduleConfigs": [],
                            "scheduleUseConfigs": [],
                            "batchUseConfigs": []
                          }
                        }
                        """)
                .build();

        service.projectCandidate(candidate);

        ArgumentCaptor<MesProRouteProcessDO> processCaptor =
                ArgumentCaptor.forClass(MesProRouteProcessDO.class);
        verify(routeProcessMapper).insert(processCaptor.capture());
        assertEquals(9401L, processCaptor.getValue().getRouteId());
        assertEquals(922985L, processCaptor.getValue().getProcessId());
        assertEquals(980010L, processCaptor.getValue().getWorkstationId());
    }

    @Test
    void projectCandidate_shouldInheritTeamLeaderLossReasonsAndDeviceParameterRulesToNewRouteProcessIds() {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(2502L)
                .routeId(9501L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH)
                .routeSnapshotJson("""
                        {
                          "routeId": 9501,
                          "routeCode": "RT-9501-V2",
                          "routeName": "生产组长配置继承路线",
                          "status": 0,
                          "configSnapshots": {
                            "flowGraph": {
                              "graphVersion": 13,
                              "nodes": [
                                {
                                  "routeProcessId": 8101,
                                  "processId": 701,
                                  "routeProcessWorkstationId": 980101,
                                  "sort": 1,
                                  "keyFlag": false,
                                  "checkFlag": false
                                }
                              ],
                              "edges": []
                            },
                            "products": [],
                            "scheduleConfigs": [],
                            "scheduleUseConfigs": [],
                            "batchUseConfigs": []
                          }
                        }
                        """)
                .build();
        lenient().when(defectReasonMapper.selectList(any())).thenReturn(List.of(
                MesProcessPoolDefectReasonDO.builder()
                        .id(7101L)
                        .routeProcessId(8101L)
                        .processId(701L)
                        .reasonType(MesProcessPoolDefectReasonDO.REASON_TYPE_LOSS)
                        .reasonCode("LOSS-8101-001")
                        .reasonName("装配不到位")
                        .enabled(Boolean.TRUE)
                        .remark("旧路线工序损耗原因")
                        .build()));
        lenient().when(parameterRuleMapper.selectList(any())).thenReturn(List.of(
                MesProcessPoolDeviceParameterRuleDO.builder()
                        .id(7201L)
                        .routeProcessId(8101L)
                        .processId(701L)
                        .deviceId(4401L)
                        .parameterCode("PRESSURE")
                        .parameterName("撤压检测压力")
                        .unit("ATM")
                        .lowerLimit(new BigDecimal("20"))
                        .upperLimit(new BigDecimal("25"))
                        .defaultValue(new BigDecimal("22.5"))
                        .valueType(MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_DECIMAL)
                        .decimalScale(1)
                        .enabled(Boolean.TRUE)
                        .build()));

        service.projectCandidate(candidate);

        ArgumentCaptor<MesProcessPoolDefectReasonDO> lossCaptor =
                ArgumentCaptor.forClass(MesProcessPoolDefectReasonDO.class);
        verify(defectReasonMapper).insert(lossCaptor.capture());
        assertEquals(null, lossCaptor.getValue().getId());
        assertEquals(3001L, lossCaptor.getValue().getRouteProcessId());
        assertEquals(701L, lossCaptor.getValue().getProcessId());
        assertEquals(MesProcessPoolDefectReasonDO.REASON_TYPE_LOSS, lossCaptor.getValue().getReasonType());
        assertEquals("LOSS-8101-001", lossCaptor.getValue().getReasonCode());
        assertEquals("装配不到位", lossCaptor.getValue().getReasonName());

        ArgumentCaptor<MesProcessPoolDeviceParameterRuleDO> parameterCaptor =
                ArgumentCaptor.forClass(MesProcessPoolDeviceParameterRuleDO.class);
        verify(parameterRuleMapper).insert(parameterCaptor.capture());
        assertEquals(null, parameterCaptor.getValue().getId());
        assertEquals(3001L, parameterCaptor.getValue().getRouteProcessId());
        assertEquals(701L, parameterCaptor.getValue().getProcessId());
        assertEquals(4401L, parameterCaptor.getValue().getDeviceId());
        assertEquals("PRESSURE", parameterCaptor.getValue().getParameterCode());
        assertEquals("撤压检测压力", parameterCaptor.getValue().getParameterName());
        assertEquals(0, parameterCaptor.getValue().getLowerLimit().compareTo(new BigDecimal("20")));
        assertEquals(0, parameterCaptor.getValue().getUpperLimit().compareTo(new BigDecimal("25")));
    }

    @Test
    void projectCandidate_shouldNotInheritTeamLeaderConfigsFromClientRouteProcessId() {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(2503L)
                .routeId(9502L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH)
                .routeSnapshotJson("""
                        {
                          "routeId": 9502,
                          "routeCode": "RT-9502-V2",
                          "routeName": "临时工序 ID 不继承配置",
                          "status": 0,
                          "configSnapshots": {
                            "flowGraph": {
                              "graphVersion": 13,
                              "nodes": [
                                {
                                  "routeProcessId": 8102,
                                  "clientRouteProcessId": 9902,
                                  "processId": 702,
                                  "routeProcessWorkstationId": 980102,
                                  "sort": 1,
                                  "keyFlag": false,
                                  "checkFlag": false
                                }
                              ],
                              "edges": []
                            },
                            "products": [],
                            "scheduleConfigs": [],
                            "scheduleUseConfigs": [],
                            "batchUseConfigs": []
                          }
                        }
                        """)
                .build();
        when(defectReasonMapper.selectList(any())).thenReturn(List.of(), List.of(
                MesProcessPoolDefectReasonDO.builder()
                        .id(7301L)
                        .routeProcessId(9902L)
                        .processId(702L)
                        .reasonType(MesProcessPoolDefectReasonDO.REASON_TYPE_LOSS)
                        .reasonCode("LOSS-CLIENT-001")
                        .reasonName("不应继承的临时 ID 损耗原因")
                        .enabled(Boolean.TRUE)
                        .build()));
        when(parameterRuleMapper.selectList(any())).thenReturn(List.of(), List.of(
                MesProcessPoolDeviceParameterRuleDO.builder()
                        .id(7401L)
                        .routeProcessId(9902L)
                        .processId(702L)
                        .deviceId(4402L)
                        .parameterCode("CLIENT_ONLY")
                        .parameterName("不应继承的临时 ID 参数")
                        .valueType(MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_TEXT_STANDARD)
                        .standardText("禁止继承")
                        .enabled(Boolean.TRUE)
                        .build()));

        service.projectCandidate(candidate);

        verify(defectReasonMapper, never()).insert(any(MesProcessPoolDefectReasonDO.class));
        verify(parameterRuleMapper, never()).insert(any(MesProcessPoolDeviceParameterRuleDO.class));
    }

    @Test
    void projectCandidate_shouldProjectRouteProcessesFlowProductsAndBatchUseConfig() {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(2002L)
                .routeId(9001L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 9001,
                          "routeCode": "RT-9001-V2",
                          "routeName": "V2 路线",
                          "status": 0,
                          "configSnapshots": {
                            "flowGraph": {
                              "graphVersion": 7,
                              "nodes": [
                                {"sort": 1, "processName": "称量"},
                                {"sort": 2, "processName": "混合"}
                              ],
                              "edges": [
                                {"sourceSort": 1, "targetSort": 2, "relationType": "NORMAL", "sort": 1}
                              ]
                            },
                            "products": ["产品A"],
                            "scheduleConfigs": [],
                            "batchUseConfigs": [
                              {
                                "sort": 1,
                                "processName": "称量",
                                "useType": "BATCH",
                                 "enabled": true,
                                 "executionMode": "SEQUENTIAL",
                                 "productionQuantityFactor": 1.25,
                                 "formBindings": [
                                   {
                                     "formBindingKey": "FB-001",
                                     "formTemplateId": 2001,
                                     "formTemplateName": "生产记录表",
                                     "lastPublishedTemplateVersionId": 3001,
                                     "lastPublishedTemplateVersionNo": "V1",
                                     "instanceScope": "BATCH_SHARED",
                                     "sharedFormKey": "process-inspection-v1",
                                     "fillableScopeJson": "{\\"ranges\\":[{\\"sourceTableIndex\\":0,\\"startRow\\":0,\\"endRow\\":1}]}",
                                     "recordCategory": "BATCH_RECORD",
                                     "validationProfile": "CONTROLLED_BATCH",
                                     "permissionScopeId": 5001,
                                     "recordCategorySnapshotHash": "2222222222222222222222222222222222222222222222222222222222222222",
                                     "requiredPolicy": "REQUIRED",
                                     "requiredConditionJson": null,
                                     "ownerRoleKey": "PRODUCTION",
                                     "archiveVisibility": "FINAL_DHR",
                                     "candidateSourceType": "USERS",
                                     "candidateSourceIds": [9001],
                                     "candidateSourceNames": ["张三"],
                                     "slotConfigSnapshotHash": "1111111111111111111111111111111111111111111111111111111111111111",
                                     "reportSort": 1
                                   }
                                 ]
                               }
                            ]
                          }
                        }
                        """)
                .build();
        when(processMapper.selectByName("称量")).thenReturn(MesProProcessDO.builder().id(101L).name("称量").build());
        when(processMapper.selectByName("混合")).thenReturn(MesProProcessDO.builder().id(102L).name("混合").build());
        when(itemMapper.selectListByName("产品A")).thenReturn(List.of(MesMdItemDO.builder().id(501L).name("产品A").build()));
        TenantContextHolder.setTenantId(122L);
        when(businessApprovalPolicyMapper.selectPublishedByAction(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        service.projectCandidate(candidate);

        ArgumentCaptor<MesProRouteDO> routeCaptor = ArgumentCaptor.forClass(MesProRouteDO.class);
        verify(routeMapper).updateById(routeCaptor.capture());
        assertEquals(9001L, routeCaptor.getValue().getId());
        assertEquals("RT-9001-V2", routeCaptor.getValue().getCode());
        assertEquals("V2 路线", routeCaptor.getValue().getName());

        verify(routeProcessMapper).deleteByRouteId(9001L);
        ArgumentCaptor<MesProRouteProcessDO> processCaptor = ArgumentCaptor.forClass(MesProRouteProcessDO.class);
        verify(routeProcessMapper, org.mockito.Mockito.times(2)).insert(processCaptor.capture());
        assertEquals(101L, processCaptor.getAllValues().get(0).getProcessId());
        assertEquals(102L, processCaptor.getAllValues().get(1).getProcessId());

        verify(flowEdgeMapper).deleteByRouteId(9001L);
        ArgumentCaptor<MesProRouteProcessFlowEdgeDO> edgeCaptor =
                ArgumentCaptor.forClass(MesProRouteProcessFlowEdgeDO.class);
        verify(flowEdgeMapper).insert(edgeCaptor.capture());
        assertEquals(7L, edgeCaptor.getValue().getGraphVersion());
        assertEquals("NORMAL", edgeCaptor.getValue().getRelationType());

        verify(routeProductMapper).deleteByRouteId(9001L);
        ArgumentCaptor<MesProRouteProductDO> productCaptor = ArgumentCaptor.forClass(MesProRouteProductDO.class);
        verify(routeProductMapper).insert(productCaptor.capture());
        assertEquals(501L, productCaptor.getValue().getItemId());

        verify(routeFlowConfigMapper).deleteByRouteIdAndUseType(9001L, "BATCH");
        ArgumentCaptor<MesProRouteFlowProcessConfigDO> processConfigCaptor =
                ArgumentCaptor.forClass(MesProRouteFlowProcessConfigDO.class);
        verify(routeFlowProcessConfigMapper).insert(processConfigCaptor.capture());
        assertEquals("BATCH", processConfigCaptor.getValue().getUseType());
        assertEquals("SEQUENTIAL", processConfigCaptor.getValue().getExecutionMode());

        ArgumentCaptor<MesProRouteFlowProcessBatchRecordDO> batchRecordCaptor =
                ArgumentCaptor.forClass(MesProRouteFlowProcessBatchRecordDO.class);
        verify(routeFlowProcessBatchRecordMapper).insert(batchRecordCaptor.capture());
        assertEquals(null, batchRecordCaptor.getValue().getBatchRecordReportId());
        assertEquals("FB-001", batchRecordCaptor.getValue().getFormBindingKey());
        assertEquals(2001L, batchRecordCaptor.getValue().getFormTemplateId());
        assertEquals("生产记录表", batchRecordCaptor.getValue().getFormTemplateNameSnapshot());
        assertEquals(3001L, batchRecordCaptor.getValue().getLastPublishedTemplateVersionId());
        assertEquals("BATCH_SHARED", batchRecordCaptor.getValue().getInstanceScope());
        assertEquals("process-inspection-v1", batchRecordCaptor.getValue().getSharedFormKey());
        assertEquals("{\"ranges\":[{\"sourceTableIndex\":0,\"startRow\":0,\"endRow\":1}]}",
                batchRecordCaptor.getValue().getFillableScopeJson());
        assertEquals("BATCH_RECORD", batchRecordCaptor.getValue().getRecordCategory());
        assertEquals("CONTROLLED_BATCH", batchRecordCaptor.getValue().getValidationProfile());
        assertEquals(5001L, batchRecordCaptor.getValue().getPermissionScopeId());
        assertEquals("2222222222222222222222222222222222222222222222222222222222222222",
                batchRecordCaptor.getValue().getRecordCategorySnapshotHash());
        assertEquals("REQUIRED", batchRecordCaptor.getValue().getRequiredPolicy());
        assertEquals("PRODUCTION", batchRecordCaptor.getValue().getOwnerRoleKey());
        assertEquals("FINAL_DHR", batchRecordCaptor.getValue().getArchiveVisibility());
        assertEquals("1111111111111111111111111111111111111111111111111111111111111111",
                batchRecordCaptor.getValue().getSlotConfigSnapshotHash());
        assertEquals("USERS", batchRecordCaptor.getValue().getCandidateSourceType());
        assertEquals("9001", batchRecordCaptor.getValue().getCandidateSourceIds());
        assertEquals("[\"张三\"]", batchRecordCaptor.getValue().getCandidateSourceNames());

        ArgumentCaptor<MesProEdhrProcessFormPermissionRuleDO> ruleCaptor =
                ArgumentCaptor.forClass(MesProEdhrProcessFormPermissionRuleDO.class);
        verify(processFormPermissionRuleMapper).physicalDeleteByRouteProcessReportAndVersion(3001L, "FB-001", 2002L);
        verify(processFormPermissionRuleMapper).insert(ruleCaptor.capture());
        assertEquals(3001L, ruleCaptor.getValue().getRouteProcessId());
        assertEquals("FB-001", ruleCaptor.getValue().getBatchRecordReportId());
        assertEquals(2002L, ruleCaptor.getValue().getBatchRecordVersionId());
        assertEquals("FILL", ruleCaptor.getValue().getRuleType());
        assertEquals("USERS", ruleCaptor.getValue().getCandidateSourceType());
        assertEquals("9001", ruleCaptor.getValue().getCandidateSourceIds());
        assertEquals("ANY_ONE", ruleCaptor.getValue().getCompletionPolicy());
        assertEquals(Integer.MAX_VALUE, ruleCaptor.getValue().getDueMinutes());
        verify(formCenterRuntimeService, never()).savePolicy(any());
        verify(formCenterRuntimeService, never()).publishPolicy(any());
        ArgumentCaptor<BusinessApprovalPolicyDO> businessPolicyCaptor =
                ArgumentCaptor.forClass(BusinessApprovalPolicyDO.class);
        verify(businessApprovalPolicyMapper).insert(businessPolicyCaptor.capture());
        BusinessApprovalPolicyDO businessPolicy = businessPolicyCaptor.getValue();
        assertEquals("EDHR_RF_2002_FB-001", businessPolicy.getActionCode());
        assertEquals("DIRECT", businessPolicy.getPolicyMode());
        assertEquals("MES_EDHR_ROUTE_FORM_FILL", businessPolicy.getEffectExecutorCode());
        assertEquals("REQUIRED", businessPolicy.getFormPolicyType());
        assertEquals("PUBLISHED", businessPolicy.getStatus());
        List<FormPolicySlot> slots = JsonUtils.parseArray(businessPolicy.getFormSlotsJson(), FormPolicySlot.class);
        assertEquals("EDHR_ROUTE_FORM", slots.get(0).getSlotCode());
        assertEquals(true, slots.get(0).isRequired());
        assertEquals("2001", slots.get(0).getTemplateVersionRef().getTemplateCode());
    }

    @Test
    void projectCandidate_shouldUseSnapshotOrderWhenFlowEdgeSortMissing() {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(2022L)
                .routeId(9021L)
                .versionNo("V5")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH)
                .routeSnapshotJson("""
                        {
                          "routeId": 9021,
                          "routeCode": "RT-9021-V5",
                          "routeName": "V5 路线",
                          "status": 0,
                          "configSnapshots": {
                            "flowGraph": {
                              "graphVersion": 12,
                              "nodes": [
                                {"routeProcessId": 101, "sort": 1, "processName": "称量"},
                                {"routeProcessId": 102, "sort": 2, "processName": "混合"},
                                {"routeProcessId": 103, "sort": 3, "processName": "包装"}
                              ],
                              "edges": [
                                {"sourceSort": 1, "targetSort": 2, "relationType": "NORMAL"},
                                {"sourceSort": 2, "targetSort": 3, "relationType": "NORMAL"}
                              ]
                            },
                            "products": [],
                            "scheduleConfigs": [],
                            "scheduleUseConfigs": [],
                            "batchUseConfigs": []
                          }
                        }
                        """)
                .build();
        when(processMapper.selectByName("称量")).thenReturn(MesProProcessDO.builder().id(101L).name("称量").build());
        when(processMapper.selectByName("混合")).thenReturn(MesProProcessDO.builder().id(102L).name("混合").build());
        when(processMapper.selectByName("包装")).thenReturn(MesProProcessDO.builder().id(103L).name("包装").build());

        service.projectCandidate(candidate);

        ArgumentCaptor<MesProRouteProcessFlowEdgeDO> edgeCaptor =
                ArgumentCaptor.forClass(MesProRouteProcessFlowEdgeDO.class);
        verify(flowEdgeMapper, org.mockito.Mockito.times(2)).insert(edgeCaptor.capture());
        assertEquals(1, edgeCaptor.getAllValues().get(0).getSort());
        assertEquals(2, edgeCaptor.getAllValues().get(1).getSort());
        assertEquals(3001L, edgeCaptor.getAllValues().get(0).getSourceRouteProcessId());
        assertEquals(3002L, edgeCaptor.getAllValues().get(0).getTargetRouteProcessId());
        assertEquals(3002L, edgeCaptor.getAllValues().get(1).getSourceRouteProcessId());
        assertEquals(3003L, edgeCaptor.getAllValues().get(1).getTargetRouteProcessId());
    }

    @Test
    void projectCandidate_shouldResolveGeneratedRouteProcessIdsBeforeProjectingEdges() {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(2032L)
                .routeId(9031L)
                .versionNo("V5")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH)
                .routeSnapshotJson("""
                        {
                          "routeId": 9031,
                          "routeCode": "RT-9031-V5",
                          "routeName": "V5 路线",
                          "status": 0,
                          "configSnapshots": {
                            "flowGraph": {
                              "graphVersion": 13,
                              "nodes": [
                                {"routeProcessId": 922483, "sort": 1, "processId": 401},
                                {"routeProcessId": 922484, "sort": 2, "processId": 402}
                              ],
                              "edges": [
                                {"sourceRouteProcessId": 922483, "targetRouteProcessId": 922484, "relationType": "NORMAL"}
                              ]
                            },
                            "products": [],
                            "scheduleConfigs": [],
                            "scheduleUseConfigs": [],
                            "batchUseConfigs": []
                          }
                        }
                        """)
                .build();
        org.mockito.Mockito.doAnswer(invocation -> 1)
                .when(routeProcessMapper).insert(any(MesProRouteProcessDO.class));
        when(routeProcessMapper.selectByRouteIdAndSort(9031L, 1))
                .thenReturn(MesProRouteProcessDO.builder().id(6101L).routeId(9031L).processId(401L).sort(1).build());
        when(routeProcessMapper.selectByRouteIdAndSort(9031L, 2))
                .thenReturn(MesProRouteProcessDO.builder().id(6102L).routeId(9031L).processId(402L).sort(2).build());

        service.projectCandidate(candidate);

        ArgumentCaptor<MesProRouteProcessFlowEdgeDO> edgeCaptor =
                ArgumentCaptor.forClass(MesProRouteProcessFlowEdgeDO.class);
        verify(flowEdgeMapper).insert(edgeCaptor.capture());
        assertEquals(6101L, edgeCaptor.getValue().getSourceRouteProcessId());
        assertEquals(6102L, edgeCaptor.getValue().getTargetRouteProcessId());
        assertEquals(1, edgeCaptor.getValue().getSort());
    }

    @Test
    void projectCandidate_shouldProjectProductObjectsFromCopiedRouteSnapshot() {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(2052L)
                .routeId(9051L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 9051,
                          "routeCode": "RT-9051-V2",
                          "routeName": "复制路线 V2",
                          "status": 0,
                          "configSnapshots": {
                            "flowGraph": {
                              "graphVersion": 1,
                              "nodes": [
                                {"sort": 1, "processName": "称量"}
                              ],
                              "edges": []
                            },
                            "products": [
                              {
                                "itemId": 922280,
                                "quantity": 1000,
                                "productionTime": 1.5,
                                "timeUnitType": "DAY",
                                "remark": "复制路线产品快照"
                              }
                            ],
                            "scheduleConfigs": [],
                            "batchUseConfigs": []
                          }
                        }
                        """)
                .build();
        when(processMapper.selectByName("称量")).thenReturn(MesProProcessDO.builder().id(101L).name("称量").build());

        service.projectCandidate(candidate);

        ArgumentCaptor<MesProRouteProductDO> productCaptor = ArgumentCaptor.forClass(MesProRouteProductDO.class);
        verify(routeProductMapper).insert(productCaptor.capture());
        assertEquals(922280L, productCaptor.getValue().getItemId());
        assertEquals(1000, productCaptor.getValue().getQuantity());
        assertEquals("DAY", productCaptor.getValue().getTimeUnitType());
        assertEquals("复制路线产品快照", productCaptor.getValue().getRemark());
        verify(itemMapper, never()).selectListByName(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void projectCandidate_shouldProjectScheduleBoundaryAndLayoutSnapshots() {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(2102L)
                .routeId(9101L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 9101,
                          "routeCode": "RT-9101-V2",
                          "routeName": "V2 路线",
                          "status": 0,
                          "configSnapshots": {
                            "flowGraph": {
                              "graphVersion": 8,
                              "nodes": [
                                {"sort": 1, "processName": "配液"},
                                {"sort": 2, "processName": "灌装"}
                              ],
                              "edges": [
                                {"sourceSort": 1, "targetSort": 2, "relationType": "NORMAL", "sort": 1}
                              ],
                              "boundaryEdges": [
                                {"boundaryType": "START", "routeProcessSort": 1, "sort": 1},
                                {"boundaryType": "END", "routeProcessSort": 2, "sort": 1}
                              ],
                              "layouts": [
                                {"routeProcessSort": 1, "x": 120, "y": 80, "width": 180, "height": 72},
                                {"routeProcessSort": 2, "x": 380, "y": 80, "width": 180, "height": 72}
                              ]
                            },
                            "products": [],
                            "scheduleConfigs": [
                              {
                                "sort": 1,
                                "capacityMode": "FINITE_HOURLY",
                                "hourlyCapacity": 10.5,
                                "nightShiftEnabled": true,
                                "calendarRuleId": 301,
                                "configVersion": "CFG-V2",
                                "remark": "候选排产配置"
                              }
                            ],
                            "batchUseConfigs": []
                          }
                        }
                        """)
                .build();
        when(processMapper.selectByName("配液")).thenReturn(MesProProcessDO.builder().id(201L).name("配液").build());
        when(processMapper.selectByName("灌装")).thenReturn(MesProProcessDO.builder().id(202L).name("灌装").build());

        service.projectCandidate(candidate);

        verify(routeScheduleConfigMapper).deleteByRouteVersionId(candidate.getId());
        ArgumentCaptor<MesProRouteScheduleConfigDO> scheduleCaptor =
                ArgumentCaptor.forClass(MesProRouteScheduleConfigDO.class);
        verify(routeScheduleConfigMapper).insert(scheduleCaptor.capture());
        assertEquals(candidate.getId(), scheduleCaptor.getValue().getRouteVersionId());
        assertEquals("FINITE_HOURLY", scheduleCaptor.getValue().getCapacityMode());
        assertEquals(0, scheduleCaptor.getValue().getHourlyCapacity().compareTo(new java.math.BigDecimal("10.5")));
        assertEquals(3001L, scheduleCaptor.getValue().getRouteProcessId());

        ArgumentCaptor<MesProRouteProcessFlowBoundaryEdgeDO> boundaryCaptor =
                ArgumentCaptor.forClass(MesProRouteProcessFlowBoundaryEdgeDO.class);
        verify(boundaryEdgeMapper, org.mockito.Mockito.times(2)).insert(boundaryCaptor.capture());
        assertEquals("START", boundaryCaptor.getAllValues().get(0).getBoundaryType());
        assertEquals(3001L, boundaryCaptor.getAllValues().get(0).getRouteProcessId());
        assertEquals("END", boundaryCaptor.getAllValues().get(1).getBoundaryType());
        assertEquals(3002L, boundaryCaptor.getAllValues().get(1).getRouteProcessId());

        ArgumentCaptor<MesProRouteProcessFlowLayoutDO> layoutCaptor =
                ArgumentCaptor.forClass(MesProRouteProcessFlowLayoutDO.class);
        verify(flowLayoutMapper, org.mockito.Mockito.times(2)).insert(layoutCaptor.capture());
        assertEquals(120, layoutCaptor.getAllValues().get(0).getX());
        assertEquals(3001L, layoutCaptor.getAllValues().get(0).getRouteProcessId());
        assertEquals(380, layoutCaptor.getAllValues().get(1).getX());
        assertEquals(3002L, layoutCaptor.getAllValues().get(1).getRouteProcessId());
    }

    @Test
    void projectCandidate_shouldProjectScheduleUseConfigs() {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(2202L)
                .routeId(9201L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .routeSnapshotJson("""
                        {
                          "routeId": 9201,
                          "routeCode": "RT-9201-V2",
                          "routeName": "V2 路线",
                          "status": 0,
                          "configSnapshots": {
                            "flowGraph": {
                              "graphVersion": 9,
                              "nodes": [
                                {"sort": 1, "processName": "灭菌"}
                              ],
                              "edges": []
                            },
                            "products": [],
                            "scheduleConfigs": [],
                            "scheduleUseConfigs": [
                              {
                                "sort": 1,
                                "processName": "灭菌",
                                "useType": "SCHEDULE",
                                "enabled": false,
                                "executionMode": "PARALLEL",
                                "productionQuantityFactor": 2.50,
                                "remark": "候选智能排产用途"
                              }
                            ],
                            "batchUseConfigs": []
                          }
                        }
                        """)
                .build();
        when(processMapper.selectByName("灭菌")).thenReturn(MesProProcessDO.builder().id(301L).name("灭菌").build());

        service.projectCandidate(candidate);

        verify(routeFlowConfigMapper).deleteByRouteIdAndUseType(9201L, "SCHEDULE");
        verify(routeFlowProcessConfigMapper).deleteByRouteIdAndUseType(9201L, "SCHEDULE");
        verify(routeFlowProcessBatchRecordMapper).deleteByRouteIdAndUseType(9201L, "SCHEDULE");

        ArgumentCaptor<MesProRouteFlowConfigDO> flowConfigCaptor =
                ArgumentCaptor.forClass(MesProRouteFlowConfigDO.class);
        verify(routeFlowConfigMapper, org.mockito.Mockito.atLeastOnce()).insert(flowConfigCaptor.capture());
        MesProRouteFlowConfigDO scheduleFlow = flowConfigCaptor.getAllValues().stream()
                .filter(config -> "SCHEDULE".equals(config.getUseType()))
                .findFirst()
                .orElseThrow();
        assertEquals(9201L, scheduleFlow.getRouteId());
        assertEquals(Boolean.TRUE, scheduleFlow.getEnabled());

        ArgumentCaptor<MesProRouteFlowProcessConfigDO> processConfigCaptor =
                ArgumentCaptor.forClass(MesProRouteFlowProcessConfigDO.class);
        verify(routeFlowProcessConfigMapper, org.mockito.Mockito.atLeastOnce()).insert(processConfigCaptor.capture());
        MesProRouteFlowProcessConfigDO scheduleProcessConfig = processConfigCaptor.getAllValues().stream()
                .filter(config -> "SCHEDULE".equals(config.getUseType()))
                .findFirst()
                .orElseThrow();
        assertEquals(3001L, scheduleProcessConfig.getRouteProcessId());
        assertEquals(Boolean.FALSE, scheduleProcessConfig.getEnabled());
        assertEquals("PARALLEL", scheduleProcessConfig.getExecutionMode());
        assertEquals(0, scheduleProcessConfig.getProductionQuantityFactor().compareTo(new java.math.BigDecimal("2.50")));
        assertEquals("候选智能排产用途", scheduleProcessConfig.getRemark());
    }

    @Test
    void projectCandidate_shouldKeepBatchUseConfigWithoutBatchRecordBinding() {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(2212L)
                .routeId(9211L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH)
                .routeSnapshotJson("""
                        {
                          "routeId": 9211,
                          "routeCode": "RT-9211-V2",
                          "routeName": "V2 路线",
                          "status": 0,
                          "configSnapshots": {
                            "flowGraph": {
                              "graphVersion": 10,
                              "nodes": [
                                {"sort": 1, "processName": "包装"}
                              ],
                              "edges": []
                            },
                            "products": [],
                            "scheduleConfigs": [],
                            "batchUseConfigs": [
                              {
                                "sort": 1,
                                "processName": "包装",
                                "useType": "BATCH",
                                "enabled": true,
                                "executionMode": "SEQUENTIAL",
                                "productionQuantityFactor": 1.00,
                                "remark": "批记录暂未绑定"
                              }
                            ]
                          }
                        }
                        """)
                .build();
        when(processMapper.selectByName("包装")).thenReturn(MesProProcessDO.builder().id(302L).name("包装").build());

        service.projectCandidate(candidate);

        ArgumentCaptor<MesProRouteFlowProcessConfigDO> processConfigCaptor =
                ArgumentCaptor.forClass(MesProRouteFlowProcessConfigDO.class);
        verify(routeFlowProcessConfigMapper).insert(processConfigCaptor.capture());
        assertEquals("BATCH", processConfigCaptor.getValue().getUseType());
        assertEquals(3001L, processConfigCaptor.getValue().getRouteProcessId());
        assertEquals("SEQUENTIAL", processConfigCaptor.getValue().getExecutionMode());
        assertEquals(null, processConfigCaptor.getValue().getBatchRecordReportId());
        assertEquals("批记录暂未绑定", processConfigCaptor.getValue().getRemark());
        verify(routeFlowProcessBatchRecordMapper, never()).insert(any(MesProRouteFlowProcessBatchRecordDO.class));
    }

    @Test
    void projectCandidate_shouldIgnoreBlankBatchRecordReportPlaceholders() {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(2222L)
                .routeId(9221L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH)
                .routeSnapshotJson("""
                        {
                          "routeId": 9221,
                          "routeCode": "RT-9221-V2",
                          "routeName": "V2 路线",
                          "status": 0,
                          "configSnapshots": {
                            "flowGraph": {
                              "graphVersion": 10,
                              "nodes": [
                                {"sort": 1, "processName": "包装"}
                              ],
                              "edges": []
                            },
                            "products": [],
                            "scheduleConfigs": [],
                            "batchUseConfigs": [
                              {
                                "sort": 1,
                                "processName": "包装",
                                "useType": "BATCH",
                                "enabled": true,
                                "executionMode": "SEQUENTIAL",
                                "productionQuantityFactor": 1.00,
                                "batchRecordReports": [
                                  {
                                    "batchRecordReportId": "",
                                    "reportId": "",
                                    "formSlotType": "MAIN",
                                    "recordCategory": "BATCH_RECORD",
                                    "reportSort": 1
                                  }
                                ],
                                "remark": "批记录占位未绑定"
                              }
                            ]
                          }
                        }
                        """)
                .build();
        when(processMapper.selectByName("包装")).thenReturn(MesProProcessDO.builder().id(303L).name("包装").build());

        service.projectCandidate(candidate);

        ArgumentCaptor<MesProRouteFlowProcessConfigDO> processConfigCaptor =
                ArgumentCaptor.forClass(MesProRouteFlowProcessConfigDO.class);
        verify(routeFlowProcessConfigMapper).insert(processConfigCaptor.capture());
        assertEquals("BATCH", processConfigCaptor.getValue().getUseType());
        assertEquals(3001L, processConfigCaptor.getValue().getRouteProcessId());
        assertEquals(null, processConfigCaptor.getValue().getBatchRecordReportId());
        assertEquals("批记录占位未绑定", processConfigCaptor.getValue().getRemark());
        verify(routeFlowProcessBatchRecordMapper, never()).insert(any(MesProRouteFlowProcessBatchRecordDO.class));
    }
}
