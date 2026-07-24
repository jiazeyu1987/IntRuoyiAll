package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateImpactCheckResult;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateImportCommand;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateRecognition;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateStatus;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateVersion;

public class FormTemplateLifecycleService {

    private final FormTemplateRecognizer recognizer;
    private final FormTemplateReferenceChecker referenceChecker;
    private final FormTemplateVersionStore templateVersionStore;

    public FormTemplateLifecycleService(FormTemplateRecognizer recognizer,
            FormTemplateReferenceChecker referenceChecker, FormTemplateVersionStore templateVersionStore) {
        this.recognizer = recognizer;
        this.referenceChecker = referenceChecker;
        this.templateVersionStore = templateVersionStore;
    }

    public FormTemplateVersion importDoc(FormTemplateImportCommand command) {
        if (!isDocSource(command.getSourceFileName())) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_TYPE_UNSUPPORTED,
                    "Only doc/docx template source files are supported: " + command.getSourceFileName());
        }
        if (command.getSourceBytes().length == 0) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                    "Template source file is empty: " + command.getSourceFileName());
        }
        FormTemplateRecognition recognition = recognizer.recognize(command);
        if (!recognition.isSuccess() || recognition.getFields().isEmpty()) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_RECOGNITION_FAILED,
                    "Template recognition failed: " + recognition.getFailureReason());
        }
        FormTemplateVersion version = new FormTemplateVersion(templateVersionStore.nextTemplateId(),
                templateVersionStore.nextVersionId(), command.getTemplateName(), command.getVersionNo(),
                command.getSourceFileName(), recognition.getFields(), command.getRemark());
        templateVersionStore.insert(version);
        return version;
    }

    public void saveJimuSchema(Long templateId, String versionNo, String jimuSchema) {
        FormTemplateVersion version = findVersion(templateId, versionNo);
        if (version.getStatus() == FormTemplateStatus.PUBLISHED) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_VERSION_IMMUTABLE,
                    "Published template version cannot be modified in place: " + templateId + "/" + versionNo);
        }
        version.saveJimuSchema(jimuSchema);
        templateVersionStore.update(version);
    }

    public void publish(Long templateId, String versionNo) {
        FormTemplateVersion version = findVersion(templateId, versionNo);
        version.publish();
        templateVersionStore.update(version);
    }

    public FormTemplateImpactCheckResult disableImpactCheck(Long templateId, String versionNo) {
        FormTemplateVersion version = findVersion(templateId, versionNo);
        return FormTemplateImpactCheckResult.of(referenceChecker.findPublishedPolicyImpacts(version.getVersionId()));
    }

    public void disable(Long templateId, String versionNo) {
        FormTemplateImpactCheckResult impact = disableImpactCheck(templateId, versionNo);
        if (impact.isBlocked()) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_POOL_IMPACT_BLOCKED,
                    "Template version is referenced by published policies: " + templateId + "/" + versionNo);
        }
        FormTemplateVersion version = findVersion(templateId, versionNo);
        version.disable();
        templateVersionStore.update(version);
    }

    public FormTemplateVersion findVersion(Long templateId, String versionNo) {
        FormTemplateVersion version = templateVersionStore.findVersion(templateId, versionNo);
        if (version == null) {
            throw new FormCenterException(FormCenterErrorCode.TEMPLATE_SOURCE_INVALID,
                    "Template version not found: " + templateId + "/" + versionNo);
        }
        return version;
    }

    private boolean isDocSource(String fileName) {
        return fileName != null && (fileName.endsWith(".doc") || fileName.endsWith(".docx"));
    }

}
