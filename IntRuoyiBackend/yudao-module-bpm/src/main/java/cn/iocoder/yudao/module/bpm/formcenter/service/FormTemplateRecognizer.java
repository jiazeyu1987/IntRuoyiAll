package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateImportCommand;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTemplateRecognition;

public interface FormTemplateRecognizer {

    FormTemplateRecognition recognize(FormTemplateImportCommand command);

}
