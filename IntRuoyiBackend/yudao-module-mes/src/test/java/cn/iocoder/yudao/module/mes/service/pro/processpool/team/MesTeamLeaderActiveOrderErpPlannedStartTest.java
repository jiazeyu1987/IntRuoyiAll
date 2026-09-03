package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesRouteDccProjectBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionDiffMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolQuantityFragmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProcessPoolReviewCopyFieldMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProcessPoolReviewCopyMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDeviceParameterRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamProcessDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolOrderProcessCompletionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationAdjustmentAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationStateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.productissue.MesWmProductIssueMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolWorkOrderAbnormalMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderErpPlannedStartTest {

    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesProWorkOrderService workOrderService;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private MesMdItemMapper itemMapper;
    @Mock
    private MesProcessPoolTeamMaintenanceAuditMapper auditMapper;
    @Mock
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Mock
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Mock
    private MesProRouteProductMapper routeProductMapper;
    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesRouteDccProjectBindingMapper routeDccProjectBindingMapper;
    @Mock
    private MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    @Mock
    private MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper;
    @Mock
    private MesProcessPoolTeamProcessDeviceMapper processDeviceMapper;
    @Mock
    private MesProcessPoolReportAllocationMapper reportAllocationMapper;
    @Mock
    private MesProcessPoolReportAllocationStateMapper reportAllocationStateMapper;
    @Mock
    private MesProcessPoolReportAllocationAdjustmentAuditMapper reportAllocationAdjustmentAuditMapper;
    @Mock
    private MesProcessPoolOrderProcessCompletionMapper orderProcessCompletionMapper;
    @Mock
    private MesProProcessPoolEventMapper processPoolEventMapper;
    @Mock
    private MesProFeedbackMapper feedbackMapper;
    @Mock
    private MesProProcessPoolPqcRecordMapper pqcRecordMapper;
    @Mock
    private MesProcessPoolSubmissionReviewMapper submissionReviewMapper;
    @Mock
    private MesProcessPoolReviewCopyFieldMapper reviewCopyFieldMapper;
    @Mock
    private MesProcessPoolReviewCopyMapper reviewCopyMapper;
    @Mock
    private MesProProcessPoolEventRevisionDiffMapper eventRevisionDiffMapper;
    @Mock
    private MesProProcessPoolEventRevisionMapper eventRevisionMapper;
    @Mock
    private MesProProcessPoolQuantityFragmentMapper quantityFragmentMapper;
    @Mock
    private MesPqcProcessInspectionAggregateDetailMapper pqcAggregateDetailMapper;
    @Mock
    private MesPqcInspectionPieceDetailMapper pqcPieceDetailMapper;
    @Mock
    private MesQaInspectionRegulationMapper inspectionRegulationMapper;
    @Mock
    private MesQaInspectionRegulationVersionMapper inspectionRegulationVersionMapper;
    @Mock
    private MesQaInspectionRegulationProcessMapper inspectionRegulationProcessMapper;
    @Mock
    private MesQaInspectionRegulationItemMapper inspectionRegulationItemMapper;
    @Mock
    private MesPqcInspectionTaskMapper pqcInspectionTaskMapper;
    @Mock
    private MesWorkOrderAbnormalStateService abnormalStateService;
    @Mock
    private MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;
    @Mock
    private DccProjectCodeMapper dccProjectCodeMapper;
    @Mock
    private MesReportAllocationOrderChangeService reportAllocationOrderChangeService;
    @Mock
    private ErpKingdeeProductionPickListMapper pickListMapper;
    @Mock
    private ErpKingdeeProductionPickListItemMapper pickListItemMapper;
    @Mock
    private MesProcessPoolActiveOrderPickListBindingMapper pickListBindingMapper;
    @Mock
    private MesProcessPoolActiveOrderPickListBindingItemMapper pickListBindingItemMapper;
    @Mock private MesProWorkOrderBomMapper workOrderBomMapper;
    @Mock private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock private MesWmProductIssueMapper productIssueMapper;
    @Mock private MesProcessPoolWorkOrderAbnormalMapper workOrderAbnormalMapper;

    private MesTeamLeaderActiveOrderService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderActiveOrderServiceImpl(activeOrderMapper, workOrderService, workOrderMapper,
                itemMapper, auditMapper, scheduleOrderMapper, scheduleOrderProcessMapper, routeProductMapper, routeMapper,
                routeVersionMapper, routeDccProjectBindingMapper, processSnapshotMapper, parameterRuleMapper,
                processDeviceMapper,
                reportAllocationMapper, reportAllocationStateMapper, reportAllocationAdjustmentAuditMapper,
                orderProcessCompletionMapper, processPoolEventMapper, feedbackMapper, pqcRecordMapper,
                submissionReviewMapper,
                reviewCopyFieldMapper, reviewCopyMapper, eventRevisionDiffMapper, eventRevisionMapper,
                quantityFragmentMapper, pqcAggregateDetailMapper, pqcPieceDetailMapper,
                inspectionRegulationMapper,
                inspectionRegulationVersionMapper, inspectionRegulationProcessMapper,
                inspectionRegulationItemMapper, pqcInspectionTaskMapper,
                abnormalStateService, releaseApplicationMapper, dccProjectCodeMapper,
                reportAllocationOrderChangeService,
                pickListBindingMapper, pickListBindingItemMapper, workOrderBomMapper,
                batchExecutionMapper, productIssueMapper, workOrderAbnormalMapper);
        lenient().when(itemMapper.selectListByCodeOrNameLike(any(), eq(20))).thenReturn(List.of());
        lenient().when(reportAllocationMapper.selectListByActiveOrderIds(any())).thenReturn(List.of());
        lenient().when(routeDccProjectBindingMapper.selectCurrentByRouteId(922119L))
                .thenReturn(MesRouteDccProjectBindingDO.builder()
                        .id(61001L)
                        .routeId(922119L)
                        .dccProjectCodeId(147L)
                        .version(1L)
                        .build());
        lenient().when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(List.of(9001L))).thenReturn(List.of());
        lenient().when(routeProductMapper.selectListByItemIds(List.of(1001L))).thenReturn(List.of(
                MesProRouteProductDO.builder().id(7001L).itemId(1001L).routeId(922119L).build()));
        List<MesProRouteProductDO> routeProducts = List.of(
                MesProRouteProductDO.builder().id(7001L).itemId(1001L).routeId(922119L).build(),
                MesProRouteProductDO.builder().id(7002L).itemId(924005L).routeId(922119L).build());
        lenient().when(routeProductMapper.selectListByRouteIds(List.of(922119L))).thenReturn(routeProducts);
        lenient().when(routeProductMapper.selectListByRouteId(922119L)).thenReturn(routeProducts);
        lenient().when(routeMapper.selectBatchIds(List.of(922119L))).thenReturn(List.of(MesProRouteDO.builder()
                .id(922119L)
                .code("ROUTE-922119")
                .build()));
        lenient().when(routeVersionMapper.selectListByRouteIds(List.of(922119L))).thenReturn(List.of(
                MesProRouteVersionDO.builder()
                        .id(448L)
                        .routeId(922119L)
                        .active(Boolean.TRUE)
                        .lifecycleStatus("ACTIVE")
                        .routeSnapshotJson(activeRouteSnapshotJson())
                        .build()));
        List<MesMdItemDO> routeItems = List.of(
                MesMdItemDO.builder().id(1001L).code("WO-PRODUCT").name("生产工单产品")
                        .productMasterId(11L).build(),
                MesMdItemDO.builder().id(924005L).code("ID").name("球囊扩张压力泵")
                        .productMasterId(11L).build());
        lenient().when(itemMapper.selectBatchIds(any())).thenReturn(routeItems);
        lenient().when(itemMapper.selectListByIds(any())).thenReturn(routeItems);
        lenient().when(dccProjectCodeMapper.selectById(147L)).thenReturn(DccProjectCodeDO.builder()
                .id(147L)
                .productMasterId(11L)
                .projectCode("ID")
                .projectName("球囊扩张压力泵")
                .status("ENABLE")
                .build());
        lenient().when(inspectionRegulationMapper.selectListByDccProjectCodeIds(any()))
                .thenReturn(List.of(publishedRegulation()));
        lenient().when(inspectionRegulationVersionMapper.selectBatchIds(List.of(9902L)))
                .thenReturn(List.of(publishedRegulationVersion()));
        lenient().when(inspectionRegulationVersionMapper.selectById(9902L))
                .thenReturn(publishedRegulationVersion());
        lenient().when(inspectionRegulationVersionMapper.selectLatestPublishedByRegulationId(9901L))
                .thenReturn(publishedRegulationVersion());
        lenient().when(inspectionRegulationItemMapper.selectListByVersionIds(List.of(9902L))).thenReturn(pqcItems());
        lenient().when(inspectionRegulationItemMapper.selectListByVersionId(9902L)).thenReturn(pqcItems());
        lenient().when(inspectionRegulationProcessMapper.selectListByVersionIds(List.of(9902L)))
                .thenReturn(List.of(qaProcess()));
        lenient().when(inspectionRegulationProcessMapper.selectListByVersionId(9902L))
                .thenReturn(List.of(qaProcess()));
        lenient().when(pqcInspectionTaskMapper.selectByQaIdentity(any(), any(), any(), any(), any(), any()))
                .thenReturn(null);
        lenient().when(pqcInspectionTaskMapper.insert(any(MesPqcInspectionTaskDO.class))).thenReturn(1);
        lenient().when(releaseApplicationMapper.selectLatestByActiveOrderIds(any())).thenReturn(List.of());
        lenient().when(pickListMapper.selectById(901L)).thenReturn(ErpKingdeeProductionPickListDO.builder()
                .id(901L).sourceFormId("PRD_PICK").sourceFid("FID-901").sourceBillNo("ISSUE-901")
                .documentStatus("C").build());
        lenient().when(pickListItemMapper.selectListByPickListIds(List.of(901L))).thenReturn(List.of(
                ErpKingdeeProductionPickListItemDO.builder().id(902L).productionPickListId(901L)
                        .sourceEntryId("ENTRY-1").sourceLineKey("LINE-1").productionOrderNo("WO-9001")
                        .materialNumber("WO-PRODUCT").actualQuantity(new BigDecimal("200"))
                        .requestedQuantity(new BigDecimal("200")).build()));
    }

    @Test
    void shouldKeepUnscheduledCandidateEligibleWhenErpPlannedStartMissing() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrderWithoutPlannedStart()));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertTrue(candidates.get(0).isEligible(), candidates.get(0).getIneligibleReason());
        assertNull(candidates.get(0).getIneligibleReason());
    }

    @Test
    void shouldUseJoinedDateForUnscheduledPqcTasksWhenErpPlannedStartMissing() {
        when(workOrderService.validateWorkOrderExists(9001L)).thenReturn(confirmedWorkOrderWithoutPlannedStart());
        when(activeOrderMapper.insert(any(MesProcessPoolActiveOrderDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolActiveOrderDO.class).setId(8101L);
            return 1;
        });
        when(processSnapshotMapper.insertBatch(any(Collection.class))).thenReturn(Boolean.TRUE);

        MesTeamLeaderActiveOrderAddResult result = service.addActiveOrder(MesTeamLeaderActiveOrderAddReqBO.builder()
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .idempotencyKey("erp-planned-start-test-1")
                .build());

        assertEquals(8101L, result.getActiveOrderId());
        assertEquals(MesTeamLeaderActiveOrderAddResult.ACTION_ADD, result.getAction());
        ArgumentCaptor<MesProcessPoolActiveOrderDO> activeOrderCaptor =
                ArgumentCaptor.forClass(MesProcessPoolActiveOrderDO.class);
        verify(activeOrderMapper).insert(activeOrderCaptor.capture());
        LocalDate joinedDate = activeOrderCaptor.getValue().getJoinedAt().toLocalDate();
        ArgumentCaptor<MesPqcInspectionTaskDO> taskCaptor = ArgumentCaptor.forClass(MesPqcInspectionTaskDO.class);
        verify(pqcInspectionTaskMapper, times(4)).insert(taskCaptor.capture());
        assertTrue(taskCaptor.getAllValues().stream()
                .allMatch(task -> joinedDate.equals(task.getBusinessDate())));
    }

    @Test
    void shouldGeneratePqcTasksByQaProcessItemAndRuleKeyWhenSameProcessHasMultipleFinalItems() {
        List<MesQaInspectionRegulationItemDO> items = List.of(
                pqcItem("FIRST", "FIRST-APPEARANCE", 5, null),
                pqcItem("PATROL", "PATROL-SEAL", 10, null),
                pqcItem("FINAL", "FINAL-APPEARANCE", 3, null),
                pqcItem("FINAL", "FINAL-PRESSURE", 4, null));
        when(inspectionRegulationItemMapper.selectListByVersionId(9902L)).thenReturn(items);
        when(workOrderService.validateWorkOrderExists(9001L)).thenReturn(confirmedWorkOrderWithoutPlannedStart());
        when(activeOrderMapper.insert(any(MesProcessPoolActiveOrderDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolActiveOrderDO.class).setId(8101L);
            return 1;
        });
        when(processSnapshotMapper.insertBatch(any(Collection.class))).thenReturn(Boolean.TRUE);

        MesTeamLeaderActiveOrderAddResult result = service.addActiveOrder(MesTeamLeaderActiveOrderAddReqBO.builder()
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .idempotencyKey("erp-planned-start-test-2")
                .build());

        assertEquals(8101L, result.getActiveOrderId());
        ArgumentCaptor<MesPqcInspectionTaskDO> taskCaptor = ArgumentCaptor.forClass(MesPqcInspectionTaskDO.class);
        verify(pqcInspectionTaskMapper, times(5)).insert(taskCaptor.capture());
        assertEquals(List.of("FIRST-APPEARANCE", "PATROL-SEAL", "PATROL-SEAL",
                        "FINAL-APPEARANCE", "FINAL-PRESSURE"),
                taskCaptor.getAllValues().stream().map(MesPqcInspectionTaskDO::getQaItemCode).toList());
        assertEquals(List.of("FIRST", "PATROL_AM", "PATROL_PM", "FINAL", "FINAL"),
                taskCaptor.getAllValues().stream().map(MesPqcInspectionTaskDO::getInspectionRuleKey).toList());
        assertEquals(List.of(5, 10, 10, 3, 4),
                taskCaptor.getAllValues().stream().map(MesPqcInspectionTaskDO::getPlannedInspectionQuantity).toList());
    }

    private static MesProWorkOrderDO confirmedWorkOrderWithoutPlannedStart() {
        return MesProWorkOrderDO.builder()
                .id(9001L)
                .code("WO-9001")
                .productId(1001L)
                .quantity(new BigDecimal("200"))
                .build();
    }

    private static MesQaInspectionRegulationDO publishedRegulation() {
        return MesQaInspectionRegulationDO.builder()
                .id(9901L)
                .dccProjectCodeId(147L)
                .productId(1001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .routeProcessId(928609L)
                .processId(6001L)
                .ownerModule(MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA)
                .lifecycleStatus("PUBLISHED")
                .currentVersionId(9902L)
                .build();
    }

    private static MesQaInspectionRegulationVersionDO publishedRegulationVersion() {
        return MesQaInspectionRegulationVersionDO.builder()
                .id(9902L)
                .regulationId(9901L)
                .versionNo("V1")
                .lifecycleStatus("PUBLISHED")
                .finalInspectionApplicable(Boolean.TRUE)
                .inspectionTypeRulesJson("""
                        [
                          {"key":"FIRST","inspectionType":"FIRST","label":"首检","required":true,"fixedQuantity":5},
                          {"key":"PATROL_AM","inspectionType":"PATROL","label":"上午巡检","required":true},
                          {"key":"PATROL_PM","inspectionType":"PATROL","label":"下午巡检","required":true},
                          {"key":"FINAL","inspectionType":"FINAL","label":"末检","required":true,"fixedQuantity":3}
                        ]
                        """)
                .snapshotJson("{}")
                .build();
    }

    private static List<MesQaInspectionRegulationItemDO> pqcItems() {
        return List.of(
                pqcItem("FIRST", "FIRST-001", 5, null),
                pqcItem("PATROL", "PATROL-001", null, new BigDecimal("0.050000")),
                pqcItem("FINAL", "FINAL-001", 3, null));
    }

    private static MesQaInspectionRegulationProcessDO qaProcess() {
        return MesQaInspectionRegulationProcessDO.builder().id(9910L).regulationVersionId(9902L)
                .processCode("ID-QA-001").processName("清洗").sort(1).build();
    }

    private static MesQaInspectionRegulationItemDO pqcItem(String inspectionType, Integer fixedQuantity,
                                                            BigDecimal patrolRatio) {
        return pqcItem(inspectionType, inspectionType + "-001", fixedQuantity, patrolRatio);
    }

    private static MesQaInspectionRegulationItemDO pqcItem(String inspectionType, String itemCode,
                                                            Integer fixedQuantity, BigDecimal patrolRatio) {
        return MesQaInspectionRegulationItemDO.builder()
                .regulationVersionId(9902L)
                .qaProcessId(9910L)
                .inspectionType(inspectionType)
                .itemCode(itemCode)
                .firstInspectionQuantity(fixedQuantity)
                .patrolInspectionRatio(patrolRatio)
                .build();
    }

    private static String activeRouteSnapshotJson() {
        return """
                {
                  "configSnapshots": {
                    "flowGraph": {
                      "nodes": [
                        {"routeProcessId": 928609, "processId": 6001, "processCode": "CLEAN-001",
                         "processName": "清洗", "sort": 10}
                      ]
                    },
                    "scheduleUseConfigs": [
                      {
                        "routeId": 922119,
                        "routeProcessId": 928609,
                        "useType": "SCHEDULE",
                        "enabled": true,
                        "productionQuantityFactor": 1.000000
                      }
                    ]
                  }
                }
                """;
    }
}
