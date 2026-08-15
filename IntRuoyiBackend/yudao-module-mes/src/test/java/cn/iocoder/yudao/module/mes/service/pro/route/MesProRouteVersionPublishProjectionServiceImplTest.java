package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalPolicyDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalPolicyMapper;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicySlot;
import cn.iocoder.yudao.module.bpm.formcenter.runtime.FormCenterRuntimeService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrProcessFormPermissionRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
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
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeDetailResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeSaveCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeService;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
    private BusinessApprovalPolicyMapper businessApprovalPolicyMapper;
    @Mock
    private MesProEdhrProcessFormPermissionRuleMapper processFormPermissionRuleMapper;
    @Mock
    private MesProcessPoolDefectReasonMapper defectReasonMapper;
    @Mock
    private MesProcessPoolDeviceParameterRuleMapper deviceParameterRuleMapper;
    @Mock
    private MesProEdhrPermissionScopeService permissionScopeService;

    @BeforeEach
    void setUpProcessPoolConfigMappers() {
        lenient().when(defectReasonMapper.selectList(any())).thenReturn(List.of());
        lenient().when(deviceParameterRuleMapper.selectList(any())).thenReturn(List.of());
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void routeFormActionCode_shouldStayWithinApprovalPolicyColumnForLongFormBindingKey() {
        String shortActionCode = MesProRouteVersionPublishProjectionServiceImpl.routeFormActionCode(
                9202L, "FB-IPQC");
        assertEquals("EDHR_RF_9202_FB-IPQC", shortActionCode);

        String actionCode = MesProRouteVersionPublishProjectionServiceImpl.routeFormActionCode(
                631L, "FORM_BINDING_AORD_1786339591064_980674_PROCESS_INSPECTION");

        assertTrue(actionCode.startsWith("EDHR_RF_631_"));
        assertTrue(actionCode.length() <= 64);
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
        when(businessApprovalPolicyMapper.selectPublishedByAction(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
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
        verify(processFormPermissionRuleMapper).physicalDeleteByRouteProcessReportAndVersion(9502L, "FB-IPQC", 9202L);
        verify(processFormPermissionRuleMapper).insert(ruleCaptor.capture());
        MesProEdhrProcessFormPermissionRuleDO rule = ruleCaptor.getValue();
        assertEquals(9502L, rule.getRouteProcessId());
        assertEquals("FB-IPQC", rule.getBatchRecordReportId());
        assertEquals(9202L, rule.getBatchRecordVersionId());
        assertEquals("FILL", rule.getRuleType());
        assertEquals("ROLE", rule.getCandidateSourceType());
        assertEquals("8001", rule.getCandidateSourceIds());
        assertEquals("ANY_ONE", rule.getCompletionPolicy());
        assertEquals(Integer.MAX_VALUE, rule.getDueMinutes());
        verify(formCenterRuntimeService, never()).savePolicy(any());
        verify(formCenterRuntimeService, never()).publishPolicy(any());
        ArgumentCaptor<BusinessApprovalPolicyDO> businessPolicyCaptor =
                ArgumentCaptor.forClass(BusinessApprovalPolicyDO.class);
        verify(businessApprovalPolicyMapper).insert(businessPolicyCaptor.capture());
        BusinessApprovalPolicyDO businessPolicy = businessPolicyCaptor.getValue();
        assertEquals("EDHR_RF_9202_FB-IPQC", businessPolicy.getActionCode());
        assertEquals("DIRECT", businessPolicy.getPolicyMode());
        assertEquals("MES_EDHR_ROUTE_FORM_FILL", businessPolicy.getEffectExecutorCode());
        assertEquals("REQUIRED", businessPolicy.getFormPolicyType());
        assertEquals("PUBLISHED", businessPolicy.getStatus());
        List<FormPolicySlot> slots = JsonUtils.parseArray(businessPolicy.getFormSlotsJson(), FormPolicySlot.class);
        assertEquals("EDHR_ROUTE_FORM", slots.get(0).getSlotCode());
        assertEquals(false, slots.get(0).isRequired());
        assertEquals("2001", slots.get(0).getTemplateVersionRef().getTemplateCode());
    }

    @Test
    void projectCandidate_shouldRestoreMainBatchRecordAndKeepLossFormInIndependentSlot() {
        TenantContextHolder.setTenantId(122L);
        when(businessApprovalPolicyMapper.selectPublishedByAction(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(9204L)
                .routeId(9004L)
                .versionNo("V5")
                .routeSnapshotJson("""
                        {
                          "routeId": 9004,
                          "routeCode": "RT-PUBLISH-LEGACY-MAIN",
                          "routeName": "发布保留主批记录",
                          "status": 1,
                          "candidateSource": "EDHR_WORD_IMPORT",
                          "configSnapshots": {
                            "flowGraph": {
                              "graphVersion": 3,
                              "nodes": [
                                {
                                  "routeProcessId": 9304,
                                  "processId": 9404,
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
                                "routeProcessId": 9304,
                                "executionMode": "SEQUENTIAL",
                                "batchRecordReports": [
                                  {
                                    "batchRecordReportId": "REPORT-MAIN",
                                    "batchRecordDefinitionId": 1001,
                                    "batchRecordVersionId": 2001,
                                    "formSlotType": "MAIN",
                                    "instanceScope": "PROCESS",
                                    "recordCategory": "BATCH_RECORD",
                                    "validationProfile": "CONTROLLED_BATCH",
                                    "permissionScopeId": 3001,
                                    "recordCategorySnapshotHash": "record-hash",
                                    "requiredPolicy": "REQUIRED",
                                    "ownerRoleKey": "PRODUCTION",
                                    "archiveVisibility": "FINAL_DHR",
                                    "slotConfigSnapshotHash": "slot-hash",
                                    "reportSort": 1,
                                    "remark": "主批记录"
                                  }
                                ],
                                "formBindings": [
                                  {
                                    "formBindingKey": "FB-LOSS",
                                    "formTemplateId": 2002,
                                    "formTemplateName": "损耗单",
                                    "formSlotType": "LOSS_REPORT",
                                    "lastPublishedTemplateVersionId": 3002,
                                    "lastPublishedTemplateVersionNo": "V2",
                                    "instanceScope": "BATCH_SHARED",
                                    "sharedFormKey": "LOSS_SHARED",
                                    "recordCategory": "INTERNAL_RECORD",
                                    "validationProfile": "INTERNAL_TRACE",
                                    "requiredPolicy": "OPTIONAL",
                                    "ownerRoleKey": "PRODUCTION",
                                    "archiveVisibility": "FINAL_DHR",
                                    "candidateSourceType": "USERS",
                                    "candidateSourceIds": [8002],
                                    "candidateSourceNames": ["生产人员"],
                                    "reportSort": 1
                                  }
                                ]
                              }
                            ],
                            "routeStartProductionLeaders": [
                              {
                                "candidateSourceType": "USERS",
                                "candidateSourceIds": [8001],
                                "sort": 1
                              }
                            ],
                            "batchRecordAttachmentOwners": [],
                            "scheduleUseConfigs": []
                          }
                        }
                        """)
                .build();
        doAnswer(invocation -> {
            MesProRouteProcessDO process = invocation.getArgument(0);
            process.setId(9504L);
            return 1;
        }).when(routeProcessMapper).insert(any(MesProRouteProcessDO.class));

        service.projectCandidate(candidate);

        ArgumentCaptor<MesProRouteProcessDO> routeProcessUpdateCaptor =
                ArgumentCaptor.forClass(MesProRouteProcessDO.class);
        verify(routeProcessMapper).updateById(routeProcessUpdateCaptor.capture());
        assertEquals(9504L, routeProcessUpdateCaptor.getValue().getId());
        assertEquals("REPORT-MAIN", routeProcessUpdateCaptor.getValue().getBatchRecordReportId());

        ArgumentCaptor<MesProRouteFlowProcessBatchRecordDO> recordCaptor =
                ArgumentCaptor.forClass(MesProRouteFlowProcessBatchRecordDO.class);
        verify(routeFlowProcessBatchRecordMapper, times(2)).insert(recordCaptor.capture());
        MesProRouteFlowProcessBatchRecordDO mainRecord = recordCaptor.getAllValues().get(0);
        assertEquals("REPORT-MAIN", mainRecord.getBatchRecordReportId());
        assertEquals("MAIN", mainRecord.getFormSlotType());
        assertEquals(1, mainRecord.getReportSort());
        assertEquals(1001L, mainRecord.getBatchRecordDefinitionId());
        assertEquals(2001L, mainRecord.getBatchRecordVersionId());
        assertEquals(3001L, mainRecord.getPermissionScopeId());
        assertEquals("record-hash", mainRecord.getRecordCategorySnapshotHash());
        assertEquals("slot-hash", mainRecord.getSlotConfigSnapshotHash());

        MesProRouteFlowProcessBatchRecordDO lossForm = recordCaptor.getAllValues().get(1);
        assertEquals(null, lossForm.getBatchRecordReportId());
        assertEquals("FB-LOSS", lossForm.getFormBindingKey());
        assertEquals("LOSS_REPORT", lossForm.getFormSlotType());
        assertEquals("INTERNAL_RECORD", lossForm.getRecordCategory());
        assertEquals("INTERNAL_TRACE", lossForm.getValidationProfile());
        assertEquals(2, lossForm.getReportSort());

        JSONObject configSnapshots = JSONObject.parseObject(candidate.getRouteSnapshotJson())
                .getJSONObject("configSnapshots");
        assertEquals(1, configSnapshots.getJSONArray("routeStartProductionLeaders").size());
        assertTrue(configSnapshots.getJSONArray("batchRecordAttachmentOwners").isEmpty());
        assertFalse(configSnapshots.containsKey("routeEndBindings"));
        assertFalse(configSnapshots.containsKey("processEndBindings"));
    }

    @Test
    void projectCandidate_whenNewEdhrProcessUsesClientReference_createsFormalPermissionScope() {
        when(permissionScopeService.saveRules(any(MesProEdhrPermissionScopeSaveCommand.class)))
                .thenReturn(new MesProEdhrPermissionScopeDetailResult().setScopeId(7301L));
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(9206L)
                .routeId(9006L)
                .versionNo("V7")
                .routeSnapshotJson("""
                        {
                          "routeId": 9006,
                          "routeCode": "RT-PUBLISH-NEW-PROCESS",
                          "routeName": "发布新增工序",
                          "status": 1,
                          "candidateSource": "EDHR_WORD_IMPORT",
                          "configSnapshots": {
                            "flowGraph": {
                              "graphVersion": 3,
                              "nodes": [
                                {
                                  "clientRouteProcessId": -1,
                                  "processId": 9406,
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
                                "routeProcessId": -1,
                                "executionMode": "SEQUENTIAL",
                                "batchRecordReports": [
                                  {
                                    "batchRecordReportId": "REPORT-NEW",
                                    "batchRecordDefinitionId": 1006,
                                    "batchRecordVersionId": 2006,
                                    "formSlotType": "MAIN",
                                    "permissionScopeId": -1,
                                    "recordCategorySnapshotHash": "client-record-hash",
                                    "slotConfigSnapshotHash": "client-slot-hash",
                                    "reportSort": 1
                                  }
                                ],
                                "formBindings": []
                              }
                            ],
                            "routeStartProductionLeaders": [],
                            "batchRecordAttachmentOwners": [],
                            "scheduleUseConfigs": []
                          }
                        }
                        """)
                .build();
        doAnswer(invocation -> {
            MesProRouteProcessDO process = invocation.getArgument(0);
            process.setId(9506L);
            return 1;
        }).when(routeProcessMapper).insert(any(MesProRouteProcessDO.class));

        service.projectCandidate(candidate);

        ArgumentCaptor<MesProRouteFlowProcessBatchRecordDO> recordCaptor =
                ArgumentCaptor.forClass(MesProRouteFlowProcessBatchRecordDO.class);
        verify(routeFlowProcessBatchRecordMapper).insert(recordCaptor.capture());
        MesProRouteFlowProcessBatchRecordDO record = recordCaptor.getValue();
        assertEquals(7301L, record.getPermissionScopeId());
        assertFalse("client-record-hash".equals(record.getRecordCategorySnapshotHash()));
        assertFalse("client-slot-hash".equals(record.getSlotConfigSnapshotHash()));

        ArgumentCaptor<MesProEdhrPermissionScopeSaveCommand> scopeCaptor =
                ArgumentCaptor.forClass(MesProEdhrPermissionScopeSaveCommand.class);
        verify(permissionScopeService).saveRules(scopeCaptor.capture());
        assertEquals("9506|REPORT-NEW", scopeCaptor.getValue().getObjectId());
        assertEquals(2, scopeCaptor.getValue().getRules().size());
    }

    @Test
    void projectCandidate_whenEdhrBindingArraysAreIncomplete_rejectsBeforeLiveMutation() {
        MesProRouteVersionDO candidate = buildIncompleteEdhrCandidate("""
                {
                  "routeProcessId": 9305,
                  "executionMode": "SEQUENTIAL",
                  "batchRecordReports": []
                }
                """, """
                "routeStartProductionLeaders": [],
                "batchRecordAttachmentOwners": []
                """);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.projectCandidate(candidate));

        assertTrue(exception.getMessage().contains("formBindings"));
        verifyNoInteractions(routeMapper, routeProcessMapper, flowEdgeMapper, boundaryEdgeMapper,
                flowLayoutMapper, routeProductMapper, routeProductBomMapper, routeScheduleConfigMapper,
                routeFlowConfigMapper, routeFlowProcessConfigMapper, routeFlowProcessBatchRecordMapper);
    }

    @Test
    void projectCandidate_whenEdhrStartArraysAreIncomplete_rejectsBeforeLiveMutation() {
        MesProRouteVersionDO candidate = buildIncompleteEdhrCandidate("""
                {
                  "routeProcessId": 9305,
                  "executionMode": "SEQUENTIAL",
                  "batchRecordReports": [],
                  "formBindings": []
                }
                """, """
                "batchRecordAttachmentOwners": []
                """);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.projectCandidate(candidate));

        assertTrue(exception.getMessage().contains("routeStartProductionLeaders"));
        verifyNoInteractions(routeMapper, routeProcessMapper, flowEdgeMapper, boundaryEdgeMapper,
                flowLayoutMapper, routeProductMapper, routeProductBomMapper, routeScheduleConfigMapper,
                routeFlowConfigMapper, routeFlowProcessConfigMapper, routeFlowProcessBatchRecordMapper);
    }

    private MesProRouteVersionDO buildIncompleteEdhrCandidate(String batchUseConfig,
                                                               String startConfigurations) {
        return MesProRouteVersionDO.builder()
                .id(9205L)
                .routeId(9005L)
                .versionNo("V6")
                .routeSnapshotJson("""
                        {
                          "routeId": 9005,
                          "routeCode": "RT-PUBLISH-EDHR-PREFLIGHT",
                          "routeName": "发布 Word 候选预检",
                          "status": 1,
                          "candidateSource": "EDHR_WORD_IMPORT",
                          "configSnapshots": {
                            "flowGraph": {
                              "graphVersion": 3,
                              "nodes": [
                                {
                                  "routeProcessId": 9305,
                                  "processId": 9405,
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
                            "batchUseConfigs": [%s],
                            %s,
                            "scheduleUseConfigs": []
                          }
                        }
                        """.formatted(batchUseConfig, startConfigurations))
                .build();
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

    @Test
    void projectCandidate_shouldDefaultLegacyFlatBatchRecordSnapshot() {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(9203L)
                .routeId(9003L)
                .versionNo("V4")
                .routeSnapshotJson("""
                        {
                          "routeId": 9003,
                          "routeCode": "RT-PUBLISH-FLAT-BATCH",
                          "routeName": "发布扁平批记录快照",
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
                                "batchRecordReportId": "RPT-FLAT-BATCH"
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

        service.projectCandidate(candidate);

        ArgumentCaptor<MesProRouteFlowProcessBatchRecordDO> batchRecordCaptor =
                ArgumentCaptor.forClass(MesProRouteFlowProcessBatchRecordDO.class);
        verify(routeFlowProcessBatchRecordMapper).insert(batchRecordCaptor.capture());
        MesProRouteFlowProcessBatchRecordDO inserted = batchRecordCaptor.getValue();
        assertEquals("RPT-FLAT-BATCH", inserted.getBatchRecordReportId());
        assertEquals("MAIN", inserted.getFormSlotType());
        assertEquals("PROCESS", inserted.getInstanceScope());
        assertEquals("BATCH_RECORD", inserted.getRecordCategory());
        assertEquals("CONTROLLED_BATCH", inserted.getValidationProfile());
        assertEquals("REQUIRED", inserted.getRequiredPolicy());
        assertEquals("PRODUCTION", inserted.getOwnerRoleKey());
        assertEquals("FINAL_DHR", inserted.getArchiveVisibility());
        assertEquals(1, inserted.getReportSort());
    }
}
