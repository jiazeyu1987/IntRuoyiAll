package cn.iocoder.yudao.module.bpm.formcenter.service;

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
public class FormTemplateUpgradeBusinessApprovalEffectExecutor implements BusinessApprovalEffectExecutor {

    public static final String EXECUTOR_CODE = "FORM_TEMPLATE_UPGRADE";
    public static final String PROCESS_DEFINITION_KEY = "form-template-upgrade-v1";
    private static final String DATA_DOMAIN = "FORM_CENTER";
    private static final String SYSTEM_CODE = "FORM_CENTER";
    private static final String OBJECT_TYPE = "FORM_TEMPLATE";
    private static final String ACTION_CODE = "UPGRADE";

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
        requireVersionWithStatus(context, FormTemplateStatus.DRAFT);
    }

    @Override
    public BusinessApprovalEffectResult executeDirect(BusinessApprovalContext context, BusinessApprovalRequest request) {
        requireVersionWithStatus(context, FormTemplateStatus.DRAFT);
        throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_MODE_INVALID,
                "Form template upgrade requires BPM approval");
    }

    @Override
    public BusinessApprovalEffectResult markPending(BusinessApprovalContext context, BusinessApprovalRequest request) {
        FormTemplateVersionDO version = requireVersionWithStatus(context, FormTemplateStatus.DRAFT);
        requireProcessInstanceId(request);
        updateStatus(version, FormTemplateStatus.PENDING_APPROVAL);
        return BusinessApprovalEffectResult.pending(FormTemplateStatus.PENDING_APPROVAL.name());
    }

    @Override
    public BusinessApprovalEffectResult executeApproved(BusinessApprovalContext context,
                                                        BusinessApprovalRequest request,
                                                        Long actorUserId) {
        FormTemplateVersionDO version = requireVersionWithStatus(context, FormTemplateStatus.PENDING_APPROVAL);
        updateStatus(version, FormTemplateStatus.PUBLISHED);
        return BusinessApprovalEffectResult.completed(FormTemplateStatus.PUBLISHED.name());
    }

    @Override
    public BusinessApprovalEffectResult reject(BusinessApprovalContext context,
                                               BusinessApprovalRequest request,
                                               Long actorUserId,
                                               String reason) {
        FormTemplateVersionDO version = requireVersionWithStatus(context, FormTemplateStatus.PENDING_APPROVAL);
        updateStatus(version, FormTemplateStatus.REJECTED);
        return BusinessApprovalEffectResult.rejected(FormTemplateStatus.REJECTED.name());
    }

    @Override
    public BusinessApprovalEffectResult cancel(BusinessApprovalContext context,
                                               BusinessApprovalRequest request,
                                               Long actorUserId,
                                               String reason) {
        FormTemplateVersionDO version = requireVersionWithStatus(context, FormTemplateStatus.PENDING_APPROVAL);
        updateStatus(version, FormTemplateStatus.REJECTED);
        return BusinessApprovalEffectResult.cancelled(FormTemplateStatus.REJECTED.name());
    }

    private FormTemplateVersionDO requireVersionWithStatus(BusinessApprovalContext context, FormTemplateStatus status) {
        requireContext(context);
        FormTemplateVersionDO version = templateVersionMapper.selectById(parseVersionId(context));
        if (version == null || !Objects.equals(version.getTenantId(), context.getTenantId())) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "Form template upgrade target version not found");
        }
        if (!status.name().equals(version.getStatus())) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "Form template upgrade target status invalid: " + version.getStatus());
        }
        return version;
    }

    private void requireContext(BusinessApprovalContext context) {
        if (context == null
                || !DATA_DOMAIN.equals(context.getDataDomain())
                || !SYSTEM_CODE.equals(context.getSystemCode())
                || !OBJECT_TYPE.equals(context.getObjectType())
                || !ACTION_CODE.equals(context.getActionCode())
                || !FormTemplateStatus.DRAFT.name().equals(context.getObjectState())) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "Form template upgrade approval context is invalid");
        }
        parseVersionId(context);
    }

    private Long parseVersionId(BusinessApprovalContext context) {
        try {
            return Long.valueOf(context.getObjectId());
        } catch (RuntimeException ex) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID,
                    "Form template upgrade target version id is invalid");
        }
    }

    private String requireProcessInstanceId(BusinessApprovalRequest request) {
        if (request == null || request.getProcessInstanceId() == null || request.getProcessInstanceId().isBlank()) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_PROCESS_NOT_STARTED,
                    "Form template upgrade approval process instance is missing");
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
                    "Form template upgrade status update failed: " + version.getId());
        }
        version.setStatus(status.name());
    }

}
