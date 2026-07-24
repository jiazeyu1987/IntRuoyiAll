package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.bpm.formcenter.model.BusinessActionContext;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormControlledActionApprovalOutcome;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectExecutor;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectPrecheck;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectResult;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormControlledActionLifecycleAdapter;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class DccControlledFileFormEffectExecutor
        implements FormBusinessEffectExecutor, FormControlledActionLifecycleAdapter {

    public static final String EXECUTOR_CODE = "DCC_UPLOAD";
    private static final String LIFECYCLE_CONTEXT_ERROR =
            "DCC_UPLOAD lifecycle adapter only accepts DCC CONTROLLED_FILE UPLOAD actions";

    private final DccControlledFileWorkflowService workflowService;

    public DccControlledFileFormEffectExecutor(@Lazy DccControlledFileWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Override
    public String getExecutorCode() {
        return EXECUTOR_CODE;
    }

    @Override
    public FormBusinessEffectResult execute(FormActionInstance instance, String idempotencyKey) {
        if (!supports(instance)) {
            return FormBusinessEffectResult.failure("DCC_UPLOAD executor only accepts DCC CONTROLLED_FILE UPLOAD actions");
        }
        try {
            String approvalProcessInstanceId = instance.getBpmBinding() == null
                    ? null : instance.getBpmBinding().getProcessInstanceId();
            Long fileId = workflowService.submitControlledFileWithoutApproval(instance.getApplicantUserId(),
                    toSubmitReqVO(instance.getFormData()), approvalProcessInstanceId, idempotencyKey);
            return FormBusinessEffectResult.success(String.valueOf(fileId));
        } catch (IllegalArgumentException ex) {
            return FormBusinessEffectResult.failure(ex.getMessage());
        }
    }

    @Override
    public boolean supports(FormActionInstance instance) {
        return instance != null && isDccUpload(instance.getBusinessContext());
    }

    @Override
    public FormBusinessEffectPrecheck preflight(FormActionInstance instance) {
        if (!supports(instance)) {
            return FormBusinessEffectPrecheck.fail(LIFECYCLE_CONTEXT_ERROR);
        }
        try {
            toSubmitReqVO(instance.getFormData());
            return FormBusinessEffectPrecheck.pass();
        } catch (IllegalArgumentException ex) {
            return FormBusinessEffectPrecheck.fail(ex.getMessage());
        }
    }

    @Override
    public void onPendingApprovalStarted(FormActionInstance instance) {
        requirePreflightPassed(instance);
    }

    @Override
    public void onPendingApprovalClosed(FormActionInstance instance, FormControlledActionApprovalOutcome outcome,
            String reason) {
        requirePreflightPassed(instance);
        if (outcome == null) {
            throw new IllegalArgumentException("DCC_UPLOAD lifecycle outcome is required");
        }
    }

    private void requirePreflightPassed(FormActionInstance instance) {
        FormBusinessEffectPrecheck precheck = preflight(instance);
        if (!precheck.isPassed()) {
            throw new IllegalArgumentException(precheck.getFailureReason());
        }
    }

    private boolean isDccUpload(BusinessActionContext context) {
        return context != null
                && "DCC".equals(context.getSystemCode())
                && "CONTROLLED_FILE".equals(context.getObjectType())
                && "UPLOAD".equals(context.getActionCode());
    }

    private DccControlledFileSubmitReqVO toSubmitReqVO(Map<String, Object> formData) {
        DccControlledFileSubmitReqVO reqVO = new DccControlledFileSubmitReqVO();
        reqVO.setCategoryId(requiredLong(formData, "categoryId"));
        reqVO.setSessionId(optionalString(formData, "sessionId"));
        reqVO.setOriginalUploadTicket(optionalString(formData, "originalUploadTicket"));
        reqVO.setSourceUploadTicket(optionalString(formData, "sourceUploadTicket"));
        reqVO.setDrawingPdfUploadTicket(optionalString(formData, "drawingPdfUploadTicket"));
        reqVO.setOriginalFileId(optionalLong(formData, "originalFileId"));
        reqVO.setSourceFileId(optionalLong(formData, "sourceFileId"));
        reqVO.setSourceFileName(optionalString(formData, "sourceFileName"));
        reqVO.setDrawingPdfFileId(optionalLong(formData, "drawingPdfFileId"));
        reqVO.setProductMasterId(optionalLong(formData, "productMasterId"));
        reqVO.setProductCode(optionalString(formData, "productCode"));
        reqVO.setNeedTraining(optionalBoolean(formData, "needTraining"));
        reqVO.setProcessType(optionalString(formData, "processType"));
        reqVO.setChangeType(requiredString(formData, "changeType"));
        reqVO.setSelectedSignoffUserIds(optionalLongList(formData, "selectedSignoffUserIds"));
        reqVO.setFileName(requiredString(formData, "fileName"));
        reqVO.setFileNumber(requiredString(formData, "fileNumber"));
        reqVO.setDirectoryId(requiredLong(formData, "directoryId"));
        reqVO.setVersionNo(requiredString(formData, "versionNo"));
        reqVO.setEffectiveDate(requiredDate(formData, "effectiveDate"));
        reqVO.setRemark(optionalString(formData, "remark"));
        return reqVO;
    }

    private String requiredString(Map<String, Object> formData, String key) {
        String value = optionalString(formData, key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing DCC upload form field: " + key);
        }
        return value;
    }

    private String optionalString(Map<String, Object> formData, String key) {
        Object value = formData.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Long requiredLong(Map<String, Object> formData, String key) {
        Long value = optionalLong(formData, key);
        if (value == null) {
            throw new IllegalArgumentException("Missing DCC upload form field: " + key);
        }
        return value;
    }

    private Long optionalLong(Map<String, Object> formData, String key) {
        Object value = formData.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private Boolean optionalBoolean(Map<String, Object> formData, String key) {
        Object value = formData.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.valueOf(String.valueOf(value));
    }

    private LocalDate requiredDate(Map<String, Object> formData, String key) {
        String value = requiredString(formData, key);
        return LocalDate.parse(value);
    }

    private List<Long> optionalLongList(Map<String, Object> formData, String key) {
        Object value = formData.get(key);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> values)) {
            throw new IllegalArgumentException("DCC upload form field must be a list: " + key);
        }
        return values.stream()
                .map(item -> {
                    if (item instanceof Number number) {
                        return number.longValue();
                    }
                    return Long.valueOf(String.valueOf(item));
                })
                .toList();
    }

}
