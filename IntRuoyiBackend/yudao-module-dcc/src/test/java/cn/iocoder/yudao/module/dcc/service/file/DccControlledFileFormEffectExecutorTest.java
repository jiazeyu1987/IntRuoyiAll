package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.formcenter.model.BusinessActionContext;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormBpmBinding;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionPolicy;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionResolution;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicyType;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectPrecheck;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectResult;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileFormEffectExecutorTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileWorkflowService workflowService;

    @InjectMocks
    private DccControlledFileFormEffectExecutor executor;

    @Test
    void executeDccUpload_submitsControlledFileWithoutStartingSecondApproval() {
        when(workflowService.submitControlledFileWithoutApproval(eq(99L), any(), eq("form-process-1"),
                eq("IDEM-DCC-1"))).thenReturn(900L);
        FormActionInstance instance = dccUploadInstance();
        instance.setBpmBinding(new FormBpmBinding("form-process-1", "task-1"));

        FormBusinessEffectResult result = executor.execute(instance, "IDEM-DCC-1");

        assertTrue(result.isSuccess());
        assertEquals("900", result.getResultRef());
        ArgumentCaptor<DccControlledFileSubmitReqVO> submitCaptor =
                ArgumentCaptor.forClass(DccControlledFileSubmitReqVO.class);
        verify(workflowService).submitControlledFileWithoutApproval(eq(99L), submitCaptor.capture(),
                eq("form-process-1"), eq("IDEM-DCC-1"));
        DccControlledFileSubmitReqVO submitReqVO = submitCaptor.getValue();
        assertEquals(10L, submitReqVO.getCategoryId());
        assertEquals("session-1", submitReqVO.getSessionId());
        assertEquals("UT-SOURCE", submitReqVO.getOriginalUploadTicket());
        assertEquals("UT-SOURCE", submitReqVO.getSourceUploadTicket());
        assertEquals("UT-DRAWING", submitReqVO.getDrawingPdfUploadTicket());
        assertEquals("SOP-001.docx", submitReqVO.getSourceFileName());
        assertEquals(21L, submitReqVO.getDirectoryId());
        assertEquals("SOP-001", submitReqVO.getFileName());
        assertEquals("SOP-001", submitReqVO.getFileNumber());
        assertEquals("V1.0", submitReqVO.getVersionNo());
        assertEquals(LocalDate.of(2026, 7, 18), submitReqVO.getEffectiveDate());
        assertEquals(List.of(201L, 202L), submitReqVO.getSelectedSignoffUserIds());
    }

    @Test
    void executeDccUpload_wrongBusinessContextFailsBeforeWorkflow() {
        FormActionInstance instance = instance("MES", "BATCH_RECORD", "OPEN_FORM", "DCC_UPLOAD");
        instance.setFormData(dccFormData());

        FormBusinessEffectResult result = executor.execute(instance, "IDEM-DCC-2");

        assertFalse(result.isSuccess());
        assertEquals("DCC_UPLOAD executor only accepts DCC CONTROLLED_FILE UPLOAD actions", result.getFailureReason());
        verify(workflowService, never()).submitControlledFileWithoutApproval(any(), any());
    }

    @Test
    void lifecycleAdapterSupportsOnlyDccUploadContext() {
        assertTrue(executor.supports(dccUploadInstance()));
        assertFalse(executor.supports(instance("MES", "BATCH_RECORD", "OPEN_FORM", "DCC_UPLOAD")));
    }

    @Test
    void lifecyclePreflightValidatesDccUploadFormBeforeBpmStarts() {
        FormBusinessEffectPrecheck precheck = executor.preflight(dccUploadInstance());

        assertTrue(precheck.isPassed());
        verify(workflowService, never()).submitControlledFileWithoutApproval(any(), any());
    }

    @Test
    void lifecyclePreflightRejectsWrongContextAndMissingRequiredField() {
        FormActionInstance wrongContext = instance("MES", "BATCH_RECORD", "OPEN_FORM", "DCC_UPLOAD");
        wrongContext.setFormData(dccFormData());
        FormBusinessEffectPrecheck wrongContextResult = executor.preflight(wrongContext);

        assertFalse(wrongContextResult.isPassed());
        assertEquals("DCC_UPLOAD lifecycle adapter only accepts DCC CONTROLLED_FILE UPLOAD actions",
                wrongContextResult.getFailureReason());

        FormActionInstance missingFileName = dccUploadInstance();
        Map<String, Object> formData = new LinkedHashMap<>(missingFileName.getFormData());
        formData.remove("fileName");
        missingFileName.setFormData(formData);

        FormBusinessEffectPrecheck missingFieldResult = executor.preflight(missingFileName);

        assertFalse(missingFieldResult.isPassed());
        assertEquals("Missing DCC upload form field: fileName", missingFieldResult.getFailureReason());
        verify(workflowService, never()).submitControlledFileWithoutApproval(any(), any());
    }

    private FormActionInstance dccUploadInstance() {
        FormActionInstance instance = instance("DCC", "CONTROLLED_FILE", "UPLOAD", "DCC_UPLOAD");
        instance.setFormData(dccFormData());
        return instance;
    }

    private FormActionInstance instance(String systemCode, String objectType, String actionCode, String executorCode) {
        FormActionPolicy policy = FormActionPolicy.builder()
                .policyId(20L)
                .tenantId(122L)
                .dataDomain(systemCode)
                .systemCode(systemCode)
                .objectType(objectType)
                .actionCode(actionCode)
                .objectState("DRAFT")
                .policyType(FormPolicyType.REQUIRED)
                .bpmProcessKey("form-change-approval")
                .effectExecutorCode(executorCode)
                .status(FormActionPolicy.STATUS_PUBLISHED)
                .build();
        return new FormActionInstance("FCI-1", FormActionResolution.from(policy),
                BusinessActionContext.builder()
                        .tenantId(122L)
                        .dataDomain(systemCode)
                        .systemCode(systemCode)
                        .objectType(objectType)
                        .objectId("SOP-001")
                        .objectVersion("V1.0")
                        .actionCode(actionCode)
                        .objectState("DRAFT")
                        .build(),
                99L, "IDEM-DCC-1");
    }

    private Map<String, Object> dccFormData() {
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("categoryId", 10L);
        formData.put("directoryId", 21L);
        formData.put("sessionId", "session-1");
        formData.put("originalUploadTicket", "UT-SOURCE");
        formData.put("sourceUploadTicket", "UT-SOURCE");
        formData.put("sourceFileName", "SOP-001.docx");
        formData.put("drawingPdfUploadTicket", "UT-DRAWING");
        formData.put("fileName", "SOP-001");
        formData.put("fileNumber", "SOP-001");
        formData.put("productMasterId", 5000L);
        formData.put("productCode", "PRD2026071801");
        formData.put("needTraining", Boolean.FALSE);
        formData.put("selectedSignoffUserIds", List.of(201L, 202L));
        formData.put("processType", "CONTROLLED_FILE");
        formData.put("changeType", "NEW");
        formData.put("versionNo", "V1.0");
        formData.put("effectiveDate", "2026-07-18");
        formData.put("remark", "form center approved");
        return formData;
    }
}
