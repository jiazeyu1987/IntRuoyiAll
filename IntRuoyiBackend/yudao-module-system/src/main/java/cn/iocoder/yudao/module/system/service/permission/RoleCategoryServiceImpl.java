package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.rolecategory.RoleCategorySaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleCategoryDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleCategoryMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;

@Service
@Validated
public class RoleCategoryServiceImpl implements RoleCategoryService {

    @Resource
    private RoleCategoryMapper roleCategoryMapper;
    @Resource
    private RoleMapper roleMapper;

    @Override
    public Long createRoleCategory(RoleCategorySaveReqVO createReqVO) {
        validateRoleCategoryForCreateOrUpdate(null, createReqVO.getName(), createReqVO.getCode());
        RoleCategoryDO category = BeanUtils.toBean(createReqVO, RoleCategoryDO.class);
        roleCategoryMapper.insert(category);
        return category.getId();
    }

    @Override
    public void updateRoleCategory(RoleCategorySaveReqVO updateReqVO) {
        validateRoleCategoryForCreateOrUpdate(updateReqVO.getId(), updateReqVO.getName(), updateReqVO.getCode());
        roleCategoryMapper.updateById(BeanUtils.toBean(updateReqVO, RoleCategoryDO.class));
    }

    @Override
    public void deleteRoleCategory(Long id) {
        RoleCategoryDO category = validateRoleCategoryExists(id);
        if (roleMapper.selectCountByCategoryId(id) > 0) {
            throw exception(ROLE_CATEGORY_HAS_ROLES, category.getName());
        }
        roleCategoryMapper.deleteById(id);
    }

    @Override
    public RoleCategoryDO getRoleCategory(Long id) {
        return roleCategoryMapper.selectById(id);
    }

    @Override
    public List<RoleCategoryDO> getRoleCategoryList() {
        return roleCategoryMapper.selectListOrderBySort();
    }

    @Override
    public List<RoleCategoryDO> getEnabledRoleCategoryList() {
        return roleCategoryMapper.selectEnabledList();
    }

    @Override
    public Map<Long, RoleCategoryDO> getRoleCategoryMap(Collection<Long> ids) {
        if (CollectionUtils.isAnyEmpty(ids)) {
            return Collections.emptyMap();
        }
        return convertMap(roleCategoryMapper.selectByIds(ids), RoleCategoryDO::getId);
    }

    @Override
    public RoleCategoryDO validateRoleCategoryEnabled(Long id) {
        RoleCategoryDO category = validateRoleCategoryExists(id);
        if (!CommonStatusEnum.ENABLE.getStatus().equals(category.getStatus())) {
            throw exception(ROLE_CATEGORY_DISABLE, category.getName());
        }
        return category;
    }

    private void validateRoleCategoryForCreateOrUpdate(Long id, String name, String code) {
        if (id != null) {
            validateRoleCategoryExists(id);
        }
        validateRoleCategoryNameUnique(id, name);
        validateRoleCategoryCodeUnique(id, code);
    }

    private RoleCategoryDO validateRoleCategoryExists(Long id) {
        RoleCategoryDO category = roleCategoryMapper.selectById(id);
        if (category == null) {
            throw exception(ROLE_CATEGORY_NOT_EXISTS);
        }
        return category;
    }

    private void validateRoleCategoryNameUnique(Long id, String name) {
        RoleCategoryDO category = roleCategoryMapper.selectByName(name);
        if (category != null && (id == null || !category.getId().equals(id))) {
            throw exception(ROLE_CATEGORY_NAME_DUPLICATE, name);
        }
    }

    private void validateRoleCategoryCodeUnique(Long id, String code) {
        RoleCategoryDO category = roleCategoryMapper.selectByCode(code);
        if (category != null && (id == null || !category.getId().equals(id))) {
            throw exception(ROLE_CATEGORY_CODE_DUPLICATE, code);
        }
    }

}
