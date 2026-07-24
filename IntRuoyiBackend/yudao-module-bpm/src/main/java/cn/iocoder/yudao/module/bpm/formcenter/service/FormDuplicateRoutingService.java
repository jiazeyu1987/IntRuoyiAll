package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.BusinessActionContext;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionResolution;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormDuplicateDecision;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormDuplicateStrategy;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormInstanceStatus;

import java.util.Objects;

public class FormDuplicateRoutingService {

    private final FormInstanceLifecycleService lifecycleService;
    private final FormActionInstanceStore instanceStore;

    public FormDuplicateRoutingService(FormInstanceLifecycleService lifecycleService,
            FormActionInstanceStore instanceStore) {
        this.lifecycleService = lifecycleService;
        this.instanceStore = instanceStore;
    }

    public FormDuplicateDecision resolveOrCreate(FormActionResolution resolution, BusinessActionContext context,
            Long applicantUserId, String idempotencyKey, FormDuplicateStrategy duplicateStrategy) {
        for (FormActionInstance instance : instanceStore.findSameBusinessAction(context)) {
            if (Objects.equals(instance.getApplicantUserId(), applicantUserId)
                    && instance.getStatus() == FormInstanceStatus.DRAFT) {
                return FormDuplicateDecision.existingDraft(instance);
            }
            if (duplicateStrategy == FormDuplicateStrategy.BLOCK_ACTIVE && isActive(instance)) {
                throw new FormCenterException(FormCenterErrorCode.DUPLICATE_APPLICATION_ACTIVE,
                        "Active duplicate application exists: " + instance.getInstanceCode());
            }
        }
        FormActionInstance created = lifecycleService.createDraft(resolution, context, applicantUserId, idempotencyKey);
        instanceStore.insert(created);
        return FormDuplicateDecision.created(created);
    }

    private boolean isActive(FormActionInstance instance) {
        return instance.getStatus() == FormInstanceStatus.IN_APPROVAL
                || instance.getStatus() == FormInstanceStatus.REWORKING;
    }

}
