package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
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
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper projectMapper;
    public boolean isLibrary(Long projectId) {
        if(projectId==null) return false;
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var request=org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        String key=getClass().getName()+".library."+tenant+"."+projectId;
        if(request!=null && request.getAttribute(key,0) instanceof Boolean cached) return cached;
        var project=projectMapper.selectById(projectId);
        boolean library=project!=null && project.isLibrary() && java.util.Objects.equals(project.getTenantId(),tenant);
        if(request!=null) request.setAttribute(key,library,0);
        return library;
    }


    @Resource
    private DccFileCategoryPermissionRuleMapper permissionRuleMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private DccProjectAccessService projectAccessService;

    public static final String EDIT_PERMISSION = "dcc:controlled-file:edit";

    public boolean hasEditableProjectCategory(Long userId, Long projectId, Long categoryId) {
        if (isLibrary(projectId)) return projectAccessService.hasProjectEditorOrOwner(userId, projectId);
        return projectId != null
                && hasCategoryPermission(categoryId, userId, DccFileCategoryPermissionActionEnum.EDIT)
                && projectAccessService.hasProjectEditorOrOwner(userId, projectId);
    }

    public boolean hasFilePermission(DccControlledFileDO file, Long userId,
                                     DccFileCategoryPermissionActionEnum action) {
        if (action == null || userId == null || file == null || Boolean.TRUE.equals(file.getDeleted()) || TenantContextHolder.isIgnore()
                || !java.util.Objects.equals(file.getTenantId(), TenantContextHolder.getTenantId())) return false;
        if (isLibrary(file.getDccProjectCodeId()) && action != DccFileCategoryPermissionActionEnum.REVIEW
                && action != DccFileCategoryPermissionActionEnum.APPROVE) {
            return action == DccFileCategoryPermissionActionEnum.VIEW
                    ? projectAccessService.listReadableProjectIds(userId).contains(file.getDccProjectCodeId())
                    : projectAccessService.hasProjectEditorOrOwner(userId, file.getDccProjectCodeId());
        }
        if (action == DccFileCategoryPermissionActionEnum.VIEW) {
            return file.getDccProjectCodeId() != null
                    && projectAccessService.listReadableProjectIds(userId).contains(file.getDccProjectCodeId())
                    && (evaluateExact(file.getCategoryId(), userId, action).allowed()
                        || hasEditableProjectCategory(userId, file.getDccProjectCodeId(), file.getCategoryId()));
        }
        // Frozen approval tasks authorize their actors independently of ordinary file editing.
        if (action == DccFileCategoryPermissionActionEnum.REVIEW || action == DccFileCategoryPermissionActionEnum.APPROVE)
            return evaluateExact(file.getCategoryId(), userId, action).allowed();
        return hasEditableProjectCategory(userId, file.getDccProjectCodeId(), file.getCategoryId());
    }

    public boolean hasCategoryPermission(Long categoryId, Long userId, DccFileCategoryPermissionActionEnum actionType) {
        return evaluateCategoryPermission(categoryId, userId, actionType).allowed();
    }

    public CategoryPermissionEvaluation evaluateCategoryPermission(Long categoryId, Long userId,
                                                                   DccFileCategoryPermissionActionEnum actionType) {
        if (actionType == null) return CategoryPermissionEvaluation.denied();
        if (actionType == DccFileCategoryPermissionActionEnum.REVIEW || actionType == DccFileCategoryPermissionActionEnum.APPROVE)
            return evaluateExact(categoryId, userId, actionType);
        if (actionType == DccFileCategoryPermissionActionEnum.VIEW) {
            var view = evaluateExact(categoryId, userId, actionType);
            if (view.allowed()) return view;
        }
        if (userId == null || !permissionApi.hasAnyPermissions(userId, EDIT_PERMISSION))
            return CategoryPermissionEvaluation.denied();
        return evaluateExact(categoryId, userId, DccFileCategoryPermissionActionEnum.EDIT);
    }

    public boolean hasWorkflowTrainingPermission(Long categoryId, Long userId) {
        return evaluateExact(categoryId, userId, DccFileCategoryPermissionActionEnum.TRAINING_RECORD).allowed();
    }

    private CategoryPermissionEvaluation evaluateExact(Long categoryId, Long userId,
                                                        DccFileCategoryPermissionActionEnum actionType) {
        if (TenantContextHolder.isIgnore() || categoryId == null || userId == null || actionType == null) {
            return CategoryPermissionEvaluation.denied();
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (!DccAccessUserIdentity.isEnabledTenantUser(userId, user)) {
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
