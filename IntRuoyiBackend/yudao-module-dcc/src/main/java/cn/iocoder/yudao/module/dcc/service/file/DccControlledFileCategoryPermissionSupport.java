package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryPermissionRuleDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryPermissionRuleMapper;
import cn.iocoder.yudao.module.dcc.enums.DccAccessSubjectTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionScopeEnum;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DccControlledFileCategoryPermissionSupport {

    @Resource
    private DccFileCategoryPermissionRuleMapper permissionRuleMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private DeptApi deptApi;

    public boolean hasCategoryPermission(Long categoryId, Long userId, DccFileCategoryPermissionActionEnum actionType) {
        return evaluateCategoryPermission(categoryId, userId, actionType).allowed();
    }

    public CategoryPermissionEvaluation evaluateCategoryPermission(Long categoryId, Long userId,
                                                                   DccFileCategoryPermissionActionEnum actionType) {
        if (categoryId == null || userId == null || actionType == null) {
            return CategoryPermissionEvaluation.denied();
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null) {
            return CategoryPermissionEvaluation.denied();
        }
        List<DccFileCategoryPermissionRuleDO> matchedRules = permissionRuleMapper
                .selectList(DccFileCategoryPermissionRuleDO::getCategoryId, categoryId).stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getActive()))
                .filter(rule -> StrUtil.equalsIgnoreCase(rule.getActionType(), actionType.getCode()))
                .filter(rule -> matchesPermissionSubject(rule, userId, user))
                .toList();
        if (matchedRules.isEmpty()) {
            return CategoryPermissionEvaluation.denied();
        }
        boolean hasGlobalScope = matchedRules.stream()
                .anyMatch(rule -> !isProductGroupScope(rule));
        boolean hasProductGroupScope = matchedRules.stream().anyMatch(this::isProductGroupScope);
        return new CategoryPermissionEvaluation(true, hasGlobalScope, hasProductGroupScope);
    }

    private boolean matchesPermissionSubject(DccFileCategoryPermissionRuleDO rule, Long userId, AdminUserRespDTO user) {
        String subjectType = rule.getSubjectType();
        if (matchesSubjectType(subjectType, DccAccessSubjectTypeEnum.USER) && userId.equals(rule.getSubjectId())) {
            return true;
        }
        if (matchesSubjectType(subjectType, DccAccessSubjectTypeEnum.DEPT)
                && matchesDepartment(rule.getSubjectId(), user.getDeptId())) {
            return true;
        }
        if (matchesSubjectType(subjectType, DccAccessSubjectTypeEnum.POSITION)
                && user.getPostIds() != null && user.getPostIds().contains(rule.getSubjectId())) {
            return true;
        }
        return matchesSubjectType(subjectType, DccAccessSubjectTypeEnum.ROLE)
                && permissionApi.getUserRoleIdListByRoleIds(List.of(rule.getSubjectId())).contains(userId);
    }

    private boolean matchesSubjectType(String actual, DccAccessSubjectTypeEnum expected) {
        return StrUtil.equals(actual, String.valueOf(expected.getType()))
                || StrUtil.equalsIgnoreCase(actual, expected.name());
    }

    private boolean matchesDepartment(Long subjectDeptId, Long userDeptId) {
        if (subjectDeptId == null || userDeptId == null) {
            return false;
        }
        if (userDeptId.equals(subjectDeptId)) {
            return true;
        }
        return deptApi.getChildDeptList(subjectDeptId).stream()
                .anyMatch(dept -> userDeptId.equals(dept.getId()));
    }

    private boolean isProductGroupScope(DccFileCategoryPermissionRuleDO rule) {
        return StrUtil.equalsIgnoreCase(rule.getScopeType(),
                DccFileCategoryPermissionScopeEnum.PRODUCT_GROUP.getCode());
    }

    public record CategoryPermissionEvaluation(boolean allowed,
                                               boolean hasGlobalScope,
                                               boolean hasProductGroupScope) {

        public boolean productGroupScopedOnly() {
            return allowed && hasProductGroupScope && !hasGlobalScope;
        }

        public static CategoryPermissionEvaluation denied() {
            return new CategoryPermissionEvaluation(false, false, false);
        }

        public static CategoryPermissionEvaluation allowedGlobal() {
            return new CategoryPermissionEvaluation(true, true, false);
        }

        public static CategoryPermissionEvaluation allowedProductGroupScoped() {
            return new CategoryPermissionEvaluation(true, false, true);
        }
    }
}
