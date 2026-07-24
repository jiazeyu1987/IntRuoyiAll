package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalPolicyDO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormPolicyRespVO;
import cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalPolicyMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionPolicyMapper;
import cn.iocoder.yudao.module.bpm.formcenter.runtime.FormCenterRuntimeService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrProcessFormPermissionRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MesProRouteVersionPublishProjectionServiceImplTest {

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
    private FormActionPolicyMapper formActionPolicyMapper;
    @Mock
    private BusinessApprovalPolicyMapper businessApprovalPolicyMapper;
    @Mock
    private MesProEdhrProcessFormPermissionRuleMapper processFormPermissionRuleMapper;

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void projectCandidate_shouldPreserveProcessFlagsFromSnapshotNodes() {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(9201L)
                .routeId(9001L)
                .versionNo("V2")
                .routeSnapshotJson("""
                        {
                          "routeId": 9001,
                          "routeCode": "RT-PUBLISH-FLAGS",
                          "routeName": "发布保留工序标记",
                          "status": 1,
                          "configSnapshots": {
                            "flowGraph": {
                              "graphVersion": 3,
                              "nodes": [
                                {
                                  "routeProcessId": 9301,
                                  "processId": 9401,
                                  "sort": 1,
                                  "keyFlag": true,
                                  "checkFlag": true
                                }
                              ],
                              "edges": [],
                              "boundaryEdges": [],
                              "layouts": []
                            },
                            "products": [],
                            "productBoms": [],
                            "scheduleConfigs": [],
                            "batchUseConfigs": [],
                            "scheduleUseConfigs": []
                          }
                        }
                        """)
                .build();
        doAnswer(invocation -> {
            MesProRouteProcessDO process = invocation.getArgument(0);
            process.setId(9501L);
            return 1;
        }).when(routeProcessMapper).insert(any(MesProRouteProcessDO.class));

        service.projectCandidate(candidate);

        ArgumentCaptor<MesProRouteProcessDO> processCaptor =
                ArgumentCaptor.forClass(MesProRouteProcessDO.class);
        verify(routeProcessMapper).insert(processCaptor.capture());
        MesProRouteProcessDO inserted = processCaptor.getValue();
        assertEquals(Boolean.TRUE, inserted.getKeyFlag());
        assertEquals(Boolean.TRUE, inserted.getCheckFlag());
    }

    @Test
    void projectCandidate_shouldProjectDynamicFormBindingFillerRule() {
        TenantContextHolder.setTenantId(122L);
        FormPolicyRespVO policy = new FormPolicyRespVO();
        policy.setId(9901L);
        when(formActionPolicyMapper.selectPublishedByAction(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(businessApprovalPolicyMapper.selectPublishedByAction(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(formCenterRuntimeService.savePolicy(any())).thenReturn(policy);
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(9202L)
                .routeId(9002L)
                .versionNo("V3")
                .routeSnapshotJson("""
                        {
                          "routeId": 9002,
                          "routeCode": "RT-PUBLISH-BATCH-DEFAULTS",
                          "routeName": "发布批记录默认字段",
                          "status": 1,
                          "configSnapshots": {
                            "flowGraph": {
                              "graphVersion": 3,
                              "nodes": [
                                {
                                  "routeProcessId": 9302,
                                  "processId": 9402,
                                  "sort": 1
                                }
                              ],
                              "edges": [],
                              "boundaryEdges": [],
                              "layouts": []
                            },
                            "products": [],
                            "productBoms": [],
                            "scheduleConfigs": [],
                            "batchUseConfigs": [
                              {
                                "routeProcessId": 9302,
                                "executionMode": "SEQUENTIAL",
                                "formBindings": [
                                  {
                                    "formBindingKey": "FB-IPQC",
                                    "formTemplateId": 2001,
                                    "formTemplateName": "过程检验表",
                                    "lastPublishedTemplateVersionId": 3001,
                                    "lastPublishedTemplateVersionNo": "V1",
                                    "instanceScope": "BATCH_SHARED",
                                    "sharedFormKey": "IPQC_SHARED",
                                    "fillableScopeJson": "{\\"ranges\\":[{\\"sourceTableIndex\\":0,\\"startRow\\":0,\\"endRow\\":1}]}",
                                    "recordCategory": "INTERNAL_RECORD",
                                    "validationProfile": "INTERNAL_TRACE",
                                    "requiredPolicy": "OPTIONAL",
                                    "ownerRoleKey": "QUALITY",
                                    "archiveVisibility": "FINAL_DHR",
                                    "candidateSourceType": "ROLE",
                                    "candidateSourceIds": [8001],
                                    "candidateSourceNames": ["生产角色"],
                                    "reportSort": 2
                                  }
                                ]
                              }
                            ],
                            "scheduleUseConfigs": []
                          }
                        }
                        """)
                .build();
        doAnswer(invocation -> {
            MesProRouteProcessDO process = invocation.getArgument(0);
            process.setId(9502L);
            return 1;
        }).when(routeProcessMapper).insert(any(MesProRouteProcessDO.class));

        service.projectCandidate(candidate);

        ArgumentCaptor<MesProRouteFlowProcessBatchRecordDO> batchRecordCaptor =
                ArgumentCaptor.forClass(MesProRouteFlowProcessBatchRecordDO.class);
        verify(routeFlowProcessBatchRecordMapper).insert(batchRecordCaptor.capture());
        MesProRouteFlowProcessBatchRecordDO inserted = batchRecordCaptor.getValue();
        assertEquals(null, inserted.getBatchRecordReportId());
        assertEquals("FB-IPQC", inserted.getFormBindingKey());
        assertEquals(2001L, inserted.getFormTemplateId());
        assertEquals("BATCH_SHARED", inserted.getInstanceScope());
        assertEquals("INTERNAL_RECORD", inserted.getRecordCategory());
        assertEquals("INTERNAL_TRACE", inserted.getValidationProfile());
        assertEquals("OPTIONAL", inserted.getRequiredPolicy());
        assertEquals("QUALITY", inserted.getOwnerRoleKey());
        assertEquals("FINAL_DHR", inserted.getArchiveVisibility());
        assertEquals("ROLE", inserted.getCandidateSourceType());
        assertEquals("8001", inserted.getCandidateSourceIds());
        assertEquals("[\"生产角色\"]", inserted.getCandidateSourceNames());

        ArgumentCaptor<MesProEdhrProcessFormPermissionRuleDO> ruleCaptor =
                ArgumentCaptor.forClass(MesProEdhrProcessFormPermissionRuleDO.class);
        verify(processFormPermissionRuleMapper).physicalDeleteByRouteProcessAndReport(9502L, "FB-IPQC");
        verify(processFormPermissionRuleMapper).insert(ruleCaptor.capture());
        MesProEdhrProcessFormPermissionRuleDO rule = ruleCaptor.getValue();
        assertEquals(9502L, rule.getRouteProcessId());
        assertEquals("FB-IPQC", rule.getBatchRecordReportId());
        assertEquals("FILL", rule.getRuleType());
        assertEquals("ROLE", rule.getCandidateSourceType());
        assertEquals("8001", rule.getCandidateSourceIds());
        assertEquals("ANY_ONE", rule.getCompletionPolicy());
        assertEquals(Integer.MAX_VALUE, rule.getDueMinutes());
        verify(formCenterRuntimeService).publishPolicy(9901L);
        ArgumentCaptor<BusinessApprovalPolicyDO> businessPolicyCaptor =
                ArgumentCaptor.forClass(BusinessApprovalPolicyDO.class);
        verify(businessApprovalPolicyMapper).insert(businessPolicyCaptor.capture());
        assertEquals("EDHR_RF_9202_FB-IPQC", businessPolicyCaptor.getValue().getActionCode());
        assertEquals("DIRECT", businessPolicyCaptor.getValue().getPolicyMode());
        assertEquals("MES_EDHR_ROUTE_FORM_FILL", businessPolicyCaptor.getValue().getEffectExecutorCode());
        assertEquals("PUBLISHED", businessPolicyCaptor.getValue().getStatus());
    }

    @Test
    void projectCandidate_shouldRejectDynamicFormBindingWithoutFiller() {
        TenantContextHolder.setTenantId(122L);
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(9203L)
                .routeId(9003L)
                .versionNo("V4")
                .routeSnapshotJson("""
                        {
                          "routeId": 9003,
                          "routeCode": "RT-PUBLISH-FILLER-REQUIRED",
                          "routeName": "发布缺少填写人失败",
                          "status": 1,
                          "configSnapshots": {
                            "flowGraph": {
                              "graphVersion": 3,
                              "nodes": [
                                {
                                  "routeProcessId": 9303,
                                  "processId": 9403,
                                  "sort": 1
                                }
                              ],
                              "edges": [],
                              "boundaryEdges": [],
                              "layouts": []
                            },
                            "products": [],
                            "productBoms": [],
                            "scheduleConfigs": [],
                            "batchUseConfigs": [
                              {
                                "routeProcessId": 9303,
                                "executionMode": "SEQUENTIAL",
                                "formBindings": [
                                  {
                                    "formBindingKey": "FB-MISSING-FILLER",
                                    "formTemplateId": 2002,
                                    "formTemplateName": "生产记录表",
                                    "lastPublishedTemplateVersionId": 3002,
                                    "lastPublishedTemplateVersionNo": "V1",
                                    "instanceScope": "BATCH_SHARED",
                                    "sharedFormKey": "MAIN_2002",
                                    "requiredPolicy": "REQUIRED",
                                    "candidateSourceType": null,
                                    "candidateSourceIds": [],
                                    "candidateSourceNames": [],
                                    "reportSort": 1
                                  }
                                ]
                              }
                            ],
                            "scheduleUseConfigs": []
                          }
                        }
                        """)
                .build();
        doAnswer(invocation -> {
            MesProRouteProcessDO process = invocation.getArgument(0);
            process.setId(9503L);
            return 1;
        }).when(routeProcessMapper).insert(any(MesProRouteProcessDO.class));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.projectCandidate(candidate));

        assertTrue(exception.getMessage().contains("FB-MISSING-FILLER"));
        verify(routeFlowProcessBatchRecordMapper, never()).insert(any(MesProRouteFlowProcessBatchRecordDO.class));
        verify(processFormPermissionRuleMapper, never()).insert(any(MesProEdhrProcessFormPermissionRuleDO.class));
    }
}
