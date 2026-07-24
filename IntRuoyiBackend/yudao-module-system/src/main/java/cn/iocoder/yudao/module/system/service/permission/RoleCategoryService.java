package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.module.system.controller.admin.permission.vo.rolecategory.RoleCategorySaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleCategoryDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface RoleCategoryService {

    Long createRoleCategory(@Valid RoleCategorySaveReqVO createReqVO);

    void updateRoleCategory(@Valid RoleCategorySaveReqVO updateReqVO);

    void deleteRoleCategory(Long id);

    RoleCategoryDO getRoleCategory(Long id);

    List<RoleCategoryDO> getRoleCategoryList();

    List<RoleCategoryDO> getEnabledRoleCategoryList();

    Map<Long, RoleCategoryDO> getRoleCategoryMap(Collection<Long> ids);

    RoleCategoryDO validateRoleCategoryEnabled(Long id);

}
