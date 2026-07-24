package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.BusinessActionContext;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionResolution;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormBpmBinding;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormInstancePermissionCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormInstanceStatus;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormSnapshot;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormSnapshotType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class FormInstanceLifecycleService {

    private final AtomicLong sequence = new AtomicLong(1);

    public FormActionInstance createDraft(FormActionResolution resolution, BusinessActionContext context,
            Long applicantUserId, String idempotencyKey) {
        String instanceCode = "FCI-" + context.getTenantId() + "-" + sequence.getAndIncrement();
        FormActionInstance instance = new FormActionInstance(instanceCode, resolution, context, applicantUserId,
                idempotencyKey);
        instance.grantInstancePermissions(applicantUserId, Set.of(
                FormInstancePermissionCode.VIEW,
                FormInstancePermissionCode.EDIT_DRAFT,
                FormInstancePermissionCode.SUBMIT,
                FormInstancePermissionCode.ABANDON));
        return instance;
    }

    public FormSnapshot saveDraft(FormActionInstance instance, Map<String, Object> formData,
            List<String> attachmentIds) {
        requireStatus(instance, FormInstanceStatus.DRAFT, FormInstanceStatus.REWORKING);
        FormSnapshot snapshot = new FormSnapshot(FormSnapshotType.DRAFT, formData, attachmentIds,
                instance.getBusinessContext());
        instance.addSnapshot(snapshot);
        return snapshot;
    }

    public FormSnapshot submit(FormActionInstance instance, Map<String, Object> formData, String processInstanceId) {
        requireStatus(instance, FormInstanceStatus.DRAFT, FormInstanceStatus.REWORKING);
        FormSnapshot snapshot = new FormSnapshot(FormSnapshotType.SUBMIT, formData, List.of(),
                instance.getBusinessContext());
        instance.addSnapshot(snapshot);
        instance.setBpmBinding(new FormBpmBinding(processInstanceId, null));
        instance.setStatus(FormInstanceStatus.IN_APPROVAL);
        return snapshot;
    }

    public void markReworkRequired(FormActionInstance instance, String taskId) {
        instance.setBpmBinding(new FormBpmBinding(instance.getBpmBinding().getProcessInstanceId(), taskId));
        instance.setStatus(FormInstanceStatus.REWORKING);
    }

    public FormSnapshot reworkSubmit(FormActionInstance instance, Map<String, Object> formData) {
        requireStatus(instance, FormInstanceStatus.REWORKING);
        FormSnapshot snapshot = new FormSnapshot(FormSnapshotType.REWORK_SUBMIT, formData, List.of(),
                instance.getBusinessContext());
        instance.addSnapshot(snapshot);
        instance.setStatus(FormInstanceStatus.IN_APPROVAL);
        return snapshot;
    }

    public void abandon(FormActionInstance instance) {
        requireStatus(instance, FormInstanceStatus.DRAFT, FormInstanceStatus.REWORKING, FormInstanceStatus.REJECTED);
        instance.setStatus(FormInstanceStatus.ABANDONED);
    }

    private void requireStatus(FormActionInstance instance, FormInstanceStatus... allowedStatuses) {
        for (FormInstanceStatus allowedStatus : allowedStatuses) {
            if (instance.getStatus() == allowedStatus) {
                return;
            }
        }
        throw new FormCenterException(FormCenterErrorCode.FORM_INSTANCE_STATUS_INVALID,
                "Form instance status invalid: " + instance.getStatus());
    }

}
