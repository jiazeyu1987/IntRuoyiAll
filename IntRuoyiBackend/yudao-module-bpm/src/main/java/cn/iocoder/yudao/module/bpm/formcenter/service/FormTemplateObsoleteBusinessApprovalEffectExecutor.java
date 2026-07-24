package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalEffectResult;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalEffectExecutor;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalErrorCode;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateStatus;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class FormTemplateObsoleteBusinessApprovalEffectExecutor implements BusinessApprovalEffectExecutor {

    public static final String EXECUTOR_CODE = "FORM_TEMPLATE_OBSOLETE";
    public static final String PROCESS_DEFINITION_KEY = "form-template-obsolete-v1";
    private static final String DATA_DOMAIN = "FORM_CENTER";
    private static final String SYSTEM_CODE = "FORM_CENTER";
    private static final String OBJECT_TYPE = "FORM_TEMPLATE";
    private static final String ACTION_CODE = "OBSOLETE";

    @Resource
    private FormTemplateVersionMapper templateVersionMapper;

    @Override
    public String getExecutorCode() {
        return EXECUTOR_CODE;
    }

    @Override
    public String getBpmProcessDefinitionKey() {
        return PROCESS_DEFINITION_KEY;
    }

    @Override
    public void precheck(BusinessApprovalContext context) {
        requireObsoletableVersion(context);
    }

    @Override
    public BusinessApprovalEffectResult executeDirect(BusinessApprovalContext context, BusinessApprovalRequest request) {
        requireObsoletableVersion(context);
        throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_MODE_INVALID,
                "Form template obsolete requires BPM approval");
    }

    @Override
    public BusinessApprovalEffectResult markPending(BusinessApprovalContext context, BusinessApprovalRequest request) {
        FormTemplateVersionDO version = requireObsoletableVersion(context);
        requireProcessInstanceId(request);
        updateStatus(version, FormTemplateStatus.PENDING_APPROVAL);
        return BusinessApprovalEffectResult.pending(FormTemplateStatus.PENDING_APPROVAL.name());
    }

    @Override
    public BusinessApprovalEffectResult executeApproved(BusinessApprovalContext context,
                                                        BusinessApprovalRequest request,
                                                        Long actorUserId) {
        FormTemplateVersionDO version = requireVersionWithStatus(context, FormTemplateStatus.PENDING_APPROVAL);
        updateStatus(version, FormTemplateStatus.OBSOLETE);
        return BusinessApprovalEffectResult.completed(FormTemplateStatus.OBSOLETE.name());
    }

    @Override
    public BusinessApprovalEffectResult reject(BusinessApprovalContext context,
                                               BusinessApprovalRequest request,
                                               Long actorUserId,
                                               String reason) {
        FormTemplateVersionDO version = requireVersionWithStatus(context, FormTemplateStatus.PENDING_APPROVAL);
        FormTemplateStatus originalStatus = requireOriginalStatus(context);
        updateStatus(version, originalStatus);
        return BusinessApprovalEffectResult.rejected(originalStatus.name());
    }

    @Override
    public BusinessApprovalEffectResult cancel(BusinessApprovalContext context,
                                               BusinessApprovalRequest request,
                                               Long actorUserId,
                                               String reason) {
        FormTemplateVersionDO version = requireVersionWithStatus(context, FormTemplateStatus.PENDING_APPROVAL);
        FormTemplateStatus originalStatus = requireOriginalStatus(context);
        updateStatus(version, originalStatus);
        return BusinessApprovalEffectResult.cancelled(originalStatus.name());
    }

    private FormTemplateVersionDO requireObsoletableVersion(BusinessApprovalContext context) {
        requireContext(context);
        FormTemplateVersionDO version = requireVersion(context);
        if (FormTemplateStatus.PENDING_APPROVAL.name().equals(version.getStatus())
                || FormTemplateStatus.OBSOLETE.name().equals(version.getStatus())) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "Form template obsolete target status invalid: " + version.getStatus());
        }
        if (!Objects.equals(version.getStatus(), context.getObjectState())) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "Form template obsolete object state mismatched: " + context.getObjectState());
        }
        return version;
    }

    private FormTemplateVersionDO requireVersionWithStatus(BusinessApprovalContext context, FormTemplateStatus status) {
        requireContext(context);
        FormTemplateVersionDO version = requireVersion(context);
        if (!status.name().equals(version.getStatus())) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "Form template obsolete target status invalid: " + version.getStatus());
        }
        return version;
    }

    private FormTemplateVersionDO requireVersion(BusinessApprovalContext context) {
        FormTemplateVersionDO version = templateVersionMapper.selectById(parseVersionId(context));
        if (version == null || !Objects.equals(version.getTenantId(), context.getTenantId())) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "Form template obsolete target version not found");
        }
        if (!Objects.equals(version.getVersionNo(), context.getObjectVersion())) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "Form template obsolete target version mismatched: " + context.getObjectVersion());
        }
        return version;
    }

    private void requireContext(BusinessApprovalContext context) {
        if (context == null
                || !DATA_DOMAIN.equals(context.getDataDomain())
                || !SYSTEM_CODE.equals(context.getSystemCode())
                || !OBJECT_TYPE.equals(context.getObjectType())
                || !ACTION_CODE.equals(context.getActionCode())
                || StrUtil.isBlank(context.getReason())) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "Form template obsolete approval context is invalid");
        }
        requireOriginalStatus(context);
        parseVersionId(context);
    }

    private FormTemplateStatus requireOriginalStatus(BusinessApprovalContext context) {
        try {
            FormTemplateStatus status = FormTemplateStatus.valueOf(context.getObjectState());
            if (status == FormTemplateStatus.PENDING_APPROVAL || status == FormTemplateStatus.OBSOLETE) {
                throw new IllegalArgumentException("terminal or pending state cannot be original obsolete state");
            }
            return status;
        } catch (RuntimeException ex) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "Form template obsolete original status is invalid");
        }
    }

    private Long parseVersionId(BusinessApprovalContext context) {
        try {
            return Long.valueOf(context.getObjectId());
        } catch (RuntimeException ex) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "Form template obsolete target version id is invalid");
        }
    }

    private String requireProcessInstanceId(BusinessApprovalRequest request) {
        if (request == null || StrUtil.isBlank(request.getProcessInstanceId())) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_PROCESS_NOT_STARTED,
                    "Form template obsolete approval process instance is missing");
        }
        return request.getProcessInstanceId();
    }

    private void updateStatus(FormTemplateVersionDO version, FormTemplateStatus status) {
        FormTemplateVersionDO update = new FormTemplateVersionDO();
        update.setId(version.getId());
        update.setStatus(status.name());
        int updated = templateVersionMapper.updateById(update);
        if (updated != 1) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "Form template obsolete status update failed: " + version.getId());
        }
        version.setStatus(status.name());
    }

}
