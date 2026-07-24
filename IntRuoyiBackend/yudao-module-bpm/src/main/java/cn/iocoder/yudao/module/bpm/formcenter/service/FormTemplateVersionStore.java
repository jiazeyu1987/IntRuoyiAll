package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateVersion;

public interface FormTemplateVersionStore {

    Long nextTemplateId();

    Long nextVersionId();

    void insert(FormTemplateVersion version);

    void update(FormTemplateVersion version);

    FormTemplateVersion findVersion(Long templateId, String versionNo);

}
