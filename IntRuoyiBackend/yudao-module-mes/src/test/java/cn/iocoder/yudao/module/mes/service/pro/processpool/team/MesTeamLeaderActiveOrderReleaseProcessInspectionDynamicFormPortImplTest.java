package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.BusinessActionContextReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceDraftReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceSnapshotRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceSubmitReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper;
import cn.iocoder.yudao.module.bpm.formcenter.runtime.FormCenterRuntimeService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPortImplTest {

    private FormTemplateVersionMapper templateVersionMapper;
    private FormActionInstanceMapper instanceMapper;
    private FormCenterRuntimeService runtimeService;
    private MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPort port;

    @BeforeEach
    void setUp() {
        templateVersionMapper = mock(FormTemplateVersionMapper.class);
        instanceMapper = mock(FormActionInstanceMapper.class);
        runtimeService = mock(FormCenterRuntimeService.class);
        port = new MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPortImpl(
                templateVersionMapper, instanceMapper, runtimeService);
    }

    @Test
    void resolvesExactPublishedTemplateFieldsAndPersistsAuditedEffectiveInstance() {
        MesProRouteFlowProcessBatchRecordDO binding = binding();
        List<MesProBatchRecordCellLinkRuleDO> rules = List.of(
                rule(11L, "PQC|PRESSURE|1|measuredValue", "measuredValue", 3, 1, "NUMBER"),
                rule(12L, "PQC|reviewedAt", "reviewedAt", 3, 3, "DATETIME"));
        FormTemplateVersionDO template = template();
        when(templateVersionMapper.selectById(2801L)).thenReturn(template);

        MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPort.TargetResolution target =
                port.resolveTarget(binding, rules);

        assertTrue(target.isValid());
        assertEquals(Map.of(11L, "fieldMeasured", 12L, "fieldReviewedAt"), target.getTargetFieldCodes());

        MesProEdhrBatchExecutionTaskDO task = task();
        BusinessActionContextReqVO context = new BusinessActionContextReqVO();
        context.setTenantId(1L);
        context.setSystemCode("MES");
        context.setObjectType("EDHR_ROUTE_FORM");
        context.setObjectId(String.valueOf(task.getId()));
        context.setObjectVersion("632");
        context.setActionCode("EDHR_RF_632_PI_9901");
        context.setDataDomain("MES");
        context.setObjectState("ACTIVE");
        FormActionInstanceDO instance = FormActionInstanceDO.builder()
                .id(9801L).tenantId(1L).applicantUserId(149L).status("DRAFT")
                .systemCode("MES").objectType("EDHR_ROUTE_FORM").objectId(String.valueOf(task.getId()))
                .objectVersion("632").actionCode("EDHR_RF_632_PI_9901")
                .businessContextJson(JsonUtils.toJsonString(context))
                .formDataJson(JsonUtils.toJsonString(Map.of("batchExecutionId", 901L, "batchTaskId", 902L)))
                .build();
        when(instanceMapper.selectById(9801L)).thenReturn(instance);
        AtomicReference<Map<String, Object>> written = new AtomicReference<>();
        org.mockito.Mockito.doAnswer(invocation -> {
            FormInstanceDraftReqVO request = invocation.getArgument(1);
            written.set(new LinkedHashMap<>(request.getFormData()));
            return null;
        }).when(runtimeService).saveDraft(eq(9801L), any(), eq(149L));
        FormInstanceRespVO submitted = new FormInstanceRespVO();
        submitted.setId(9801L);
        submitted.setStatus("EFFECTIVE");
        when(runtimeService.submitInstance(eq(9801L), any(FormInstanceSubmitReqVO.class), eq(149L)))
                .thenReturn(submitted);
        when(runtimeService.getInstanceSnapshots(9801L)).thenAnswer(invocation -> {
            FormInstanceSnapshotRespVO snapshot = new FormInstanceSnapshotRespVO();
            snapshot.setId(9802L);
            snapshot.setInstanceId(9801L);
            snapshot.setSnapshotType("SUBMIT");
            snapshot.setSnapshotVersion(3);
            snapshot.setFormData(written.get());
            return List.of(snapshot);
        });
        List<MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPort.FieldWrite> fields = List.of(
                field(rules.get(0), "fieldMeasured", "10.5", "field-hash-1"),
                field(rules.get(1), "fieldReviewedAt", "2026-08-10 10:11:12", "field-hash-2"));
        MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPort.WriteCommand command =
                new MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPort.WriteCommand()
                        .setTenantId(1L).setBatchExecutionId(901L).setBatchTask(task).setBinding(binding)
                        .setTarget(target).setFields(fields).setSourceSnapshotHash("source-snapshot")
                        .setEvidenceHash("inspection-evidence")
                        .setSignatureEvidence(List.of(
                                new MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult.SignatureEvidence()
                                        .setRole("FILLER").setSourceType("PQC_SUBMIT").setSourceId(602L)
                                        .setSignatureId(1101L).setUserId(149L).setEvidenceHash("signature-hash")));

        MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPort.WriteResult result = port.write(command);

        assertEquals(9801L, result.getFormCenterInstanceId());
        assertEquals(9802L, result.getFieldAuditSnapshotId());
        assertEquals("EFFECTIVE", result.getEffectiveStatus());
        assertNotNull(result.getFieldAuditHeadHash());
        assertEquals("10.5", written.get().get("fieldMeasured"));
        assertEquals("2026-08-10 10:11:12", written.get().get("fieldReviewedAt"));
        assertTrue(written.get().containsKey("_processInspectionReleaseAudit"));
        assertFalse(JsonUtils.toJsonString(written.get()).contains("rawPayload"));
        ArgumentCaptor<FormInstanceSubmitReqVO> submitCaptor =
                ArgumentCaptor.forClass(FormInstanceSubmitReqVO.class);
        verify(runtimeService).submitInstance(eq(9801L), submitCaptor.capture(), eq(149L));
        assertEquals(written.get(), submitCaptor.getValue().getFormData());
    }

    @Test
    void nonPublishedOrMismatchedTemplateReturnsBlockerWithoutInstanceWrite() {
        FormTemplateVersionDO template = template().setStatus("DRAFT");
        when(templateVersionMapper.selectById(2801L)).thenReturn(template);

        MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPort.TargetResolution target =
                port.resolveTarget(binding(), List.of(
                        rule(11L, "PQC|PRESSURE|1|measuredValue", "measuredValue", 3, 1, "NUMBER")));

        assertFalse(target.isValid());
        assertEquals("PROCESS_INSPECTION_DYNAMIC_FORM_TEMPLATE_REQUIRED", target.getBlockerType());
        verify(instanceMapper, never()).selectById(any());
        verify(runtimeService, never()).saveDraft(any(), any(), any());
        verify(runtimeService, never()).submitInstance(any(), any(), any());
    }

    private static MesProRouteFlowProcessBatchRecordDO binding() {
        return MesProRouteFlowProcessBatchRecordDO.builder()
                .id(801L).routeId(401L).routeProcessId(501L).useType("BATCH")
                .formSlotType("PROCESS_INSPECTION").formBindingKey("PI_9901").formTemplateId(28L)
                .lastPublishedTemplateVersionId(2801L).lastPublishedTemplateVersionNo("V1")
                .recordCategory("INTERNAL_RECORD").validationProfile("INTERNAL_TRACE").ownerRoleKey("QUALITY")
                .recordCategorySnapshotHash("record-category-hash").slotConfigSnapshotHash("slot-hash").build();
    }

    private static FormTemplateVersionDO template() {
        return FormTemplateVersionDO.builder().id(2801L).tenantId(1L).templateId(28L)
                .templateName("过程检验记录").versionNo("V1").status("PUBLISHED")
                .recognizedSchemaJson("""
                        [{"fieldCode":"fieldMeasured","label":"实测值","fieldType":"number","required":true},
                         {"fieldCode":"fieldReviewedAt","label":"复核时间","fieldType":"datetime","required":true}]
                        """).build();
    }

    private static MesProBatchRecordCellLinkRuleDO rule(long id, String sourceCellKey, String sourceFieldCode,
                                                        int row, int column, String valueType) {
        MesProBatchRecordCellLinkRuleDO rule = new MesProBatchRecordCellLinkRuleDO();
        rule.setId(id);
        rule.setScopeType("FORM_TEMPLATE_VERSION");
        rule.setScopeId(2801L);
        rule.setSourceType("PQC_AGGREGATE_DETAIL");
        rule.setSourceCellKey(sourceCellKey);
        rule.setSourceFieldCode(sourceFieldCode);
        rule.setSourceValueType(valueType);
        rule.setTargetReportId("FORMTPL:2801");
        rule.setTargetRowIndex(row);
        rule.setTargetColumnIndex(column);
        rule.setTargetCellKey(row + ":" + column);
        rule.setTargetValueType(valueType);
        rule.setTemplateSnapshotHash("template-snapshot");
        rule.setRuleVersion(1L);
        rule.setEnabled(true);
        return rule;
    }

    private static MesProEdhrBatchExecutionTaskDO task() {
        return MesProEdhrBatchExecutionTaskDO.builder().id(902L).batchExecutionId(901L).nodeType("ROUTE_FORM")
                .routeProcessId(501L).processId(502L).formSlotType("PROCESS_INSPECTION")
                .formBindingKey("PI_9901").formTemplateId(28L).formTemplateVersionId(2801L)
                .formTemplateVersionNo("V1").formCenterInstanceId(9801L).routeBindingId(801L)
                .routeBindingSnapshotHash("binding-hash").slotConfigSnapshotHash("slot-hash").build();
    }

    private static MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPort.FieldWrite field(
            MesProBatchRecordCellLinkRuleDO rule, String targetFieldCode, Object value, String sourceValueHash) {
        return new MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPort.FieldWrite()
                .setRuleId(rule.getId()).setRuleVersion(rule.getRuleVersion())
                .setSourceCellKey(rule.getSourceCellKey()).setSourceFieldCode(rule.getSourceFieldCode())
                .setTargetFieldCode(targetFieldCode).setValue(value).setDisplayValue(String.valueOf(value))
                .setSourceValueHash(sourceValueHash);
    }
}
