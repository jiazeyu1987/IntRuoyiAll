package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink.MesProductionPickListSourceService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionSnapshotResolver;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteSnapshotCanonicalizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MesProEdhrCompletionSourceBackfillServiceTest {
    private final MesProEdhrBatchExecutionOriginMapper origins = mock(MesProEdhrBatchExecutionOriginMapper.class);
    private final MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort receipts = mock(MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort.class);
    private final MesProRouteVersionMapper routeVersions = mock(MesProRouteVersionMapper.class);
    private final MesProRouteSnapshotCanonicalizer canonicalizer = new MesProRouteSnapshotCanonicalizer();
    private final MesProRouteVersionSnapshotResolver routes = new MesProRouteVersionSnapshotResolver(routeVersions, canonicalizer);
    private final MesProBatchRecordCellLinkRuleMapper ruleMapper = mock(MesProBatchRecordCellLinkRuleMapper.class);
    private final MesProWorkOrderMapper workOrders = mock(MesProWorkOrderMapper.class);
    private final MesProBatchRecordExecutionMapper executions = mock(MesProBatchRecordExecutionMapper.class);
    private final MesProBatchRecordExecutionFieldAuditBatchMapper audits = mock(MesProBatchRecordExecutionFieldAuditBatchMapper.class);
    private final MesProBatchRecordExecutionFieldAuditItemMapper items = mock(MesProBatchRecordExecutionFieldAuditItemMapper.class);
    private final MesProBatchRecordExecutionFieldAuditService auditService = mock(MesProBatchRecordExecutionFieldAuditService.class);
    private final MesProBatchRecordExecutionService singleExecution = mock(MesProBatchRecordExecutionService.class);
    private final MesProductionPickListSourceService pickLists = mock(MesProductionPickListSourceService.class);
    private final MesProEdhrBatchExecutionTaskMapper taskMapper = mock(MesProEdhrBatchExecutionTaskMapper.class);
    private final MesProEdhrOperationAuditEventMapper operationAudits = mock(MesProEdhrOperationAuditEventMapper.class);
    private MesProEdhrCompletionSourceBackfillService service;
    private MesTeamLeaderBatchRecordBackfillService backfill;
    private MesProEdhrBatchExecutionDO batch;
    private MesProEdhrBatchExecutionTaskDO task;
    private MesFlow6CompletionBackfillReceipt receipt;
    private MesProBatchRecordExecutionDO execution;
    private MesProBatchRecordExecutionFieldAuditBatchDO persistedAudit;
    private List<MesProBatchRecordExecutionFieldAuditItemDO> persistedItems;
    private List<MesProBatchRecordCellLinkRuleDO> rules;
    private Map<String, Object> source;
    private MesProProcessPoolEventDO event;
    private MesProcessPoolSubmissionReviewDO review;
    private MesProRouteVersionDO publishedVersion;
    private Long versionTenant = 1L;
    private List<MesProEdhrOperationAuditEventDO> persistedOpens = List.of();

    @BeforeEach
    void setup() {
        TenantContextHolder.setTenantId(1L);
        String routeJson = """
                {"routeId":40,"configSnapshots":{"flowGraph":{"nodes":[{"routeProcessId":15,"processId":50}]},
                "batchUseConfigs":[{"routeProcessId":15,"useType":"BATCH",
                "batchRecordReports":[{"routeBindingId":70,"batchRecordReportId":"BR-15",
                "batchRecordDefinitionId":55,"batchRecordVersionId":173,"instanceScope":"PROCESS",
                "recordCategory":"BATCH_RECORD","formSlotType":"MAIN"}]}]}}
                """;
        batch = new MesProEdhrBatchExecutionDO().setId(100L).setTenantId(1L).setWorkOrderId(20L)
                .setProductId(30L).setRouteId(40L).setRouteVersionId(749L).setBatchCode("B-20")
                .setRouteSnapshotJson(routeJson);
        task = new MesProEdhrBatchExecutionTaskDO().setId(200L).setBatchExecutionId(100L)
                .setRouteProcessId(15L).setProcessId(50L).setRouteBindingId(70L).setInstanceScope("PROCESS")
                .setRecordCategory("BATCH_RECORD").setBatchRecordReportId("BR-15")
                .setFormSlotType("MAIN")
                .setBatchRecordDefinitionId(55L).setBatchRecordVersionId(173L).setExecutionId(300L);
        rules = new ArrayList<>();
        String[] fields = {"outputQuantity", "lossQuantity", "totalQuantity", "signatureUserId", "reviewSignatureUserId",
                "operatorUserId", "actualEmployeeId"};
        List<Map<String, Object>> snapshotFields = new ArrayList<>();
        for (int index = 0; index < fields.length; index++) {
            MesProBatchRecordCellLinkRuleDO rule = new MesProBatchRecordCellLinkRuleDO();
            rule.setId((long) index + 1);
            rule.setScopeType("ROUTE_VERSION"); rule.setScopeId(173L); rule.setRouteProcessId(15L);
            rule.setSourceType("PROCESS_POOL_REPORT"); rule.setSourceFieldCode(fields[index]);
            rule.setTargetReportId("BR-15"); rule.setTargetRowIndex(index); rule.setTargetColumnIndex(0);
            rule.setTargetCellKey("R" + index + "C0"); rule.setTargetValueType("NUMBER");
            rule.setEnabled(true); rules.add(rule);
            snapshotFields.add(Map.of("fieldPath", "report." + fields[index], "fieldKey", fields[index],
                    "rowIndex", index, "columnIndex", 0, "valueType", "NUMBER"));
        }
        execution = new MesProBatchRecordExecutionDO().setId(300L).setBatchExecutionId(100L).setTaskId(200L)
                .setWorkOrderId(20L).setRouteProcessId(15L).setBatchRecordReportId("BR-15")
                .setRouteId(40L).setBatchCode("B-20").setRouteBindingId(70L).setInstanceScope("PROCESS")
                .setBatchRecordDefinitionId(55L).setBatchRecordVersionId(173L).setRecordCategory("BATCH_RECORD")
                .setStatus(0).setExecutionSnapshotJson(JsonUtils.toJsonString(Map.of("fields", snapshotFields)))
                .setCellValuesJson("[]").setCellValuesHash("before").setFieldAuditRevision(0L).setFieldAuditHeadHash("genesis");
        event = new MesProProcessPoolEventDO().setId(500L).setEventType("PRODUCTION_SUBMIT")
                .setWorkOrderId(20L).setRouteId(40L).setRouteProcessId(15L).setProcessId(50L)
                .setActualEmployeeId(342L).setSignatureId(600L).setSignatureUserId(342L).setDeviceAccountId(341L)
                .setServerSubmitTime(LocalDateTime.of(2026, 10, 9, 10, 0)).setRawPayload("""
                {"outputQuantity":10,"lossQuantity":2,"deviceAccountUserId":341,"operatorUserId":999,
                "actualEmployeeId":342,"signatureEmployeeId":342,"signatureIdentityDomain":"MES_EMPLOYEE"}
                """);
        event.setTenantId(1L);
        MesProcessPoolReportAllocationDO allocation = MesProcessPoolReportAllocationDO.builder().id(700L)
                .eventId(500L).reviewId(800L).workOrderId(20L).routeProcessId(15L).processId(50L)
                .activeOrderId(437L).lifecycleStatus("CURRENT")
                .allocatedQuantity(new BigDecimal("10")).confirmedAt(event.getServerSubmitTime()).build();
        allocation.setTenantId(1L);
        review = MesProcessPoolSubmissionReviewDO.builder().id(800L).eventId(500L)
                .leaderType("PRODUCTION").leaderUserId(341L).reviewStatus("APPROVED")
                .reviewSignatureId(900L).reviewSignatureUserId(341L).reviewedAt(event.getServerSubmitTime()).build();
        review.setTenantId(1L);
        source = new LinkedHashMap<>();
        source.put("allocations", List.of(allocation));
        source.put("completions", List.of(MesProcessPoolOrderProcessCompletionDO.builder().workOrderId(20L)
                .routeProcessId(15L).processId(50L).completionStatus("COMPLETED").lastEventId(500L).build()));
        source.put("productionFacts", Map.of("events", List.of(event), "reviews", List.of(review)));
        // Two independent material documents never require selecting a representative for pure production rules.
        source.put("pickListBindings", List.of(Map.of("id", 901L), Map.of("id", 902L)));
        receipt = new MesFlow6CompletionBackfillReceipt().setReceiptId(400L).setTenantId(1L).setActiveOrderId(437L)
                .setWorkOrderId(20L).setBatchCode("B-20").setRouteId(40L).setRouteVersionId(749L)
                .setSourceSnapshotHash("a".repeat(64)).setReceiptHash("b".repeat(64));
        when(origins.selectListByBatchExecutionId(100L)).thenAnswer(invocation -> List.of(new MesProEdhrBatchExecutionOriginDO()
                .setId(401L).setTenantId(1L).setBatchExecutionId(100L).setWorkOrderId(20L)
                .setEntryType("ACTIVE_ORDER_COMPLETION").setActiveOrderId(437L).setCompletionBackfillReceiptId(400L)
                .setSourceSnapshotHash(frozenReceipt().getSourceSnapshotHash()).setCompletionBackfillReceiptHash(receipt.getReceiptHash())));
        when(receipts.getByReceiptId(400L, 1L)).thenAnswer(invocation -> frozenReceipt());
        publishedVersion = new MesProRouteVersionDO();
        publishedVersion.setId(749L); publishedVersion.setRouteId(40L);
        publishedVersion.setActive(true); publishedVersion.setLifecycleStatus("ACTIVE");
        publishedVersion.setRouteSnapshotJson(routeJson); publishedVersion.setDeleted(false);
        publishedVersion.setRouteSnapshotSha256(canonicalizer.sha256(routeJson));
        publishedVersion.setRouteSnapshotFormatVersion(MesProRouteSnapshotCanonicalizer.FORMAT_VERSION);
        when(routeVersions.selectById(749L)).thenReturn(publishedVersion);
        when(routeVersions.selectByMap(any())).thenAnswer(invocation -> {
            Map<?, ?> criteria = invocation.getArgument(0);
            assertEquals(Map.of("id", 749L, "tenant_id", 1L), criteria);
            return versionTenant.equals(criteria.get("tenant_id")) ? List.of(publishedVersion) : List.of();
        });
        when(operationAudits.selectByMap(any())).thenAnswer(invocation -> {
            assertEquals(Map.of("tenant_id", 1L, "batch_execution_id", 100L,
                    "operation_type", "OPEN", "result_status", "SUCCESS"), invocation.getArgument(0));
            return persistedOpens;
        });
        when(routeVersions.selectSnapshotIdentityEnforcementReady()).thenReturn(1);
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 173L, "BR-15")).thenAnswer(i -> rules);
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 173L, "BR-15", 15L))
                .thenAnswer(i -> rules.stream().filter(rule -> Objects.equals(15L, rule.getRouteProcessId())).toList());
        when(executions.selectById(300L)).thenReturn(execution);
        when(workOrders.selectById(20L)).thenReturn(new MesProWorkOrderDO().setId(20L).setProductId(30L).setCode("WO20"));
        when(singleExecution.openOrCreateByContext(any())).thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(300L));
        when(singleExecution.openOrCreateForCompletionSource(any())).thenReturn(new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(300L));
        when(taskMapper.updateById(any(MesProEdhrBatchExecutionTaskDO.class))).thenReturn(1);
        when(audits.selectListByExecutionId(300L)).thenAnswer(i -> persistedAudit == null ? List.of() : List.of(persistedAudit));
        when(items.selectListByBatchId(1000L)).thenAnswer(i -> persistedItems);
        when(auditService.verifyChain(300L)).thenReturn(MesProBatchRecordExecutionFieldAuditHashVerification.valid(
                "c".repeat(64), "c".repeat(64), 1, 7));
        when(auditService.saveSystemCellLinkChanges(any())).thenAnswer(invocation -> {
            MesProBatchRecordExecutionFieldAuditSaveChangesCommand command = invocation.getArgument(0);
            persistedAudit = new MesProBatchRecordExecutionFieldAuditBatchDO().setId(1000L).setTenantId(1L)
                    .setExecutionId(300L).setIdempotencyKey(command.getIdempotencyKey()).setReasonText(command.getReasonText())
                    .setFieldCount(command.getChanges().size()).setNewHeadHash("c".repeat(64));
            persistedItems = command.getChanges().stream().map(change -> new MesProBatchRecordExecutionFieldAuditItemDO()
                    .setAuditBatchId(1000L).setExecutionId(300L).setRowIndex(change.getRowIndex())
                    .setColumnIndex(change.getColumnIndex()).setFieldPath(change.getFieldPath()).setFieldKey(change.getFieldKey())
                    .setReasonText(command.getReasonText()).setNewValueJson(JsonUtils.toJsonString(change.getNewValueJson()))).toList();
            return new MesProBatchRecordExecutionFieldAuditSaveResult().setAuditBatchId(1000L)
                    .setChangedFieldCount(command.getChanges().size()).setFieldAuditHeadHash("c".repeat(64));
        });
        backfill = spy(new MesTeamLeaderBatchRecordBackfillServiceImpl(mock(MesProRouteFlowProcessBatchRecordMapper.class),
                singleExecution, executions, ruleMapper, auditService, pickLists));
        service = new MesProEdhrCompletionSourceBackfillService(origins, receipts, routes, ruleMapper, workOrders,
                executions, audits, items, auditService, backfill, taskMapper, routeVersions, canonicalizer, operationAudits);
    }

    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    private MesFlow6CompletionBackfillReceipt frozenReceipt() {
        receipt.setFormalSourceSnapshotJson(JsonUtils.toJsonString(source)).setLossConditionFactsJson("[]");
        receipt.setSourceSnapshotHash(MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer
                .sourceSnapshotHash(receipt.getFormalSourceSnapshotJson(), receipt.getLossConditionFactsJson()));
        return receipt;
    }

    private MesBatchExecutionProvisionCommand provisionCommand() {
        frozenReceipt();
        task.setMaterialSourceSnapshotHash(receipt.getSourceSnapshotHash());
        return new MesBatchExecutionProvisionCommand().setEntryType("ACTIVE_ORDER_COMPLETION")
                .setTenantId(1L).setSourceCredentialId("400").setCompletionBackfillReceiptId("400")
                .setCompletionBackfillReceiptHash(receipt.getReceiptHash()).setSourceSnapshotHash(receipt.getSourceSnapshotHash())
                .setActiveOrderId(receipt.getActiveOrderId()).setWorkOrderId(20L).setRouteId(40L)
                .setRouteVersionId(749L).setBatchCode("B-20");
    }

    @Test void actualTaskProducerKeepsProductInformationMemberOutsideDirectQuantityFanout() throws Exception {
        quantityRules();
        var producer = new MesProEdhrBatchExecutionServiceImpl();
        List<MesProEdhrBatchExecutionTaskDO> generated = generatedTasksWithProductInfoMember(producer);
        var generatedAuditItems = configureGeneratedQuantityEvidence(generated);
        var processTask = generated.get(0);
        var memberTask = generated.get(1);
        assertEquals(70L, processTask.getRouteBindingId());
        assertEquals(processTask.getRouteBindingId(), memberTask.getRouteBindingId());
        assertEquals(processTask.getRouteProcessId(), memberTask.getRouteProcessId());
        assertEquals(55L, memberTask.getBatchRecordDefinitionId());
        assertEquals(173L, memberTask.getBatchRecordVersionId());
        assertEquals("MAIN", memberTask.getFormSlotType());
        assertEquals("PROCESS", memberTask.getInstanceScope());
        assertEquals("BATCH_RECORD", memberTask.getRecordCategory());
        assertEquals("BR-PRODUCT-INFO", memberTask.getBatchRecordReportId());
        assertEquals(80, memberTask.getBatchRecordSort());
        ReflectionTestUtils.setField(producer, "completionSourceBackfillService", service);
        ReflectionTestUtils.setField(producer, "batchTaskMapper", taskMapper);
        when(taskMapper.selectByIdForUpdate(200L)).thenReturn(processTask);
        when(taskMapper.selectByIdForUpdate(201L)).thenReturn(memberTask);
        for (var generatedTask : generated.subList(2, generated.size())) {
            when(taskMapper.selectByIdForUpdate(generatedTask.getId())).thenReturn(generatedTask);
        }
        for (String defect : List.of("missing", "duplicate", "report", "binding", "routeProcess", "definition",
                "version", "sourceHash", "scope", "category", "slot", "process", "batch", "taskId",
                "lockedMissing", "lockedIdentity")) {
            List<MesProEdhrBatchExecutionTaskDO> candidates = new ArrayList<>(generated.stream()
                    .map(row -> JsonUtils.parseObject(JsonUtils.toJsonString(row), MesProEdhrBatchExecutionTaskDO.class)).toList());
            var last = candidates.get(candidates.size() - 1);
            switch (defect) {
                case "missing" -> candidates.remove(last);
                case "duplicate" -> candidates.add(last);
                case "report" -> last.setBatchRecordReportId("WRONG");
                case "binding" -> last.setRouteBindingId(999L);
                case "routeProcess" -> last.setRouteProcessId(999L);
                case "definition" -> last.setBatchRecordDefinitionId(99L);
                case "version" -> last.setBatchRecordVersionId(99L);
                case "sourceHash" -> last.setMaterialSourceSnapshotHash("wrong");
                case "scope" -> last.setInstanceScope("BATCH");
                case "category" -> last.setRecordCategory("OTHER");
                case "slot" -> last.setFormSlotType("PROCESS_INSPECTION");
                case "process" -> last.setProcessId(99L);
                case "batch" -> last.setBatchExecutionId(99L);
                case "taskId" -> last.setId(candidates.get(0).getId());
                case "lockedMissing", "lockedIdentity" -> { }
                default -> throw new AssertionError(defect);
            }
            for (var candidate : candidates) {
                when(taskMapper.selectByIdForUpdate(candidate.getId())).thenReturn(candidate);
            }
            if ("lockedMissing".equals(defect)) when(taskMapper.selectByIdForUpdate(last.getId())).thenReturn(null);
            if ("lockedIdentity".equals(defect)) when(taskMapper.selectByIdForUpdate(last.getId()))
                    .thenReturn(new MesProEdhrBatchExecutionTaskDO().setId(999L));
            assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(producer,
                    "materializeProvisionedCompletionSources", batch, candidates, provisionCommand(), 6000L), defect);
            verifyNoInteractions(backfill, singleExecution, auditService);
            assertNull(persistedAudit, defect);
        }
        for (var generatedTask : generated) {
            when(taskMapper.selectByIdForUpdate(generatedTask.getId())).thenReturn(generatedTask);
        }
        String frozenSnapshot = batch.getRouteSnapshotJson();
        for (String defect : List.of("missingBinding", "duplicateBinding", "malformedReports", "missingConfigs")) {
            var snapshot = (com.fasterxml.jackson.databind.node.ObjectNode) JsonUtils.parseTree(frozenSnapshot);
            var config = (com.fasterxml.jackson.databind.node.ObjectNode) snapshot.path("configSnapshots")
                    .path("batchUseConfigs").get(0);
            var reports = (com.fasterxml.jackson.databind.node.ArrayNode) config.path("batchRecordReports");
            switch (defect) {
                case "missingBinding" -> reports.remove(reports.size() - 1);
                case "duplicateBinding" -> reports.add(reports.get(0).deepCopy());
                case "malformedReports" -> config.put("batchRecordReports", "invalid");
                case "missingConfigs" -> ((com.fasterxml.jackson.databind.node.ObjectNode) snapshot.path("configSnapshots"))
                        .remove("batchUseConfigs");
                default -> throw new AssertionError(defect);
            }
            batch.setRouteSnapshotJson(snapshot.toString());
            assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(producer,
                    "materializeProvisionedCompletionSources", batch, generated, provisionCommand(), 6000L), defect);
            verifyNoInteractions(backfill, singleExecution, auditService);
            assertNull(persistedAudit, defect);
        }
        for (String optionalReports : List.of("absent", "null", "empty")) {
            var snapshot = (com.fasterxml.jackson.databind.node.ObjectNode) JsonUtils.parseTree(frozenSnapshot);
            var config = (com.fasterxml.jackson.databind.node.ObjectNode) snapshot.path("configSnapshots")
                    .path("batchUseConfigs").get(0);
            var slotBinding = config.path("batchRecordReports").get(0).deepCopy();
            ((com.fasterxml.jackson.databind.node.ObjectNode) slotBinding).put("formSlotType", "PROCESS_INSPECTION");
            config.putArray("formBindings").add(slotBinding);
            switch (optionalReports) {
                case "absent" -> config.remove("batchRecordReports");
                case "null" -> config.putNull("batchRecordReports");
                case "empty" -> config.putArray("batchRecordReports");
                default -> throw new AssertionError(optionalReports);
            }
            batch.setRouteSnapshotJson(snapshot.toString());
            var slotTask = JsonUtils.parseObject(JsonUtils.toJsonString(processTask), MesProEdhrBatchExecutionTaskDO.class)
                    .setFormSlotType("PROCESS_INSPECTION");
            String noQuantityMetadata = ReflectionTestUtils.invokeMethod(producer, "materializeProvisionedCompletionSources",
                    batch, List.of(slotTask), provisionCommand(), 6000L);
            assertEquals(0, JsonUtils.parseTree(noQuantityMetadata).path("completionSourceMaterializations").size(), optionalReports);
            verifyNoInteractions(backfill, singleExecution, auditService);
            assertNull(persistedAudit, optionalReports);
        }
        batch.setRouteSnapshotJson(frozenSnapshot);
        clearInvocations(taskMapper);
        String metadata = ReflectionTestUtils.invokeMethod(producer, "materializeProvisionedCompletionSources",
                batch, generated, provisionCommand(), 6000L);
        var materializations = JsonUtils.parseTree(metadata).path("completionSourceMaterializations");
        assertEquals(15, materializations.size());
        assertEquals(200L, materializations.get(0).path("taskId").asLong());
        for (int index = 0; index < 10; index++) {
            assertEquals("APPLIED", materializations.get(index).path("status").asText());
            assertEquals(3, materializations.get(index).path("appliedFieldCount").asInt());
        }
        for (int index = 10; index < materializations.size(); index++) {
            assertEquals("SKIPPED_NO_SOURCE_RULES", materializations.get(index).path("status").asText());
            var skippedExecution = materializations.get(index).path("executionId");
            assertTrue(skippedExecution.isNull() || skippedExecution.isMissingNode());
            assertEquals(0, materializations.get(index).path("appliedFieldCount").asInt());
        }
        assertEquals(10, generatedAuditItems.size());
        assertEquals(30, generatedAuditItems.values().stream().mapToInt(List::size).sum());
        assertNull(memberTask.getExecutionId());
        verify(taskMapper, never()).selectByIdForUpdate(201L);
        verify(singleExecution, times(10)).openOrCreateForCompletionSource(any());
        verify(singleExecution, never()).openOrCreateForCompletionSource(argThat(req -> req.getTaskId().equals(201L)));
        persistedOpens = List.of(openEvidence("APPLIED"));
        when(routeVersions.selectSnapshotIdentityEnforcementReady()).thenReturn(0);
        assertThrows(IllegalStateException.class, () -> service.preview(batch, memberTask));
        verify(routeVersions).selectSnapshotIdentityEnforcementReady();
        verifyNoInteractions(operationAudits);
    }

    private Map<Long, List<MesProBatchRecordExecutionFieldAuditItemDO>> configureGeneratedQuantityEvidence(
            List<MesProEdhrBatchExecutionTaskDO> generated) {
        Map<Long, MesProBatchRecordExecutionDO> generatedExecutions = new HashMap<>();
        Map<Long, MesProBatchRecordExecutionFieldAuditBatchDO> generatedAudits = new HashMap<>();
        Map<Long, List<MesProBatchRecordExecutionFieldAuditItemDO>> generatedItems = new HashMap<>();
        for (var generatedTask : generated) {
            String reportId = generatedTask.getBatchRecordReportId();
            if (!"BR-15".equals(reportId) && !reportId.startsWith("BR-QUANTITY-")) continue;
            List<MesProBatchRecordCellLinkRuleDO> reportRules = rules.stream()
                    .map(row -> JsonUtils.parseObject(JsonUtils.toJsonString(row), MesProBatchRecordCellLinkRuleDO.class)
                            .setTargetReportId(reportId)).toList();
            when(ruleMapper.selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 173L, reportId, 15L))
                    .thenReturn(reportRules);
            var generatedExecution = JsonUtils.parseObject(JsonUtils.toJsonString(execution), MesProBatchRecordExecutionDO.class)
                    .setId(100L + generatedTask.getId()).setTaskId(generatedTask.getId())
                    .setRouteBindingId(generatedTask.getRouteBindingId()).setBatchRecordReportId(reportId);
            generatedExecutions.put(generatedExecution.getId(), generatedExecution);
        }
        when(executions.selectById(anyLong())).thenAnswer(i -> generatedExecutions.get(i.<Long>getArgument(0)));
        when(singleExecution.openOrCreateForCompletionSource(any())).thenAnswer(i -> {
            MesProBatchRecordExecutionOpenOrCreateByContextReqVO req = i.getArgument(0);
            return new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(100L + req.getTaskId());
        });
        when(audits.selectListByExecutionId(anyLong())).thenAnswer(i -> {
            var audit = generatedAudits.get(i.<Long>getArgument(0));
            return audit == null ? List.of() : List.of(audit);
        });
        when(items.selectListByBatchId(anyLong())).thenAnswer(i -> generatedItems.get(i.<Long>getArgument(0)));
        when(auditService.verifyChain(anyLong())).thenReturn(MesProBatchRecordExecutionFieldAuditHashVerification.valid(
                "c".repeat(64), "c".repeat(64), 1, 3));
        doAnswer(i -> {
            MesProBatchRecordExecutionFieldAuditSaveChangesCommand command = i.getArgument(0);
            Long auditId = 1000L + command.getExecutionId();
            generatedAudits.put(command.getExecutionId(), new MesProBatchRecordExecutionFieldAuditBatchDO()
                    .setId(auditId).setTenantId(1L).setExecutionId(command.getExecutionId())
                    .setIdempotencyKey(command.getIdempotencyKey()).setReasonText(command.getReasonText())
                    .setFieldCount(command.getChanges().size()).setNewHeadHash("c".repeat(64)));
            generatedItems.put(auditId, command.getChanges().stream().map(change -> new MesProBatchRecordExecutionFieldAuditItemDO()
                    .setAuditBatchId(auditId).setExecutionId(command.getExecutionId()).setRowIndex(change.getRowIndex())
                    .setColumnIndex(change.getColumnIndex()).setFieldPath(change.getFieldPath()).setFieldKey(change.getFieldKey())
                    .setReasonText(command.getReasonText()).setNewValueJson(JsonUtils.toJsonString(change.getNewValueJson()))).toList());
            return new MesProBatchRecordExecutionFieldAuditSaveResult().setAuditBatchId(auditId)
                    .setChangedFieldCount(command.getChanges().size()).setFieldAuditHeadHash("c".repeat(64));
        }).when(auditService).saveSystemCellLinkChanges(any());
        return generatedItems;
    }

    private List<MesProEdhrBatchExecutionTaskDO> generatedTasksWithProductInfoMember(
            MesProEdhrBatchExecutionServiceImpl producer) throws Exception {
        var reportMapper = mock(MesProBatchRecordReportMapper.class);
        var processReport = new MesProBatchRecordReportDO();
        processReport.setReportId("BR-15"); processReport.setReportName("粗洗工序生产记录");
        processReport.setFormSlotType("MAIN"); processReport.setBatchRecordDefinitionId(55L);
        processReport.setBatchRecordVersionId(173L);
        var memberReport = new MesProBatchRecordReportDO();
        memberReport.setReportId("BR-PRODUCT-INFO"); memberReport.setReportName("产品信息");
        memberReport.setFormSlotType("MAIN"); memberReport.setBatchRecordDefinitionId(55L);
        memberReport.setBatchRecordVersionId(173L);
        when(reportMapper.selectListByDefinitionIdAndVersionId(55L, 173L)).thenReturn(List.of(processReport, memberReport));
        ReflectionTestUtils.setField(producer, "reportMapper", reportMapper);
        var routeProcess = MesProRouteProcessDO.builder().id(15L).routeId(40L).processId(50L).sort(1).build();
        var binding = JsonUtils.parseObject(JsonUtils.parseTree(batch.getRouteSnapshotJson())
                .path("configSnapshots").path("batchUseConfigs").get(0)
                .path("batchRecordReports").get(0).toString(), MesProRouteFlowProcessBatchRecordDO.class);
        binding.setId(70L); binding.setRouteId(40L); binding.setRouteProcessId(15L);
        binding.setReportSort(1);
        var configConstructor = Class.forName(MesProEdhrBatchExecutionServiceImpl.class.getName() + "$BatchTaskConfig")
                .getDeclaredConstructors()[0];
        configConstructor.setAccessible(true);
        List<Object> sourceConfigs = new ArrayList<>();
        sourceConfigs.add(configConstructor.newInstance(routeProcess, new MesProProcessDO().setId(50L), binding,
                processReport, "SEQUENTIAL", null, null, null, null));
        var snapshot = (com.fasterxml.jackson.databind.node.ObjectNode) JsonUtils.parseTree(batch.getRouteSnapshotJson());
        var reports = (com.fasterxml.jackson.databind.node.ArrayNode) snapshot.path("configSnapshots")
                .path("batchUseConfigs").get(0).path("batchRecordReports");
        for (int index = 1; index <= 14; index++) {
            String reportId = index <= 9 ? "BR-QUANTITY-" + index : "BR-NO-QUANTITY-" + (index - 9);
            var noQuantityBinding = (com.fasterxml.jackson.databind.node.ObjectNode) reports.get(0).deepCopy();
            noQuantityBinding.put("routeBindingId", 70L + index);
            noQuantityBinding.put("batchRecordReportId", reportId);
            reports.add(noQuantityBinding);
            var noQuantityReport = new MesProBatchRecordReportDO();
            noQuantityReport.setReportId(reportId);
            noQuantityReport.setReportName("正式报告" + index); noQuantityReport.setFormSlotType("MAIN");
            noQuantityReport.setBatchRecordDefinitionId(55L); noQuantityReport.setBatchRecordVersionId(173L);
            var noQuantityRecord = JsonUtils.parseObject(noQuantityBinding.toString(), MesProRouteFlowProcessBatchRecordDO.class);
            noQuantityRecord.setId(70L + index); noQuantityRecord.setRouteId(40L); noQuantityRecord.setRouteProcessId(15L);
            noQuantityRecord.setReportSort(index + 1);
            sourceConfigs.add(configConstructor.newInstance(routeProcess, new MesProProcessDO().setId(50L), noQuantityRecord,
                    noQuantityReport, "SEQUENTIAL", null, null, null, null));
        }
        batch.setRouteSnapshotJson(snapshot.toString()); publishedVersion.setRouteSnapshotJson(snapshot.toString());
        publishedVersion.setRouteSnapshotSha256(canonicalizer.sha256(snapshot.toString()));
        List<?> configs = ReflectionTestUtils.invokeMethod(producer, "includeProductInfoMemberTaskConfigs", sourceConfigs);
        assertEquals(16, configs.size());
        List<MesProEdhrBatchExecutionTaskDO> generated = new ArrayList<>();
        for (int index = 0; index < configs.size(); index++) {
            MesProEdhrBatchExecutionTaskDO generatedTask = ReflectionTestUtils.invokeMethod(producer, "toTaskDO",
                    100L, configs.get(index), frozenReceipt().getSourceSnapshotHash());
            generatedTask.setId(200L + index);
            generated.add(generatedTask);
        }
        generated.get(0).setExecutionId(300L);
        return generated;
    }

    private void quantityRules() {
        rules.subList(3, rules.size()).clear();
        rules.get(1).setSourceFieldCode("outputQuantity");
        rules.get(2).setSourceFieldCode("lossQuantity");
        rules.forEach(rule -> rule.setAggregationStrategy("SUM").setOverwritePolicy("ONLY_WHEN_EMPTY"));
    }

    @Test void quantityAutoReplayAndReadonlyPreviewUseExactPublishedIdentityWhileLegacyGlobalGuardIsZero() {
        quantityRules();
        when(routeVersions.selectSnapshotIdentityEnforcementReady()).thenReturn(0);
        event.setRawPayload("{\"outputQuantity\":4,\"lossQuantity\":0}");
        addSecondProductionEvent("{\"outputQuantity\":6,\"lossQuantity\":0}");
        task.setExecutionId(null);
        var command = provisionCommand();
        assertEquals("APPLIED", service.materializeProvisioned(batch, task, command).status());
        assertEquals(List.of("10", "10", "0"), persistedItems.stream()
                .map(MesProBatchRecordExecutionFieldAuditItemDO::getNewValueJson).toList());
        clearInvocations(backfill, taskMapper);
        assertEquals("APPLIED", service.materializeProvisioned(batch, task, command).status());
        service.materialize(batch, task);
        var preview = service.preview(batch, task);
        assertEquals("APPLIED", preview.evidence().getStatus());
        assertEquals(3, preview.evidence().getAppliedFieldCount());
        assertEquals(0, preview.evidence().getExecutionStatus());
        verifyNoInteractions(backfill, taskMapper);
        verify(routeVersions, never()).selectSnapshotIdentityEnforcementReady();
        assertEquals("route snapshot consumer enforcement is not ready",
                assertThrows(IllegalStateException.class, () -> routes.resolveVersion(749L)).getMessage());
    }

    @Test void traditionalMaterializeStillRequiresGlobalReadinessAndDoesNotSwitchToQuantityOnFailure() {
        when(routeVersions.selectSnapshotIdentityEnforcementReady()).thenReturn(0);
        assertEquals("route snapshot consumer enforcement is not ready",
                assertThrows(IllegalStateException.class, () -> service.materialize(batch, task)).getMessage());
        verify(routeVersions, never()).selectById(any());
        verifyNoInteractions(backfill, auditService, singleExecution);
    }

    @Test void existingTraditionalAuditPreviewStillRequiresItsOriginalGlobalContract() {
        service.materialize(batch, task);
        when(routeVersions.selectSnapshotIdentityEnforcementReady()).thenReturn(0);
        assertEquals("route snapshot consumer enforcement is not ready",
                assertThrows(IllegalStateException.class, () -> service.preview(batch, task)).getMessage());
    }

    @Test void fiveNoQuantityReportsHaveExplicitReadonlyMissingSourceWithoutExecutionOrPrivateDraftProjection() {
        when(routeVersions.selectSnapshotIdentityEnforcementReady()).thenReturn(0);
        String original = batch.getRouteSnapshotJson();
        for (int index = 0; index < 5; index++) {
            long routeProcessId = 16L + index;
            String json = original.replace("\"routeProcessId\":15", "\"routeProcessId\":" + routeProcessId)
                    .replace("\"processId\":50", "\"processId\":" + (51 + index))
                    .replace("\"routeBindingId\":70", "\"routeBindingId\":" + (71 + index))
                    .replace("\"BR-15\"", "\"BR-" + routeProcessId + "\"");
            publishedVersion.setRouteSnapshotJson(json); publishedVersion.setRouteSnapshotSha256(canonicalizer.sha256(json));
            batch.setRouteSnapshotJson(json);
            task.setId(201L + index).setRouteProcessId(routeProcessId).setProcessId(51L + index)
                    .setRouteBindingId(71L + index).setBatchRecordReportId("BR-" + routeProcessId);
            task.setExecutionId(index == 0 ? 300L : null);
            execution.setCellValuesJson("[{\"rowIndex\":6,\"columnIndex\":0,\"value\":\"private manual draft\"}]");
            assertEquals("SKIPPED_NO_SOURCE_RULES", service.materializeProvisioned(batch, task, provisionCommand()).status());
            persistedOpens = List.of(openEvidence("SKIPPED_NO_SOURCE_RULES"));
            var preview = service.preview(batch, task);
            assertEquals("CONFIGURATION_MISSING", preview.evidence().getStatus());
            assertEquals("[]", preview.cellValuesJson());
        }
        verify(routeVersions, never()).selectSnapshotIdentityEnforcementReady();
        verifyNoInteractions(singleExecution, backfill, auditService, taskMapper);
    }

    @Test void oldPreviewWithoutQuantityProvisionEvidenceKeepsTraditionalContractAndDoesNotExposePrivateDraft() {
        rules.removeIf(rule -> !"actualEmployeeId".equals(rule.getSourceFieldCode()));
        execution.setCellValuesJson("[{\"rowIndex\":6,\"columnIndex\":0,\"value\":\"private manual draft\"}]");
        var preview = service.preview(batch, task);
        assertEquals("PENDING", preview.evidence().getStatus());
        assertEquals("[]", preview.cellValuesJson());
        assertNull(preview.evidence().getAuditBatchId());
        verify(routeVersions).selectSnapshotIdentityEnforcementReady();
        when(routeVersions.selectSnapshotIdentityEnforcementReady()).thenReturn(0);
        assertThrows(IllegalStateException.class, () -> service.preview(batch, task));
        verifyNoInteractions(backfill, auditService, taskMapper);
    }

    @Test void reservedQuantityTextWithoutMatchingVerifiedAuditCannotEstablishQuantityReentry() {
        quantityRules();
        when(routeVersions.selectSnapshotIdentityEnforcementReady()).thenReturn(0);
        persistedAudit = new MesProBatchRecordExecutionFieldAuditBatchDO().setId(1000L).setTenantId(1L)
                .setExecutionId(300L).setIdempotencyKey("fabricated")
                .setReasonText(MesProEdhrCompletionSourceBackfillService.AUDIT_REASON_PREFIX + "scope=QUANTITY|fabricated");
        assertThrows(ServiceException.class, () -> service.materialize(batch, task));
        assertThrows(ServiceException.class, () -> service.preview(batch, task));
        verifyNoInteractions(backfill, singleExecution, auditService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "id", "tenant", "route", "draft", "activeFalse", "supersededTrue", "activeNull",
            "deleted", "hashNull", "hashEmpty", "hashBad", "hashUppercase", "formatNull", "formatUnsupported"})
    void quantityNeverRepairsOrSubstitutesMissingOrInvalidExactPublishedIdentity(String defect) {
        quantityRules();
        when(routeVersions.selectSnapshotIdentityEnforcementReady()).thenReturn(0);
        switch (defect) {
            case "missing" -> doReturn(List.of()).when(routeVersions).selectByMap(any());
            case "id" -> publishedVersion.setId(750L);
            case "tenant" -> versionTenant = 2L;
            case "route" -> publishedVersion.setRouteId(41L);
            case "draft" -> publishedVersion.setLifecycleStatus("DRAFT");
            case "activeFalse" -> publishedVersion.setActive(false);
            case "supersededTrue" -> publishedVersion.setLifecycleStatus("SUPERSEDED");
            case "activeNull" -> publishedVersion.setActive(null);
            case "deleted" -> publishedVersion.setDeleted(true);
            case "hashNull" -> publishedVersion.setRouteSnapshotSha256(null);
            case "hashEmpty" -> publishedVersion.setRouteSnapshotSha256("");
            case "hashBad" -> publishedVersion.setRouteSnapshotSha256("0".repeat(64));
            case "hashUppercase" -> publishedVersion.setRouteSnapshotSha256(publishedVersion.getRouteSnapshotSha256().toUpperCase());
            case "formatNull" -> publishedVersion.setRouteSnapshotFormatVersion(null);
            case "formatUnsupported" -> publishedVersion.setRouteSnapshotFormatVersion("CANONICAL_V2");
            default -> throw new AssertionError(defect);
        }
        assertThrows(ServiceException.class, () -> service.materializeProvisioned(batch, task, provisionCommand()));
        persistedOpens = List.of(openEvidence("SKIPPED_NO_SOURCE_RULES"));
        rules.clear();
        assertThrows(ServiceException.class, () -> service.preview(batch, task));
        verify(routeVersions, never()).selectSnapshotIdentityEnforcementReady();
        verifyNoInteractions(backfill, singleExecution, taskMapper, auditService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"routeId", "duplicateProcess", "processId", "bindingId", "report", "definition", "version", "slot"})
    void quantityValidHashCannotAuthorizeMalformedFormalSnapshotOrAnotherBinding(String defect) {
        quantityRules();
        String json = publishedVersion.getRouteSnapshotJson();
        json = switch (defect) {
            case "routeId" -> json.replace("\"routeId\":40", "\"routeId\":41");
            case "duplicateProcess" -> json.replace("\"nodes\":[", "\"nodes\":[{\"routeProcessId\":15,\"processId\":50},");
            case "processId" -> json.replace("\"processId\":50", "\"processId\":51");
            case "bindingId" -> json.replace("\"routeBindingId\":70", "\"routeBindingId\":71");
            case "report" -> json.replace("\"BR-15\"", "\"BR-16\"");
            case "definition" -> json.replace("\"batchRecordDefinitionId\":55", "\"batchRecordDefinitionId\":56");
            case "version" -> json.replace("\"batchRecordVersionId\":173", "\"batchRecordVersionId\":174");
            case "slot" -> json.replace("\"MAIN\"", "\"PROCESS_INSPECTION\"");
            default -> throw new AssertionError(defect);
        };
        publishedVersion.setRouteSnapshotJson(json);
        publishedVersion.setRouteSnapshotSha256(canonicalizer.sha256(json));
        batch.setRouteSnapshotJson(json);
        assertThrows(ServiceException.class, () -> service.materializeProvisioned(batch, task, provisionCommand()));
        verifyNoInteractions(backfill, singleExecution, taskMapper, auditService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"mismatch", "duplicateKey", "trailingJson"})
    void quantityBatchSnapshotMustMatchOfficialCanonicalIdentityWithStrictJson(String defect) {
        quantityRules();
        String json = batch.getRouteSnapshotJson();
        batch.setRouteSnapshotJson(switch (defect) {
            case "mismatch" -> json.replace("\"routeBindingId\":70", "\"routeBindingId\":71");
            case "duplicateKey" -> json.replace("\"routeId\":40", "\"routeId\":40,\"routeId\":40");
            case "trailingJson" -> json + "{}";
            default -> throw new AssertionError(defect);
        });
        assertThrows(RuntimeException.class, () -> service.materializeProvisioned(batch, task, provisionCommand()));
        verifyNoInteractions(backfill, singleExecution, taskMapper, auditService);
    }

    @Test void quantityExactSupersededInactiveIdentityAndCanonicalWhitespaceRemainValid() {
        quantityRules();
        publishedVersion.setLifecycleStatus("SUPERSEDED"); publishedVersion.setActive(false);
        when(routeVersions.selectSnapshotIdentityEnforcementReady()).thenReturn(0);
        batch.setRouteSnapshotJson(JsonUtils.toJsonString(JsonUtils.parseTree(batch.getRouteSnapshotJson())));
        assertEquals("APPLIED", service.materializeProvisioned(batch, task, provisionCommand()).status());
        assertEquals("APPLIED", service.preview(batch, task).evidence().getStatus());
        verify(routeVersions, never()).selectSnapshotIdentityEnforcementReady();
    }

    @Test void existingQuantityAuditNeverReplacesMissingPublishedIdentityOnAutoReplayManualOpenOrPreview() {
        quantityRules(); var command = provisionCommand();
        when(routeVersions.selectSnapshotIdentityEnforcementReady()).thenReturn(0);
        assertEquals("APPLIED", service.materializeProvisioned(batch, task, command).status());
        publishedVersion.setRouteSnapshotSha256(null);
        clearInvocations(backfill, auditService, singleExecution);
        assertThrows(ServiceException.class, () -> service.materializeProvisioned(batch, task, command));
        assertThrows(ServiceException.class, () -> service.materialize(batch, task));
        assertThrows(ServiceException.class, () -> service.preview(batch, task));
        verifyNoInteractions(backfill, auditService, singleExecution);
        verify(routeVersions, never()).selectSnapshotIdentityEnforcementReady();
    }

    @Test void exactTenantVersionMustResolveOnceBeforeQuantityWriting() {
        quantityRules();
        doReturn(List.of(publishedVersion, publishedVersion)).when(routeVersions).selectByMap(any());
        assertThrows(ServiceException.class, () -> service.materializeProvisioned(batch, task, provisionCommand()));
        verifyNoInteractions(backfill, auditService, singleExecution);
    }

    private MesProEdhrOperationAuditEventDO openEvidence(String status) {
        String json = ReflectionTestUtils.invokeMethod(new MesProEdhrBatchExecutionServiceImpl(),
                "entryAuditMetadata", provisionCommand(), 5000L);
        var metadata = (com.fasterxml.jackson.databind.node.ObjectNode) JsonUtils.parseTree(json);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", task.getId()); result.put("status", status);
        result.put("executionId", "APPLIED".equals(status) ? 300L : null);
        result.put("appliedFieldCount", "APPLIED".equals(status) ? 3 : 0);
        metadata.set("completionSourceMaterializations", JsonUtils.parseTree(JsonUtils.toJsonString(List.of(result))));
        return new MesProEdhrOperationAuditEventDO().setId(6000L).setObjectType("BATCH_EXECUTION")
                .setObjectId("100").setBatchExecutionId(100L).setRouteId(40L)
                .setOperationType("OPEN").setResultStatus("SUCCESS").setPermissionDecision("ALLOW")
                .setMetadataJson(metadata.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"batch", "route", "version", "active", "work", "receipt", "receiptHash", "sourceHash",
            "task", "duplicates", "appliedNoAudit", "count", "countOverflow", "execution", "entry", "missingTask", "duplicateJson", "onlyReplay"})
    void quantitySkipPreviewRejectsWrongAmbiguousOrAppliedWithoutActualAuditOpenEvidence(String defect) {
        rules.clear();
        var open = openEvidence("appliedNoAudit".equals(defect) ? "APPLIED" : "SKIPPED_NO_SOURCE_RULES");
        var metadata = (com.fasterxml.jackson.databind.node.ObjectNode) JsonUtils.parseTree(open.getMetadataJson());
        var result = (com.fasterxml.jackson.databind.node.ObjectNode) metadata.path("completionSourceMaterializations").get(0);
        switch (defect) {
            case "batch" -> open.setBatchExecutionId(101L);
            case "route" -> metadata.put("routeId", 41L);
            case "version" -> metadata.put("routeVersionId", 750L);
            case "active" -> metadata.put("activeOrderId", 438L);
            case "work" -> metadata.put("workOrderId", 21L);
            case "receipt" -> metadata.put("completionBackfillReceiptId", "401");
            case "receiptHash" -> metadata.put("completionBackfillReceiptHash", "wrong");
            case "sourceHash" -> metadata.put("sourceSnapshotHash", "wrong");
            case "task" -> result.put("taskId", 201L);
            case "duplicates" -> ((com.fasterxml.jackson.databind.node.ArrayNode) metadata.path("completionSourceMaterializations")).add(result.deepCopy());
            case "count" -> result.put("appliedFieldCount", 1);
            case "countOverflow" -> result.put("appliedFieldCount", 4294967296L);
            case "execution" -> result.put("executionId", 300L);
            case "entry" -> metadata.put("entryType", "SCHEDULED");
            case "missingTask" -> metadata.putArray("completionSourceMaterializations");
            case "onlyReplay" -> metadata.putNull("batchProvisionReceiptId");
            case "appliedNoAudit", "duplicateJson" -> { }
            default -> throw new AssertionError(defect);
        }
        open.setMetadataJson(metadata.toString());
        if ("duplicateJson".equals(defect)) open.setMetadataJson(open.getMetadataJson().replace("\"routeId\":40", "\"routeId\":40,\"routeId\":40"));
        persistedOpens = List.of(open);
        assertThrows(RuntimeException.class, () -> service.preview(batch, task));
        verify(routeVersions, never()).selectSnapshotIdentityEnforcementReady();
        verifyNoInteractions(backfill, singleExecution, auditService);
    }

    @Test void identicalSuccessfulSkipReplayIsUnambiguousButNewQuantityMappingsCannotReuseSkip() {
        rules.clear();
        when(routeVersions.selectSnapshotIdentityEnforcementReady()).thenReturn(0);
        var open = openEvidence("SKIPPED_NO_SOURCE_RULES");
        var replay = openEvidence("SKIPPED_NO_SOURCE_RULES");
        var metadata = (com.fasterxml.jackson.databind.node.ObjectNode) JsonUtils.parseTree(replay.getMetadataJson());
        metadata.putNull("batchProvisionReceiptId"); replay.setMetadataJson(metadata.toString());
        persistedOpens = List.of(replay, open, open);
        assertEquals("CONFIGURATION_MISSING", service.preview(batch, task).evidence().getStatus());
        rules.add(new MesProBatchRecordCellLinkRuleDO().setId(1L).setScopeType("ROUTE_VERSION").setScopeId(173L)
                .setTargetReportId("BR-15").setRouteProcessId(15L).setSourceType("PROCESS_POOL_REPORT")
                .setSourceFieldCode("outputQuantity").setTargetRowIndex(0).setTargetColumnIndex(0).setEnabled(true));
        assertThrows(ServiceException.class, () -> service.preview(batch, task));
    }

    @Test void nonMainTraditionalPreviewDoesNotInterpretMainOnlyQuantityProvisionResults() {
        task.setFormSlotType("PROCESS_INSPECTION");
        persistedOpens = List.of(openEvidence("SKIPPED_NO_SOURCE_RULES"));
        assertEquals("PENDING", service.preview(batch, task).evidence().getStatus());
        verifyNoInteractions(operationAudits);
        when(routeVersions.selectSnapshotIdentityEnforcementReady()).thenReturn(0);
        assertThrows(IllegalStateException.class, () -> service.preview(batch, task));
        verifyNoInteractions(operationAudits);
    }

    @Test void provisionedReceiptWithoutOriginCreatesOnlyDraftBindingAndExactQuantityAudit() {
        quantityRules();
        event.setRawPayload("{\"outputQuantity\":4,\"lossQuantity\":0}");
        addSecondProductionEvent("{\"outputQuantity\":6,\"lossQuantity\":0}");
        task.setExecutionId(null).setStatus(0);
        var command = provisionCommand();
        when(origins.selectListByBatchExecutionId(100L)).thenReturn(List.of());
        var result = service.materializeProvisioned(batch, task, command);
        assertEquals("APPLIED", result.status());
        assertEquals(List.of("10", "10", "0"), persistedItems.stream()
                .map(MesProBatchRecordExecutionFieldAuditItemDO::getNewValueJson).toList());
        assertEquals(300L, task.getExecutionId());
        assertEquals(0, task.getStatus());
        assertEquals(0, execution.getStatus());
        assertTrue(persistedAudit.getReasonText().contains("receipt=400"));
        verify(singleExecution).openOrCreateForCompletionSource(any());
        verify(singleExecution, never()).openOrCreateByContext(any());
        verifyNoInteractions(origins);
        clearInvocations(backfill, taskMapper);
        assertEquals("APPLIED", service.materializeProvisioned(batch, task, command).status());
        verifyNoInteractions(backfill, taskMapper);
    }

    @Test void provisionedNoRulesExplicitlySkipsWithoutCreatingExecutionOrAudit() {
        rules.clear(); task.setExecutionId(null);
        var result = service.materializeProvisioned(batch, task, provisionCommand());
        assertEquals("SKIPPED_NO_SOURCE_RULES", result.status());
        assertNull(result.executionId()); assertNull(task.getExecutionId());
        assertEquals(0, result.appliedFieldCount());
        verifyNoInteractions(backfill, taskMapper, auditService, singleExecution);
    }

    @Test void provisionedQuantitiesCoexistWithOtherFormalRulesWithoutWritingOrRehashingThem() {
        quantityRules();
        var actor = new MesProBatchRecordCellLinkRuleDO().setId(20L).setScopeType("ROUTE_VERSION")
                .setScopeId(173L).setRouteProcessId(15L).setTargetReportId("BR-15")
                .setSourceType("PROCESS_POOL_REPORT").setSourceFieldCode("actualEmployeeId")
                .setTargetRowIndex(20).setTargetColumnIndex(0).setTargetCellKey("R20C0")
                .setTargetValueType("NUMBER").setEnabled(true);
        var date = new MesProBatchRecordCellLinkRuleDO().setId(21L).setScopeType("ROUTE_VERSION")
                .setScopeId(173L).setRouteProcessId(15L).setTargetReportId("BR-15")
                .setSourceType("PROCESS_POOL_REPORT").setSourceFieldCode("serverSubmitTime")
                .setTargetRowIndex(21).setTargetColumnIndex(0).setTargetCellKey("R21C0")
                .setTargetValueType("DATETIME").setEnabled(true);
        var pick = new MesProBatchRecordCellLinkRuleDO().setId(22L).setScopeType("ROUTE_VERSION")
                .setScopeId(173L).setRouteProcessId(15L).setTargetReportId("BR-15")
                .setSourceType("PRODUCTION_PICK_LIST").setSourceFieldCode("materialCode")
                .setTargetRowIndex(22).setTargetColumnIndex(0).setTargetCellKey("R22C0")
                .setTargetValueType("TEXT").setEnabled(true);
        rules.addAll(List.of(actor, date, pick));
        var command = provisionCommand();
        assertEquals("APPLIED", service.materializeProvisioned(batch, task, command).status());
        assertEquals(3, persistedItems.size());
        assertEquals("APPLIED", service.preview(batch, task).evidence().getStatus());
        assertEquals(3, service.preview(batch, task).evidence().getAppliedFieldCount());
        clearInvocations(backfill, singleExecution);
        service.materialize(batch, task);
        verifyNoInteractions(backfill, singleExecution, pickLists);
        rules.get(0).setTargetRowIndex(24).setTargetCellKey("R24C0");
        assertThrows(ServiceException.class, () -> service.materializeProvisioned(batch, task, command));
        assertThrows(ServiceException.class, () -> service.preview(batch, task));
    }

    @Test void provisionedUnsupportedSourceRulesExplicitlySkipButDuplicateTargetsStillFail() {
        rules.removeIf(rule -> Set.of("outputQuantity", "lossQuantity").contains(rule.getSourceFieldCode()));
        task.setExecutionId(null);
        assertEquals("SKIPPED_NO_SOURCE_RULES", service.materializeProvisioned(batch, task, provisionCommand()).status());
        rules.add(new MesProBatchRecordCellLinkRuleDO().setId(21L).setScopeType("ROUTE_VERSION")
                .setScopeId(173L).setRouteProcessId(15L).setTargetReportId("BR-15")
                .setSourceType("PROCESS_POOL_REPORT").setSourceFieldCode("outputQuantity")
                .setTargetRowIndex(rules.get(0).getTargetRowIndex()).setTargetColumnIndex(0)
                .setTargetCellKey("duplicate").setTargetValueType("NUMBER").setEnabled(true));
        assertThrows(ServiceException.class, () -> service.materializeProvisioned(batch, task, provisionCommand()));
        verifyNoInteractions(backfill, singleExecution, auditService);
    }

    @Test void provisionedPqcEntryUsesSameCanonicalReceiptAndReadonlyEvidence() {
        quantityRules();
        assertEquals("APPLIED", service.materializeProvisioned(batch, task,
                provisionCommand().setEntryType("ACTIVE_ORDER_PQC")).status());
        assertEquals("APPLIED", service.preview(batch, task).evidence().getStatus());
    }

    @Test void provisionedWrongReceiptHashTenantSourceOrBindingIsRejectedBeforeWriting() {
        quantityRules();
        var command = provisionCommand().setCompletionBackfillReceiptHash("wrong");
        assertThrows(ServiceException.class, () -> service.materializeProvisioned(batch, task, command));
        command.setCompletionBackfillReceiptHash(receipt.getReceiptHash()).setTenantId(2L);
        assertThrows(ServiceException.class, () -> service.materializeProvisioned(batch, task, command));
        command.setTenantId(1L).setSourceSnapshotHash("wrong");
        assertThrows(ServiceException.class, () -> service.materializeProvisioned(batch, task, command));
        command.setSourceSnapshotHash(receipt.getSourceSnapshotHash()); task.setRouteBindingId(71L);
        assertThrows(ServiceException.class, () -> service.materializeProvisioned(batch, task, command));
        verifyNoInteractions(backfill, taskMapper, auditService, singleExecution);
    }

    @Test void provisionedSubmittedOrManualTargetCannotBeOverwritten() {
        quantityRules(); var command = provisionCommand();
        execution.setStatus(1);
        assertThrows(ServiceException.class, () -> service.materializeProvisioned(batch, task, command));
        execution.setStatus(0).setCellValuesJson("[{\"rowIndex\":0,\"columnIndex\":0,\"value\":99}]");
        assertThrows(ServiceException.class, () -> service.materializeProvisioned(batch, task, command));
        verify(auditService, never()).saveSystemCellLinkChanges(any());
    }

    @Test void provisionedTamperedFrozenFactsDoNotUseCanonicalHashOrLiveFactsAsSubstitute() {
        quantityRules(); var command = provisionCommand();
        when(receipts.getByReceiptId(400L, 1L)).thenReturn(receipt.setFormalSourceSnapshotJson("{}"));
        assertThrows(ServiceException.class, () -> service.materializeProvisioned(batch, task, command));
        verifyNoInteractions(backfill, taskMapper, auditService, singleExecution);
    }

    @Test void mapsQualifiedOutputLossTotalAndIndependentProductionIdentitiesThroughRealBackfill() {
        service.materialize(batch, task);
        assertEquals(List.of("10", "2", "12", "342", "341", "341", "342"), persistedItems.stream()
                .map(MesProBatchRecordExecutionFieldAuditItemDO::getNewValueJson).toList());
        var capture = org.mockito.ArgumentCaptor.forClass(MesTeamLeaderBatchRecordBackfillCommand.class);
        verify(backfill).backfillCompletedProcess(capture.capture());
        var command = capture.getValue();
        assertEquals(341L, command.getEvent().getDeviceAccountId());
        assertEquals(342L, command.getEvent().getActualEmployeeId());
        assertEquals(600L, command.getEvent().getSignatureId());
        assertEquals("MES_EMPLOYEE", JsonUtils.parseTree(command.getEvent().getRawPayload()).path("signatureIdentityDomain").asText());
        assertEquals(341L, command.getReviews().get(0).getReviewSignatureUserId());
        assertNull(command.getPickListBindingId());
        verifyNoInteractions(pickLists);
        assertEquals(0, execution.getStatus());
        assertEquals("APPLIED", service.preview(batch, task).evidence().getStatus());
    }

    @Test void missingFrozenOperatorNeverUsesSelfReportedRawOperator() {
        event.setDeviceAccountId(null);
        assertThrows(ServiceException.class, () -> service.materialize(batch, task));
        assertNull(persistedAudit);
    }

    @Test void historicalReceiptWithoutOperatorCanStillConsumeOtherExplicitRules() {
        event.setDeviceAccountId(null);
        rules.removeIf(rule -> "operatorUserId".equals(rule.getSourceFieldCode()));
        service.materialize(batch, task);
        assertEquals(6, persistedItems.size());
    }

    @Test void zeroMappingIsVisibleInReadonlyPreviewAndFailsOpenBeforeWriting() {
        rules.clear();
        assertEquals("CONFIGURATION_MISSING", service.preview(batch, task).evidence().getStatus());
        assertThrows(ServiceException.class, () -> service.materialize(batch, task));
        verifyNoInteractions(backfill);
    }

    @Test void currentFactsDoNotReplaceAnotherReceiptBatchOrFrozenBinding() {
        receipt.setWorkOrderId(21L);
        assertThrows(ServiceException.class, () -> service.materialize(batch, task));
        verifyNoInteractions(backfill);
    }

    @Test void changedFrozenBindingIsRejectedWithoutAutoBinding() {
        task.setRouteBindingId(71L);
        assertThrows(ServiceException.class, () -> service.materialize(batch, task));
        verifyNoInteractions(backfill);
    }

    @Test void pqcReviewCannotFillProductionReviewerField() {
        review.setLeaderType("PQC").setReviewSignatureUserId(343L);
        assertThrows(ServiceException.class, () -> service.materialize(batch, task));
        verifyNoInteractions(backfill);
    }

    @Test void pickListConsumerReportsMissingFrozenSelectionContractOnlyWhenConsumed() {
        rules.get(0).setSourceType("PRODUCTION_PICK_LIST");
        assertEquals("CONFIGURATION_MISSING", service.preview(batch, task).evidence().getStatus());
        assertThrows(ServiceException.class, () -> service.materialize(batch, task));
        verifyNoInteractions(backfill, pickLists);
    }

    @Test void replayReadsTheSameAuditWithoutOverwritingManualDraftOrSubmittedValues() {
        service.materialize(batch, task);
        execution.setCellValuesJson("[{\"rowIndex\":99,\"columnIndex\":0,\"value\":\"private manual draft\"}]");
        execution.setStatus(2);
        service.materialize(batch, task);
        verify(backfill, times(1)).backfillCompletedProcess(any());
        assertFalse(service.preview(batch, task).cellValuesJson().contains("private manual draft"));
        assertEquals(2, execution.getStatus());
    }

    @Test void changedMappingCannotReuseThePreviousSourceAudit() {
        service.materialize(batch, task);
        rules.get(0).setAggregationStrategy("FIRST");
        assertThrows(ServiceException.class, () -> service.materialize(batch, task));
        verify(backfill, times(1)).backfillCompletedProcess(any());
    }

    @Test void unsignedManualTargetIsNotOverwritten() {
        execution.setCellValuesJson("[{\"rowIndex\":0,\"columnIndex\":0,\"value\":99}]");
        assertThrows(ServiceException.class, () -> service.materialize(batch, task));
        verify(auditService, never()).saveSystemCellLinkChanges(any());
    }

    @Test void submittedExecutionWithoutSourceAuditIsNotBackfilled() {
        execution.setStatus(2);
        assertThrows(ServiceException.class, () -> service.materialize(batch, task));
        verifyNoInteractions(backfill);
    }

    @Test void invalidManualDraftCannotBeTreatedAsEmpty() {
        execution.setCellValuesJson("{broken draft");
        assertThrows(ServiceException.class, () -> service.materialize(batch, task));
        verifyNoInteractions(backfill);
    }

    @Test void sourceAuditReplayStillRequiresExactExecutionContext() {
        service.materialize(batch, task);
        execution.setTaskId(201L);
        assertThrows(ServiceException.class, () -> service.materialize(batch, task));
        assertThrows(ServiceException.class, () -> service.preview(batch, task));
        verify(backfill, times(1)).backfillCompletedProcess(any());
    }

    @Test void unrelatedManualBatchKeepsOrdinaryBehavior() {
        when(origins.selectListByBatchExecutionId(100L)).thenReturn(List.of());
        assertNull(service.preview(batch, task));
        service.materialize(batch, task);
        verifyNoInteractions(backfill, receipts);
    }

    @Test void multipleProductionEventsWithoutDeclaredAggregationAreRejected() {
        addSecondProductionEvent("{\"outputQuantity\":4,\"lossQuantity\":1}");
        assertThrows(ServiceException.class, () -> service.materialize(batch, task));
        verify(auditService, never()).saveSystemCellLinkChanges(any());
    }

    @Test void anotherProcessSharingTheReportAndVersionDoesNotPolluteCurrentMappings() {
        MesProBatchRecordCellLinkRuleDO other = JsonUtils.parseObject(JsonUtils.toJsonString(rules.get(0)),
                MesProBatchRecordCellLinkRuleDO.class);
        other.setId(20L); other.setRouteProcessId(16L); rules.add(other);
        service.materialize(batch, task);
        assertEquals(7, persistedItems.size());
        assertEquals(List.of("10", "2", "12", "342", "341", "341", "342"), persistedItems.stream()
                .map(MesProBatchRecordExecutionFieldAuditItemDO::getNewValueJson).toList());
    }

    @Test void unrelatedProcessMappingChangeDoesNotChangeCurrentSourceIdentityOrReplay() {
        MesProBatchRecordCellLinkRuleDO other = JsonUtils.parseObject(JsonUtils.toJsonString(rules.get(0)),
                MesProBatchRecordCellLinkRuleDO.class);
        other.setId(20L); other.setRouteProcessId(16L); rules.add(other);
        service.materialize(batch, task);
        String before = service.preview(batch, task).evidence().getMappingHash();
        other.setAggregationStrategy("SUM"); other.setTargetRowIndex(20);
        service.materialize(batch, task);
        assertEquals(before, service.preview(batch, task).evidence().getMappingHash());
        verify(backfill, times(1)).backfillCompletedProcess(any());
    }

    @Test void identityFieldsRejectNumericAggregationEvenForASingleEvent() {
        for (String field : List.of("operatorUserId", "actualEmployeeId")) {
            var rule = rules.stream().filter(item -> field.equals(item.getSourceFieldCode())).findFirst().orElseThrow();
            for (String strategy : List.of("SUM", "MIN", "MAX")) {
                rule.setAggregationStrategy(strategy);
                assertThrows(ServiceException.class, () -> service.materialize(batch, task));
                assertNull(persistedAudit);
            }
            rule.setAggregationStrategy(null);
        }
    }

    @Test void identityFieldsUseOnlyExplicitFirstOrLastAcrossFrozenEvents() {
        addSecondProductionEvent("{\"outputQuantity\":4,\"lossQuantity\":1}");
        var facts = (Map<?, ?>) source.get("productionFacts");
        var secondEvent = (MesProProcessPoolEventDO) ((List<?>) facts.get("events")).get(1);
        secondEvent.setDeviceAccountId(345L).setActualEmployeeId(346L);
        rules.removeIf(rule -> !Set.of("operatorUserId", "actualEmployeeId").contains(rule.getSourceFieldCode()));
        rules.get(0).setAggregationStrategy("FIRST");
        rules.get(1).setAggregationStrategy("LAST");
        service.materialize(batch, task);
        assertEquals(List.of("341", "346"), persistedItems.stream()
                .map(MesProBatchRecordExecutionFieldAuditItemDO::getNewValueJson).toList());
    }

    @Test void identityFieldsRequireExplicitSelectionAcrossMultipleEvents() {
        addSecondProductionEvent("{\"outputQuantity\":4,\"lossQuantity\":1}");
        rules.removeIf(rule -> !Set.of("operatorUserId", "actualEmployeeId").contains(rule.getSourceFieldCode()));
        assertThrows(ServiceException.class, () -> service.materialize(batch, task));
        assertNull(persistedAudit);
    }

    @Test void explicitSumUsesAllFrozenEventsWithoutInventingQuantityDefaults() {
        addSecondProductionEvent("{\"outputQuantity\":4,\"lossQuantity\":1}");
        rules.subList(3, rules.size()).clear();
        rules.forEach(rule -> rule.setAggregationStrategy("SUM"));
        service.materialize(batch, task);
        assertEquals(List.of("14", "3", "17"), persistedItems.stream()
                .map(MesProBatchRecordExecutionFieldAuditItemDO::getNewValueJson).toList());
    }

    @Test void explicitSumStillRejectsMissingLossInsteadOfDefaultingToZero() {
        addSecondProductionEvent("{\"outputQuantity\":4}");
        rules.subList(3, rules.size()).clear();
        rules.forEach(rule -> rule.setAggregationStrategy("SUM"));
        assertThrows(ServiceException.class, () -> service.materialize(batch, task));
        verify(auditService, never()).saveSystemCellLinkChanges(any());
    }

    private void addSecondProductionEvent(String payload) {
        MesProProcessPoolEventDO secondEvent = JsonUtils.parseObject(JsonUtils.toJsonString(event),
                MesProProcessPoolEventDO.class).setId(501L).setSignatureId(601L)
                .setServerSubmitTime(event.getServerSubmitTime().plusMinutes(1)).setRawPayload(payload);
        MesProcessPoolSubmissionReviewDO secondReview = JsonUtils.parseObject(JsonUtils.toJsonString(review),
                MesProcessPoolSubmissionReviewDO.class).setId(801L).setEventId(501L).setReviewSignatureId(901L);
        MesProcessPoolReportAllocationDO first = JsonUtils.parseObject(JsonUtils.toJsonString(
                ((List<?>) source.get("allocations")).get(0)), MesProcessPoolReportAllocationDO.class);
        MesProcessPoolReportAllocationDO second = JsonUtils.parseObject(JsonUtils.toJsonString(first),
                MesProcessPoolReportAllocationDO.class).setId(701L).setEventId(501L).setReviewId(801L)
                .setAllocatedQuantity(new BigDecimal("4")).setConfirmedAt(secondEvent.getServerSubmitTime());
        source.put("allocations", List.of(first, second));
        source.put("productionFacts", Map.of("events", List.of(event, secondEvent),
                "reviews", List.of(review, secondReview)));
        source.put("completions", List.of(MesProcessPoolOrderProcessCompletionDO.builder().workOrderId(20L)
                .routeProcessId(15L).processId(50L).completionStatus("COMPLETED").lastEventId(501L).build()));
    }
}
