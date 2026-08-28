package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolWorkOrderAbnormalDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListMapper;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesRouteDccProjectBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
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
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDeviceParameterRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolOrderProcessCompletionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationAdjustmentAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationStateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderServiceTest {

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

    private MesTeamLeaderActiveOrderService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderActiveOrderServiceImpl(activeOrderMapper, workOrderService, workOrderMapper,
                itemMapper, auditMapper, scheduleOrderMapper, scheduleOrderProcessMapper, routeProductMapper, routeMapper,
                routeVersionMapper, routeDccProjectBindingMapper, processSnapshotMapper, parameterRuleMapper,
                reportAllocationMapper, reportAllocationStateMapper, reportAllocationAdjustmentAuditMapper,
                orderProcessCompletionMapper, processPoolEventMapper, feedbackMapper, pqcRecordMapper,
                submissionReviewMapper,
                reviewCopyFieldMapper, reviewCopyMapper, eventRevisionDiffMapper, eventRevisionMapper,
                quantityFragmentMapper, pqcAggregateDetailMapper, pqcPieceDetailMapper,
                inspectionRegulationMapper, inspectionRegulationVersionMapper, inspectionRegulationProcessMapper,
                inspectionRegulationItemMapper, pqcInspectionTaskMapper, abnormalStateService,
                releaseApplicationMapper, dccProjectCodeMapper, reportAllocationOrderChangeService,
                pickListMapper, pickListItemMapper, pickListBindingMapper, pickListBindingItemMapper);
        lenient().when(itemMapper.selectListByCodeOrNameLike(any(), eq(20))).thenReturn(List.of());
        lenient().when(pickListMapper.selectById(9001L)).thenReturn(ErpKingdeeProductionPickListDO.builder()
                .id(9001L).sourceFormId("PRD_PickMtrl").sourceFid("9001").sourceBillNo("PICK-9001")
                .documentStatus("C").build());
        lenient().when(pickListItemMapper.selectListByPickListIds(List.of(9001L))).thenReturn(List.of(
                ErpKingdeeProductionPickListItemDO.builder().id(9101L).productionPickListId(9001L)
                        .sourceEntryId("10").sourceLineKey("9001:10").materialNumber("MAT-001")
                        .materialName("手柄").unitName("个").actualQuantity(new BigDecimal("5"))
                        .requestedQuantity(new BigDecimal("6")).productionOrderNo("WO-9001")
                        .build()));
        lenient().when(pickListBindingMapper.selectByIdempotencyKey(anyString())).thenReturn(null);
        lenient().when(pickListBindingMapper.selectByActiveOrderId(anyLong())).thenAnswer(invocation ->
                MesProcessPoolActiveOrderPickListBindingDO.builder().id(8801L)
                        .activeOrderId(invocation.getArgument(0, Long.class)).workOrderId(9001L)
                        .pickListId(9001L).sourceSnapshotHash(defaultPickListSnapshotHash())
                        .bindingStatus("BOUND").bindingVersion(1).build());
        lenient().when(pickListBindingMapper.insert(any(MesProcessPoolActiveOrderPickListBindingDO.class)))
                .thenAnswer(invocation -> {
                    invocation.getArgument(0, MesProcessPoolActiveOrderPickListBindingDO.class).setId(8801L);
                    return 1;
                });
        lenient().when(pickListBindingItemMapper.insert(any(MesProcessPoolActiveOrderPickListBindingItemDO.class)))
                .thenReturn(1);
        lenient().when(inspectionRegulationMapper.selectListByDccProjectCodeIds(any()))
                .thenReturn(List.of(publishedRegulation(9902L)));
        lenient().when(inspectionRegulationMapper.selectById(9901L))
                .thenReturn(publishedRegulation(9902L));
        lenient().when(inspectionRegulationVersionMapper.selectById(9902L))
                .thenReturn(publishedRegulationVersion(true, null));
        lenient().when(inspectionRegulationVersionMapper.selectLatestPublishedByRegulationId(9901L))
                .thenReturn(publishedRegulationVersion(9902L, true, null));
        lenient().when(routeMapper.selectById(922119L)).thenReturn(MesProRouteDO.builder()
                .id(922119L).code("ROUTE-001").name("冻结工艺路线").build());
        lenient().when(routeVersionMapper.selectById(448L)).thenReturn(MesProRouteVersionDO.builder()
                .id(448L).routeId(922119L).versionNo("V1").lifecycleStatus("SUPERSEDED")
                .routeSnapshotJson(activeRouteSnapshotJson(1)).build());
        lenient().when(processSnapshotMapper.selectListByActiveOrderId(8101L))
                .thenReturn(List.of(frozenProcessSnapshot()));
        lenient().when(inspectionRegulationProcessMapper.selectListByVersionIds(List.of(9902L)))
                .thenReturn(List.of(qaProcess(9902L)));
        lenient().when(inspectionRegulationItemMapper.selectListByVersionId(9902L)).thenReturn(defaultPqcItems());
        lenient().when(pqcInspectionTaskMapper.selectListByActiveOrderId(8101L))
                .thenReturn(frozenPqcTasks());
        lenient().when(pqcInspectionTaskMapper.selectByQaIdentity(any(), any(), any(), any(), any(), any()))
                .thenReturn(null);
        lenient().when(pqcInspectionTaskMapper.insert(any(MesPqcInspectionTaskDO.class))).thenReturn(1);
        lenient().when(abnormalStateService.findLatestOpenByWorkOrderIds(any())).thenReturn(Map.of());
        lenient().when(releaseApplicationMapper.selectLatestByActiveOrderIds(any())).thenReturn(List.of());
        lenient().when(reportAllocationMapper.selectListByActiveOrderIds(any())).thenReturn(List.of());
    }

    @Test
    void shouldReturnUnreleasedAllocationsBeforeRemovingActiveOrder() {
        MesProcessPoolActiveOrderDO activeOrder = existingActiveOrder(8101L, "ACTIVE", 7);
        when(activeOrderMapper.selectByIdForUpdate(8101L)).thenReturn(activeOrder);
        when(activeOrderMapper.removeActiveOrder(eq(8101L), eq(7), any(LocalDateTime.class))).thenReturn(1);

        service.removeActiveOrder(MesTeamLeaderActiveOrderRemoveReqBO.builder()
                .leaderUserId(3001L).activeOrderId(8101L).build());

        InOrder order = inOrder(reportAllocationOrderChangeService, activeOrderMapper);
        order.verify(reportAllocationOrderChangeService)
                .invalidateActiveOrder(8101L, 3001L, "活跃订单移除");
        order.verify(activeOrderMapper).removeActiveOrder(eq(8101L), eq(7), any(LocalDateTime.class));
    }

    @Test
    void shouldSearchWorkOrderCandidatesByCodeWhenProductionRouteExists() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrder()));
        stubCandidatePqcPrerequisites(publishedRegulation(9902L));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertEquals(9001L, candidates.get(0).getWorkOrderId());
        assertEquals("WO-9001", candidates.get(0).getWorkOrderCode());
        assertTrue(candidates.get(0).isEligible());
        assertEquals("ADDABLE", candidates.get(0).getCandidateState());
        assertEquals(null, candidates.get(0).getIneligibleReason());
        verify(workOrderMapper).selectCandidatesByKeyword("WO-9", List.of());
        verify(scheduleOrderMapper, never()).selectEffectiveListByWorkOrderIds(any());
        verify(routeProductMapper).selectListByItemIds(List.of(1001L));
        verify(routeVersionMapper).selectListByRouteIds(List.of(922119L));
    }

    @Test
    void shouldSearchWorkOrderCandidatesByProductKeywordWhenProductionRouteExists() {
        when(itemMapper.selectListByCodeOrNameLike("球囊", 20)).thenReturn(List.of(
                MesMdItemDO.builder().id(1001L).code("AW.107.02.01.2010").name("球囊扩张压力泵").build()));
        when(workOrderMapper.selectCandidatesByKeyword("球囊", List.of(1001L)))
                .thenReturn(List.of(confirmedWorkOrder()));
        stubCandidatePqcPrerequisites(publishedRegulation(9902L));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("球囊");

        assertEquals(1, candidates.size());
        assertEquals(9001L, candidates.get(0).getWorkOrderId());
        assertEquals("WO-9001", candidates.get(0).getWorkOrderCode());
        assertTrue(candidates.get(0).isEligible());
        verify(itemMapper).selectListByCodeOrNameLike("球囊", 20);
        verify(workOrderMapper).selectCandidatesByKeyword("球囊", List.of(1001L));
    }

    @Test
    void shouldReturnReusableCandidateFromUniqueActiveHistoryWithoutResolvingCurrentRoute() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrder()));
        when(activeOrderMapper.selectHistoryByWorkOrderIds(List.of(9001L)))
                .thenReturn(List.of(existingActiveOrder(8101L, "ACTIVE", 7)));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertTrue(candidates.get(0).isEligible());
        assertEquals("REUSABLE", candidates.get(0).getCandidateState());
        assertEquals(null, candidates.get(0).getIneligibleReason());
        verify(routeProductMapper, never()).selectListByItemIds(any());
        verify(routeVersionMapper, never()).selectListByRouteIds(any());
        verify(inspectionRegulationMapper, never()).selectListByDccProjectCodeIds(any());
    }

    @Test
    void shouldBlockRecoverableCandidateWhenCurrentRouteIsUnavailable() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrder()));
        when(activeOrderMapper.selectHistoryByWorkOrderIds(List.of(9001L)))
                .thenReturn(List.of(existingActiveOrder(8101L, "REMOVED", 7)));
        when(routeProductMapper.selectListByItemIds(List.of(1001L))).thenReturn(List.of());

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertFalse(candidates.get(0).isEligible());
        assertEquals("BLOCKED", candidates.get(0).getCandidateState());
        assertTrue(candidates.get(0).getIneligibleReason().contains("缺少产品工艺路线绑定"));
        verify(routeProductMapper).selectListByItemIds(List.of(1001L));
        verify(routeVersionMapper, never()).selectListByRouteIds(any());
    }

    @Test
    void shouldBlockCandidateWithExactAmbiguousHistoryReasonBeforeResolvingCurrentRoute() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrder()));
        when(activeOrderMapper.selectHistoryByWorkOrderIds(List.of(9001L))).thenReturn(List.of(
                existingActiveOrder(8101L, "REMOVED", 7),
                existingActiveOrder(8102L, "REMOVED", 3)));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertFalse(candidates.get(0).isEligible());
        assertEquals("BLOCKED", candidates.get(0).getCandidateState());
        assertEquals("生产工单存在多条历史记录，无法确定应复用的冻结订单：workOrderId=9001，activeOrderIds=[8101, 8102]",
                candidates.get(0).getIneligibleReason());
        verify(routeProductMapper, never()).selectListByItemIds(any());
        verify(routeVersionMapper, never()).selectListByRouteIds(any());
    }

    @Test
    void shouldBlockCandidateWithExactInvalidFrozenIdentityReasonBeforeResolvingCurrentRoute() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrder()));
        stubCandidatePqcPrerequisites(publishedRegulation(9902L));
        MesProcessPoolActiveOrderDO removed = existingActiveOrder(8101L, "REMOVED", 7);
        removed.setQaRegulationVersionId(null);
        when(activeOrderMapper.selectHistoryByWorkOrderIds(List.of(9001L))).thenReturn(List.of(removed));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertTrue(candidates.get(0).isEligible());
        assertEquals("RECOVERABLE", candidates.get(0).getCandidateState());
        assertEquals(null, candidates.get(0).getIneligibleReason());
    }

    @Test
    void shouldBlockRemovedCandidateWhenFrozenRouteVersionDoesNotBelongToFrozenRoute() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrder()));
        stubCandidatePqcPrerequisites(publishedRegulation(9902L));
        when(activeOrderMapper.selectHistoryByWorkOrderIds(List.of(9001L)))
                .thenReturn(List.of(existingActiveOrder(8101L, "REMOVED", 7)));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertTrue(candidates.get(0).isEligible());
        assertEquals("RECOVERABLE", candidates.get(0).getCandidateState());
        assertEquals(null, candidates.get(0).getIneligibleReason());
        verify(activeOrderMapper, never()).reactivateRemovedActiveOrder(any(), any(), any(), any(), any());
    }

    @Test
    void shouldBlockRemovedCandidateWhenFrozenProcessSnapshotsAreMissing() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrder()));
        stubCandidatePqcPrerequisites(publishedRegulation(9902L));
        when(activeOrderMapper.selectHistoryByWorkOrderIds(List.of(9001L)))
                .thenReturn(List.of(existingActiveOrder(8101L, "REMOVED", 7)));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertTrue(candidates.get(0).isEligible());
        assertEquals("RECOVERABLE", candidates.get(0).getCandidateState());
        assertEquals(null, candidates.get(0).getIneligibleReason());
        verify(activeOrderMapper, never()).reactivateRemovedActiveOrder(any(), any(), any(), any(), any());
    }

    @Test
    void shouldBlockRemovedCandidateWhenFrozenProcessSnapshotsDoNotCoverFrozenRouteVersion() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrder()));
        stubCandidatePqcPrerequisites(publishedRegulation(9902L));
        when(activeOrderMapper.selectHistoryByWorkOrderIds(List.of(9001L)))
                .thenReturn(List.of(existingActiveOrder(8101L, "REMOVED", 7)));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertTrue(candidates.get(0).isEligible());
        assertEquals("RECOVERABLE", candidates.get(0).getCandidateState());
        assertEquals(null, candidates.get(0).getIneligibleReason());
        verify(activeOrderMapper, never()).reactivateRemovedActiveOrder(any(), any(), any(), any(), any());
    }

    @Test
    void shouldBlockRemovedCandidateWhenFrozenPqcTaskIdentityIsMismatched() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrder()));
        stubCandidatePqcPrerequisites(publishedRegulation(9902L));
        when(activeOrderMapper.selectHistoryByWorkOrderIds(List.of(9001L)))
                .thenReturn(List.of(existingActiveOrder(8101L, "REMOVED", 7)));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertTrue(candidates.get(0).isEligible());
        assertEquals("RECOVERABLE", candidates.get(0).getCandidateState());
        assertEquals(null, candidates.get(0).getIneligibleReason());
        verify(activeOrderMapper, never()).reactivateRemovedActiveOrder(any(), any(), any(), any(), any());
    }

    @Test
    void shouldResolveProductionRouteWhenWorkOrderProductDiffers() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrder(9001L, "WO-9001", new BigDecimal("200"), 1002L)));
        MesQaInspectionRegulationDO routeRegulation = publishedRegulation(
                9902L, 1001L, 922119L, 448L, 928609L, 6001L);
        stubFormalRouteQaContext(1002L, 448L, routeRegulation);

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertTrue(candidates.get(0).isEligible());
        assertEquals(null, candidates.get(0).getIneligibleReason());
        verify(routeProductMapper).selectListByItemIds(List.of(1002L));
        verify(routeVersionMapper).selectListByRouteIds(List.of(922119L));
        verify(routeDccProjectBindingMapper).selectCurrentByRouteId(922119L);
        verify(dccProjectCodeMapper).selectById(147L);
        verify(inspectionRegulationMapper).selectListByDccProjectCodeIds(List.of(147L));
    }

    @Test
    void shouldIgnoreQaRouteVersionFieldsWhenResolvingProductionRoute() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrder()));
        MesQaInspectionRegulationDO legacyRouteFields = publishedRegulation(
                9902L, 1001L, 922119L, 448L, 928608L, 6000L);
        stubFormalRouteQaContext(1001L, 627L, legacyRouteFields);

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertTrue(candidates.get(0).isEligible());
        assertEquals(null, candidates.get(0).getIneligibleReason());
    }

    @Test
    void shouldKeepCandidateVisibleButIneligibleWhenRouteHasNoDccProjectBinding() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrder()));
        stubFormalRouteQaContext(1001L, 448L, publishedRegulation(9902L));
        lenient().when(routeDccProjectBindingMapper.selectCurrentByRouteId(922119L)).thenReturn(null);

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertFalse(candidates.get(0).isEligible());
        assertTrue(candidates.get(0).getIneligibleReason().contains("DCC"));
    }

    @Test
    void shouldKeepCandidateVisibleButIneligibleWhenRouteDccProjectIsDisabled() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrder()));
        stubFormalRouteQaContext(1001L, 448L, publishedRegulation(9902L));
        lenient().when(dccProjectCodeMapper.selectById(147L)).thenReturn(DccProjectCodeDO.builder()
                .id(147L).productMasterId(11L).projectCode("ID").status("DISABLE").build());

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertFalse(candidates.get(0).isEligible());
        assertTrue(candidates.get(0).getIneligibleReason().contains("DCC"));
    }

    @Test
    void shouldKeepCandidateVisibleButIneligibleWhenRouteItemProductMasterIsMissing() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrder()));
        stubFormalRouteQaContext(1001L, 448L, publishedRegulation(9902L));
        doReturn(List.of(
                MesMdItemDO.builder().id(1001L).productMasterId(null).build(),
                MesMdItemDO.builder().id(924005L).productMasterId(11L).build()))
                .when(itemMapper).selectListByIds(any());

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertFalse(candidates.get(0).isEligible());
        assertTrue(candidates.get(0).getIneligibleReason().contains("缺少 MDM 产品主档关系"));
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldKeepCandidateVisibleButIneligibleWhenRouteProductMasterIsAmbiguous() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrder()));
        stubFormalRouteQaContext(1001L, 448L, publishedRegulation(9902L));
        doReturn(List.of(
                MesMdItemDO.builder().id(1001L).productMasterId(11L).build(),
                MesMdItemDO.builder().id(924005L).productMasterId(12L).build()))
                .when(itemMapper).selectListByIds(any());

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertFalse(candidates.get(0).isEligible());
        assertTrue(candidates.get(0).getIneligibleReason().contains("多个 MDM 产品主档"));
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldKeepCandidateVisibleButIneligibleWhenRouteAndDccProductMasterDrifted() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrder()));
        stubFormalRouteQaContext(1001L, 448L, publishedRegulation(9902L));
        lenient().when(dccProjectCodeMapper.selectById(147L)).thenReturn(DccProjectCodeDO.builder()
                .id(147L).productMasterId(12L).projectCode("ID").status("ENABLE").build());

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertFalse(candidates.get(0).isEligible());
        assertTrue(candidates.get(0).getIneligibleReason().contains("MDM 产品主档不一致"));
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldIgnoreDeletedRouteBindingWhenOneFormalRouteStillExists() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrder()));
        stubFormalRouteQaContext(1001L, 448L, publishedRegulation(9902L));
        doReturn(List.of(
                MesProRouteProductDO.builder().id(7001L).routeId(922119L).itemId(1001L).build(),
                MesProRouteProductDO.builder().id(7002L).routeId(999999L).itemId(1001L).build()))
                .when(routeProductMapper).selectListByItemIds(any());
        when(routeMapper.selectBatchIds(any())).thenReturn(List.of(
                MesProRouteDO.builder().id(922119L).code("ROUTE-922119").build()));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertTrue(candidates.get(0).isEligible());
    }

    @Test
    void shouldBlockCancelledWorkOrderBeforeProductionRouteResolution() {
        MesProWorkOrderDO cancelled = confirmedWorkOrder();
        cancelled.setStatus(MesProWorkOrderStatusEnum.CANCELED.getStatus());
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of())).thenReturn(List.of(cancelled));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertFalse(candidates.get(0).isEligible());
        assertEquals("生产工单已取消", candidates.get(0).getIneligibleReason());
        verify(inspectionRegulationMapper, never()).selectListByDccProjectCodeIds(any());
    }

    @Test
    void shouldRequireFormalProductRouteBindingForCandidate() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrderWithPlannedStart()));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertFalse(candidates.get(0).isEligible());
        assertEquals("缺少产品工艺路线绑定", candidates.get(0).getIneligibleReason());
        verify(scheduleOrderMapper, never()).selectEffectiveListByWorkOrderIds(any());
        verify(routeProductMapper).selectListByItemIds(List.of(1001L));
    }

    @Test
    void shouldKeepCandidateVisibleButIneligibleWhenPublishedQaIsMissing() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrderWithPlannedStart()));
        stubFormalRouteQaContext(1001L, 448L);

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertFalse(candidates.get(0).isEligible());
        assertTrue(candidates.get(0).getIneligibleReason().contains("QA"));
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldSortEligibleActiveOrderCandidatesBeforeBlockedCandidates() {
        when(workOrderMapper.selectCandidatesByKeyword("WO", List.of())).thenReturn(List.of(
                confirmedWorkOrder(9002L, "WO-9002", new BigDecimal("200"), 1002L),
                confirmedWorkOrder(9001L, "WO-9001", new BigDecimal("200"), 1001L)));
        stubCandidatePqcPrerequisites(publishedRegulation(9902L, 928609L, 6001L));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO");

        assertEquals(List.of(9001L, 9002L), candidates.stream()
                .map(MesTeamLeaderActiveOrderCandidateBO::getWorkOrderId)
                .toList());
        assertTrue(candidates.get(0).isEligible());
        assertEquals(null, candidates.get(0).getIneligibleReason());
        assertFalse(candidates.get(1).isEligible());
        assertEquals("缺少产品工艺路线绑定", candidates.get(1).getIneligibleReason());
        verify(activeOrderMapper, never()).insert(any(MesProcessPoolActiveOrderDO.class));
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(pqcInspectionTaskMapper, never()).insert(any(MesPqcInspectionTaskDO.class));
    }

    @Test
    void shouldLoadQaIdentityWithoutLoadingQaItemsForRemoteDropdown() {
        when(workOrderMapper.selectCandidatesByKeyword("88", List.of())).thenReturn(List.of(
                confirmedWorkOrder(9001L, "881MO093613", new BigDecimal("200"), 1001L),
                confirmedWorkOrder(9002L, "881MO093615", new BigDecimal("200"), 1001L)));
        stubCandidatePqcPrerequisites(publishedRegulation(9902L, 928609L, 6001L));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("88");

        assertEquals(List.of(9001L, 9002L), candidates.stream()
                .map(MesTeamLeaderActiveOrderCandidateBO::getWorkOrderId)
                .toList());
        assertTrue(candidates.stream().allMatch(MesTeamLeaderActiveOrderCandidateBO::isEligible));
        verify(scheduleOrderMapper, never()).selectEffectiveListByWorkOrderIds(any());
        verify(scheduleOrderProcessMapper, never()).selectListByScheduleOrderIds(any());
        verify(scheduleOrderProcessMapper, never()).selectListByScheduleOrderId(any());
        verify(inspectionRegulationMapper).selectListByDccProjectCodeIds(List.of(147L));
        verify(inspectionRegulationVersionMapper).selectLatestPublishedByRegulationId(9901L);
        verify(inspectionRegulationVersionMapper, never()).selectById(any());
        verify(inspectionRegulationItemMapper, never()).selectListByVersionIds(any());
        verify(inspectionRegulationItemMapper, never()).selectListByVersionId(any());
    }

    @Test
    void shouldApplyCandidateLimitAfterEligibilityEvaluationForBroadKeyword() {
        List<MesProWorkOrderDO> matches = new ArrayList<>();
        for (int index = 1; index <= 20; index++) {
            MesProWorkOrderDO cancelled = confirmedWorkOrder(9100L + index,
                    "883MO-CANCELLED-" + index, new BigDecimal("200"), 2000L + index);
            cancelled.setStatus(MesProWorkOrderStatusEnum.CANCELED.getStatus());
            matches.add(cancelled);
        }
        matches.add(confirmedWorkOrder(9001L, "881MO090935", new BigDecimal("517"), 1001L));
        matches.add(confirmedWorkOrder(9002L, "881MO090972", new BigDecimal("5223"), 1001L));
        matches.add(confirmedWorkOrder(9003L, "881MO090973", new BigDecimal("4223"), 1001L));
        matches.add(confirmedWorkOrder(9004L, "881MO090974", new BigDecimal("8543"), 1001L));
        when(workOrderMapper.selectCandidatesByKeyword("88", List.of())).thenReturn(matches);
        stubCandidatePqcPrerequisites(publishedRegulation(9902L, 928609L, 6001L));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("88");

        assertEquals(20, candidates.size());
        assertEquals(List.of("881MO090935", "881MO090972", "881MO090973", "881MO090974"),
                candidates.subList(0, 4).stream()
                        .map(MesTeamLeaderActiveOrderCandidateBO::getWorkOrderCode)
                        .toList());
        assertTrue(candidates.subList(0, 4).stream().allMatch(MesTeamLeaderActiveOrderCandidateBO::isEligible));
        assertTrue(candidates.subList(4, candidates.size()).stream()
                .allMatch(candidate -> !candidate.isEligible()
                        && "生产工单已取消".equals(candidate.getIneligibleReason())));
    }

    @Test
    void shouldAddWorkOrderToLeaderActivePoolWithServerResolvedProductionRoute() {
        stubWorkOrderExists(confirmedWorkOrder());
        stubFormalRouteQaContext(1001L, 448L, activeRouteSnapshotJson(2),
                publishedRegulation(9902L, 928609L, 6001L));
        when(activeOrderMapper.selectLastByLeaderForUpdate(3001L))
                .thenReturn(MesProcessPoolActiveOrderDO.builder().id(8000L).sortOrder(40L).build());
        stubSuccessfulActiveOrderInsert();

        MesTeamLeaderActiveOrderAddResult result = service.addActiveOrder(activeOrderReq());

        assertEquals(8101L, result.getActiveOrderId());
        assertEquals(MesTeamLeaderActiveOrderAddResult.ACTION_ADD, result.getAction());
        verify(workOrderService).validateWorkOrderExists(9001L);
        verify(workOrderService, never()).validateWorkOrderConfirmed(any());
        verify(scheduleOrderMapper, never()).selectEffectiveListByWorkOrderIds(any());
        verify(routeProductMapper).selectListByItemIds(List.of(1001L));
        verify(routeVersionMapper).selectListByRouteIds(List.of(922119L));
        ArgumentCaptor<MesProcessPoolActiveOrderDO> captor =
                ArgumentCaptor.forClass(MesProcessPoolActiveOrderDO.class);
        verify(activeOrderMapper).insert(captor.capture());
        MesProcessPoolActiveOrderDO activeOrder = captor.getValue();
        assertEquals(3001L, activeOrder.getLeaderUserId());
        assertEquals(9001L, activeOrder.getWorkOrderId());
        assertEquals(922119L, activeOrder.getRouteId());
        assertEquals(448L, activeOrder.getRouteVersionId());
        assertEquals(147L, activeOrder.getDccProjectCodeId());
        assertEquals(9901L, activeOrder.getQaRegulationId());
        assertEquals(9902L, activeOrder.getQaRegulationVersionId());
        assertEquals(new BigDecimal("200"), activeOrder.getErpFixedQuantitySnapshot());
        assertEquals("ACTIVE", activeOrder.getActiveStatus());
        assertEquals("ACTIVE", activeOrder.getBusinessStatus());
        assertEquals(41L, activeOrder.getSortOrder());
        assertEquals(0, activeOrder.getVersion());
        assertNotNull(activeOrder.getJoinedAt());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MesProcessPoolActiveOrderProcessSnapshotDO>> snapshotCaptor =
                ArgumentCaptor.forClass(Collection.class);
        verify(processSnapshotMapper).insertBatch(snapshotCaptor.capture());
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots = List.copyOf(snapshotCaptor.getValue());
        assertEquals(2, snapshots.size());
        assertSnapshot(snapshots.get(0), 8101L, 9001L, 922119L, 448L, 928601L, 6001L,
                "200", "1.000000", "200.000000");
        assertSnapshot(snapshots.get(1), 8101L, 9001L, 922119L, 448L, 928602L, 6002L,
                "200", "1.000000", "200.000000");
        ArgumentCaptor<MesPqcInspectionTaskDO> taskCaptor =
                ArgumentCaptor.forClass(MesPqcInspectionTaskDO.class);
        verify(pqcInspectionTaskMapper, times(4)).insert(taskCaptor.capture());
        assertEquals(Set.of("FIRST", "PATROL_AM", "PATROL_PM", "FINAL"),
                taskCaptor.getAllValues().stream().map(MesPqcInspectionTaskDO::getInspectionRuleKey)
                        .collect(java.util.stream.Collectors.toSet()));
        assertTrue(taskCaptor.getAllValues().stream()
                .allMatch(task -> Objects.equals(19902L, task.getQaProcessId())
                        && Objects.equals(9902L, task.getRegulationVersionId())
                        && Objects.equals(928601L, task.getRouteProcessId())
                        && Objects.equals(6001L, task.getProcessId())));
        verify(auditMapper).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldAddWorkOrderToLeaderActivePoolWithoutPickList() {
        stubWorkOrderExists(confirmedWorkOrder());
        stubFormalRouteQaContext(1001L, 448L, activeRouteSnapshotJson(2),
                publishedRegulation(9902L, 928609L, 6001L));
        when(activeOrderMapper.selectLastByLeaderForUpdate(3001L))
                .thenReturn(MesProcessPoolActiveOrderDO.builder().id(8000L).sortOrder(40L).build());
        stubSuccessfulActiveOrderInsert();

        MesTeamLeaderActiveOrderAddResult result = service.addActiveOrder(MesTeamLeaderActiveOrderAddReqBO.builder()
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .idempotencyKey("IDEMP-9001-DIRECT")
                .build());

        assertEquals(8101L, result.getActiveOrderId());
        assertEquals(MesTeamLeaderActiveOrderAddResult.ACTION_ADD, result.getAction());
        assertNull(result.getPickListBindingId());
        assertNull(result.getPickListId());
        assertNull(result.getSourceSnapshotHash());
        assertNull(result.getBindingVersion());
        verify(pickListMapper, never()).selectById(any());
        verify(pickListItemMapper, never()).selectListByPickListIds(any());
        verify(pickListBindingMapper, never()).insert(any(MesProcessPoolActiveOrderPickListBindingDO.class));
    }

    @Test
    void shouldFreezeCanonicalDeviceParametersForExactRouteProcess() {
        stubWorkOrderExists(confirmedWorkOrder());
        stubFormalRouteQaContext(1001L, 448L, activeRouteSnapshotJson(2),
                publishedRegulation(9902L, 928609L, 6001L));
        stubSuccessfulActiveOrderInsert();
        when(parameterRuleMapper.selectList(any())).thenReturn(List.of(
                parameterRule(12L, 928601L, 6001L, 501L, "temperature", "80"),
                parameterRule(11L, 928601L, 6001L, 501L, "Pressure", "10"),
                parameterRule(13L, 999999L, 6001L, 501L, "ignored", "99")));

        service.addActiveOrder(activeOrderReq());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MesProcessPoolActiveOrderProcessSnapshotDO>> snapshotCaptor =
                ArgumentCaptor.forClass(Collection.class);
        verify(processSnapshotMapper).insertBatch(snapshotCaptor.capture());
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots = List.copyOf(snapshotCaptor.getValue());
        List<MesDeviceParameterSnapshotRule> firstRules = MesDeviceParameterSnapshotCodec.parse(
                snapshots.get(0).getParameterSnapshotJson());
        assertEquals(List.of("pressure", "temperature"), firstRules.stream()
                .map(MesDeviceParameterSnapshotRule::getParameterCode).toList());
        assertEquals(new BigDecimal("10"), firstRules.get(0).getUpperLimit());
        assertEquals(MesDeviceParameterSnapshotCodec.sha256(snapshots.get(0).getParameterSnapshotJson()),
                snapshots.get(0).getParameterSnapshotSha256());
        assertEquals("[]", snapshots.get(1).getParameterSnapshotJson());
    }

    @Test
    void shouldRejectDuplicateParameterCanonicalKeyBeforeSnapshotInsert() {
        stubWorkOrderExists(confirmedWorkOrder());
        stubFormalRouteQaContext(1001L, 448L, activeRouteSnapshotJson(2),
                publishedRegulation(9902L, 928609L, 6001L));
        when(activeOrderMapper.insert(any(MesProcessPoolActiveOrderDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolActiveOrderDO.class).setId(8101L);
            return 1;
        });
        when(parameterRuleMapper.selectList(any())).thenReturn(List.of(
                parameterRule(11L, 928601L, 6001L, 501L, "pressure", "10"),
                parameterRule(12L, 928601L, 6001L, 501L, " PRESSURE ", "12")));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.addActiveOrder(activeOrderReq()));

        assertTrue(ex.getMessage().contains("Duplicate device parameter canonical key 501|pressure"));
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(pqcInspectionTaskMapper, never()).insert(any(MesPqcInspectionTaskDO.class));
    }

    @Test
    void shouldSnapshotAllRouteProcessesAndCreateQaOwnedPqcTasks() {
        stubWorkOrderExists(confirmedWorkOrder(new BigDecimal("10")));
        stubFormalRouteQaContext(1001L, 448L, activeRouteSnapshotJson(10),
                publishedRegulation(9902L, null, null));
        stubSuccessfulActiveOrderInsert();

        MesTeamLeaderActiveOrderAddResult result = service.addActiveOrder(activeOrderReq());

        assertEquals(8101L, result.getActiveOrderId());
        assertEquals(MesTeamLeaderActiveOrderAddResult.ACTION_ADD, result.getAction());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MesProcessPoolActiveOrderProcessSnapshotDO>> snapshotCaptor =
                ArgumentCaptor.forClass(Collection.class);
        verify(processSnapshotMapper).insertBatch(snapshotCaptor.capture());
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots = List.copyOf(snapshotCaptor.getValue());
        assertEquals(10, snapshots.size());
        assertSnapshot(snapshots.get(0), 8101L, 9001L, 922119L, 448L, 928601L, 6001L,
                "10", "1.000000", "10.000000");
        assertSnapshot(snapshots.get(9), 8101L, 9001L, 922119L, 448L, 928610L, 6010L,
                "10", "1.000000", "10.000000");
        ArgumentCaptor<MesPqcInspectionTaskDO> taskCaptor =
                ArgumentCaptor.forClass(MesPqcInspectionTaskDO.class);
        verify(pqcInspectionTaskMapper, times(4)).insert(taskCaptor.capture());
        List<MesPqcInspectionTaskDO> tasks = taskCaptor.getAllValues();
        LocalDate expectedBusinessDate = LocalDate.now();
        assertPqcTask(tasks.get(0), "FIRST", "FIRST", "FIRST", 5, expectedBusinessDate);
        assertPqcTask(tasks.get(1), "PATROL", "PATROL_AM", "AM", 1, expectedBusinessDate);
        assertPqcTask(tasks.get(2), "PATROL", "PATROL_PM", "PM", 1, expectedBusinessDate);
        assertPqcTask(tasks.get(3), "FINAL", "FINAL", "FINAL", 3, expectedBusinessDate);
    }

    @Test
    void shouldRoundPatrolRatioUpForAmAndPmTasks() {
        stubWorkOrderExists(confirmedWorkOrder(new BigDecimal("301")));
        stubFormalRouteQaContext(1001L, 448L, activeRouteSnapshotJson(2),
                publishedRegulation(9902L, 928609L, 6001L));
        stubSuccessfulActiveOrderInsert();

        service.addActiveOrder(activeOrderReq());

        ArgumentCaptor<MesPqcInspectionTaskDO> taskCaptor =
                ArgumentCaptor.forClass(MesPqcInspectionTaskDO.class);
        verify(pqcInspectionTaskMapper, times(4)).insert(taskCaptor.capture());
        List<MesPqcInspectionTaskDO> tasks = taskCaptor.getAllValues();
        LocalDate expectedBusinessDate = LocalDate.now();
        assertPqcTask(tasks.get(1), "PATROL", "PATROL_AM", "AM", 16, expectedBusinessDate);
        assertPqcTask(tasks.get(2), "PATROL", "PATROL_PM", "PM", 16, expectedBusinessDate);
    }

    @Test
    void shouldKeepFirstAndPatrolQuantitiesPerQaProcessWhenProjectFinalInspectionIsDisabled() {
        stubWorkOrderExists(confirmedWorkOrder(new BigDecimal("200")));
        stubFormalRouteQaContext(1001L, 448L, activeRouteSnapshotJson(2),
                publishedRegulation(9902L, 928609L, 6001L));
        when(inspectionRegulationVersionMapper.selectLatestPublishedByRegulationId(9901L))
                .thenReturn(publishedRegulationVersion(false, "项目不适用末检"));
        when(inspectionRegulationProcessMapper.selectListByVersionIds(List.of(9902L))).thenReturn(List.of(
                qaProcess(9902L, 19902L, "QA-P1", "清洗", 1),
                qaProcess(9902L, 19903L, "QA-P2", "装配", 2)));
        when(inspectionRegulationItemMapper.selectListByVersionId(9902L)).thenReturn(List.of(
                pqcItem(9902L, 19902L, "FIRST", 2, null),
                pqcItem(9902L, 19902L, "PATROL", null, new BigDecimal("5.000000")),
                pqcItem(9902L, 19903L, "FIRST", 7, null),
                pqcItem(9902L, 19903L, "PATROL", null, new BigDecimal("12.500000"))));
        stubSuccessfulActiveOrderInsert();

        service.addActiveOrder(activeOrderReq());

        ArgumentCaptor<MesPqcInspectionTaskDO> taskCaptor =
                ArgumentCaptor.forClass(MesPqcInspectionTaskDO.class);
        verify(pqcInspectionTaskMapper, times(6)).insert(taskCaptor.capture());
        Map<Long, Map<String, Integer>> quantitiesByProcess = taskCaptor.getAllValues().stream()
                .collect(java.util.stream.Collectors.groupingBy(MesPqcInspectionTaskDO::getQaProcessId,
                        java.util.stream.Collectors.toMap(MesPqcInspectionTaskDO::getInspectionRuleKey,
                                MesPqcInspectionTaskDO::getPlannedInspectionQuantity)));
        assertEquals(Map.of("FIRST", 2, "PATROL_AM", 10, "PATROL_PM", 10),
                quantitiesByProcess.get(19902L));
        assertEquals(Map.of("FIRST", 7, "PATROL_AM", 25, "PATROL_PM", 25),
                quantitiesByProcess.get(19903L));
        assertTrue(taskCaptor.getAllValues().stream()
                .noneMatch(task -> "FINAL".equals(task.getInspectionRuleKey())));
    }

    @Test
    void shouldGenerateItemScopedPqcTasksForSameQaProcessRuleWhenItemsDiffer() {
        stubWorkOrderExists(confirmedWorkOrder(new BigDecimal("200")));
        stubFormalRouteQaContext(1001L, 448L, activeRouteSnapshotJson(2),
                publishedRegulation(9902L, 928609L, 6001L));
        when(inspectionRegulationVersionMapper.selectLatestPublishedByRegulationId(9901L))
                .thenReturn(publishedRegulationVersion(false, "项目不适用末检"));
        when(inspectionRegulationProcessMapper.selectListByVersionIds(List.of(9902L))).thenReturn(List.of(
                qaProcess(9902L, 19902L, "QA-P1", "清洗", 1)));
        when(inspectionRegulationItemMapper.selectListByVersionId(9902L)).thenReturn(List.of(
                pqcItem(9902L, 19902L, "FIRST", 2, null)
                        .setItemCode("FIRST-A").setItemName("外观"),
                pqcItem(9902L, 19902L, "FIRST", 7, null)
                        .setItemCode("FIRST-B").setItemName("尺寸"),
                pqcItem(9902L, 19902L, "PATROL", null, new BigDecimal("5.000000"))
                        .setItemCode("PATROL-C").setItemName("巡检压力")));
        stubSuccessfulActiveOrderInsert();

        service.addActiveOrder(activeOrderReq());

        ArgumentCaptor<MesPqcInspectionTaskDO> taskCaptor =
                ArgumentCaptor.forClass(MesPqcInspectionTaskDO.class);
        verify(pqcInspectionTaskMapper, times(4)).insert(taskCaptor.capture());
        List<MesPqcInspectionTaskDO> tasks = taskCaptor.getAllValues();
        assertEquals(List.of("FIRST-A", "FIRST-B"),
                tasks.stream()
                        .filter(task -> "FIRST".equals(task.getInspectionRuleKey()))
                        .map(MesPqcInspectionTaskDO::getQaItemCode)
                        .sorted()
                        .toList());
        assertEquals(Map.of("FIRST-A", 2, "FIRST-B", 7),
                tasks.stream()
                        .filter(task -> "FIRST".equals(task.getInspectionRuleKey()))
                        .collect(java.util.stream.Collectors.toMap(MesPqcInspectionTaskDO::getQaItemCode,
                                MesPqcInspectionTaskDO::getPlannedInspectionQuantity)));
        assertEquals(List.of("PATROL-C", "PATROL-C"),
                tasks.stream()
                        .filter(task -> task.getInspectionRuleKey().startsWith("PATROL"))
                        .map(MesPqcInspectionTaskDO::getQaItemCode)
                        .sorted()
                        .toList());
        assertEquals(Set.of("FIRST", "PATROL_AM", "PATROL_PM"),
                tasks.stream().map(MesPqcInspectionTaskDO::getInspectionRuleKey)
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void shouldGenerateFinalPqcTasksPerQaItemWhenSameQaProcessHasMultipleFinalItems() {
        stubWorkOrderExists(confirmedWorkOrder(new BigDecimal("200")));
        stubFormalRouteQaContext(1001L, 448L, activeRouteSnapshotJson(2),
                publishedRegulation(9902L, 928609L, 6001L));
        when(inspectionRegulationVersionMapper.selectLatestPublishedByRegulationId(9901L))
                .thenReturn(publishedRegulationVersion(true, null));
        when(inspectionRegulationProcessMapper.selectListByVersionIds(List.of(9902L))).thenReturn(List.of(
                qaProcess(9902L, 19902L, "QA-P1", "清洗", 1)));
        when(inspectionRegulationItemMapper.selectListByVersionId(9902L)).thenReturn(List.of(
                pqcItem(9902L, 19902L, "FIRST", 2, null)
                        .setItemCode("FIRST-A").setItemName("外观"),
                pqcItem(9902L, 19902L, "PATROL", null, new BigDecimal("5.000000"))
                        .setItemCode("PATROL-C").setItemName("巡检压力"),
                pqcItem(9902L, 19902L, "FINAL", 3, null)
                        .setItemCode("FINAL-D").setItemName("末检尺寸"),
                pqcItem(9902L, 19902L, "FINAL", 3, null)
                        .setItemCode("FINAL-E").setItemName("末检外观")));
        stubSuccessfulActiveOrderInsert();

        service.addActiveOrder(activeOrderReq());

        ArgumentCaptor<MesPqcInspectionTaskDO> taskCaptor =
                ArgumentCaptor.forClass(MesPqcInspectionTaskDO.class);
        verify(pqcInspectionTaskMapper, times(5)).insert(taskCaptor.capture());
        List<MesPqcInspectionTaskDO> tasks = taskCaptor.getAllValues();
        assertEquals(List.of("FINAL-D", "FINAL-E"),
                tasks.stream()
                        .filter(task -> "FINAL".equals(task.getInspectionRuleKey()))
                        .map(MesPqcInspectionTaskDO::getQaItemCode)
                        .sorted()
                        .toList());
        assertEquals(Set.of("FIRST", "PATROL_AM", "PATROL_PM", "FINAL"),
                tasks.stream().map(MesPqcInspectionTaskDO::getInspectionRuleKey)
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void rebuildPreviewShouldExposeExistingSnapshotsWithoutTreatingPendingPqcTasksAsResults() {
        when(activeOrderMapper.selectByIdForUpdate(8101L))
                .thenReturn(existingActiveOrder(8101L, "ACTIVE", 7));
        when(reportAllocationMapper.selectAllListByActiveOrderIdForUpdate(8101L)).thenReturn(List.of());
        when(pqcInspectionTaskMapper.selectListByActiveOrderIdForUpdate(8101L)).thenReturn(frozenPqcTasks());
        when(orderProcessCompletionMapper.selectListByWorkOrderIdsForUpdate(List.of(9001L))).thenReturn(List.of());
        when(processSnapshotMapper.selectListByActiveOrderIdForUpdate(8101L))
                .thenReturn(List.of(frozenProcessSnapshot()));
        when(releaseApplicationMapper.selectListByActiveOrderIdsForUpdate(List.of(8101L))).thenReturn(List.of());
        when(processPoolEventMapper.selectListPqcByTaskId(any(), any())).thenReturn(List.of());
        when(pqcAggregateDetailMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of());

        MesTeamLeaderActiveOrderRebuildPreview preview = service.previewRebuildActiveOrder(3001L, 8101L);

        assertFalse(preview.isHasHistoricalRuntimeData());
        assertEquals(0, preview.getProductionReportCount());
        assertEquals(0, preview.getProductionProgressCount());
        assertEquals(0, preview.getPqcInspectionResultCount());
        assertEquals(1, preview.getProcessSnapshotCount());
        assertEquals(4, preview.getPqcTaskCount());
    }

    @Test
    void rebuildActiveOrderShouldRequireConfirmationBeforeDeletingHistoricalRuntimeData() {
        stubRebuildHistoricalRuntimePreview();

        ServiceException ex = assertThrows(ServiceException.class, () -> service.rebuildActiveOrder(
                MesTeamLeaderActiveOrderRebuildReqBO.builder()
                        .leaderUserId(3001L)
                        .activeOrderId(8101L)
                        .confirmDeleteHistoricalRuntimeData(false)
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_REBUILD_CONFIRM_REQUIRED.getCode(),
                ex.getCode());
        verify(processSnapshotMapper, never()).deleteByActiveOrderId(any());
        verify(pqcInspectionTaskMapper, never()).deleteByActiveOrderId(any());
        verify(reportAllocationMapper, never()).deleteAllByActiveOrderId(any());
        verify(activeOrderMapper, never()).refreshActiveOrderSnapshot(any(MesProcessPoolActiveOrderDO.class));
        verify(auditMapper, never()).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void rebuildActiveOrderShouldDeleteRuntimeHistoryThenRebuildSnapshotsFromCurrentSources() {
        stubRebuildHistoricalRuntimePreview();
        stubWorkOrderExists(confirmedWorkOrder(new BigDecimal("200")));
        stubFormalRouteQaContext(1001L, 448L, activeRouteSnapshotJson(2),
                publishedRegulation(9902L, 928609L, 6001L));
        when(activeOrderMapper.refreshActiveOrderSnapshot(any(MesProcessPoolActiveOrderDO.class))).thenReturn(1);
        when(processSnapshotMapper.insertBatch(any())).thenReturn(Boolean.TRUE);

        MesTeamLeaderActiveOrderRebuildResult result = service.rebuildActiveOrder(
                MesTeamLeaderActiveOrderRebuildReqBO.builder()
                        .leaderUserId(3001L)
                        .activeOrderId(8101L)
                        .confirmDeleteHistoricalRuntimeData(true)
                        .build());

        assertTrue(result.isHistoricalRuntimeDataDeleted());
        assertEquals(1, result.getDeletedProductionReportCount());
        assertEquals(1, result.getDeletedProductionProgressCount());
        assertEquals(3, result.getDeletedPqcInspectionResultCount());
        assertEquals(1, result.getDeletedProcessSnapshotCount());
        assertEquals(1, result.getDeletedPqcTaskCount());
        assertEquals(2, result.getRebuiltProcessSnapshotCount());
        assertEquals(4, result.getRebuiltPqcTaskCount());
        verify(pqcAggregateDetailMapper).deleteByActiveOrderId(8101L);
        verify(pqcPieceDetailMapper).deleteByTaskIds(List.of(8305L));
        verify(pqcRecordMapper).deleteByEventIds(Set.of(8801L, 8802L, 8803L));
        verify(submissionReviewMapper).deleteByEventIds(Set.of(8801L, 8802L, 8803L));
        verify(reviewCopyFieldMapper).deleteByEventIds(Set.of(8801L, 8802L, 8803L));
        verify(reviewCopyMapper).deleteByEventIds(Set.of(8801L, 8802L, 8803L));
        verify(eventRevisionDiffMapper).deleteByEventIds(Set.of(8801L, 8802L, 8803L));
        verify(eventRevisionMapper).deleteByEventIds(Set.of(8801L, 8802L, 8803L));
        verify(quantityFragmentMapper).deleteByEventIds(Set.of(8801L, 8802L, 8803L));
        verify(reportAllocationStateMapper).deleteByEventIds(Set.of(8801L, 8802L, 8803L));
        verify(reportAllocationAdjustmentAuditMapper).deleteByActiveOrderId(8101L);
        verify(reportAllocationMapper).deleteAllByActiveOrderId(8101L);
        verify(orderProcessCompletionMapper).deleteByWorkOrderId(9001L);
        verify(pqcInspectionTaskMapper).deleteByActiveOrderId(8101L);
        verify(processSnapshotMapper).deleteByActiveOrderId(8101L);
        verify(releaseApplicationMapper).deleteByActiveOrderId(8101L);
        verify(processPoolEventMapper).deleteActiveOrderRuntimeEventsByIds(Set.of(8801L, 8802L, 8803L));
        verify(feedbackMapper).deleteByIds(List.of(5501L));
        verify(activeOrderMapper).refreshActiveOrderSnapshot(argThat((MesProcessPoolActiveOrderDO update) ->
                Objects.equals(8101L, update.getId())
                        && Objects.equals(3001L, update.getLeaderUserId())
                        && Objects.equals(922119L, update.getRouteId())
                        && Objects.equals(448L, update.getRouteVersionId())
                        && Objects.equals(9902L, update.getQaRegulationVersionId())
                        && Objects.equals(7, update.getVersion())
                        && Objects.equals("ACTIVE", update.getActiveStatus())));
        verify(processSnapshotMapper).insertBatch(any());
        verify(pqcInspectionTaskMapper, times(4)).insert(any(MesPqcInspectionTaskDO.class));
        verify(auditMapper).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void rebuildActiveOrderShouldResolveLatestPublishedQaVersionInsteadOfRegulationPointer() {
        stubRebuildHistoricalRuntimePreview();
        stubWorkOrderExists(confirmedWorkOrder(new BigDecimal("200")));
        stubFormalRouteQaContext(1001L, 448L, activeRouteSnapshotJson(2),
                publishedRegulation(9902L, 928609L, 6001L));
        when(inspectionRegulationVersionMapper.selectLatestPublishedByRegulationId(9901L))
                .thenReturn(publishedRegulationVersion(9903L, true, null));
        when(inspectionRegulationProcessMapper.selectListByVersionIds(List.of(9903L)))
                .thenReturn(List.of(qaProcess(9903L)));
        when(inspectionRegulationItemMapper.selectListByVersionId(9903L))
                .thenReturn(defaultPqcItems(9903L));
        when(activeOrderMapper.refreshActiveOrderSnapshot(any(MesProcessPoolActiveOrderDO.class))).thenReturn(1);
        when(processSnapshotMapper.insertBatch(any())).thenReturn(Boolean.TRUE);

        service.rebuildActiveOrder(MesTeamLeaderActiveOrderRebuildReqBO.builder()
                .leaderUserId(3001L)
                .activeOrderId(8101L)
                .confirmDeleteHistoricalRuntimeData(true)
                .build());

        verify(activeOrderMapper).refreshActiveOrderSnapshot(argThat((MesProcessPoolActiveOrderDO update) ->
                Objects.equals(9903L, update.getQaRegulationVersionId())));
        ArgumentCaptor<MesPqcInspectionTaskDO> taskCaptor = ArgumentCaptor.forClass(MesPqcInspectionTaskDO.class);
        verify(pqcInspectionTaskMapper, times(4)).insert(taskCaptor.capture());
        assertTrue(taskCaptor.getAllValues().stream().allMatch(task ->
                Objects.equals(9903L, task.getRegulationVersionId())
                        && Objects.equals(928601L, task.getRouteProcessId())
                        && Objects.equals(6001L, task.getProcessId())));
    }

    @Test
    void shouldAddUsingProductionRouteWhenWorkOrderProductDiffers() {
        stubWorkOrderExists(confirmedWorkOrder(9001L, "WO-9001", new BigDecimal("200"), 1002L));
        stubFormalRouteQaContext(1002L, 448L,
                publishedRegulation(9902L, 1001L, 922119L, 448L, 928609L, 6001L));
        stubSuccessfulActiveOrderInsert();

        MesTeamLeaderActiveOrderAddResult result = service.addActiveOrder(activeOrderReq());

        assertEquals(8101L, result.getActiveOrderId());
        assertEquals(MesTeamLeaderActiveOrderAddResult.ACTION_ADD, result.getAction());
        verify(routeProductMapper).selectListByItemIds(List.of(1002L));
        verify(inspectionRegulationMapper).selectListByDccProjectCodeIds(List.of(147L));
        ArgumentCaptor<MesProcessPoolActiveOrderDO> captor =
                ArgumentCaptor.forClass(MesProcessPoolActiveOrderDO.class);
        verify(activeOrderMapper).insert(captor.capture());
        assertEquals(922119L, captor.getValue().getRouteId());
        assertEquals(448L, captor.getValue().getRouteVersionId());
    }

    @Test
    void shouldRejectAddWithoutFormalProductRouteBinding() {
        stubWorkOrderExists(confirmedWorkOrderWithPlannedStart());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addActiveOrder(activeOrderReq()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("缺少产品工艺路线绑定"));
        verify(workOrderService).validateWorkOrderExists(9001L);
        verify(workOrderService, never()).validateWorkOrderConfirmed(any());
        verify(scheduleOrderMapper, never()).selectEffectiveListByWorkOrderIds(any());
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldAllowUnconfirmedWorkOrderWhenProductionRouteExists() {
        MesProWorkOrderDO draftWorkOrder = confirmedWorkOrderWithPlannedStart();
        draftWorkOrder.setStatus(0);
        stubWorkOrderExists(draftWorkOrder);
        stubCandidatePqcPrerequisites(publishedRegulation(9902L));
        stubSuccessfulActiveOrderInsert();

        MesTeamLeaderActiveOrderAddResult result = service.addActiveOrder(activeOrderReq());

        assertEquals(8101L, result.getActiveOrderId());
        assertEquals(MesTeamLeaderActiveOrderAddResult.ACTION_ADD, result.getAction());
        verify(workOrderService, never()).validateWorkOrderConfirmed(any());
    }

    @Test
    void shouldRejectNewActiveOrderWhenPublishedQaIsMissingWithoutAnyWrite() {
        stubWorkOrderExists(confirmedWorkOrderWithPlannedStart());
        stubFormalRouteQaContext(1001L, 448L);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addActiveOrder(activeOrderReq()));

        assertEquals(ErrorCodeConstants.PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED.getCode(), ex.getCode());
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldRejectActiveOrderWhenPqcTaskIdentityAlreadyExists() {
        stubWorkOrderExists(confirmedWorkOrder(new BigDecimal("10")));
        stubFormalRouteQaContext(1001L, 448L, activeRouteSnapshotJson(2),
                publishedRegulation(9902L, 928609L, 6001L));
        stubSuccessfulActiveOrderInsert();
        when(pqcInspectionTaskMapper.selectByQaIdentity(eq(8101L), eq(9902L), eq(19902L),
                eq("FIRST-001"), eq("FIRST"), any(LocalDate.class)))
                .thenReturn(MesPqcInspectionTaskDO.builder().id(990001L).build());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addActiveOrder(activeOrderReq()));

        assertEquals(ErrorCodeConstants.PRO_PQC_INSPECTION_TASK_IDENTITY_CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void shouldReturnExistingActiveOrderWhenSameWorkOrderRouteVersionAlreadyActive() {
        stubWorkOrderExists(confirmedWorkOrder());
        when(activeOrderMapper.selectHistoryByWorkOrderIdForUpdate(9001L))
                .thenReturn(List.of(existingActiveOrder(8101L, "ACTIVE", 0)));

        MesTeamLeaderActiveOrderAddResult result = service.addActiveOrder(activeOrderReq());

        assertEquals(8101L, result.getActiveOrderId());
        assertEquals(MesTeamLeaderActiveOrderAddResult.ACTION_REUSE, result.getAction());
        verify(routeProductMapper, never()).selectListByItemIds(any());
        verify(routeVersionMapper, never()).selectListByRouteIds(any());
        verify(activeOrderMapper, never()).insert(any(MesProcessPoolActiveOrderDO.class));
        verify(processSnapshotMapper, never()).insertBatch(any());
    }

    @Test
    void shouldReturnExistingActiveOrderWhenConcurrentInsertHitsUniqueKey() {
        stubWorkOrderExists(confirmedWorkOrder());
        stubCandidatePqcPrerequisites(publishedRegulation(9902L));
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(9001L, 922119L, 448L))
                .thenReturn(null, existingActiveOrder(8102L, "ACTIVE", 0));
        when(activeOrderMapper.insert(any(MesProcessPoolActiveOrderDO.class)))
                .thenThrow(new DuplicateKeyException("uk_mes_pp_active_order"));

        MesTeamLeaderActiveOrderAddResult result = service.addActiveOrder(activeOrderReq());

        assertEquals(8102L, result.getActiveOrderId());
        assertEquals(MesTeamLeaderActiveOrderAddResult.ACTION_REUSE, result.getAction());
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(auditMapper, never()).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldReactivateRemovedActiveOrderWhenSameWorkOrderRouteVersionIsJoinedAgain() {
        stubWorkOrderExists(confirmedWorkOrder());
        stubCandidatePqcPrerequisites(publishedRegulation(9902L));
        when(activeOrderMapper.selectHistoryByWorkOrderIdForUpdate(9001L))
                .thenReturn(List.of(existingActiveOrder(8101L, "REMOVED", 7)));
        when(reportAllocationMapper.selectAllListByActiveOrderIdForUpdate(8101L)).thenReturn(List.of());
        when(pqcInspectionTaskMapper.selectListByActiveOrderIdForUpdate(8101L)).thenReturn(List.of());
        when(activeOrderMapper.reactivateRemovedActiveOrder(any(), any(), any(), any(), any())).thenReturn(1);
        when(activeOrderMapper.refreshActiveOrderSnapshot(any(MesProcessPoolActiveOrderDO.class))).thenReturn(1);
        when(processSnapshotMapper.insertBatch(any())).thenReturn(Boolean.TRUE);

        MesTeamLeaderActiveOrderAddResult result = service.addActiveOrder(activeOrderReq());

        assertEquals(8101L, result.getActiveOrderId());
        assertEquals(MesTeamLeaderActiveOrderAddResult.ACTION_RECOVER, result.getAction());
        verify(activeOrderMapper).reactivateRemovedActiveOrder(
                eq(8101L), eq(3001L), eq(7), any(LocalDateTime.class), eq(1L));
        verify(activeOrderMapper, never()).insert(any(MesProcessPoolActiveOrderDO.class));
        verify(activeOrderMapper).refreshActiveOrderSnapshot(argThat((MesProcessPoolActiveOrderDO update) ->
                Objects.equals(8101L, update.getId())
                        && Objects.equals(9902L, update.getQaRegulationVersionId())));
        verify(processSnapshotMapper).insertBatch(any());
        verify(pqcInspectionTaskMapper, times(4)).insert(any(MesPqcInspectionTaskDO.class));
        verify(inspectionRegulationMapper, never()).selectByDccProjectCodeId(any());
        verify(auditMapper).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldReactivateFrozenRemovedOrderBeforeResolvingRepublishedRoute() {
        stubWorkOrderExists(confirmedWorkOrder());
        stubFormalRouteQaContext(1001L, 627L, publishedRegulation(9902L));
        when(activeOrderMapper.selectHistoryByWorkOrderIdForUpdate(9001L))
                .thenReturn(List.of(existingActiveOrder(8101L, "REMOVED", 7)));
        when(reportAllocationMapper.selectAllListByActiveOrderIdForUpdate(8101L)).thenReturn(List.of());
        when(pqcInspectionTaskMapper.selectListByActiveOrderIdForUpdate(8101L)).thenReturn(List.of());
        when(activeOrderMapper.reactivateRemovedActiveOrder(any(), any(), any(), any(), any())).thenReturn(1);
        when(activeOrderMapper.refreshActiveOrderSnapshot(any(MesProcessPoolActiveOrderDO.class))).thenReturn(1);
        when(processSnapshotMapper.insertBatch(any())).thenReturn(Boolean.TRUE);

        MesTeamLeaderActiveOrderAddResult result = service.addActiveOrder(activeOrderReq());
        assertEquals(8101L, result.getActiveOrderId());
        assertEquals(MesTeamLeaderActiveOrderAddResult.ACTION_RECOVER, result.getAction());

        verify(activeOrderMapper).reactivateRemovedActiveOrder(
                eq(8101L), eq(3001L), eq(7), any(LocalDateTime.class), eq(1L));
        verify(activeOrderMapper, never()).insert(any(MesProcessPoolActiveOrderDO.class));
        verify(activeOrderMapper).refreshActiveOrderSnapshot(argThat((MesProcessPoolActiveOrderDO update) ->
                Objects.equals(627L, update.getRouteVersionId())
                        && Objects.equals(9902L, update.getQaRegulationVersionId())));
        verify(processSnapshotMapper).insertBatch(any());
        verify(pqcInspectionTaskMapper, times(4)).insert(any(MesPqcInspectionTaskDO.class));
    }

    @Test
    void shouldReactivateFrozenRemovedOrderWhenCurrentRouteSourceIsUnavailable() {
        stubWorkOrderExists(confirmedWorkOrder());
        when(activeOrderMapper.selectHistoryByWorkOrderIdForUpdate(9001L))
                .thenReturn(List.of(existingActiveOrder(8101L, "REMOVED", 7)));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.addActiveOrder(activeOrderReq()));

        assertTrue(exception.getMessage().contains("缺少产品工艺路线绑定"));
        verify(activeOrderMapper, never()).reactivateRemovedActiveOrder(any(), any(), any(), any(), any());
        verify(activeOrderMapper, never()).insert(any(MesProcessPoolActiveOrderDO.class));
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(pqcInspectionTaskMapper, never()).insert(any(MesPqcInspectionTaskDO.class));
    }

    @Test
    void shouldReactivateRemovedOrderByRebuildingLatestPublishedQaVersion() {
        stubWorkOrderExists(confirmedWorkOrder(new BigDecimal("200")));
        stubFormalRouteQaContext(1001L, 448L, activeRouteSnapshotJson(2),
                publishedRegulation(9902L, 928609L, 6001L));
        when(activeOrderMapper.selectHistoryByWorkOrderIdForUpdate(9001L))
                .thenReturn(List.of(existingActiveOrder(8101L, "REMOVED", 7)));
        when(inspectionRegulationVersionMapper.selectLatestPublishedByRegulationId(9901L))
                .thenReturn(publishedRegulationVersion(9903L, true, null));
        when(inspectionRegulationProcessMapper.selectListByVersionIds(List.of(9903L)))
                .thenReturn(List.of(qaProcess(9903L)));
        when(inspectionRegulationItemMapper.selectListByVersionId(9903L))
                .thenReturn(defaultPqcItems(9903L));
        when(reportAllocationMapper.selectAllListByActiveOrderIdForUpdate(8101L)).thenReturn(List.of());
        when(pqcInspectionTaskMapper.selectListByActiveOrderIdForUpdate(8101L)).thenReturn(frozenPqcTasks());
        when(processPoolEventMapper.selectListPqcByTaskId(any(), any())).thenReturn(List.of());
        when(activeOrderMapper.reactivateRemovedActiveOrder(any(), any(), any(), any(), any())).thenReturn(1);
        when(activeOrderMapper.refreshActiveOrderSnapshot(any(MesProcessPoolActiveOrderDO.class))).thenReturn(1);
        when(processSnapshotMapper.insertBatch(any())).thenReturn(Boolean.TRUE);

        MesTeamLeaderActiveOrderAddResult result = service.addActiveOrder(activeOrderReq());

        assertEquals(8101L, result.getActiveOrderId());
        assertEquals(MesTeamLeaderActiveOrderAddResult.ACTION_RECOVER, result.getAction());
        verify(pqcInspectionTaskMapper).deleteByActiveOrderId(8101L);
        verify(processSnapshotMapper).deleteByActiveOrderId(8101L);
        verify(activeOrderMapper).refreshActiveOrderSnapshot(argThat((MesProcessPoolActiveOrderDO update) ->
                Objects.equals(8101L, update.getId())
                        && Objects.equals(9903L, update.getQaRegulationVersionId())));
        ArgumentCaptor<MesPqcInspectionTaskDO> taskCaptor =
                ArgumentCaptor.forClass(MesPqcInspectionTaskDO.class);
        verify(pqcInspectionTaskMapper, times(4)).insert(taskCaptor.capture());
        assertTrue(taskCaptor.getAllValues().stream()
                .allMatch(task -> Objects.equals(9903L, task.getRegulationVersionId())
                        && Objects.equals(19903L, task.getQaProcessId())));
    }

    @Test
    void shouldRecoverRemovedOrderWithBrokenHistoricalQaLockByRebuildingLatestPublishedQaVersion() {
        stubWorkOrderExists(confirmedWorkOrder(new BigDecimal("200")));
        stubFormalRouteQaContext(1001L, 448L, activeRouteSnapshotJson(2),
                publishedRegulation(9902L, 928609L, 6001L));
        MesProcessPoolActiveOrderDO removed = existingActiveOrder(8101L, "REMOVED", 7);
        removed.setQaRegulationVersionId(null);
        when(activeOrderMapper.selectHistoryByWorkOrderIdForUpdate(9001L)).thenReturn(List.of(removed));
        when(inspectionRegulationVersionMapper.selectLatestPublishedByRegulationId(9901L))
                .thenReturn(publishedRegulationVersion(9903L, true, null));
        when(inspectionRegulationProcessMapper.selectListByVersionIds(List.of(9903L)))
                .thenReturn(List.of(qaProcess(9903L)));
        when(inspectionRegulationItemMapper.selectListByVersionId(9903L))
                .thenReturn(defaultPqcItems(9903L));
        when(reportAllocationMapper.selectAllListByActiveOrderIdForUpdate(8101L)).thenReturn(List.of());
        when(pqcInspectionTaskMapper.selectListByActiveOrderIdForUpdate(8101L)).thenReturn(List.of());
        when(activeOrderMapper.reactivateRemovedActiveOrder(any(), any(), any(), any(), any())).thenReturn(1);
        when(activeOrderMapper.refreshActiveOrderSnapshot(any(MesProcessPoolActiveOrderDO.class))).thenReturn(1);
        when(processSnapshotMapper.insertBatch(any())).thenReturn(Boolean.TRUE);

        MesTeamLeaderActiveOrderAddResult result = service.addActiveOrder(activeOrderReq());

        assertEquals(8101L, result.getActiveOrderId());
        assertEquals(MesTeamLeaderActiveOrderAddResult.ACTION_RECOVER, result.getAction());
        verify(inspectionRegulationVersionMapper, never()).selectById(null);
        verify(activeOrderMapper).refreshActiveOrderSnapshot(argThat((MesProcessPoolActiveOrderDO update) ->
                Objects.equals(8101L, update.getId())
                        && Objects.equals(9903L, update.getQaRegulationVersionId())));
        ArgumentCaptor<MesPqcInspectionTaskDO> taskCaptor =
                ArgumentCaptor.forClass(MesPqcInspectionTaskDO.class);
        verify(pqcInspectionTaskMapper, times(4)).insert(taskCaptor.capture());
        assertTrue(taskCaptor.getAllValues().stream()
                .allMatch(task -> Objects.equals(9903L, task.getRegulationVersionId())
                        && Objects.equals(19903L, task.getQaProcessId())));
    }

    @Test
    void shouldRejectAmbiguousActiveOrderHistoryBeforeResolvingCurrentRoute() {
        stubWorkOrderExists(confirmedWorkOrder());
        when(activeOrderMapper.selectHistoryByWorkOrderIdForUpdate(9001L)).thenReturn(List.of(
                existingActiveOrder(8101L, "REMOVED", 7),
                existingActiveOrder(8102L, "REMOVED", 3)));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.addActiveOrder(activeOrderReq()));

        assertTrue(exception.getMessage().contains("多条历史记录"));
        verify(routeProductMapper, never()).selectListByItemIds(any());
        verify(routeVersionMapper, never()).selectListByRouteIds(any());
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldRejectRemovedOrderWhenCurrentDccProjectIsDisabled() {
        stubWorkOrderExists(confirmedWorkOrder());
        stubCandidatePqcPrerequisites(publishedRegulation(9902L));
        when(activeOrderMapper.selectHistoryByWorkOrderIdForUpdate(9001L))
                .thenReturn(List.of(existingActiveOrder(8101L, "REMOVED", 7)));
        when(dccProjectCodeMapper.selectById(147L)).thenReturn(DccProjectCodeDO.builder()
                .id(147L).productMasterId(11L).projectCode("ID").status("DISABLE").build());

        ServiceException exception = assertThrows(ServiceException.class, () -> service.addActiveOrder(activeOrderReq()));

        assertTrue(exception.getMessage().contains("DCC 项目代码不存在或已停用"));
        verify(activeOrderMapper, never()).reactivateRemovedActiveOrder(any(), any(), any(), any(), any());
        verify(inspectionRegulationMapper, never()).selectListByDccProjectCodeIds(any());
    }

    @Test
    void shouldRejectNewActiveOrderWhenDccProductMasterDriftedAfterRouteBinding() {
        stubWorkOrderExists(confirmedWorkOrder());
        stubCandidatePqcPrerequisites(publishedRegulation(9902L));
        when(routeProductMapper.selectListByRouteId(922119L)).thenReturn(List.of(
                MesProRouteProductDO.builder().id(7001L).routeId(922119L).itemId(1001L).build()));
        when(itemMapper.selectListByIds(List.of(1001L))).thenReturn(List.of(MesMdItemDO.builder()
                .id(1001L).productMasterId(11L).build()));
        when(dccProjectCodeMapper.selectById(147L)).thenReturn(DccProjectCodeDO.builder()
                .id(147L).productMasterId(12L).projectCode("ID").status("ENABLE").build());

        assertThrows(ServiceException.class, () -> service.addActiveOrder(activeOrderReq()));

        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldListActiveOrdersWithFormalRouteDisplayFieldsUsingBatchQueries() {
        List<MesProcessPoolActiveOrderDO> activeOrderRows = List.of(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .activeStatus("ACTIVE")
                .joinedAt(LocalDateTime.of(2026, 7, 31, 8, 30))
                .build());
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(activeOrderRows);
        when(routeMapper.selectBatchIds(List.of(922119L))).thenReturn(List.of(MesProRouteDO.builder()
                .id(922119L)
                .name("按压式球囊扩充压力泵工艺路线")
                .build()));
        when(routeVersionMapper.selectBatchIds(List.of(448L))).thenReturn(List.of(MesProRouteVersionDO.builder()
                .id(448L)
                .routeId(922119L)
                .versionNo("V1")
                .build()));
        when(workOrderMapper.selectBatchIds(List.of(9001L))).thenReturn(List.of(confirmedWorkOrder()));
        when(itemMapper.selectBatchIds(List.of(1001L))).thenReturn(List.of(MesMdItemDO.builder()
                .id(1001L)
                .code("AW.107.02.01.2010")
                .name("球囊扩张压力泵")
                .build()));
        when(abnormalStateService.findLatestOpenByWorkOrderIds(List.of(9001L))).thenReturn(Map.of(9001L,
                MesProcessPoolWorkOrderAbnormalDO.builder()
                        .id(8801L)
                        .workOrderId(9001L)
                        .abnormalDescription("设备停机")
                        .reportStatus(MesProcessPoolWorkOrderAbnormalDO.REPORT_STATUS_REPORTED)
                        .reportedAt(LocalDateTime.of(2026, 8, 7, 11, 0))
                        .build()));
        when(processSnapshotMapper.selectListByActiveOrderIds(List.of(8101L)))
                .thenReturn(processSnapshots(8101L, 9001L, 10));
        when(pqcInspectionTaskMapper.selectListByActiveOrderIds(List.of(8101L))).thenReturn(List.of());

        List<MesTeamLeaderActiveOrderRow> activeOrders = service.listActiveOrders(3001L);

        assertEquals(1, activeOrders.size());
        assertEquals(8101L, activeOrders.get(0).getId());
        assertEquals("WO-9001", activeOrders.get(0).getWorkOrderCode());
        assertEquals("BATCH-9001", activeOrders.get(0).getBatchCode());
        assertEquals("球囊扩张压力泵", activeOrders.get(0).getProductName());
        assertEquals("AW.107.02.01.2010", activeOrders.get(0).getProductCode());
        assertEquals(new BigDecimal("200"), activeOrders.get(0).getQuantity());
        assertEquals("按压式球囊扩充压力泵工艺路线", activeOrders.get(0).getRouteName());
        assertEquals("V1", activeOrders.get(0).getRouteVersionNo());
        assertEquals(new BigDecimal("0.000000"), activeOrders.get(0).getProductionProgressPercent());
        assertEquals(new BigDecimal("0.000000"), activeOrders.get(0).getInspectionProgressPercent());
        assertTrue(activeOrders.get(0).getAbnormal());
        assertEquals("设备停机", activeOrders.get(0).getAbnormalReason());
        assertEquals(LocalDateTime.of(2026, 8, 7, 11, 0), activeOrders.get(0).getAbnormalReportedAt());
        verify(activeOrderMapper).selectActiveListByLeader(3001L);
        verify(routeMapper).selectBatchIds(List.of(922119L));
        verify(routeVersionMapper).selectBatchIds(List.of(448L));
        verify(workOrderMapper).selectBatchIds(List.of(9001L));
        verify(itemMapper).selectBatchIds(List.of(1001L));
        verify(activeOrderMapper, never()).selectActiveList();
        verify(scheduleOrderProcessMapper, never()).selectListByScheduleOrderId(any());
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(scheduleOrderMapper, never()).selectEffectiveListByWorkOrderIds(any());
    }

    @Test
    void shouldListActiveOrdersWithProductionProgressByFormalProcessAndInspectionProgressByFixedPqcTasks() {
        List<MesProcessPoolActiveOrderDO> activeOrderRows = List.of(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .activeStatus("ACTIVE")
                .joinedAt(LocalDateTime.of(2026, 7, 31, 8, 30))
                .build());
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(activeOrderRows);
        when(routeMapper.selectBatchIds(List.of(922119L))).thenReturn(List.of(MesProRouteDO.builder()
                .id(922119L)
                .name("按压式球囊扩充压力泵工艺路线")
                .build()));
        when(routeVersionMapper.selectBatchIds(List.of(448L))).thenReturn(List.of(MesProRouteVersionDO.builder()
                .id(448L)
                .routeId(922119L)
                .versionNo("V1")
                .build()));
        when(workOrderMapper.selectBatchIds(List.of(9001L))).thenReturn(List.of(confirmedWorkOrder()));
        when(itemMapper.selectBatchIds(List.of(1001L))).thenReturn(List.of(MesMdItemDO.builder()
                .id(1001L)
                .code("AW.107.02.01.2010")
                .name("球囊扩张压力泵")
                .build()));
        when(processSnapshotMapper.selectListByActiveOrderIds(List.of(8101L)))
                .thenReturn(processSnapshots(8101L, 9001L, 10));
        when(reportAllocationMapper.selectListByActiveOrderIds(List.of(8101L))).thenReturn(List.of(
                allocation(8101L, 9001L, 5001L, 6001L, "200"),
                allocation(8101L, 9001L, 5002L, 6002L, "199"),
                allocation(8101L, 9001L, 5099L, 6099L, "200")));
        when(pqcInspectionTaskMapper.selectListByActiveOrderIds(List.of(8101L))).thenReturn(List.of(
                pqcTask(8101L, 5003L, 6003L, MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED),
                pqcTask(8101L, 5004L, 6004L, MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED),
                pqcTask(8101L, 5005L, 6005L, MesPqcInspectionTaskDO.TASK_STATUS_PENDING)));

        List<MesTeamLeaderActiveOrderRow> activeOrders = service.listActiveOrders(3001L);

        assertEquals(1, activeOrders.size());
        assertEquals(new BigDecimal("10.000000"), activeOrders.get(0).getProductionProgressPercent());
        assertEquals(new BigDecimal("33.333333"), activeOrders.get(0).getInspectionProgressPercent());
        verify(processSnapshotMapper).selectListByActiveOrderIds(List.of(8101L));
        verify(reportAllocationMapper).selectListByActiveOrderIds(List.of(8101L));
        verify(pqcInspectionTaskMapper).selectListByActiveOrderIds(List.of(8101L));
    }

    @Test
    void shouldListActiveOrdersWithPerProcessRemainingAndQuantityConflictAfterOverage() {
        List<MesProcessPoolActiveOrderDO> activeOrderRows = List.of(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .activeStatus("ACTIVE")
                .joinedAt(LocalDateTime.of(2026, 7, 31, 8, 30))
                .build());
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(activeOrderRows);
        when(routeMapper.selectBatchIds(List.of(922119L))).thenReturn(List.of(MesProRouteDO.builder()
                .id(922119L)
                .name("按压式球囊扩充压力泵工艺路线")
                .build()));
        when(routeVersionMapper.selectBatchIds(List.of(448L))).thenReturn(List.of(MesProRouteVersionDO.builder()
                .id(448L)
                .routeId(922119L)
                .versionNo("V1")
                .build()));
        when(workOrderMapper.selectBatchIds(List.of(9001L))).thenReturn(List.of(confirmedWorkOrder(new BigDecimal("100"))));
        when(itemMapper.selectBatchIds(List.of(1001L))).thenReturn(List.of(MesMdItemDO.builder()
                .id(1001L)
                .code("AW.107.02.01.2010")
                .name("球囊扩张压力泵")
                .build()));
        when(processSnapshotMapper.selectListByActiveOrderIds(List.of(8101L))).thenReturn(List.of(
                processSnapshot(8101L, 9001L, 5001L, 6001L, "100.000000"),
                processSnapshot(8101L, 9001L, 5002L, 6002L, "100.000000")));
        when(reportAllocationMapper.selectListByActiveOrderIds(List.of(8101L))).thenReturn(List.of(
                allocation(8101L, 9001L, 5001L, 6001L, "70"),
                allocation(8101L, 9001L, 5002L, 6002L, "130")));
        when(pqcInspectionTaskMapper.selectListByActiveOrderIds(List.of(8101L))).thenReturn(List.of());

        List<MesTeamLeaderActiveOrderRow> activeOrders = service.listActiveOrders(3001L);

        assertEquals(1, activeOrders.size());
        assertEquals(2, activeOrders.get(0).getProcessRemainingQuantities().size());
        assertEquals(5001L, activeOrders.get(0).getProcessRemainingQuantities().get(0).getRouteProcessId());
        assertEquals(6001L, activeOrders.get(0).getProcessRemainingQuantities().get(0).getProcessId());
        assertEquals(new BigDecimal("100.000000"),
                activeOrders.get(0).getProcessRemainingQuantities().get(0).getPlannedQuantity());
        assertEquals(new BigDecimal("70"),
                activeOrders.get(0).getProcessRemainingQuantities().get(0).getAllocatedQuantity());
        assertEquals(new BigDecimal("30.000000"),
                activeOrders.get(0).getProcessRemainingQuantities().get(0).getRemainingQuantity());
        assertEquals(new BigDecimal("0.000000"),
                activeOrders.get(0).getProcessRemainingQuantities().get(1).getRemainingQuantity());
        assertFalse(activeOrders.get(0).getProcessRemainingQuantities().get(0).getQuantityConflict());
        assertTrue(activeOrders.get(0).getProcessRemainingQuantities().get(1).getQuantityConflict());
        assertEquals(new BigDecimal("30.000000"),
                activeOrders.get(0).getProcessRemainingQuantities().get(1).getOverageQuantity());
        assertTrue(activeOrders.get(0).getHasQuantityConflict());
        assertTrue(activeOrders.get(0).getQuantityConflict());
        assertEquals(1, activeOrders.get(0).getQuantityConflictProcessCount());
        assertEquals(new BigDecimal("30.000000"), activeOrders.get(0).getOverageQuantity());
    }

    @Test
    void shouldRecalculateProductionProgressFromCurrentAllocationAfterQuantityReduction() {
        List<MesProcessPoolActiveOrderDO> activeOrderRows = List.of(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .erpFixedQuantitySnapshot(new BigDecimal("200"))
                .activeStatus("ACTIVE")
                .joinedAt(LocalDateTime.of(2026, 7, 31, 8, 30))
                .build());
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(activeOrderRows);
        when(routeMapper.selectBatchIds(List.of(922119L))).thenReturn(List.of(MesProRouteDO.builder()
                .id(922119L)
                .name("按压式球囊扩充压力泵工艺路线")
                .build()));
        when(routeVersionMapper.selectBatchIds(List.of(448L))).thenReturn(List.of(MesProRouteVersionDO.builder()
                .id(448L)
                .routeId(922119L)
                .versionNo("V1")
                .routeSnapshotJson(activeRouteSnapshotJson(10))
                .build()));
        when(workOrderMapper.selectBatchIds(List.of(9001L))).thenReturn(List.of(confirmedWorkOrder()));
        when(itemMapper.selectBatchIds(List.of(1001L))).thenReturn(List.of(MesMdItemDO.builder()
                .id(1001L)
                .code("AW.107.02.01.2010")
                .name("球囊扩张压力泵")
                .build()));
        when(processSnapshotMapper.selectListByActiveOrderIds(List.of(8101L)))
                .thenReturn(formalRouteProcessSnapshots(8101L, 9001L, 10));
        when(reportAllocationMapper.selectListByActiveOrderIds(List.of(8101L))).thenReturn(List.of(
                allocation(8101L, 9001L, 928601L, 6001L, "199"),
                allocation(8101L, 9001L, 928602L, 6002L, "200")));
        when(pqcInspectionTaskMapper.selectListByActiveOrderIds(List.of(8101L))).thenReturn(List.of());

        List<MesTeamLeaderActiveOrderRow> activeOrders = service.listActiveOrders(3001L);

        assertEquals(1, activeOrders.size());
        assertEquals(new BigDecimal("10.000000"), activeOrders.get(0).getProductionProgressPercent());
        verify(reportAllocationMapper).selectListByActiveOrderIds(List.of(8101L));
    }

    @Test
    void shouldCalculateProductionProgressFromFormalRouteWhenActiveOrderSnapshotIsIncomplete() {
        List<MesProcessPoolActiveOrderDO> activeOrderRows = List.of(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .erpFixedQuantitySnapshot(new BigDecimal("200"))
                .activeStatus("ACTIVE")
                .joinedAt(LocalDateTime.of(2026, 7, 31, 8, 30))
                .build());
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(activeOrderRows);
        when(routeMapper.selectBatchIds(List.of(922119L))).thenReturn(List.of(MesProRouteDO.builder()
                .id(922119L)
                .name("按压式球囊扩充压力泵工艺路线")
                .build()));
        when(routeVersionMapper.selectBatchIds(List.of(448L))).thenReturn(List.of(MesProRouteVersionDO.builder()
                .id(448L)
                .routeId(922119L)
                .versionNo("V1")
                .routeSnapshotJson(activeRouteSnapshotJson(10))
                .build()));
        when(workOrderMapper.selectBatchIds(List.of(9001L))).thenReturn(List.of(confirmedWorkOrder()));
        when(itemMapper.selectBatchIds(List.of(1001L))).thenReturn(List.of(MesMdItemDO.builder()
                .id(1001L)
                .code("AW.107.02.01.2010")
                .name("球囊扩张压力泵")
                .build()));
        when(processSnapshotMapper.selectListByActiveOrderIds(List.of(8101L)))
                .thenReturn(formalRouteProcessSnapshots(8101L, 9001L, 1));
        when(reportAllocationMapper.selectListByActiveOrderIds(List.of(8101L))).thenReturn(List.of(
                allocation(8101L, 9001L, 928601L, 6001L, "200")));
        when(pqcInspectionTaskMapper.selectListByActiveOrderIds(List.of(8101L))).thenReturn(List.of());

        List<MesTeamLeaderActiveOrderRow> activeOrders = service.listActiveOrders(3001L);

        assertEquals(1, activeOrders.size());
        assertEquals(new BigDecimal("10.000000"), activeOrders.get(0).getProductionProgressPercent());
        assertEquals(new BigDecimal("0.000000"), activeOrders.get(0).getInspectionProgressPercent());
    }

    @Test
    void shouldSkipActiveOrderListWhenFormalRouteIsMissing() {
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(List.of(
                MesProcessPoolActiveOrderDO.builder()
                        .id(8101L)
                        .routeId(922119L)
                        .routeVersionId(448L)
                        .build()));
        when(routeMapper.selectBatchIds(List.of(922119L))).thenReturn(List.of());

        assertTrue(service.listActiveOrders(3001L).isEmpty());
        verify(routeVersionMapper).selectBatchIds(List.of(448L));
    }

    @Test
    void shouldDisplayActiveOrderFromFrozenRouteSnapshotWhenRouteMasterWasDeleted() {
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(List.of(
                MesProcessPoolActiveOrderDO.builder()
                        .id(8101L)
                        .leaderUserId(3001L)
                        .workOrderId(9001L)
                        .routeId(922119L)
                        .routeVersionId(448L)
                        .erpFixedQuantitySnapshot(new BigDecimal("200"))
                        .activeStatus("ACTIVE")
                        .joinedAt(LocalDateTime.of(2026, 7, 31, 8, 30))
                        .build()));
        when(routeMapper.selectBatchIds(List.of(922119L))).thenReturn(List.of());
        when(routeVersionMapper.selectBatchIds(List.of(448L))).thenReturn(List.of(
                MesProRouteVersionDO.builder()
                        .id(448L)
                        .routeId(922119L)
                        .versionNo("V1")
                        .routeSnapshotJson(activeRouteSnapshotJsonWithRouteIdentity(1))
                        .build()));
        when(workOrderMapper.selectBatchIds(List.of(9001L))).thenReturn(List.of(confirmedWorkOrder()));
        when(itemMapper.selectBatchIds(List.of(1001L))).thenReturn(List.of(MesMdItemDO.builder()
                .id(1001L)
                .code("AW.107.02.01.2010")
                .name("球囊扩张压力泵")
                .build()));
        when(processSnapshotMapper.selectListByActiveOrderIds(List.of(8101L)))
                .thenReturn(List.of(frozenProcessSnapshot()));
        when(pqcInspectionTaskMapper.selectListByActiveOrderIds(List.of(8101L))).thenReturn(List.of());

        List<MesTeamLeaderActiveOrderRow> activeOrders = service.listActiveOrders(3001L);

        assertEquals(1, activeOrders.size());
        assertEquals("冻结工艺路线", activeOrders.get(0).getRouteName());
    }

    @Test
    void shouldSkipInvalidActiveOrderWithoutBlockingValidRows() {
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(List.of(
                MesProcessPoolActiveOrderDO.builder()
                        .id(8101L)
                        .leaderUserId(3001L)
                        .workOrderId(9001L)
                        .routeId(922119L)
                        .routeVersionId(448L)
                        .erpFixedQuantitySnapshot(new BigDecimal("200"))
                        .activeStatus("ACTIVE")
                        .joinedAt(LocalDateTime.of(2026, 7, 31, 8, 30))
                        .build(),
                MesProcessPoolActiveOrderDO.builder()
                        .id(8102L)
                        .leaderUserId(3001L)
                        .workOrderId(9002L)
                        .routeId(9999L)
                        .routeVersionId(9998L)
                        .erpFixedQuantitySnapshot(new BigDecimal("200"))
                        .activeStatus("ACTIVE")
                        .joinedAt(LocalDateTime.of(2026, 7, 31, 8, 31))
                        .build()));
        when(routeMapper.selectBatchIds(List.of(922119L, 9999L))).thenReturn(List.of(MesProRouteDO.builder()
                .id(922119L)
                .name("按压式球囊扩充压力泵工艺路线")
                .build()));
        when(routeVersionMapper.selectBatchIds(List.of(448L, 9998L))).thenReturn(List.of(
                MesProRouteVersionDO.builder()
                        .id(448L)
                        .routeId(922119L)
                        .versionNo("V1")
                        .routeSnapshotJson(activeRouteSnapshotJson(1))
                        .build()));
        when(workOrderMapper.selectBatchIds(List.of(9001L))).thenReturn(List.of(confirmedWorkOrder()));
        when(itemMapper.selectBatchIds(List.of(1001L))).thenReturn(List.of(MesMdItemDO.builder()
                .id(1001L)
                .code("AW.107.02.01.2010")
                .name("球囊扩张压力泵")
                .build()));
        when(processSnapshotMapper.selectListByActiveOrderIds(List.of(8101L)))
                .thenReturn(List.of(frozenProcessSnapshot()));
        when(pqcInspectionTaskMapper.selectListByActiveOrderIds(List.of(8101L))).thenReturn(List.of());

        List<MesTeamLeaderActiveOrderRow> activeOrders = service.listActiveOrders(3001L);

        assertEquals(List.of(8101L), activeOrders.stream()
                .map(MesTeamLeaderActiveOrderRow::getId)
                .toList());
    }

    @Test
    void shouldPropagateUnknownActiveOrderListException() {
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(List.of(
                MesProcessPoolActiveOrderDO.builder()
                        .id(8101L)
                        .routeId(922119L)
                        .routeVersionId(448L)
                        .build()));
        doThrow(new ServiceException(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED))
                .when(routeMapper).selectBatchIds(any());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.listActiveOrders(3001L));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void shouldSkipActiveOrderListWhenVersionDoesNotBelongToRoute() {
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(List.of(
                MesProcessPoolActiveOrderDO.builder()
                        .id(8101L)
                        .routeId(922119L)
                        .routeVersionId(448L)
                        .build()));
        when(routeMapper.selectBatchIds(List.of(922119L))).thenReturn(List.of(MesProRouteDO.builder()
                .id(922119L)
                .name("按压式球囊扩充压力泵工艺路线")
                .build()));
        when(routeVersionMapper.selectBatchIds(List.of(448L))).thenReturn(List.of(MesProRouteVersionDO.builder()
                .id(448L)
                .routeId(922120L)
                .versionNo("V1")
                .build()));

        assertTrue(service.listActiveOrders(3001L).isEmpty());
    }

    private static MesTeamLeaderActiveOrderAddReqBO activeOrderReq() {
        return MesTeamLeaderActiveOrderAddReqBO.builder()
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .pickListId(9001L)
                .idempotencyKey("IDEMP-9001")
                .build();
    }

    private static String defaultPickListSnapshotHash() {
        try {
            Method method = MesTeamLeaderActiveOrderServiceImpl.class.getDeclaredMethod("pickListSnapshotHash",
                    ErpKingdeeProductionPickListDO.class, List.class);
            method.setAccessible(true);
            ErpKingdeeProductionPickListDO header = ErpKingdeeProductionPickListDO.builder()
                    .id(9001L).sourceFid("9001").sourceBillNo("PICK-9001").documentStatus("C").build();
            ErpKingdeeProductionPickListItemDO item = ErpKingdeeProductionPickListItemDO.builder()
                    .id(9101L).sourceEntryId("10").sourceLineKey("9001:10").materialNumber("MAT-001")
                    .materialName("手柄").unitName("个").actualQuantity(new BigDecimal("5"))
                    .requestedQuantity(new BigDecimal("6")).productionOrderNo("WO-9001").build();
            return (String) method.invoke(null, header, List.of(item));
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void stubWorkOrderExists(MesProWorkOrderDO workOrder) {
        when(workOrderService.validateWorkOrderExists(9001L)).thenReturn(workOrder);
    }

    private static MesProWorkOrderDO confirmedWorkOrder() {
        return confirmedWorkOrder(new BigDecimal("200"));
    }

    private static MesProWorkOrderDO confirmedWorkOrder(BigDecimal quantity) {
        return confirmedWorkOrder(9001L, "WO-9001", quantity, 1001L);
    }

    private static MesProWorkOrderDO confirmedWorkOrderWithPlannedStart() {
        return MesProWorkOrderDO.builder()
                .id(9001L)
                .code("WO-9001")
                .productId(1001L)
                .quantity(new BigDecimal("200"))
                .plannedStartTime(LocalDateTime.of(2026, 8, 5, 8, 0))
                .build();
    }

    private static MesProWorkOrderDO confirmedWorkOrder(Long id, String code, BigDecimal quantity, Long productId) {
        return MesProWorkOrderDO.builder()
                .id(id)
                .code(code)
                .batchCode("BATCH-" + id)
                .productId(productId)
                .quantity(quantity)
                .build();
    }

    private void stubEffectiveSchedules(MesProScheduleOrderDO... schedules) {
        stubEffectiveSchedulesFor(9001L, schedules);
    }

    private static List<MesProcessPoolActiveOrderProcessSnapshotDO> processSnapshots(Long activeOrderId,
                                                                                     Long workOrderId,
                                                                                     int processCount) {
        return java.util.stream.IntStream.rangeClosed(1, processCount)
                .mapToObj(index -> MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                        .activeOrderId(activeOrderId)
                        .workOrderId(workOrderId)
                        .routeId(922119L)
                        .routeVersionId(448L)
                        .routeProcessId(5000L + index)
                        .processId(6000L + index)
                        .plannedQuantitySnapshot(new BigDecimal("200.000000"))
                        .build())
                .toList();
    }

    private static MesProcessPoolActiveOrderProcessSnapshotDO processSnapshot(Long activeOrderId, Long workOrderId,
                                                                               Long routeProcessId, Long processId,
                                                                               String plannedQuantity) {
        return MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                .activeOrderId(activeOrderId)
                .workOrderId(workOrderId)
                .routeId(922119L)
                .routeVersionId(448L)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .plannedQuantitySnapshot(new BigDecimal(plannedQuantity))
                .build();
    }

    private static List<MesProcessPoolActiveOrderProcessSnapshotDO> formalRouteProcessSnapshots(Long activeOrderId,
                                                                                                 Long workOrderId,
                                                                                                 int processCount) {
        return java.util.stream.IntStream.rangeClosed(1, processCount)
                .mapToObj(index -> MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                        .activeOrderId(activeOrderId)
                        .workOrderId(workOrderId)
                        .routeId(922119L)
                        .routeVersionId(448L)
                        .routeProcessId(928600L + index)
                        .processId(6000L + index)
                        .plannedQuantitySnapshot(new BigDecimal("200.000000"))
                        .build())
                .toList();
    }

    private static MesProcessPoolReportAllocationDO allocation(Long activeOrderId, Long workOrderId,
                                                               Long routeProcessId, Long processId,
                                                               String quantity) {
        return MesProcessPoolReportAllocationDO.builder()
                .activeOrderId(activeOrderId)
                .workOrderId(workOrderId)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .allocatedQuantity(new BigDecimal(quantity))
                .lifecycleStatus(MesProcessPoolReportAllocationDO.LIFECYCLE_CURRENT)
                .build();
    }

    private static MesPqcInspectionTaskDO pqcTask(Long activeOrderId, Long routeProcessId, Long processId,
                                                   String status) {
        return MesPqcInspectionTaskDO.builder()
                .activeOrderId(activeOrderId)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .taskStatus(status)
                .build();
    }

    private void stubEffectiveSchedulesFor(Long workOrderId, MesProScheduleOrderDO... schedules) {
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(List.of(workOrderId))).thenReturn(List.of(schedules));
    }

    private void stubSuccessfulInsertAndProcesses(List<MesProScheduleOrderProcessDO> processes) {
        stubSuccessfulActiveOrderInsert();
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(7701L)).thenReturn(processes);
    }

    private void stubSuccessfulActiveOrderInsert() {
        when(activeOrderMapper.insert(any(MesProcessPoolActiveOrderDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolActiveOrderDO.class).setId(8101L);
            return 1;
        });
        when(processSnapshotMapper.insertBatch(any())).thenReturn(Boolean.TRUE);
    }

    private void stubUnscheduledActiveRoute() {
        stubUnscheduledActiveRoute(activeRouteSnapshotJson());
    }

    private void stubUnscheduledActiveRoute(String routeSnapshotJson) {
        when(routeProductMapper.selectListByItemIds(List.of(1001L))).thenReturn(List.of(
                MesProRouteProductDO.builder().id(7001L).itemId(1001L).routeId(922119L).build()));
        when(routeVersionMapper.selectListByRouteIds(List.of(922119L))).thenReturn(List.of(
                activeRouteVersion(routeSnapshotJson)));
    }

    private static MesProRouteVersionDO activeRouteVersion(String routeSnapshotJson) {
        return MesProRouteVersionDO.builder()
                .id(448L)
                .routeId(922119L)
                .active(Boolean.TRUE)
                .lifecycleStatus("ACTIVE")
                .routeSnapshotJson(routeSnapshotJson)
                .build();
    }

    private static String activeRouteSnapshotJson() {
        return """
                {
                  "configSnapshots": {
                    "flowGraph": {
                      "nodes": [
                        {"routeProcessId": 928609, "processId": 6001, "processCode": "PROC-6001",
                         "processName": "工序6001", "sort": 10}
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

    private static String activeRouteSnapshotJsonWithoutProductionQuantityFactor() {
        return """
                {
                  "configSnapshots": {
                    "flowGraph": {
                      "nodes": [
                        {"routeProcessId": 928609, "processId": 6001, "processCode": "PROC-6001",
                         "processName": "工序6001", "sort": 10}
                      ]
                    },
                    "scheduleUseConfigs": [
                      {
                        "routeId": 922119,
                        "routeProcessId": 928609,
                        "useType": "SCHEDULE",
                        "enabled": true
                      }
                    ]
                  }
                }
                """;
    }

    private static MesProScheduleOrderDO effectiveSchedule(Long id, Long routeId, Long routeVersionId) {
        return effectiveSchedule(id, 9001L, routeId, routeVersionId);
    }

    private static MesProScheduleOrderDO effectiveSchedule(Long id, Long workOrderId, Long routeId,
                                                           Long routeVersionId) {
        return MesProScheduleOrderDO.builder()
                .id(id)
                .workOrderId(workOrderId)
                .productId(1001L)
                .routeId(routeId)
                .routeVersionId(routeVersionId)
                .build();
    }

    private static MesProcessPoolActiveOrderDO existingActiveOrder(Long id, String status, Integer version) {
        return MesProcessPoolActiveOrderDO.builder()
                .id(id)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .activeStatus(status)
                .businessStatus(status)
                .dccProjectCodeId(147L)
                .qaRegulationId(9901L)
                .qaRegulationVersionId(9902L)
                .erpFixedQuantitySnapshot(new BigDecimal("200.000000"))
                .version(version)
                .joinedAt(LocalDateTime.of(2026, 8, 4, 8, 30))
                .removedAt("REMOVED".equals(status) ? LocalDateTime.of(2026, 8, 4, 10, 30) : null)
                .build();
    }

    private static MesProcessPoolActiveOrderProcessSnapshotDO frozenProcessSnapshot() {
        return MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                .id(8201L)
                .activeOrderId(8101L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .routeProcessId(928601L)
                .processId(6001L)
                .erpFixedQuantitySnapshot(new BigDecimal("200.000000"))
                .productionQuantityFactorSnapshot(new BigDecimal("1.000000"))
                .plannedQuantitySnapshot(new BigDecimal("200.000000"))
                .build();
    }

    private static List<MesPqcInspectionTaskDO> frozenPqcTasks() {
        return List.of(
                frozenPqcTask(8301L, "FIRST", "FIRST", 5),
                frozenPqcTask(8302L, "PATROL", "PATROL_AM", 10),
                frozenPqcTask(8303L, "PATROL", "PATROL_PM", 10),
                frozenPqcTask(8304L, "FINAL", "FINAL", 3));
    }

    private static MesPqcInspectionTaskDO frozenPqcTask(Long id, String inspectionType,
                                                         String inspectionRuleKey, Integer plannedQuantity) {
        return MesPqcInspectionTaskDO.builder()
                .id(id)
                .activeOrderId(8101L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .routeProcessId(928601L)
                .processId(6001L)
                .qaProcessId(19902L)
                .qaItemCode(inspectionType + "-001")
                .regulationVersionId(9902L)
                .inspectionType(inspectionType)
                .inspectionRuleKey(inspectionRuleKey)
                .businessDate(LocalDate.of(2026, 8, 4))
                .shiftCode(switch (inspectionRuleKey) {
                    case "PATROL_AM" -> "AM";
                    case "PATROL_PM" -> "PM";
                    default -> inspectionRuleKey;
                })
                .roundNo(1)
                .plannedInspectionQuantity(plannedQuantity)
                .actualInspectionQuantity(0)
                .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_PENDING)
                .build();
    }

    private void stubRebuildHistoricalRuntimePreview() {
        MesPqcInspectionTaskDO submittedTask = frozenPqcTask(8305L, "FIRST", "FIRST", 5)
                .setTaskStatus(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED)
                .setSubmittedEventId(8802L);
        when(activeOrderMapper.selectByIdForUpdate(8101L))
                .thenReturn(existingActiveOrder(8101L, "ACTIVE", 7));
        when(reportAllocationMapper.selectAllListByActiveOrderIdForUpdate(8101L)).thenReturn(List.of(
                MesProcessPoolReportAllocationDO.builder()
                        .id(8401L)
                        .activeOrderId(8101L)
                        .eventId(8801L)
                        .workOrderId(9001L)
                        .build()));
        when(pqcInspectionTaskMapper.selectListByActiveOrderIdForUpdate(8101L)).thenReturn(List.of(submittedTask));
        when(orderProcessCompletionMapper.selectListByWorkOrderIdsForUpdate(List.of(9001L))).thenReturn(List.of(
                MesProcessPoolOrderProcessCompletionDO.builder()
                        .id(8501L)
                        .workOrderId(9001L)
                        .routeProcessId(928601L)
                        .processId(6001L)
                        .build()));
        when(processSnapshotMapper.selectListByActiveOrderIdForUpdate(8101L))
                .thenReturn(List.of(frozenProcessSnapshot()));
        when(releaseApplicationMapper.selectListByActiveOrderIdsForUpdate(List.of(8101L))).thenReturn(List.of(
                MesProcessPoolActiveOrderReleaseApplicationDO.builder()
                        .id(8601L)
                        .activeOrderId(8101L)
                        .workOrderId(9001L)
                        .build()));
        when(processPoolEventMapper.selectListPqcByTaskId(any(), eq(8305L))).thenReturn(List.of(
                MesProProcessPoolEventDO.builder()
                        .id(8803L)
                        .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                        .feedbackSourceId(8305L)
                        .build()));
        when(processPoolEventMapper.selectByIdForUpdate(8801L)).thenReturn(
                MesProProcessPoolEventDO.builder()
                        .id(8801L)
                        .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                        .feedbackSourceId(5501L)
                        .build());
        when(processPoolEventMapper.selectByIdForUpdate(8802L)).thenReturn(
                MesProProcessPoolEventDO.builder()
                        .id(8802L)
                        .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                        .feedbackSourceId(8305L)
                        .build());
        when(processPoolEventMapper.selectByIdForUpdate(8803L)).thenReturn(
                MesProProcessPoolEventDO.builder()
                        .id(8803L)
                        .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                        .feedbackSourceId(8305L)
                        .build());
        lenient().when(reportAllocationMapper.selectAllListByEventIdForUpdate(8801L)).thenReturn(List.of(
                MesProcessPoolReportAllocationDO.builder()
                        .id(8401L)
                        .activeOrderId(8101L)
                        .eventId(8801L)
                        .build()));
        when(pqcAggregateDetailMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcProcessInspectionAggregateDetailDO.builder()
                        .id(8701L)
                        .activeOrderId(8101L)
                        .pqcTaskId(8305L)
                        .eventId(8803L)
                        .build()));
    }

    private void verifyNoActiveOrderWrites() {
        verify(activeOrderMapper, never()).insert(any(MesProcessPoolActiveOrderDO.class));
        verify(activeOrderMapper, never()).reactivateRemovedActiveOrder(any(), any(), any(), any(), any());
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(pqcInspectionTaskMapper, never()).insert(any(MesPqcInspectionTaskDO.class));
        verify(auditMapper, never()).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    private static MesProScheduleOrderProcessDO scheduleProcess(Long routeProcessId, Long processId, String factor,
                                                                String plannedQuantity) {
        return scheduleProcess(7701L, routeProcessId, processId, factor, plannedQuantity);
    }

    private static MesProScheduleOrderProcessDO scheduleProcess(Long scheduleOrderId, Long routeProcessId,
                                                                Long processId, String factor,
                                                                String plannedQuantity) {
        return scheduleProcess(scheduleOrderId, routeProcessId, processId, factor, plannedQuantity,
                LocalDate.of(2026, 8, 5));
    }

    private static MesProScheduleOrderProcessDO scheduleProcessWithoutPlanDate(Long routeProcessId, Long processId,
                                                                               String factor,
                                                                               String plannedQuantity) {
        return scheduleProcess(7701L, routeProcessId, processId, factor, plannedQuantity, null);
    }

    private static MesProScheduleOrderProcessDO scheduleProcess(Long scheduleOrderId, Long routeProcessId,
                                                                Long processId, String factor,
                                                                String plannedQuantity, LocalDate planDate) {
        return MesProScheduleOrderProcessDO.builder()
                .scheduleOrderId(scheduleOrderId)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .enabled(Boolean.TRUE)
                .planDate(planDate)
                .productionQuantityFactor(new BigDecimal(factor))
                .plannedQuantity(new BigDecimal(plannedQuantity))
                .build();
    }

    private static MesQaInspectionRegulationDO publishedRegulation(Long versionId) {
        return publishedRegulation(versionId, 928609L, 6001L);
    }

    private static MesQaInspectionRegulationDO publishedRegulation(Long versionId, Long routeProcessId,
                                                                   Long processId) {
        return publishedRegulation(versionId, 1001L, 922119L, 448L, routeProcessId, processId);
    }

    private static MesQaInspectionRegulationDO publishedRegulation(Long versionId, Long productId, Long routeId,
                                                                   Long routeVersionId, Long routeProcessId,
                                                                   Long processId) {
        return MesQaInspectionRegulationDO.builder()
                .id(9901L)
                .dccProjectCodeId(147L)
                .productId(productId)
                .routeId(routeId)
                .routeVersionId(routeVersionId)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .ownerModule(MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA)
                .lifecycleStatus("PUBLISHED")
                .currentVersionId(versionId)
                .build();
    }

    private void stubCandidatePqcPrerequisites(MesQaInspectionRegulationDO... regulations) {
        stubFormalRouteQaContext(1001L, 448L, regulations);
    }

    private void stubFormalRouteQaContext(Long workOrderProductId, Long activeRouteVersionId,
                                          MesQaInspectionRegulationDO... regulations) {
        stubFormalRouteQaContext(workOrderProductId, activeRouteVersionId, activeRouteSnapshotJson(List.of(regulations)),
                regulations);
    }

    private void stubFormalRouteQaContext(Long workOrderProductId, Long activeRouteVersionId, String routeSnapshotJson,
                                          MesQaInspectionRegulationDO... regulations) {
        List<MesQaInspectionRegulationDO> regulationList = List.of(regulations);
        LinkedHashSet<Long> routeItemIds = new LinkedHashSet<>();
        routeItemIds.add(workOrderProductId);
        regulationList.stream().map(MesQaInspectionRegulationDO::getProductId).forEach(routeItemIds::add);
        routeItemIds.add(924005L);
        List<MesProRouteProductDO> routeProducts = routeItemIds.stream()
                .map(itemId -> MesProRouteProductDO.builder()
                        .id(7000L + itemId)
                        .routeId(922119L)
                        .itemId(itemId)
                        .build())
                .toList();
        lenient().when(routeProductMapper.selectListByRouteId(922119L)).thenReturn(routeProducts);
        lenient().when(itemMapper.selectListByIds(any())).thenAnswer(invocation -> {
            Collection<Long> requestedItemIds = invocation.getArgument(0);
            return requestedItemIds.stream()
                    .map(itemId -> MesMdItemDO.builder().id(itemId).productMasterId(11L).build())
                    .toList();
        });
        lenient().when(routeProductMapper.selectListByItemIds(any())).thenAnswer(invocation -> {
            Collection<Long> requestedItemIds = invocation.getArgument(0);
            return requestedItemIds.contains(workOrderProductId)
                    ? List.of(MesProRouteProductDO.builder()
                    .id(7001L)
                    .routeId(922119L)
                    .itemId(workOrderProductId)
                    .build())
                    : List.of();
        });
        lenient().when(routeProductMapper.selectListByRouteIds(any())).thenReturn(routeProducts);
        lenient().when(routeMapper.selectBatchIds(any())).thenReturn(List.of(MesProRouteDO.builder()
                .id(922119L)
                .code("ROUTE-922119")
                .build()));
        lenient().when(routeVersionMapper.selectListByRouteIds(any())).thenReturn(List.of(MesProRouteVersionDO.builder()
                .id(activeRouteVersionId)
                .routeId(922119L)
                .versionNo("V-ACTIVE")
                .active(Boolean.TRUE)
                .lifecycleStatus("ACTIVE")
                .routeSnapshotJson(routeSnapshotJson)
                .build()));
        lenient().when(itemMapper.selectBatchIds(any())).thenReturn(List.of(MesMdItemDO.builder()
                .id(924005L)
                .code("ID")
                .name("球囊扩张压力泵")
                .build()));
        lenient().when(routeDccProjectBindingMapper.selectCurrentByRouteId(922119L)).thenReturn(MesRouteDccProjectBindingDO.builder()
                .id(61001L)
                .routeId(922119L)
                .dccProjectCodeId(147L)
                .version(1L)
                .build());
        lenient().when(dccProjectCodeMapper.selectById(147L)).thenReturn(DccProjectCodeDO.builder()
                .id(147L)
                .productMasterId(11L)
                .projectCode("ID")
                .projectName("球囊扩张压力泵")
                .status("ENABLE")
                .build());
        lenient().when(inspectionRegulationMapper.selectListByDccProjectCodeIds(any())).thenReturn(regulationList);
        if (!regulationList.isEmpty()) {
            lenient().when(inspectionRegulationVersionMapper.selectBatchIds(any())).thenAnswer(invocation ->
                    regulationList.stream()
                            .map(MesQaInspectionRegulationDO::getCurrentVersionId)
                            .map(versionId -> publishedRegulationVersion(versionId, true, null))
                            .toList());
            lenient().when(inspectionRegulationItemMapper.selectListByVersionIds(any())).thenAnswer(invocation ->
                    regulationList.stream()
                            .map(MesQaInspectionRegulationDO::getCurrentVersionId)
                            .flatMap(versionId -> defaultPqcItems(versionId).stream())
                            .toList());
            lenient().when(inspectionRegulationProcessMapper.selectListByVersionIds(any())).thenAnswer(invocation ->
                    regulationList.stream()
                            .map(MesQaInspectionRegulationDO::getCurrentVersionId)
                            .map(MesTeamLeaderActiveOrderServiceTest::qaProcess)
                            .toList());
        }
    }

    private static MesQaInspectionRegulationVersionDO publishedRegulationVersion(
            Boolean finalInspectionApplicable, String reason) {
        return publishedRegulationVersion(9902L, finalInspectionApplicable, reason);
    }

    private static MesQaInspectionRegulationVersionDO publishedRegulationVersion(
            Long id, Boolean finalInspectionApplicable, String reason) {
        return MesQaInspectionRegulationVersionDO.builder()
                .id(id)
                .regulationId(9901L)
                .versionNo("V21-QA-1")
                .lifecycleStatus("PUBLISHED")
                .finalInspectionApplicable(finalInspectionApplicable)
                .finalInspectionNotApplicableReason(reason)
                .inspectionTypeRulesJson("""
                        [
                          {"key":"FIRST","inspectionType":"FIRST","label":"首检","required":true,"fixedQuantity":5},
                          {"key":"PATROL_AM","inspectionType":"PATROL","label":"上午巡检","required":true},
                          {"key":"PATROL_PM","inspectionType":"PATROL","label":"下午巡检","required":true},
                          {"key":"FINAL","inspectionType":"FINAL","label":"末检","required":%s,"fixedQuantity":3}
                        ]
                        """.formatted(Boolean.TRUE.equals(finalInspectionApplicable)))
                .snapshotJson("{}")
                .build();
    }

    private static List<MesQaInspectionRegulationItemDO> defaultPqcItems() {
        return defaultPqcItems(9902L);
    }

    private static String activeRouteSnapshotJson(List<MesQaInspectionRegulationDO> regulations) {
        List<MesQaInspectionRegulationDO> source = regulations == null || regulations.isEmpty()
                ? List.of(publishedRegulation(9902L)) : regulations;
        String nodes = source.stream()
                .map(regulation -> "{\"routeProcessId\":" + regulation.getRouteProcessId()
                        + ",\"processId\":" + regulation.getProcessId()
                        + ",\"processCode\":\"" + processCode(regulation.getProcessId()) + "\""
                        + ",\"processName\":\"" + processName(regulation.getProcessId()) + "\""
                        + ",\"sort\":10}")
                .collect(java.util.stream.Collectors.joining(","));
        String configs = source.stream()
                .map(regulation -> "{\"routeId\":922119,\"routeProcessId\":" + regulation.getRouteProcessId()
                        + ",\"useType\":\"SCHEDULE\",\"enabled\":true,\"productionQuantityFactor\":1.000000}")
                .collect(java.util.stream.Collectors.joining(","));
        return """
                {
                  "configSnapshots": {
                    "flowGraph": {
                      "nodes": [%s]
                    },
                    "scheduleUseConfigs": [%s]
                  }
                }
                """.formatted(nodes, configs);
    }

    private static String activeRouteSnapshotJson(int processCount) {
        String nodes = java.util.stream.IntStream.rangeClosed(1, processCount)
                .mapToObj(index -> "{\"routeProcessId\":" + (928600L + index)
                        + ",\"processId\":" + (6000L + index)
                        + ",\"processCode\":\"" + processCode(6000L + index) + "\""
                        + ",\"processName\":\"" + processName(6000L + index) + "\""
                        + ",\"sort\":" + (index * 10) + "}")
                .collect(java.util.stream.Collectors.joining(","));
        String configs = java.util.stream.IntStream.rangeClosed(1, processCount)
                .mapToObj(index -> "{\"routeId\":922119,\"routeProcessId\":" + (928600L + index)
                        + ",\"useType\":\"SCHEDULE\",\"enabled\":true,\"productionQuantityFactor\":1.000000}")
                .collect(java.util.stream.Collectors.joining(","));
        return """
                {
                  "configSnapshots": {
                    "flowGraph": {
                      "nodes": [%s]
                    },
                    "scheduleUseConfigs": [%s]
                  }
                }
                """.formatted(nodes, configs);
    }

    private static String activeRouteSnapshotJsonWithRouteIdentity(int processCount) {
        String snapshot = activeRouteSnapshotJson(processCount);
        return snapshot.replaceFirst("\\{\\s*\\\"configSnapshots\\\":",
                "{\\\"routeId\\\":922119,\\\"routeCode\\\":\\\"ROUTE-001\\\","
                        + "\\\"routeName\\\":\\\"冻结工艺路线\\\",\\\"configSnapshots\\\":");
    }

    private static List<MesQaInspectionRegulationItemDO> defaultPqcItems(Long regulationVersionId) {
        return List.of(
                pqcItem(regulationVersionId, "FIRST", 5, null),
                pqcItem(regulationVersionId, "PATROL", null, new BigDecimal("5.000000")),
                pqcItem(regulationVersionId, "FINAL", 3, null));
    }

    private static MesQaInspectionRegulationProcessDO qaProcess(Long regulationVersionId) {
        return MesQaInspectionRegulationProcessDO.builder()
                .id(regulationVersionId + 10000L)
                .regulationVersionId(regulationVersionId)
                .processCode("ID-QA-001")
                .processName("清洗")
                .sort(1)
                .build();
    }

    private static MesQaInspectionRegulationProcessDO qaProcess(Long regulationVersionId, Long qaProcessId,
                                                                 String processCode, String processName, int sort) {
        return MesQaInspectionRegulationProcessDO.builder()
                .id(qaProcessId)
                .regulationVersionId(regulationVersionId)
                .processCode(processCode)
                .processName(processName)
                .sort(sort)
                .build();
    }

    private static MesQaInspectionRegulationItemDO pqcItem(String inspectionType, Integer fixedQuantity,
                                                           BigDecimal patrolRatio) {
        return pqcItem(9902L, inspectionType, fixedQuantity, patrolRatio);
    }

    private static MesQaInspectionRegulationItemDO pqcItem(Long regulationVersionId, String inspectionType,
                                                           Integer fixedQuantity, BigDecimal patrolRatio) {
        return pqcItem(regulationVersionId, regulationVersionId + 10000L,
                inspectionType, fixedQuantity, patrolRatio);
    }

    private static MesQaInspectionRegulationItemDO pqcItem(Long regulationVersionId, Long qaProcessId,
                                                           String inspectionType, Integer fixedQuantity,
                                                           BigDecimal patrolRatio) {
        return MesQaInspectionRegulationItemDO.builder()
                .regulationVersionId(regulationVersionId)
                .qaProcessId(qaProcessId)
                .inspectionType(inspectionType)
                .itemCode(inspectionType + "-001")
                .itemName(inspectionType + " 检验项目")
                .inspectionMethod("目视")
                .standardText("符合规程")
                .resultType("BOOLEAN")
                .firstInspectionQuantity(fixedQuantity)
                .patrolInspectionRatio(patrolRatio)
                .build();
    }

    private static void assertPqcTask(MesPqcInspectionTaskDO task, String inspectionType, String inspectionRuleKey,
                                      String shiftCode, Integer plannedInspectionQuantity, LocalDate businessDate) {
        assertEquals(8101L, task.getActiveOrderId());
        assertEquals(9001L, task.getWorkOrderId());
        assertEquals(922119L, task.getRouteId());
        assertEquals(448L, task.getRouteVersionId());
        assertEquals(928601L, task.getRouteProcessId());
        assertEquals(6001L, task.getProcessId());
        assertEquals(19902L, task.getQaProcessId());
        assertEquals(9902L, task.getRegulationVersionId());
        assertEquals(inspectionType, task.getInspectionType());
        assertEquals(inspectionRuleKey, task.getInspectionRuleKey());
        assertEquals(businessDate, task.getBusinessDate());
        assertEquals(shiftCode, task.getShiftCode());
        assertEquals(1, task.getRoundNo());
        assertEquals(plannedInspectionQuantity, task.getPlannedInspectionQuantity());
        assertEquals(0, task.getActualInspectionQuantity());
        assertEquals("PENDING", task.getTaskStatus());
    }

    private static void assertSnapshot(MesProcessPoolActiveOrderProcessSnapshotDO snapshot, Long activeOrderId,
                                       Long workOrderId, Long routeId, Long routeVersionId, Long routeProcessId,
                                       Long processId, String erpQuantity, String factor, String plannedQuantity) {
        assertEquals(activeOrderId, snapshot.getActiveOrderId());
        assertEquals(workOrderId, snapshot.getWorkOrderId());
        assertEquals(routeId, snapshot.getRouteId());
        assertEquals(routeVersionId, snapshot.getRouteVersionId());
        assertEquals(routeProcessId, snapshot.getRouteProcessId());
        assertEquals(processId, snapshot.getProcessId());
        assertEquals(processCode(processId), snapshot.getProcessCodeSnapshot());
        assertEquals(processName(processId), snapshot.getProcessNameSnapshot());
        assertAmount(erpQuantity, snapshot.getErpFixedQuantitySnapshot());
        assertAmount(factor, snapshot.getProductionQuantityFactorSnapshot());
        assertAmount(plannedQuantity, snapshot.getPlannedQuantitySnapshot());
        assertEquals("[]", snapshot.getParameterSnapshotJson());
        assertEquals(MesDeviceParameterSnapshotCodec.sha256("[]"), snapshot.getParameterSnapshotSha256());
        assertEquals(MesDeviceParameterSnapshotCodec.STATE_FROZEN, snapshot.getParameterSnapshotState());
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private static String processCode(Long processId) {
        return "PROC-" + processId;
    }

    private static String processName(Long processId) {
        return "工序" + processId;
    }

    private static MesProcessPoolDeviceParameterRuleDO parameterRule(
            Long id, Long routeProcessId, Long processId, Long deviceId, String code, String upperLimit) {
        return MesProcessPoolDeviceParameterRuleDO.builder()
                .id(id)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .deviceId(deviceId)
                .parameterCode(code)
                .parameterName(code)
                .unit("MPa")
                .lowerLimit(BigDecimal.ZERO)
                .upperLimit(new BigDecimal(upperLimit))
                .valueType("DECIMAL")
                .enabled(Boolean.TRUE)
                .build();
    }
}
