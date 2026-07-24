package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccApprovalPositionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccPositionAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccApprovalPositionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccPositionAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteNodeMapper;
import cn.iocoder.yudao.module.dcc.service.position.DccApprovalPositionRuntimeResolver;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DccControlledFileReviewMatrixAccessService {

    public static final String SOURCE_CURRENT_REVIEW_MATRIX = "CURRENT_REVIEW_MATRIX";
    public static final String SUBJECT_USER = "USER";
    public static final String SUBJECT_DEPT = "DEPT";
    public static final String SUBJECT_ROLE = "ROLE";
    public static final String SUBJECT_POST = "POST";
    public static final String SUBJECT_POSITION = "POSITION";
    public static final String SUBJECT_DCC_POSITION = "DCC_POSITION";

    @Resource
    private DccCategoryApprovalRouteMapper routeMapper;
    @Resource
    private DccCategoryApprovalRouteNodeMapper routeNodeMapper;
    @Resource
    private DccPositionAssignmentMapper positionAssignmentMapper;
    @Resource
    private DccApprovalPositionMapper approvalPositionMapper;
    @Resource
    private DccApprovalPositionRuntimeResolver positionRuntimeResolver;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private RoleApi roleApi;

    public boolean canAccessCurrentReviewMatrix(Long userId, DccControlledFileDO file) {
        if (userId == null || file == null || file.getCategoryId() == null) {
            return false;
        }
        return resolveCurrentReviewMatrixUserIds(file).contains(userId);
    }

    public Set<Long> resolveCurrentReviewMatrixUserIds(DccControlledFileDO file) {
        return resolveCurrentReviewMatrixAccessDetails(file).subjects().stream()
                .map(ReviewMatrixAccessSubject::userId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public ReviewMatrixAccessResolution resolveCurrentReviewMatrixAccessDetails(DccControlledFileDO file) {
        if (file == null || file.getCategoryId() == null) {
            return new ReviewMatrixAccessResolution(null, null, List.of(), List.of());
        }
        DccCategoryApprovalRouteDO route = routeMapper.selectLatestActiveByCategoryId(file.getCategoryId());
        if (route == null) {
            return new ReviewMatrixAccessResolution(null, null, List.of(),
                    List.of(new ReviewMatrixAccessRisk("MATRIX_NOT_CONFIGURED",
                            "当前文件类型未配置生效审阅矩阵，普通查阅权限无法解析。")));
        }
        return resolveRouteNodeAccessDetails(route.getId(), route.getVersionNo(),
                routeNodeMapper.selectListByRouteId(route.getId()), file.getRequesterId(), false);
    }

    public ReviewMatrixAccessResolution previewReviewMatrixAccessDetails(Long requesterUserId,
                                                                        List<DccCategoryApprovalRouteNodeDO> routeNodes) {
        return resolveRouteNodeAccessDetails(null, null, routeNodes, requesterUserId, true);
    }

    private ReviewMatrixAccessResolution resolveRouteNodeAccessDetails(Long routeId, Integer routeVersionNo,
                                                                      List<DccCategoryApprovalRouteNodeDO> routeNodes,
                                                                      Long requesterUserId,
                                                                      boolean previewMode) {
        List<ReviewMatrixAccessSubject> subjects = new ArrayList<>();
        List<ReviewMatrixAccessRisk> risks = new ArrayList<>();
        if (routeNodes == null) {
            routeNodes = List.of();
        }
        for (DccCategoryApprovalRouteNodeDO node : routeNodes) {
            subjects.addAll(resolveRouteNodeSubjects(node, requesterUserId, risks, previewMode));
        }
        Map<Long, String> userNameMap = resolveUserNameMap(subjects.stream()
                .map(ReviewMatrixAccessSubject::userId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        List<ReviewMatrixAccessSubject> namedSubjects = subjects.stream()
                .map(subject -> new ReviewMatrixAccessSubject(subject.userId(),
                        userNameMap.get(subject.userId()),
                        subject.source(),
                        subject.stageNo(),
                        subject.stageName(),
                        subject.stageType(),
                        subject.positionId(),
                        subject.positionName(),
                        subject.subjectLabel(),
                        subject.marker(),
                        subject.subjectType(),
                        subject.subjectId(),
                        subject.reason()))
                .toList();
        if (namedSubjects.isEmpty()) {
            risks.add(new ReviewMatrixAccessRisk("MATRIX_NO_RESOLVED_USER",
                    "当前审阅矩阵未解析到任何实际用户，普通查阅权限将无人可用。"));
        }
        return new ReviewMatrixAccessResolution(routeId, routeVersionNo, namedSubjects, risks);
    }

    private List<ReviewMatrixAccessSubject> resolveRouteNodeSubjects(DccCategoryApprovalRouteNodeDO routeNode,
                                                                     Long submitterUserId,
                                                                     List<ReviewMatrixAccessRisk> risks,
                                                                     boolean previewMode) {
        if (routeNode == null) {
            return List.of();
        }
        List<Long> candidateSourceIds = readCandidateSourceIds(routeNode.getCandidateSourceIds(),
                routeNode.getCandidateSourceId());
        String sourceType = normalizeSourceType(routeNode);
        return switch (sourceType) {
            case SUBJECT_USER -> resolveUserSubjects(routeNode, candidateSourceIds);
            case SUBJECT_DEPT -> resolveDepartmentSubjects(routeNode, candidateSourceIds, risks);
            case SUBJECT_ROLE -> resolveRoleSubjects(routeNode, candidateSourceIds, risks);
            case SUBJECT_POST, SUBJECT_POSITION -> resolvePostSubjects(routeNode, candidateSourceIds, risks);
            case SUBJECT_DCC_POSITION -> resolveDccPositionSubjects(routeNode, candidateSourceIds, submitterUserId, risks,
                    previewMode);
            default -> {
                risks.add(new ReviewMatrixAccessRisk("SUBJECT_TYPE_UNSUPPORTED",
                        "阶段“" + displayStage(routeNode) + "”的主体类型 " + sourceType + " 暂不支持解析。"));
                yield List.of();
            }
        };
    }

    private List<ReviewMatrixAccessSubject> resolveUserSubjects(DccCategoryApprovalRouteNodeDO routeNode,
                                                                List<Long> userIds) {
        return userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(userId -> buildSubject(userId, routeNode, null, null, "当前审阅矩阵阶段直接指定用户"))
                .toList();
    }

    private List<ReviewMatrixAccessSubject> resolveDepartmentSubjects(DccCategoryApprovalRouteNodeDO routeNode,
                                                                      List<Long> deptIds,
                                                                      List<ReviewMatrixAccessRisk> risks) {
        List<ReviewMatrixAccessSubject> subjects = new ArrayList<>();
        for (Long deptId : deptIds) {
            DeptRespDTO department = resolveDepartment(deptId);
            if (department == null || department.getLeaderUserId() == null) {
                risks.add(new ReviewMatrixAccessRisk("DEPT_LEADER_MISSING",
                        "阶段“" + displayStage(routeNode) + "”配置的部门 “"
                                + displayDept(department, deptId) + "”未配置负责人，无法按部门负责人解析。"));
                continue;
            }
            AdminUserRespDTO leader = adminUserApi.getUser(department.getLeaderUserId());
            if (leader == null || leader.getId() == null) {
                risks.add(new ReviewMatrixAccessRisk("DEPT_LEADER_USER_NOT_FOUND",
                        "阶段“" + displayStage(routeNode) + "”配置的部门 “"
                                + displayDept(department, deptId) + "”负责人用户不存在或不可用，无法按部门负责人解析。"));
                continue;
            }
            subjects.add(buildSubject(leader.getId(), routeNode, null, null,
                    "当前审阅矩阵阶段通过部门“" + displayDept(department, deptId) + "”负责人解析"));
        }
        return subjects;
    }

    private List<ReviewMatrixAccessSubject> resolveRoleSubjects(DccCategoryApprovalRouteNodeDO routeNode,
                                                                List<Long> roleIds,
                                                                List<ReviewMatrixAccessRisk> risks) {
        List<ReviewMatrixAccessSubject> subjects = new ArrayList<>();
        for (Long roleId : roleIds) {
            if (roleId == null) {
                continue;
            }
            roleApi.validRoleList(List.of(roleId));
            Set<Long> matchedUserIds = permissionApi.getUserRoleIdListByRoleIds(List.of(roleId));
            List<Long> userIds = matchedUserIds == null ? List.of() : matchedUserIds.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (userIds.isEmpty()) {
                risks.add(new ReviewMatrixAccessRisk("ROLE_EMPTY",
                        "阶段“" + displayStage(routeNode) + "”配置的系统角色 “"
                                + displayRole(routeNode, roleId) + "”未解析到任何有效用户。"));
                continue;
            }
            userIds.stream()
                    .map(userId -> buildSubject(userId, routeNode, null, null,
                            "当前审阅矩阵阶段通过系统角色解析"))
                    .forEach(subjects::add);
        }
        return subjects;
    }

    private List<ReviewMatrixAccessSubject> resolvePostSubjects(DccCategoryApprovalRouteNodeDO routeNode,
                                                                List<Long> postIds,
                                                                List<ReviewMatrixAccessRisk> risks) {
        List<ReviewMatrixAccessSubject> subjects = new ArrayList<>();
        for (Long postId : postIds) {
            List<AdminUserRespDTO> users = adminUserApi.getUserListByPostIds(List.of(postId));
            List<Long> userIds = users == null ? List.of() : users.stream()
                    .map(AdminUserRespDTO::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (userIds.isEmpty()) {
                risks.add(new ReviewMatrixAccessRisk("POSITION_EMPTY",
                        "阶段“" + displayStage(routeNode) + "”配置的系统岗位 " + postId + " 未解析到任何有效用户。"));
                continue;
            }
            userIds.stream()
                    .map(userId -> buildSubject(userId, routeNode, null, null,
                            "当前审阅矩阵阶段通过系统岗位全员解析"))
                    .forEach(subjects::add);
        }
        return subjects;
    }

    private List<ReviewMatrixAccessSubject> resolveDccPositionSubjects(DccCategoryApprovalRouteNodeDO routeNode,
                                                                       List<Long> positionIds,
                                                                       Long submitterUserId,
                                                                       List<ReviewMatrixAccessRisk> risks,
                                                                       boolean previewMode) {
        List<ReviewMatrixAccessSubject> subjects = new ArrayList<>();
        for (Long positionId : positionIds) {
            if (positionRuntimeResolver.isUploaderDerivedPosition(positionId)) {
                List<Long> resolvedUserIds = positionRuntimeResolver.resolveUserIds(positionId, submitterUserId, previewMode);
                if (resolvedUserIds.isEmpty()) {
                    risks.add(new ReviewMatrixAccessRisk("POSITION_EMPTY",
                            "阶段“" + displayStage(routeNode) + "”的上传人派生岗位 " + positionId + " 未解析到用户。"));
                    continue;
                }
                resolvedUserIds.stream()
                        .distinct()
                        .map(userId -> buildSubject(userId, routeNode, positionId, displayDccPosition(positionId),
                                "当前审阅矩阵阶段通过上传人派生岗位解析"))
                        .forEach(subjects::add);
                continue;
            }
            DccApprovalPositionDO position = approvalPositionMapper.selectById(positionId);
            List<Long> resolvedUserIds = positionAssignmentMapper.selectActiveListByPositionId(positionId).stream()
                    .flatMap(assignment -> resolveAssignmentUsers(assignment).stream())
                    .distinct()
                    .toList();
            if (resolvedUserIds.isEmpty()) {
                risks.add(new ReviewMatrixAccessRisk("POSITION_EMPTY",
                        "阶段“" + displayStage(routeNode) + "”配置的 DCC 岗位 " + positionId + " 未分配任何有效用户。"));
                continue;
            }
            String positionName = position == null ? "岗位#" + positionId : position.getName();
            resolvedUserIds.stream()
                    .map(userId -> buildSubject(userId, routeNode, positionId, positionName,
                            "当前审阅矩阵阶段通过 DCC 岗位分配解析"))
                    .forEach(subjects::add);
        }
        return subjects;
    }

    private List<Long> resolveAssignmentUsers(DccPositionAssignmentDO assignment) {
        if (assignment == null) {
            return List.of();
        }
        if (assignment.getUserId() != null) {
            return List.of(assignment.getUserId());
        }
        if ("POST".equalsIgnoreCase(assignment.getAssignmentType()) && assignment.getSystemPostId() != null) {
            List<AdminUserRespDTO> users = adminUserApi.getUserListByPostIds(List.of(assignment.getSystemPostId()));
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

    private DeptRespDTO resolveDepartment(Long deptId) {
        if (deptId == null) {
            return null;
        }
        List<DeptRespDTO> departments = deptApi.getDeptList(List.of(deptId));
        if (departments == null || departments.isEmpty()) {
            return null;
        }
        return departments.stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String normalizeSourceType(DccCategoryApprovalRouteNodeDO routeNode) {
        String subjectType = StrUtil.blankToDefault(routeNode.getSubjectType(), "").trim().toUpperCase();
        if (StrUtil.isNotBlank(subjectType)) {
            return SUBJECT_POSITION.equals(subjectType) ? SUBJECT_DCC_POSITION : subjectType;
        }
        String candidateSourceType = StrUtil.blankToDefault(routeNode.getCandidateSourceType(), "").trim().toUpperCase();
        if (SUBJECT_POSITION.equals(candidateSourceType)) {
            return SUBJECT_DCC_POSITION;
        }
        return candidateSourceType;
    }

    private List<Long> readCandidateSourceIds(String candidateSourceIds, Long candidateSourceId) {
        if (StrUtil.isNotBlank(candidateSourceIds)) {
            return Arrays.stream(candidateSourceIds.split(","))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .map(Long::valueOf)
                    .toList();
        }
        return candidateSourceId == null ? List.of() : List.of(candidateSourceId);
    }

    private ReviewMatrixAccessSubject buildSubject(Long userId, DccCategoryApprovalRouteNodeDO routeNode,
                                                   Long positionId, String positionName, String reason) {
        String subjectType = normalizeSourceType(routeNode);
        Long subjectId = routeNode.getSubjectId() != null ? routeNode.getSubjectId() : routeNode.getCandidateSourceId();
        if (subjectId == null && SUBJECT_DCC_POSITION.equals(subjectType)) {
            subjectId = positionId;
        }
        return new ReviewMatrixAccessSubject(userId,
                null,
                SOURCE_CURRENT_REVIEW_MATRIX,
                routeNode.getStageNo(),
                routeNode.getStageName(),
                resolveStageType(routeNode),
                positionId,
                positionName,
                StrUtil.blankToDefault(routeNode.getSubjectLabel(), routeNode.getSubjectName()),
                "▲",
                subjectType,
                subjectId,
                reason);
    }

    private Map<Long, String> resolveUserNameMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> orderedUserIds = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        List<AdminUserRespDTO> users = adminUserApi.getUserList(orderedUserIds);
        if (users == null) {
            return Collections.emptyMap();
        }
        return users.stream()
                .filter(user -> user.getId() != null)
                .collect(Collectors.toMap(AdminUserRespDTO::getId,
                        user -> StrUtil.blankToDefault(user.getNickname(), "用户#" + user.getId()),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private String displayStage(DccCategoryApprovalRouteNodeDO routeNode) {
        if (routeNode == null) {
            return "-";
        }
        return StrUtil.blankToDefault(routeNode.getStageName(),
                routeNode.getStageNo() == null ? "-" : String.valueOf(routeNode.getStageNo()));
    }

    private String displayDept(DeptRespDTO dept, Long deptId) {
        if (dept != null && StrUtil.isNotBlank(dept.getName())) {
            return dept.getName();
        }
        return deptId == null ? "部门" : "部门#" + deptId;
    }

    private String displayRole(DccCategoryApprovalRouteNodeDO routeNode, Long roleId) {
        if (routeNode != null && StrUtil.isNotBlank(routeNode.getSubjectName())) {
            return routeNode.getSubjectName();
        }
        if (routeNode != null && StrUtil.isNotBlank(routeNode.getSubjectLabel())) {
            return routeNode.getSubjectLabel();
        }
        return roleId == null ? "系统角色" : "角色#" + roleId;
    }

    private String displayDccPosition(Long positionId) {
        if (positionId == null) {
            return null;
        }
        DccApprovalPositionDO position = approvalPositionMapper.selectById(positionId);
        return position == null ? "岗位#" + positionId : position.getName();
    }

    private String resolveStageType(DccCategoryApprovalRouteNodeDO routeNode) {
        if (routeNode == null || routeNode.getStageNo() == null) {
            return null;
        }
        if (routeNode.getStageNo() == 2) {
            return "SIGNOFF";
        }
        if (routeNode.getStageNo() == 3) {
            return "APPROVAL";
        }
        return null;
    }

    public record ReviewMatrixAccessResolution(Long routeId,
                                               Integer routeVersionNo,
                                               List<ReviewMatrixAccessSubject> subjects,
                                               List<ReviewMatrixAccessRisk> risks) {
    }

    public record ReviewMatrixAccessSubject(Long userId,
                                            String userName,
                                            String source,
                                            Integer stageNo,
                                            String stageName,
                                            String stageType,
                                            Long positionId,
                                            String positionName,
                                            String subjectLabel,
                                            String marker,
                                            String subjectType,
                                            Long subjectId,
                                            String reason) {
    }

    public record ReviewMatrixAccessRisk(String code, String message) {
    }
}
