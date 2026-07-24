package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryTrainingRuleSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryTrainingRuleDO;

import java.util.List;

public interface DccCategoryTrainingRuleAdminService {

    List<DccFileCategoryTrainingRuleDO> getTrainingRules(Long categoryId);

    List<DccFileCategoryTrainingRuleDO> replaceTrainingRules(Long categoryId,
                                                             List<DccCategoryTrainingRuleSaveReqVO> reqVOList);

    List<DccFileCategoryTrainingRuleDO> importTrainingRules(Long categoryId,
                                                            List<DccCategoryTrainingRuleSaveReqVO> reqVOList);
}
