package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;

public interface MesFrontlineParameterAuditService {

    MesFrontlineParameterAuditResult resolveAndApply(MesProFrontlineFeedbackSubmitReqVO reqVO);
}
