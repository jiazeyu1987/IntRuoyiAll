package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryViewMatrixRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccApprovalPositionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccPositionAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryViewMatrixRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccApprovalPositionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccPositionAssignmentMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DccControlledFileViewMatrixAccessService {

    public static final String SOURCE_CURRENT_VIEW_MATRIX = "CURRENT_VIEW_MATRIX";
    public static final String SCOPE_ALL_MEMBERS = "ALL_MEMBERS";
    public static final String SCOPE_MANAGER_AND_ABOVE = "MANAGER_AND_ABOVE";
    public static final String SUBJECT_USER = "USER";
    public static final String SUBJECT_DEPT = "DEPT";
    public static final String SUBJECT_POST = "POST";
    public static final String SUBJECT_POSITION = "POSITION";
    public static final String SUBJECT_ROLE = "ROLE";
    public static final String SUBJECT_DCC_POSITION = "DCC_POSITION";
    public static final String SUBJECT_UNMAPPED_EXCEL = "UNMAPPED_EXCEL";

    @Resource
    private DccCategoryViewMatrixRuleMapper ruleMapper;
    @Resource
    private DccPositionAssignmentMapper positionAssignmentMapper;
    @Resource
    private DccApprovalPositionMapper approvalPositionMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private PermissionApi permissionApi;

    public boolean canAccessCurrentViewMatrix(Long userId, DccControlledFileDO file) {
        if (userId == null || file == null || file.getCategoryId() == null) {
            return false;
        }
        return resolveCurrentViewMatrixUserIds(file).contains(userId);
    }

    public Set<Long> resolveCurrentViewMatrixUserIds(DccControlledFileDO file) {
        if (file == null || file.getCategoryId() == null) {
            return Set.of();
        }
        return resolveCurrentViewMatrixAccessDetails(file.getCategoryId()).subjects().stream()
                .map(ViewMatrixAccessSubject::userId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public ViewMatrixAccessResolution resolveCurrentViewMatrixAccessDetails(Long categoryId) {
        return resolveCurrentViewMatrixAccessDetails(categoryId, newResolutionContext());
    }

    public ViewMatrixAccessResolution resolveCurrentViewMatrixAccessDetails(Long categoryId,
                                                                            ViewMatrixResolutionContext context) {
        if (categoryId == null) {
            return new ViewMatrixAccessResolution(null, List.of(), List.of(
                    risk("VIEW_MATRIX_CATEGORY_MISSING", "查看矩阵解析缺少文件类型。", "ERROR", true)));
        }
        List<ViewMatrixRuleInput> rules = ruleMapper.selectActiveListByCategoryId(categoryId).stream()
                .map(this::toInput)
                .toList();
        return resolveCurrentViewMatrixAccessDetails(categoryId, rules, context);
    }

    public ViewMatrixAccessResolution resolveCurrentViewMatrixAccessDetails(Long categoryId,
                                                                            List<ViewMatrixRuleInput> rules,
                                                                            ViewMatrixResolutionContext context) {
        Objects.requireNonNull(context, "viewMatrixResolutionContext must not be null");
        if (categoryId == null) {
            return new ViewMatrixAccessResolution(null, List.of(), List.of(
                    risk("VIEW_MATRIX_CATEGORY_MISSING", "查看矩阵解析缺少文件类型。", "ERROR", true)));
        }
        List<ViewMatrixRuleInput> activeRules = rules == null ? List.of() : rules.stream()
                .filter(rule -> Boolean.TRUE.equals(rule.active()))
                .toList();
        if (activeRules.isEmpty()) {
            return new ViewMatrixAccessResolution(categoryId, List.of(), List.of(
                    risk("VIEW_MATRIX_NOT_CONFIGURED", "当前文件类型未配置查看矩阵，普通查阅权限无法解析。", "ERROR", true)));
        }
        return resolveRules(categoryId, activeRules, context);
    }

    public ViewMatrixAccessResolution previewViewMatrixAccessDetails(Long categoryId, List<ViewMatrixRuleInput> rules) {
        if (categoryId == null) {
            return new ViewMatrixAccessResolution(null, List.of(), List.of(
                    risk("VIEW_MATRIX_CATEGORY_MISSING", "查看矩阵预览缺少文件类型。", "ERROR", true)));
        }
        if (rules == null || rules.stream().noneMatch(rule -> Boolean.TRUE.equals(rule.active()))) {
            return new ViewMatrixAccessResolution(categoryId, List.of(), List.of(
                    risk("VIEW_MATRIX_NOT_CONFIGURED", "查看矩阵未配置任何启用规则。", "ERROR", true)));
        }
        return resolveRules(categoryId, rules.stream().filter(rule -> Boolean.TRUE.equals(rule.active())).toList(),
                newResolutionContext());
    }

    public ViewMatrixRuleInput toInput(DccCategoryViewMatrixRuleDO rule) {
        return new ViewMatrixRuleInput(rule.getId(), rule.getCategoryId(), rule.getExcelFileName(),
                rule.getExcelRowNo(), rule.getExcelColumnLetter(), rule.getSubjectLabel(),
                rule.getSubjectTopHeader(), rule.getSubjectSubHeader(), rule.getMarker(), rule.getScopeType(),
                rule.getSubjectType(), rule.getSubjectId(), Boolean.TRUE.equals(rule.getActive()), rule.getRemark());
    }

    public ViewMatrixAccessResolution resolveRules(Long categoryId, List<ViewMatrixRuleInput> rules) {
        return resolveRules(categoryId, rules, newResolutionContext());
    }

    public ViewMatrixAccessResolution resolveRules(Long categoryId, List<ViewMatrixRuleInput> rules,
                                                   ViewMatrixResolutionContext context) {
        Objects.requireNonNull(context, "viewMatrixResolutionContext must not be null");
        List<ViewMatrixAccessSubject> subjects = new ArrayList<>();
        List<ViewMatrixAccessRisk> risks = new ArrayList<>();
        for (ViewMatrixRuleInput rule : rules == null ? List.<ViewMatrixRuleInput>of() : rules) {
            if (!Boolean.TRUE.equals(rule.active())) {
                continue;
            }
            subjects.addAll(resolveRuleSubjects(rule, risks, context));
        }
        subjects = fillMissingUserNames(subjects, context);
        addDuplicateUserRisks(subjects, risks);
        if (subjects.isEmpty() && risks.stream().noneMatch(ViewMatrixAccessRisk::blocking)) {
            risks.add(risk("VIEW_MATRIX_NO_RESOLVED_USER", "当前查看矩阵未解析到任何实际用户。", "ERROR", true));
        }
        return new ViewMatrixAccessResolution(categoryId, subjects, risks);
    }

    private List<ViewMatrixAccessSubject> resolveRuleSubjects(ViewMatrixRuleInput rule,
                                                              List<ViewMatrixAccessRisk> risks,
                                                              ViewMatrixResolutionContext context) {
        String subjectType = normalize(rule.subjectType());
        String scopeType = normalizeScope(rule);
        if (StrUtil.isBlank(scopeType)) {
            risks.add(risk("VIEW_MATRIX_SCOPE_MISSING",
                    "规则“" + describeRule(rule) + "”缺少 ●/▲ 对应的范围。", "ERROR", true));
            return List.of();
        }
        if (StrUtil.isBlank(subjectType) || SUBJECT_UNMAPPED_EXCEL.equals(subjectType)) {
            risks.add(risk("VIEW_MATRIX_SUBJECT_UNMAPPED",
                    "Excel 列“" + describeRule(rule) + "”未映射到系统用户、部门、岗位或 DCC 岗位。", "ERROR", true));
            return List.of();
        }
        if (SCOPE_MANAGER_AND_ABOVE.equals(scopeType)
                && !SUBJECT_USER.equals(subjectType)
                && !SUBJECT_DEPT.equals(subjectType)
                && !SUBJECT_ROLE.equals(subjectType)) {
            risks.add(risk("VIEW_MATRIX_MANAGER_SCOPE_UNRESOLVED",
                    "Excel 列“" + describeRule(rule) + "”为 ▲，但系统没有明确主管及以上解析规则。", "ERROR", true));
            return List.of();
        }
        if (rule.subjectId() == null) {
            risks.add(risk("VIEW_MATRIX_SUBJECT_ID_MISSING",
                    "规则“" + describeRule(rule) + "”缺少系统主体 ID。", "ERROR", true));
            return List.of();
        }
        return switch (subjectType) {
            case SUBJECT_USER -> resolveUserRule(rule, scopeType, risks, context);
            case SUBJECT_DEPT -> resolveDepartmentRule(rule, scopeType, risks, context);
            case SUBJECT_POST, SUBJECT_POSITION -> resolvePostRule(rule, scopeType, risks, context);
            case SUBJECT_ROLE -> resolveRoleRule(rule, scopeType, risks);
            case SUBJECT_DCC_POSITION -> resolveDccPositionRule(rule, scopeType, risks, context);
            default -> {
                risks.add(risk("VIEW_MATRIX_SUBJECT_TYPE_UNSUPPORTED",
                        "规则“" + describeRule(rule) + "”的主体类型 " + rule.subjectType() + " 不受支持。",
                        "ERROR", true));
                yield List.of();
            }
        };
    }

    private List<ViewMatrixAccessSubject> resolveUserRule(ViewMatrixRuleInput rule, String scopeType,
                                                          List<ViewMatrixAccessRisk> risks,
                                                          ViewMatrixResolutionContext context) {
        AdminUserRespDTO user = getUser(rule.subjectId(), context);
        if (user == null || user.getId() == null) {
            risks.add(risk("VIEW_MATRIX_USER_NOT_FOUND",
                    "规则“" + describeRule(rule) + "”指定的用户不存在或不可用。", "ERROR", true));
            return List.of();
        }
        return List.of(toSubject(rule, scopeType, user.getId(), displayUser(user),
                SCOPE_MANAGER_AND_ABOVE.equals(scopeType)
                        ? "当前查看矩阵通过显式主管用户解析"
                        : "当前查看矩阵通过显式用户解析"));
    }

    private List<ViewMatrixAccessSubject> resolveDepartmentRule(ViewMatrixRuleInput rule, String scopeType,
                                                                List<ViewMatrixAccessRisk> risks,
                                                                ViewMatrixResolutionContext context) {
        if (SCOPE_MANAGER_AND_ABOVE.equals(scopeType)) {
            return resolveDepartmentLeaderRule(rule, scopeType, risks, context);
        }
        List<Long> deptIds = resolveDepartmentTreeIds(rule, risks, context);
        if (deptIds.isEmpty()) {
            return List.of();
        }
        List<AdminUserRespDTO> users = getUserListByDeptIds(deptIds, context);
        if (users == null || users.stream().noneMatch(user -> user.getId() != null)) {
            risks.add(risk("VIEW_MATRIX_SUBJECT_EMPTY",
                    "规则“" + describeRule(rule) + "”映射的部门及子部门没有任何有效用户。", "ERROR", true));
            return List.of();
        }
        return users.stream()
                .filter(user -> user.getId() != null)
                .map(user -> toSubject(rule, scopeType, user.getId(), displayUser(user),
                        "当前查看矩阵通过部门及子部门全员解析"))
                .toList();
    }

    private List<ViewMatrixAccessSubject> resolveDepartmentLeaderRule(ViewMatrixRuleInput rule, String scopeType,
                                                                      List<ViewMatrixAccessRisk> risks,
                                                                      ViewMatrixResolutionContext context) {
        List<Long> deptIds = List.of(rule.subjectId());
        if (deptIds.isEmpty()) {
            return List.of();
        }
        List<DeptRespDTO> departments = getDeptList(deptIds, context);
        if (departments == null || departments.size() != deptIds.size()
                || departments.stream().anyMatch(dept -> dept == null || dept.getId() == null)) {
            risks.add(risk("VIEW_MATRIX_DEPT_NOT_FOUND",
                    "规则“" + describeRule(rule) + "”映射的部门不存在或不可用。", "ERROR", true));
            return List.of();
        }
        List<DeptRespDTO> missingLeaderDepartments = departments.stream()
                .filter(dept -> dept.getLeaderUserId() == null)
                .toList();
        if (!missingLeaderDepartments.isEmpty()) {
            risks.add(risk("VIEW_MATRIX_DEPT_LEADER_MISSING",
                    "规则“" + describeRule(rule) + "”映射的部门未配置负责人："
                            + missingLeaderDepartments.stream().map(this::displayDept).collect(Collectors.joining("、")),
                    "ERROR", true));
            return List.of();
        }
        List<ViewMatrixAccessSubject> subjects = new ArrayList<>();
        for (DeptRespDTO dept : departments) {
            AdminUserRespDTO leader = getUser(dept.getLeaderUserId(), context);
            if (leader == null || leader.getId() == null) {
                risks.add(risk("VIEW_MATRIX_DEPT_LEADER_USER_NOT_FOUND",
                        "规则“" + describeRule(rule) + "”映射的部门负责人用户不存在或不可用："
                                + displayDept(dept), "ERROR", true));
                return List.of();
            }
            subjects.add(toSubject(rule, scopeType, leader.getId(), displayUser(leader),
                    "当前查看矩阵通过部门“" + displayDept(dept) + "”负责人解析"));
        }
        return subjects;
    }

    private List<Long> resolveDepartmentTreeIds(ViewMatrixRuleInput rule, List<ViewMatrixAccessRisk> risks,
                                                ViewMatrixResolutionContext context) {
        List<DeptRespDTO> childDepartments = getChildDeptList(rule.subjectId(), context);
        if (childDepartments == null) {
            risks.add(risk("VIEW_MATRIX_DEPT_TREE_UNAVAILABLE",
                    "规则“" + describeRule(rule) + "”无法读取子部门树，不能确认递归查阅主体。", "ERROR", true));
            return List.of();
        }
        List<Long> deptIds = new ArrayList<>();
        deptIds.add(rule.subjectId());
        deptIds.addAll(childDepartments.stream()
                .map(DeptRespDTO::getId)
                .filter(Objects::nonNull)
                .toList());
        return deptIds.stream().distinct().toList();
    }

    private List<ViewMatrixAccessSubject> resolvePostRule(ViewMatrixRuleInput rule, String scopeType,
                                                          List<ViewMatrixAccessRisk> risks,
                                                          ViewMatrixResolutionContext context) {
        List<AdminUserRespDTO> users = getUserListByPostIds(List.of(rule.subjectId()), context);
        if (users == null || users.stream().noneMatch(user -> user.getId() != null)) {
            risks.add(risk("VIEW_MATRIX_SUBJECT_EMPTY",
                    "规则“" + describeRule(rule) + "”映射的系统岗位没有任何有效用户。", "ERROR", true));
            return List.of();
        }
        return users.stream()
                .filter(user -> user.getId() != null)
                .map(user -> toSubject(rule, scopeType, user.getId(), displayUser(user),
                        "当前查看矩阵通过系统岗位全员解析"))
                .toList();
    }

    private List<ViewMatrixAccessSubject> resolveRoleRule(ViewMatrixRuleInput rule, String scopeType,
                                                          List<ViewMatrixAccessRisk> risks) {
        Set<Long> userIds = permissionApi.getUserRoleIdListByRoleIds(List.of(rule.subjectId()));
        if (userIds == null || userIds.isEmpty()) {
            risks.add(risk("VIEW_MATRIX_SUBJECT_EMPTY",
                    "规则“" + describeRule(rule) + "”映射的系统角色没有任何有效用户。", "ERROR", true));
            return List.of();
        }
        return userIds.stream()
                .filter(Objects::nonNull)
                .sorted()
                .map(userId -> toSubject(rule, scopeType, userId, null,
                        SCOPE_MANAGER_AND_ABOVE.equals(scopeType)
                                ? "当前查看矩阵通过主管角色解析"
                                : "当前查看矩阵通过系统角色解析"))
                .toList();
    }

    private List<ViewMatrixAccessSubject> resolveDccPositionRule(ViewMatrixRuleInput rule, String scopeType,
                                                                 List<ViewMatrixAccessRisk> risks,
                                                                 ViewMatrixResolutionContext context) {
        DccApprovalPositionDO position = approvalPositionMapper.selectById(rule.subjectId());
        if (position == null) {
            risks.add(risk("VIEW_MATRIX_DCC_POSITION_NOT_FOUND",
                    "规则“" + describeRule(rule) + "”映射的 DCC 岗位不存在。", "ERROR", true));
            return List.of();
        }
        List<Long> userIds = positionAssignmentMapper.selectActiveListByPositionId(rule.subjectId()).stream()
                .flatMap(assignment -> resolveAssignmentUsers(assignment, context).stream())
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            risks.add(risk("VIEW_MATRIX_SUBJECT_EMPTY",
                    "规则“" + describeRule(rule) + "”映射的 DCC 岗位没有任何有效用户。", "ERROR", true));
            return List.of();
        }
        return userIds.stream()
                .map(userId -> toSubject(rule, scopeType, userId, null,
                        "当前查看矩阵通过 DCC 岗位“" + position.getName() + "”解析"))
                .toList();
    }

    private List<Long> resolveAssignmentUsers(DccPositionAssignmentDO assignment, ViewMatrixResolutionContext context) {
        if (assignment == null) {
            return List.of();
        }
        if (assignment.getUserId() != null) {
            return List.of(assignment.getUserId());
        }
        if ("POST".equalsIgnoreCase(assignment.getAssignmentType()) && assignment.getSystemPostId() != null) {
            List<AdminUserRespDTO> users = getUserListByPostIds(List.of(assignment.getSystemPostId()), context);
            if (users == null) {
                return List.of();
            }
            return users.stream()
                    .map(AdminUserRespDTO::getId)
                    .filter(Objects::nonNull)
                    .toList();
        }
        return List.of();
    }

    private List<ViewMatrixAccessSubject> fillMissingUserNames(List<ViewMatrixAccessSubject> subjects,
                                                               ViewMatrixResolutionContext context) {
        List<Long> unnamedUserIds = subjects.stream()
                .filter(subject -> StrUtil.isBlank(subject.userName()))
                .map(ViewMatrixAccessSubject::userId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> userNameMap = resolveUserNameMap(unnamedUserIds, context);
        return subjects.stream()
                .map(subject -> StrUtil.isNotBlank(subject.userName()) ? subject
                        : new ViewMatrixAccessSubject(subject.userId(), userNameMap.get(subject.userId()),
                        subject.source(), subject.excelFileName(), subject.excelRowNo(), subject.excelColumnLetter(),
                        subject.subjectLabel(), subject.marker(), subject.scopeType(), subject.subjectType(),
                        subject.subjectId(), subject.reason()))
                .toList();
    }

    private Map<Long, String> resolveUserNameMap(List<Long> userIds, ViewMatrixResolutionContext context) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<AdminUserRespDTO> users = getUserList(userIds, context);
        if (users == null) {
            throw new IllegalStateException("AdminUserApi#getUserList returned null while resolving view matrix names");
        }
        return users.stream()
                .filter(user -> user.getId() != null)
                .collect(Collectors.toMap(AdminUserRespDTO::getId, this::displayUser, (left, right) -> left,
                        LinkedHashMap::new));
    }

    public ViewMatrixResolutionContext newResolutionContext() {
        return new ViewMatrixResolutionContext();
    }

    private List<DeptRespDTO> getChildDeptList(Long deptId, ViewMatrixResolutionContext context) {
        if (!context.childDepartmentsByDeptId.containsKey(deptId)) {
            context.childDepartmentsByDeptId.put(deptId, deptApi.getChildDeptList(deptId));
        }
        return context.childDepartmentsByDeptId.get(deptId);
    }

    private List<DeptRespDTO> getDeptList(List<Long> deptIds, ViewMatrixResolutionContext context) {
        String key = idsKey(deptIds);
        if (!context.departmentsByIds.containsKey(key)) {
            context.departmentsByIds.put(key, deptApi.getDeptList(deptIds));
        }
        return context.departmentsByIds.get(key);
    }

    private AdminUserRespDTO getUser(Long userId, ViewMatrixResolutionContext context) {
        if (!context.userById.containsKey(userId)) {
            context.userById.put(userId, adminUserApi.getUser(userId));
        }
        return context.userById.get(userId);
    }

    private List<AdminUserRespDTO> getUserListByDeptIds(List<Long> deptIds, ViewMatrixResolutionContext context) {
        String key = idsKey(deptIds);
        if (!context.usersByDeptIds.containsKey(key)) {
            context.usersByDeptIds.put(key, adminUserApi.getUserListByDeptIds(deptIds));
        }
        return context.usersByDeptIds.get(key);
    }

    private List<AdminUserRespDTO> getUserListByPostIds(List<Long> postIds, ViewMatrixResolutionContext context) {
        String key = idsKey(postIds);
        if (!context.usersByPostIds.containsKey(key)) {
            context.usersByPostIds.put(key, adminUserApi.getUserListByPostIds(postIds));
        }
        return context.usersByPostIds.get(key);
    }

    private List<AdminUserRespDTO> getUserList(List<Long> userIds, ViewMatrixResolutionContext context) {
        String key = idsKey(userIds);
        if (!context.usersByIds.containsKey(key)) {
            context.usersByIds.put(key, adminUserApi.getUserList(userIds));
        }
        return context.usersByIds.get(key);
    }

    private String idsKey(List<Long> ids) {
        return ids == null ? "" : ids.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private void addDuplicateUserRisks(List<ViewMatrixAccessSubject> subjects, List<ViewMatrixAccessRisk> risks) {
        Map<Long, Long> counts = subjects.stream()
                .filter(subject -> subject.userId() != null)
                .collect(Collectors.groupingBy(ViewMatrixAccessSubject::userId, LinkedHashMap::new,
                        Collectors.counting()));
        counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .findFirst()
                .ifPresent(entry -> risks.add(risk("VIEW_MATRIX_DUPLICATE_USER",
                        "用户#" + entry.getKey() + " 被多条查看矩阵规则重复解析，请管理员确认来源是否重复。",
                        "WARNING", false)));
    }

    private String normalizeScope(ViewMatrixRuleInput rule) {
        String scopeType = normalize(rule.scopeType());
        if (StrUtil.isNotBlank(scopeType)) {
            return scopeType;
        }
        if ("●".equals(rule.marker())) {
            return SCOPE_ALL_MEMBERS;
        }
        if ("▲".equals(rule.marker())) {
            return SCOPE_MANAGER_AND_ABOVE;
        }
        return null;
    }

    private String normalize(String value) {
        return StrUtil.blankToDefault(value, "").trim().toUpperCase();
    }

    private ViewMatrixAccessSubject toSubject(ViewMatrixRuleInput rule, String scopeType, Long userId,
                                              String userName, String reason) {
        return new ViewMatrixAccessSubject(userId, userName, SOURCE_CURRENT_VIEW_MATRIX,
                rule.excelFileName(), rule.excelRowNo(), rule.excelColumnLetter(), rule.subjectLabel(),
                rule.marker(), scopeType, normalize(rule.subjectType()), rule.subjectId(), reason);
    }

    private ViewMatrixAccessRisk risk(String code, String message, String severity, boolean blocking) {
        return new ViewMatrixAccessRisk(code, message, severity, blocking);
    }

    private String displayUser(AdminUserRespDTO user) {
        return user == null ? null : StrUtil.blankToDefault(user.getNickname(), "用户#" + user.getId());
    }

    private String displayDept(DeptRespDTO dept) {
        return dept == null ? "-" : StrUtil.blankToDefault(dept.getName(), "部门#" + dept.getId());
    }

    private String describeRule(ViewMatrixRuleInput rule) {
        if (rule == null) {
            return "-";
        }
        if (StrUtil.isNotBlank(rule.subjectLabel())) {
            return rule.subjectLabel();
        }
        if (StrUtil.isNotBlank(rule.excelColumnLetter())) {
            return rule.excelColumnLetter();
        }
        return StrUtil.blankToDefault(rule.excelFileName(), "-");
    }

    public record ViewMatrixAccessResolution(Long categoryId,
                                             List<ViewMatrixAccessSubject> subjects,
                                             List<ViewMatrixAccessRisk> risks) {
    }

    public record ViewMatrixRuleInput(Long id,
                                      Long categoryId,
                                      String excelFileName,
                                      Integer excelRowNo,
                                      String excelColumnLetter,
                                      String subjectLabel,
                                      String subjectTopHeader,
                                      String subjectSubHeader,
                                      String marker,
                                      String scopeType,
                                      String subjectType,
                                      Long subjectId,
                                      Boolean active,
                                      String remark) {
    }

    public record ViewMatrixAccessSubject(Long userId,
                                          String userName,
                                          String source,
                                          String excelFileName,
                                          Integer excelRowNo,
                                          String excelColumnLetter,
                                          String subjectLabel,
                                          String marker,
                                          String scopeType,
                                          String subjectType,
                                          Long subjectId,
                                          String reason) {
    }

    public record ViewMatrixAccessRisk(String code,
                                       String message,
                                       String severity,
                                       Boolean blocking) {
    }

    public static class ViewMatrixResolutionContext {

        private final Map<Long, List<DeptRespDTO>> childDepartmentsByDeptId = new LinkedHashMap<>();
        private final Map<String, List<DeptRespDTO>> departmentsByIds = new LinkedHashMap<>();
        private final Map<Long, AdminUserRespDTO> userById = new LinkedHashMap<>();
        private final Map<String, List<AdminUserRespDTO>> usersByDeptIds = new LinkedHashMap<>();
        private final Map<String, List<AdminUserRespDTO>> usersByPostIds = new LinkedHashMap<>();
        private final Map<String, List<AdminUserRespDTO>> usersByIds = new LinkedHashMap<>();
    }
}
