package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MesTeamLeaderActiveOrderReleaseSourceSnapshotHasherTest {

    private final MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher hasher =
            new MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher();

    @Test
    void canonicalHashMustIgnoreCollectionOrderDecimalScaleAndSubsecondNoise() {
        String first = hasher.hash(input(false, "PASS", "10.0", 123_000_000));
        String reordered = hasher.hash(input(true, "PASS", "10.00", 999_000_000));

        assertEquals(first, reordered);
    }

    @Test
    void canonicalHashMustChangeWhenFormalMappedSourceValueChanges() {
        String passing = hasher.hash(input(false, "PASS", "10.0", 0));
        String failing = hasher.hash(input(false, "FAIL", "10.0", 0));

        assertNotEquals(passing, failing);
    }

    private static MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher.Input input(
            boolean reverse, String inspectionValue, String quantity, int nanos) {
        MesProcessPoolActiveOrderProcessSnapshotDO snapshot = snapshot();
        MesProcessPoolOrderProcessCompletionDO completion = MesProcessPoolOrderProcessCompletionDO.builder()
                .id(101L).workOrderId(20L).routeProcessId(40L).processId(50L)
                .targetQuantity(new BigDecimal(quantity)).confirmedQuantity(new BigDecimal(quantity))
                .completionStatus("COMPLETED")
                .completedAt(LocalDateTime.of(2026, 8, 9, 10, 0, 1, nanos))
                .backfillStatus("SUCCESS").backfillExecutionId(102L).lastEventId(103L).lastReviewId(104L)
                .aggregateHash("production-aggregate").backfillIdempotencyKey("backfill-key").build();

        MesTeamLeaderActiveOrderReleaseBatchRecordPlan batchPlan = batchPlan(snapshot, reverse, nanos);
        MesTeamLeaderActiveOrderReleaseProcessInspectionPlan inspectionPlan =
                inspectionPlan(reverse, inspectionValue, nanos);
        MesTeamLeaderActiveOrderReleaseLossReportPlan lossPlan = lossPlan(snapshot, reverse, nanos);
        MesProEdhrWorkTaskAssignmentRuleDO releaseRule = MesProEdhrWorkTaskAssignmentRuleDO.builder()
                .id(901L).scopeType("ROUTE").scopeId(30L).taskType("RELEASE_APPROVE")
                .candidateSourceType("ROLE_GROUP").candidateSourceId(902L).enabled(true).build();
        return new MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher.Input(
                1L,
                MesProcessPoolActiveOrderDO.builder().id(10L).leaderUserId(11L).workOrderId(20L)
                        .routeId(30L).routeVersionId(31L).activeStatus("ACTIVE")
                        .businessStatus("PRODUCING").erpFixedQuantitySnapshot(new BigDecimal(quantity)).build(),
                MesProWorkOrderDO.builder().id(20L).code("WO-20").productId(21L)
                        .batchCode("BATCH-20").quantity(new BigDecimal(quantity)).build(),
                MesMdItemDO.builder().id(21L).code("PRODUCT-21").specification("SPEC")
                        .unitMeasureId(22L).itemTypeId(23L).status(0).batchFlag(true).build(),
                MesProRouteDO.builder().id(30L).code("ROUTE-30").status(0).build(),
                MesProRouteVersionDO.builder().id(31L).routeId(30L).versionNo("V1")
                        .active(true).lifecycleStatus("ACTIVE").routeSnapshotJson("{\"version\":1}")
                        .publishedBy(11L).publishedTime(LocalDateTime.of(2026, 8, 8, 8, 0)).build(),
                List.of(snapshot), List.of(completion), batchPlan, inspectionPlan, lossPlan,
                releaseRule, "ROLE_GROUP", 902L, reverse ? List.of(904L, 903L) : List.of(903L, 904L));
    }

    private static MesTeamLeaderActiveOrderReleaseBatchRecordPlan batchPlan(
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot, boolean reverse, int nanos) {
        MesTeamLeaderActiveOrderReleaseBatchRecordPlan.PreparedProcess prepared =
                new MesTeamLeaderActiveOrderReleaseBatchRecordPlan.PreparedProcess()
                        .setSource(new MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand.ProcessSource()
                                .setSnapshot(snapshot))
                        .setBinding(binding(201L, "BATCH", "BATCH_RECORD", null))
                        .setRules(reverse ? List.of(rule(302L), rule(301L)) : List.of(rule(301L), rule(302L)));
        return new MesTeamLeaderActiveOrderReleaseBatchRecordPlan()
                .setPreparedProcesses(List.of(prepared))
                .setSourceObjectIds(reverse ? List.of(202L, 201L) : List.of(201L, 202L))
                .setSourceValueHashes(reverse ? List.of("batch-b", "batch-a") : List.of("batch-a", "batch-b"))
                .setSignatureEvidence(reverse
                        ? List.of(batchSignature("REVIEWER", 402L, nanos), batchSignature("FILLER", 401L, nanos))
                        : List.of(batchSignature("FILLER", 401L, nanos), batchSignature("REVIEWER", 402L, nanos)));
    }

    private static MesTeamLeaderActiveOrderReleaseProcessInspectionPlan inspectionPlan(
            boolean reverse, String mappedValue, int nanos) {
        MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source =
                new MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource()
                        .setTask(MesPqcInspectionTaskDO.builder().id(501L).routeProcessId(40L).processId(50L).build());
        MesTeamLeaderActiveOrderReleaseProcessInspectionPlan.MappedValue first =
                new MesTeamLeaderActiveOrderReleaseProcessInspectionPlan.MappedValue()
                        .setRule(rule(303L)).setValue(mappedValue);
        MesTeamLeaderActiveOrderReleaseProcessInspectionPlan.MappedValue second =
                new MesTeamLeaderActiveOrderReleaseProcessInspectionPlan.MappedValue()
                        .setRule(rule(304L)).setValue("SECONDARY");
        MesTeamLeaderActiveOrderReleaseProcessInspectionPlan.PreparedInspection prepared =
                new MesTeamLeaderActiveOrderReleaseProcessInspectionPlan.PreparedInspection()
                        .setSource(source).setBinding(binding(203L, "PQC", "INTERNAL_RECORD", "PROCESS_INSPECTION"))
                        .setMappedValues(reverse ? List.of(second, first) : List.of(first, second));
        return new MesTeamLeaderActiveOrderReleaseProcessInspectionPlan()
                .setPreparedInspections(List.of(prepared))
                .setSourceObjectIds(reverse ? List.of(502L, 501L) : List.of(501L, 502L))
                .setSourceValueHashes(reverse ? List.of("pqc-b", "pqc-a") : List.of("pqc-a", "pqc-b"))
                .setSignatureEvidence(reverse
                        ? List.of(inspectionSignature("REVIEWER", 404L, nanos),
                        inspectionSignature("FILLER", 403L, nanos))
                        : List.of(inspectionSignature("FILLER", 403L, nanos),
                        inspectionSignature("REVIEWER", 404L, nanos)));
    }

    private static MesTeamLeaderActiveOrderReleaseLossReportPlan lossPlan(
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot, boolean reverse, int nanos) {
        Map<String, Object> mapped = new LinkedHashMap<>();
        if (reverse) {
            mapped.put("reason", "MATERIAL");
            mapped.put("quantity", new BigDecimal("1.00"));
        } else {
            mapped.put("quantity", new BigDecimal("1.0"));
            mapped.put("reason", "MATERIAL");
        }
        MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource source =
                new MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource().setSnapshot(snapshot);
        MesTeamLeaderActiveOrderReleaseLossReportPlan.PreparedLossReport prepared =
                new MesTeamLeaderActiveOrderReleaseLossReportPlan.PreparedLossReport()
                        .setSources(List.of(source)).setBinding(binding(204L, "LOSS", "INTERNAL_RECORD", "LOSS_REPORT"))
                        .setRules(reverse ? List.of(rule(306L), rule(305L)) : List.of(rule(305L), rule(306L)))
                        .setMappedValues(mapped);
        return new MesTeamLeaderActiveOrderReleaseLossReportPlan()
                .setPreparedReports(List.of(prepared))
                .setSourceObjectIds(reverse ? List.of(602L, 601L) : List.of(601L, 602L))
                .setSourceValueHashes(reverse ? List.of("loss-b", "loss-a") : List.of("loss-a", "loss-b"))
                .setSignatureEvidence(reverse
                        ? List.of(commonSignature("REVIEWER", 406L, nanos), commonSignature("FILLER", 405L, nanos))
                        : List.of(commonSignature("FILLER", 405L, nanos), commonSignature("REVIEWER", 406L, nanos)));
    }

    private static MesProcessPoolActiveOrderProcessSnapshotDO snapshot() {
        return MesProcessPoolActiveOrderProcessSnapshotDO.builder().id(100L).activeOrderId(10L)
                .workOrderId(20L).routeId(30L).routeVersionId(31L)
                .routeProcessId(40L).processId(50L).build();
    }

    private static MesProRouteFlowProcessBatchRecordDO binding(
            Long id, String reportId, String category, String slotType) {
        return MesProRouteFlowProcessBatchRecordDO.builder().id(id).routeId(30L).routeProcessId(40L)
                .useType("BATCH").batchRecordReportId(reportId).batchRecordDefinitionId(id + 10)
                .batchRecordVersionId(id + 20).recordCategory(category).formSlotType(slotType)
                .recordCategorySnapshotHash("record-" + id).slotConfigSnapshotHash("slot-" + id).build();
    }

    private static MesProBatchRecordCellLinkRuleDO rule(Long id) {
        return new MesProBatchRecordCellLinkRuleDO().setId(id).setRuleVersion(1L)
                .setSourceType("FORMAL_SOURCE").setSourceFieldCode("value-" + id)
                .setTargetReportId("TARGET").setTargetRowIndex(0).setTargetColumnIndex(id.intValue())
                .setTargetCellKey("0:" + id).setTargetValueType("STRING").setEnabled(true);
    }

    private static MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult.SignatureEvidence batchSignature(
            String role, Long id, int nanos) {
        return new MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult.SignatureEvidence()
                .setRole(role).setSourceType("PRODUCTION").setSourceId(700L).setSignatureId(id).setUserId(701L)
                .setSignedAt(LocalDateTime.of(2026, 8, 9, 10, 1, 1, nanos)).setEvidenceHash("sig-" + id);
    }

    private static MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult.SignatureEvidence inspectionSignature(
            String role, Long id, int nanos) {
        return new MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult.SignatureEvidence()
                .setRole(role).setSourceType("PQC").setSourceId(702L).setSignatureId(id).setUserId(703L)
                .setSignedAt(LocalDateTime.of(2026, 8, 9, 10, 1, 1, nanos)).setEvidenceHash("sig-" + id);
    }

    private static MesTeamLeaderActiveOrderReleaseSignatureEvidence commonSignature(
            String role, Long id, int nanos) {
        return new MesTeamLeaderActiveOrderReleaseSignatureEvidence()
                .setRole(role).setSourceType("LOSS").setSourceId(704L).setSignatureId(id).setUserId(705L)
                .setSignedAt(LocalDateTime.of(2026, 8, 9, 10, 1, 1, nanos)).setEvidenceHash("sig-" + id);
    }
}
