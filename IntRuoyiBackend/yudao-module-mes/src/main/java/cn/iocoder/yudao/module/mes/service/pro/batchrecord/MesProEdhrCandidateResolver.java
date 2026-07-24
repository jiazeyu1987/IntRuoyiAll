package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_REVIEW_ASSIGNEE_NOT_UNIQUE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_REVIEW_SOURCE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_ASSIGNEE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_CANDIDATE_POOL_EMPTY;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_CANDIDATE_SOURCE_INVALID;

@Service
public class MesProEdhrCandidateResolver {

    public static final String CANDIDATE_SOURCE_TYPE_USER = "USER";

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private DeptApi deptApi;

    public MesProEdhrCandidateContract resolveAssignmentRule(MesProEdhrWorkTaskAssignmentRuleDO rule) {
        String sourceType = StrUtil.blankToDefault(rule.getCandidateSourceType(), CANDIDATE_SOURCE_TYPE_USER);
        if (CANDIDATE_SOURCE_TYPE_USER.equals(sourceType)) {
            Long sourceId = rule.getCandidateSourceId() != null ? rule.getCandidateSourceId() : rule.getAssigneeUserId();
            if (sourceId == null) {
                return new MesProEdhrCandidateContract(sourceType, null, "");
            }
            AdminUserRespDTO user = adminUserApi.getUser(sourceId);
            if (user == null || !CommonStatusEnum.isEnable(user.getStatus())) {
                throw exception(PRO_EDHR_WORK_TASK_ASSIGNEE_INVALID);
            }
            return new MesProEdhrCandidateContract(sourceType, sourceId, sourceId.toString());
        }
        Long sourceId = rule.getCandidateSourceId();
        if (sourceId == null) {
            throw exception(PRO_EDHR_WORK_TASK_CANDIDATE_SOURCE_INVALID);
        }
        List<AdminUserRespDTO> users;
        if ("ROLE_GROUP".equals(sourceType)) {
            Set<Long> userIds = permissionApi.getUserRoleIdListByRoleIds(Set.of(sourceId));
            users = userIds == null || userIds.isEmpty() ? List.of() : adminUserApi.getUserList(userIds);
        } else if ("DEPT_GROUP".equals(sourceType)) {
            users = adminUserApi.getUserListByDeptIds(Set.of(sourceId));
        } else {
            throw exception(PRO_EDHR_WORK_TASK_CANDIDATE_SOURCE_INVALID);
        }
        return new MesProEdhrCandidateContract(sourceType, sourceId, toRequiredSnapshot(users,
                PRO_EDHR_WORK_TASK_CANDIDATE_POOL_EMPTY, null));
    }

    public MesProEdhrCandidateContract resolveProcessFormRule(MesProEdhrProcessFormPermissionRuleDO rule) {
        List<Long> sourceIds = parseCandidateSourceIds(rule.getCandidateSourceIds());
        if (sourceIds.isEmpty()) {
            throw exception(PRO_EDHR_WORK_TASK_CANDIDATE_SOURCE_INVALID);
        }
        List<AdminUserRespDTO> users = resolveUsers(rule.getCandidateSourceType(), sourceIds,
                PRO_EDHR_WORK_TASK_CANDIDATE_SOURCE_INVALID, null);
        return new MesProEdhrCandidateContract(rule.getCandidateSourceType(), null,
                toRequiredSnapshot(users, PRO_EDHR_WORK_TASK_CANDIDATE_POOL_EMPTY, null));
    }

    public List<MesProEdhrCandidateUser> resolveReviewCandidates(String signatureCellKey, String reviewSourceType,
                                                                 Long reviewSourceId, List<Long> reviewSourceIds) {
        if (StrUtil.isBlank(reviewSourceType)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_SOURCE_INVALID, signatureCellKey);
        }
        List<AdminUserRespDTO> users;
        if (Objects.equals("POST", reviewSourceType)) {
            if (reviewSourceId == null) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_SOURCE_INVALID, signatureCellKey);
            }
            users = adminUserApi.getUserListByPostIds(Set.of(reviewSourceId));
            return requireUniqueEnabledReviewCandidate(signatureCellKey, users);
        } else if (Objects.equals("ROLE", reviewSourceType)) {
            if (reviewSourceId == null) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_SOURCE_INVALID, signatureCellKey);
            }
            Set<Long> userIds = permissionApi.getUserRoleIdListByRoleIds(Set.of(reviewSourceId));
            users = userIds == null || userIds.isEmpty() ? List.of() : adminUserApi.getUserList(userIds);
            return requireEnabledReviewCandidates(signatureCellKey, users);
        } else if (Objects.equals("USER", reviewSourceType)) {
            if (reviewSourceId == null) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_SOURCE_INVALID, signatureCellKey);
            }
            users = adminUserApi.getUserList(Set.of(reviewSourceId));
            return requireUniqueEnabledReviewCandidate(signatureCellKey, users);
        } else if (Objects.equals("ROLES", reviewSourceType)) {
            if (reviewSourceIds == null || reviewSourceIds.isEmpty()) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_SOURCE_INVALID, signatureCellKey);
            }
            Set<Long> userIds = permissionApi.getUserRoleIdListByRoleIds(new LinkedHashSet<>(reviewSourceIds));
            users = userIds == null || userIds.isEmpty() ? List.of() : adminUserApi.getUserList(userIds);
            return requireEnabledReviewCandidates(signatureCellKey, users);
        } else if (Objects.equals("DEPT", reviewSourceType)) {
            if (reviewSourceId == null) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_SOURCE_INVALID, signatureCellKey);
            }
            users = adminUserApi.getUserListByDeptIds(Set.of(reviewSourceId));
            return requireEnabledReviewCandidates(signatureCellKey, users);
        } else if (Objects.equals("DEPTS", reviewSourceType)) {
            if (reviewSourceIds == null || reviewSourceIds.isEmpty()) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_SOURCE_INVALID, signatureCellKey);
            }
            users = adminUserApi.getUserListByDeptIds(new LinkedHashSet<>(reviewSourceIds));
            return requireEnabledReviewCandidates(signatureCellKey, users);
        } else if (Objects.equals("USERS", reviewSourceType)) {
            if (reviewSourceIds == null || reviewSourceIds.isEmpty()) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_SOURCE_INVALID, signatureCellKey);
            }
            users = adminUserApi.getUserList(new LinkedHashSet<>(reviewSourceIds));
            return requireEnabledReviewCandidates(signatureCellKey, users);
        } else if (Objects.equals("DEPT_LEADER", reviewSourceType)) {
            if (reviewSourceIds == null || reviewSourceIds.isEmpty()) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_SOURCE_INVALID, signatureCellKey);
            }
            users = resolveDeptLeaderUsers(reviewSourceIds);
            return requireEnabledReviewCandidates(signatureCellKey, users);
        }
        throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_SOURCE_INVALID, signatureCellKey + ":" + reviewSourceType);
    }

    public List<Long> parseCandidateSourceIds(String rawIds) {
        if (StrUtil.isBlank(rawIds)) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String item : rawIds.split(",")) {
            if (StrUtil.isBlank(item)) {
                continue;
            }
            ids.add(Long.parseLong(item.trim()));
        }
        return ids;
    }

    private List<AdminUserRespDTO> resolveUsers(String sourceType, List<Long> sourceIds,
                                                ErrorCode sourceInvalidCode, String sourceContext) {
        if ("USER".equals(sourceType) || "USERS".equals(sourceType)) {
            return adminUserApi.getUserList(sourceIds);
        }
        if ("ROLE".equals(sourceType)) {
            Set<Long> userIds = permissionApi.getUserRoleIdListByRoleIds(sourceIds);
            return userIds == null || userIds.isEmpty() ? List.of() : adminUserApi.getUserList(userIds);
        }
        if ("DEPT".equals(sourceType)) {
            return adminUserApi.getUserListByDeptIds(sourceIds);
        }
        if ("DEPT_LEADER".equals(sourceType)) {
            return resolveDeptLeaderUsers(sourceIds);
        }
        throw sourceContext == null ? exception(sourceInvalidCode) : exception(sourceInvalidCode, sourceContext);
    }

    private List<AdminUserRespDTO> resolveDeptLeaderUsers(List<Long> sourceIds) {
        List<Long> leaderUserIds = deptApi.getDeptList(sourceIds).stream()
                .filter(dept -> dept != null && CommonStatusEnum.isEnable(dept.getStatus()))
                .map(DeptRespDTO::getLeaderUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return leaderUserIds.isEmpty() ? List.of() : adminUserApi.getUserList(leaderUserIds);
    }

    private List<MesProEdhrCandidateUser> requireUniqueEnabledReviewCandidate(String signatureCellKey,
                                                                              List<AdminUserRespDTO> users) {
        List<MesProEdhrCandidateUser> enabledUsers = requireEnabledReviewCandidates(signatureCellKey, users);
        if (enabledUsers.size() != 1) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_ASSIGNEE_NOT_UNIQUE, signatureCellKey);
        }
        return enabledUsers;
    }

    private List<MesProEdhrCandidateUser> requireEnabledReviewCandidates(String signatureCellKey,
                                                                         List<AdminUserRespDTO> users) {
        if (users == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_ASSIGNEE_NOT_UNIQUE, signatureCellKey);
        }
        Map<Long, MesProEdhrCandidateUser> enabledUsers = new LinkedHashMap<>();
        for (AdminUserRespDTO user : users) {
            if (user != null && user.getId() != null && CommonStatusEnum.isEnable(user.getStatus())) {
                enabledUsers.putIfAbsent(user.getId(), new MesProEdhrCandidateUser(user.getId(), user.getNickname()));
            }
        }
        if (enabledUsers.isEmpty()) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_REVIEW_ASSIGNEE_NOT_UNIQUE, signatureCellKey);
        }
        return new ArrayList<>(enabledUsers.values());
    }

    private String toRequiredSnapshot(List<AdminUserRespDTO> users, ErrorCode emptyCode, String emptyContext) {
        String snapshot = users == null ? "" : users.stream()
                .filter(Objects::nonNull)
                .filter(user -> user.getId() != null && CommonStatusEnum.isEnable(user.getStatus()))
                .map(AdminUserRespDTO::getId)
                .distinct()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        if (snapshot.isBlank()) {
            throw emptyContext == null ? exception(emptyCode) : exception(emptyCode, emptyContext);
        }
        return snapshot;
    }

    public record MesProEdhrCandidateContract(String sourceType, Long sourceId, String userSnapshot) {
    }

    public record MesProEdhrCandidateUser(Long userId, String userName) {
    }
}
