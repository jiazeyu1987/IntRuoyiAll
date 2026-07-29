package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitRespVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface MesProFrontlineFeedbackSubmitService {

    MesProFrontlineFeedbackSubmitRespVO submit(@Valid @NotNull MesProFrontlineFeedbackSubmitReqVO reqVO);

}
