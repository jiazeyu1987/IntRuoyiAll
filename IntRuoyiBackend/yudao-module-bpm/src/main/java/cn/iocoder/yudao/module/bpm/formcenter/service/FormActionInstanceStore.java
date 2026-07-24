package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.BusinessActionContext;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;

import java.util.List;

public interface FormActionInstanceStore {

    void insert(FormActionInstance instance);

    void update(FormActionInstance instance);

    List<FormActionInstance> findSameBusinessAction(BusinessActionContext context);

}
