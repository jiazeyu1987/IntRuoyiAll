package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccFileTypeTaxonomySaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileTypeTaxonomyDO;

import java.util.List;

public interface DccFileTypeTaxonomyAdminService {

    Long createTaxonomy(DccFileTypeTaxonomySaveReqVO reqVO);

    void updateTaxonomy(DccFileTypeTaxonomySaveReqVO reqVO);

    void deleteTaxonomy(Long id);

    List<DccFileTypeTaxonomyDO> getTaxonomyList();

    DccFileTypeTaxonomyPath resolveActivePath(Long id);

    List<Long> listActiveDescendantIds(Long id);

    List<DccFileTypeTaxonomyPath> listActiveDescendantPaths(Long id);

    Long resolveActiveIdByPath(String level1, String level2, String level3, String level4, String level5);
}
