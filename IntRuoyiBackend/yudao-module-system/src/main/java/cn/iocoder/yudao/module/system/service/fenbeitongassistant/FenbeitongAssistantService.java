package cn.iocoder.yudao.module.system.service.fenbeitongassistant;

import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthFenbeitongAssistantStatusRespVO;

public interface FenbeitongAssistantService {

    AuthFenbeitongAssistantStatusRespVO getStatus();

    AuthFenbeitongAssistantStatusRespVO start();

}
