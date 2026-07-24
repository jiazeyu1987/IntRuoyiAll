package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryDistributionRuleSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDistributionRuleDO;

import java.util.List;

public interface DccCategoryDistributionRuleAdminService {

    List<DccFileCategoryDistributionRuleDO> getDistributionRules(Long categoryId);

    List<DccFileCategoryDistributionRuleDO> replaceDistributionRules(Long categoryId,
                                                                     List<DccCategoryDistributionRuleSaveReqVO> reqVOList);

    List<DccFileCategoryDistributionRuleDO> importDistributionRules(Long categoryId,
                                                                    List<DccCategoryDistributionRuleSaveReqVO> reqVOList);
}
