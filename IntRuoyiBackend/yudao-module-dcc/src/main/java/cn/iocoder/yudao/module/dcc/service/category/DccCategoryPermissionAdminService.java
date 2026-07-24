package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryPermissionRuleSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryPermissionRuleDO;

import java.util.List;

public interface DccCategoryPermissionAdminService {

    List<DccFileCategoryPermissionRuleDO> getPermissionRules(Long categoryId);

    List<DccFileCategoryPermissionRuleDO> replacePermissionRules(Long categoryId,
                                                                 List<DccCategoryPermissionRuleSaveReqVO> reqVOList);
}
