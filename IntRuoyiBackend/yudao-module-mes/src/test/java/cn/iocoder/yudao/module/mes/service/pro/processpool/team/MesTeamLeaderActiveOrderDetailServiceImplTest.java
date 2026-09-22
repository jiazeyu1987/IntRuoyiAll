package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderDetailReadMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrOperationAuditEventMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrOperationAuditEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesTeamLeaderActiveOrderEventPartyReadDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesTeamLeaderActiveOrderDetailReadDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterial;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterialService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderDetailServiceImplTest {

    @Mock
    private cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionBackfillMapper backfillMapper;
    @Mock
    private MesProcessPoolActiveOrderPickListBindingMapper bindingMapper;
    @Mock
    private MesProcessPoolActiveOrderPickListBindingItemMapper bindingItemMapper;

    @Test
    void completedDetailReadsBatchesFromCompletionSnapshot() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L).leaderUserId(3001L).workOrderId(9001L).routeId(9201L).activeStatus("ACTIVE").build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of(
                new MesFrontlineProcessMaterial(3101L, "MAT-A", "物料A", "S1", "INPUT", null,
                        List.of(), null, null, null, List.of(), List.of(), List.of(), null)));
        String seed = """
                {"activeOrderBinding":{"id":8101,"workOrderId":9001},
                 "pickListBindings":[{"id":11,"pickListId":21,"sourceBillNo":"LL-21"},
                                     {"id":12,"pickListId":22,"sourceBillNo":"LL-22"}],
                 "pickListBindingItems":{"11":[{"pickListItemId":31,"materialNumber":"MAT-A","lotNumber":"LOT-A","requestedQuantity":6,"actualQuantity":5,"baseActualQuantity":5}],
                                         "12":[{"pickListItemId":32,"materialNumber":"MAT-A","lotNumber":"LOT-B","requestedQuantity":4,"actualQuantity":4,"baseActualQuantity":4}]}}
                """;
        var backfill = cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionBackfillDO.builder()
                .id(71L).activeOrderId(8101L).workOrderId(9001L).backfillType("BATCH_RECORD").status("SUCCESS")
                .sourceSnapshotHash("completion-hash")
                .payloadJson(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(java.util.Map.of(
                        "type", "BATCH_RECORD", "status", "SUCCESS", "sourceSnapshotHash", "completion-hash",
                        "formalSourceSnapshot", seed))).build();
        lenient().when(backfillMapper.selectByActiveOrderAndType(8101L, "BATCH_RECORD")).thenReturn(backfill);

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);
        var material = detail.getProcesses().get(0).getInputMaterials().get(0);
        assertEquals(List.of("LOT-A", "LOT-B"), material.getBatchCodes());
        assertEquals(List.of("LL-21", "LL-22"), material.getSourcePickListNos());
        assertEquals(List.of(21L, 22L), material.getSourcePickListIds());
        assertEquals(List.of(31L, 32L), material.getSourcePickListItemIds());
        assertEquals(new BigDecimal("9"), material.getActualQuantity());
        assertEquals("completion-hash", material.getSourceSnapshotHash());
        assertEquals(1, detail.getInputMaterialUsages().size());
        assertEquals(List.of("LOT-A", "LOT-B"),
                detail.getInputMaterialUsages().get(0).getBatchCodes());
    }

    @Test
    void activeDetailDoesNotReadPickListBatchesBeforeCompletionBackfill() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L).leaderUserId(3001L).workOrderId(9001L).routeId(9201L).activeStatus("ACTIVE").build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of(
                new MesFrontlineProcessMaterial(3101L, "MAT-A", "物料A", "S1", "INPUT", null,
                        List.of(), null, null, null, List.of(), List.of(), List.of(), null)));
        when(backfillMapper.selectByActiveOrderAndType(8101L, "BATCH_RECORD")).thenReturn(null);
        lenient().when(bindingMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesProcessPoolActiveOrderPickListBindingDO.builder()
                        .id(7001L).activeOrderId(8101L).workOrderId(9001L).pickListId(8001L)
                        .sourceBillNo("REAL-PICK-001").sourceSnapshotHash("binding-hash").build()));
        lenient().when(bindingItemMapper.selectListByBindingId(7001L)).thenReturn(List.of(
                MesProcessPoolActiveOrderPickListBindingItemDO.builder()
                        .id(9101L).bindingId(7001L).pickListItemId(9201L).materialNumber("MAT-A")
                        .lotNumber("LOT-REAL-001").requestedQuantity(new BigDecimal("6"))
                        .actualQuantity(new BigDecimal("5")).baseActualQuantity(new BigDecimal("5")).build()));

        var material = service.getDetail(3001L, 8101L).getProcesses().get(0).getInputMaterials().get(0);

        assertEquals(List.of(), material.getBatchCodes());
        assertEquals(List.of(), material.getSourcePickListNos());
        assertEquals(List.of(), material.getSourcePickListIds());
        assertEquals(List.of(), material.getSourcePickListItemIds());
        assertNull(material.getActualQuantity());
        assertNull(material.getRequestedQuantity());
        assertNull(material.getSourceSnapshotHash());
    }

    @Test
    void activeDetailDoesNotExposeOrderInputMaterialUsagesBeforeCompletionBackfill() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L).leaderUserId(3001L).workOrderId(9001L).routeId(9201L).activeStatus("ACTIVE").build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(backfillMapper.selectByActiveOrderAndType(8101L, "BATCH_RECORD")).thenReturn(null);
        lenient().when(bindingMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesProcessPoolActiveOrderPickListBindingDO.builder()
                        .id(7001L).activeOrderId(8101L).workOrderId(9001L).pickListId(8001L)
                        .sourceBillNo("REAL-PICK-001").sourceSnapshotHash("binding-hash").build()));
        lenient().when(bindingItemMapper.selectListByBindingId(7001L)).thenReturn(List.of(
                MesProcessPoolActiveOrderPickListBindingItemDO.builder()
                        .id(9101L).bindingId(7001L).pickListItemId(9201L).materialNumber("MAT-PML-001")
                        .materialName("用料清单物料").materialSpecification("S1").lotNumber("LOT-LOW")
                        .requestedQuantity(new BigDecimal("6")).actualQuantity(new BigDecimal("5"))
                        .baseActualQuantity(new BigDecimal("5")).build(),
                MesProcessPoolActiveOrderPickListBindingItemDO.builder()
                        .id(9102L).bindingId(7001L).pickListItemId(9202L).materialNumber("MAT-PML-001")
                        .materialName("用料清单物料").materialSpecification("S1").lotNumber("LOT-HIGH")
                        .requestedQuantity(new BigDecimal("8")).actualQuantity(new BigDecimal("7"))
                        .baseActualQuantity(new BigDecimal("7")).build()));

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        assertEquals(0, detail.getProcesses().get(0).getInputMaterials().size());
        assertEquals(0, detail.getInputMaterialUsages().size());
    }

    @Test
    void formalDetailIncludesPqcReleaseSignatureFromTheSameActiveOrderFact() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L).leaderUserId(3001L).workOrderId(9001L).routeId(9201L).activeStatus("CLOSED").build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(backfillMapper.selectByActiveOrderAndType(8101L, "BATCH_RECORD")).thenReturn(null);
        when(releaseApplicationMapper.selectLatestByActiveOrderId(8101L))
                .thenReturn(cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team
                        .MesProcessPoolActiveOrderReleaseApplicationDO.builder()
                        .id(8801L)
                        .activeOrderId(8101L)
                        .batchExecutionId(9901L)
                        .pqcDecision("APPROVE")
                        .applicationStatus("REPORT_UPLOAD_PENDING")
                        .dossierSummaryJson(JSON.toJSONString(java.util.Map.of("signatureId", 7701L)))
                        .build());
        when(signatureQueryService.getById(7701L)).thenReturn(new ElectronicSignatureEvidenceDTO(
                7701L, "MES", "PQC_RELEASE", "BATCH_EXECUTION",
                MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(9901L,
                        MesProBatchRecordExecutionSignatureService.ACTION_PQC_RELEASE,
                        null, null, null, null, null, null, null,
                        "PQC_RELEASE_APPLICATION", 8801L, "PQC生产放行",
                        MesProBatchRecordExecutionSignatureService.ACTION_PQC_RELEASE,
                        null, null, null, null),
                null, 3001L,
                "PQC_RELEASE", "PQC生产放行", null, LocalDateTime.of(2026, 9, 18, 21, 22, 52),
                "time-1", "SESSION_PLUS_PASSWORD", "content", "evidence", "SHA-256",
                "v1", "p1", "VALID", null, null, null, null, null, null, null, null));
        when(signatureQueryService.verifyEvidence(7701L)).thenReturn(new ElectronicSignatureVerificationDTO(
                7701L, "VALID", "content", "content", "evidence", "evidence", "SHA-256", "v1"));
        when(adminUserService.getUser(3001L)).thenReturn(new AdminUserDO().setId(3001L).setNickname("PQC组长"));

        MesTeamLeaderActiveOrderDetail detail = service.getFormalDetail(8101L);

        assertEquals("已生产放行", detail.getPqcProductionRelease().getStatusLabel());
        assertEquals(7701L, detail.getPqcProductionRelease().getSignature().getSignatureId());
        assertEquals("PQC组长", detail.getPqcProductionRelease().getSignature().getSignerName());
    }

    @Test
    void archivedFormalDetailReadsClosedOrArchivedSourceOrder() {
        when(activeOrderMapper.selectByIdIgnoreDeleted(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L).leaderUserId(3001L).workOrderId(9001L).routeId(9201L).activeStatus("CLOSED").build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100", null, null, null, null, null)));
        when(processMaterialService.listArchivedFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(backfillMapper.selectByActiveOrderAndType(8101L, "BATCH_RECORD")).thenReturn(null);

        MesTeamLeaderActiveOrderDetail detail = service.getArchivedFormalDetail(8101L);

        assertEquals(8101L, detail.getActiveOrderId());
        assertEquals("881MO090889", detail.getWorkOrderCode());
    }

    @Test
    void activeOrderOperationsShouldAppearInFormalFactChain() throws Exception {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L).leaderUserId(3001L).workOrderId(9001L).routeId(9201L).activeStatus("CLOSED").build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(backfillMapper.selectByActiveOrderAndType(8101L, "BATCH_RECORD")).thenReturn(null);
        when(operationAuditEventMapper.selectSuccessfulListByActiveOrderId(8101L)).thenReturn(List.of(
                MesProEdhrOperationAuditEventDO.builder()
                        .id(8801L)
                        .objectType("ACTIVE_ORDER")
                        .objectId("8101")
                        .operationType("BATCH_REWORK")
                        .actionName("批次返工")
                        .actorUserId(3001L)
                        .actorUsername("生产组长甲")
                        .resultStatus("SUCCESS")
                        .afterSummaryHash("snapshot-1")
                        .metadataJson("{\"activeOrderId\":8101,\"signatureId\":7701}")
                        .occurredAt(LocalDateTime.of(2026, 9, 18, 22, 10))
                        .build()));

        MesTeamLeaderActiveOrderDetail detail = service.getFormalDetail(8101L);
        var getter = MesTeamLeaderActiveOrderDetail.class.getMethod("getOperationFacts");
        List<?> facts = (List<?>) getter.invoke(detail);

        assertEquals(1, facts.size());
        Object fact = facts.get(0);
        assertEquals("BATCH_REWORK", fact.getClass().getMethod("getOperationType").invoke(fact));
        assertEquals("ACTIVE_ORDER", fact.getClass().getMethod("getSourceType").invoke(fact));
        assertEquals("8101", fact.getClass().getMethod("getSourceId").invoke(fact));
        assertEquals(7701L, fact.getClass().getMethod("getSignatureId").invoke(fact));
    }

    @Test
    void activeOrderMaintenanceOperationsShouldAppearInFormalFactChain() throws Exception {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L).leaderUserId(3001L).workOrderId(9001L).routeId(9201L).activeStatus("CLOSED").build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(backfillMapper.selectByActiveOrderAndType(8101L, "BATCH_RECORD")).thenReturn(null);
        when(adminUserService.getUser(3001L)).thenReturn(new AdminUserDO().setId(3001L).setNickname("生产组长甲"));
        when(maintenanceAuditMapper.selectSuccessfulListByActiveOrderId(8101L)).thenReturn(List.of(
                MesProcessPoolTeamMaintenanceAuditDO.builder()
                        .id(9901L)
                        .operatorUserId(3001L)
                        .actionType("APPLY_ACTIVE_ORDER_VERSION_UPGRADE")
                        .targetType("ACTIVE_ORDER")
                        .targetId(8101L)
                        .resultStatus("SUCCESS")
                        .changeSummary("活跃订单版本升级审批通过后重启")
                        .auditTime(LocalDateTime.of(2026, 9, 18, 22, 20))
                        .build()));

        MesTeamLeaderActiveOrderDetail detail = service.getFormalDetail(8101L);
        var getter = MesTeamLeaderActiveOrderDetail.class.getMethod("getOperationFacts");
        List<?> facts = (List<?>) getter.invoke(detail);

        assertEquals(1, facts.size());
        Object fact = facts.get(0);
        assertEquals("APPLY_ACTIVE_ORDER_VERSION_UPGRADE",
                fact.getClass().getMethod("getOperationType").invoke(fact));
        assertEquals("ACTIVE_ORDER", fact.getClass().getMethod("getSourceType").invoke(fact));
        assertEquals("8101", fact.getClass().getMethod("getSourceId").invoke(fact));
        assertEquals(3001L, fact.getClass().getMethod("getActorUserId").invoke(fact));
    }

    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;
    @Mock
    private MesProEdhrOperationAuditEventMapper operationAuditEventMapper;
    @Mock
    private MesProcessPoolTeamMaintenanceAuditMapper maintenanceAuditMapper;
    @Mock
    private MesProcessPoolActiveOrderDetailReadMapper detailReadMapper;
    @Mock
    private MesFrontlineProcessMaterialService processMaterialService;
    @Mock
    private MesPqcInspectionTaskMapper pqcTaskMapper;
    @Mock
    private MesPqcProcessInspectionAggregateDetailMapper pqcAggregateDetailMapper;
    @Mock
    private MesQaInspectionRegulationProcessMapper qaProcessMapper;
    @Mock
    private ErpKingdeeProductionReplenishmentListItemMapper replenishmentListItemMapper;
    @Mock
    private ErpKingdeeProductionReplenishmentListMapper replenishmentListMapper;
    @Mock
    private MesMdItemMapper itemMapper;
    @Mock
    private MesProProcessPoolEventMapper eventMapper;
    @Mock
    private MesProProcessPoolEventRevisionMapper eventRevisionMapper;
    @Mock
    private ElectronicSignatureQueryService signatureQueryService;
    @Mock
    private AdminUserService adminUserService;
    @InjectMocks
    private MesTeamLeaderActiveOrderDetailServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(replenishmentListItemMapper.selectListByProductionOrderNo("881MO090889"))
                .thenReturn(List.of());
        lenient().when(eventMapper.selectBatchIds(anyList())).thenAnswer(invocation -> {
            List<Long> eventIds = invocation.getArgument(0);
            return eventIds.stream()
                    .map(MesTeamLeaderActiveOrderDetailServiceImplTest::defaultPqcEvent)
                    .toList();
        });
        lenient().when(eventRevisionMapper.selectListByEventId(anyLong())).thenReturn(List.of());
    }

    @Test
    void shouldGroupMultipleEmployeesAndSubmissionsByFormalProcessSnapshot() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(9201L)
                .qaRegulationVersionId(5201L)
                .activeStatus("ACTIVE")
                .build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100.000000", 7001L, "30", "张三", "生产组长甲",
                        "2026-08-13T08:10:00"),
                row(9101L, 5001L, 6001L, "粗洗", "100.000000", 7002L, "40", "李四", null,
                        "2026-08-13T09:20:00"),
                row(9102L, 5002L, 6002L, "精洗", "100.000000", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5002L, 6002L)).thenReturn(List.of());
        when(pqcTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of());
        when(pqcAggregateDetailMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of());

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        assertEquals(8101L, detail.getActiveOrderId());
        assertEquals("881MO090889", detail.getWorkOrderCode());
        assertEquals("球囊扩张压力泵工艺路线", detail.getRouteName());
        assertEquals(2, detail.getProcesses().size());
        MesTeamLeaderActiveOrderDetail.ProcessDetail roughWash = detail.getProcesses().get(0);
        assertEquals(new BigDecimal("100.000000"), roughWash.getRequiredQuantity());
        assertEquals(Boolean.TRUE, roughWash.getKeyFlag());
        assertEquals(new BigDecimal("70"), roughWash.getSubmittedQuantity());
        assertEquals(2, roughWash.getSubmissionCount());
        assertEquals("张三", roughWash.getSubmissions().get(0).getSubmitterName());
        assertEquals("生产组长甲", roughWash.getSubmissions().get(0).getReviewerName());
        assertEquals("李四", roughWash.getSubmissions().get(1).getSubmitterName());
        assertNull(roughWash.getSubmissions().get(1).getReviewerName());
        assertEquals(0, detail.getProcesses().get(1).getSubmissionCount());
        assertEquals(List.of(), detail.getProcesses().get(1).getSubmissions());
    }

    @Test
    void shouldExposeInputMaterialBatchesAndPqcSubmissionDetailsByProcess() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(9201L)
                .routeVersionId(9301L)
                .qaRegulationVersionId(5201L)
                .activeStatus("ACTIVE")
                .build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100.000000", 7001L, "30", "张三", "生产组长甲",
                        "2026-08-13T08:10:00")));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of(
                new MesFrontlineProcessMaterial(3101L, "MAT-A", "物料A", "S1",
                        MesFrontlineProcessMaterial.ROLE_INPUT, null, List.of("LOT-01", "LOT-02"),
                        new BigDecimal("10"), new BigDecimal("9"), new BigDecimal("9"),
                        List.of(2101L, 2102L), List.of("SIM-SOUT-001", "SIM-SOUT-002"),
                        List.of(2201L, 2202L), "hash-pick-list"),
                new MesFrontlineProcessMaterial(3102L, "OUT-A", "产出A", "S2",
                        MesFrontlineProcessMaterial.ROLE_OUTPUT, null, List.of(), null,
                        null, null, List.of(), List.of(), List.of(), null)));
        when(pqcTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcInspectionTaskDO.builder()
                        .id(4101L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .inspectionType("FIRST")
                        .qaItemCode("WIDTH")
                        .inspectionRuleKey("FIRST")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED)
                        .actualInspectionQuantity(2)
                        .qaProcessId(5101L)
                        .regulationVersionId(5201L)
                        .submittedEventId(7101L)
                        .build()));
        when(qaProcessMapper.selectBatchIds(List.of(5101L))).thenReturn(List.of(
                MesQaInspectionRegulationProcessDO.builder()
                        .id(5101L)
                        .regulationVersionId(5201L)
                        .processCode("QA-粗洗")
                        .processName("粗洗检验")
                        .sort(1)
                        .build()));
        when(detailReadMapper.selectEventPartiesByEventIds(List.of(7101L))).thenReturn(List.of(
                new MesTeamLeaderActiveOrderEventPartyReadDO()
                        .setEventId(7101L)
                        .setProductionEventId(7001L)
                        .setSubmitterName("PQC王五")
                        .setReviewerName("PQC主管甲")));
        when(pqcAggregateDetailMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcProcessInspectionAggregateDetailDO.builder()
                        .id(4201L)
                        .pqcTaskId(4101L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .sampleNo(1)
                        .itemCode("WIDTH")
                        .itemName("宽度")
                        .measuredValue("12.3")
                        .itemResult("12.3")
                        .judgement("PASS")
                        .selectedEquipmentNumber("EQ-001")
                        .standardText("10-15")
                        .build()));

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        MesTeamLeaderActiveOrderDetail.ProcessDetail process = detail.getProcesses().get(0);
        assertEquals(1, process.getInputMaterials().size());
        assertEquals("MAT-A", process.getInputMaterials().get(0).getMaterialCode());
        assertEquals(List.of("LOT-01", "LOT-02"), process.getInputMaterials().get(0).getBatchCodes());
        assertEquals(List.of(2101L, 2102L), process.getInputMaterials().get(0).getSourcePickListIds());
        assertEquals(List.of("SIM-SOUT-001", "SIM-SOUT-002"),
                process.getInputMaterials().get(0).getSourcePickListNos());
        assertEquals(1, process.getPqcSubmissions().size());
        MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail pqcSubmission = process.getPqcSubmissions().get(0);
        assertEquals(4101L, pqcSubmission.getPqcTaskId());
        assertEquals(7101L, pqcSubmission.getSubmittedEventId());
        assertEquals(7001L, pqcSubmission.getProductionEventId());
        assertEquals(List.of(7001L), pqcSubmission.getProductionEventIds());
        assertEquals("PQC王五", pqcSubmission.getSubmitterName());
        assertEquals("PQC主管甲", pqcSubmission.getReviewerName());
        assertEquals(1, pqcSubmission.getProcessInspectionItems().size());
        assertEquals("WIDTH", pqcSubmission.getProcessInspectionItems().get(0).getItemCode());
        assertEquals("EQ-001", pqcSubmission.getProcessInspectionItems().get(0).getSelectedEquipmentNumber());
    }

    @Test
    void shouldSeparateOriginalSubmittedItemsFromCurrentProcessInspectionItems() throws Exception {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(9201L)
                .routeVersionId(9301L)
                .qaRegulationVersionId(5201L)
                .activeStatus("ACTIVE")
                .build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100.000000", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(pqcTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcInspectionTaskDO.builder()
                        .id(4101L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .inspectionType("FIRST")
                        .qaItemCode("WIDTH")
                        .inspectionRuleKey("FIRST")
                        .businessDate(LocalDate.of(2026, 8, 13))
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED)
                        .actualInspectionQuantity(2)
                        .qaProcessId(5101L)
                        .regulationVersionId(5201L)
                        .submittedEventId(7101L)
                        .build()));
        when(qaProcessMapper.selectBatchIds(List.of(5101L))).thenReturn(List.of(
                MesQaInspectionRegulationProcessDO.builder()
                        .id(5101L)
                        .regulationVersionId(5201L)
                        .processCode("QA-粗洗")
                        .processName("粗洗检验")
                        .sort(1)
                        .build()));
        when(detailReadMapper.selectEventPartiesByEventIds(List.of(7101L))).thenReturn(List.of(
                new MesTeamLeaderActiveOrderEventPartyReadDO()
                        .setEventId(7101L)
                        .setProductionEventId(7001L)
                        .setSubmitterName("PQC王五")
                        .setReviewerName("PQC主管甲")));
        when(eventMapper.selectBatchIds(List.of(7101L))).thenReturn(List.of(
                MesProProcessPoolEventDO.builder()
                        .id(7101L)
                        .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                        .rawPayload("""
                                {"pqcItemDetails":[{"itemCode":"WIDTH","itemName":"宽度","inspectionMethod":"卡尺","standardText":"10-15","sampleValues":["15.5"],"judgement":"FAIL","selectedEquipmentName":"游标卡尺","selectedEquipmentNumber":"EQ-NEW"}]}
                                """)
                        .build()));
        when(eventRevisionMapper.selectListByEventId(7101L)).thenReturn(List.of(
                MesProProcessPoolEventRevisionDO.builder()
                        .id(9001L)
                        .eventId(7101L)
                        .beforePayload("""
                                {"actualInspectionQuantity":13,"scrapQuantity":1,"pqcItemDetails":[{"itemCode":"WIDTH","itemName":"宽度","inspectionMethod":"卡尺","standardText":"10-15","sampleValues":["12.3"],"judgement":"PASS","selectedEquipmentName":"游标卡尺","selectedEquipmentNumber":"EQ-OLD"}]}
                                """)
                        .serverRevisionTime(LocalDateTime.parse("2026-08-13T10:00:00"))
                        .revisionStatus(MesProProcessPoolEventRevisionDO.STATUS_EFFECTIVE)
                        .build()));
        when(pqcAggregateDetailMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcProcessInspectionAggregateDetailDO.builder()
                        .id(4201L)
                        .pqcTaskId(4101L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .sampleNo(1)
                        .itemCode("WIDTH")
                        .itemName("宽度")
                        .measuredValue("99.9")
                        .itemResult("99.9")
                        .judgement("FAIL")
                        .selectedEquipmentName("当前设备")
                        .selectedEquipmentNumber("EQ-CURRENT")
                        .standardText("10-15")
                        .build()));

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail submission =
                detail.getProcesses().get(0).getPqcSubmissions().get(0);
        assertEquals("12.3", submission.getSubmittedItems().get(0).getMeasuredValue());
        assertEquals("EQ-OLD", submission.getSubmittedItems().get(0).getSelectedEquipmentNumber());
        assertEquals("99.9", submission.getProcessInspectionItems().get(0).getMeasuredValue());
        assertEquals("EQ-CURRENT", submission.getProcessInspectionItems().get(0).getSelectedEquipmentNumber());
        assertEquals(13, submission.getClass().getMethod("getSubmittedInspectionQuantity").invoke(submission));
        assertEquals(1, submission.getClass().getMethod("getSubmittedScrapQuantity").invoke(submission));
        assertEquals(2, submission.getActualInspectionQuantity());
    }

    @Test
    void shouldJudgeEachDistinctOriginalNumericSampleUsingSnapshotLimits() {
        var items = parseSubmittedItems("""
                {"itemCode":"WIDTH","resultType":"NUMERIC","standardLowerLimit":10,
                 "standardUpperLimit":15,"standardPrecision":1,"sampleValues":["12.3","15.5","12.3"],
                 "judgement":"SUCCESS"}
                """);
        assertEquals(List.of("SUCCESS", "FAILURE", "SUCCESS"),
                items.stream().map(MesTeamLeaderActiveOrderDetail.PqcSubmissionItemDetail::getJudgement).toList());
    }

    @Test
    void shouldJudgeEachDistinctOriginalBooleanSample() {
        var items = parseSubmittedItems("""
                {"itemCode":"APPEARANCE","resultType":"BOOLEAN",
                 "sampleValues":["合格","不合格"],"judgement":"SUCCESS"}
                """);
        assertEquals(List.of("SUCCESS", "FAILURE"),
                items.stream().map(MesTeamLeaderActiveOrderDetail.PqcSubmissionItemDetail::getJudgement).toList());
    }

    @Test
    void shouldPreserveSnapshotJudgementForIdenticalValues() {
        var items = parseSubmittedItems("""
                {"itemCode":"APPEARANCE","sampleValues":["OK","OK"],"judgement":"PASS"}
                """);
        assertEquals(List.of("PASS", "PASS"),
                items.stream().map(MesTeamLeaderActiveOrderDetail.PqcSubmissionItemDetail::getJudgement).toList());
    }

    @Test
    void shouldRejectDistinctNumericSamplesWithoutSnapshotLimits() {
        assertThrows(ServiceException.class, () -> parseSubmittedItems("""
                {"itemCode":"WIDTH","resultType":"NUMERIC",
                 "sampleValues":["12.3","15.5"],"judgement":"SUCCESS"}
                """));
    }

    @Test
    void shouldSerializePqcBusinessDateAsIsoStringEvenWithTimestampDatesEnabled() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .enable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var response = new cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo
                .MesTeamLeaderActiveOrderDetailRespVO.PqcSubmissionDetail()
                .setBusinessDate(LocalDate.of(2026, 9, 17));
        assertEquals("2026-09-17", mapper.readTree(mapper.writeValueAsString(response))
                .get("businessDate").asText());
    }

    private static List<MesTeamLeaderActiveOrderDetail.PqcSubmissionItemDetail> parseSubmittedItems(String json) {
        java.util.Map<?, ?> item = cn.iocoder.yudao.framework.common.util.json.JsonUtils
                .parseObject(json, java.util.Map.class);
        return org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                MesTeamLeaderActiveOrderDetailServiceImpl.class, "resolvePqcSubmittedItemDetails", List.of(item), 8101L);
    }

    @Test
    void shouldCollapseStage1PqcSubmissionCopiesAcrossProductionProcesses() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(9201L)
                .routeVersionId(9301L)
                .qaRegulationVersionId(5201L)
                .activeStatus("ACTIVE")
                .simulated(Boolean.TRUE)
                .simulationStage("STAGE1")
                .build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100.000000", null, null, null, null, null),
                row(9102L, 5002L, 6002L, "精洗", "100.000000", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5002L, 6002L)).thenReturn(List.of());
        when(pqcTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcInspectionTaskDO.builder()
                        .id(4101L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .inspectionType("FIRST")
                        .qaItemCode("WIDTH")
                        .inspectionRuleKey("FIRST")
                        .businessDate(LocalDate.of(2026, 8, 13))
                        .shiftCode("FIRST")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED)
                        .actualInspectionQuantity(2)
                        .qaProcessId(5101L)
                        .regulationVersionId(5201L)
                        .submittedEventId(7101L)
                        .build(),
                MesPqcInspectionTaskDO.builder()
                        .id(4102L)
                        .activeOrderId(8101L)
                        .routeProcessId(5002L)
                        .processId(6002L)
                        .inspectionType("FIRST")
                        .qaItemCode("WIDTH")
                        .inspectionRuleKey("FIRST")
                        .businessDate(LocalDate.of(2026, 8, 13))
                        .shiftCode("FIRST")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED)
                        .actualInspectionQuantity(2)
                        .qaProcessId(5101L)
                        .regulationVersionId(5201L)
                        .submittedEventId(7102L)
                        .build()));
        when(qaProcessMapper.selectBatchIds(List.of(5101L))).thenReturn(List.of(
                MesQaInspectionRegulationProcessDO.builder()
                        .id(5101L)
                        .regulationVersionId(5201L)
                        .processCode("PQC-通用")
                        .processName("过程检验")
                        .sort(1)
                        .build()));
        when(detailReadMapper.selectEventPartiesByEventIds(List.of(7101L, 7102L))).thenReturn(List.of(
                new MesTeamLeaderActiveOrderEventPartyReadDO()
                        .setEventId(7101L)
                        .setProductionEventId(7001L)
                        .setSubmitterName("PQC管理员")
                        .setReviewerName("PQC管理员"),
                new MesTeamLeaderActiveOrderEventPartyReadDO()
                        .setEventId(7102L)
                        .setProductionEventId(7002L)
                        .setSubmitterName("PQC管理员")
                        .setReviewerName("PQC管理员")));
        when(pqcAggregateDetailMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcProcessInspectionAggregateDetailDO.builder()
                        .id(4201L)
                        .pqcTaskId(4101L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .sampleNo(1)
                        .itemCode("WIDTH")
                        .itemName("宽度")
                        .measuredValue("12.3")
                        .itemResult("12.3")
                        .judgement("PASS")
                        .build(),
                MesPqcProcessInspectionAggregateDetailDO.builder()
                        .id(4202L)
                        .pqcTaskId(4102L)
                        .activeOrderId(8101L)
                        .routeProcessId(5002L)
                        .processId(6002L)
                        .sampleNo(1)
                        .itemCode("WIDTH")
                        .itemName("宽度")
                        .measuredValue("12.3")
                        .itemResult("12.3")
                        .judgement("PASS")
                        .build()));

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        int pqcSubmissionCount = detail.getProcesses().stream()
                .mapToInt(process -> process.getPqcSubmissions().size())
                .sum();
        assertEquals(1, pqcSubmissionCount);
        MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail submission =
                detail.getProcesses().get(0).getPqcSubmissions().get(0);
        assertEquals(4101L, submission.getPqcTaskId());
        assertEquals("FIRST", submission.getInspectionRuleKey());
        assertEquals("WIDTH", submission.getQaItemCode());
        assertEquals(List.of(7101L), submission.getSubmittedEventIds());
        assertEquals(1, submission.getProcessInspectionItems().size());
    }

    @Test
    void shouldExposeMaterialDeviceMeteringValidityFromOriginalPayload() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(9201L)
                .qaRegulationVersionId(5201L)
                .activeStatus("ACTIVE")
                .build());
        MesTeamLeaderActiveOrderDetailReadDO productionRow = row(9101L, 5001L, 6001L, "粗洗",
                "10.000000", 7001L, "10", "张三", "生产组长甲", "2026-08-13T08:10:00");
        productionRow.setOriginalPayloadJson("""
                {
                  "materialDetails": [
                    {
                      "materialId": 3102,
                      "materialCode": "OUT-A",
                      "materialName": "产出A",
                      "outputQuantity": 10,
                      "lossQuantity": 0,
                      "clearanceConfirmations": [
                        { "key": "workplace", "label": "清场", "confirmed": true },
                        { "key": "material", "label": "物料", "confirmed": true },
                        { "key": "cleaning", "label": "清洁", "confirmed": true }
                      ],
                      "selectedDevices": [
                        {
                          "deviceId": 980009,
                          "deviceCode": "B09393",
                          "deviceName": "超声波清洗机",
                          "inMeteringValidityPeriod": false
                        }
                      ],
                      "deviceParameterReadings": [
                        {
                          "deviceId": 980009,
                          "deviceCode": "B09393",
                          "deviceName": "超声波清洗机",
                          "parameterCode": "TEMP",
                          "parameterName": "清洗温度",
                          "unit": "℃",
                          "value": 45,
                          "textValue": "45",
                          "lowerLimit": 20,
                          "upperLimit": 30,
                          "parameterStatus": "ABOVE_UPPER"
                        }
                      ]
                    }
                  ]
                }
                """);
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(productionRow));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(pqcTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of());
        when(pqcAggregateDetailMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of());

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        MesTeamLeaderActiveOrderDetail.SubmissionMaterialDetail material =
                detail.getProcesses().get(0).getSubmissions().get(0).getMaterials().get(0);
        assertEquals("OUT-A", material.getMaterialCode());
        assertEquals(1, material.getDevices().size());
        assertEquals("B09393", material.getDevices().get(0).getDeviceCode());
        assertEquals(Boolean.FALSE, material.getDevices().get(0).getInMeteringValidityPeriod());
        assertEquals(3, material.getClearanceConfirmations().size());
        assertEquals("workplace", material.getClearanceConfirmations().get(0).getKey());
        assertEquals(Boolean.TRUE, material.getClearanceConfirmations().get(0).getConfirmed());
        assertEquals(1, material.getDeviceParameters().size());
        assertEquals("TEMP", material.getDeviceParameters().get(0).getParameterCode());
        assertEquals(new BigDecimal("20"), material.getDeviceParameters().get(0).getLowerLimit());
        assertEquals(new BigDecimal("30"), material.getDeviceParameters().get(0).getUpperLimit());
        assertEquals("ABOVE_UPPER", material.getDeviceParameters().get(0).getParameterStatus());
    }

    @Test
    void shouldKeepSameProcessFinalInspectionTasksSeparatedByQaItemCode() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(9201L)
                .qaRegulationVersionId(5201L)
                .activeStatus("ACTIVE")
                .build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100.000000", 7001L, "100", "张三", "生产组长甲",
                        "2026-08-13T08:10:00")));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(pqcTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcInspectionTaskDO.builder()
                        .id(4101L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .inspectionType("FINAL")
                        .qaItemCode("APPEARANCE")
                        .inspectionRuleKey("FINAL")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED)
                        .actualInspectionQuantity(3)
                        .qaProcessId(5101L)
                        .regulationVersionId(5201L)
                        .submittedEventId(8869L)
                        .build(),
                MesPqcInspectionTaskDO.builder()
                        .id(4102L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .inspectionType("FINAL")
                        .qaItemCode("CLEAN")
                        .inspectionRuleKey("FINAL")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED)
                        .actualInspectionQuantity(3)
                        .qaProcessId(5101L)
                        .regulationVersionId(5201L)
                        .submittedEventId(8872L)
                        .build()));
        when(qaProcessMapper.selectBatchIds(List.of(5101L))).thenReturn(List.of(
                MesQaInspectionRegulationProcessDO.builder()
                        .id(5101L)
                        .regulationVersionId(5201L)
                        .processCode("QA-粗洗")
                        .processName("粗洗检验")
                        .sort(1)
                        .build()));
        when(detailReadMapper.selectEventPartiesByEventIds(List.of(8869L, 8872L))).thenReturn(List.of(
                new MesTeamLeaderActiveOrderEventPartyReadDO()
                        .setEventId(8869L)
                        .setProductionEventId(7001L)
                        .setSubmitterName("PQC王五")
                        .setReviewerName("PQC主管甲"),
                new MesTeamLeaderActiveOrderEventPartyReadDO()
                        .setEventId(8872L)
                        .setProductionEventId(7002L)
                        .setSubmitterName("PQC赵六")
                        .setReviewerName("PQC主管甲")));
        when(pqcAggregateDetailMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcProcessInspectionAggregateDetailDO.builder()
                        .id(4201L)
                        .pqcTaskId(4101L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .sampleNo(1)
                        .itemCode("APPEARANCE")
                        .itemName("外观")
                        .judgement("PASS")
                        .build(),
                MesPqcProcessInspectionAggregateDetailDO.builder()
                        .id(4202L)
                        .pqcTaskId(4102L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .sampleNo(1)
                        .itemCode("CLEAN")
                        .itemName("清洁度")
                        .judgement("PASS")
                        .build()));

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        MesTeamLeaderActiveOrderDetail.ProcessDetail process = detail.getProcesses().get(0);
        assertEquals(2, process.getPqcSubmissions().size());
        MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail appearance = process.getPqcSubmissions().get(0);
        MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail clean = process.getPqcSubmissions().get(1);
        assertEquals("FINAL", appearance.getInspectionType());
        assertEquals("FINAL", appearance.getInspectionRuleKey());
        assertEquals("APPEARANCE", appearance.getQaItemCode());
        assertEquals(List.of(8869L), appearance.getSubmittedEventIds());
        assertEquals(List.of(7001L), appearance.getProductionEventIds());
        assertEquals("PQC王五", appearance.getSubmitterName());
        assertEquals(1, appearance.getProcessInspectionItems().size());
        assertEquals("APPEARANCE", appearance.getProcessInspectionItems().get(0).getItemCode());
        assertEquals("FINAL", clean.getInspectionType());
        assertEquals("FINAL", clean.getInspectionRuleKey());
        assertEquals("CLEAN", clean.getQaItemCode());
        assertEquals(List.of(8872L), clean.getSubmittedEventIds());
        assertEquals(List.of(7002L), clean.getProductionEventIds());
        assertEquals("PQC赵六", clean.getSubmitterName());
        assertEquals(1, clean.getProcessInspectionItems().size());
        assertEquals("CLEAN", clean.getProcessInspectionItems().get(0).getItemCode());
    }

    @Test
    void shouldKeepPatrolAmAndPmAsSeparatePqcSubmissionBlocks() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(9201L)
                .qaRegulationVersionId(5201L)
                .activeStatus("ACTIVE")
                .build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "清洗", "100.000000", 7001L, "100", "张三", "生产组长甲",
                        "2026-08-13T08:10:00")));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(pqcTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcInspectionTaskDO.builder()
                        .id(4101L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .inspectionType("PATROL")
                        .qaItemCode("APPEARANCE")
                        .inspectionRuleKey("PATROL_AM")
                        .shiftCode("AM")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED)
                        .actualInspectionQuantity(1)
                        .qaProcessId(5101L)
                        .regulationVersionId(5201L)
                        .submittedEventId(10287L)
                        .build(),
                MesPqcInspectionTaskDO.builder()
                        .id(4102L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .inspectionType("PATROL")
                        .qaItemCode("APPEARANCE")
                        .inspectionRuleKey("PATROL_PM")
                        .shiftCode("PM")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED)
                        .actualInspectionQuantity(1)
                        .qaProcessId(5101L)
                        .regulationVersionId(5201L)
                        .submittedEventId(10288L)
                        .build()));
        when(qaProcessMapper.selectBatchIds(List.of(5101L))).thenReturn(List.of(
                MesQaInspectionRegulationProcessDO.builder()
                        .id(5101L)
                        .regulationVersionId(5201L)
                        .processCode("PQC-清洗")
                        .processName("清洗")
                        .sort(1)
                        .build()));
        when(detailReadMapper.selectEventPartiesByEventIds(List.of(10287L, 10288L))).thenReturn(List.of(
                new MesTeamLeaderActiveOrderEventPartyReadDO()
                        .setEventId(10287L)
                        .setProductionEventId(7001L)
                        .setSubmitterName("PQC管理员")
                        .setReviewerName("PQC管理员"),
                new MesTeamLeaderActiveOrderEventPartyReadDO()
                        .setEventId(10288L)
                        .setProductionEventId(7002L)
                        .setSubmitterName("PQC管理员")
                        .setReviewerName("PQC管理员")));
        when(pqcAggregateDetailMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcProcessInspectionAggregateDetailDO.builder()
                        .id(4201L)
                        .pqcTaskId(4101L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .sampleNo(1)
                        .itemCode("APPEARANCE")
                        .itemName("外观")
                        .judgement("PASS")
                        .build(),
                MesPqcProcessInspectionAggregateDetailDO.builder()
                        .id(4202L)
                        .pqcTaskId(4102L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .sampleNo(1)
                        .itemCode("APPEARANCE")
                        .itemName("外观")
                        .judgement("PASS")
                        .build()));

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        MesTeamLeaderActiveOrderDetail.ProcessDetail process = detail.getProcesses().get(0);
        assertEquals(2, process.getPqcSubmissions().size());
        MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail am = process.getPqcSubmissions().get(0);
        MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail pm = process.getPqcSubmissions().get(1);
        assertEquals("PATROL_AM", am.getInspectionRuleKey());
        assertEquals(List.of(10287L), am.getSubmittedEventIds());
        assertEquals(List.of(7001L), am.getProductionEventIds());
        assertEquals("PATROL_PM", pm.getInspectionRuleKey());
        assertEquals(List.of(10288L), pm.getSubmittedEventIds());
        assertEquals(List.of(7002L), pm.getProductionEventIds());
    }

    @Test
    void shouldKeepSamePqcScrapItemRowsSeparatedBySubmittedAndProductionEvent() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(9201L)
                .qaRegulationVersionId(5201L)
                .activeStatus("ACTIVE")
                .build());
        MesTeamLeaderActiveOrderDetailReadDO firstProduction = row(9101L, 5001L, 6001L, "清洗",
                "100.000000", 7001L, "40", "甲", "生产组长甲", "2026-08-13T08:10:00")
                .setSubmitterSignatureId(17001L)
                .setSubmitterSignedAt(LocalDateTime.parse("2026-08-13T08:10:00"));
        MesTeamLeaderActiveOrderDetailReadDO secondProduction = row(9101L, 5001L, 6001L, "清洗",
                "100.000000", 7002L, "60", "乙", "生产组长甲", "2026-08-13T15:20:00")
                .setSubmitterSignatureId(17002L)
                .setSubmitterSignedAt(LocalDateTime.parse("2026-08-13T15:20:00"));
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(firstProduction, secondProduction));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(pqcTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcInspectionTaskDO.builder()
                        .id(4101L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .inspectionType("FINAL")
                        .qaItemCode("APPEARANCE")
                        .inspectionRuleKey("FINAL")
                        .businessDate(LocalDate.of(2026, 8, 13))
                        .shiftCode("AM")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED)
                        .actualInspectionQuantity(1)
                        .qaProcessId(5101L)
                        .regulationVersionId(5201L)
                        .submittedEventId(7101L)
                        .build(),
                MesPqcInspectionTaskDO.builder()
                        .id(4102L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .inspectionType("FINAL")
                        .qaItemCode("APPEARANCE")
                        .inspectionRuleKey("FINAL")
                        .businessDate(LocalDate.of(2026, 8, 13))
                        .shiftCode("PM")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED)
                        .actualInspectionQuantity(1)
                        .qaProcessId(5101L)
                        .regulationVersionId(5201L)
                        .submittedEventId(7102L)
                        .build()));
        when(qaProcessMapper.selectBatchIds(List.of(5101L))).thenReturn(List.of(
                MesQaInspectionRegulationProcessDO.builder()
                        .id(5101L)
                        .regulationVersionId(5201L)
                        .processCode("PQC-清洗")
                        .processName("清洗")
                        .sort(1)
                        .build()));
        when(detailReadMapper.selectEventPartiesByEventIds(List.of(7101L, 7102L))).thenReturn(List.of(
                new MesTeamLeaderActiveOrderEventPartyReadDO()
                        .setEventId(7101L)
                        .setProductionEventId(7001L)
                        .setSubmitterName("PQC王五")
                        .setReviewerName("PQC主管甲")
                        .setScrapQuantity(2),
                new MesTeamLeaderActiveOrderEventPartyReadDO()
                        .setEventId(7102L)
                        .setProductionEventId(7002L)
                        .setSubmitterName("PQC赵六")
                        .setReviewerName("PQC主管甲")
                        .setScrapQuantity(3)));
        when(pqcAggregateDetailMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcProcessInspectionAggregateDetailDO.builder()
                        .id(4201L)
                        .pqcTaskId(4101L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .sampleNo(1)
                        .itemCode("APPEARANCE")
                        .itemName("外观")
                        .judgement("FAIL")
                        .build(),
                MesPqcProcessInspectionAggregateDetailDO.builder()
                        .id(4202L)
                        .pqcTaskId(4102L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .sampleNo(1)
                        .itemCode("APPEARANCE")
                        .itemName("外观")
                        .judgement("FAIL")
                        .build()));

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        MesTeamLeaderActiveOrderDetail.ProcessDetail process = detail.getProcesses().get(0);
        assertEquals(2, process.getPqcSubmissions().size());
        MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail firstScrap = process.getPqcSubmissions().get(0);
        MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail secondScrap = process.getPqcSubmissions().get(1);
        assertEquals(List.of(7101L), firstScrap.getSubmittedEventIds());
        assertEquals(List.of(7001L), firstScrap.getProductionEventIds());
        assertEquals(1, firstScrap.getProductionSubmitterSignatures().size());
        assertEquals(17001L, firstScrap.getProductionSubmitterSignatures().get(0).getSignatureId());
        assertEquals("甲", firstScrap.getProductionSubmitterSignatures().get(0).getSignerName());
        assertEquals(LocalDateTime.parse("2026-08-13T08:10:00"),
                firstScrap.getProductionSubmitterSignatures().get(0).getSignedAt());
        assertEquals(2, firstScrap.getScrapQuantity());
        assertEquals("PQC王五", firstScrap.getSubmitterName());
        assertEquals(List.of(7102L), secondScrap.getSubmittedEventIds());
        assertEquals(List.of(7002L), secondScrap.getProductionEventIds());
        assertEquals(1, secondScrap.getProductionSubmitterSignatures().size());
        assertEquals(17002L, secondScrap.getProductionSubmitterSignatures().get(0).getSignatureId());
        assertEquals("乙", secondScrap.getProductionSubmitterSignatures().get(0).getSignerName());
        assertEquals(LocalDateTime.parse("2026-08-13T15:20:00"),
                secondScrap.getProductionSubmitterSignatures().get(0).getSignedAt());
        assertEquals(3, secondScrap.getScrapQuantity());
        assertEquals("PQC赵六", secondScrap.getSubmitterName());
    }

    @Test
    void shouldMarkAllSubmissionsOfOverrunProcessAsQuantityConflict() throws Exception {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(9201L)
                .activeStatus("ACTIVE")
                .build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "1500.000000", 7001L, "1000", "张三", "生产组长甲",
                        "2026-08-19T16:52:08"),
                row(9101L, 5001L, 6001L, "粗洗", "1500.000000", 7002L, "2000", "李四", "生产组长甲",
                        "2026-08-19T16:53:03")));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(pqcTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of());
        when(pqcAggregateDetailMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of());

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        MesTeamLeaderActiveOrderDetail.ProcessDetail process = detail.getProcesses().get(0);
        assertEquals(Boolean.TRUE, invokeBoolean(process, "getQuantityConflict"));
        assertEquals(0, new BigDecimal("1500").compareTo(invokeBigDecimal(process, "getOverageQuantity")));
        for (MesTeamLeaderActiveOrderDetail.SubmissionDetail submission : process.getSubmissions()) {
            assertEquals(Boolean.TRUE, invokeBoolean(submission, "getQuantityConflict"));
        }
    }

    @Test
    void shouldRejectActiveOrderOutsideCurrentLeaderOrRemovedOrder() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3002L)
                .activeStatus("ACTIVE")
                .build());

        ServiceException error = assertThrows(ServiceException.class, () -> service.getDetail(3001L, 8101L));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS.getCode(), error.getCode());
    }

    @Test
    void activeOrderDetailExposesCurrentStatusForReleaseApplication() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L).leaderUserId(3001L).workOrderId(9001L).routeId(9201L).activeStatus("ACTIVE").build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(backfillMapper.selectByActiveOrderAndType(8101L, "BATCH_RECORD")).thenReturn(null);
        when(releaseApplicationMapper.selectLatestByActiveOrderId(8101L)).thenReturn(
                MesProcessPoolActiveOrderReleaseApplicationDO.builder()
                        .id(7101L)
                        .activeOrderId(8101L)
                        .workOrderId(9001L)
                        .applicationStatus(MesReleaseFlowStatus.MANAGER_RELEASE_PENDING)
                        .build());

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        assertEquals(MesReleaseFlowStatus.MANAGER_RELEASE_PENDING, detail.getActiveOrderStatus().getStatus());
        assertEquals("待上市放行", detail.getActiveOrderStatus().getStatusLabel());
    }

    @Test
    void shouldFailWhenFormalProcessSnapshotsAreMissing() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .activeStatus("ACTIVE")
                .build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of());

        ServiceException error = assertThrows(ServiceException.class, () -> service.getDetail(3001L, 8101L));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED.getCode(), error.getCode());
    }

    private static MesTeamLeaderActiveOrderDetailReadDO row(Long snapshotId,
                                                              Long routeProcessId,
                                                              Long processId,
                                                             String processName,
                                                             String requiredQuantity,
                                                             Long eventId,
                                                             String submittedQuantity,
                                                             String submitterName,
                                                             String reviewerName,
                                                             String submittedAt) {
        LocalDateTime parsedSubmittedAt = submittedAt == null ? null : LocalDateTime.parse(submittedAt);
        return new MesTeamLeaderActiveOrderDetailReadDO()
                .setSnapshotId(snapshotId)
                .setActiveOrderId(8101L)
                .setWorkOrderId(9001L)
                .setWorkOrderCode("881MO090889")
                .setRouteName("球囊扩张压力泵工艺路线")
                .setRouteProcessId(routeProcessId)
                .setProcessId(processId)
                .setProcessCode("P-" + processId)
                .setProcessName(processName)
                .setKeyFlag(true)
                .setRequiredQuantity(new BigDecimal(requiredQuantity))
                .setEventId(eventId)
                .setSubmittedQuantity(submittedQuantity == null ? null : new BigDecimal(submittedQuantity))
                .setSubmitterName(submitterName)
                .setReviewerName(reviewerName)
                .setSubmittedAt(parsedSubmittedAt)
                .setSubmitterSignatureId(eventId == null ? null : eventId + 10000)
                .setSubmitterSignedAt(parsedSubmittedAt);
    }

    private static MesProProcessPoolEventDO defaultPqcEvent(Long eventId) {
        return MesProProcessPoolEventDO.builder()
                .id(eventId)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                .rawPayload("""
                        {"actualInspectionQuantity":1,"scrapQuantity":0,"pqcItemDetails":[{"itemCode":"AUTO","itemName":"自动项目","sampleValues":["AUTO"],"judgement":"PASS"}]}
                        """)
                .build();
    }

    private static Boolean invokeBoolean(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        return (Boolean) method.invoke(target);
    }

    private static BigDecimal invokeBigDecimal(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        return (BigDecimal) method.invoke(target);
    }
}
