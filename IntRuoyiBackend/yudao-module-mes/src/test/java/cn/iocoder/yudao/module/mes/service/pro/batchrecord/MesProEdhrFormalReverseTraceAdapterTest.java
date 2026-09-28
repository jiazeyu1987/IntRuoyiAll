package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTraceLinkDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrFormValueDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTraceLinkMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrFormValueMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.Category;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.Condition;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionReceiptHash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import cn.hutool.crypto.digest.DigestUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrFormalReverseTraceAdapterTest {

    @Mock private MesProBatchRecordExecutionMapper executionMapper;
    @Mock private MesProEdhrBatchExecutionOriginMapper originMapper;
    @Mock private MesProEdhrBatchExecutionTraceLinkMapper traceLinkMapper;
    @Mock private MesProEdhrBatchExecutionTaskMapper taskMapper;
    @Mock private MesProEdhrFormValueMapper formValueMapper;
    @Mock private cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper bpmInstanceMapper;
    @Mock private MesProBatchRecordExecutionFieldAuditItemMapper fieldAuditMapper;
    @Mock private MesProProcessPoolEventMapper eventMapper;
    @Mock private MesProProcessPoolPqcRecordMapper pqcRecordMapper;
    @Mock private MesProcessPoolSubmissionReviewMapper reviewMapper;
    @Mock private MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;
    @Mock private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Mock private MesProcessPoolActiveOrderPickListBindingMapper bindingMapper;
    @Mock private MesProcessPoolActiveOrderPickListBindingItemMapper bindingItemMapper;
    @Mock private MesProcessPoolActiveOrderCompletionReceiptMapper completionReceiptMapper;

    private MesProEdhrBatchExecutionDO batch;
    private List<MesProProcessPoolEventDO> frozenProductionEvents = List.of();

    @BeforeEach
    void setUp() {
        batch = new MesProEdhrBatchExecutionDO().setId(9001L).setWorkOrderId(1001L).setRouteId(2001L)
                .setRouteVersionId(2002L)
                .setAggregateHash("batch-hash").setTenantId(1L);
        batch.setUpdateTime(LocalDateTime.of(2026, 9, 25, 1, 0));
    }

    @Test
    void emptyCatalogAndEvaluationUseTheSameSourceVersion() {
        when(executionMapper.selectPage(any(MesProBatchRecordExecutionPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(), 0L));
        MesProEdhrFormalReverseTraceAdapter adapter = adapter(Category.FIELD);

        var catalog = adapter.readCatalog(batch);
        var result = adapter.evaluate(batch, List.of(condition("FIELD:4001:temperature", "EQ", 32, "SAVED_RECORD")));

        assertEquals("NO_RECORDED_FACT", catalog.status());
        assertEquals(catalog.sourceVersion(), result.sourceVersion());
        assertEquals(catalog.sourceVersion(), adapter.readEvidence(batch, List.of()).sourceVersion());
        assertEquals(catalog.sourceIdentity(), result.sourceIdentity());
        assertEquals("COMPLETE", result.status());
        assertTrue(result.matches().stream().noneMatch(MesProEdhrReverseTraceSourceAdapter.ConditionMatch::matched));
        when(executionMapper.selectPage(any(MesProBatchRecordExecutionPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(r1SavedExecution(3001L, "temperature", 32)), 1L));
        org.junit.jupiter.api.Assertions.assertNotEquals(catalog.sourceVersion(), adapter.readCatalog(batch).sourceVersion());
    }

    @Test
    void fieldReadsSavedCellValuesFromTheSameBatchExecution() {
        MesProBatchRecordExecutionDO execution = r1SavedExecution(3001L, "temperature", 32);
        execution.setUpdateTime(batch.getUpdateTime());
        when(executionMapper.selectPage(any(MesProBatchRecordExecutionPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(execution), 1L));

        MesProEdhrFormalReverseTraceAdapter adapter = adapter(Category.FIELD);
        var catalog = adapter.readCatalog(batch);

        assertEquals("FIELD:4001:[\"report-A\",\"sheet[0].rows[1].cells[2].temperature\",\"temperature\"]",
                catalog.items().get(0).getEvidenceKey());
        assertEquals("32", catalog.items().get(0).getSavedValue());
        assertTrue(adapter.evaluate(batch, List.of(condition(catalog.items().get(0).getEvidenceKey(), "EQ", 32, "SAVED_RECORD"))).matches().get(0).matched());
    }

    @Test
    void fieldReadsAllPagesWhenFormalExecutionRowsExceedPageSize() {
        List<MesProBatchRecordExecutionDO> firstPage = java.util.stream.IntStream.range(0, 200)
                .mapToObj(index -> r1SavedExecution(3001L + index, "field" + index, index))
                .toList();
        MesProBatchRecordExecutionDO secondPage = r1SavedExecution(3201L, "pageTwoField", 201);
        when(executionMapper.selectPage(any(MesProBatchRecordExecutionPageReqVO.class))).thenAnswer(invocation -> {
            MesProBatchRecordExecutionPageReqVO request = invocation.getArgument(0);
            assertEquals(200, request.getPageSize());
            return request.getPageNo() == 1
                    ? new PageResult<>(firstPage, 201L)
                    : new PageResult<>(List.of(secondPage), 201L);
        });

        var catalog = adapter(Category.FIELD).readCatalog(batch);

        assertTrue(catalog.items().stream().anyMatch(item -> item.getEvidenceKey().equals(
                "FIELD:4001:[\"report-A\",\"sheet[0].rows[1].cells[2].pageTwoField\",\"pageTwoField\"]")));
    }

    @Test
    void sourceVersionChangesWhenTheFormalSavedFactChanges() {
        MesProBatchRecordExecutionDO first = r1SavedExecution(3001L, "temperature", 32);
        MesProBatchRecordExecutionDO revised = r1SavedExecution(3001L, "temperature", 35);
        when(executionMapper.selectPage(any(MesProBatchRecordExecutionPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(first), 1L), new PageResult<>(List.of(revised), 1L));

        MesProEdhrFormalReverseTraceAdapter adapter = adapter(Category.FIELD);
        String firstVersion = adapter.readCatalog(batch).sourceVersion();
        String revisedVersion = adapter.readCatalog(batch).sourceVersion();

        assertTrue(!firstVersion.equals(revisedVersion), "正式来源内容变化后sourceVersion必须变化");
    }

    @Test
    void unrelatedMesFormValueCannotSupplyABpmInstanceFact() {
        when(executionMapper.selectPage(any(MesProBatchRecordExecutionPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(), 0L));
        when(taskMapper.selectListByBatchExecutionId(batch.getId())).thenReturn(List.of(
                new MesProEdhrBatchExecutionTaskDO().setBatchExecutionId(batch.getId())
                        .setFormTemplateVersionId(4201L).setFormCenterInstanceId(9101L).setSubmittedAt(batch.getUpdateTime())));
        org.mockito.Mockito.lenient().when(formValueMapper.selectListByInstanceId(9101L)).thenReturn(List.of(
                new MesProEdhrFormValueDO().setInstanceId(9101L).setFieldKey("ph")
                        .setFieldLabel("PH值").setValueText("7.0")));

        MesProEdhrFormalReverseTraceAdapter adapter = adapter(Category.FIELD);
        assertTrue(assertThrows(IllegalStateException.class,
                () -> adapter.evaluate(batch, List.of(condition("FIELD_DYNAMIC:4201:ph", "EQ", "7.0", "SAVED_RECORD"))))
                .getMessage().startsWith("SOURCE_MISSING:"));
        org.mockito.Mockito.verifyNoInteractions(formValueMapper);
    }

    @Test
    void dynamicBpmSavedValuesPreserveTypesAndRepeatRows() {
        var instance = stubDynamicBpmInstance();
        var adapter = adapter(Category.FIELD);
        var catalog = adapter.readCatalog(batch);
        assertTrue(adapter.evaluate(batch, List.of(condition("FIELD_DYNAMIC:4201:ph", "EQ", 7, "SAVED_RECORD")))
                .matches().get(0).matched());
        assertEquals("string", catalog.items().stream().filter(item -> item.getLabel().equals("serial")).findFirst().orElseThrow().getValueType());
        assertEquals(2, catalog.items().stream().filter(item -> item.getLabel().startsWith("rows[")).count());
        assertEquals(catalog.items().size(), catalog.items().stream().map(item -> item.getEvidenceKey()).distinct().count());
        assertTrue(catalog.items().stream().allMatch(item -> item.getSourceRef().startsWith("bpm-form-instance:9101#")));
        instance.setTenantId(2L);
        assertTrue(assertThrows(IllegalStateException.class, () -> adapter.readCatalog(batch)).getMessage().startsWith("SOURCE_CONFLICT:"));
    }

    @Test
    void dynamicBpmTemplateVersionDriftBlocksInsteadOfReadingCurrentTemplate() {
        var instance = stubDynamicBpmInstance();
        instance.setFormDataJson(instance.getFormDataJson().replace("4201", "4202"));
        assertTrue(assertThrows(IllegalStateException.class, () -> adapter(Category.FIELD).readCatalog(batch))
                .getMessage().startsWith("SOURCE_CONFLICT:"));
    }

    private cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO stubDynamicBpmInstance() {
        batch.setRouteVersionId(2002L);
        when(executionMapper.selectPage(any(MesProBatchRecordExecutionPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(), 0L));
        when(taskMapper.selectListByBatchExecutionId(batch.getId())).thenReturn(List.of(
                new MesProEdhrBatchExecutionTaskDO().setId(9100L).setBatchExecutionId(batch.getId())
                        .setFormTemplateId(4200L).setFormTemplateVersionId(4201L)
                        .setFormBindingKey("MAIN").setFormCenterInstanceId(9101L)));
        var instance = cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO.builder()
                .id(9101L).tenantId(1L).dataDomain("MES").systemCode("MES").objectType("EDHR_ROUTE_FORM")
                .objectId("9100").objectVersion("2002")
                .actionCode(cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionPublishProjectionServiceImpl.routeFormActionCode(2002L, "MAIN"))
                .formDataJson("{\"batchExecutionId\":9001,\"batchTaskId\":9100,\"formTemplateId\":4200,\"formTemplateVersionId\":4201,\"ph\":7,\"serial\":\"007\",\"rows\":[{\"value\":1},{\"value\":2}]}")
                .build();
        when(bpmInstanceMapper.selectById(9101L)).thenReturn(instance);
        return instance;
    }

    @Test
    void eventSourcesKeepParameterEquipmentAndPeopleSeparate() {
        when(traceLinkMapper.selectListByBatchExecutionId(batch.getId())).thenReturn(List.of(
                new MesProEdhrBatchExecutionTraceLinkDO().setBatchExecutionId(batch.getId())
                        .setLinkType("PRODUCTION_SUBMIT").setSourceEventId(5001L)));
        when(eventMapper.selectProductionSubmitsByIds(List.of(5001L)))
                .thenReturn(List.of(new MesProProcessPoolEventDO().setId(5001L).setDeviceId(77L)
                        .setActualEmployeeId(11L).setSignatureUserId(12L).setServerSubmitTime(batch.getUpdateTime())
                        .setRawPayload("{\"selectedDevices\":[{\"deviceId\":78,\"deviceCode\":\"P-78\"},{\"deviceId\":79,\"deviceCode\":\"P-79\"}],\"deviceParameterReadings\":[{\"parameterCode\":\"TEMP\",\"value\":32}]}")));
        when(originMapper.selectListByBatchExecutionId(batch.getId())).thenReturn(List.of(
                new MesProEdhrBatchExecutionOriginDO().setBatchExecutionId(batch.getId()).setTenantId(1L)
                        .setActiveOrderId(6001L).setWorkOrderId(batch.getWorkOrderId()).setCompletionBackfillReceiptId(8101L)));
        when(completionReceiptMapper.selectByIdAndTenantId(8101L, 1L)).thenReturn(formalReceipt("{\"pqcDetails\":[]}"));
        when(releaseApplicationMapper.selectListByBatchExecutionIds(List.of(batch.getId()))).thenReturn(List.of(
                new MesProcessPoolActiveOrderReleaseApplicationDO().setId(5301L).setPqcDecidedBy(41L)
                        .setAppliedBy(42L).setPqcDecidedAt(batch.getUpdateTime()).setAppliedAt(batch.getUpdateTime())));
        when(releaseTransactionMapper.selectByBatchExecutionId(batch.getId())).thenReturn(
                new MesProEdhrReleaseTransactionDO().setId(5401L).setSubmittedBy(42L).setApprovedBy(43L)
                        .setSubmittedAt(batch.getUpdateTime()).setApprovedAt(batch.getUpdateTime()));

        bindStubbedEvents(List.of(5001L));

        assertTrue(adapter(Category.PARAMETER).evaluate(batch,
                List.of(formalParameterCondition("PARAMETER:TEMP", "EQ", 32))).matches().get(0).matched());
        var equipmentCatalog = adapter(Category.EQUIPMENT).readCatalog(batch);
        var productionDevice78 = equipmentCatalog.items().stream()
                .filter(item -> "EQUIPMENT:PRODUCTION:deviceId:78".equals(item.getEvidenceKey()))
                .findFirst().orElseThrow();
        var productionDevice79 = equipmentCatalog.items().stream()
                .filter(item -> "EQUIPMENT:PRODUCTION:deviceId:79".equals(item.getEvidenceKey()))
                .findFirst().orElseThrow();
        assertTrue(productionDevice78.getQualifiers().isEmpty());
        assertTrue(productionDevice79.getQualifiers().isEmpty());
        assertEquals("P-78", adapter(Category.EQUIPMENT).readEvidence(batch, List.of(
                condition("EQUIPMENT:PRODUCTION:deviceId:78", "EQ", 78))).items().get(0).getSourceContext().get("deviceCode"));
        assertEquals("P-79", adapter(Category.EQUIPMENT).readEvidence(batch, List.of(
                condition("EQUIPMENT:PRODUCTION:deviceId:79", "EQ", 79))).items().get(0).getSourceContext().get("deviceCode"));
        org.junit.jupiter.api.Assertions.assertFalse(adapter(Category.EQUIPMENT).evaluate(batch, List.of(condition("EQUIPMENT:PRODUCTION:deviceId:77", "EQ", 77))).matches().get(0).matched());
        assertTrue(adapter(Category.EQUIPMENT).evaluate(batch, List.of(condition("EQUIPMENT:PRODUCTION:deviceId:78", "EQ", 78))).matches().get(0).matched());
        assertTrue(adapter(Category.EQUIPMENT).evaluate(batch, List.of(condition("EQUIPMENT:PRODUCTION:deviceId:79", "EQ", 79))).matches().get(0).matched());
        assertTrue(adapter(Category.PERSON).evaluate(batch, List.of(condition("PERSON:EMPLOYEE:actualEmployeeId:11", "EQ", 11))).matches().get(0).matched());
        assertTrue(adapter(Category.PERSON).evaluate(batch, List.of(condition("PERSON:SYSTEM_USER:signatureUserId:12", "EQ", 12))).matches().get(0).matched());
        assertTrue(adapter(Category.PERSON).evaluate(batch, List.of(condition("PERSON:SYSTEM_USER:PRODUCTION_REVIEW:31", "EQ", 31))).matches().get(0).matched());
        assertTrue(adapter(Category.PERSON).evaluate(batch, List.of(condition("PERSON:SYSTEM_USER:PQC_RELEASE:41", "EQ", 41))).matches().get(0).matched());
        assertTrue(adapter(Category.PERSON).evaluate(batch, List.of(condition("PERSON:SYSTEM_USER:LISTING_RELEASE_APPROVER:43", "EQ", 43))).matches().get(0).matched());
    }

    @Test
    void pqcPersonnelResolveThroughFormalTaskAndAggregateWhenProductionSubmitEventIdIsNull() {
        stubEvent("{}");
        String snapshot = "{\"allocations\":[{\"eventId\":5001,\"routeProcessId\":3001,\"processId\":4001}],"
                + "\"pqcTasks\":[{\"id\":8001,\"tenantId\":1,\"activeOrderId\":6001,\"workOrderId\":1001,"
                + "\"routeId\":2001,\"routeVersionId\":2002,\"routeProcessId\":3001,\"processId\":4001,"
                + "\"qaProcessId\":7101,\"submittedEventId\":5101}],"
                + "\"pqcDetails\":[{\"id\":7001,\"tenantId\":1,\"sourcePqcRecordId\":5201,"
                + "\"sourcePieceDetailId\":5301,\"eventId\":5101,\"reviewId\":6101,\"productionSubmitEventId\":null,"
                + "\"pqcTaskId\":8001,\"activeOrderId\":6001,\"workOrderId\":1001,\"routeId\":2001,"
                + "\"routeVersionId\":2002,\"routeProcessId\":3001,\"processId\":4001,"
                + "\"regulationVersionId\":7201}]}";
        stubReceipt(snapshot);

        MesProProcessPoolEventDO pqcEvent = new MesProProcessPoolEventDO().setId(5101L)
                .setEventType("PQC_INSPECTION").setWorkOrderId(1001L).setRouteId(2001L)
                .setQaProcessId(7101L).setActualEmployeeId(21L).setSignatureUserId(21L)
                .setFeedbackSourceType("MES_PQC_INSPECTION_TASK").setFeedbackSourceId(8001L)
                .setRecordbookSourceType("MES_PQC_INSPECTION_TASK").setRecordbookSourceId(8001L)
                .setServerSubmitTime(batch.getUpdateTime());
        pqcEvent.setTenantId(1L);
        when(eventMapper.selectById(5101L)).thenReturn(pqcEvent);
        MesProProcessPoolPqcRecordDO pqcRecord = new MesProProcessPoolPqcRecordDO()
                .setId(5201L).setEventId(5101L).setWorkOrderId(1001L).setRouteId(2001L)
                .setQaProcessId(7101L).setActualEmployeeId(21L).setSignatureUserId(21L)
                .setServerSubmitTime(batch.getUpdateTime());
        pqcRecord.setTenantId(1L);
        when(pqcRecordMapper.selectByEventId(5101L)).thenReturn(pqcRecord);
        var pqcReview = new MesProcessPoolSubmissionReviewDO().setId(6101L).setEventId(5101L)
                .setLeaderUserId(31L).setLeaderType("PQC").setReviewStatus("APPROVED")
                .setReviewedAt(batch.getUpdateTime()).setReviewSignatureId(9101L).setReviewSignatureUserId(31L)
                .setReviewSignatureSnapshotJson(JsonUtils.toJsonString(Map.of("signatureId", 9101L,
                        "actorId", 31L, "processPoolEventId", 5101L, "actionType", "TEAM_LEADER_REVIEW",
                        "eventType", "PQC_INSPECTION", "leaderType", "PQC", "reviewStatus", "APPROVED")));
        pqcReview.setTenantId(1L);
        when(reviewMapper.selectById(6101L)).thenReturn(pqcReview);
        when(releaseApplicationMapper.selectListByBatchExecutionIds(List.of(batch.getId()))).thenReturn(List.of());

        var adapter = adapter(Category.PERSON);

        assertTrue(adapter.evaluate(batch,
                List.of(condition("PERSON:EMPLOYEE:PQC_OPERATOR:21", "EQ", 21))).matches().get(0).matched());
        assertTrue(adapter.evaluate(batch,
                List.of(condition("PERSON:SYSTEM_USER:PQC_SIGNATURE:21", "EQ", 21))).matches().get(0).matched());
        org.mockito.Mockito.verify(pqcRecordMapper, org.mockito.Mockito.never())
                .selectListByProductionSubmitEventId(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void parameterReadingsRetainTheirFormalMaterialDeviceAndStandardQualifiers() {
        when(traceLinkMapper.selectListByBatchExecutionId(batch.getId())).thenReturn(List.of(
                new MesProEdhrBatchExecutionTraceLinkDO().setBatchExecutionId(batch.getId())
                        .setLinkType("PRODUCTION_SUBMIT").setSourceEventId(5501L)));
        when(eventMapper.selectProductionSubmitsByIds(List.of(5501L))).thenReturn(List.of(
                new MesProProcessPoolEventDO().setId(5501L).setServerSubmitTime(batch.getUpdateTime())
                        .setRawPayload("{\"materialDetails\":["
                                + "{\"materialId\":601,\"deviceParameterReadings\":[{\"deviceId\":77,\"parameterCode\":\"TEMP\",\"value\":32,\"unit\":\"C\",\"lowerLimit\":10,\"upperLimit\":30,\"parameterStatus\":\"ABOVE_UPPER\"}]},"
                                + "{\"materialId\":602,\"deviceParameterReadings\":[{\"deviceId\":78,\"parameterCode\":\"TEMP\",\"value\":25,\"unit\":\"C\",\"lowerLimit\":10,\"upperLimit\":30,\"parameterStatus\":\"NORMAL\"}]}],"
                                + "\"deviceParameterReadings\":[]}")));

        MesProEdhrFormalReverseTraceAdapter adapter = adapter(Category.PARAMETER);
        bindStubbedEvents(List.of(5501L));
        var catalog = adapter.readCatalog(batch);
        var targetFact = catalog.items().stream().filter(item -> "32".equals(item.getSavedValue())).findFirst().orElseThrow();
        assertEquals("601", targetFact.getQualifiers().get("materialId"));
        assertEquals("77", targetFact.getQualifiers().get("deviceId"));
        assertEquals("10", targetFact.getLowerLimit());
        assertEquals("30", targetFact.getUpperLimit());
        assertTrue(adapter.evaluate(batch, List.of(formalParameterCondition("PARAMETER:TEMP", "OUT_OF_LIMIT",
                java.util.Map.of("lower", 10, "upper", 30)).setQualifiers(targetFact.getQualifiers())))
                .matches().get(0).matched());
    }

    @Test
    void inspectionAndMaterialUseFormalPersistedRows() {
        String snapshot = "{\"pickListBindingItems\":{\"8001\":[{\"id\":9001,\"pickListItemId\":9002,\"materialNumber\":\"MAT-01\",\"materialName\":\"原料\",\"lotNumber\":\"LOT-01\",\"sourceModifyTime\":\"2026-09-25T01:00:00\"}]},\"pqcDetails\":[{\"id\":7001,\"routeProcessId\":3001,\"regulationVersionId\":7101,\"sampleNo\":2,\"itemCode\":\"APPEARANCE\",\"itemName\":\"外观\",\"standardText\":\"PASS\",\"standardLowerLimit\":0,\"standardUpperLimit\":1,\"standardUnit\":\"级\",\"standardPrecision\":0,\"resultType\":\"NUMBER\",\"itemResult\":\"1\",\"measuredValue\":\"1\",\"judgement\":\"PASS\",\"selectedEquipmentId\":8801,\"selectedEquipmentCode\":\"PQC-01\",\"selectedEquipmentName\":\"检验台\",\"selectedEquipmentNumber\":\"EQ-8801\",\"aggregatedAt\":\"2026-09-25T01:00:00\"}]}";
        stubReceipt(snapshot);

        var inspection = adapter(Category.INSPECTION);
        var catalog = inspection.readCatalog(batch);
        var inspectionItem = catalog.items().stream().filter(item -> item.getEvidenceKey().contains("APPEARANCE")).findFirst().orElseThrow();
        assertEquals("number", inspectionItem.getValueType());
        assertEquals("1", inspectionItem.getSavedValue());
        assertEquals("7101", inspectionItem.getQualifiers().get("regulationVersionId"));
        assertEquals("2", inspectionItem.getQualifiers().get("sampleNo"));
        assertEquals("APPEARANCE", inspectionItem.getQualifiers().get("itemCode"));
        assertEquals("PQC-01", inspectionItem.getQualifiers().get("selectedEquipmentCode"));
        assertEquals("EQ-8801", inspectionItem.getQualifiers().get("selectedEquipmentNumber"));
        assertEquals(0, inspectionItem.getStandardPrecision());
        assertTrue(inspection.evaluate(batch,
                List.of(new Condition().setConditionId("C1").setEvidenceKey(inspectionItem.getEvidenceKey())
                        .setSourceView("RECORDED").setOperator("EQ").setValue(new BigDecimal("1"))
                        .setQualifiers(java.util.Map.of("regulationVersionId", "7101", "sampleNo", "2",
                                "selectedEquipmentCode", "PQC-01", "selectedEquipmentNumber", "EQ-8801"))))
                .matches().get(0).matched());
        assertTrue(adapter(Category.MATERIAL).evaluate(batch,
                List.of(condition("MATERIAL:MAT-01:LOT-01", "EQ", "LOT-01"))).matches().get(0).matched());
        assertTrue(adapter(Category.EQUIPMENT).evaluate(batch,
                List.of(condition("EQUIPMENT:PQC:selectedEquipmentId:8801", "EQ", 8801))).matches().get(0).matched());
    }

    @Test
    void samePqcEquipmentAcrossInspectionProjectsAndSamplesRemainsMatchable() {
        String snapshot = "{\"pqcDetails\":["
                + "{\"id\":7001,\"routeProcessId\":3001,\"regulationVersionId\":7101,\"sampleNo\":1,\"itemCode\":\"APPEARANCE\",\"itemName\":\"外观\",\"resultType\":\"TEXT\",\"itemResult\":\"OK\",\"measuredValue\":\"OK\",\"judgement\":\"PASS\",\"selectedEquipmentId\":8801,\"selectedEquipmentCode\":\"PQC-01\",\"selectedEquipmentNumber\":\"EQ-8801\",\"aggregatedAt\":\"2026-09-25T01:00:00\"},"
                + "{\"id\":7002,\"routeProcessId\":3002,\"regulationVersionId\":7102,\"sampleNo\":2,\"itemCode\":\"WIDTH\",\"itemName\":\"宽度\",\"resultType\":\"TEXT\",\"itemResult\":\"OK\",\"measuredValue\":\"OK\",\"judgement\":\"PASS\",\"selectedEquipmentId\":8801,\"selectedEquipmentCode\":\"PQC-01\",\"selectedEquipmentNumber\":\"EQ-8801\",\"aggregatedAt\":\"2026-09-25T01:01:00\"}]}";
        stubReceipt(snapshot);

        var adapter = adapter(Category.EQUIPMENT);
        var catalog = adapter.readCatalog(batch);
        var equipmentItems = catalog.items().stream()
                .filter(item -> "EQUIPMENT:PQC:selectedEquipmentId:8801".equals(item.getEvidenceKey()))
                .toList();
        assertEquals(2, equipmentItems.size());

        var condition = condition("EQUIPMENT:PQC:selectedEquipmentId:8801", "EQ", 8801);
        assertTrue(adapter.evaluate(batch, List.of(condition)).matches().get(0).matched());
        var evidence = adapter.readEvidence(batch, List.of(condition));

        assertEquals(2, evidence.items().size(), "设备条件不应被项目或样本上下文缩窄");
        assertTrue(evidence.items().stream().allMatch(item -> item.getSourceRef().startsWith("pqc-aggregate-snapshot:")));
        assertEquals("7101", evidence.items().get(0).getSourceContext().get("regulationVersionId"));
        assertEquals("3001", evidence.items().get(0).getSourceContext().get("routeProcessId"));
        assertEquals("1", evidence.items().get(0).getSourceContext().get("sampleNo"));
        assertEquals("APPEARANCE", evidence.items().get(0).getSourceContext().get("itemCode"));
        assertEquals("7102", evidence.items().get(1).getSourceContext().get("regulationVersionId"));
        assertEquals("3002", evidence.items().get(1).getSourceContext().get("routeProcessId"));
        assertEquals("2", evidence.items().get(1).getSourceContext().get("sampleNo"));
        assertEquals("WIDTH", evidence.items().get(1).getSourceContext().get("itemCode"));
    }

    @Test
    void productionAndPqcEquipmentWithSameIdRemainDifferentIdentities() {
        stubEvent("{\"selectedDevices\":[{\"deviceId\":8801,\"deviceCode\":\"PROD-01\"}]}");
        stubReceipt("{\"pqcDetails\":[{\"id\":7001,\"routeProcessId\":3001,\"regulationVersionId\":7101,\"sampleNo\":1,\"itemCode\":\"APPEARANCE\",\"itemName\":\"外观\",\"resultType\":\"TEXT\",\"itemResult\":\"OK\",\"measuredValue\":\"OK\",\"judgement\":\"PASS\",\"selectedEquipmentId\":8801,\"selectedEquipmentCode\":\"PQC-01\",\"selectedEquipmentNumber\":\"EQ-8801\",\"aggregatedAt\":\"2026-09-25T01:00:00\"}],\"allocations\":[{\"eventId\":5001}]}");

        var adapter = adapter(Category.EQUIPMENT);
        var catalog = adapter.readCatalog(batch);

        assertTrue(catalog.items().stream().anyMatch(item ->
                "EQUIPMENT:PRODUCTION:deviceId:8801".equals(item.getEvidenceKey())));
        assertTrue(catalog.items().stream().anyMatch(item ->
                "EQUIPMENT:PQC:selectedEquipmentId:8801".equals(item.getEvidenceKey())));
        assertTrue(adapter.evaluate(batch, List.of(
                condition("EQUIPMENT:PRODUCTION:deviceId:8801", "EQ", 8801)))
                .matches().get(0).matched());
        assertTrue(adapter.evaluate(batch, List.of(
                condition("EQUIPMENT:PQC:selectedEquipmentId:8801", "EQ", 8801)))
                .matches().get(0).matched());
    }

    @Test
    void formalSnapshotTimesUseJsonUtilsEpochMillisProtocol() {
        LocalDateTime expected = LocalDateTime.of(2026, 9, 25, 1, 2, 3, 456_000_000);
        String snapshot = JsonUtils.toJsonString(Map.of(
                "pickListBindingItems", List.of(Map.of(
                        "id", 9001L,
                        "pickListItemId", 9002L,
                        "materialNumber", "MAT-EPOCH",
                        "materialName", "原料",
                        "lotNumber", "LOT-EPOCH",
                        "sourceModifyTime", expected)),
                "pqcDetails", List.of(Map.ofEntries(
                        Map.entry("id", 7001L),
                        Map.entry("routeProcessId", 3001L),
                        Map.entry("regulationVersionId", 7101L),
                        Map.entry("sampleNo", 2),
                        Map.entry("itemCode", "EPOCH"),
                        Map.entry("itemName", "时间戳检验"),
                        Map.entry("standardText", "PASS"),
                        Map.entry("standardLowerLimit", 0),
                        Map.entry("standardUpperLimit", 1),
                        Map.entry("standardUnit", "级"),
                        Map.entry("standardPrecision", 0),
                        Map.entry("resultType", "NUMBER"),
                        Map.entry("itemResult", "1"),
                        Map.entry("measuredValue", "1"),
                        Map.entry("judgement", "PASS"),
                        Map.entry("selectedEquipmentId", 8801L),
                        Map.entry("selectedEquipmentCode", "PQC-EPOCH"),
                        Map.entry("selectedEquipmentName", "检验台"),
                        Map.entry("selectedEquipmentNumber", "EQ-EPOCH"),
                        Map.entry("aggregatedAt", expected)))));
        long expectedEpochMillis = expected.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertTrue(snapshot.contains("\"sourceModifyTime\":" + expectedEpochMillis));
        assertTrue(snapshot.contains("\"aggregatedAt\":" + expectedEpochMillis));
        stubReceipt(snapshot);

        var material = adapter(Category.MATERIAL).readCatalog(batch);
        assertEquals(expected, material.items().get(0).getRecordedAt());
        var inspection = adapter(Category.INSPECTION).readCatalog(batch);
        assertEquals(expected, inspection.items().get(0).getRecordedAt());
    }

    @Test
    void missingFormalSourceDoesNotCreateFacts() {
        when(originMapper.selectListByBatchExecutionId(batch.getId())).thenReturn(List.of());
        var catalog = adapter(Category.MATERIAL).readCatalog(batch);
        assertEquals("SOURCE_MISSING", catalog.status());
        assertTrue(assertThrows(IllegalStateException.class, () -> adapter(Category.MATERIAL).evaluate(batch,
                List.of(condition("MATERIAL:MAT-01:LOT-01", "EQ", "LOT-01"))))
                .getMessage().startsWith("SOURCE_MISSING:"));
        assertTrue(assertThrows(IllegalStateException.class, () -> adapter(Category.MATERIAL).readEvidence(batch,
                List.of(condition("MATERIAL:MAT-01:LOT-01", "EQ", "LOT-01"))))
                .getMessage().startsWith("SOURCE_MISSING:"));
    }

    @Test
    void formalReceiptIntegrityAndTenantMismatchBlockFactGeneration() {
        stubReceipt("{\"pickListBindingItems\":[]}");
        MesProcessPoolActiveOrderCompletionReceiptDO receipt = completionReceiptMapper.selectByIdAndTenantId(8101L, 1L);
        receipt.setSourceSnapshotHash("snapshot-drift");
        when(completionReceiptMapper.selectByIdAndTenantId(8101L, 1L)).thenReturn(receipt);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> adapter(Category.MATERIAL).readCatalog(batch));

        assertTrue(exception.getMessage().startsWith("SOURCE_CONFLICT:"));
    }

    @Test
    void textualPqcValueKeepsTextTypeAndItemCodeIsScopedByVersionAndSample() {
        String snapshot = "{\"pqcDetails\":["
                + "{\"id\":7001,\"routeProcessId\":3001,\"regulationVersionId\":7101,\"sampleNo\":1,\"itemCode\":\"SAME\",\"itemName\":\"文本\",\"resultType\":\"TEXT\",\"itemResult\":\"123\",\"measuredValue\":\"123\",\"judgement\":\"PASS\",\"standardText\":\"123\"},"
                + "{\"id\":7002,\"routeProcessId\":3001,\"regulationVersionId\":7101,\"sampleNo\":2,\"itemCode\":\"SAME\",\"itemName\":\"文本\",\"resultType\":\"TEXT\",\"itemResult\":\"456\",\"measuredValue\":\"456\",\"judgement\":\"PASS\",\"standardText\":\"456\"}]}";
        stubReceipt(snapshot);

        var catalog = adapter(Category.INSPECTION).readCatalog(batch);

        assertEquals(2, catalog.items().stream().filter(item -> item.getEvidenceKey().endsWith(":SAME")).count());
        var first = catalog.items().stream().filter(item -> item.getQualifiers().get("sampleNo").equals("1"))
                .findFirst().orElseThrow();
        assertEquals("string", first.getValueType());
        assertEquals("123", first.getSavedValue());
        assertEquals("123", first.getRecordedStandard());
    }

    @Test
    void missingFormalReceiptStatusStopsFactGeneration() {
        stubReceipt("{\"pickListBindingItems\":[]}");
        MesProcessPoolActiveOrderCompletionReceiptDO receipt = completionReceiptMapper.selectByIdAndTenantId(8101L, 1L);
        receipt.setCompletionStatus(null);
        when(completionReceiptMapper.selectByIdAndTenantId(8101L, 1L)).thenReturn(receipt);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> adapter(Category.MATERIAL).readCatalog(batch));

        assertTrue(exception.getMessage().startsWith("SOURCE_MISSING:"));
    }

    private MesProBatchRecordExecutionDO r1SavedExecution(long id, String fieldKey, int value) {
        return new MesProBatchRecordExecutionDO().setId(id).setBatchExecutionId(batch.getId())
                .setBatchRecordVersionId(4001L).setBatchRecordReportId("report-A").setFieldAuditRevision(2L)
                .setExecutionSnapshotJson("{\"fields\":[{\"fieldKey\":\"" + fieldKey
                        + "\",\"fieldPath\":\"sheet[0].rows[1].cells[2]." + fieldKey
                        + "\",\"label\":\"" + fieldKey
                        + "\",\"rowIndex\":1,\"columnIndex\":2,\"valueType\":\"NUMBER\"}]}")
                .setCellValuesJson("[{\"rowIndex\":1,\"columnIndex\":2,\"valueType\":\"NUMBER\",\"value\":" + value + "}]");
    }

    private MesProEdhrFormalReverseTraceAdapter adapter(Category category) {
        return new MesProEdhrFormalReverseTraceAdapter(category, executionMapper, originMapper, traceLinkMapper, taskMapper,
                bpmInstanceMapper, fieldAuditMapper, eventMapper,
                pqcRecordMapper, reviewMapper, releaseApplicationMapper, releaseTransactionMapper, completionReceiptMapper);
    }

    @Test
    void auditFactsUseFrozenVersionAndTypedJsonInsteadOfDisplayText() {
        var execution = r1SavedExecution(3001L, "temperature", 32);
        when(executionMapper.selectPage(any(MesProBatchRecordExecutionPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(execution), 1L));
        var audit = new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemDO()
                .setId(11L).setExecutionId(3001L).setTenantId(1L).setFieldAuditRevision(2L)
                .setFieldKey("temperature").setFieldPath("temperature").setValueType("NUMBER")
                .setOldValueJson("30").setNewValueJson("32.00").setOldValueDisplay("30 °C").setNewValueDisplay("32 °C");
        when(fieldAuditMapper.selectListByExecutionId(3001L)).thenReturn(List.of(audit));
        var adapter = adapter(Category.FIELD);
        var after = adapter.readCatalog(batch).items().stream().filter(i -> i.getSourceView().equals("FIELD_CHANGE")
                && i.getEvidenceKey().endsWith(":AFTER")).findFirst().orElseThrow();
        assertEquals("FIELD_CHANGE:4001:temperature:AFTER", after.getEvidenceKey());
        assertEquals("number", after.getValueType());
        assertTrue(adapter.evaluate(batch, List.of(condition(after.getEvidenceKey(), "EQ", 32, "FIELD_CHANGE"))).matches().get(0).matched());
        execution.setBatchRecordVersionId(4002L);
        org.junit.jupiter.api.Assertions.assertFalse(adapter.evaluate(batch,
                List.of(condition(after.getEvidenceKey(), "EQ", 32, "FIELD_CHANGE"))).matches().get(0).matched());
    }

    @Test
    void savedFieldCannotInventAnUnknownTemplateVersion() {
        when(executionMapper.selectPage(any(MesProBatchRecordExecutionPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(r1SavedExecution(3001L, "temperature", 32)
                        .setBatchRecordVersionId(null)), 1L));
        assertTrue(assertThrows(IllegalStateException.class, () -> adapter(Category.FIELD).readCatalog(batch))
                .getMessage().startsWith("SOURCE_MISSING:"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"{\"pqcDetails\":[42]}", "{\"pqcDetails\":[{}]}",
            "{\"pqcDetails\":[{\"id\":1,\"regulationVersionId\":2,\"sampleNo\":1,\"itemCode\":\"N\",\"resultType\":\"NUMBER\",\"measuredValue\":\"3\"}]}",
            "{\"pqcDetails\":[{\"id\":1,\"routeProcessId\":3,\"regulationVersionId\":2,\"sampleNo\":1,\"itemCode\":\"N\",\"resultType\":\"NUMBER\",\"judgement\":\"PASS\"}]}"})
    void malformedOrIncompleteInspectionCannotBecomeAnEmptyOrTextFact(String snapshot) {
        stubReceipt(snapshot);
        assertThrows(IllegalStateException.class, () -> adapter(Category.INSPECTION).readCatalog(batch));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("incompleteFormalPqcResults")
    void incompleteOrInconsistentFormalPqcResultIsNotAQueryableFact(String snapshot, String expectedPrefix) {
        stubReceipt(snapshot);
        var adapter = adapter(Category.INSPECTION);
        var conditions = List.of(condition("INSPECTION:7101:3001:1:N", "EQ", "ok"));
        assertTrue(assertThrows(IllegalStateException.class, () -> adapter.readCatalog(batch))
                .getMessage().startsWith(expectedPrefix));
        assertTrue(assertThrows(IllegalStateException.class, () -> adapter.evaluate(batch, conditions))
                .getMessage().startsWith(expectedPrefix));
        assertTrue(assertThrows(IllegalStateException.class, () -> adapter.readEvidence(batch, conditions))
                .getMessage().startsWith(expectedPrefix));
    }

    private static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> incompleteFormalPqcResults() {
        return java.util.stream.Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(
                        "{\"pqcDetails\":[{\"id\":7001,\"routeProcessId\":3001,\"regulationVersionId\":7101,\"sampleNo\":1,\"itemCode\":\"N\",\"resultType\":\"TEXT\",\"measuredValue\":\"ok\",\"judgement\":\"PASS\"}]}",
                        "SOURCE_MISSING:"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "{\"pqcDetails\":[{\"id\":7001,\"routeProcessId\":3001,\"regulationVersionId\":7101,\"sampleNo\":1,\"itemCode\":\"N\",\"resultType\":\"TEXT\",\"itemResult\":\"ok\",\"judgement\":\"PASS\"}]}",
                        "SOURCE_MISSING:"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "{\"pqcDetails\":[{\"id\":7001,\"routeProcessId\":3001,\"regulationVersionId\":7101,\"sampleNo\":1,\"itemCode\":\"N\",\"resultType\":\"TEXT\",\"itemResult\":\"ok\",\"measuredValue\":\"ok\"}]}",
                        "SOURCE_MISSING:"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "{\"pqcDetails\":[{\"id\":7001,\"routeProcessId\":3001,\"regulationVersionId\":7101,\"sampleNo\":1,\"itemCode\":\"N\",\"resultType\":\"TEXT\",\"itemResult\":\"other\",\"measuredValue\":\"ok\",\"judgement\":\"PASS\"}]}",
                        "SOURCE_CONFLICT:"));
    }

    @Test
    void completeFormalPqcResultRemainsQueryable() {
        stubReceipt("{\"pqcDetails\":[{\"id\":7001,\"routeProcessId\":3001,\"regulationVersionId\":7101,\"sampleNo\":1,\"itemCode\":\"N\",\"resultType\":\"TEXT\",\"itemResult\":\"ok\",\"measuredValue\":\"ok\",\"judgement\":\"PASS\"}]}");
        var adapter = adapter(Category.INSPECTION);
        var conditions = List.of(condition("INSPECTION:7101:3001:1:N", "EQ", "ok"));
        assertEquals(1, adapter.readCatalog(batch).items().size());
        assertTrue(adapter.evaluate(batch, conditions).matches().get(0).matched());
        assertEquals(1, adapter.readEvidence(batch, conditions).items().size());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"missingId", "wrongBatch", "missingBatch"})
    void fieldExecutionIdentityMustMatchTheRequestedBatch(String defect) {
        MesProBatchRecordExecutionDO execution = r1SavedExecution(3001L, "temperature", 32);
        switch (defect) {
            case "missingId" -> execution.setId(null);
            case "wrongBatch" -> execution.setBatchExecutionId(9002L);
            case "missingBatch" -> execution.setBatchExecutionId(null);
        }
        when(executionMapper.selectPage(any(MesProBatchRecordExecutionPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(execution), 1L));
        assertTrue(assertThrows(IllegalStateException.class, () -> adapter(Category.FIELD).readCatalog(batch))
                .getMessage().startsWith("SOURCE_CONFLICT:"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"{\"pickListBindingItems\":42}", "{\"pickListBindingItems\":[{}]}",
            "{\"pickListBindingItems\":[{\"id\":1,\"materialNumber\":\"M\",\"lotNumber\":\"L\",\"sourceModifyTime\":\"bad-time\"}]}"})
    void malformedMaterialSnapshotCannotDisappear(String snapshot) {
        stubReceipt(snapshot);
        assertThrows(IllegalStateException.class, () -> adapter(Category.MATERIAL).readCatalog(batch));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"{\"deviceParameterReadings\":[{}]}",
            "{\"deviceParameterReadings\":42}", "{\"materialDetails\":42}"})
    void malformedParameterPayloadCannotBecomeNoMatch(String payload) {
        stubEvent(payload);
        assertThrows(IllegalStateException.class, () -> adapter(Category.PARAMETER).readCatalog(batch));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("invalidParameterContainers")
    void nonArrayOrMalformedParameterRowsReportConflictAtEveryReadBoundary(boolean material, String readings) {
        stubEvent(parameterPayload(material, readings));
        var adapter = adapter(Category.PARAMETER);
        var conditions = List.of(condition("PARAMETER:T", "EQ", 32));
        org.junit.jupiter.api.Assertions.assertAll(
                () -> assertTrue(assertThrows(IllegalStateException.class, () -> adapter.readCatalog(batch))
                        .getMessage().startsWith("SOURCE_CONFLICT:")),
                () -> assertTrue(assertThrows(IllegalStateException.class, () -> adapter.evaluate(batch, conditions))
                        .getMessage().startsWith("SOURCE_CONFLICT:")),
                () -> assertTrue(assertThrows(IllegalStateException.class, () -> adapter.readEvidence(batch, conditions))
                        .getMessage().startsWith("SOURCE_CONFLICT:")));
    }

    private static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> invalidParameterContainers() {
        return java.util.stream.Stream.of("{}", "{\"T\":32}", "{\"T\":null}", "{\"\":32}",
                        "{null:32}", "42", "[null]", "[42]")
                .flatMap(readings -> java.util.stream.Stream.of(false, true)
                        .map(material -> org.junit.jupiter.params.provider.Arguments.of(material, readings)));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void formalParameterArraysStillProduceMatchingFactsAndEvidence(boolean material) {
        stubEvent(parameterPayload(material, "[{\"parameterCode\":\"T\",\"value\":32}]"));
        var adapter = adapter(Category.PARAMETER);
        var catalog = adapter.readCatalog(batch);
        assertEquals("AVAILABLE", catalog.status());
        assertEquals(1, catalog.items().size());
        assertEquals("32", catalog.items().get(0).getSavedValue());
        if (material) assertEquals("17", catalog.items().get(0).getQualifiers().get("materialId"));
        var conditions = List.of(condition("PARAMETER:T", "EQ", 32)
                .setQualifiers(catalog.items().get(0).getQualifiers()));
        assertTrue(adapter.evaluate(batch, conditions).matches().get(0).matched());
        var evidence = adapter.readEvidence(batch, conditions);
        assertEquals(1, evidence.items().size());
        assertEquals("32", evidence.items().get(0).getActualValue());
    }

    @Test
    void parameterConditionRequiresCompleteFormalSemanticIdentity() {
        stubEvent("{\"deviceParameterReadings\":[{\"parameterCode\":\"T\",\"value\":32,\"unit\":\"C\"}]}");
        var adapter = adapter(Category.PARAMETER);
        var item = adapter.readCatalog(batch).items().get(0);

        var missing = condition("PARAMETER:T", "EQ", 32);
        var partial = condition("PARAMETER:T", "EQ", 32)
                .setQualifiers(Map.of("routeVersionId", item.getQualifiers().get("routeVersionId"),
                        "routeProcessId", item.getQualifiers().get("routeProcessId"),
                        "processId", item.getQualifiers().get("processId")));
        var complete = condition("PARAMETER:T", "EQ", 32).setQualifiers(item.getQualifiers());

        assertEquals("CONDITION_INVALID", adapter.validateCondition(item, missing).reasonCode());
        assertEquals("CONDITION_INVALID", adapter.validateCondition(item, partial).reasonCode());
        assertTrue(adapter.validateCondition(item, complete).valid());
        assertFalse(adapter.evaluate(batch, List.of(missing)).matches().get(0).matched());
        assertFalse(adapter.evaluate(batch, List.of(partial)).matches().get(0).matched());
        assertTrue(adapter.evaluate(batch, List.of(complete)).matches().get(0).matched());
        assertEquals("PARAMETER|T|2002|3001|4001|C", item.getSemanticIdentity());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void legalEmptyParameterArraysKeepConsistentSourceVersions(boolean material) {
        stubEvent(parameterPayload(material, "[]"));
        var adapter = adapter(Category.PARAMETER);
        var catalog = adapter.readCatalog(batch);
        var conditions = List.of(condition("PARAMETER:T", "EQ", 32));
        var evaluation = adapter.evaluate(batch, conditions);
        var evidence = adapter.readEvidence(batch, conditions);
        assertEquals("NO_RECORDED_FACT", catalog.status());
        assertTrue(catalog.items().isEmpty());
        assertEquals("COMPLETE", evaluation.status());
        org.junit.jupiter.api.Assertions.assertFalse(evaluation.matches().get(0).matched());
        assertTrue(evidence.items().isEmpty());
        assertEquals(catalog.sourceVersion(), evaluation.sourceVersion());
        assertEquals(catalog.sourceVersion(), evidence.sourceVersion());
    }

    @Test
    void materialParameterArrayWithMissingValueReportsMissingAtEveryReadBoundary() {
        stubEvent(parameterPayload(true, "[{\"parameterCode\":\"T\",\"value\":null}]"));
        var adapter = adapter(Category.PARAMETER);
        var conditions = List.of(condition("PARAMETER:T", "EQ", 32));
        assertTrue(assertThrows(IllegalStateException.class, () -> adapter.readCatalog(batch))
                .getMessage().startsWith("SOURCE_MISSING:"));
        assertTrue(assertThrows(IllegalStateException.class, () -> adapter.evaluate(batch, conditions))
                .getMessage().startsWith("SOURCE_MISSING:"));
        assertTrue(assertThrows(IllegalStateException.class, () -> adapter.readEvidence(batch, conditions))
                .getMessage().startsWith("SOURCE_MISSING:"));
    }

    private static String parameterPayload(boolean material, String readings) {
        return material ? "{\"materialDetails\":[{\"materialId\":17,\"deviceParameterReadings\":" + readings + "}]}"
                : "{\"deviceParameterReadings\":" + readings + "}";
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"version", "receiptHash", "snapshotHash", "receiptId", "missingVersion", "missingHash"})
    void receiptMustMatchTheFrozenOriginVersionAndHashes(String defect) {
        stubReceipt("{\"pickListBindingItems\":[]}");
        var origin = originMapper.selectListByBatchExecutionId(batch.getId()).get(0);
        switch (defect) {
            case "version" -> origin.setCompletionVersion(2);
            case "receiptHash" -> origin.setCompletionBackfillReceiptHash("other");
            case "snapshotHash" -> origin.setSourceSnapshotHash("other");
            case "missingVersion" -> origin.setCompletionVersion(null);
            case "missingHash" -> origin.setCompletionBackfillReceiptHash(null);
            case "receiptId" -> completionReceiptMapper.selectByIdAndTenantId(8101L, 1L).setId(8102L);
        }
        assertThrows(IllegalStateException.class, () -> adapter(Category.MATERIAL).readCatalog(batch));
    }

    @Test
    void normalAnchorCanFindAboveUpperCandidateUsingItsOwnOneSidedLimit() {
        stubEvent("{\"deviceParameterReadings\":[{\"parameterCode\":\"N\",\"deviceId\":7,\"value\":25,\"upperLimit\":30,\"parameterStatus\":\"NORMAL\"}]}");
        var adapter = adapter(Category.PARAMETER);
        var anchor = adapter.readCatalog(batch).items().get(0);
        var condition = condition(anchor.getEvidenceKey(), "OUT_OF_LIMIT", java.util.Map.of("lower", 0, "upper", 999))
                .setQualifiers(anchor.getQualifiers());
        stubEvent("{\"deviceParameterReadings\":[{\"parameterCode\":\"N\",\"deviceId\":7,\"value\":32,\"upperLimit\":31,\"parameterStatus\":\"ABOVE_UPPER\"}]}");
        assertTrue(adapter.evaluate(batch, List.of(condition)).matches().get(0).matched());
        assertEquals("<= 31", adapter.readEvidence(batch, List.of(condition)).items().get(0).getRecordedStandard());
    }

    @Test
    void parameterTextSurvivesAnExplicitNullNumericSlot() {
        stubEvent("{\"deviceParameterReadings\":[{\"parameterCode\":\"T\",\"value\":null,\"textValue\":\"032\"}]}");
        var adapter = adapter(Category.PARAMETER);
        assertEquals("032", adapter.readCatalog(batch).items().get(0).getSavedValue());
        assertTrue(adapter.evaluate(batch,
                List.of(formalParameterCondition("PARAMETER:T", "EQ", "032"))).matches().get(0).matched());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"", ",\"value\":null", ",\"value\":null,\"textValue\":null",
            ",\"textValue\":\"\"", ",\"textValue\":\"   \""})
    void incompleteParameterReadingBlocksRatherThanBecomingNoMatch(String slots) {
        stubEvent("{\"deviceParameterReadings\":[{\"parameterCode\":\"T\"" + slots + "}]}");
        var adapter = adapter(Category.PARAMETER);
        var conditions = List.of(condition("PARAMETER:T", "EQ", "x"));
        assertTrue(assertThrows(IllegalStateException.class, () -> adapter.readCatalog(batch))
                .getMessage().startsWith("SOURCE_MISSING:"));
        assertTrue(assertThrows(IllegalStateException.class, () -> adapter.evaluate(batch, conditions))
                .getMessage().startsWith("SOURCE_MISSING:"));
        assertTrue(assertThrows(IllegalStateException.class, () -> adapter.readEvidence(batch, conditions))
                .getMessage().startsWith("SOURCE_MISSING:"));
    }

    @Test
    void repeatedAllocationIdentityIsAConflictBeforeEventReads() {
        // A valid event/link permits the old implementation to silently deduplicate the duplicate row.
        var link = stubEvent("{\"deviceParameterReadings\":[{\"parameterCode\":\"N\",\"value\":32}]}");
        stubReceipt("{\"allocations\":[{\"id\":6101,\"eventId\":5001},{\"id\":6101,\"eventId\":5001}]}");
        // Strict stubs would otherwise obscure the early integrity rejection after the fix.
        org.mockito.Mockito.lenient().when(traceLinkMapper.selectListByBatchExecutionId(batch.getId())).thenReturn(List.of(link));
        org.mockito.Mockito.clearInvocations(eventMapper);
        assertTrue(assertThrows(IllegalStateException.class, () -> adapter(Category.PARAMETER).readCatalog(batch))
                .getMessage().startsWith("SOURCE_CONFLICT:"));
        org.mockito.Mockito.verifyNoInteractions(eventMapper);
    }

    @Test
    void crossWorkOrderEventUsesFormalTargetAllocationForBatchOwnership() {
        stubEventContext(1002L, 1L, 3001L, 4001L,
                "{\"allocations\":[{\"id\":6101,\"eventId\":5001,\"tenantId\":1,"
                        + "\"activeOrderId\":6001,\"workOrderId\":1001,\"routeProcessId\":3001,\"processId\":4001}]}" );

        var catalog = adapter(Category.PARAMETER).readCatalog(batch);

        assertEquals("PARAMETER:N", catalog.items().get(0).getEvidenceKey());
    }

    @Test
    void mismatchedAllocationProcessStillBlocksTheEvent() {
        stubReceipt(
                "{\"allocations\":[{\"id\":6101,\"eventId\":5001,\"tenantId\":1,"
                        + "\"activeOrderId\":6001,\"workOrderId\":1001,\"routeProcessId\":3001,\"processId\":4999}]}" );

        assertTrue(assertThrows(IllegalStateException.class,
                () -> adapter(Category.PARAMETER).readCatalog(batch)).getMessage().startsWith("SOURCE_CONFLICT:"));
    }

    @Test
    void mismatchedAllocationTargetStillBlocksTheEvent() {
        stubReceipt(
                "{\"allocations\":[{\"id\":6101,\"eventId\":5001,\"tenantId\":1,"
                        + "\"activeOrderId\":6999,\"workOrderId\":1002,\"routeProcessId\":3001,\"processId\":4001}]}" );

        assertTrue(assertThrows(IllegalStateException.class,
                () -> adapter(Category.PARAMETER).readCatalog(batch)).getMessage().startsWith("SOURCE_CONFLICT:"));
    }

    @Test
    void mismatchedEventTenantStillBlocksTheEvent() {
        stubEventContext(1002L, 2L, 3001L, 4001L,
                "{\"allocations\":[{\"id\":6101,\"eventId\":5001,\"tenantId\":1,"
                        + "\"activeOrderId\":6001,\"workOrderId\":1001,\"routeProcessId\":3001,\"processId\":4001}]}" );

        assertTrue(assertThrows(IllegalStateException.class,
                () -> adapter(Category.PARAMETER).readCatalog(batch)).getMessage().startsWith("SOURCE_CONFLICT:"));
    }

    private void stubEventContext(Long sourceWorkOrderId, Long tenantId, Long routeProcessId, Long processId,
                                  String receiptSnapshot) {
        var event = new MesProProcessPoolEventDO().setId(5001L)
                .setEventType("PRODUCTION_SUBMIT").setWorkOrderId(sourceWorkOrderId)
                .setRouteProcessId(routeProcessId).setProcessId(processId)
                .setRawPayload("{\"deviceParameterReadings\":[{\"parameterCode\":\"N\",\"value\":32}]}")
                .setServerSubmitTime(batch.getUpdateTime());
        event.setTenantId(tenantId);
        event.setRouteId(batch.getRouteId());
        frozenProductionEvents = List.of(event);
        stubReceipt(receiptSnapshot);
        String eventSnapshot = com.alibaba.fastjson.JSON.toJSONString(event);
        var link = new MesProEdhrBatchExecutionTraceLinkDO().setBatchExecutionId(batch.getId()).setTenantId(1L)
                .setLinkType("PRODUCTION_SUBMIT").setSourceEventId(5001L).setRelationStatus("BOUND")
                .setSnapshotJson(eventSnapshot)
                .setSnapshotHash(MesProEdhrBatchTraceSourceHash.calculate("PRODUCTION_SUBMIT", eventSnapshot));
        when(traceLinkMapper.selectListByBatchExecutionId(batch.getId())).thenReturn(List.of(link));
    }

    @Test
    void distinctAllocationsMayReferenceOneEventWithoutDuplicatingItsFacts() {
        stubEvent("{\"deviceParameterReadings\":[{\"parameterCode\":\"N\",\"value\":0}]}");
        stubReceipt("{\"allocations\":[{\"id\":6101,\"eventId\":5001},{\"id\":6102,\"eventId\":5001}]}");
        var adapter = adapter(Category.PARAMETER);
        assertEquals(1, adapter.readCatalog(batch).items().size());
        var condition = formalParameterCondition("PARAMETER:N", "EQ", 0);
        assertTrue(adapter.evaluate(batch, List.of(condition)).matches().get(0).matched());
        assertEquals(1, adapter.readEvidence(batch, List.of(condition)).items().size());
    }

    @Test
    void emptyAllocationsRemainALegalEmptyParameterSource() {
        stubReceipt("{\"allocations\":[]}");
        var adapter = adapter(Category.PARAMETER);
        var catalog = adapter.readCatalog(batch);
        assertEquals("NO_RECORDED_FACT", catalog.status());
        var result = adapter.evaluate(batch, List.of(condition("PARAMETER:N", "EQ", 0)));
        assertEquals("COMPLETE", result.status());
        assertEquals(catalog.sourceVersion(), result.sourceVersion());
        assertTrue(result.matches().stream().noneMatch(MesProEdhrReverseTraceSourceAdapter.ConditionMatch::matched));
        assertTrue(adapter.readEvidence(batch, List.of()).items().isEmpty());
        org.mockito.Mockito.verifyNoInteractions(eventMapper);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"null", "{\"deviceParameterReadings\":[null]}",
            "{\"materialDetails\":[null]}"})
    void nullPayloadRowsReturnFormalConflictInsteadOfUnstructuredFailure(String payload) {
        stubEvent(payload);
        assertTrue(assertThrows(IllegalStateException.class, () -> adapter(Category.PARAMETER).readCatalog(batch))
                .getMessage().startsWith("SOURCE_CONFLICT:"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"pqc", "review", "application"})
    void missingPersonReadResultCannotBeSilentlyTreatedAsNoRecords(String source) {
        stubEvent("{}");
        switch (source) {
            case "pqc" -> stubReceipt("{\"allocations\":[{\"eventId\":5001,\"routeProcessId\":3001,\"processId\":4001}],\"pqcTasks\":[{\"id\":8001,\"tenantId\":1,\"activeOrderId\":6001,\"workOrderId\":1001,"
                    + "\"routeId\":2001,\"routeVersionId\":2002,\"routeProcessId\":3001,\"processId\":4001,"
                    + "\"qaProcessId\":7101,\"submittedEventId\":5101}],\"pqcDetails\":[{\"id\":7001,"
                    + "\"sourcePqcRecordId\":5201,\"eventId\":5101,\"pqcTaskId\":8001,\"tenantId\":1,"
                    + "\"activeOrderId\":6001,\"workOrderId\":1001,\"routeId\":2001,\"routeVersionId\":2002,"
                    + "\"routeProcessId\":3001,\"processId\":4001}]}");
            case "review" -> {
                var receipt = completionReceiptMapper.selectByIdAndTenantId(8101L, 1L);
                var snapshot = com.alibaba.fastjson.JSON.parseObject(receipt.getFormalSourceSnapshotJson());
                snapshot.getJSONObject("productionFacts").remove("reviews");
                stubReceipt(snapshot.toJSONString());
            }
            case "application" -> when(releaseApplicationMapper.selectListByBatchExecutionIds(List.of(batch.getId()))).thenReturn(null);
        }
        String reason = assertThrows(IllegalStateException.class, () -> adapter(Category.PERSON).readCatalog(batch))
                .getMessage();
        assertTrue(reason.startsWith("SOURCE_MISSING:"), reason);
    }

    @Test
    void materialSelectedEquipmentIsAnActualFactWithoutParameterReadings() {
        stubEvent("{\"materialDetails\":[{\"materialId\":601,\"selectedDevices\":[{\"deviceId\":78,\"deviceCode\":\"D78\"}]}]}");
        stubReceipt("{\"allocations\":[{\"eventId\":5001}],\"pqcDetails\":[]}");
        var catalog = adapter(Category.EQUIPMENT).readCatalog(batch);
        var item = catalog.items().stream().filter(value -> value.getEvidenceKey().equals("EQUIPMENT:PRODUCTION:deviceId:78")).findFirst();
        assertTrue(item.isPresent(), "persisted material equipment must be included");
        assertTrue(item.orElseThrow().getQualifiers().isEmpty());
        assertEquals("601", adapter(Category.EQUIPMENT).readEvidence(batch, List.of(
                condition("EQUIPMENT:PRODUCTION:deviceId:78", "EQ", 78))).items().get(0).getSourceContext().get("materialId"));
    }

    @Test
    void parameterStandardsAndStatusAreDisplayMetadataNotHiddenPredicates() {
        stubEvent("{\"deviceParameterReadings\":[{\"deviceId\":77,\"parameterCode\":\"TEMP\",\"value\":32,\"unit\":\"C\",\"lowerLimit\":10,\"upperLimit\":30,\"parameterStatus\":\"ABOVE_UPPER\"}]}");
        var item = adapter(Category.PARAMETER).readCatalog(batch).items().get(0);
        assertEquals("2002", item.getQualifiers().get("routeVersionId"));
        assertEquals("3001", item.getQualifiers().get("routeProcessId"));
        assertEquals("4001", item.getQualifiers().get("processId"));
        assertEquals("C", item.getQualifiers().get("unit"));
        assertEquals("77", item.getQualifiers().get("deviceId"));
        var json = (com.alibaba.fastjson.JSONObject) com.alibaba.fastjson.JSON.toJSON(item);
        assertEquals("30", json.getString("upperLimit"));
        assertEquals("ABOVE_UPPER", json.getString("parameterStatus"));
    }

    @Test
    void sameParameterCodeWithDifferentIdentityDoesNotMatch() {
        stubEvent("{\"deviceParameterReadings\":[{\"parameterCode\":\"TEMP\",\"value\":32,\"unit\":\"C\"}]}");
        var adapter = adapter(Category.PARAMETER);
        var originalItem = adapter.readCatalog(batch).items().get(0);

        batch.setRouteVersionId(2003L);
        stubEvent("{\"deviceParameterReadings\":[{\"parameterCode\":\"TEMP\",\"value\":32,\"unit\":\"F\"}]}",
                2003L, 3002L, 4002L);

        assertTrue(!adapter.evaluate(batch, List.of(condition(originalItem.getEvidenceKey(), "EQ", 32)
                        .setQualifiers(originalItem.getQualifiers())))
                .matches().get(0).matched());
    }

    @Test
    void sameParameterIdentityMatches() {
        stubEvent("{\"deviceParameterReadings\":[{\"parameterCode\":\"TEMP\",\"value\":32,\"unit\":\"C\"}]}");
        var adapter = adapter(Category.PARAMETER);
        var item = adapter.readCatalog(batch).items().get(0);

        var condition = condition(item.getEvidenceKey(), "EQ", 32).setQualifiers(item.getQualifiers());
        assertTrue(adapter.evaluate(batch, List.of(condition)).matches().get(0).matched());
    }

    @Test
    void numericEqualityIgnoresScaleButTextDoesNot() {
        stubEvent("{\"deviceParameterReadings\":[{\"parameterCode\":\"N\",\"value\":32.00},{\"parameterCode\":\"T\",\"textValue\":\"032\"}]}");
        var adapter = adapter(Category.PARAMETER);
        assertTrue(adapter.evaluate(batch, List.of(formalParameterCondition("PARAMETER:N", "EQ", java.util.Map.of("decimal", "32"))))
                .matches().get(0).matched());
        org.junit.jupiter.api.Assertions.assertFalse(adapter.evaluate(batch,
                List.of(formalParameterCondition("PARAMETER:T", "EQ", "32"))).matches().get(0).matched());
    }

    @Test
    void outOfLimitUsesEachRecordedStandardAndRejectsReversedBounds() {
        stubEvent("{\"deviceParameterReadings\":[{\"parameterCode\":\"TEMP\",\"value\":32,\"lowerLimit\":10,\"upperLimit\":30}]}");
        var adapter = adapter(Category.PARAMETER);
        var item = adapter.readCatalog(batch).items().get(0);
        var condition = formalParameterCondition("PARAMETER:TEMP", "OUT_OF_LIMIT", "");
        assertTrue(adapter.validateCondition(item, condition).valid());
        assertTrue(adapter.evaluate(batch, List.of(condition)).matches().get(0).matched());
        assertEquals("10 ~ 30", adapter.readEvidence(batch, List.of(condition)).items().get(0).getRecordedStandard());
        org.junit.jupiter.api.Assertions.assertFalse(adapter.validateCondition(item,
                condition("PARAMETER:TEMP", "BETWEEN", java.util.Map.of("lower", 30, "upper", 10))).valid());
    }

    @Test
    void judgementComparisonUsesJudgementInsteadOfNumericMeasuredValue() {
        stubReceipt("{\"pqcDetails\":[{\"id\":7001,\"pqcTaskId\":8001,\"routeProcessId\":3001,\"regulationVersionId\":7101,\"sampleNo\":1,\"itemCode\":\"N\",\"resultType\":\"NUMBER\",\"itemResult\":\"32\",\"measuredValue\":\"32\",\"judgement\":\"NG\"}]}");
        var adapter = adapter(Category.INSPECTION);
        var item = adapter.readCatalog(batch).items().get(0);
        var condition = condition(item.getEvidenceKey(), "JUDGEMENT_EQ", "NG");
        assertTrue(adapter.validateCondition(item, condition).valid());
        assertTrue(adapter.evaluate(batch, List.of(condition)).matches().get(0).matched());
    }

    @Test
    void inspectionRowsWithSameCodeVersionAndSampleKeepSourceRowIdentity() {
        stubReceipt("{\"pqcDetails\":["
                + "{\"id\":7001,\"pqcTaskId\":8001,\"routeProcessId\":3001,\"regulationVersionId\":7101,\"sampleNo\":1,\"itemCode\":\"N\",\"resultType\":\"TEXT\",\"itemResult\":\"a\",\"measuredValue\":\"a\",\"judgement\":\"PASS\"},"
                + "{\"id\":7002,\"pqcTaskId\":8002,\"routeProcessId\":3002,\"regulationVersionId\":7101,\"sampleNo\":1,\"itemCode\":\"N\",\"resultType\":\"TEXT\",\"itemResult\":\"b\",\"measuredValue\":\"b\",\"judgement\":\"PASS\"}]}");
        var items = adapter(Category.INSPECTION).readCatalog(batch).items();
        assertEquals(2, items.stream().map(item -> item.getEvidenceKey()).distinct().count());
    }

    @Test
    void inspectionQueryUsesSemanticVersionAndProcessNotAnchorRowId() {
        String snapshot = "{\"pqcDetails\":[{\"id\":7001,\"routeProcessId\":3001,\"regulationVersionId\":7101,\"sampleNo\":1,\"itemCode\":\"N\",\"resultType\":\"TEXT\",\"itemResult\":\"ok\",\"measuredValue\":\"ok\",\"judgement\":\"PASS\"}]}";
        stubReceipt(snapshot);
        var adapter = adapter(Category.INSPECTION);
        String key = adapter.readCatalog(batch).items().get(0).getEvidenceKey();
        stubReceipt(snapshot.replace("7001", "7002"));
        assertTrue(adapter.evaluate(batch, List.of(condition(key, "EQ", "ok"))).matches().get(0).matched());
        assertEquals("pqc-aggregate-snapshot:7002", adapter.readEvidence(batch, List.of(condition(key, "EQ", "ok"))).items().get(0).getSourceRef());
    }

    @Test
    void absentRequiredSnapshotSectionIsMissingNotLegalEmpty() {
        stubReceipt("{\"pqcDetails\":42}");
        assertTrue(assertThrows(IllegalStateException.class, () -> adapter(Category.INSPECTION).readCatalog(batch))
                .getMessage().startsWith("SOURCE_MISSING:"));
    }

    @Test
    void missingLinkedEventCannotBecomeNoMatch() {
        stubReceipt("{\"allocations\":[{\"eventId\":5001}]}");
        when(traceLinkMapper.selectListByBatchExecutionId(batch.getId())).thenReturn(List.of(
                new MesProEdhrBatchExecutionTraceLinkDO().setBatchExecutionId(batch.getId()).setTenantId(1L)
                        .setLinkType("PRODUCTION_SUBMIT").setSourceEventId(5001L).setRelationStatus("BOUND")
                        .setSnapshotJson("{}").setSnapshotHash(MesProEdhrBatchTraceSourceHash.calculate("PRODUCTION_SUBMIT", "{}"))));
        assertTrue(assertThrows(IllegalStateException.class, () -> adapter(Category.PARAMETER).readCatalog(batch))
                .getMessage().startsWith("SOURCE_MISSING:"));
    }

    @Test
    void tamperedProductionTraceHashBlocksEvenWhenEventExists() {
        var link = stubEvent("{\"deviceParameterReadings\":[{\"parameterCode\":\"N\",\"value\":1}]}");
        link.setSnapshotHash("tampered");
        org.mockito.Mockito.reset(eventMapper);
        assertTrue(assertThrows(IllegalStateException.class, () -> adapter(Category.PARAMETER).readCatalog(batch))
                .getMessage().startsWith("SOURCE_CONFLICT:"));
        org.mockito.Mockito.verifyNoInteractions(eventMapper);
    }

    @Test
    void applicationActorIsFlow07AndTransactionActorIsFlow09() {
        stubReceipt("{\"allocations\":[]}");
        when(releaseApplicationMapper.selectListByBatchExecutionIds(List.of(batch.getId()))).thenReturn(List.of(
                new MesProcessPoolActiveOrderReleaseApplicationDO().setId(5301L).setAppliedBy(42L)));
        when(releaseTransactionMapper.selectByBatchExecutionId(batch.getId())).thenReturn(
                new MesProEdhrReleaseTransactionDO().setId(5401L).setSubmittedBy(43L));
        var evidence = adapter(Category.PERSON).readEvidence(batch,
                List.of(condition("PERSON:SYSTEM_USER:PQC_RELEASE_APPLICANT:42", "EQ", 42)));
        assertEquals(1, evidence.items().size());
        assertEquals("FLOW-07", evidence.items().get(0).getSourceStage());
        assertEquals("FLOW-09", adapter(Category.PERSON).readEvidence(batch,
                List.of(condition("PERSON:SYSTEM_USER:LISTING_RELEASE_SUBMIT:43", "EQ", 43))).items().get(0).getSourceStage());
    }

    private MesProEdhrBatchExecutionTraceLinkDO stubEvent(String payload) {
        return stubEvent(payload, batch.getRouteVersionId(), 3001L, 4001L);
    }

    private MesProEdhrBatchExecutionTraceLinkDO stubEvent(String payload, Long routeVersionId,
                                                          Long routeProcessId, Long processId) {
        var event = new MesProProcessPoolEventDO().setId(5001L).setEventType("PRODUCTION_SUBMIT")
                .setWorkOrderId(batch.getWorkOrderId()).setRouteId(batch.getRouteId()).setRouteProcessId(routeProcessId).setProcessId(processId)
                .setRawPayload(payload).setServerSubmitTime(batch.getUpdateTime());
        event.setTenantId(1L);
        frozenProductionEvents = List.of(event);
        stubReceipt("{\"allocations\":[{\"eventId\":5001,\"routeProcessId\":" + routeProcessId
                + ",\"processId\":" + processId + "}]}", routeVersionId, routeProcessId, processId);
        var snapshot = com.alibaba.fastjson.JSON.toJSONString(event);
        var link = new MesProEdhrBatchExecutionTraceLinkDO().setBatchExecutionId(batch.getId()).setTenantId(1L)
                .setLinkType("PRODUCTION_SUBMIT").setSourceEventId(5001L).setRelationStatus("BOUND")
                .setSnapshotJson(snapshot).setSnapshotHash(MesProEdhrBatchTraceSourceHash.calculate("PRODUCTION_SUBMIT", snapshot));
        when(traceLinkMapper.selectListByBatchExecutionId(batch.getId())).thenReturn(List.of(link));
        return link;
    }

    @Test
    void producerSummaryTraceResolvesEventsFromCanonicalReceiptAllocations() {
        stubEvent("{\"deviceParameterReadings\":[{\"parameterCode\":\"N\",\"value\":32}]}");
        String snapshot = "{\"allocations\":[{\"eventId\":5001}],\"pqcDetails\":[]}";
        stubReceipt(snapshot);
        var receipt = formalReceipt(snapshot);
        var witness = new com.alibaba.fastjson.JSONObject();
        witness.put("sourceType", "PRODUCTION_SUBMIT");
        witness.put("sourceId", receipt.getBatchRecordId());
        witness.put("witnessHash", receipt.getReceiptHash());
        var link = new MesProEdhrBatchExecutionTraceLinkDO().setBatchExecutionId(batch.getId()).setTenantId(1L)
                .setLinkType("PRODUCTION_SUBMIT").setSourceObjectId(receipt.getBatchRecordId()).setRelationStatus("BOUND")
                .setSnapshotJson(witness.toJSONString())
                .setSnapshotHash(MesProEdhrBatchTraceSourceHash.calculate("PRODUCTION_SUBMIT", witness.toJSONString()));
        when(traceLinkMapper.selectListByBatchExecutionId(batch.getId())).thenReturn(List.of(link));
        assertTrue(adapter(Category.PARAMETER).evaluate(batch,
                List.of(formalParameterCondition("PARAMETER:N", "EQ", 32))).matches().get(0).matched());
    }

    private void stubReceipt(String snapshot) {
        stubReceipt(snapshot, batch.getRouteVersionId());
    }

    private void stubReceipt(String snapshot, Long routeVersionId) {
        stubReceipt(snapshot, routeVersionId, 3001L, 4001L);
    }

    private void stubReceipt(String snapshot, Long routeVersionId, Long routeProcessId, Long processId) {
        var receipt = formalReceipt(snapshot, routeVersionId, routeProcessId, processId);
        when(originMapper.selectListByBatchExecutionId(batch.getId())).thenReturn(List.of(
                new MesProEdhrBatchExecutionOriginDO().setBatchExecutionId(batch.getId()).setTenantId(1L)
                        .setActiveOrderId(6001L).setWorkOrderId(batch.getWorkOrderId()).setCompletionBackfillReceiptId(8101L)
                        .setCompletionVersion(receipt.getCompletedVersion()).setCompletionBackfillReceiptHash(receipt.getReceiptHash())
                        .setSourceSnapshotHash(receipt.getSourceSnapshotHash())));
        when(completionReceiptMapper.selectByIdAndTenantId(8101L, 1L)).thenReturn(receipt);
    }

    private MesProcessPoolActiveOrderCompletionReceiptDO formalReceipt(String snapshot) {
        return formalReceipt(snapshot, batch.getRouteVersionId());
    }

    private MesProcessPoolActiveOrderCompletionReceiptDO formalReceipt(String snapshot, Long routeVersionId) {
        return formalReceipt(snapshot, routeVersionId, 3001L, 4001L);
    }

    private MesProcessPoolActiveOrderCompletionReceiptDO formalReceipt(String snapshot, Long routeVersionId,
                                                                         Long routeProcessId, Long processId) {
        var parsed = com.alibaba.fastjson.JSON.parseObject(snapshot);
        if (!parsed.containsKey("allocations")) parsed.put("allocations", List.of());
        var activeOrderBinding = new java.util.LinkedHashMap<String, Object>(java.util.Map.of(
                "id", 6001L, "tenantId", 1L, "workOrderId", batch.getWorkOrderId(), "routeId", batch.getRouteId()));
        if (routeVersionId != null) activeOrderBinding.put("routeVersionId", routeVersionId);
        parsed.putIfAbsent("activeOrderBinding", activeOrderBinding);
        parsed.putIfAbsent("workOrderBinding", new java.util.LinkedHashMap<>(java.util.Map.of(
                "id", batch.getWorkOrderId())));
        if (!parsed.containsKey("pqcTasks")) parsed.put("pqcTasks", List.of());
        if (!parsed.containsKey("pqcDetails")) parsed.put("pqcDetails", List.of());
        parsed.putIfAbsent("snapshots", List.of(new java.util.LinkedHashMap<>(java.util.Map.of(
                "routeProcessId", routeProcessId, "processId", processId))));
        if (parsed.get("allocations") instanceof java.util.Collection<?> rows) {
            for (Object raw : rows) {
                if (raw instanceof com.alibaba.fastjson.JSONObject allocation) {
                    allocation.putIfAbsent("tenantId", 1L);
                    allocation.putIfAbsent("activeOrderId", 6001L);
                    allocation.putIfAbsent("workOrderId", batch.getWorkOrderId());
                    allocation.putIfAbsent("routeProcessId", routeProcessId);
                    allocation.putIfAbsent("processId", processId);
                    allocation.putIfAbsent("reviewId", allocation.getLong("eventId") + 200L);
                    allocation.putIfAbsent("leaderUserId", 31L);
                }
            }
        }
        if (!parsed.containsKey("productionFacts")) {
            var eventRows = new java.util.ArrayList<com.alibaba.fastjson.JSONObject>();
            var reviewRows = new java.util.ArrayList<java.util.Map<String, Object>>();
            for (var event : frozenProductionEvents) {
                var frozen = com.alibaba.fastjson.JSON.parseObject(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(event));
                if (event.getRawPayload() != null) frozen.put("payloadContentHash", DigestUtil.sha256Hex(event.getRawPayload()));
                eventRows.add(frozen);
                long reviewId = event.getId() + 200;
                reviewRows.add(java.util.Map.of("id", reviewId, "eventId", event.getId(), "tenantId", 1L,
                        "leaderUserId", 31L, "leaderType", "PRODUCTION", "reviewStatus", "APPROVED",
                        "reviewedAt", batch.getUpdateTime(), "reviewSignatureId", reviewId + 1,
                        "reviewSignatureUserId", 31L, "reviewSignatureSnapshotJson",
                        cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(java.util.Map.of(
                                "signatureId", reviewId + 1, "actorId", 31L, "processPoolEventId", event.getId(),
                                "actionType", "TEAM_LEADER_REVIEW", "eventType", "PRODUCTION_SUBMIT",
                                "leaderType", "PRODUCTION", "reviewStatus", "APPROVED"))));
            }
            parsed.put("productionFacts", java.util.Map.of("formatVersion", 1, "events", eventRows, "reviews", reviewRows));
        }
        snapshot = cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(parsed);
        MesProcessPoolActiveOrderCompletionReceiptDO receipt = new MesProcessPoolActiveOrderCompletionReceiptDO()
                .setId(8101L).setActiveOrderId(6001L).setWorkOrderId(batch.getWorkOrderId())
                .setRouteId(batch.getRouteId()).setRouteVersionId(routeVersionId)
                .setFormalSourceSnapshotJson(snapshot).setLossConditionFactsJson("[]")
                .setReceiptStatus(MesProcessPoolActiveOrderCompletionReceiptDO.RECEIPT_STATUS_BACKFILL_SUCCEEDED)
                .setCompletionStatus(MesProcessPoolActiveOrderCompletionReceiptDO.STATUS_SUCCESS)
                .setBatchRecordStatus(MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS)
                .setProcessInspectionStatus(MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS)
                .setCompletedVersion(1).setBatchRecordId(8201L).setProcessInspectionId(8301L);
        receipt.setTenantId(1L);
        receipt.setSourceSnapshotHash(DigestUtil.sha256Hex(
                DigestUtil.sha256Hex(snapshot) + "|" + receipt.getLossConditionFactsJson()));
        receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
        return receipt;
    }

    private void bindStubbedEvents(List<Long> ids) {
        var events = eventMapper.selectProductionSubmitsByIds(ids);
        frozenProductionEvents = events;
        for (var event : events) {
            event.setTenantId(1L);
            event.setWorkOrderId(batch.getWorkOrderId()).setRouteId(batch.getRouteId()).setRouteProcessId(3001L).setProcessId(4001L)
                    .setEventType("PRODUCTION_SUBMIT");
        }
        var links = traceLinkMapper.selectListByBatchExecutionId(batch.getId());
        for (var link : links) {
            var event = events.stream().filter(row -> row.getId().equals(link.getSourceEventId())).findFirst().orElseThrow();
            String snapshot = com.alibaba.fastjson.JSON.toJSONString(event);
            link.setTenantId(1L).setRelationStatus("BOUND").setSnapshotJson(snapshot)
                    .setSnapshotHash(MesProEdhrBatchTraceSourceHash.calculate("PRODUCTION_SUBMIT", snapshot));
        }
        stubReceipt(com.alibaba.fastjson.JSON.toJSONString(java.util.Map.of("pqcDetails", List.of(), "allocations",
                ids.stream().map(id -> java.util.Map.of("eventId", id)).toList())));
    }

    private Condition condition(String evidenceKey, String operator, Object value) {
        return condition(evidenceKey, operator, value, "RECORDED");
    }

    private Condition condition(String evidenceKey, String operator, Object value, String sourceView) {
        return new Condition().setConditionId("C1").setEvidenceKey(evidenceKey).setSourceView(sourceView)
                .setOperator(operator).setValue(value);
    }

    private Condition formalParameterCondition(String evidenceKey, String operator, Object value) {
        var item = adapter(Category.PARAMETER).readCatalog(batch).items().stream()
                .filter(candidate -> evidenceKey.equals(candidate.getEvidenceKey())).findFirst().orElseThrow();
        return condition(evidenceKey, operator, value).setQualifiers(item.getQualifiers());
    }
}
