package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateImpact;

import java.util.List;

public interface FormTemplateReferenceChecker {

    List<FormTemplateImpact> findPublishedPolicyImpacts(Long templateVersionId);

}
