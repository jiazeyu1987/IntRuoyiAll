package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTraceLinkDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.CatalogItem;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.Category;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.Condition;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionReceiptHash;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Actual adapter over hashed formal receipts; no database or live business writes. */
class MesProEdhrFormalReverseTraceCatalogOccurrenceTest {

    @ParameterizedTest
    @ValueSource(ints = {2, 8})
    void materialAtomsProduceOneOccurrenceEachAndFlattenedSummaryProducesNone(int materialCount) {
        var raw = materialPayload(materialCount);
        var fixture = new Fixture(raw);
        var catalog = fixture.adapter.readCatalog(fixture.batch);

        assertEquals(materialCount * 2, catalog.items().size());
        assertEquals(catalog.items().size(), catalog.items().stream().map(this::identity).distinct().count());
        assertTrue(catalog.items().stream().allMatch(item -> item.getQualifiers().containsKey("materialId")));
        for (int i = 0; i < materialCount; i++) {
            String materialId = String.valueOf(601 + i);
            assertEquals(2, catalog.items().stream()
                    .filter(item -> materialId.equals(item.getQualifiers().get("materialId"))).count());
        }
        var item = catalog.items().get(0);
        var condition = new Condition().setConditionId("C1").setEvidenceKey(item.getEvidenceKey())
                .setSourceView(item.getSourceView()).setQualifiers(item.getQualifiers()).setOperator("EQ").setValue(32);
        assertTrue(fixture.adapter.evaluate(fixture.batch, List.of(condition)).matches().get(0).matched());
        assertEquals(1, fixture.adapter.readEvidence(fixture.batch, List.of(condition)).items().size());
    }

    @Test
    void formalEventOnlySourceRemainsAnExplicitSourceShape() {
        var raw = new JSONObject();
        raw.put("deviceParameterReadings", readings());
        var fixture = new Fixture(raw);
        var items = fixture.adapter.readCatalog(fixture.batch).items();
        assertEquals(2, items.size());
        assertTrue(items.stream().noneMatch(item -> item.getQualifiers().containsKey("materialId")));
    }

    @Test
    void formalMaterialOnlySourceDoesNotRequireAnAbsentSummary() {
        var raw = materialPayload(2);
        raw.remove("deviceParameterReadings");
        var fixture = new Fixture(raw);
        assertEquals(4, fixture.adapter.readCatalog(fixture.batch).items().size());
    }

    @Test
    void equalValuesInSeparateFormalEventsRemainSeparateOccurrences() {
        var fixture = new Fixture(materialPayload(2), materialPayload(2));
        var items = fixture.adapter.readCatalog(fixture.batch).items();
        assertEquals(8, items.size());
        assertEquals(8, items.stream().map(this::identity).distinct().count());
        assertEquals(4, items.stream().filter(item -> item.getSourceRef().startsWith("event:5001#")).count());
        assertEquals(4, items.stream().filter(item -> item.getSourceRef().startsWith("event:5002#")).count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"empty", "value", "count", "unit"})
    void contradictorySummaryFailsInsteadOfCreatingOrDiscardingAnonymousFacts(String defect) {
        var raw = materialPayload(2);
        var summary = raw.getJSONArray("deviceParameterReadings");
        switch (defect) {
            case "empty" -> summary.clear();
            case "count" -> summary.remove(summary.size() - 1);
            case "value" -> summary.getJSONObject(0).put("value", 99);
            case "unit" -> summary.getJSONObject(0).put("unit", "F");
            default -> throw new AssertionError(defect);
        }
        assertSourceConflict(raw, "生产参数汇总与正式物料参数明细不一致");
    }

    @Test
    void emptyMaterialListHasFormalEventLevelReadingsForProcessesWithoutOutputMaterials() {
        var raw = materialPayload(1);
        raw.getJSONArray("materialDetails").clear();
        var fixture = new Fixture(raw);
        var items = fixture.adapter.readCatalog(fixture.batch).items();
        assertEquals(2, items.size());
        assertTrue(items.stream().noneMatch(item -> item.getQualifiers().containsKey("materialId")));
    }

    @Test
    void emptyMaterialListStillRequiresUniqueEventLevelAtomicIdentity() {
        var raw = materialPayload(1);
        raw.getJSONArray("materialDetails").clear();
        var top = raw.getJSONArray("deviceParameterReadings");
        top.add(JSON.parseObject(top.getJSONObject(0).toJSONString()));
        assertSourceConflict(raw, "同一正式参数来源的设备参数身份重复");
    }

    @Test
    void duplicateAtomicDeviceCodeFailsInsteadOfBeingSilentlyDeduplicated() {
        var raw = materialPayload(1);
        var nested = raw.getJSONArray("materialDetails").getJSONObject(0).getJSONArray("deviceParameterReadings");
        nested.add(JSON.parseObject(nested.getJSONObject(0).toJSONString()));
        raw.put("deviceParameterReadings", JSON.parseArray(nested.toJSONString()));
        assertSourceConflict(raw, "同一正式参数来源的设备参数身份重复");
    }

    @Test
    void repeatedMaterialIdCannotProduceTheSameOccurrenceTwice() {
        var raw = materialPayload(2);
        raw.getJSONArray("materialDetails").getJSONObject(1).put("materialId", 601);
        assertSourceConflict(raw, "生产参数物料身份无效或重复");
    }

    @Test
    void duplicateRootAtomicDeviceCodeAlsoFails() {
        var raw = new JSONObject();
        var values = readings();
        values.add(new LinkedHashMap<>(values.get(0)));
        raw.put("deviceParameterReadings", values);
        assertSourceConflict(raw, "同一正式参数来源的设备参数身份重复");
    }

    @ParameterizedTest
    @ValueSource(strings = {"material", "nested", "summary"})
    void malformedSourceArraysFailWithoutChangingSourceShape(String defect) {
        var raw = materialPayload(1);
        switch (defect) {
            case "material" -> raw.put("materialDetails", Map.of("materialId", 601));
            case "nested" -> raw.getJSONArray("materialDetails").getJSONObject(0)
                    .put("deviceParameterReadings", Map.of("parameterCode", "TEMP"));
            case "summary" -> raw.put("deviceParameterReadings", Map.of("parameterCode", "TEMP"));
            default -> throw new AssertionError(defect);
        }
        assertSourceConflict(raw, switch (defect) {
            case "material" -> "生产物料明细不是正式数组";
            case "nested" -> "物料参数明细不是正式数组";
            case "summary" -> "生产参数汇总不是正式数组";
            default -> throw new AssertionError(defect);
        });
    }

    @Test
    void materialIdentityIsRequiredBeforeReadingAnonymousAtoms() {
        var raw = materialPayload(1);
        raw.getJSONArray("materialDetails").getJSONObject(0).remove("materialId");
        var fixture = new Fixture(raw);
        var error = assertThrows(IllegalStateException.class, () -> fixture.adapter.readCatalog(fixture.batch));
        assertEquals("SOURCE_MISSING:生产参数物料身份缺失", error.getMessage());
    }

    private void assertSourceConflict(JSONObject raw, String exactReason) {
        var fixture = new Fixture(raw);
        var error = assertThrows(IllegalStateException.class, () -> fixture.adapter.readCatalog(fixture.batch));
        assertEquals("SOURCE_CONFLICT:" + exactReason, error.getMessage());
    }

    private String identity(CatalogItem item) {
        return item.getCategory() + "|" + item.getSourceView() + "|" + item.getEvidenceKey() + "|" + item.getSourceRef();
    }

    private static List<Map<String, Object>> readings() {
        return new ArrayList<>(List.of(new LinkedHashMap<>(Map.of("deviceId", 77, "parameterCode", "TEMP",
                "value", 32, "unit", "C", "lowerLimit", 10, "upperLimit", 40, "parameterStatus", "NORMAL")),
                new LinkedHashMap<>(Map.of("deviceId", 77, "parameterCode", "DURATION", "value", 5,
                        "unit", "min", "lowerLimit", 1, "upperLimit", 10, "parameterStatus", "NORMAL"))));
    }

    private static JSONObject materialPayload(int count) {
        List<Map<String, Object>> materials = new ArrayList<>();
        List<Map<String, Object>> flatten = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            var readings = readings();
            materials.add(Map.of("materialId", 601 + i, "deviceParameterReadings", readings));
            flatten.addAll(readings);
        }
        // Serialize each formal source independently: one graph would emit $ref and restore shared reading objects.
        var payload = JSON.parseObject(JSON.toJSONString(Map.of("materialDetails", materials)));
        payload.put("deviceParameterReadings", JSON.parseArray(JSON.toJSONString(flatten)));
        int summaryIndex = 0;
        for (Object value : payload.getJSONArray("materialDetails")) {
            var material = (JSONObject) value;
            for (Object reading : material.getJSONArray("deviceParameterReadings")) {
                assertNotSame(reading, payload.getJSONArray("deviceParameterReadings").get(summaryIndex++),
                        "Formal nested atom and root summary must be independent test objects");
            }
        }
        return payload;
    }

    private static final class Fixture {
        final MesProEdhrBatchExecutionDO batch = new MesProEdhrBatchExecutionDO().setId(9001L)
                .setWorkOrderId(1001L).setRouteId(2001L).setRouteVersionId(2002L).setAggregateHash("batch-hash")
                .setTenantId(1L);
        final MesProEdhrFormalReverseTraceAdapter adapter;

        Fixture(JSONObject... payloads) {
            batch.setUpdateTime(LocalDateTime.of(2026, 10, 4, 11, 0));
            var originMapper = mock(MesProEdhrBatchExecutionOriginMapper.class);
            var traceMapper = mock(MesProEdhrBatchExecutionTraceLinkMapper.class);
            var receiptMapper = mock(MesProcessPoolActiveOrderCompletionReceiptMapper.class);
            List<JSONObject> events = new ArrayList<>();
            List<Map<String, Object>> allocations = new ArrayList<>();
            List<Map<String, Object>> reviews = new ArrayList<>();
            List<MesProEdhrBatchExecutionTraceLinkDO> links = new ArrayList<>();
            for (int i = 0; i < payloads.length; i++) {
                long eventId = 5001L + i;
                long reviewId = eventId + 200;
                var event = new MesProProcessPoolEventDO().setId(eventId).setEventType("PRODUCTION_SUBMIT")
                        .setWorkOrderId(1001L).setRouteId(2001L).setRouteProcessId(3001L).setProcessId(4001L)
                        .setRawPayload(payloads[i].toJSONString()).setServerSubmitTime(batch.getUpdateTime());
                event.setTenantId(1L);
                String eventJson = JsonUtils.toJsonString(event);
                var frozen = JSON.parseObject(eventJson);
                frozen.put("payloadContentHash", DigestUtil.sha256Hex(event.getRawPayload()));
                events.add(frozen);
                allocations.add(Map.of("id", 6101L + i, "eventId", eventId, "tenantId", 1L, "activeOrderId", 6001L,
                        "workOrderId", 1001L, "routeProcessId", 3001L, "processId", 4001L,
                        "reviewId", reviewId, "leaderUserId", 31L));
                String signature = JsonUtils.toJsonString(Map.of("signatureId", reviewId + 1, "actorId", 31L,
                        "processPoolEventId", eventId, "actionType", "TEAM_LEADER_REVIEW",
                        "eventType", "PRODUCTION_SUBMIT", "leaderType", "PRODUCTION", "reviewStatus", "APPROVED"));
                reviews.add(Map.of("id", reviewId, "eventId", eventId, "tenantId", 1L, "leaderUserId", 31L,
                        "leaderType", "PRODUCTION", "reviewStatus", "APPROVED", "reviewedAt", batch.getUpdateTime(),
                        "reviewSignatureId", reviewId + 1, "reviewSignatureUserId", 31L,
                        "reviewSignatureSnapshotJson", signature));
                links.add(new MesProEdhrBatchExecutionTraceLinkDO().setBatchExecutionId(batch.getId()).setTenantId(1L)
                        .setLinkType("PRODUCTION_SUBMIT").setSourceEventId(eventId).setRelationStatus("BOUND")
                        .setSnapshotJson(eventJson).setSnapshotHash(MesProEdhrBatchTraceSourceHash.calculate("PRODUCTION_SUBMIT", eventJson)));
            }
            String snapshot = JsonUtils.toJsonString(Map.of("activeOrderBinding", Map.of("id", 6001L, "tenantId", 1L,
                            "workOrderId", 1001L, "routeId", 2001L, "routeVersionId", 2002L),
                    "workOrderBinding", Map.of("id", 1001L), "snapshots", List.of(Map.of("routeProcessId", 3001L, "processId", 4001L)),
                    "allocations", allocations, "pqcTasks", List.of(), "pqcDetails", List.of(),
                    "productionFacts", Map.of("formatVersion", 1, "events", events, "reviews", reviews)));
            var receipt = new MesProcessPoolActiveOrderCompletionReceiptDO().setId(8101L).setActiveOrderId(6001L)
                    .setWorkOrderId(1001L).setRouteId(2001L).setRouteVersionId(2002L).setFormalSourceSnapshotJson(snapshot)
                    .setLossConditionFactsJson("[]").setReceiptStatus(MesProcessPoolActiveOrderCompletionReceiptDO.RECEIPT_STATUS_BACKFILL_SUCCEEDED)
                    .setCompletionStatus(MesProcessPoolActiveOrderCompletionReceiptDO.STATUS_SUCCESS)
                    .setBatchRecordStatus(MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS)
                    .setProcessInspectionStatus(MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS)
                    .setCompletedVersion(1).setBatchRecordId(8201L).setProcessInspectionId(8301L);
            receipt.setTenantId(1L);
            receipt.setSourceSnapshotHash(MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.sourceSnapshotHash(
                    snapshot, receipt.getLossConditionFactsJson()));
            receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
            when(originMapper.selectListByBatchExecutionId(batch.getId())).thenReturn(List.of(
                    new MesProEdhrBatchExecutionOriginDO().setBatchExecutionId(batch.getId()).setTenantId(1L)
                            .setActiveOrderId(6001L).setWorkOrderId(1001L).setCompletionBackfillReceiptId(8101L)
                            .setCompletionVersion(1).setCompletionBackfillReceiptHash(receipt.getReceiptHash())
                            .setSourceSnapshotHash(receipt.getSourceSnapshotHash())));
            when(receiptMapper.selectByIdAndTenantId(8101L, 1L)).thenReturn(receipt);
            when(traceMapper.selectListByBatchExecutionId(batch.getId())).thenReturn(links);
            adapter = new MesProEdhrFormalReverseTraceAdapter(Category.PARAMETER,
                    mock(MesProBatchRecordExecutionMapper.class), originMapper, traceMapper,
                    mock(MesProEdhrBatchExecutionTaskMapper.class), mock(FormActionInstanceMapper.class),
                    mock(MesProBatchRecordExecutionFieldAuditItemMapper.class), mock(MesProProcessPoolEventMapper.class),
                    mock(MesProProcessPoolPqcRecordMapper.class), mock(MesProcessPoolSubmissionReviewMapper.class),
                    mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class), mock(MesProEdhrReleaseTransactionMapper.class), receiptMapper);
        }
    }
}
