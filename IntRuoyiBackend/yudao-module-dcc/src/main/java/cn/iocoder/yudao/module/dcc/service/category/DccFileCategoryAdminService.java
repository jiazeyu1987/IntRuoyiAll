package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryDirectoryBindingSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccFileCategorySaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryDirectoryBindingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;

import java.util.List;
import java.util.Map;

public interface DccFileCategoryAdminService {
    Long createCategory(DccFileCategorySaveReqVO reqVO);
    void updateCategory(DccFileCategorySaveReqVO reqVO);
    void deleteCategory(Long id);
    DccFileCategoryDO getCategory(Long id);
    List<DccFileCategoryDO> getCategoryList();
    DccFileCategoryImportResult importCategoriesFromIntAuth();
    Map<Long, Long> getCategoryDirectoryBindingMap();
    DccCategoryDirectoryBindingDO bindDirectory(Long categoryId, DccCategoryDirectoryBindingSaveReqVO reqVO);
}
