package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.*;

public interface FormCenterRuntimeService {

    PageResult<FormCenterTemplateRespVO> getTemplatePool(FormCenterTemplatePoolPageReqVO reqVO);

    FormCenterTemplateRespVO getTemplateVersion(Long templateId, String versionNo);

    PageResult<FormPolicyRespVO> getPolicyPage(FormPolicyPageReqVO reqVO);

    FormCenterTemplateImportRespVO importDoc(FormCenterTemplateImportReqVO reqVO, Long applicantUserId);

    FormPolicyRespVO savePolicy(FormPolicySaveReqVO reqVO);

    void publishPolicy(Long policyId);

    FormPolicyRespVO switchPolicyApprovalMode(Long policyId, FormPolicySwitchApprovalModeReqVO reqVO);

    void saveJimuSchema(Long templateId, String versionNo, FormCenterTemplateJimuSchemaReqVO reqVO);

    FormTemplateFillRuleAutoDetectRespVO autoDetectTemplateFillRules(Long templateId, String versionNo);

    void publishTemplate(Long templateId, String versionNo);

    void disableTemplate(Long templateId, String versionNo);

    void enableTemplate(Long templateId, String versionNo);

    void obsoleteTemplate(Long templateId, String versionNo);

    FormTemplateObsoleteRespVO submitTemplateObsoleteRequest(Long templateId, String versionNo,
            FormTemplateObsoleteReqVO reqVO, Long applicantUserId);

    FormTemplateObsoletePendingRespVO findTemplateObsoletePendingRequest(Long templateId, String versionNo,
            Long currentUserId);

    void withdrawTemplateObsoleteRequest(Long templateId, String versionNo, String reason, Long currentUserId);

    byte[] getTemplateSourceFile(Long templateId, String versionNo);

    FormActionResolutionRespVO resolveAction(BusinessActionContextReqVO reqVO);

    FormInstanceRespVO findActiveBusinessAction(BusinessActionContextReqVO reqVO);

    FormInstanceRespVO createInstance(FormInstanceCreateReqVO reqVO, Long userId);

    void saveDraft(Long instanceId, FormInstanceDraftReqVO reqVO, Long userId);

    FormInstanceRespVO submitInstance(Long instanceId, FormInstanceSubmitReqVO reqVO, Long userId);

    void reworkSubmitInstance(Long instanceId, FormInstanceSubmitReqVO reqVO, Long userId);

    void abandonInstance(Long instanceId, Long userId);

    void onBpmTaskCreated(FormBpmTaskCreatedReqVO reqVO);

    void onBpmTaskCompleted(FormBpmTaskCompletedReqVO reqVO);

    void onBpmReworkRequired(FormBpmReworkRequiredReqVO reqVO);

    void onBpmProcessRejected(FormBpmProcessRejectedReqVO reqVO);

    void onBpmProcessCancelled(FormBpmProcessCancelledReqVO reqVO);

    FormEffectExecutionRespVO onBpmProcessApproved(FormBpmProcessApprovedReqVO reqVO);

    java.util.List<FormInstanceSnapshotRespVO> getInstanceSnapshots(Long instanceId);

    PageResult<FormEffectExecutionRespVO> getPendingEffects(FormEffectPendingPageReqVO reqVO);

    FormEffectExecutionRespVO retryEffect(Long instanceId);

}
