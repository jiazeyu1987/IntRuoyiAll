package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormBpmStartRequest;

public interface FormBpmStarter {

    String start(Long userId, FormBpmStartRequest request);

}
