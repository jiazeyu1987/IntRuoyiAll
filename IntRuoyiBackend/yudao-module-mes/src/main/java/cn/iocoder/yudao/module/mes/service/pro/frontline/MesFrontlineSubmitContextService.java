package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlineSubmitContextReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlineSubmitContextRespVO;

public interface MesFrontlineSubmitContextService {

    MesFrontlineSubmitContextRespVO resolve(Long loginUserId, MesFrontlineSubmitContextReqVO reqVO);
}
