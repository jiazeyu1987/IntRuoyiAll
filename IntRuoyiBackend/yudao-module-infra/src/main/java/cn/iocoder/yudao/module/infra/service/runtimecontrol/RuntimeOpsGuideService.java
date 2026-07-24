package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlWizardRecommendationReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlWizardRecommendationRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlWizardScenarioRespVO;

import java.util.List;

public interface RuntimeOpsGuideService {

    List<RuntimeControlWizardScenarioRespVO> getScenarios();

    RuntimeControlWizardRecommendationRespVO recommend(RuntimeControlWizardRecommendationReqVO reqVO);
}
