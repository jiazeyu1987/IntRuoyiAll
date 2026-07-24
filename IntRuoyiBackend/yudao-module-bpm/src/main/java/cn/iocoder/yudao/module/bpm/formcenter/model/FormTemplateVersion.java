package cn.iocoder.yudao.module.bpm.formcenter.model;

import java.time.LocalDateTime;
import java.util.List;

public class FormTemplateVersion {

    private final Long templateId;
    private final Long versionId;
    private final String templateName;
    private final String versionNo;
    private final String sourceFileName;
    private final List<FormRecognizedField> recognizedFields;
    private final String remark;
    private FormTemplateStatus status;
    private String jimuSchema;
    private LocalDateTime updatedTime;

    public FormTemplateVersion(Long templateId, Long versionId, String templateName, String versionNo,
            String sourceFileName, List<FormRecognizedField> recognizedFields, String remark) {
        this.templateId = templateId;
        this.versionId = versionId;
        this.templateName = templateName;
        this.versionNo = versionNo;
        this.sourceFileName = sourceFileName;
        this.recognizedFields = List.copyOf(recognizedFields);
        this.remark = remark;
        this.status = FormTemplateStatus.DRAFT;
        this.updatedTime = LocalDateTime.now();
    }

    public void saveJimuSchema(String jimuSchema) {
        this.jimuSchema = jimuSchema;
        this.updatedTime = LocalDateTime.now();
    }

    public void publish() {
        this.status = FormTemplateStatus.PUBLISHED;
        this.updatedTime = LocalDateTime.now();
    }

    public void disable() {
        this.status = FormTemplateStatus.DISABLED;
        this.updatedTime = LocalDateTime.now();
    }

    public boolean isDisabled() {
        return status == FormTemplateStatus.DISABLED;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public Long getVersionId() {
        return versionId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getVersionNo() {
        return versionNo;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public List<FormRecognizedField> getRecognizedFields() {
        return recognizedFields;
    }

    public String getRemark() {
        return remark;
    }

    public FormTemplateStatus getStatus() {
        return status;
    }

    public String getJimuSchema() {
        return jimuSchema;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

}
