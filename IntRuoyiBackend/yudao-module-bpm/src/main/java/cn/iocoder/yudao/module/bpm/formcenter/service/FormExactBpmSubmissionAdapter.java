package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import java.util.Map;
import java.util.List;

/** An opted-in business adapter verifies its persisted preparation; HTTP definition fields are never trusted. */
public interface FormExactBpmSubmissionAdapter extends FormControlledActionLifecycleAdapter {
    record PersistedDraft(Long tenantId, Long formInstanceId, String formInstanceCode, Long applicantUserId,
            String status, String processInstanceId, String formDataJson) { }
    record VerifiedSubmission(String processDefinitionKey, String processDefinitionId, String formDataJson,
            Map<String, List<Long>> startUserSelectAssignees, Map<String, List<Long>> approveUserSelectAssignees) { }
    VerifiedSubmission verifyExactSubmission(PersistedDraft persisted, FormActionInstance requested,
            Long actorUserId);
}
