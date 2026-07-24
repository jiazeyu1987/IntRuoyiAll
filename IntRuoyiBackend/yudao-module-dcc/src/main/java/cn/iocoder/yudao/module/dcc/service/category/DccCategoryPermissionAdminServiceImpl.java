package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryPermissionRuleSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryPermissionRuleDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryPermissionRuleMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CATEGORY_PERMISSION_RULE_MANUAL_REVIEW_APPROVE_FORBIDDEN;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_NOT_EXISTS;

@Service
@Validated
public class DccCategoryPermissionAdminServiceImpl implements DccCategoryPermissionAdminService {

    private static final Set<String> MATRIX_MANAGED_ACTIONS = Set.of("REVIEW", "APPROVE");

    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DccFileCategoryPermissionRuleMapper permissionRuleMapper;

    @Override
    public List<DccFileCategoryPermissionRuleDO> getPermissionRules(Long categoryId) {
        validateCategoryExists(categoryId);
        return permissionRuleMapper.selectList(DccFileCategoryPermissionRuleDO::getCategoryId, categoryId).stream()
                .sorted(Comparator.comparing(DccFileCategoryPermissionRuleDO::getActionType)
                        .thenComparing(DccFileCategoryPermissionRuleDO::getSubjectType)
                        .thenComparing(DccFileCategoryPermissionRuleDO::getSubjectId))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<DccFileCategoryPermissionRuleDO> replacePermissionRules(Long categoryId,
                                                                        List<DccCategoryPermissionRuleSaveReqVO> reqVOList) {
        validateCategoryExists(categoryId);
        boolean containsMatrixManagedAction = reqVOList.stream()
                .anyMatch(reqVO -> reqVO.getActionType() != null
                        && MATRIX_MANAGED_ACTIONS.contains(reqVO.getActionType().trim().toUpperCase()));
        if (containsMatrixManagedAction) {
            throw exception(CATEGORY_PERMISSION_RULE_MANUAL_REVIEW_APPROVE_FORBIDDEN);
        }
        // Non-matrix rules are rebuilt as configuration state; REVIEW/APPROVE
        // remain owned by the approval matrix service.
        permissionRuleMapper.deleteConfigurableByCategoryIdHard(categoryId);
        List<DccFileCategoryPermissionRuleDO> rules = CollectionUtils.convertList(reqVOList, reqVO -> {
            DccFileCategoryPermissionRuleDO rule = BeanUtils.toBean(reqVO, DccFileCategoryPermissionRuleDO.class);
            rule.setCategoryId(categoryId);
            return rule;
        });
        rules.forEach(permissionRuleMapper::insert);
        return getPermissionRules(categoryId);
    }

    private DccFileCategoryDO validateCategoryExists(Long categoryId) {
        DccFileCategoryDO category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw exception(FILE_CATEGORY_NOT_EXISTS);
        }
        return category;
    }
}
