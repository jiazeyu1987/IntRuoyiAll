package cn.iocoder.yudao.module.bpm.formcenter.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FormTemplateVersionRef {

    private final Long versionId;
    private final String templateCode;
    private final String versionNo;
    private final String templateName;

    @JsonCreator
    public FormTemplateVersionRef(@JsonProperty("versionId") Long versionId,
            @JsonProperty("templateCode") String templateCode,
            @JsonProperty("versionNo") String versionNo,
            @JsonProperty("templateName") String templateName) {
        this.versionId = versionId;
        this.templateCode = templateCode;
        this.versionNo = versionNo;
        this.templateName = templateName;
    }

    public static FormTemplateVersionRef of(Long versionId, String templateCode, String versionNo, String templateName) {
        return new FormTemplateVersionRef(versionId, templateCode, versionNo, templateName);
    }

    public Long getVersionId() {
        return versionId;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public String getVersionNo() {
        return versionNo;
    }

    public String getTemplateName() {
        return templateName;
    }

}
